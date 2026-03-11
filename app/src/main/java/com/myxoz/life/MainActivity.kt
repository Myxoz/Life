package com.myxoz.life

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myxoz.life.android.notifications.createNotificationChannels
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.repositories.MainApplication
import com.myxoz.life.screens.LocalScreensProvider
import com.myxoz.life.screens.ModifyLocation
import com.myxoz.life.screens.NavPath
import com.myxoz.life.screens.feed.commits.FullScreenCommit
import com.myxoz.life.screens.feed.commits.FullScreenRepo
import com.myxoz.life.screens.feed.commits.FullScreenRepos
import com.myxoz.life.screens.feed.dayoverview.DayOverviewComposable
import com.myxoz.life.screens.feed.dayoverview.ScreenTimeOverview
import com.myxoz.life.screens.feed.fullscreenevent.FullScreenEvent
import com.myxoz.life.screens.feed.instantevents.InstantEventsScreen
import com.myxoz.life.screens.feed.main.HomeComposable
import com.myxoz.life.screens.feed.search.AdvancedSearch
import com.myxoz.life.screens.feed.summarizeday.SummarizeDay
import com.myxoz.life.screens.map.MapBoxMap
import com.myxoz.life.screens.options.AISettings
import com.myxoz.life.screens.options.DebugScreen
import com.myxoz.life.screens.options.InformationComposable
import com.myxoz.life.screens.options.MenuComposable
import com.myxoz.life.screens.options.MoreComposable
import com.myxoz.life.screens.options.SettingsComposable
import com.myxoz.life.screens.options.SettingsPermissionComposable
import com.myxoz.life.screens.person.Contacts
import com.myxoz.life.screens.person.SocialGraph
import com.myxoz.life.screens.person.displayperson.PhotoPicker
import com.myxoz.life.screens.person.displayperson.ProfileFullScreen
import com.myxoz.life.screens.pick.PickExistingLocation
import com.myxoz.life.screens.transactions.MyCard
import com.myxoz.life.screens.transactions.TransactionFeed
import com.myxoz.life.screens.transactions.TransactionList
import com.myxoz.life.screens.transactions.TransactionOverview
import com.myxoz.life.screens.wrapped.LifeWrappedScreen
import com.myxoz.life.utils.rememberTextSelectionColors
import com.myxoz.life.utils.systemColorScheme
import com.myxoz.life.viewmodels.AISettingsViewModel
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.CommitsViewModel
import com.myxoz.life.viewmodels.ContactsViewModel
import com.myxoz.life.viewmodels.DayOverviewViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import com.myxoz.life.viewmodels.InstantEventsViewModel
import com.myxoz.life.viewmodels.LargeDataCache
import com.myxoz.life.viewmodels.LocationEditingViewModel
import com.myxoz.life.viewmodels.MainViewModelFactory
import com.myxoz.life.viewmodels.MapViewModel
import com.myxoz.life.viewmodels.ProfileInfoModel
import com.myxoz.life.viewmodels.Settings
import com.myxoz.life.viewmodels.SocialGraphViewModel
import com.myxoz.life.viewmodels.TransactionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private lateinit var prefs: SharedPreferences
    private val repositories by lazy {
        (application as MainApplication).repositories
    }
    private lateinit var settings: Settings
    private val factory by lazy{
        MainViewModelFactory(
            (application as MainApplication).repositories,
        )
    }
    private val calendarViewModel: CalendarViewModel by viewModels { factory }
    private val inspectedEventViewModel: InspectedEventViewModel by viewModels { factory }
    private val locationEditingViewModel: LocationEditingViewModel by viewModels { factory }
    private val transactionViewModel: TransactionViewModel by viewModels { factory }
    private val dayOverviewViewModel: DayOverviewViewModel by viewModels { factory }
    private val instantEventsViewModel: InstantEventsViewModel by viewModels { factory }
    private val largeDataCache: LargeDataCache by viewModels{ factory }
    private val profileInfoModel: ProfileInfoModel by viewModels{ factory }
    private val contacsViewModel: ContactsViewModel by viewModels{ factory }
    private val socialGraphViewModel: SocialGraphViewModel by viewModels{ factory }
    private val commitsViewModel: CommitsViewModel by viewModels{ factory }
    private val mapViewModel: MapViewModel by viewModels{ factory }
    private val aiSettingsViewModel: AISettingsViewModel by viewModels { factory }
    private val photoPicker = PhotoPicker()
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            Log.d("ImagePicker", "Selected uri is $selectedImageUri")
            photoPicker.setURI(selectedImageUri?:return@registerForActivityResult, applicationContext)
        }
    }.apply { photoPicker.pickerLauncher = this }
    private var stashedRoute: String? = null
    private var controller: NavController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        prefs = getSharedPreferences(localClassName, MODE_PRIVATE)
        settings = Settings(prefs, applicationContext, this)
        CoroutineScope(Dispatchers.IO).launch {
            if(settings.permissions.contacts.checkEnabled())
                contacsViewModel.requestRefetchDeviceContacts()
            calendarViewModel.requireAllPeople()
            largeDataCache.preloadAll(applicationContext)
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
                    profileInfoModel,
                    calendarViewModel,
                    socialGraphViewModel,
                    inspectedEventViewModel,
                    mapViewModel,
                    transactionViewModel,
                    instantEventsViewModel,
                    locationEditingViewModel,
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
                    // Ignore for now TODO
                    // Routine checks
//                    if(!settings.features.stepCounting.has.value){
//                        db.proposedSteps.clearAll() // Not recording is expensive, we just discard all proposedSteps each time
                        // 26.1.2026 There must be a better solution for this TODO
//                    }
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
                        if(!showHome) { return@composable } // Do not render
                        HomeComposable(calendarViewModel, inspectedEventViewModel)
                    }
                    composable(NavPath.FULLSCREEN_EVENT) {
                        FullScreenEvent(inspectedEventViewModel)
                    }
                    composable(NavPath.SUMMARIZE_DAY) {
                        SummarizeDay(dayOverviewViewModel)
                    }
                    composable(NavPath.INSTANT_EVENT_SELECTION) {
                        InstantEventsScreen(instantEventsViewModel)
                    }
                    composable(NavPath.ADVANCED_SEARCH) {
                        AdvancedSearch(calendarViewModel)
                    }

                        //  ---------- FEED -> Location ----------
                        composable(NavPath.Pick.LOCATION) {
                            PickExistingLocation(mapViewModel)
                        }
                        composable(NavPath.MODIFY_LOCATION) {
                            ModifyLocation(locationEditingViewModel)
                        }

                        //  ---------- FEED -> Transaction ----------
                        composable(NavPath.Menu.TRANSACTION_FEED) {
                            TransactionFeed(transactionViewModel)
                        }
                        composable(NavPath.Transaction.DETAILS) {
                            TransactionOverview(largeDataCache, transactionViewModel)
                        }
                        composable(NavPath.Transaction.ME) {
                            MyCard(largeDataCache, transactionViewModel)
                        }

                        //  ---------- FEED -> DAY_OVERVIEW ----------
                        composable(NavPath.DAY_OVERVIEW.asTemplate, arguments = listOf(
                            navArgument(NavPath.DAY_OVERVIEW.parameterName) { type = NavType.LongType }
                        )) {
                            val epochDay = (it.arguments?.getLong(NavPath.DAY_OVERVIEW.parameterName) ?: 0).run { if(this == 0L) LocalDate.now().toEpochDay() else this}
                            // Semantic value: 0 == today, due to pending intent targetRoute, which isn't computable
                            DayOverviewComposable(LocalDate.ofEpochDay(epochDay), dayOverviewViewModel)
                        }
                        composable(NavPath.DayOverview.SCREENTIME.asTemplate, arguments = listOf(
                            navArgument(NavPath.DayOverview.SCREENTIME.parameterName) { type = NavType.LongType }
                        )) {
                            val epochDay = it.arguments?.getLong(NavPath.DayOverview.SCREENTIME.parameterName) ?: 0
                            ScreenTimeOverview(LocalDate.ofEpochDay(epochDay), dayOverviewViewModel)
                        }
                        composable(NavPath.DayOverview.TRANSACTIONS.asTemplate, arguments = listOf(
                            navArgument(NavPath.DayOverview.TRANSACTIONS.parameterName) { type = NavType.LongType }
                        )) {
                            val epochDay = it.arguments?.getLong(NavPath.DayOverview.TRANSACTIONS.parameterName) ?: 0
                            TransactionList(LocalDate.ofEpochDay(epochDay), transactionViewModel)
                        }

                    //  ---------- Menu ----------
                    composable(NavPath.MENU) {
                        MenuComposable()
                    }
                    composable(NavPath.Menu.SOCIAL_GRAPH) {
                        SocialGraph(socialGraphViewModel)
                    }
                    composable(NavPath.Menu.LIFE_WRAPPED) {
                        LifeWrappedScreen(repositories.api.getReadableDaosForWrapped(), profileInfoModel)
                    }
                    composable(
                        NavPath.Menu.MAP,
                        exitTransition = {
                            slideOutHorizontally { it }
                        },
                        enterTransition = {
                            slideInHorizontally { it }
                        },

                        ) {
                        MapBoxMap(mapViewModel)
                    }

                        //  ---------- Menu -> REPOS ----------
                        composable(NavPath.Menu.REPOS) {
                            FullScreenRepos(commitsViewModel)
                        }
                        composable(NavPath.Menu.Repos.COMMIT.asTemplate) {
                            val sha = it.arguments?.getString(NavPath.Menu.Repos.COMMIT.parameterName) ?: return@composable
                            FullScreenCommit(sha, commitsViewModel)
                        }
                        composable(NavPath.Menu.Repos.REPO.asTemplate) {
                            val name = it.arguments?.getString(NavPath.Menu.Repos.REPO.parameterName) ?: return@composable
                            FullScreenRepo(name, commitsViewModel)
                        }

                        //  ---------- Menu -> Contacts ----------
                        composable(NavPath.Menu.CONTACTS) {
                            Contacts(contacsViewModel)
                        }
                        composable(NavPath.Menu.Contacts.DISPLAY_PERSON.asTemplate, arguments = listOf(
                            navArgument(NavPath.Menu.Contacts.DISPLAY_PERSON.parameterName) { type = NavType.LongType }
                        )){
                            val personId = it.arguments?.getLong(NavPath.Menu.Contacts.DISPLAY_PERSON.parameterName) ?: return@composable
                            ProfileFullScreen(personId, photoPicker, largeDataCache, profileInfoModel)
                        }

                        //  ---------- Menu -> More ----------
                        composable(NavPath.Menu.MORE) {
                            MoreComposable()
                        }
                        composable(NavPath.Menu.More.INFORMATION) {
                            InformationComposable()
                        }
                        composable(NavPath.Menu.More.AI) {
                            AISettings(aiSettingsViewModel)
                        }
                        composable(NavPath.Menu.More.DEBUG) {
                            DebugScreen(
                                repositories.api.heyAPIAlmighlyGodEtcCanIPleaseOnlyForDebugHaveAllDaoAccessImReallyTheDebugOnlyPleasePleasePlease(),
                                repositories.api
                            )
                        }

                            //  ---------- Menu -> More -> Settings ----------
                            composable(NavPath.Menu.More.SETTINGS) {
                                SettingsComposable()
                            }
                            composable(NavPath.Menu.More.Settings.PERMISSIONS) {
                                SettingsPermissionComposable(calendarViewModel)
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
        intent.getStringExtra("shared_travel_event")?.let { jsonString ->
            try {
                val travelEvent = ProposedEvent.getProposedEventByJson(JSONObject(jsonString))
                if(travelEvent !is TravelEvent) return
                inspectedEventViewModel.setInspectedEventTo(
                    if(inspectedEventViewModel.isEditing.value) {
                        inspectedEventViewModel.event.value.copy(proposedEvent = travelEvent)
                    } else {
                        inspectedEventViewModel.setEditing(true)
                        SyncedEvent(-1L, 0L, null, travelEvent)
                    }
                )

                // Clear the intent extra to avoid reprocessing
                intent.removeExtra("shared_travel_event")
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