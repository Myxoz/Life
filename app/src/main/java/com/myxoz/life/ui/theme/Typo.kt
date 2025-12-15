package com.myxoz.life.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.myxoz.life.R

enum class FontColor(val color: Color) {
    PRIMARY(Colors.PRIMARYFONT),
    SECONDARY(Colors.SECONDARYFONT),
    SELECTED(Colors.SELECTED),
    FLAGSLIDER(Colors.FLAGSLIDER),
    ONSLIDER(Color(0xFF000000)),
    ONSECONDARY(Colors.ONSECONDARY),
    TERTIARY(Colors.TERTIARYFONT)
}

enum class FontSize(val size: TextUnit) {
    XSMALL(8.sp),
    SMALL(10.sp),
    SMALLM(12.sp),
    MEDIUMM(14.sp),
    MEDIUM(16.sp),
    LARGE(20.sp),
    MLARGE(23.sp),
    XLARGE(30.sp),
    XXLARGE(40.sp),
    DISPLAY(50.sp),
}

enum class FontFamily(val family: FontFamily) {
    Display(AppFont.ChangeOne)
}

fun TypoStyle(
    color: FontColor,
    fontSize: FontSize,
    fontFamily: com.myxoz.life.ui.theme.FontFamily? = null
): TextStyle =
    TextStyle.Default.copy(
        color = color.color,
        fontSize = fontSize.size,
        fontFamily = fontFamily?.family
    )

val TextUnit.dp: Dp @Composable get() = with(LocalDensity.current) { this@dp.toDp() }
fun TextUnit.dp(density: Density): Dp = with(density) { this@dp.toDp() }

//val Float.pxAsDp: Dp @Composable get() = (LocalDensity.current.density * this).dp
object AppFont {
    val ChangeOne = FontFamily(
        Font(R.font.changa_one, weight = FontWeight.W900),
    )
}