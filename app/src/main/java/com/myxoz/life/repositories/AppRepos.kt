package com.myxoz.life.repositories

import com.myxoz.life.repositories.supermodule.CalendarSuper
import com.myxoz.life.repositories.supermodule.CrossRepoSuper
import com.myxoz.life.storage.interfaces.DatabaseInterface
import com.myxoz.life.ui.alarm.AlarmRepo
import com.myxoz.life.ui.feed.CalendarRepo
import com.myxoz.life.ui.transactions.TransactionFeedRepo

class AppRepos(dbInterface: DatabaseInterface) {
    val transportRepo = TransportRepo()
    val calendarSuper = CalendarSuper()
    val crossRepoSuper = CrossRepoSuper(dbInterface)
    val largeDataCache = LargeDataCache()

    val alarmRepo = AlarmRepo(dbInterface)
    val calendarRepo = CalendarRepo(dbInterface, calendarSuper)
    val transactionFeedRepo = TransactionFeedRepo(dbInterface)
    val socialGraphRepo = SocialGraphRepo(dbInterface)
    val birthdayQuizRepo = BirthdayQuizRepo(dbInterface)
    val contactsRepo = ContactsRepo(dbInterface)
    val aiSettingsRepo = AISettingsRepo(dbInterface)
    val streakRepo = StreakRepo(dbInterface)
}