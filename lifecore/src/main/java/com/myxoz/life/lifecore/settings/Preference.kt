package com.myxoz.life.lifecore.settings

import com.myxoz.life.storage.stored.SettingsStored

sealed class Preference<T>(
    val storedSetting: SettingsStored<T>,
    val default: T
) {
    object DisplayedDaysOptions: Preference<Int>(SettingsStored.DisplayedDaysOption, 0b10101110)

    fun getValue(settingsChecker: Permission.SettingsChecker) =
        settingsChecker.store.get(this.storedSetting.key)

    fun asState(settingsChecker: Permission.SettingsChecker) =
        settingsChecker.store.getAsState(this.storedSetting.key)
}