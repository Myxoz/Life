package com.myxoz.life

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myxoz.life.android.MainApplication
import com.myxoz.life.android.notifications.createNotificationChannels
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.ui.AlarmUI
import com.myxoz.life.ui.LocalScreensProvider
import com.myxoz.life.ui.ModifyLocation
import com.myxoz.life.ui.NavPath
import com.myxoz.life.ui.alarm.screens.AlarmScreen
import com.myxoz.life.ui.alarm.screens.AlarmSoundSettings
import com.myxoz.life.ui.feed.commits.FullScreenCommit
import com.myxoz.life.ui.feed.commits.FullScreenCommitViewModel
import com.myxoz.life.ui.feed.commits.FullScreenRepo
import com.myxoz.life.ui.feed.commits.FullScreenRepoViewModel
import com.myxoz.life.ui.feed.commits.FullScreenRepos
import com.myxoz.life.ui.feed.dayoverview.DayOverviewComposable
import com.myxoz.life.ui.feed.dayoverview.DayOverviewTransactionModel
import com.myxoz.life.ui.feed.dayoverview.DayOverviewViewModel
import com.myxoz.life.ui.feed.dayoverview.ScreenTimeOverview
import com.myxoz.life.ui.feed.dayoverview.ScreenTimeOverviewModel
import com.myxoz.life.ui.feed.dayoverview.TransactionList
import com.myxoz.life.ui.feed.fullscreenevent.FullScreenEvent
import com.myxoz.life.ui.feed.fullscreenevent.InspectedEventViewModel
import com.myxoz.life.ui.feed.instantevents.InstantEventsScreen
import com.myxoz.life.ui.feed.instantevents.InstantEventsViewModel
import com.myxoz.life.ui.feed.main.CalendarViewModel
import com.myxoz.life.ui.feed.main.HomeComposable
import com.myxoz.life.ui.feed.search.AdvancedSearch
import com.myxoz.life.ui.feed.summarizeday.SummarizeDay
import com.myxoz.life.ui.map.MapBoxMap
import com.myxoz.life.ui.map.MapViewModel
import com.myxoz.life.ui.options.AISettings
import com.myxoz.life.ui.options.DebugScreen
import com.myxoz.life.ui.options.InformationComposable
import com.myxoz.life.ui.options.MenuComposable
import com.myxoz.life.ui.options.MoreComposable
import com.myxoz.life.ui.options.settings.PreferenceComposable
import com.myxoz.life.ui.options.settings.SettingsComposable
import com.myxoz.life.ui.options.settings.SettingsPermissionComposable
import com.myxoz.life.ui.person.Contacts
import com.myxoz.life.ui.person.FullScreenDebt
import com.myxoz.life.ui.person.PersonalDebtViewModel
import com.myxoz.life.ui.person.SocialGraph
import com.myxoz.life.ui.person.displayperson.PhotoPicker
import com.myxoz.life.ui.person.displayperson.ProfileFullScreen
import com.myxoz.life.ui.pick.PickExistingLocation
import com.myxoz.life.ui.quiz.BirthdayGuesser
import com.myxoz.life.ui.streaks.EditStreaksScreen
import com.myxoz.life.ui.streaks.StreakFullScreen
import com.myxoz.life.ui.streaks.StreaksScreen
import com.myxoz.life.ui.todo.FullScreenTodo
import com.myxoz.life.ui.todo.TodoViewModel
import com.myxoz.life.ui.transactions.MyCard
import com.myxoz.life.ui.transactions.TransactionFeed
import com.myxoz.life.ui.transactions.TransactionOverview
import com.myxoz.life.ui.transactions.TransactionOverviewViewModel
import com.myxoz.life.ui.wrapped.LifeWrappedScreen
import com.myxoz.life.ui.wrapped.WrappedViewModel
import com.myxoz.life.utils.rememberTextSelectionColors
import com.myxoz.life.utils.systemColorScheme
import com.myxoz.life.viewmodels.LocationEditingViewModel
import com.myxoz.life.viewmodels.MainViewModelFactory
import com.myxoz.life.viewmodels.ProfileInfoModel
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences
    private val lifeApplication by lazy { application as MainApplication }
    private val dbInterface by lazy { lifeApplication.dbInterface }
    private val appRepos by lazy { lifeApplication.appRepos }
    private lateinit var settings: Settings.CompositionSettings
    private val factory by lazy{
        MainViewModelFactory(lifeApplication.dbInterface, appRepos)
    }
    private val locationEditingViewModel: LocationEditingViewModel by viewModels { factory }
    private val photoPicker = PhotoPicker(this)
    private var stashedRoute: String? = null
    private var controller: NavController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        prefs = getSharedPreferences(localClassName, MODE_PRIVATE)
        settings = Settings.CompositionSettings(dbInterface.permissionChecker, this)
        CoroutineScope(Dispatchers.IO).launch {
            if(settings.hasAssured(Settings.Feature.AddNewPerson))
                appRepos.contactsRepo.requestRefetchDeviceContacts()
            appRepos.calendarRepo.requireAllPeople()
            appRepos.calendarRepo.loadRepeatingEvents()
            appRepos.largeDataCache.preloadAll(applicationContext)
        }
        createNotificationChannels(applicationContext)
        handleIntent(intent)
        enableEdgeToEdge(SystemBarStyle.dark(0), SystemBarStyle.dark(0))
        setContent {
            val navController = rememberNavController()
            controller = navController
            val colorScheme = systemColorScheme()
            val selectionColors = rememberTextSelectionColors(colorScheme)
            MaterialTheme() { }
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSettings provides settings,
                LocalScreens provides LocalScreensProvider(
                    appRepos,
                    navController
                ),
                LocalColors provides colorScheme,
                LocalTextSelectionColors provides selectionColors
            ) {
                LaunchedEffect(Unit) {
                    intent?.let {
                        val bundle = intent.extras
                        if (bundle != null) {
                            val keys = bundle.keySet()
                            val it = keys.iterator()
                            while (it.hasNext()) {
                                val key = it.next()
                                Log.d("Activity","[" + key + " = " + bundle.get(key) + "]")
                            }
                        }
                    }
                    // Routine checks
                    // if(!settings.features.stepCounting.has.value){
                    //  db.proposedSteps.clearAll() // Not recording is expensive, we just discard all proposedSteps each time
                    // 26.1.2026 There must be a better solution for this
                    // No way! We actually did it 25.03.2026. Reach for the stars, everything will fix itself eventually.
                }
                val navigationTransitionSpec: FiniteAnimationSpec<Float> = remember {
                    tween(250)
                }
                var showHome by remember { mutableStateOf(stashedRoute==null) }
                NavHost(
                    navController = navController,
                    startDestination = NavPath.HOME,
                    modifier = Modifier.fillMaxSize().background(Theme.background),
                    enterTransition = {
                        slideInHorizontally { it/2 } + fadeIn(navigationTransitionSpec)
                    },
                    exitTransition = {
                        slideOutHorizontally { -it/2 } + fadeOut(navigationTransitionSpec)
                    },
                    popEnterTransition = {
                        slideInHorizontally { -it/2 } + fadeIn(navigationTransitionSpec)
                    },
                    popExitTransition = {
                        slideOutHorizontally { it/2 } + fadeOut(navigationTransitionSpec)
                    },
                ) {
                    //  ---------- FEED ----------
                    composable(NavPath.HOME) {
                        if(!showHome) return@composable
                        val calenderViewModel = viewModel<CalendarViewModel>(factory = factory)
                        HomeComposable(appRepos.calendarRepo, calenderViewModel)
                    }
                    composable(
                        NavPath.FULLSCREEN_EVENT.asTemplate,
                        arguments = NavPath.FULLSCREEN_EVENT.asLongArg()
                    ) {
                        val inspectedEventViewModel = viewModel<InspectedEventViewModel>(factory = factory)
                        FullScreenEvent(inspectedEventViewModel)
                    }
                    composable(NavPath.SUMMARIZE_DAY) {
                        // This is not clean, summarizeDay doesnt need the DayOverviewContent
                        val summarizeViewModel = viewModel<DayOverviewViewModel>(factory = factory)
                        SummarizeDay(summarizeViewModel)
                    }
                    composable(NavPath.INSTANT_EVENT_SELECTION.asTemplate, NavPath.INSTANT_EVENT_SELECTION.asLongArg()) {
                        val instantEventsViewModel = viewModel<InstantEventsViewModel>(factory = factory)
                        InstantEventsScreen(instantEventsViewModel)
                    }
                    composable(NavPath.ADVANCED_SEARCH) {
                        AdvancedSearch(appRepos.calendarRepo)
                    }

                        //  ---------- Pick -> ExistingLocation ----------
                        composable(NavPath.Pick.LOCATION) {
                            PickExistingLocation(appRepos.crossRepoSuper)
                        }

                        //  ---------- FEED -> Location ----------
                        composable(NavPath.MODIFY_LOCATION) {
                            ModifyLocation(locationEditingViewModel)
                        }

                        //  ---------- FEED -> Transaction ----------
                        composable(NavPath.Menu.TRANSACTION_FEED) {
                            TransactionFeed(appRepos.transactionFeedRepo)
                        }
                        composable(NavPath.Transaction.DETAILS.asTemplate, NavPath.Transaction.DETAILS.asStringArg()) {
                            val transactionOverviewViewModel  = viewModel<TransactionOverviewViewModel>(factory = factory)
                            TransactionOverview(transactionOverviewViewModel)
                        }
                        composable(NavPath.Transaction.ME) {
                            MyCard(appRepos.largeDataCache, appRepos.transactionFeedRepo)
                        }

                        //  ---------- FEED -> DAY_OVERVIEW ----------
                        composable(NavPath.DAY_OVERVIEW.asTemplate, NavPath.DAY_OVERVIEW.asLongArg()) {
                            // Semantic value: 0 == today, due to pending intent targetRoute, which isn't computable
                            // And jep the resulting bug is that 1.1.1970 always displays the current day in the dayoverview, congrats for finding out
                            val overviewModel: DayOverviewViewModel = viewModel(factory = factory)
                            DayOverviewComposable(overviewModel)
                        }
                        composable(NavPath.DayOverview.SCREENTIME.asTemplate, NavPath.DayOverview.SCREENTIME.asLongArg()) {
                            val screenTimeOverviewModel: ScreenTimeOverviewModel = viewModel(factory = factory)
                            ScreenTimeOverview(screenTimeOverviewModel)
                        }
                        composable(NavPath.DayOverview.TRANSACTIONS.asTemplate, NavPath.DayOverview.TRANSACTIONS.asLongArg()) {
                            val overviewTransactitonModel: DayOverviewTransactionModel = viewModel(factory = factory)
                            TransactionList(overviewTransactitonModel)
                        }

                    //  ---------- Menu ----------
                    composable(NavPath.MENU) {
                        MenuComposable()
                    }
                    composable(NavPath.Menu.SOCIAL_GRAPH) {
                        SocialGraph(appRepos.socialGraphRepo)
                    }
                    composable(NavPath.Menu.BIRTHDAY_QUIZ) {
                        BirthdayGuesser(appRepos.birthdayQuizRepo)
                    }
                    composable(NavPath.Menu.LIFE_WRAPPED) {
                        val wrappedViewModel = viewModel<WrappedViewModel>(factory = factory)
                        LifeWrappedScreen(dbInterface.api.getReadableDaosForWrapped(), wrappedViewModel)
                    }
                    composable(
                        NavPath.Menu.MAP.asTemplate,
                        NavPath.Menu.MAP.asLongActualStringArg(),
                        exitTransition = {
                            slideOutHorizontally { it }
                        },
                        enterTransition = {
                            slideInHorizontally { it }
                        },

                        )
                    {
                        val mapViewModel = viewModel<MapViewModel>(factory = factory)
                        MapBoxMap(mapViewModel)
                    }

                        //  ---------- Menu -> TODOS ----------
                        composable(NavPath.Menu.Todo.MAIN) {
                            // TODO
                        }
                        composable(NavPath.Menu.Todo.DETAILS.asTemplate, NavPath.Menu.Todo.DETAILS.asLongArg()) {
                            val todoViewModel = viewModel<TodoViewModel>(factory = factory)
                            FullScreenTodo(todoViewModel)
                        }

                        //  ---------- Menu -> REPOS ----------
                        composable(NavPath.Menu.REPOS) {
                            FullScreenRepos(dbInterface.commitsInterface)
                        }
                        composable(
                            NavPath.Menu.Repos.COMMIT.asTemplate,
                            NavPath.Menu.Repos.COMMIT.asStringArg()
                        ) {
                            val viewModel: FullScreenCommitViewModel = viewModel(factory = factory)
                            FullScreenCommit(viewModel)
                        }
                        composable(
                            NavPath.Menu.Repos.REPO.asTemplate,
                            arguments = NavPath.Menu.Repos.REPO.asStringArg()
                        ) {
                            val viewModel: FullScreenRepoViewModel = viewModel(factory = factory)
                            FullScreenRepo(viewModel)
                        }

                        //  ---------- Menu -> Contacts ----------
                        composable(NavPath.Menu.CONTACTS) {
                            Contacts(appRepos.contactsRepo)
                        }
                        composable(
                            NavPath.Menu.Contacts.DISPLAY_PERSON.asTemplate,
                            NavPath.Menu.Contacts.DISPLAY_PERSON.asLongArg()
                        ) {
                            val profileInfoModel = viewModel<ProfileInfoModel>(factory = factory)
                            ProfileFullScreen(photoPicker, profileInfoModel)
                        }
                        composable(NavPath.Menu.Contacts.DEBT_DISPLAY.asTemplate, NavPath.Menu.Contacts.DEBT_DISPLAY.asLongArg()) {
                            val personalDebtViewModel = viewModel<PersonalDebtViewModel>(factory = factory)
                            FullScreenDebt(personalDebtViewModel)
                        }

                        //  ---------- Menu -> Alarm ----------
                        composable(NavPath.Menu.ALARM) {
                            AlarmUI.AlarmScreen(appRepos.alarmRepo)
                        }
                        composable(NavPath.Menu.Alarm.ALARM_SOUND_SETTINGS) {
                            AlarmUI.AlarmSoundSettings(appRepos.alarmRepo)
                        }

                        //  ---------- Menu -> Streak ----------
                        composable(NavPath.Menu.STREAK) {
                            StreaksScreen(appRepos.streakRepo)
                        }
                        composable(NavPath.Menu.Streak.FULL_SCREEN_STREAK.asTemplate, arguments = listOf(
                            navArgument(NavPath.Menu.Streak.FULL_SCREEN_STREAK.parameterName) { type = NavType.LongType }
                        )){
                            val streakId = it.arguments?.getLong(NavPath.Menu.Streak.FULL_SCREEN_STREAK.parameterName) ?: return@composable
                            StreakFullScreen(appRepos.streakRepo, streakId)
                        }
                        composable(NavPath.Menu.Streak.EDIT_SCREEN_STREAK) {
                            EditStreaksScreen(appRepos.streakRepo)
                        }

                        //  ---------- Menu -> More ----------
                        composable(NavPath.Menu.MORE) {
                            MoreComposable()
                        }
                        composable(NavPath.Menu.More.INFORMATION) {
                            InformationComposable()
                        }
                        composable(NavPath.Menu.More.AI) {
                            AISettings(appRepos.aiSettingsRepo)
                        }
                        composable(NavPath.Menu.More.DEBUG) {
                            DebugScreen(
                                dbInterface.api.heyAPIAlmighlyGodEtcCanIPleaseOnlyForDebugHaveAllDaoAccessImReallyTheDebugOnlyPleasePleasePlease(),
                                dbInterface.api,
                                dbInterface,
                                appRepos
                            )
                        }

                            //  ---------- Menu -> More -> Settings ----------
                            composable(NavPath.Menu.More.SETTINGS) {
                                SettingsComposable()
                            }
                            composable(NavPath.Menu.More.Settings.PERMISSIONS) {
                                SettingsPermissionComposable()
                            }
                            composable(NavPath.Menu.More.Settings.PREFERENCES) {
                                PreferenceComposable()
                            }
                }
                LaunchedEffect(Unit) {
                    if(stashedRoute!=null) {
                        stashedRoute?.let {
                            navController.navigate(it)
                            stashedRoute = null
                            delay(250)
                            showHome = true
                        }
                    }
                }
            }
        }
    }
    private fun handleIntent(intent: Intent){
        Log.w("Activity", "Might be an old intent")
        handleSharingIntent(intent)
        val route = intent.getStringExtra("targetRoute") ?: return
        val navController = controller
        if(navController!=null) {
            if (route.isNotBlank()) {
                if (navController.currentDestination?.route != route) {
                    navController.navigate(route)
                }
            }
        } else {
            stashedRoute = route
        }
    }
    override fun onNewIntent(intent: Intent) {
        Log.w("Activity","New Intent")
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    private fun handleSharingIntent(intent: Intent) {
        intent.getStringExtra("shared_event")?.let { jsonString ->
            try {
                val event = SyncedEvent.fromJSON(JSONObject(jsonString))
                appRepos.calendarSuper.setInspectedEventTo(
                    if (!event.isSynced()) {
                        if(appRepos.calendarSuper.isEditing.value) {
                            appRepos.calendarSuper.event.value.copy(rawEvent = event.raw)
                        } else {
                            appRepos.calendarSuper.setEditing(true)
                            event
                        }
                    } else {
                        appRepos.calendarSuper.setEditing(false)
                        event
                    }
                )

                // Clear the intent extra to avoid reprocessing
                intent.removeExtra("shared_event")
            } catch (e: Exception) {
                Log.e("Activity", "Failed to parse travel event from intent", e)
            }
        }
    }
    companion object {
        fun restartApp(context: Context) {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val componentName = intent?.component ?: return
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            mainIntent.setPackage(context.packageName)
            context.startActivity(mainIntent)
            exitProcess(0)
        }
        private lateinit var instance: MainActivity
        fun getAppContext(): Context { return instance.applicationContext }
    }
}