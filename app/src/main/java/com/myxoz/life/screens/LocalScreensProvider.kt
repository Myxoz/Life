package com.myxoz.life.screens

import android.content.Context
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.MotionDurationScale
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.myxoz.life.api.syncables.Location
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.screens.feed.search.SearchField
import com.myxoz.life.screens.map.EARTH_R
import com.myxoz.life.utils.def
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import com.myxoz.life.viewmodels.MapViewModel
import com.myxoz.life.viewmodels.ProfileInfoModel
import com.myxoz.life.viewmodels.SocialGraphViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.min

class LocalScreensProvider(
    private val profileInfoModel: ProfileInfoModel,
    private val calendarViewModel: CalendarViewModel,
    private val socialGraphViewModel: SocialGraphViewModel,
    private val inspectedEventViewModel: InspectedEventViewModel,
    private val mapViewModel: MapViewModel,
    private val nav: NavController,
    private val context: Context
) {
    private var calendarCooldown = System.currentTimeMillis()
    fun openPersonDetails(personId: Long){
        profileInfoModel.openPersonDetails(personId, nav, context)
    }
    fun setProfileInfoChartScale(scale: Int){
        profileInfoModel.chartScale.value = scale
    }
    fun openCalendarWithSearch(applied: SearchField.()->Unit){
        calendarViewModel.search.openCalendarWithSearch(nav, applied)
    }
    fun openCalendarAt(date: LocalDate){
        if(System.currentTimeMillis() - calendarCooldown > calendarViewModel.viewModelScope.coroutineContext[MotionDurationScale]?.scaleFactor.def(1f)*2000L) {
            calendarViewModel.setDay(date)
            nav.popBackStack("home", false)
            calendarCooldown = System.currentTimeMillis()
        }
    }
    fun openSocialGraphWithNodeSelected(personId: Long?, adjustDateRangeToInclude: Long?){
        socialGraphViewModel.selectedNode.value = personId
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
    fun openLocation(location: Location, screenWidthPx: Float){
        nav.navigate("map")
        mapViewModel.isEditing.value = false
        mapViewModel.setSheetLocation(location)
        val targetMetersOnScreen = 2 * location.radiusM.takeIf { it != 0 }.def(10) / .002f // TODO TWEAK
        val metersPerPixel = targetMetersOnScreen / screenWidthPx
        val zoom = ln(EARTH_R * cos(Math.toRadians(location.lat)) / metersPerPixel) / ln(2.0)
        mapViewModel.viewModelScope.launch {
            snapshotFlow { mapViewModel.cameraOptions.cameraState }
                .filterNotNull()
                .drop(1)
                .first()
            mapViewModel.cameraOptions.flyTo(
                cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(location.longitude, location.lat))
                    .zoom(zoom.coerceIn(0.0, 22.0))
                    .bearing(0.0)
                    .pitch(0.0)
                    .build()
            )
        }
    }
}