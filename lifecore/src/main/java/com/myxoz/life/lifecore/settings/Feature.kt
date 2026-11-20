package com.myxoz.life.lifecore.settings

import com.myxoz.life.storage.DatabaseInterfacable
import com.myxoz.life.storage.stored.SettingsStored

enum class Feature(
    val storedSetting: SettingsStored<Boolean>,
    val reliesOn: List<Permission>,
    val onEnable: (Permission.SettingsChanger.() -> Unit)? = null,
    val onDisable: (Permission.SettingsChanger.() -> Unit)? = null
) {
    AutoDetectPhoneCalls(
        SettingsStored.AutoDetectPhoneCalls,
        listOf(Permission.ReadCallLogs),
    ),
    AutoDetectWhatsAppCalls(
        SettingsStored.AutoDetectWhatsAppCalls,
        listOf(Permission.ReadNotifications)
    ),
    AutoDetectSleep(
        SettingsStored.AutoDetectSleep,
        listOf(Permission.UsageStats)
    ),
    ShowLocationOnMap(
        SettingsStored.ShowLocationOnMap,
        listOf(Permission.Location)
    ),
    StepCounting(
        SettingsStored.StepCounting,
        listOf(Permission.PhysicalActivity, Permission.Notifications),
        {
            serviceController.enable(context, Service.StepCounting)
        },
        {
            serviceController.disable(context, Service.StepCounting)
        },
    ),
    ScreenTime(
        SettingsStored.ScreenTime,
        listOf(Permission.UsageStats)
    ),
    CallFromLife(
        SettingsStored.CallFromLife,
        listOf(Permission.Phone)
    ),
    AddNewPerson(
        SettingsStored.AddNewPerson,
        listOf(Permission.Contacts)
    ),
    ReadPaymentNotifications(
        SettingsStored.ReadPaymentNotifications,
        listOf(Permission.ReadNotifications)
    ),
    LifeAlarmClock(
        SettingsStored.LifeAlarmClock,
        listOf(Permission.AlarmAndTimers, Permission.Notifications)
    ),
    SyncWithServer(
        SettingsStored.SyncWithServer,
        listOf(Permission.Internet),
        {(context.applicationContext as? DatabaseInterfacable)?.suggestResync()},
        {(context.applicationContext as? DatabaseInterfacable)?.stopResync()},
    );
    fun isEnabled(settingsChecker: Permission.SettingsChecker) =
        settingsChecker.store.get(storedSetting.key, false)

    fun allReliesOnEnabled(settingsChecker: Permission.SettingsChecker) =
        reliesOn.all { it.check(settingsChecker) }

    fun enabledAssured(settingsChecker: Permission.SettingsChecker) =
        isEnabled(settingsChecker) && allReliesOnEnabled(settingsChecker)
}
