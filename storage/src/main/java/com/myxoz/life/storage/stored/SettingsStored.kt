package com.myxoz.life.storage.stored

import com.myxoz.life.storage.dbwrapper.state.KeyValueStoreKey
import com.myxoz.life.storage.dbwrapper.state.KeyValueStorePrefix
import com.myxoz.life.storage.dbwrapper.state.KeyValueType

sealed class SettingsStored<T>(
    val key: KeyValueStoreKey<T>
) {
    companion object {
        private val PERMISSIONPREFIX = KeyValueStorePrefix("permission")
        val INTERNETPREFIX = PERMISSIONPREFIX.keyOfType("internet", KeyValueType.BooleanType)
        private val FEATUREPREFIX = KeyValueStorePrefix("features")
    }
    object AutoDetectPhoneCalls: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("autodetectphonecalls", KeyValueType.BooleanType))
    object AutoDetectWhatsAppCalls: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("autodetectwhatsappcalls", KeyValueType.BooleanType))
    object AutoDetectSleep: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("autodetectsleep", KeyValueType.BooleanType))
    object ShowLocationOnMap: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("showlocationonmap", KeyValueType.BooleanType))
    object StepCounting: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("stepcounting", KeyValueType.BooleanType))
    object ScreenTime: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("screentime", KeyValueType.BooleanType))
    object CallFromLife: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("callfromlife", KeyValueType.BooleanType))
    object AddNewPerson: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("addnewperson", KeyValueType.BooleanType))
    object ReadPaymentNotifications: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("readpaymentnotifications", KeyValueType.BooleanType))
    object LifeAlarmClock: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("lifealarmclock", KeyValueType.BooleanType))
    object SyncWithServer: SettingsStored<Boolean>(FEATUREPREFIX.keyOfType("syncwithserver", KeyValueType.BooleanType))
    object DisplayedDaysOption: SettingsStored<Int>(FEATUREPREFIX.keyOfType("displayeddaysoption", KeyValueType.IntType))
}