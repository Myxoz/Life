package com.myxoz.life.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.myxoz.life.R

enum class FontColor(val color: Color) {
    PRIMARY(OldColors.PRIMARYFONT),
    SECONDARY(OldColors.SECONDARYFONT),
    SELECTED(OldColors.SELECTED),
    FLAGSLIDER(OldColors.FLAGSLIDER),
    ONSLIDER(Color(0xFF000000)),
    ONSECONDARY(OldColors.ONSECONDARY),
    TERTIARY(OldColors.TERTIARYFONT)
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
    color: Color,
    fontSize: FontSize,
    fontFamily: com.myxoz.life.ui.theme.FontFamily? = null
): TextStyle =
    TextStyle.Default.copy(
        color = color,
        fontSize = fontSize.size,
        fontFamily = fontFamily?.family
    )

@Deprecated("Use Typostyle instead", ReplaceWith("TypoStyle()"))
fun TypoStyleOld(
    color: FontColor,
    fontSize: FontSize,
    fontFamily: com.myxoz.life.ui.theme.FontFamily? = null
): TextStyle =
    TextStyle.Default.copy(
        color = color.color,
        fontSize = fontSize.size,
        fontFamily = fontFamily?.family
    )

//val Float.pxAsDp: Dp @Composable get() = (LocalDensity.current.density * this).dp
object AppFont {
    val ChangeOne = FontFamily(
        Font(R.font.changa_one, weight = FontWeight.W900),
    )
}