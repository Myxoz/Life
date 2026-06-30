package com.myxoz.life.ui

import androidx.navigation.NavController
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.extensions.StreakExtendable
import com.myxoz.life.api.syncables.LocationSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.api.syncables.TodoSyncable
import com.myxoz.life.repositories.AppRepos
import com.myxoz.life.storage.interfaces.BankingRepo
import com.myxoz.life.ui.feed.instantevents.InstantEvent
import com.myxoz.life.ui.feed.search.SearchField
import com.myxoz.life.ui.person.displayperson.navigateForResult
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import kotlin.math.min

class LocalScreensProvider(
    val appRepos: AppRepos,
    private val nav: NavController,
) {
    private var calendarCooldown = System.currentTimeMillis()
    fun openPersonDetails(personId: Long){
        nav.navigate(NavPath.Menu.Contacts.DISPLAY_PERSON.with(personId))
    }
    fun setProfileInfoChartScale(scale: Int){
        // TODO
    }
    fun openCalendarWithSearch(applied: SearchField.()->Unit){
        appRepos.calendarRepo.search.openCalendarWithSearch(nav, applied)
    }
    fun openCalendarAt(date: LocalDate, pop: Boolean = true){
        if(System.currentTimeMillis() - calendarCooldown > 2000L) {
            // appRepos.calendarRepo.setDay(date)
            // Todo
            if(pop) {
                nav.popBackStack(NavPath.HOME, false)
            } else {
                nav.navigate(NavPath.HOME)
            }
            calendarCooldown = System.currentTimeMillis()
        }
    }
    fun openSocialGraphWithNodeSelected(personId: Long?, adjustDateRangeToInclude: Long?) {
        appRepos.socialGraphRepo.selectedNode.value = personId
        if(adjustDateRangeToInclude != null) {
            appRepos.socialGraphRepo.chartScale.value = min(
                appRepos.socialGraphRepo.chartScale.value,
                when ((System.currentTimeMillis() - adjustDateRangeToInclude) / (1000L * 3600L * 24L)) {
                    in Int.MIN_VALUE..6 -> 3
                    in 6..29 -> 2
                    in 29..356 -> 1
                    else -> 0
                }
            )
        }
        nav.navigate(NavPath.Menu.SOCIAL_GRAPH)
    }
    fun openFullScreenEvent(event: SyncedEvent) {
        nav.navigate(NavPath.FULLSCREEN_EVENT.with(event.id))
    }
    fun gotoEventDetails() {
        nav.navigate(NavPath.FULLSCREEN_EVENT.with(appRepos.calendarRepo.event.value.id))
    }
    fun openLocation(location: LocationSyncable){
        nav.navigate(NavPath.Menu.MAP.with(location.id))
    }
    fun openTransaction(transaction: BankingRepo.BankingDisplayEntity) {
        nav.navigate(
            NavPath.Transaction.DETAILS
                .with(BankingRepo.BankingDisplayEntity.parcel(transaction.key))
        )
    }

    fun editTransaction(transaction: BankingRepo.BankingDisplayEntity) {
        val sync = transaction.getStoredManualTransactionSyncable() ?: return
        editSyncable(sync)
    }

    fun editSyncable(syncable: Syncable.FeedInstantEventSyncable) {
        appRepos.calendarRepo.setEditedSyncableTo(syncable)
        nav.navigate(NavPath.FULLSCREEN_EVENT.with(syncable.id)) {
            popUpTo(NavPath.HOME)
        }
    }

    fun openCommit(commitSha: String) {
        nav.navigate(NavPath.Menu.Repos.COMMIT.with(commitSha))
    }
    fun openInstantEventRange(instantEvents: List<InstantEvent>){
        val upload = appRepos.transportRepo.instantEvents.upload(instantEvents)
        nav.navigate(NavPath.INSTANT_EVENT_SELECTION.with(upload))
    }

    fun editLocation(location: LocationSyncable) {
        // TODO
        // appRepos .nowEditing = location
        nav.navigate(NavPath.MODIFY_LOCATION)
    }

    fun openDayOverview(date: LocalDate) {
        nav.navigate(NavPath.DAY_OVERVIEW.with(date.toEpochDay()))
    }

    fun fullScreenTodo(todo: TodoSyncable) {
        nav.navigate(NavPath.Menu.Todo.DETAILS.with(todo.id))
    }
    fun getPerson(onResolve: (Long?)->Unit){
        appRepos.contactsRepo.selectMode.value = true
        nav.navigateForResult(
            NavPath.Menu.CONTACTS,
            "person",
            onComplete = { it: Long? ->
                onResolve(it)
                appRepos.contactsRepo.selectMode.value = false
            }
        )
    }

    fun returnContact(id: Long?) {
        nav.previousBackStackEntry?.savedStateHandle?.set("person", id)
        appRepos.contactsRepo.selectMode.value = false
        nav.popBackStack()
    }

    fun openStreak(streak: StreakExtendable.StreakItem) {
        val now = streak.timespan.now()
        appRepos.streakRepo.days.value = (now.minus(20)..now).reversed().toList()
        appRepos.streakRepo.scrollStateSubScreen.requestScrollToItem(0)
        nav.navigate(NavPath.Menu.Streak.FULL_SCREEN_STREAK.with(streak.id))
    }
    fun openStreakEditing(streak: StreakExtendable.StreakItem) {
        // TODO Streak View Model to Streak
        appRepos.streakRepo.editingStreak.update { streak }
        nav.navigate(NavPath.Menu.Streak.EDIT_SCREEN_STREAK)
    }
}