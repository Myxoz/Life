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
import com.myxoz.life.screens.options.MenuComposable
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
            calendarViewModel.requestAutoDetectedEventStart(settings)
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
                    navController,
                    applicationContext
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
                    startDestination = "home",
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
                    composable("home") {
                        if(!showHome) { return@composable } // Do not render
                        HomeComposable(calendarViewModel, inspectedEventViewModel)
                    }
                    composable("fullscreen_event") {
                        FullScreenEvent(inspectedEventViewModel)
                    }
                    composable("display_person/{personId}", arguments = listOf(
                        navArgument("personId") { type = NavType.LongType }
                    )){
                        val personId = it.arguments?.getLong("personId") ?: return@composable
                        ProfileFullScreen(personId, photoPicker, largeDataCache, profileInfoModel)
                    }
                    composable("modify_event/add_location") {
                        ModifyLocation(locationEditingViewModel)
                    }
                    composable("pick/existing/location") {
                        PickExistingLocation(mapViewModel)
                    }
                    composable("menu") {
                        MenuComposable()
                    }
                    composable("settings") {
                        SettingsComposable()
                    }
                    composable("settings/permissions") {
                        SettingsPermissionComposable(calendarViewModel)
                    }
                    composable("settings/ai") {
                        AISettings(aiSettingsViewModel)
                    }
                    composable("summarize_day") {
                        SummarizeDay(dayOverviewViewModel)
                    }
                    composable("transactions") {
                        TransactionFeed(transactionViewModel)
                    }
                    composable("contacts") {
                        Contacts(contacsViewModel)
                    }
                    composable("day/{epochDay}/overview", arguments = listOf(
                        navArgument("epochDay") { type = NavType.LongType }
                    )) {
                        val epochDay = (it.arguments?.getLong("epochDay") ?: 0).run { if(this == 0L) LocalDate.now().toEpochDay() else this}
                        // Semantic value: 0 == today, due to pending intent targetRoute, which isnt computable
                        DayOverviewComposable(LocalDate.ofEpochDay(epochDay), dayOverviewViewModel)
                    }
                    composable("instant_events_between") {
                        val start = it.arguments?.getLong("start") ?: 0L
                        val end = it.arguments?.getLong("end") ?: 0L
                        InstantEventsScreen(instantEventsViewModel)
                    }
                    composable("day/{epochDay}/screentime", arguments = listOf(
                        navArgument("epochDay") { type = NavType.LongType }
                    )) {
                        val epochDay = it.arguments?.getLong("epochDay") ?: 0
                        ScreenTimeOverview(LocalDate.ofEpochDay(epochDay), dayOverviewViewModel)
                    }
                    composable("day/{epochDay}/transactions", arguments = listOf(
                        navArgument("epochDay") { type = NavType.LongType }
                    )) {
                        val epochDay = it.arguments?.getLong("epochDay") ?: 0
                        TransactionList(LocalDate.ofEpochDay(epochDay), transactionViewModel)
                    }
                    composable("bank/transaction") {
                        TransactionOverview(largeDataCache, transactionViewModel)
                    }
                    composable("bank/me") {
                        MyCard(largeDataCache, transactionViewModel)
                    }
                    composable("information") {
                        DebugScreen(
                            repositories.api.heyAPIAlmighlyGodEtcCanIPleaseOnlyForDebugHaveAllDaoAccessImReallyTheDebugOnlyPleasePleasePlease(),
                            repositories.api
                        )
                    }
                    composable("social_graph") {
                        SocialGraph(socialGraphViewModel)
                    }
                    composable(
                        "map",
                        exitTransition = {
                            slideOutHorizontally { it }
                        },
                        enterTransition = {
                            slideInHorizontally { it }
                        },

                    ) {
                        MapBoxMap(mapViewModel)
                    }
                    composable("advanced_search") {
                        AdvancedSearch(calendarViewModel)
                    }

                    composable("commits/commit/{sha}") {
                        val sha = it.arguments?.getString("sha") ?: return@composable
                        FullScreenCommit(sha, commitsViewModel)
                    }
                    composable("commits/repos") {
                        FullScreenRepos(commitsViewModel)
                    }
                    composable("commits/repo/{name}") {
                        val name = it.arguments?.getString("name") ?: return@composable
                        FullScreenRepo(name, commitsViewModel)
                    }
                    composable("life_wrapped") {
                        LifeWrappedScreen(repositories.api.getReadableDaosForWrapped(), profileInfoModel)
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