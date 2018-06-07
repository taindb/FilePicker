package com.example.taindb.pickfile

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import android.support.annotation.NonNull
import java.io.File


/**
 * Created by Taindb
 * on 5/30/18.
 * taindb@gmail.com
 */
class FilePicker {

    companion object {
        private const val MIME_TYPE_AUDIO = "audio/*"

        private const val MIME_TYPE_TEXT = "text/*"

        private const val MIME_TYPE_IMAGE = "image/*"

        private const val MIME_TYPE_VIDEO = "video/*"

        private const val MIME_TYPE_APP = "application/*"
    }


    private var mChooserIntent: Intent? = null

    var mImageUri: Uri? = null

    fun setImageUri(imageUri: Uri) {
        mImageUri = imageUri
    }

    private fun setChooserIntent(@NonNull chooserIntent: Intent) {
        mChooserIntent = chooserIntent
    }

    fun getChooserIntent(): Intent? {
        return mChooserIntent
    }

    /**
     * [Builder]
     */
    class Builder {
        private var mContext: Context? = null

        private var mFileTargetIntent: Intent? = null

        private var mImageTargetIntent: Intent? = null

        private var mTakePhotoTargetIntent: Intent? = null

        private var mChooserIntent: Intent? = null

        private var mImageUri: Uri? = null

        private val mTargetIntents = ArrayList<Intent>()

        fun setContext(context: Context): Builder {
            mContext = context
            return this
        }

        fun createFileTargetIntent(): Builder {
            mFileTargetIntent = Intent()
            mFileTargetIntent!!.action = Intent.ACTION_GET_CONTENT
            mFileTargetIntent!!.type = MIME_TYPE_APP
            return this
        }

        fun createImageTargetIntent(): Builder {
            mImageTargetIntent = Intent()
            mImageTargetIntent!!.action = Intent.ACTION_PICK
            mImageTargetIntent!!.type = MIME_TYPE_IMAGE
            return this
        }

        fun createTakePhotoTargetIntent(): Builder {
            mTakePhotoTargetIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val contentValues = ContentValues()
            contentValues.put(MediaStore.Images.Media.TITLE, "new_picture")
            contentValues.put(MediaStore.Images.Media.DESCRIPTION, "From_your_camera")
            mImageUri = mContext!!.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            mTakePhotoTargetIntent!!.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri)
            return this
        }

        fun createChooserIntent(title: String): Builder {
            if (mImageTargetIntent != null)
                addTargetIntent(mImageTargetIntent)

            if (mFileTargetIntent != null)
                addTargetIntent(mFileTargetIntent)

            if (mTakePhotoTargetIntent != null)
                addTargetIntent(mTakePhotoTargetIntent)

            mChooserIntent = Intent.createChooser(
                    mTargetIntents.removeAt(mTargetIntents.size - 1),
                    title
            )

            mChooserIntent?.putExtra(Intent.EXTRA_INITIAL_INTENTS, mTargetIntents.toTypedArray<Parcelable>())
            return this
        }


        private fun addTargetIntent(targetIntent: Intent?) {
            val resInfo = mContext?.packageManager?.queryIntentActivities(targetIntent, 0)
            resInfo?.forEach { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val targetedIntent = Intent(targetIntent)
                targetedIntent.`package` = packageName
                targetIntent?.let { mTargetIntents.add(it) }
            }
        }

        fun build(): FilePicker {
            var filePicker = FilePicker()
            mChooserIntent?.let { filePicker.setChooserIntent(it) }
            mImageUri?.let { filePicker.setImageUri(it) }
            return filePicker
        }
    }


    interface OnPickFileListener {
        fun onPickFileSuccess(file: File)

        fun onPickFileFail()
    }
}