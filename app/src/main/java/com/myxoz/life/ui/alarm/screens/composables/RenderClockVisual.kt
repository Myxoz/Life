package com.myxoz.life.ui.alarm.screens.composables

import android.icu.util.Calendar
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.myxoz.life.Theme
import com.myxoz.life.ui.getMaxTabletScreenWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.atEndAsMillis
import com.myxoz.life.utils.atStartAsMillis
import com.myxoz.life.utils.diagrams.chartBasedLongAnimation
import com.myxoz.life.utils.formatDayTime
import com.myxoz.life.utils.toLocalDate
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val LongToAnimationVector = TwoWayConverter<Long, AnimationVector1D>(
    convertToVector = { AnimationVector1D(it.toFloat()) },
    convertFromVector = { it.value.toLong() }
)
@Composable
fun com.myxoz.life.ui.AlarmUI.RenderClockVisual(alarmRepo: com.myxoz.life.ui.alarm.AlarmRepo, eventStart: Long) {
    val boxSize = getMaxTabletScreenWidth()
    val now by alarmRepo.minuteFlow.collectAsState()
    val getReadyMinutes by alarmRepo.minutesToGetReady.collectAsState()
    val zone = remember { ZoneId.systemDefault() }

    // Yes this can be 23 or 25, maybe I'll fix this later TODO optional
    val hoursInDay = ((now.toLocalDate(zone).atEndAsMillis(zone) - now.toLocalDate(zone).atStartAsMillis(zone)) / (3600 * 1000)).toInt()
    val anglesInDay = (hoursInDay*1000f*3600/360f)
    val nowAngle = remember(now) { (now - now.toLocalDate(zone).atStartAsMillis(zone)) / anglesInDay - 90f }

    val wakeUpTarget = eventStart - getReadyMinutes * 60_000L
    val wakeUpAnim = remember {
        Animatable(
            System.currentTimeMillis() + 10_000L,
            LongToAnimationVector
        )
    }
    LaunchedEffect(wakeUpTarget) {
        wakeUpAnim.animateTo( wakeUpTarget, chartBasedLongAnimation)
    }
    val animatedWakeUp = wakeUpAnim.value

    val rotationAngle = remember(animatedWakeUp, now) {
        val goal = (animatedWakeUp - now) / anglesInDay
        // This might happen if you go to bed before midnight (so a really unlikely edgecase for me)
        if (animatedWakeUp < nowAngle) goal + 360f else goal
    }

    val calendar = remember { Calendar.getInstance() }
    val primaryFill = Theme.primary
    val secondaryFill = Theme.primaryContainer
    val clockDotColor = Theme.outlineVariant
    val secondaryText = Theme.outline
    val dotColor = Theme.background
    val textMeasurer = rememberTextMeasurer(hoursInDay)
    val onPrimaryContainer = Theme.onPrimaryContainer
    val messuredNowText = remember { textMeasurer.measure("Jetzt", TypoStyle(onPrimaryContainer, FontSize.SMALLM)) }
    val onPrimary = Theme.onPrimary
    val messuredWakeUpTimeText = remember(animatedWakeUp) {
        textMeasurer.measure(animatedWakeUp.formatDayTime(calendar), TypoStyle(onPrimary, FontSize.MEDIUM, FontFamily.Display))
    }
    Box(
        Modifier
            .width(boxSize)
            .aspectRatio(1f)
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val diameter = size.width - max(messuredWakeUpTimeText.size.width, messuredNowText.size.width) - size.width / 2.5f
            val offset = (size.width - diameter) / 2
            drawArc(
                secondaryFill,
                rotationAngle + nowAngle,
                360f - rotationAngle,
                false,
                Offset(offset, offset),
                Size(diameter, diameter),
                style = Stroke(size.width / 60f, cap = StrokeCap.Round)
            )
            drawArc(
                primaryFill,
                nowAngle,
                rotationAngle,
                false,
                Offset(offset, offset),
                Size(diameter, diameter),
                style = Stroke(size.width / 30f, cap = StrokeCap.Round)
            )
            val radius = diameter / 2f
            fun rad(angle: Float) = Math.toRadians(angle.toDouble()).toFloat()
            fun drawRotatedDot(angle: Float){
                drawCircle(
                    dotColor,
                    size.width / 90f,
                    Offset(
                        x = center.x + cos(rad(angle)) * radius,
                        y = center.y + sin(rad(angle)) * radius
                    )
                )
            }
            drawRotatedDot(nowAngle)
            drawRotatedDot(rotationAngle + nowAngle)
            val largeClockTextStyle = TypoStyle(primaryFill, FontSize.MEDIUM).copy(fontWeight = FontWeight.Bold)
            val smallClockTextStyle = TypoStyle(secondaryText, FontSize.MEDIUM)

            repeat(hoursInDay){ hour ->
                val angle = (hour / hoursInDay.toFloat() * 360f) - 90f
                val radius = diameter / 2.5f
                val center = Offset(
                    x = center.x + cos(rad(angle)) * radius,
                    y = center.y + sin(rad(angle)) * radius
                )
                if(hour % 3 == 0) {
                    val result = textMeasurer.measure(hour.toString(), if(hour % 6 == 0) largeClockTextStyle else smallClockTextStyle)
                    drawText(result, topLeft = center - Offset(result.size.width.toFloat(), result.size.height.toFloat()) / 2f)
                } else {
                    drawCircle(clockDotColor, size.width / 130f, center)
                }
            }

            fun floatingText(text: TextLayoutResult, angle: Float, fill: Color){
                val radius = diameter / 2
                val center = Offset(
                    x = center.x + cos(rad(angle)) * (radius + text.size.width + size.width / 60f),
                    y = center.y + sin(rad(angle)) * (radius + text.size.height + size.width / 30f)
                )
                val horrizontalPadding = radius / 10f
                val verticalPadding = radius / 40f
                drawRoundRect(
                    fill,
                    Offset(
                        center.x - text.size.width / 2 - horrizontalPadding,
                        center.y - text.size.height / 2 - verticalPadding,
                    ),
                    Size(
                        text.size.width + 2 * horrizontalPadding,
                        text.size.height + 2 * verticalPadding,
                    ),
                    CornerRadius(
                        text.size.height + 2*verticalPadding,
                        text.size.height + 2*verticalPadding
                    )
                )
                drawText(text, topLeft = Offset(center.x - text.size.width / 2, center.y - text.size.height / 2))
            }
            floatingText(messuredNowText, nowAngle, secondaryFill)
            floatingText(messuredWakeUpTimeText, nowAngle + rotationAngle, primaryFill)
        }
    }
}