package com.myxoz.life

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
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
import com.myxoz.life.background.notifications.createNotificationChannels
import com.myxoz.life.background.permissions.ActualServiceController
import com.myxoz.life.feature.socialgraph.SocialGraph
import com.myxoz.life.lifecore.NavPath
import com.myxoz.life.lifecore.settings.CompositionSettings
import com.myxoz.life.lifecore.settings.Feature
import com.myxoz.life.state.ActivityScopedState
import com.myxoz.life.ui.AlarmUI
import com.myxoz.life.ui.LocalScreensProvider
import com.myxoz.life.ui.ModifyLocation
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
import com.myxoz.life.ui.feed.main.HomeComposable
import com.myxoz.life.ui.feed.search.AdvancedSearch
import com.myxoz.life.ui.feed.summarizeday.SummarizeDay
import com.myxoz.life.ui.map.MapBoxMap
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
import com.myxoz.life.lifecore.android.image.ImagePicker
import com.myxoz.life.lifecore.settings.LocalSettings
import com.myxoz.life.lifecore.settings.LocalStore
import com.myxoz.life.lifeui.providers.LocalColors
import com.myxoz.life.lifeui.providers.LocalNavController
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
import com.myxoz.life.utils.rememberTextSelectionColors
import com.myxoz.life.utils.systemColorScheme
import com.myxoz.life.state.ScreenScopedViewModelFactory
import com.myxoz.life.state.screenscoped.ProfileInfoModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private val lifeApplication by lazy { application as MainApplication }
    private val dbInterface by lazy { lifeApplication.dbInterface }
    private val activityScopedState by lazy { ActivityScopedState(dbInterface, this) }
    private val permissionChecker by lazy { lifeApplication.settingsChecker }
    private val settings by lazy { CompositionSettings(
        permissionChecker,
        this,
        ActualServiceController()
    ) }
    private val screenScopedViewModelFactory by lazy {
        ScreenScopedViewModelFactory(lifeApplication.dbInterface, activityScopedState)
    }
    private val imagePicker = ImagePicker(this)
    private var stashedRoute: String? = null
    private var controller: NavController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CoroutineScope(Dispatchers.IO).launch {
            if(settings.hasAssured(Feature.AddNewPerson))
                activityScopedState.contactsRepo.requestRefetchDeviceContacts()
            activityScopedState.calendarRepo.requireAllPeople()
            activityScopedState.calendarRepo.loadRepeatingEvents()
            activityScopedState.largeDataCache.preloadAll(applicationContext)
        }
        createNotificationChannels(applicationContext)
        handleIntent(intent)
        enableEdgeToEdge(SystemBarStyle.dark(0), SystemBarStyle.dark(0))
        setContent {
            val navController = rememberNavController()
            controller = navController
            val colorScheme = systemColorScheme()
            val selectionColors = rememberTextSelectionColors(colorScheme)
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSettings provides settings,
                LocalScreens provides LocalScreensProvider(
                    activityScopedState,
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
                    startDestination = NavPath.Home,
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
                    composable<NavPath.Home> {
                        if(!showHome) return@composable
                        val calendarApplicationState = activityScopedState.calendarApplicationState
                        HomeComposable(activityScopedState.calendarRepo, calendarApplicationState)
                    }
                    composable(
                        NavPath.FULLSCREEN_EVENT.asTemplate,
                        arguments = NavPath.FULLSCREEN_EVENT.asLongArg()
                    ) {
                        val inspectedEventViewModel = viewModel<InspectedEventViewModel>(factory = screenScopedViewModelFactory)
                        FullScreenEvent(inspectedEventViewModel)
                    }
                    composable(NavPath.SUMMARIZE_DAY) {
                        // This is not clean, summarizeDay doesnt need the DayOverviewContent
                        val summarizeViewModel = viewModel<DayOverviewViewModel>(factory = screenScopedViewModelFactory)
                        SummarizeDay(summarizeViewModel)
                    }
                    composable(NavPath.INSTANT_EVENT_SELECTION.asTemplate, NavPath.INSTANT_EVENT_SELECTION.asLongArg()) {
                        val instantEventsViewModel = viewModel<InstantEventsViewModel>(factory = screenScopedViewModelFactory)
                        InstantEventsScreen(instantEventsViewModel)
                    }
                    composable(NavPath.ADVANCED_SEARCH) {
                        AdvancedSearch(activityScopedState.calendarRepo)
                    }

                        //  ---------- Pick -> ExistingLocation ----------
                        composable(NavPath.Pick.LOCATION) {
                            PickExistingLocation(activityScopedState.crossRepoSuper)
                        }

                        //  ---------- FEED -> Location ----------
                        composable(NavPath.MODIFY_LOCATION) {
                            ModifyLocation(locationEditingViewModel)
                        }

                        //  ---------- FEED -> Transaction ----------
                        composable(NavPath.Menu.TRANSACTION_FEED) {
                            TransactionFeed(activityScopedState.transactionFeedRepo)
                        }
                        composable(NavPath.Transaction.DETAILS.asTemplate, NavPath.Transaction.DETAILS.asStringArg()) {
                            val transactionOverviewViewModel  = viewModel<TransactionOverviewViewModel>(factory = screenScopedViewModelFactory)
                            TransactionOverview(transactionOverviewViewModel)
                        }
                        composable(NavPath.Transaction.ME) {
                            MyCard(activityScopedState.largeDataCache, activityScopedState.transactionFeedRepo)
                        }

                        //  ---------- FEED -> DAY_OVERVIEW ----------
                        composable(NavPath.DAY_OVERVIEW.asTemplate, NavPath.DAY_OVERVIEW.asLongArg()) {
                            val overviewModel: DayOverviewViewModel = viewModel(factory = screenScopedViewModelFactory)
                            DayOverviewComposable(overviewModel)
                        }
                        composable(NavPath.DayOverview.SCREENTIME.asTemplate, NavPath.DayOverview.SCREENTIME.asLongArg()) {
                            val screenTimeOverviewModel: ScreenTimeOverviewModel = viewModel(factory = screenScopedViewModelFactory)
                            ScreenTimeOverview(screenTimeOverviewModel)
                        }
                        composable(NavPath.DayOverview.TRANSACTIONS.asTemplate, NavPath.DayOverview.TRANSACTIONS.asLongArg()) {
                            val overviewTransactitonModel: DayOverviewTransactionModel = viewModel(factory = screenScopedViewModelFactory)
                            TransactionList(overviewTransactitonModel)
                        }

                    //  ---------- Menu ----------
                    composable(NavPath.MENU) {
                        MenuComposable()
                    }
                    composable(NavPath.Menu.SOCIAL_GRAPH) {
                        SocialGraph(activityScopedState.socialGraphRepo)
                    }
                    composable(NavPath.Menu.BIRTHDAY_QUIZ) {
                        BirthdayGuesser(activityScopedState.birthdayQuizRepo)
                    }
                    composable(NavPath.Menu.LIFE_WRAPPED) {
                        LifeWrappedScreen(activityScopedState.wrappedActivityViewModel)
                    }
                    composable(
                        NavPath.Menu.MAP,
                        exitTransition = {
                            slideOutHorizontally { it }
                        },
                        enterTransition = {
                            slideInHorizontally { it }
                        },

                        )
                    {
                        MapBoxMap(activityScopedState.mapRepo)
                    }

                        //  ---------- Menu -> TODOS ----------
                        composable(NavPath.Menu.Todo.MAIN) {
                            // TODO
                        }
                        composable(NavPath.Menu.Todo.DETAILS.asTemplate, NavPath.Menu.Todo.DETAILS.asLongArg()) {
                            val todoViewModel = viewModel<TodoViewModel>(factory = screenScopedViewModelFactory)
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
                            val viewModel: FullScreenCommitViewModel = viewModel(factory = screenScopedViewModelFactory)
                            FullScreenCommit(viewModel)
                        }
                        composable(
                            NavPath.Menu.Repos.REPO.asTemplate,
                            arguments = NavPath.Menu.Repos.REPO.asStringArg()
                        ) {
                            val viewModel: FullScreenRepoViewModel = viewModel(factory = screenScopedViewModelFactory)
                            FullScreenRepo(viewModel)
                        }

                        //  ---------- Menu -> Contacts ----------
                        composable(NavPath.Menu.CONTACTS) {
                            Contacts(activityScopedState.contactsRepo)
                        }
                        composable(
                            NavPath.Menu.Contacts.DISPLAY_PERSON.asTemplate,
                            NavPath.Menu.Contacts.DISPLAY_PERSON.asLongArg()
                        ) {
                            val profileInfoModel = viewModel<ProfileInfoModel>(factory = screenScopedViewModelFactory)
                            ProfileFullScreen(imagePicker, profileInfoModel)
                        }
                        composable(NavPath.Menu.Contacts.DEBT_DISPLAY.asTemplate, NavPath.Menu.Contacts.DEBT_DISPLAY.asLongArg()) {
                            val personalDebtViewModel = viewModel<PersonalDebtViewModel>(factory = screenScopedViewModelFactory)
                            FullScreenDebt(personalDebtViewModel)
                        }

                        //  ---------- Menu -> Alarm ----------
                        composable(NavPath.Menu.ALARM) {
                            AlarmUI.AlarmScreen(activityScopedState.alarmRepo)
                        }
                        composable(NavPath.Menu.Alarm.ALARM_SOUND_SETTINGS) {
                            AlarmUI.AlarmSoundSettings(activityScopedState.alarmRepo)
                        }

                        //  ---------- Menu -> Streak ----------
                        composable(NavPath.Menu.STREAK) {
                            StreaksScreen(activityScopedState.streakRepo)
                        }
                        composable(NavPath.Menu.Streak.FULL_SCREEN_STREAK.asTemplate, arguments = listOf(
                            navArgument(NavPath.Menu.Streak.FULL_SCREEN_STREAK.parameterName) { type = NavType.LongType }
                        )){
                            val streakId = it.arguments?.getLong(NavPath.Menu.Streak.FULL_SCREEN_STREAK.parameterName) ?: return@composable
                            StreakFullScreen(activityScopedState.streakRepo, streakId)
                        }
                        composable(NavPath.Menu.Streak.EDIT_SCREEN_STREAK) {
                            EditStreaksScreen(activityScopedState.streakRepo)
                        }

                        //  ---------- Menu -> More ----------
                        composable(NavPath.Menu.MORE) {
                            MoreComposable()
                        }
                        composable(NavPath.Menu.More.INFORMATION) {
                            InformationComposable()
                        }
                        composable(NavPath.Menu.More.AI) {
                            AISettings(activityScopedState.aiSettingsRepo)
                        }
                        composable(NavPath.Menu.More.DEBUG) {
                            DebugScreen(
                                dbInterface.debugInterface,
                                dbInterface.api,
                                dbInterface,
                                activityScopedState
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
        // TODO Handled via channel
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
    override fun onResume() {
        super.onResume()
        lifeApplication.syncEngine.suggestResync()
    }
    override fun onPause() {
        super.onPause()
        lifeApplication.syncEngine.stopResync()
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
    }
}