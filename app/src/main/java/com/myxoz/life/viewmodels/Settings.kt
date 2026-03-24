package com.myxoz.life.viewmodels

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
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
import com.myxoz.life.android.notifications.NotificationReaderService
import com.myxoz.life.android.sensors.StepsService
import com.myxoz.life.utils.SharedPrefsUtils.get
import com.myxoz.life.utils.SharedPrefsUtils.put
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass


object Settings {
    enum class Feature(val spk: String, val displayName: String, val description: String, val reliesOn: List<Permission>, val onEnable: (Permission.PermissionChanger.() -> Unit)? = null) {
        AutoDetectCalls(
            "autodetectcalls",
            "Anruferfassung",
            "Automatische Vorschläge für Anrufe im Kalender basierend auf deinen vergangenen Telefonaten",
            listOf(Permission.ReadCallLogs),
        ),
        AutoDetectWhatsAppCalls(
            "autodetectwhatsappcalls",
            "WhatsApp-Anruferfassung",
            "Wie ${AutoDetectCalls.displayName}, aber für auch WhatsApp. Diese Einstellung fängt nur die Anruf Benachrichtigungen von \"WhatsApp\" (com.whatsapp) ab. Wenn diese App nicht installiert ist, ist diese Option funktionslos.",
            listOf(Permission.ReadNotifications),
        ),
        AutoDetectSleep(
            "autodetectsleep",
            "Schlafenszeiterfassung",
            "Automatische Vorschläge für Schlafenszeiten im Kalender basierend auf Nutzungszeiten",
            listOf(Permission.UsageStats),
        ),
        MapBoxLocation(
            "mapbox_location",
            "MapBox Standort",
            "Zeige den aktuellen Standort in MapBox an",
            listOf(Permission.Location),
        ),
        StepCounting(
            "step_counting",
            "Schritterfassung",
            "Schritterfassung im Kalenderfeed, als auch in der Tageszusammenfassung, braucht eine " +
                    "Vordergrundbenachrichtigung, um zu funktionieren, deaktiviere wenn dann die Benachrichtigungskategorie, aber nicht generell Benachrichtigungen",
            listOf(Permission.PhysicalActivity, Permission.Notifications),
            {
                Service.StepCounting.rerun(this)
            },
        ),
        ScreenTime(
            "screentime",
            "Bildschirmzeiterfassung",
            "Zum erfassen der Bildschirmzeit im Kalenderfeed, auf der Tagesübersichtsseite und bei der Tageszusammenfassung",
            listOf(Permission.UsageStats),
        ),
        CallFromLife(
            "callfromlife",
            "Anrufe aus Life",
            "Anrufe können direkt aus der App ausgeführt werden, andernfalls" +
                    " wird man in die Telefonapp mit eingetippter Nummer weitergeleitet",
            listOf(Permission.Phone),
        ),
        AddNewPerson(
            "addnewperson",
            "Kontaktvorschläge",
            "In den Kontakten werden über den Life Kontakten zusätzliche Kontakte deines Handys angezigt, " +
                    "zudem wird erkannt, ob Life Kontakte in den Telefonkontakten gespeichert sind und ein Vorschlag zum Einspeichern auf der Profilseite gemacht",
            listOf(Permission.Contacts),
        ),
        ReadPaymentNotifications(
            "readpaymentnotifications",
            "Bargeldlose Bezahlungen im Feed anzeigen",
            "Diese Einstellung fängt nur die Benachrichtigungen von \"Digitales Bezahlen\" (de.fiduciagad.android.wlwallet) ab. Wenn diese App nicht installiert ist, ist diese Option funktionslos",
            listOf(Permission.ReadNotifications),
        ),
        LifeAlarmClock(
            "lifealarmclock",
            "Wecker",
            "Ermöglicht es Wecker zum Aufstehen zu stellen. Dafür muss einerseits ein exakter Alarm gestellt werden (${Permission.AlarmAndTimers.displayName}) als auch eine Vollbildbenachrichtigung (${Permission.Notifications.displayName}) zu dem  Zeitpunkt versendet werden.",
            listOf(Permission.AlarmAndTimers, Permission.Notifications),
        ),
        SyncWithServer(
            "syncwithserver",
            "Serversync",
            "Syncronisiert alle Daten mit dem Server, nur für meine private Nutzung",
            listOf(Permission.Internet),
        );
        fun isEnabled(permissionChecker: Permission.PermissionChecker) = permissionChecker.prefs.getBoolean(spk, false)
        fun hasAssured(permissionChecker: Permission.PermissionChangerInterface) = reliesOn.all { it.check(permissionChecker) }
    }
    enum class Permission(
        val displayName: String,
        val check: PermissionChangerInterface.()-> Boolean,
        val onDisable: PermissionChanger.()->Unit,
        val onEnable: PermissionChanger.()->Unit,
        val onEnabled: (PermissionChanger.()->Unit)? = null
    ) {
        Location("Standort",{
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
        ReadCallLogs("Anrufliste",{
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
        PhysicalActivity("Physische Aktivität",{
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
        Internet("Internet", {
            prefs.getBoolean("internet", false)
        }, {
            prefs.edit { putBoolean("internet", false) }
        },{
            prefs.edit { putBoolean("internet", true) }
        }),
        ReadNotifications("Benachrichtigungen lesen", {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val component = ComponentName(context, NotificationReaderService::class.java)
            nm.isNotificationListenerAccessGranted(component)
        }, {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        },{
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }),
        UsageStats("Nutzungszeiten", {
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
        },{
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    .setData("package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }),
        Contacts("Kontake", {
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
        Phone("Telefon", {
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
        Notifications("Benachrichtigungen", {
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
        AlarmAndTimers("Wecker & Erinnerungen", {
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
        class PermissionChecker(override val prefs: SharedPreferences, override val context: Context): PermissionChangerInterface
        class PermissionChanger(override val prefs: SharedPreferences, override val context: Context, val activity: Activity): PermissionChangerInterface {
            fun openAppInfo(){
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts("package", context.packageName, null)
                context.startActivity(intent)
            }
        }
        interface PermissionChangerInterface{
           val prefs: SharedPreferences
           val context: Context
        }
    }
    enum class Service(val rerun: Permission.PermissionChanger.()->Unit) {
        StepCounting({
            if(Feature.StepCounting.hasAssured(this))
            ContextCompat.startForegroundService(context, Intent(context, StepsService::class.java))
        }),
    }
    class CompositionSettings(val permissionChecker: Permission.PermissionChecker, val activity: Activity) {
        private val features = mutableMapOf<Feature, MutableStateFlow<Boolean>>()
        private val changeContext = Permission.PermissionChanger(permissionChecker.prefs, permissionChecker.context, activity)
        private val permissions = mutableMapOf<Permission, MutableStateFlow<Boolean>>()
        private val dependencies = mutableMapOf<Permission, MutableStateFlow<Set<Feature>>>()
        val preferences = Preferences(permissionChecker.prefs)
        fun has(feature: Feature): MutableStateFlow<Boolean> {
            val cachedFeature = features[feature]
            if(cachedFeature != null) return cachedFeature
            val stored = permissionChecker.prefs.getBoolean(feature.spk, false)
            updateDependency(feature, stored)
            return features.getOrPut(feature) { MutableStateFlow(stored) }
        }

        fun has(permission: Permission): MutableStateFlow<Boolean> {
            val cachedPermission = permissions[permission]
            if(cachedPermission != null) return cachedPermission
            return permissions.getOrPut(permission) {
                MutableStateFlow(permission.check(permissionChecker))
            }
        }

        fun hasAssured(feature: Feature) =
            has(feature).value && feature.hasAssured(permissionChecker)

        fun hasAssured(permission: Permission) = permission.check(permissionChecker)

        fun set(feature: Feature, newVal: Boolean) {
            has(feature).update { newVal }
            updateDependency(feature, newVal)
            if(newVal) feature.onEnable?.invoke(changeContext)
            permissionChecker.prefs.edit { putBoolean(feature.spk, newVal) }
        }

        suspend fun set(permission: Permission, newVal: Boolean) {
            if(newVal) {
                permission.onEnable(changeContext)
            } else {
                permission.onDisable(changeContext)
                Feature.entries.forEach {
                    if(permission in it.reliesOn) set(it, false)
                }
            }
            checkAsync(permission, newVal){
                has(permission).value = newVal
                permission.onEnabled?.invoke(changeContext)
            }
        }

        private fun getDependencies(permission: Permission) =
            dependencies.getOrPut(permission) { MutableStateFlow(setOf()) }

        private fun updateDependency(feature: Feature, active: Boolean) {
            feature.reliesOn.forEach {
                if(active) getDependencies(it).value += feature else getDependencies(it).value -= feature
            }
        }

        /* Cache this value */
        fun isUseless(permission: Permission) = getDependencies(permission).map { it.isEmpty() }
        private suspend fun checkAsync(permission: Permission, expect: Boolean, onSuccess: ()-> Unit){
            while (true){
                if (permission.check(permissionChecker) == expect){
                    onSuccess()
                    break
                }
                delay(200)
            }
        }
        class Preferences(prefs: SharedPreferences) {
            val displayedDaysOptions = Preference(DISPLAYED_DAYS_OPTIONS, prefs, Int::class, 0b10101110)
            class Preference<T: Any>(
                val spk: String,
                val prefs: SharedPreferences,
                val clazz: KClass<T>,
                val defaultValue: T
            ) {
                private val _flow: MutableStateFlow<T> = MutableStateFlow(prefs.get(spk, clazz) ?: defaultValue)
                val flow: StateFlow<T> = _flow
                fun put(new: T){
                    _flow.value = new
                    prefs.edit {
                        put(spk, new, clazz)
                    }
                }
            }
            companion object {
                const val DISPLAYED_DAYS_OPTIONS = "displayed_days_options"
            }
        }
    }
}