package com.myxoz.life

import android.R.attr.end
import android.R.attr.type
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.myxoz.life.core.atEndAsMillis
import com.myxoz.life.core.atStartAsMillis
import com.myxoz.life.storage.LocalEvent
import com.myxoz.life.storage.api.extensions.StreakExtendable.DayTargetType
import com.myxoz.life.storage.api.syncables.LocationSyncable
import com.myxoz.life.storage.api.syncables.ServerSyncableCompanion
import com.myxoz.life.storage.api.syncables.TodoSyncable
import com.myxoz.life.storage.convertion.events.PreparedEventContent
import com.myxoz.life.storage.dbwrapper.days.DayScreenTimeEntity
import com.myxoz.life.storage.dbwrapper.events.EventEntity
import com.myxoz.life.storage.dbwrapper.events.WriteEventDetailsDao
import com.myxoz.life.storage.events.DigSocEvent
import com.myxoz.life.storage.events.EmptyEvent
import com.myxoz.life.storage.events.HobbyEvent
import com.myxoz.life.storage.events.LearnEvent
import com.myxoz.life.storage.events.RawEvent
import com.myxoz.life.storage.events.SleepEvent
import com.myxoz.life.storage.events.SocialEvent
import com.myxoz.life.storage.events.SpontEvent
import com.myxoz.life.storage.events.TimewasteEvent
import com.myxoz.life.storage.events.TravelEvent
import com.myxoz.life.storage.events.WorkEvent
import com.myxoz.life.storage.events.additionals.TimedTagLikeContainer
import com.myxoz.life.storage.events.additionals.Vehicle
import com.myxoz.life.storage.interfaces.BankingInterface.MixedBankingEntity
import com.myxoz.life.ui.Chip
import com.myxoz.life.ui.feed.instantevents.InstantEvent
import com.myxoz.life.ui.options.getMappedUsageDataBetween
import com.myxoz.life.ui.theme.EventColors
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.transactions.TransactionFeedRepo
import com.myxoz.life.utils.AndroidUtils
import com.myxoz.life.utils.PhoneNumberParser
import com.myxoz.life.utils.diagrams.PieChart
import com.myxoz.life.utils.formatMsToDuration
import com.myxoz.life.utils.toDp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalQueries.zone
import kotlin.collections.forEach
import kotlin.collections.get
import kotlin.div
import kotlin.math.abs
import kotlin.math.min
import kotlin.text.compareTo
import kotlin.text.get
import kotlin.text.toLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LocalEvent.Render(oneHour: Dp, startOfDay: Long, endOfDay: Long, removeProposedEvent: ()->Unit, syncEvent: ()->Unit){
    Box(
        Modifier
            .padding(top = raw.getTopPadding(oneHour, startOfDay))
            .height(raw.getHeightDp(oneHour, startOfDay, endOfDay))
            .background(raw.type.colors.bg.copy(.5f), RoundedCornerShape(10.dp))
            .fillMaxWidth()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .alpha(.5f)
        ) {
            with(raw) {
                RenderContent(oneHour, startOfDay, endOfDay, false,
                    raw.getBlockHeight(startOfDay, endOfDay)
                )
            }
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(OldColors.ACCEPT)
                    .clip(RoundedCornerShape(10.dp))
                    .rippleClick {
                        syncEvent()
                    }
            )
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(OldColors.DECLINE)
                    .clip(RoundedCornerShape(10.dp))
                    .combinedClickable(onLongClick = {
                        removeProposedEvent()
                    }) {}
            )
        }
    }
}

@Composable
fun getResolvedName(transaction: MixedBankingEntity, transactionFeedRepo: TransactionFeedRepo): State<String?> {
    return if(!transaction.isCashlessPayment) {
        flowOf(null).collectAsState(null)
    } else {
        produceState(null) {
            value = transactionFeedRepo.predictTransaction(transaction)
        }
    }
}
val icon: Int = when{
    digital && cashless -> R.drawable.bank_transfer
    digital && !cashless -> R.drawable.pay_with_card
    !digital && cashless -> R.drawable.wireless_pay
    else -> R.drawable.cash
}


@Composable
override fun ModifyEventInputs(setSyncableTo: (FeedInstantEventSyncable) -> Unit) {
    InputField(short.ifEmpty { null }, "Kategorie/Kurzfassung") {
        setSyncableTo(copy(short = it))
    }
    InputField(details?.ifEmpty { null }, "Beschreibung") {
        setSyncableTo(copy(details = it.nullIfEmpty()))
    }
}
fun timeUntilNextMidnight(): Duration {
    val now = LocalDateTime.now()
    val tomorrow = now.toLocalDate().plusDays(1).atStartOfDay()
    // Very generous 10ms for LocalDate.now() to update. Very likely never ever necessary,
    // but the user doesn't notice anything so we do not care
    return java.time.Duration.between(now, tomorrow).toMillis().milliseconds + 10.milliseconds
}
suspend fun waitUntilNextMidnight(){
    delay(timeUntilNextMidnight())
}

fun matchesWhatsAppNumber(phoneNumber: String): Boolean {
    val whatsapp = socials.find { it.platform == Platform.WhatsApp && it.handle != Platform.WhatsApp.fullName }
    if(whatsapp != null) return PhoneNumberParser.areEqual(whatsapp.handle, phoneNumber)
    return PhoneNumberParser.areEqual(this.phoneNumber ?: return false, phoneNumber)
}

enum class Platform(
    val id: Int,
    val fullName: String,
    val short: String,
    val icon: Int,
    val color: Color,
    val priority: Int,
    private val openApp: PlatformOpener.() -> Unit
) {
    Instagram(
        1,
        "Instagram",
        "insta",
        R.drawable.insta,
        Color(0xFFE1306C),
        0,
        { web("https://www.instagram.com/$handle") }
    ),
    Snapchat(
        2,
        "Snapchat",
        "snap",
        R.drawable.snap,
        Color(0xFFFFFC00),
        1,
        { web("https://www.snapchat.com/add/$handle") }
    ),
    WhatsApp(
        3,
        "WhatsApp",
        "wa",
        R.drawable.whatsapp,
        Color(0xFF25D366),
        9,
        {
            intent(
                ComponentName("com.whatsapp", "com.whatsapp.Conversation"),
                listOf(
                    "jid" to (handle.takeIf { it != WhatsApp.fullName } ?: phoneNumber ?: "").replace(
                        "\\D".toRegex(),
                        ""
                    ) + "@s.whatsapp.net"
                )
            )
        }
    ),
    Youtube(
        4,
        "YouTube",
        "youtube",
        R.drawable.yt,
        Color(0xFFFF0000),
        -1,
        { web("https://youtube.com/@$handle") }
    ),
    Signal(
        5,
        "Signal",
        "signal",
        R.drawable.signal,
        Color(0xFF3b45fd),
        10,
        { web("https://signal.me/#p/$phoneNumber") }
    ),
    Telegram(
        6,
        "Telegram",
        "telegram",
        R.drawable.telegram,
        Color(0xFF2AABEE),
        2,
        { web(if (handle != Telegram.fullName) "https://t.me/$handle" else "https://t.me/$phoneNumber") }
    )
    ;

    fun openPlatform(context: Context, handle: String, phoneNumber: String?) = PlatformOpener(context, handle, phoneNumber).openApp()

    private class PlatformOpener(
        val context: Context,
        val handle: String,
        val phoneNumber: String?
    ) {
        fun web(url: String) {
            AndroidUtils.openLink(context, url)
        }

        fun intent(componentName: ComponentName, extras: List<Pair<String, String>>) {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.component = componentName
            extras.forEach { intent.putExtra(it.first, it.second) }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    companion object {
        fun getById(id: Int): Platform? = Platform.entries.firstOrNull { it.id == id }
            .apply { if (this == null) Log.w("Person","Couldnt find social with id $id in Platforms.getById") }

        fun getByName(name: String): Platform? =
            Platform.entries.firstOrNull { it.short == name }
                .apply { if (this == null) Log.w("Person","Couldnt find social with name $name in Platforms.getById") }
    }
}
val permissionChecker = Settings.Permission.PermissionChecker(settingsPrefs, applicationContext)
val syncEngine = SyncEngine(api, permissionChecker)
fun copy(
    name: String = this.name,
    longitude: Double = this.longitude,
    lat: Double = this.lat,
    radiusM: Int = this.radiusM,
    ssid: String? = this.ssid,
    street: String? = this.street,
    number: String? = this.number,
    city: String? = this.city,
    country: String = this.country
) = LocationSyncable(name, longitude, lat, radiusM, ssid, street, number, city, country, id)

companion object : ServerSyncableCompanion<LocationSyncable> {
    fun coordinatesToString(lat: Double, longitude: Double): String {
        return "%.6f, %.6f (${coordsToDMS(lat, longitude)})".format(lat, longitude)
    }
    fun coordsToDMS(lat: Double, longitude: Double): String {
        fun toDMS(coordinate: Double, isLat: Boolean): String {
            val abs = abs(coordinate)
            val degrees = abs.toInt()
            val minutesFloat = (abs - degrees) * 60
            val minutes = minutesFloat.toInt()
            val seconds = (minutesFloat - minutes) * 60
            val hemisphere = when {
                isLat && coordinate >= 0 -> "N"
                isLat && coordinate < 0 -> "S"
                !isLat && coordinate >= 0 -> "E"
                else -> "W"
            }
            return "%d°%d'%1.1f\"%s".format(degrees, minutes, seconds, hemisphere)
        }

        val latDMS = toDMS(lat, true)
        val lonDMS = toDMS(longitude, false)
        return "$latDMS, $lonDMS"
    }

    fun openInGoogleMaps(context: Context, location: LocationSyncable?) = openInGoogleMaps(
        context,
        location?.toAddress(),
        location?.lat,
        location?.longitude,
        location?.name ?: ""
    )

    fun openInGoogleMaps(
        context: Context,
        address: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        label: String = ""
    ) {
        val uri = when {
            lat != null && lon != null ->
                "geo:$lat,$lon?q=$lat,$lon($label)".toUri()

            address != null ->
                "geo:0,0?q=${Uri.encode(address)}".toUri()

            else -> return
        }

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setPackage("com.google.android.apps.maps")
        context.startActivity(intent)
    }

    fun openRouteInGoogleMaps(
        context: Context,
        fromAddress: String? = null,
        fromLat: Double? = null,
        fromLon: Double? = null,
        toAddress: String? = null,
        toLat: Double? = null,
        toLon: Double? = null,
        mode: String = "transit" // can be "driving", "walking", "transit", "bicycling"
    ) {
        val origin = when {
            fromLat != null && fromLon != null -> "$fromLat,$fromLon"
            !fromAddress.isNullOrBlank() -> Uri.encode(fromAddress)
            else -> "My Location"
        }

        val destination = when {
            toLat != null && toLon != null -> "$toLat,$toLon"
            !toAddress.isNullOrBlank() -> Uri.encode(toAddress)
            else -> return
        }

        val uri = ("https://www.google.com/maps/dir/?api=1" +
                "&origin=$origin" +
                "&destination=$destination" +
                "&travelmode=$mode").toUri()

        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.setPackage("com.google.android.apps.maps")
        context.startActivity(intent)
    }
}
override fun asInstantEvent(): InstantEvent = InstantEvent(
    R.drawable.todos_icon,
    short,
    timestamp,
    { it is TodoSyncable && it.id == id },
) { screens ->
    screens.fullScreenTodo(this)
}
@Composable
override fun BoxScope.RenderContent(
    oneHourDp: Dp,
    startOfDay: Long,
    endOfDay: Long,
    isSmall: Boolean,
    blockHeight: Int
) {
    Row(
        Modifier
            .padding(horizontal = if(isSmall) 2.dp else 5.dp)
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .height(oneHourDp/4*blockHeight.coerceIn(0,2)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if(isSmall) Arrangement.Center else Arrangement.SpaceBetween
    ) {
        if(!isSmall) Icon(
            painterResource(if(blockHeight == 1) R.drawable.sleep_horr else R.drawable.sleep),
            "Sleep",
            Modifier
                .height(((blockHeight*.9f).coerceIn(0f, 2f)*oneHourDp/4)),
            tint = OldColors.SECONDARYFONT
        )
        Text(
            length().formatMsToDuration(true),
            fontSize = ((blockHeight*.9f).coerceIn(0f, if(!isSmall) 2f else 1.25f)*oneHourDp/4).toSp(),
            color = type.colors.textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

fun length() = (end - start).coerceAtLeast(15*1000L*60)
fun getDurationInRange(startOfDay: Long, endOfDay: Long): Long = min(length(), min(endOfDay - start, end - startOfDay))
fun getBlockHeight(startOfDay: Long, endOfDay: Long): Int = (getDurationInRange(startOfDay, endOfDay)/(900*1000)).toInt()
fun getTopPadding(oneHour: Dp, startOfDay: Long) = ((this.start - startOfDay).coerceAtLeast(0L) / (3600 * 1000L).toFloat()) * oneHour + 1.dp
fun getHeightDp(oneHour: Dp, startOfDay: Long, endOfDay: Long) = oneHour*(getBlockHeight(startOfDay, endOfDay)/4f) - 1.dp
fun overlaps(other: DefinedDurationEvent): Boolean = start < other.end && end > other.start
fun getAllStrechedDays(zone: ZoneId): List<LocalDate> {
    val startDate = Instant.ofEpochSecond(start/1000).atZone(zone).toLocalDate()
    val endDate = Instant.ofEpochSecond(end/1000-1).atZone(zone).toLocalDate()

    val dates = mutableListOf<LocalDate>()
    var current = startDate

    while (!current.isAfter(endDate)) {
        dates += current
        current = current.plusDays(1)
    }

    return dates
}
enum class EventType(val id: Int, val colors: EventColors, val selectedColor: Color, val calendarName: String){
    Empty(1, OldColors.Calendar.Empty, Color.White, "DEBUG"),
    Sleep(2, OldColors.Calendar.Sleep, Color.White, "Schlaf"),
    Spont(3, OldColors.Calendar.Spont, Color.Black, "Spontan"),
    Hobby(4, OldColors.Calendar.Hobby, Color.White, "Hobby"),
    Learn(5, OldColors.Calendar.Learn, Color.White, "Lernen"),
    Social(6, OldColors.Calendar.Social, Color.White, "Sozial"),
    Travel(7, OldColors.Calendar.Travel, Color.Black, "Travel"),
    DigSoc(8, OldColors.Calendar.DigSoc, Color.Black, "DigSoc"),
    Work(9, OldColors.Calendar.Work, Color.White, "Arbeit"),
    Timewaste(10, OldColors.Calendar.Timewaste, Color.White, "Timewaste"),
    // ALWAYS INCREMENT ID! AND ADD TO BELOW
    ;
    fun isTagEvent() = this in arrayOf(Hobby, Learn, Spont, Social, Work)
    fun isTitleEvent() = this in arrayOf(Hobby, Learn, Spont, Social, DigSoc, Work, Timewaste)
    fun isDetailsEvent() = this in arrayOf(Hobby, Learn, Work)
    fun isPeopleEvent() = this in arrayOf(DigSoc, Social)
    companion object {
        val order = arrayOf(Sleep, Spont, Hobby, Learn, Travel, Social, DigSoc, Work, Timewaste)
        fun getById(id: Int): EventType? = EventType.entries.firstOrNull { it.id == id }.apply { if(this==null) Log.w("Calendar","Couldnt find calendar with id $id in EventType.getById") }
    }
}
/**
 * Returns true if all entries were made successful, then entering will continue. This method is expected to revert all changes if false is ever returned
 */
abstract suspend fun saveEventSpecifics(writeEventDetailsDao: WriteEventDetailsDao, id: Long): Boolean
@Composable
abstract fun BoxScope.RenderContent(oneHourDp: Dp, startOfDay: Long, endOfDay: Long, isSmall: Boolean, blockHeight: Int)

protected abstract suspend fun eraseEventSpecificsFromDB(db: WriteEventDetailsDao, id: Long)
suspend fun eraseFromDB(db: WriteEventDetailsDao, id: Long) {
    db.removeEventById(id)
    eraseEventSpecificsFromDB(db, id)
}
protected abstract fun addEventSpecifics(jsonObject: JSONObject): JSONObject
fun getBackgroundBrush(alpha: Float = 1f) = if(!uss && !usl) SolidColor(type.colors.bg.copy(alpha)) else Brush.verticalGradient(
    listOf(
        if(uss) Color.Transparent else type.colors.bg.copy(alpha),
        type.colors.bg.copy(alpha),
        if(usl) Color.Transparent else type.colors.bg.copy(alpha)
    )
)
fun toJson(): JSONObject =
    addEventSpecifics(
        JSONObject()
            .put("start", start)
            .put("end", end)
            .put("type", type.id)
            .put("uss", uss)
            .put("usl", usl)
    )
abstract fun copyWithTimes(start: Long, end: Long, uss: Boolean=this.uss, usl: Boolean=this.usl): RawEvent
suspend fun saveToEventTable(writeEventDetailsDao: WriteEventDetailsDao, id: Long, created: Long, edited: Long?){
    writeEventDetailsDao.insertEvent(
        EventEntity(
            id,
            type.id,
            start,
            end,
            created,
            edited,
            uss,
            usl
        )
    )
}
/** Saves to waitingSync, events and all event specific tables returns wether the action was successful (something changed) **/
suspend fun saveEventToDB(writeEventDetailsDao: WriteEventDetailsDao, id: Long, created: Long, edited: Long?): Boolean{
    return if(saveEventSpecifics(writeEventDetailsDao, id)) {
        saveToEventTable(writeEventDetailsDao, id, created, edited)
        true
    } else {
        false
    }
}
abstract fun getInvalidReason(): String?

companion object {
    fun from(pec: PreparedEventContent): RawEvent? {
        return when (pec.event.type) {
            EventType.Sleep.id -> SleepEvent.from(pec.event)
            EventType.Spont.id -> SpontEvent.from(pec.event, pec.spont ?: return null, pec.tags)
            EventType.Hobby.id -> HobbyEvent.from(pec.event, pec.hobby ?: return null, pec.tags)
            EventType.Learn.id -> LearnEvent.from(pec.event, pec.learn ?: return null, pec.tags)
            EventType.Social.id -> SocialEvent.from(pec.event, pec.social ?: return null, pec.people, pec.tags)
            EventType.Travel.id -> TravelEvent.from(pec.event, pec.travel ?: return null, pec.vehicle)
            EventType.DigSoc.id -> DigSocEvent.from(pec.event, pec.digSoc ?: return null, pec.digSocMapping, pec.people)
            EventType.Work.id -> WorkEvent.from(pec.event, pec.work ?: return null, pec.tags)
            EventType.Timewaste.id -> TimewasteEvent.from(pec.event, pec.timewaste ?: return null, pec.twPlatform)
            else -> error("Unknown event loaded from DB $pec")
        }
    }
    fun fromJSON(json: JSONObject): RawEvent {

        return when(type) {
            EventType.Empty -> EmptyEvent(
                start,
                end,
                uss,
                usl
            )
            EventType.Sleep -> SleepEvent.fromJson(json, start, end, uss, usl)
            EventType.Spont -> SpontEvent.fromJson(json, start, end, uss, usl)
            EventType.Hobby -> HobbyEvent.fromJson(json, start, end, uss, usl)
            EventType.Learn -> LearnEvent.fromJson(json, start, end, uss, usl)
            EventType.Social -> SocialEvent.fromJson(json, start, end, uss, usl)
            EventType.Travel -> TravelEvent.fromJson(json, start, end, uss, usl)
            EventType.DigSoc -> DigSocEvent.fromJson(json, start, end, uss, usl)
            EventType.Work -> WorkEvent.fromJson(json, start, end, uss, usl)
            EventType.Timewaste -> TimewasteEvent.fromJson(json, start, end, uss, usl)
        }
    }
}
companion object {
    @Composable
    @OptIn(ExperimentalLayoutApi::class)
    fun <T: com.myxoz.life.storage.events.additionals.TagLike> TimedTagLikeBar(tags: List<TimedTagLikeContainer<T>>, onClick: ((T)->Unit)? = null){
        val size = FontSize.MEDIUM.size.toDp()
        FlowRow {
            tags.forEach {
                Box(
                    Modifier.padding(2.dp)
                    // Due to weirdness with vertical arrangement: last item ommited
                ) {
                    Chip(
                        {
                            onClick?.invoke(it.type)
                        },
                        spacing = 5.dp
                    ) {
                        Icon(
                            painterResource(it.type.drawable),
                            null,
                            Modifier.run {
                                if (it.type == Vehicle.Bus) width(2f * size).height(
                                    size
                                ) else size(size)
                            },
                            Theme.onSecondaryContainer
                        )
                        val m = it.durationMs / 1000 / 60
                        Text(
                            "${if(m >= 60) "${m/60}h " else ""}${(m%60).toString().padStart(if(m>10) 2 else 1, '0')}m",
                            style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM)
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalCoroutinesApi::class)
private val getLifeScreenTime = FlowCache<LocalDate, List<DayScreenTimeEntity>> { date ->
    repos.calendarInterface.todayFlow.flatMapLatest { today ->
        if(date > today) return@flatMapLatest MutableStateFlow(listOf())
        if(today == LocalDate.now()) {
            getScreenTimeOnAsFlow(date, repos.context)
        } else {
            MutableStateFlow(
                getMappedUsageDataBetween(
                    repos.context,
                    date.atStartAsMillis(zone),
                    date.atEndAsMillis(zone)
                )
            )
        }
    }
}
/**  Collect with lifecycle. Else this might emit unnecesarily */
fun getScreenTimeOnDayLive(date: LocalDate) = getLifeScreenTime.get(date)
@OptIn(ExperimentalCoroutinesApi::class)
private val getScreentimeForDayFlow = FlowCache<LocalDate, Long>{ date ->
    repos.daySummaryInterface.getDaySummary(date).flatMapLatest { summary ->
        val summary = summary?.value
        if(summary != null) return@flatMapLatest MutableStateFlow(summary.screenTimeMs.toLong())
        return@flatMapLatest getLifeScreenTime.get(date).map { entry -> entry.sumOf { it.duration } }
    }
}
fun getScreenTimeForDay(date: LocalDate) = getScreentimeForDayFlow.get(date)
fun getScreenTimeOnAsFlow(
    date: LocalDate,
    context: Context,
): Flow<List<DayScreenTimeEntity>> {
    val baseFlow: Flow<List<DayScreenTimeEntity>> = flow {
        while (currentCoroutineContext().isActive) {
            val list = getMappedUsageDataBetween(
                context,
                date.atStartAsMillis(zone),
                if(date == LocalDate.now()) System.currentTimeMillis() else date.atEndAsMillis(zone)
            ).sortedByDescending { it.duration }
            emit(list)
            delay(1000)
        }
    }

    return baseFlow
}
fun getDayPieChart(date: LocalDate) = repos.calendarInterface.eventsForDay(date).map { rawEvents ->
    val total = mutableMapOf<EventType, Long>()
    val startOfDay = date.atStartAsMillis(zone)
    val endOfDay = date.atEndAsMillis(zone)
    val events = rawEvents ?: return@map mapOf()
    events.forEach {
        val duration = it.raw.end.coerceAtMost(endOfDay) - it.raw.start.coerceAtLeast(startOfDay)
        total[it.raw.type] = total[it.raw.type]?.plus(duration) ?: duration
    }
    total.mapValues {
        PieChart.Companion.PieChartPart(it.key.colors.bg, it.value.toDouble())
    }.mapKeys {
        it.key.calendarName
    }
}
@Composable
fun StreakFlameActive(blurRadius: Dp){
    Image(
        painterResource(R.drawable.streak_flame),
        "",
        Modifier
            .blur(
                blurRadius,
                BlurredEdgeTreatment.Unbounded
            )
            .fillMaxSize()
    )
    Image(
        painterResource(R.drawable.streak_flame),
        "",
        Modifier
            .fillMaxSize()
    )
}
override val bgColor = when(type)  {
    DayTargetType.Steps -> OldColors.STEPS
    DayTargetType.Screentime -> OldColors.SCREENTIME
}
override val icon = when(type)  {
    DayTargetType.Steps -> R.drawable.shoe
    DayTargetType.Screentime -> R.drawable.screentime
}
override fun format(current: Long): String = when(type) {
    DayTargetType.Steps -> current.toString().reversed().chunked(3).joinToString(".").reversed()
    DayTargetType.Screentime -> current.formatMsToDuration(true)
}
override val visualTransformation = lazy { if(type == DayTargetType.Screentime) TimeBasedVisualTransformation() else null }
override fun stringToThreshold(enteredThreshold: String): Long =
    when(type){
        DayTargetType.Steps -> {
            enteredThreshold.nullIfEmpty()?.toLong() ?: 0L
        }
        DayTargetType.Screentime -> {
            TimeBasedVisualTransformation.displayMinutesToMinutes(enteredThreshold) * 60 * 1000L
        }
    }

override fun thresholdToString(current: Long): String =
    when(type) {
        DayTargetType.Steps -> current.toString()
        DayTargetType.Screentime -> current.div(60*1000L).formatMinutesToVisual()
    }

override fun getInvalidReason(): String? = null

fun currentStreakValue(events: List<FullDaySyncable>): Long {
    return when(type) {
        DayTargetType.Steps -> events.sumOf { it.steps }.toLong()
        DayTargetType.Screentime -> events.sumOf { it.screenTimeMs.toLong() }
    }
}
override val iconColor = Color.White
fun continuesStreak(value: Long, threshold: Long, atLeast: Boolean): Boolean {
    return if(atLeast) value >= threshold else value < threshold
}

abstract fun format(current: Long): String
abstract val visualTransformation: Lazy<VisualTransformation?>
abstract fun stringToThreshold(enteredThreshold: String): Long
abstract fun thresholdToString(current: Long): String
abstract fun getInvalidReason(): String?

override val visualTransformation = lazy { if(!searchEventAmount) TimeBasedVisualTransformation() else null }
override fun stringToThreshold(enteredThreshold: String): Long =
    if(searchEventAmount)
        enteredThreshold.nullIfEmpty()?.toLong() ?: 0L
    else
        TimeBasedVisualTransformation.displayMinutesToMinutes(enteredThreshold) * 60 * 1000L

override fun thresholdToString(current: Long): String =
    if(searchEventAmount) current.toString() else current.div(60*1000L).formatMinutesToVisual()

override fun getInvalidReason() =
    when{
        type == EventType.Empty -> "Wähle einen Eventyp aus"
        else -> null
    }

override val iconColor = type.colors.tagColors?.ICON ?: type.colors.textColor
override val bgColor = type.colors.bg
override val icon = tags.getOrNull(0)?.drawable ?: R.drawable.streak_icon
override fun format(current: Long): String = if(searchEventAmount) current.toString() else current.formatMsToDuration(true)

fun currentStreakValue(events: List<SyncedEvent>, atLeast: Boolean, start: Long, end: Long): Long {
    fun isMatched(event: SyncedEvent) =
        event.raw.type == type &&
                (title == null || event.raw !is TitleEvent || event.raw.title == title) &&
                (event.raw !is TagEvent || event.raw.eventTags.containsAll(tags)) &&
                (event.raw !is PeopleEvent || event.raw.people.containsAll(people))
    val foundEvents = events
        .filter {
            isMatched(it) && (!searchEventAmount || it.raw.start >= start)
        }
    if(searchEventAmount) return if(atLeast) foundEvents.size.toLong() else foundEvents.size.toLong()
    val eventLength = foundEvents.sumOf { it.raw.getDurationInRange(start, end) }
    return if(atLeast) eventLength else eventLength
}
fun getInvalidReason(): String? =
    when{
        title.isEmpty() -> "Titel darf nicht leer sein"
        else -> target.getInvalidReason()
    }
fun formatThreshold(): String = target.format(threshold)
