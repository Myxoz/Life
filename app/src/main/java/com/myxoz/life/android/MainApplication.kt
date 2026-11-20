package com.myxoz.life.android

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import com.myxoz.life.MainActivity
import com.myxoz.life.SyncEngine
import com.myxoz.life.background.ContainsAlarmScheduler
import com.myxoz.life.background.ContainsLifeRescheduling
import com.myxoz.life.background.PushApplicationState
import com.myxoz.life.background.alarm.AlarmReceiver
import com.myxoz.life.background.alarm.AlarmScheduler
import com.myxoz.life.background.livenotification.LiveRescheduling
import com.myxoz.life.background.permissions.ActualPermissionChecker
import com.myxoz.life.lifecore.settings.AppNavigatable
import com.myxoz.life.lifecore.settings.AppPermissionable
import com.myxoz.life.lifecore.settings.Permission
import com.myxoz.life.storage.DatabaseInterfacable
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.ui.alarm.screens.AlarmActivity
import kotlinx.coroutines.flow.MutableSharedFlow

class MainApplication : Application(),
    AlarmReceiver.AlarmHandler,
    DatabaseInterfacable,
    AppPermissionable,
    AppNavigatable,
    ContainsLifeRescheduling,
    ContainsAlarmScheduler,
    PushApplicationState
{
    // Maybe lazy
    override val dbInterface = DatabaseInterface.by(applicationContext)
    override val channel = MutableSharedFlow<Any?>(
        replay = 0,
        extraBufferCapacity = 0
    )
    override val settingsChecker = Permission.SettingsChecker(
        dbInterface.keyValueStore,
        applicationContext,
        ActualPermissionChecker()
    )
    val syncEngine = SyncEngine(dbInterface.api, settingsChecker)
    override fun suggestResync() {
        syncEngine.suggestResync()
    }
    override fun stopResync() {
        syncEngine.stopResync()
    }

    override val alarmScheduler = AlarmScheduler(dbInterface.aggregators.alarmAggregators)
    override val liveRescheduling = LiveRescheduling(
        applicationContext,
        dbInterface.aggregators.lifeRescheduling,
        Intent(applicationContext, MainActivity::class.java)
    )
    override fun launchAlarmFullScreen() {
        AlarmReceiver.alarmWithIntent(
            applicationContext,
            Intent(applicationContext, AlarmActivity::class.java)
        )
    }

    override fun navigateTo(route: String): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            navigateTo(route)
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}