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
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myxoz.life.android.notifications.createNotificationChannels
import com.myxoz.life.api.API
import com.myxoz.life.api.jsonObjArray
import com.myxoz.life.api.syncables.BankingSidecar
import com.myxoz.life.api.syncables.SyncedEvent
import com.myxoz.life.dbwrapper.DatabaseProvider
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.events.ProposedEvent
import com.myxoz.life.events.TravelEvent
import com.myxoz.life.screens.LocalScreensProvider
import com.myxoz.life.screens.map.MapBoxMap
import com.myxoz.life.screens.ModifyLocation
import com.myxoz.life.screens.feed.commits.FullScreenCommit
import com.myxoz.life.screens.feed.commits.FullScreenRepo
import com.myxoz.life.screens.feed.commits.FullScreenRepos
import com.myxoz.life.screens.feed.dayoverview.DayOverviewComposable
import com.myxoz.life.screens.feed.dayoverview.ScreenTimeOverview
import com.myxoz.life.screens.feed.fullscreenevent.FullScreenEvent
import com.myxoz.life.screens.feed.main.HomeComposable
import com.myxoz.life.screens.feed.search.AdvancedSearch
import com.myxoz.life.screens.options.DebugScreen
import com.myxoz.life.screens.options.SettingsComposable
import com.myxoz.life.screens.options.SettingsPermissionComposable
import com.myxoz.life.screens.options.SummarizeDay
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
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.ContactsViewModel
import com.myxoz.life.viewmodels.InspectedEventViewModel
import com.myxoz.life.viewmodels.LargeDataCache
import com.myxoz.life.viewmodels.MapViewModel
import com.myxoz.life.viewmodels.ProfileInfoModel
import com.myxoz.life.viewmodels.Settings
import com.myxoz.life.viewmodels.SocialGraphViewModel
import com.myxoz.life.viewmodels.TransactionFeedViewModel
import com.myxoz.life.viewmodels.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private lateinit var db: StorageManager
    private lateinit var prefs: SharedPreferences
    private lateinit var api: API
    private lateinit var settings: Settings
    private val calendarViewModel: CalendarViewModel by viewModel{
        CalendarViewModel(settings, db)
    }
    private val inspectedEventViewModel by viewModel{ InspectedEventViewModel() }
    private val largeDataCache by viewModel{ LargeDataCache() }
    private val profileInfoModel by viewModel{ ProfileInfoModel(db) }
    private val contacsViewModel by viewModel{ ContactsViewModel(db) }
    private val transactionFeedViewModel by viewModel{ TransactionFeedViewModel(db, ZoneId.systemDefault()) }
    private val socialGraphViewModel by viewModel { SocialGraphViewModel(db) }
    private val mapViewModel by viewModel { MapViewModel(prefs) }
    private val photoPicker = PhotoPicker()
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = result.data?.data
            println("Selected uri is $selectedImageUri")
            photoPicker.setURI(selectedImageUri?:return@registerForActivityResult, applicationContext)
        }
    }.apply { photoPicker.pickerLauncher = this }
    private var stashedRoute: String? = null
    private var controller: NavController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        prefs = getSharedPreferences(localClassName, MODE_PRIVATE)
        db = StorageManager(DatabaseProvider.getDatabase(applicationContext), prefs)
        settings = Settings(prefs, applicationContext, this)
        CoroutineScope(Dispatchers.IO).launch {
            largeDataCache.preloadAll(applicationContext)
            if(settings.features.addNewPerson.has.value) contacsViewModel.fetchDeviceContacts(applicationContext)
            contacsViewModel.refetchLifeContacts()
        }
        api = API(applicationContext, db, prefs, settings.features.syncWithServer)
        createNotificationChannels(applicationContext)
        handleIntent(intent)
        enableEdgeToEdge(SystemBarStyle.dark(0), SystemBarStyle.dark(0))
        setContent {
            val navController = rememberNavController()
            controller = navController
            CompositionLocalProvider(
                LocalAPI provides api,
                LocalNavController provides navController,
                LocalStorage provides db,
                LocalSettings provides settings,
                LocalScreens provides LocalScreensProvider(
                    profileInfoModel,
                    calendarViewModel,
                    socialGraphViewModel,
                    inspectedEventViewModel,
                    navController,
                    applicationContext
                )
            ) {
                LaunchedEffect(Unit) {
                    intent?.let {
                        val bundle = intent.extras
                        if (bundle != null) {
                            val keys = bundle.keySet()
                            val it = keys.iterator()
                            while (it.hasNext()) {
                                val key = it.next()
                                println("[" + key + " = " + bundle.get(key) + "]")
                            }
                        }
                    }
                    // Routine checks
                    if(!settings.features.stepCounting.has.value){
                        db.proposedSteps.clearAll() // Not recording is expensive, we just discard all proposedSteps each time
                    }
                    withContext(Dispatchers.IO){
                        val payments = prefs.getString("payments", null)?:"[]"
                        val json = JSONArray(payments).jsonObjArray.toMutableList()
                        for(payment in json.toList()){
                            val date = payment.getLong("timestamp")
                            val amount = payment.getInt("amount")
                            val entries = db.banking.findPossibleMobileTransaction(
                                date,
                                date + 7*24*60*1000L*60 /* 7 Werktage, ab dann nicht mehr zuordbar */,
                                -amount
                            )
                            if(entries.size != 1) {
                                println("Trying to create banking sidecar for \n${payment.toString(2)}\n was unsuccessful due to ${entries.size} possible entries:\n$entries\n")
                                continue
                            }
                            json.remove(payment)
                            val entry = entries[0]
                            BankingSidecar(
                                API.generateId(),
                                entry.id,
                                payment.getString("to"),
                                date
                            ).saveAndSync(db)
                            println("Successfully created sidecar for ${entry.id}\n${payment.toString(2)}\n")
                        }
                        prefs.edit {
                            putString("payments", JSONArray().apply { json.forEach { put(it) } }.toString())
                        }
                    }
                }
                val navigationTransitionSpec: FiniteAnimationSpec<Float> = remember {
                    tween(250)
                }
                var showHome by remember { mutableStateOf(stashedRoute==null) }
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize().background(Colors.BACKGROUND),
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
                    /*
                    composable("event/{eventId}", arguments = listOf(
                        navArgument("eventId") { type = NavType.LongType }
                    )) {
                        val eventId = it.arguments?.getLong("eventId") ?: error("Needs eventsId to work")
                        DisplayEvent()
                    }
                    */
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
                        ModifyLocation()
                    }
                    composable("pick/existing/location") {
                        PickExistingLocation()
                    }
                    composable("settings") {
                        SettingsComposable()
                    }
                    composable("settings/permissions") {
                        SettingsPermissionComposable()
                    }
                    composable("summarize_day") {
                        SummarizeDay()
                    }
                    composable("transactions") {
                        TransactionFeed(transactionFeedViewModel)
                    }
                    composable("contacts") {
                        Contacts(contacsViewModel)
                    }
                    composable("day/{epochDay}/overview", arguments = listOf(
                        navArgument("epochDay") { type = NavType.LongType }
                    )) {
                        val epochDay = (it.arguments?.getLong("epochDay") ?: 0).run { if(this == 0L) LocalDate.now().toEpochDay() else this}
                        // Semantic value: 0 == today, due to pending intent targetRoute, which isnt computable
                        DayOverviewComposable(navController, epochDay)
                    }
                    composable("day/{epochDay}/screentime", arguments = listOf(
                        navArgument("epochDay") { type = NavType.LongType }
                    )) {
                        val epochDay = it.arguments?.getLong("epochDay") ?: 0
                        ScreenTimeOverview(epochDay)
                    }
                    composable("day/{epochDay}/transactions", arguments = listOf(
                        navArgument("epochDay") { type = NavType.LongType }
                    )) {
                        val epochDay = it.arguments?.getLong("epochDay") ?: 0
                        TransactionList(epochDay)
                    }
                    composable("bank/transaction/{id}", arguments = listOf(
                        navArgument("id") { type = NavType.StringType }
                    )) {
                        val transactionId = it.arguments?.getString("id") ?: return@composable
                        TransactionOverview(transactionId, largeDataCache)
                    }
                    composable("bank/me") {
                        MyCard(largeDataCache)
                    }
                    composable("information") {
                        DebugScreen()
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
                        FullScreenCommit(sha)
                    }
                    composable("commits/repos") {
                        FullScreenRepos()
                    }
                    composable("commits/repo/{name}") {
                        val name = it.arguments?.getString("name") ?: return@composable
                        FullScreenRepo(name)
                    }
                    composable("life_wrapped") {
                        LifeWrappedScreen()
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
        println("Maybe old intent")
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
        println("New Intent")
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    private fun handleSharingIntent(intent: Intent) {
        intent.getStringExtra("shared_travel_event")?.let { jsonString ->
            try {
                val travelEvent = ProposedEvent.getProposedEventByJson(JSONObject(jsonString))
                if(travelEvent == null || travelEvent !is TravelEvent) return
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
                Log.e("MainActivity", "Failed to parse travel event from intent", e)
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