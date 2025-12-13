package com.myxoz.life.viewmodels

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.myxoz.life.notifications.NotificationReaderService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext


class Settings(val prefs: SharedPreferences, context: Context, activity: Activity) {
    val permissions = Permissions(prefs,context, activity)
    val features = Features(prefs, this@Settings.permissions)
    class Features(val prefs: SharedPreferences, permissions: Permissions){
        val stepCounting = Feature(
            STEP_COUNTING,
            "Schritterfassung",
            "Schritterfassung im Kalenderfeed, als auch in der Tageszusammenfassung",
            listOf(permissions.physicalActivity, permissions.postNotifications),
            prefs
        )
        val callFromLife = Feature(
            CALL_FROM_LIFE,
            "Anrufe aus Life",
            "Auf der Profilseite kann nach dem Klick auf \"Anrufen\" direkt angerufen werden, wenn diese Option aus ist," +
                    " wird man in die Telefonapp weitergeleitet, mit der Nummer des Kontaktes bereits eingetippt",
            listOf(permissions.phone),
            prefs
        )
        val syncWithServer = Feature(
            SYNCWITHSERVER,
            "Serversync",
            "Syncronisiert alle Daten mit dem Server, nur für meine private Nutzung",
            listOf(permissions.internet),
            prefs
        )
        val screentime = Feature(
            SCREENTIME,
            "Bildschirmzeiterfassung",
            "Zum erfassen der Bildschirmzeit im Kalenderfeed, auf der Tagesübersichtsseite und bei der Tageszusammenfassung",
            listOf(permissions.usageStats),
            prefs
        )
        val autoDetectSleep = Feature(
            AUTODETECTSLEEP,
            "Schlafenszeiterfassung",
            "Automatische Vorschläge für Schlafenszeiten im Kalender basierend auf Nutzungszeiten",
            listOf(permissions.usageStats),
            prefs,
        )
        val addNewPerson = Feature(
            ADDNEWPERSON,
            "Kontaktvorschläge",
            "Beim Hinzufügen einer neuen Person können die gespeicherten Kontakte durchsucht werden",
            listOf(permissions.contacts),
            prefs
        )
        val readPaymentNotifications = Feature(
            READPAYMENTNOTIFICATIONS,
            "Google Pay im Feed anzeigen",
            "Fängt die Google Wallet Benachrichtigungen ab und zeigt sie im Kalender an",
            listOf(permissions.readNotifications),
            prefs
        )
        class Feature(val spk: String, val name: String, val description: String, val reliesOn: List<Permissions.Permission>, val prefs: SharedPreferences) {
            private val _flow = MutableStateFlow(prefs.getBoolean(spk, false))
            val has = _flow.asStateFlow()
            fun hasAssured() = has.value && reliesOn.all { it.checkEnabled() }
            init {
                if(_flow.value){
                    reliesOn.forEach {
                        it.changeFeature(true, this)
                    }
                }
            }
            fun set(new: Boolean){
                _flow.value = new
                prefs.edit { putBoolean(spk, new) }
                reliesOn.forEach {
                    it.changeFeature(new, this)
                }
//                combine(flows = features.map { it.has })
            }
        }
    }
    class Permissions(val prefs: SharedPreferences, val context: Context, val activity: Activity) {
        val physicalActivity = Permission(context,"Physische Aktivität",{
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        }, {
            openAppInfo()
        }){
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                112
            )
        }
        val internet = Permission(context, "Internet", {
            prefs.getBoolean(INTERNET, false)
        }, {
            prefs.edit { putBoolean(INTERNET, false) }
        }){
            prefs.edit { putBoolean(INTERNET, true) }
        }
        val readNotifications = Permission(context,"Benachrichtigungen lesen", {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val component = ComponentName(context, NotificationReaderService::class.java)
            nm.isNotificationListenerAccessGranted(component)
        }, {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }){
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        val usageStats = Permission(context,"Nutzungszeiten", {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
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
        }){
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .setData("package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        val contacts = Permission(context, "Kontake", {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        }, {
            openAppInfo()
        }){
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_CONTACTS),
                112
            )
        }
        val phone = Permission(context, "Telefon", {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        }, {
            openAppInfo()
        }){
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CALL_PHONE),
                112
            )
        }
        val postNotifications = Permission(context, "Benachrichtigungen", {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        }, {
            openAppInfo()
        }){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    112
                )
            }
        }
        class Permission(val context: Context, val name: String, val check: Permission.()-> Boolean, val onDisable: Permission.()->Unit, val onEnable: Permission.()->Unit){
            private val _flow = MutableStateFlow(check())
            val has = _flow.asStateFlow()
            private val subscribedFeatures = mutableListOf<Features.Feature>()
            private val _isUseless = MutableStateFlow(true)
            val useless = _isUseless.asStateFlow()
            fun changeFeature(active: Boolean, feature: Features.Feature){
                if(active) subscribedFeatures.add(feature) else subscribedFeatures.remove(feature)
                _isUseless.value = subscribedFeatures.isEmpty()
            }
            fun set(new: Boolean){
                if(new){
                    onEnable()
                } else {
                    onDisable()
                    subscribedFeatures.forEach { it.set(false) }
                }
                checkAsync(new){
                    _flow.value = check()
                }
            }
            fun openAppInfo(){
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            }
            fun checkAsync(expect: Boolean, onSuccess: ()-> Unit){
                CoroutineScope(EmptyCoroutineContext).launch {
                    while (true){
                        if (check() == expect){
                            onSuccess()
                            break
                        }
                        delay(200)
                    }
                }
            }

            fun checkEnabled() = has.value && check()
        }
    }
    companion object {
        const val STEP_COUNTING = "step_counting"
        const val SYNCWITHSERVER = "syncwithserver"
        const val SCREENTIME = "screentime"
        const val READPAYMENTNOTIFICATIONS = "readpaymentnotifications"
        const val AUTODETECTSLEEP = "autodetectsleep"
        const val ADDNEWPERSON = "addnewperson"
        const val INTERNET = "internet"
        const val CALL_FROM_LIFE = "callfromlife"
    }
}