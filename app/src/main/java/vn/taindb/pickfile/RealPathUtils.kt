package com.example.taindb.pickfile

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.support.v4.content.CursorLoader
import android.support.v4.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by Taindb
 * on 5/30/18.
 * taindb@gmail.com
 */
class RealPathUtils {


    constructor() {
        throw IllegalStateException("Cannot instantiate object of utility class")
    }

    companion object {

        private val GOOGLE_DRIVE_FILE_NAME_FORMATTER = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

        @SuppressLint("ObsoleteSdkInt")
        fun getRealPath(context: Context, fileUri: Uri): String? {
            // SDK < API11
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                getRealPathFromURIBelowAPI11(context, fileUri)
            } else getRealPathFromURIAPI19(context, fileUri)
            // SDK >= 11 HONEYCOMB
        }

        // don't work on real devices
        @SuppressLint("NewApi")
        private fun getRealPathFromURIAPI11to18(context: Context, contentUri: Uri): String? {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            var result: String? = null

            val cursorLoader = CursorLoader(context, contentUri, proj, null, null, null)
            val cursor = cursorLoader.loadInBackground()

            if (cursor != null) {
                val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToFirst()
                result = cursor.getString(column_index)
                cursor.close()
            }
            return result
        }

        private fun getRealPathFromURIBelowAPI11(context: Context, contentUri: Uri): String {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            var column_index = 0
            var result = ""
            if (cursor != null) {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToFirst()
                result = cursor.getString(column_index)
                cursor.close()
                return result
            }
            return result
        }

        /**
         * Get a file path from a Uri. This will get the the path for Storage Access
         * Framework Documents, as well as the _data field for the MediaStore and
         * other file-based ContentProviders.
         *
         * @param context The context.
         * @param uri     The Uri to query.
         * @author paulburke
         */
        @SuppressLint("NewApi")
        private fun getRealPathFromURIAPI19(context: Context, uri: Uri): String? {

            val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val type = split[0]

                    if ("primary".equals(type, ignoreCase = true)) {
                        return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    }

                    // TODO handle non-primary volumes
                } else if (isDownloadsDocument(uri)) {

                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(id))

                    return getDataColumn(context, contentUri, null, null)
                } else if (isMediaDocument(uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val type = split[0]

                    var contentUri: Uri? = null
                    when (type) {
                        "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }

                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])

                    return getDataColumn(context, contentUri, selection, selectionArgs)
                } else if (isGoogleDriveDocument(uri)) {
                    return getPathGoogleDriveFile(context, uri)
                }// MediaProvider
                // DownloadsProvider
            } else if (isGoogleDriveDocument(uri)) {
                return getPathGoogleDriveFile(context, uri)
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {

                // Return the remote address
                return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(context, uri, null, null)

            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }// File
            // MediaStore (and general)

            return null
        }

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context       The context.
         * @param uri           The Uri to query.
         * @param selection     (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         */
        private fun getDataColumn(context: Context, uri: Uri?, selection: String?,
                                  selectionArgs: Array<String>?): String? {

            var cursor: Cursor? = null
            val column = "_data"
            val projection = arrayOf(column)

            try {
                cursor = context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndexOrThrow(column)
                    return cursor.getString(index)
                }
            } finally {
                if (cursor != null)
                    cursor.close()
            }
            return null
        }


        /**
         * Save a file on GG Drive into Disk and return the path of file
         *
         * @param context The context.
         * @param uri     The Uri to query.
         * @return the path of file save on disk
         */
        private fun getPathGoogleDriveFile(context: Context, uri: Uri): String {
            var cursor: Cursor? = null
            try {

                val proj = arrayOf(OpenableColumns.DISPLAY_NAME)
                cursor = context.contentResolver.query(uri, proj, null, null, null)
                val fileName: String
                val extension = FileUtils.getExtension(context.contentResolver.getType(uri))
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                fileName = if (documentFile != null)
                    documentFile.name + "." + extension
                else {
                    GOOGLE_DRIVE_FILE_NAME_FORMATTER.format(Date()) + "." + extension
                }
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = FileUtils.saveFile(inputStream, fileName)
                    return file.path
                }
            } catch (ignored: Exception) {
            } finally {
                if (cursor != null) cursor.close()
            }

            return ""
        }


        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is GoogleDriveDocument.
         */
        private fun isGoogleDriveDocument(uri: Uri): Boolean {
            return "com.google.android.apps.docs.storage.legacy" == uri.authority || "com.google.android.apps.docs.storage" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        private fun isExternalStorageDocument(uri: Uri): Boolean {
            return "com.android.externalstorage.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        private fun isDownloadsDocument(uri: Uri): Boolean {
            return "com.android.providers.downloads.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        private fun isMediaDocument(uri: Uri): Boolean {
            return "com.android.providers.media.documents" == uri.authority
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is Google Photos.
         */
        private fun isGooglePhotosUri(uri: Uri): Boolean {
            return "com.google.android.apps.photos.content" == uri.authority
        }

    }


}