package com.myxoz.life.screens

import android.content.Context
import androidx.navigation.NavController
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.screens.feed.search.SearchField
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import com.myxoz.life.viewmodels.ProfileInfoModel
import com.myxoz.life.viewmodels.SocialGraphViewModel
import java.time.LocalDate
import kotlin.math.min

class LocalScreensProvider(
    val profileInfoModel: ProfileInfoModel,
    val calendarViewModel: CalendarViewModel,
    val socialGraphViewModel: SocialGraphViewModel,
    val inspectedEventViewModel: InspectedEventViewModel,
    val nav: NavController,
    val context: Context
) {
    fun openPersonDetails(personId: Long){
        profileInfoModel.openPersonDetails(personId, nav, context)
    }
    fun setProfileInfoChartScale(scale: Int){
        profileInfoModel.chartScale.value = scale
    }
    inline fun openCalendarWithSearch(applied: SearchField.()->Unit){
        calendarViewModel.search.openCalendarWithSearch(nav, applied)
    }
    fun openCalendarAt(date: LocalDate){
        calendarViewModel.setDay(date)
        nav.popBackStack("home", false)
    }
    fun openSocialGraphWithNodeSelected(personId: Long?, adjustDateRangeToInclude: Long?){
        socialGraphViewModel.selectedNode.value = profileInfoModel.id.value
        if(adjustDateRangeToInclude != null) {
            socialGraphViewModel.chartScale.value = min(
                socialGraphViewModel.chartScale.value,
                when ((System.currentTimeMillis() - adjustDateRangeToInclude) / (1000L * 3600L * 24L)) {
                    in Int.MIN_VALUE..6 -> 3
                    in 6..29 -> 2
                    in 29..356 -> 1
                    else -> 0
                }
            )
        }
        nav.navigate("social_graph")
    }
    fun openFullScreenEvent(event: SyncedEvent) {
        inspectedEventViewModel.setInspectedEventTo(event)
        inspectedEventViewModel.popUpToHomeOnEdit.value = true
        nav.navigate("fullscreen_event")
    }
}