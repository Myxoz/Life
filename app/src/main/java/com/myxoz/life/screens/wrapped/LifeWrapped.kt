@file:OptIn(ExperimentalFoundationApi::class)
package com.myxoz.life.screens.wrapped

import android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
import android.icu.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.myxoz.life.api.API
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.SocialEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.PeopleEvent
import com.myxoz.life.events.additionals.TagEvent
import com.myxoz.life.events.additionals.Vehicle
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.ProfileInfoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

const val PAGELOADDELAY = 300L
const val DAYSINYEAR = 75
@Composable
fun LifeWrappedScreen(db: API.ReadSyncableDaos, profileInfoModel: ProfileInfoModel) {
    var pages by remember {
        mutableStateOf(listOf<WrappedPage>(IntroTitlePage()))
    }
    val contextObject = remember {
        WrappedPage.LifeWrappedCallContext(profileInfoModel)
    }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO){
            val futurePages = mutableListOf<WrappedPage>()
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR) - 1
            val zone = ZoneId.systemDefault()
            val startOfYearDate = LocalDate.ofYearDay(year, 1)
            val endOfYearDate = LocalDate.ofYearDay(year + 1, 1)
            val startOfYear = startOfYearDate.atStartOfDay(zone).toEpochSecond() * 1000L
            val endOfYear = endOfYearDate.atStartOfDay(zone).toEpochSecond() * 1000L
            val allYearEvents = db.eventDetailsDao.getEventsBetween(
                startOfYear,
                endOfYear
            ).mapNotNull {
                SyncedEvent.from(db.eventDetailsDao, it)?.proposed
            }
            futurePages.add(IntroTitlePage())
            futurePages.add(YearPage(year))
            val grouped = allYearEvents.groupBy { it.type }
            val sumed = grouped.map { it.key to it.value.sumOf { e -> e.end - e.start }  }
            val sortedByTime = sumed.sortedByDescending { it.second }
            val favoriteActivity = sortedByTime.getOrNull(0)
            var insertedTagsPage = false
            fun getMostUsedTagList(eventType: EventType): List<MostUsedTagsForFavActivity.Companion.MUTag>?{
                val filteredList = allYearEvents.filter { it.type == eventType }
                if(filteredList[0] !is TagEvent) return null
                val mostUsedTags = mutableMapOf<EventTag, MostUsedTagsForFavActivity.Companion.MUTag>()
                filteredList.forEach {
                    for (tag in (it as? TagEvent)?.eventTags ?: return@forEach) {
                        mostUsedTags[tag] = mostUsedTags
                            .getOrDefault(
                                tag,
                                MostUsedTagsForFavActivity.Companion.MUTag(tag, 0L, 0)
                            )
                            .run { copy(duration = this.duration + it.length(), times = this.times + 1) }
                    }
                }
                return mostUsedTags.values.sortedByDescending { it.duration }
            }
            if(favoriteActivity != null) {
                val favoriteEventType = favoriteActivity.first
                val mostUsedTags = getMostUsedTagList(favoriteEventType)
                futurePages.add(MostPopularEventType(favoriteEventType, favoriteActivity.second, mostUsedTags!=null))
                if(mostUsedTags != null) {
                    futurePages.add(MostUsedTagsForFavActivity(mostUsedTags, true))
                    insertedTagsPage = true
                }
            }
            val secondaryActivity = sortedByTime.getOrNull(1)
            if(secondaryActivity != null) {
                val secondEventType =secondaryActivity.first
                val mostUsedTags = if(insertedTagsPage) null else getMostUsedTagList(secondEventType)
                futurePages.add(SecondFavoritedEventType(secondEventType, secondaryActivity.second, !insertedTagsPage))
                if(mostUsedTags != null) {
                    futurePages.add(MostUsedTagsForFavActivity(mostUsedTags, false))
                }
            }
            val allPeople = db.peopleDao.getAllPeople().map { PersonSyncable.from(db.peopleDao, it) }
            val firstPeopleEvents = db.peopleDao.getFirstEventsFor(allPeople.map { it.id }).mapNotNull { ProposedEvent.from(db.eventDetailsDao, it) as? PeopleEvent }
            val newPeople = allPeople.filter { e -> firstPeopleEvents.find { e.id in it.people }?.let { it is ProposedEvent && it.start >= startOfYear && it.end <= endOfYear } == true }
            futurePages.add(NewSocialContact(newPeople.size))
            val mostInteractedPeople = mutableMapOf<Long, Long>()
            allYearEvents.forEach {
                for (person in (it as? PeopleEvent)?.people ?: return@forEach) {
                    mostInteractedPeople[person] = mostInteractedPeople.getOrDefault(person, 0L) + it.length()
                }
            }
            val mostInteractedPeopleList =
                mostInteractedPeople
                    .mapNotNull { (allPeople.find { p -> p.id == it.key } ?: return@mapNotNull null) to it.value }
                    .sortedByDescending { it.second }
            futurePages.add(TopThreeSocialContacts(
                mostInteractedPeopleList, context
            ))
            val mostInteractedNewPeople = mostInteractedPeople
                    .mapNotNull { (newPeople.find { p -> p.id == it.key } ?: return@mapNotNull null) to it.value }
                    .sortedByDescending { it.second }.getOrNull(0)
            if(mostInteractedNewPeople!=null) futurePages.add(SpecialNewcommer(
                mostInteractedNewPeople.first,
                mostInteractedPeopleList.map{it.first}.indexOf(mostInteractedNewPeople.first) + 1,
                mostInteractedNewPeople.second,
                context
            ))
            val mostTagedSocialEvent = mutableMapOf<EventTag, MostUsedTagsForFavActivity.Companion.MUTag>()
            allYearEvents.forEach {
                for (tag in (it as? SocialEvent)?.eventTags ?: return@forEach) {
                    mostTagedSocialEvent[tag] = mostTagedSocialEvent
                        .getOrDefault(
                            tag,
                            MostUsedTagsForFavActivity.Companion.MUTag(tag, 0L, 0)
                        )
                        .run { copy(duration = this.duration + it.length(), times = this.times + 1) }
                }
            }
            futurePages.add(
                MostUsedTagsForSocialActivity(
                    mostTagedSocialEvent.values.sortedByDescending { it.duration }
                )
            )
            val commits = db.commitsDao.getCommitsForDay(startOfYear, endOfYear)
            val additions = commits.sumOf { it.additions ?: 0 }
            val deletions = commits.sumOf { it.deletions ?: 0 }
            val changes = additions + deletions
            val groupedCommits = commits.groupBy {
                it.repoName
            }.map { it.key to it.value.size }.sortedByDescending { it.second }
            val mostPopularRepo = groupedCommits.getOrNull(0)
            if(mostPopularRepo!=null) {
                futurePages.add(
                    CommitStatsWrapped(
                        commits.size,
                        mostPopularRepo.first,
                        mostPopularRepo.second,
                        additions,
                        deletions,
                        changes
                    )
                )
            }
            val mostUsedVehicles = mutableMapOf<Vehicle, Long>()
            allYearEvents.forEach {
                for (vehicle in (it as? TravelEvent)?.vehicles ?: return@forEach) {
                    mostUsedVehicles[vehicle.type] = mostUsedVehicles.getOrDefault(vehicle.type, 0L) + vehicle.durationMs
                }
            }
            val mostUsedVehicle = mostUsedVehicles.toList().sortedByDescending { it.second }.getOrNull(0)
            if(mostUsedVehicle != null) {
                futurePages.add(
                    TravelingOverview(
                        mostUsedVehicle.first,
                        mostUsedVehicle.second,
                        mostUsedVehicles.map { it.value }.sum()
                    )
                )
            }
            val allDays = db.daysDao.getDaysBetween(startOfYearDate.toEpochDay() - 1, endOfYearDate.toEpochDay()) // Between non inclusive
            val totalSteps = allDays.sumOf { it.steps }
            futurePages.add(
                WalkingStats(
                    totalSteps.toLong(),
                    (totalSteps * .7).toLong(),
                )
            )
            val totalScreenTime = allDays.sumOf { it.screenTimeMs / 1000 }
            val mostUsedApp = db.daysDao.getScreenTimesByRange(startOfYearDate.toEpochDay() - 1, endOfYearDate.toEpochDay())
                .groupBy { it.packagename }
                .map { it.key to it.value.sumOf { it.duration / 1000 } }
                .sortedByDescending { it.second }
                .getOrNull(0)
            if(mostUsedApp!=null) {
                val pm  = context.packageManager
                val appName = pm.getApplicationInfo(mostUsedApp.first, MATCH_UNINSTALLED_PACKAGES).loadLabel(pm).toString()
                futurePages.add(
                    ScreenTimeOverview(
                        totalScreenTime.toLong(),
                        appName,
                        mostUsedApp.second
                    )
                )
            }
            futurePages.add(FinalPage())
            pages = futurePages
        }
    }
    var currentPage by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var pressPos by remember { mutableStateOf(Offset.Unspecified) }
    val pressHeat = remember { Animatable(0f) }  // lingers slightly longer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05060A))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pos ->
                        pressPos = pos
                        scope.launch {
                            pressHeat.animateTo(
                                1f,
                                animationSpec = tween(100, easing = EaseIn)
                            )
                            pressHeat.animateTo(
                                0f,
                                animationSpec = tween(1200, easing = FastOutSlowInEasing)
                            )
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                    }
                ) {
                    val leftHalf = it.x < size.width / 2f
                    if (leftHalf) {
                        val prevColorState = pages[currentPage].previousColorConfig
                        if (prevColorState != null) {
                            contextObject.flow.value = prevColorState
                        }
                        currentPage -= 1
                    } else currentPage += 1
                    currentPage = currentPage.coerceIn(0, pages.size - 1)
                }
            }
    ) {
        val colorContext by contextObject.flow.collectAsState()
        val backgroundAccentColor by animateColorAsState(colorContext.backgroundAccentColor, tween(ANIDURATION*5))
        val ribonAmp by animateFloatAsState(colorContext.ribbonAmp, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessVeryLow))
        val ribbonDetail by animateFloatAsState(colorContext.ribbonDetail, tween(ANIDURATION*2))
        val ribbonSpike by animateFloatAsState(colorContext.ribbonSpike, tween(ANIDURATION*2))
        ReactiveScreensaverBackground(
            pressPos = pressPos,
            pressHeat = pressHeat.value,
            backgroundAccentColor,
            ribbonAmp = ribonAmp,
            ribbonDetail = ribbonDetail,
            ribbonSpike = ribbonSpike
        )
        pages.forEachIndexed { i, page ->
            AnimatedVisibility(
                currentPage == i,
                enter = fadeIn() + slideInVertically { it/10 } + scaleIn(initialScale = .9f),
                exit = fadeOut() + slideOutVertically { it/10 } + scaleOut(targetScale = .9f)
            ) {
                with(page) {
                    with(contextObject) {
                        Content()
                    }
                }
            }
        }
    }
}
