package com.example.taindb.pickfile

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.support.v4.app.ActivityCompat
import android.support.v4.app.AppOpsManagerCompat
import android.support.v4.app.Fragment
import android.support.v4.content.PermissionChecker
import android.support.v4.util.SimpleArrayMap


/**
 * Created by Taindb
 * on 5/31/18.
 * taindb@gmail.com
 */
class PermissionUtils {

    constructor() {
        throw IllegalStateException("Cannot instantiate object of utility class")
    }

    companion object {
        private val MIN_SDK_PERMISSIONS: SimpleArrayMap<String, Int> = SimpleArrayMap(8)

        init {
            MIN_SDK_PERMISSIONS.put("com.android.voicemail.permission.ADD_VOICEMAIL", 14)
            MIN_SDK_PERMISSIONS.put("android.permission.BODY_SENSORS", 20)
            MIN_SDK_PERMISSIONS.put("android.permission.READ_CALL_LOG", 16)
            MIN_SDK_PERMISSIONS.put("android.permission.READ_EXTERNAL_STORAGE", 16)
            MIN_SDK_PERMISSIONS.put("android.permission.USE_SIP", 9)
            MIN_SDK_PERMISSIONS.put("android.permission.WRITE_CALL_LOG", 16)
            MIN_SDK_PERMISSIONS.put("android.permission.SYSTEM_ALERT_WINDOW", 23)
            MIN_SDK_PERMISSIONS.put("android.permission.WRITE_SETTINGS", 23)
        }


        /**
         * Checks all given permissions have been granted.
         *
         * @param grantResults results
         * @return returns true if all permissions have been granted.
         */

        fun verifyPermissions(permissions: Array<out String>, grantResults: IntArray): Boolean {
            if (permissions.isEmpty() || grantResults.isEmpty()) return false

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }

            return true
        }

        /**
         * Returns true if the permission exists in this SDK version
         *
         * @param permission permission
         * @return returns true if the permission exists in this SDK version
         */
        private fun permissionExists(permission: String): Boolean {
            // Check if the permission could potentially be missing on this device
            val minVersion = MIN_SDK_PERMISSIONS.get(permission)
            // If null was returned from the above call, there is no need for a device API level check for the permission;
            // otherwise, we check if its minimum API level requirement is met
            return minVersion == null || Build.VERSION.SDK_INT >= minVersion
        }

        /**
         * Returns true if the Activity or Fragment has access to all given permissions.
         *
         * @param context     context
         * @param permissions permission list
         * @return returns true if the Activity or Fragment has access to all given permissions.
         */
        fun hasSelfPermissions(context: Context, vararg permissions: String): Boolean {
            for (permission in permissions) {
                if (permissionExists(permission) && !hasSelfPermission(context, permission)) return false
            }

            return true
        }

        /**
         * Determine context has access to the given permission.
         *
         *
         * This is a workaround for RuntimeException of Parcel#readException.
         * For more detail, check this issue https://github.com/hotchemi/PermissionsDispatcher/issues/107
         *
         * @param context    context
         * @param permission permission
         * @return returns true if context has access to the given permission, false otherwise.
         * @see .hasSelfPermissions
         */
        private fun hasSelfPermission(context: Context, permission: String): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && "Xiaomi".equals(Build.MANUFACTURER, ignoreCase = true)) {
                return hasSelfPermissionForXiaomi(context, permission)
            }
            try {
                return PermissionChecker.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            } catch (t: RuntimeException) {
                return false
            }

        }

        private fun hasSelfPermissionForXiaomi(context: Context, permission: String): Boolean {
            val permissionToOp = AppOpsManagerCompat.permissionToOp(permission) ?: // in case of normal permissions(e.g. INTERNET)
                    return true
            val noteOp = AppOpsManagerCompat.noteOp(context, permissionToOp, Process.myUid(), context.packageName)
            return noteOp == AppOpsManagerCompat.MODE_ALLOWED && PermissionChecker.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Checks given permissions are needed to show rationale.
         *
         * @param activity    activity
         * @param permissions permission list
         * @return returns true if one of the permission is needed to show rationale.
         */
        fun shouldShowRequestPermissionRationale(activity: Activity, vararg permissions: String): Boolean {
            for (permission in permissions) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    return true
                }
            }
            return false
        }

        /**
         * Checks given permissions are needed to show rationale.
         *
         * @param fragment    fragment
         * @param permissions permission list
         * @return returns true if one of the permission is needed to show rationale.
         */
        fun shouldShowRequestPermissionRationale(fragment: Fragment, vararg permissions: String): Boolean {
            for (permission in permissions) {
                if (fragment.shouldShowRequestPermissionRationale(permission)) {
                    return true
                }
            }
            return false
        }

        /**
         * Checks given permissions are needed to show rationale.
         *
         * @param permissions permission list
         * @return returns true if one of the permission is needed to show rationale.
         */
        fun requestPermission(activity: Activity, requestCode: Int, vararg permissions: String) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode)
        }

        /**
         * Requests the provided permissions for a Fragment instance.
         *
         * @param fragment    fragment
         * @param permissions permissions list
         * @param requestCode Request code connected to the permission request
         */
        fun requestPermissions(fragment: Fragment, permissions: Array<String>, requestCode: Int) {
            fragment.activity?.let { ActivityCompat.requestPermissions(it, permissions, requestCode) }
        }
    }

}