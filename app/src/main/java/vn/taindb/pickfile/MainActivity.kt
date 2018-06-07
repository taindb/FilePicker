package com.example.taindb.pickfile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.taindb.readpdf.R
import java.io.File


/**
 * Created by Taindb
 * on 5/30/18.
 * taindb@gmail.com
 */
class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val FILE_REQUEST_CODE: Int = 110

    private val FILE_PERMISSION_REQUEST_CODE: Int = 120

    private val PERMISSION_REQUEST_LIST = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA)

    private lateinit var mPathNameTv: TextView

    private lateinit var mImageUri: Uri

    private val mPermissionGrantedRunnable = Runnable {
        val filePicker = FilePicker
                .Builder()
                .setContext(this)
                .createFileTargetIntent()
                .createImageTargetIntent()
                .createTakePhotoTargetIntent()
                .createChooserIntent("Choose file from:")
                .build()

        mImageUri = filePicker.mImageUri!!
        val chooserIntent = filePicker.getChooserIntent()
        startActivityForResult(chooserIntent, FILE_REQUEST_CODE)
    }

    private val mFilePickListener = object : FilePicker.OnPickFileListener {
        override fun onPickFileSuccess(file: File) {
            mPathNameTv.text = file.path
        }

        override fun onPickFileFail() {
            Toast.makeText(applicationContext, "Pick file un-success, please try again!", Toast.LENGTH_LONG).show()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mPathNameTv = findViewById(R.id.display_path_tv)
        this.findViewById<View>(R.id.chooser_btn).setOnClickListener(this)
    }

    private fun checkPermission() {
        if (PermissionUtils.hasSelfPermissions(this, *PERMISSION_REQUEST_LIST)) {
            mPermissionGrantedRunnable.run()
        } else {
//            val shouldShow = PermissionUtils.shouldShowRequestPermissionRationale(this, *PERMISSION_REQUEST_LIST)
            PermissionUtils.requestPermission(this, FILE_PERMISSION_REQUEST_CODE, *PERMISSION_REQUEST_LIST)
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.chooser_btn -> checkPermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST_CODE) {
            FileUtils.processAccountDataResult(applicationContext, data, mImageUri, mFilePickListener)
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == FILE_PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.verifyPermissions(permissions, grantResults))
                mPermissionGrantedRunnable.run()
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }
}
