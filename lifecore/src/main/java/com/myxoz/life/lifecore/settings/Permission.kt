package com.myxoz.life.lifecore.settings

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.myxoz.life.storage.dbwrapper.state.KeyValueStore
import com.myxoz.life.storage.stored.SettingsStored

enum class Permission(
    val check: SettingsChecker.()-> Boolean,
    val onDisable: SettingsChanger.()->Unit,
    val onEnable: SettingsChanger.()->Unit,
    val onEnabled: (SettingsChanger.()->Unit)? = null
) {
    Location({
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }, {
        openAppInfo()
    },{
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            112
        )
    }),
    ReadCallLogs({
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    }, {
        openAppInfo()
    },{
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_CALL_LOG),
            112
        )
    }),
    PhysicalActivity({
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    }, {
        openAppInfo()
    },{
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
            112
        )
    }),
    Internet({
        store.get(SettingsStored.INTERNETPREFIX, false)
    }, {
        store.put(SettingsStored.INTERNETPREFIX, false)
    },{
        store.put(SettingsStored.INTERNETPREFIX, true)
    }),
    ReadNotifications({
        specificPermissionChecker.isNotificationListenerEnabled(context)
    }, {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    },{
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }),
    UsageStats({
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        if(mode == AppOpsManager.MODE_DEFAULT) {
            context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }, {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .setData("package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    },{
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .setData("package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }),
    Contacts({
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }, {
        openAppInfo()
    },{
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_CONTACTS),
            112
        )
    }),
    Phone({
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }, {
        openAppInfo()
    },{
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CALL_PHONE),
            112
        )
    }),
    Notifications({
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }, {
        openAppInfo()
    },{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                112
            )
        }
    }),
    AlarmAndTimers({
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Automatically granted before Android 12
        }
    }, {
        openAppInfo()
    },{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            activity.startActivity(intent)
        }
    });
    open class SettingsChecker(
        internal val store: KeyValueStore,
        internal val context: Context,
        val specificPermissionChecker: SpecificPermissionChecker,
    )
    class SettingsChanger(
        store: KeyValueStore,
        context: Context,
        specificPermissionChecker: SpecificPermissionChecker,
        val activity: Activity,
        val serviceController: ServiceController,
    ): SettingsChecker(store, context, specificPermissionChecker) {
        fun openAppInfo() {
            val intent = Intent()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(intent)
        }
    }
}