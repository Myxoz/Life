package com.myxoz.life.screens.wrapped

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.EventType
import com.myxoz.life.events.additionals.Vehicle
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyleOld
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.def
import com.myxoz.life.utils.formatMToDistance
import com.myxoz.life.utils.plural
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toShape
import com.myxoz.life.viewmodels.ProfileInfoModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

const val ANIDURATION = 700

abstract class WrappedPage {
    var previousColorConfig: LifeWrappedColorContext? = null
    @Composable
    abstract fun LifeWrappedCallContext.Content()
    class LifeWrappedCallContext(val profileInfoModel: ProfileInfoModel) {
        var flow = MutableStateFlow(
            LifeWrappedColorContext(
                OldColors.Calendar.Hobby.BG,
                2f,
                0.35f,
                0f
            )
        )
        fun WrappedPage.setColorTo(newContext: LifeWrappedColorContext){
            if(this.previousColorConfig == null) { this.previousColorConfig = flow.value }
            flow.value = newContext
        }
    }
    data class LifeWrappedColorContext(
        val backgroundAccentColor: Color,
        val ribbonAmp: Float,
        val ribbonDetail: Float,
        val ribbonSpike: Float
    )
}

class IntroTitlePage : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            WavyBigTitle("LIFE WRAPPED", maxOffset = 5.dp)
            Spacer(Modifier.height(10.dp))
            SmallTag("The wrapped that knows you too well")
        }
    }
}

class YearPage(val year: Int) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer()
            Reveal(step >= 0) { SubtleLine("You did a lot in") }
            Reveal(step >= 1) {
                RollingLong(
                    year.toLong(),
                    1,
                    1970
                )
            }
            Reveal(step >= 11) { SubtleLine("But your favorite activity seams to be...") }
        }
    }
}

class MostPopularEventType(val type: EventType, val duration: Long, val followedByTagsPage: Boolean) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer(24)
            Reveal(step >= 1) { SubtleLine("Your favorite activity was ${if(type == EventType.Sleep) "(luckily)" else ""}") }
            Reveal(step >= 4, {
                setColorTo(
                    flow.value.copy(backgroundAccentColor = type.color, ribbonAmp = 0.5f)
                )
            }) {
                BigWord(type.calendarName)
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 7) { SubtleLine("With a whopping" ) }
            Reveal(step >= 9) {
                RollingLong(
                    duration,
                    9,
                ){
                    "${it/1000/3600}h"
                }
            }
            val time = (duration / DAYSINYEAR / 60000).let { "${it/60}h ${it % 60}m"}
            Reveal(step >= 19) { NoticableFact(if(type != EventType.Sleep) "Thats $time per day!" else "With an average of $time per night" ) }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 23) { SubtleLine(if(type != EventType.Sleep) "Even more than sleeping!" else if(followedByTagsPage) "Let's take a look what you were doing all this time" else "But there has been something else bugging you..." ) }
        }
    }
}

class SecondFavoritedEventType(val type: EventType, val duration: Long, val followedByTagsPage: Boolean) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer()
            Reveal(step >= 1) { SubtleLine("Your second most loved activity seams to be") }
            Reveal(step >= 4, {
                setColorTo(
                    flow.value.copy(backgroundAccentColor = type.color, ribbonAmp = 2f)
                )
            }) {
                BigWord(type.calendarName)
            }
            Spacer(Modifier.height(20.dp))
            val time = (duration / DAYSINYEAR / 60000).let { "${it/60}h ${it % 60}m"}
            Reveal(step >= 7) { SubtleLine("With another $time per day or" ) }
            Reveal(step >= 8) {
                RollingLong(
                    duration,
                    8,
                ){
                    "${it/1000/3600}h"
                }
            }
            Reveal(step >= 18) { SubtleLine( if(followedByTagsPage) "Let's take a look what you were doing all this time" else "But now, let's talk social" ) }
        }
    }
}

class MostUsedTagsForFavActivity(val tags: List<MUTag>, val isFirst: Boolean) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer()
            Reveal(step >= 1) { SubtleLine("For this, your most used tag is") }
            val first = tags.getOrNull(0)
            Spacer(Modifier.height(10.dp))
            Reveal(step >= 4) {
                WavyBigTitle(first?.tag?.displayName?:"Keine", maxOffset = 5.dp)
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 7) { NoticableFact("Used ${first?.times} time${first?.times.plural("s")}, accumulating" ) }
            Reveal(step >= 10) {
                RollingLong(
                    (first?.duration.def(0)/1000/3600),
                    10,
                ) {
                    "${it}h"
                }
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 20) { SubtleLine( if(isFirst) "Let's take a look on your second most loved activity" else "But now, let's talk social" ) }
        }
    }
    companion object {
        data class MUTag(val tag: EventTag, val duration: Long, val times: Int)
    }
}

class NewSocialContact(val amount: Int) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer()
            LaunchedEffect(Unit) {
                setColorTo(
                    flow.value.copy(
                        backgroundAccentColor = OldColors.Calendar.Social.BG,
                        ribbonDetail = 0.5f, ribbonSpike = 3f
                    )
                )
            }
            Reveal(step >= 1) { SubtleLine("You met a lot of new people this year, amounting to") }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 4) {
                RollingLong(
                    amount.toLong(),
                    4,
                )
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 14) { SubtleLine("people you've never met before") }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 17) { SubtleLine("Let's focus on your Top 3...") }
        }
    }
}

class TopThreeSocialContacts(val people: List<Pair<PersonSyncable, Long>>, context: Context) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer(50)
            Reveal(step >= 1) {
                BigTitle("Top 3")
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 4) { SubtleLine("These hold a special place in your heart") }
            Spacer(Modifier.height(20.dp))
            val conf = LocalConfiguration.current
            val screenDim = min(conf.screenWidthDp, conf.screenHeightDp)
            val pbSize = screenDim * .9f
            Box(
                Modifier
                    .size(pbSize.dp)
            ) {
                val rotationAnimation = rememberInfiniteTransition()
                val progress by rotationAnimation.animateFloat(0f, (PI*2).toFloat(), infiniteRepeatable(tween(ANIDURATION*30, easing = LinearEasing)))
                people.take(3).forEachIndexed { rindex, person ->
                    val index = 2-rindex
                    val sizeMod = (1f - rindex/10.toFloat())
                    val revealed = step > 5 + 10*index
                    Box(
                        Modifier
                            .size((pbSize*.35 * sizeMod).dp)
                            .offset(
                                x = (pbSize / 2 + -sin(progress + index * Math.PI * 2 / 3) * 0.325 * pbSize - 0.175 * pbSize * sizeMod).dp,
                                y = (pbSize / 2 + cos(progress + index * Math.PI * 2 / 3) * 0.325 * pbSize - 0.175 * pbSize * sizeMod).dp,
                            )
                    ) {
                        val rawBitmap by profileInfoModel.getProfilePicture(person.first.id).collectAsState(null)
                        rawBitmap?.let { bitmap ->
                            val revealPogress by animateFloatAsState(if(step > 10 + 10*index) 1f else 0f,  tween(ANIDURATION*5, easing = LinearEasing))
                            Image(
                                remember(bitmap) { bitmap.asImageBitmap() },
                                "Profilepicture",
                                Modifier
                                    .fillMaxSize()
                                    .blur(radius = (1-revealPogress) * 25.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                    .clip(MaterialShapes.Cookie9Sided.toShape())
                            )
                        } ?: Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(OldColors.BACKGROUND, MaterialShapes.Cookie9Sided.toShape())
                            )

                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = FontSize.MEDIUM.size.toDp()),
                        ) {
                            Reveal(step > 10 + index * 10) {
                                Text(
                                    person.first.name,
                                    style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM)
                                )
                            }
                        }
                        Text(
                            "${rindex + 1}.",
                            Modifier
                                .offset(x = (-5).dp, y = (-5).dp),
                            style = TypoStyleOld(
                                FontColor.PRIMARY,
                                FontSize.DISPLAY,
                                FontFamily.Display
                            )
                        )
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Reveal(
                                revealed
                            ) {
                                RollingLong(
                                    (person.second / 1000 / 3600),
                                    5 + 10 * index,
                                    fontSize = FontSize.XLARGE.size
                                ) {
                                    "${it}h"
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 17) { SubtleLine("And there was one special newcommer...") }
        }
    }
}

class SpecialNewcommer(val person: PersonSyncable, val ranking: Int, val time: Long, context: Context): WrappedPage(){
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer()
            Reveal(step >= 1) { SubtleLine("One person entered your life just this year") }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 3) { SubtleLine((if (ranking <= 3) "And already made it into your Top 3" else "But your Top 3 is not disrupted")) }
            Spacer(Modifier.height(20.dp))
            val conf = LocalConfiguration.current
            val screenDim = min(conf.screenWidthDp, conf.screenHeightDp)
            val pbSize = screenDim * .5f
            Reveal(
                step >= 8
            ) {
                Box(
                    Modifier
                        .size(pbSize.dp)
                ) {
                    val pp by profileInfoModel.getProfilePicture(person.id).collectAsState(null)
                    pp?.let { bitmap ->
                        Image(
                            remember(bitmap) { bitmap.asImageBitmap() },
                            "Profilepicture",
                            Modifier
                                .fillMaxSize()
                                .blur(radius = 0.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                .clip(MaterialShapes.Cookie9Sided.toShape())
                        )
                    } ?: Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                OldColors.BACKGROUND,
                                MaterialShapes.Cookie9Sided.toShape()
                            )
                    )
                    Text(
                        "${ranking}.",
                        Modifier
                            .offset(x = (-5).dp, y = (-5).dp),
                        style = TypoStyleOld(
                            FontColor.PRIMARY,
                            FontSize.DISPLAY,
                            FontFamily.Display
                        )
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        Reveal(
                            step > 6
                        ) {
                            RollingLong(
                                (time / 1000 / 3600),
                                6,
                                fontSize = FontSize.XLARGE.size
                            ) {
                                "${it}h"
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 16) { SubtleLine("But what did you do most with your friends?") }
        }
    }
}
class MostUsedTagsForSocialActivity(val tags: List<MostUsedTagsForFavActivity.Companion.MUTag>) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer()
            Reveal(step >= 1) { SubtleLine("The activity you enjoyed together the most") }
            val first = tags.getOrNull(0)
            Spacer(Modifier.height(10.dp))
            Reveal(step >= 4) {
                WavyBigTitle(first?.tag?.displayName?:"Keine", maxOffset = 5.dp)
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 7) { NoticableFact("With a total time spend of" ) }
            Reveal(step >= 10) {
                RollingLong(
                    (first?.duration.def(0)/1000/3600),
                    10,
                ) {
                    "${it}h"
                }
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 20) { SubtleLine( "But what about your Github performance?" ) }
        }
    }
}

class CommitStatsWrapped(val commits: Int, val heavilyCommitedRepo: String, val heavilyCommited: Int, val additions: Int, val deletions: Int, val changes: Int) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer(35)
            Reveal(step >= 1) { SubtleLine("You commited a total of") }
            Reveal(step >= 4, {
                setColorTo(flow.value.copy(
                    backgroundAccentColor = OldColors.GITHUBBG,
                    ribbonDetail = 0.5f,
                    ribbonSpike = 1f,
                    ribbonAmp = 1f,

                ))
            }) {
                RollingLong(
                    commits.toLong(),
                    4,
                )
            }
            Spacer(Modifier.height(10.dp))
            Reveal(step >= 14) { NoticableFact("commits, mostly to $heavilyCommitedRepo with $heavilyCommited commit${heavilyCommited.plural("s")}" ) }
            Spacer(Modifier.height(10.dp))
            Reveal(step >= 17) { SubtleLine("Your additions accumulated to" ) }
            Reveal(step >= 20) {
                RollingLong(
                    additions.toLong(),
                    20,
                )
            }
            Spacer(Modifier.height(10.dp))
            Reveal(step >= 30) { SubtleLine( "lines with $deletions deletion${deletions.plural("s")} and $changes chang${deletions.plural("es")}" ) }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 34) { SubtleLine( "Next, let's look at traveling" ) }
        }
    }
}

class TravelingOverview(val type: Vehicle, val duration: Long, val totalDuration: Long) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer()
            Reveal(step >= 1) { SubtleLine("Your total time spend traveling accumulated to") }
            Reveal(step >= 4, {
                setColorTo(
                    flow.value.copy(backgroundAccentColor = OldColors.Calendar.Travel.BG, ribbonAmp = 2f)
                )
            }) {
                RollingLong(
                    totalDuration,
                    4,
                ){
                    "${it/1000/3600}h"
                }
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 14) { NoticableFact("Your favorite vehicle is ${type.name} with an accumulated duration of" ) }
            Reveal(step >= 16) {
                RollingLong(
                    duration,
                    16,
                ){
                    "${it/1000/3600}h"
                }
            }
            Reveal(step >= 16) { SubtleLine( "Let's take a look at your wakling stats" ) }
        }
    }
}

class WalkingStats(val totalSteps: Long, val totalDistanceM: Long) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer(30)
            Reveal(step >= 1) { SubtleLine("You walked a total of") }
            Reveal(step >= 4, {
                setColorTo(
                    flow.value.copy(backgroundAccentColor = OldColors.STEPS, ribbonDetail = 1f)
                )
            }) {
                RollingLong(
                    totalSteps,
                    4,
                )
            }
            Reveal(step >= 12) { SubtleLine("steps. That ${totalSteps/DAYSINYEAR} steps/day" ) }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 14) { SubtleLine("With a total distance of" ) }
            Reveal(step >= 17, {
                setColorTo(
                    flow.value.copy(backgroundAccentColor = OldColors.STEPS, ribbonDetail = 1f)
                )
            }) {
                RollingLong(
                    totalDistanceM,
                    17,
                ) {
                    it.formatMToDistance()
                }
            }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 29) { SubtleLine( "And lastly, screentime" ) }
        }
    }
}

class ScreenTimeOverview(val totalScreenTime: Long, val mostUsedApp: String, val mostUsedAppAcc: Long) : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            val step = rememberStepSequencer(30)
            Reveal(step >= 1) { SubtleLine("In the last year, you used your phone") }
            Reveal(step >= 4, {
                setColorTo(
                    flow.value.copy(backgroundAccentColor = OldColors.SCREENTIME)
                )
            }) {
                RollingLong(
                    (totalScreenTime / 3600),
                    4,
                ) {
                    "${it}h"
                }
            }
            Reveal(step >= 14) { NoticableFact("That's ${(totalScreenTime / DAYSINYEAR / 60).let { "${it/60}h ${it % 60}m"}}/day" ) }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 17) { SubtleLine("Your most used app is" ) }
            Spacer(Modifier.height(10.dp))
            Reveal(step >= 20) {
                WavyBigTitle(mostUsedApp, maxOffset = 5.dp)
            }
            Spacer(Modifier.height(10.dp))
            Reveal(step >= 25) { SubtleLine("With ${(mostUsedAppAcc / DAYSINYEAR / 60).let { "${it/60}h ${it % 60}m"}}/day" ) }
            Spacer(Modifier.height(20.dp))
            Reveal(step >= 29) { SubtleLine( "Touch some grass" ) }
        }
    }
}

class FinalPage : WrappedPage() {
    @Composable
    override fun LifeWrappedCallContext.Content() {
        ScreenScaffold {
            LaunchedEffect(Unit) {
                setColorTo(flow.value.copy(backgroundAccentColor = OldColors.SELECTED))
            }
            SubtleLine("This has been")
            Spacer(Modifier.height(10.dp))
            WavyBigTitle("LIFE WRAPPED", maxOffset = 5.dp)
            Spacer(Modifier.height(10.dp))
            SubtleLine("Go and have fun")
            Spacer(Modifier.height(10.dp))
            SmallTag("#LifeWrapped")
        }
    }
}

@Composable
private fun ScreenScaffold(
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
        ,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(.95f)
            ,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable private fun BigTitle(text: String) {
    Text(text = text, style = TypoStyleOld(FontColor.PRIMARY, FontSize.DISPLAY, FontFamily.Display), textAlign = TextAlign.Center)
}

@Composable private fun SectionTitle(text: String) {
    Text(text = text, style = TypoStyleOld(FontColor.PRIMARY, FontSize.MLARGE), textAlign = TextAlign.Center)
}

@Composable private fun BigWord(text: String) {
    Text(text = text, style = TypoStyleOld(FontColor.PRIMARY, FontSize.DISPLAY, FontFamily.Display).copy(fontSize = 70.sp), textAlign = TextAlign.Center)
}

@Composable private fun WavyBigTitle(text: String, maxOffset: Dp = 10.dp, speedScale: Float = 1f) {
    val repeatable = rememberInfiniteTransition()
    val offset by repeatable.animateFloat(
        0f,
        2* PI.toFloat(),
        infiniteRepeatable(
            tween((ANIDURATION * 10 * speedScale).roundToInt(), easing = LinearEasing)
        )
    )
    Row {
        text.forEachIndexed { i, c ->
            Text(
                c.toString(),
                Modifier
                    .offset(y = sin(offset + 0.5f * i) * maxOffset)
                ,
                style = TypoStyleOld(FontColor.PRIMARY, FontSize.DISPLAY, FontFamily.Display),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable private fun SmallTag(text: String) {
    Text(text = text, style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUMM), textAlign = TextAlign.Center)
}

@Composable private fun SubtleLine(text: String) {
    Text(
        text = text,
        style = TypoStyleOld(FontColor.SECONDARY, FontSize.MEDIUMM),
        textAlign = TextAlign.Center
    )
}

@Composable private fun NoticableFact(text: String) {
    Text(
        text = text,
        style = TypoStyleOld(FontColor.PRIMARY, FontSize.MEDIUM),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Reveal(
    visible: Boolean,
    onReveal: ()->Unit = {},
    content: @Composable () -> Unit
) {
    val progress by animateFloatAsState(
        if(visible) 1f else 0f,
        animationSpec = tween(ANIDURATION)
    )
    LaunchedEffect(visible) {
        if(visible) onReveal()
    }
    Box(
        Modifier
            .alpha(progress)
            .offset(y = 10.dp * (1 - progress))
            .scale(progress * .1f + 0.9f)
    ) {
        content()
    }
}

@Composable
private fun rememberStepSequencer(steps: Int = 21): Int {
    var step by remember { mutableIntStateOf(-1) }
    LaunchedEffect("stepsequencer") {
        val scale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f // [page:2]
        step = -1
        delay((PAGELOADDELAY * scale).roundToLong())
        for (i in 0 until steps) {
            step = i
            delay((ANIDURATION * scale).roundToLong())
        }
    }
    return step
}

@Composable
private fun RollingLong(
    target: Long,
    delay: Int,
    startAt: Int = 0,
    fontSize: TextUnit = 70.sp,
    formatter: (Int) -> String = { it.toString() },
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target) {
        progress.snapTo(0f)
        progress.animateTo(
            1f,
            animationSpec = tween(ANIDURATION*10, delay*ANIDURATION + PAGELOADDELAY.toInt(), easing = EaseOutQuad)  // 10*step
        )
    }
    val v = (startAt + (target - startAt) * progress.value).roundToInt()
    Text(
        text = formatter(v),
        style = TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = OldColors.PRIMARYFONT,
            letterSpacing = (-0.4).sp,
            fontFamily = FontFamily.Display.family
        )
    )
}