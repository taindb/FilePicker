package com.example.taindb.pickfile

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.support.annotation.NonNull
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Created by Taindb
 * on 5/30/18.
 * taindb@gmail.com
 */

class FileUtils {

    constructor() {
        throw IllegalStateException("Cannot instantiate object of utility class")
    }

    companion object {

        private const val BYTES_IN_KILOBYTES = 1024

        private const val MB = BYTES_IN_KILOBYTES * 1024

        private const val MIN_FILE_SIZE = 30 * BYTES_IN_KILOBYTES

        private const val MAX_FILE_SIZE = 20 * MB

        private val FILE_EXTENSION_ALLOWED = listOf("bmp", "jpg", "jpeg", "png", "tiff", "pdf")

        fun processAccountDataResult(context: Context, data: Intent?, imageUriFromTakeCamera: Uri, listener: FilePicker.OnPickFileListener) {
            var fileUri: Uri? = null
            fileUri = if (data != null && data.data != null) {
                data.data
            } else {
                imageUriFromTakeCamera
            }

            if (fileUri == null) return
            // check file extension
            val extension = getExtension(getMimeType(context, fileUri))
            if (!isFileExtensionAllowed(extension)) {
                listener.onPickFileFail()
                return
            }

            // get real path of file
            val realPath = RealPathUtils.getRealPath(context, fileUri) ?: return

            //check file size
            val realFile = File(realPath)
            if (!isFileSizeAllowed(realFile.length())) {
                listener.onPickFileFail()
                return
            }

            listener.onPickFileSuccess(realFile)
        }

        fun saveFile(inputStream: InputStream, @NonNull fileName: String): File {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absoluteFile, fileName)
            if (file.exists()) return file

            val output = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024) // or other buffer size
            var read: Int

            read = inputStream.read(buffer)
            while (read != -1) {
                output.write(buffer, 0, read)
                read = inputStream.read(buffer)
            }

            output.flush()
            output.close()
            inputStream.close()
            return file
        }


        fun isFileExtensionAllowed(extension: String): Boolean {
            return FILE_EXTENSION_ALLOWED.contains(extension)
        }

        private fun isFileSizeAllowed(size: Long): Boolean {
            return size in MIN_FILE_SIZE..MAX_FILE_SIZE
        }

        fun getMimeType(context: Context, uri: Uri): String {
            return if (uri.scheme == ContentResolver.SCHEME_CONTENT)
                context.contentResolver.getType(uri)
            else
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
        }

        fun getExtension(mimeType: String): String {
            return mimeType.substring(mimeType.indexOf("/") + 1)
        }

    }

}