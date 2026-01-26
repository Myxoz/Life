package com.myxoz.life.utils

import android.os.Build
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext

@Composable
fun systemColorScheme(): ColorScheme {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    } else {
        if (darkTheme) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    }
}

// Copied because of internallity
@Composable
fun rememberTextSelectionColors(colorScheme: ColorScheme): TextSelectionColors {
    val primaryColor = colorScheme.primary
    return remember(primaryColor) {
        TextSelectionColors(
            handleColor = primaryColor,
            backgroundColor = primaryColor.copy(alpha = TextSelectionBackgroundOpacity),
        )
    }
}

const val TextSelectionBackgroundOpacity = 0.4f

@Composable
fun rememberColorScemeFromColor(color: Color, vararg key: Any) = remember(*key) {
    colorSchemeFromColor(color)
}
fun colorSchemeFromColor(color: Color): ColorScheme =
    darkColorScheme().copy(
        primary = Color(0x40FFFFFF).compositeOver(color),
        secondary = Color(0xA0FFFFFF).compositeOver(color),
        tertiary = Color(0xC0FFFFFF).compositeOver(color),
        primaryContainer = Color(0x80888888).compositeOver(color),
        secondaryContainer = Color(0xD0333333).compositeOver(color),
        onSecondaryContainer = Color(0xC0FFFFFF).compositeOver(color),
        onPrimaryContainer = Color(0xC0000000).compositeOver(color),
        surfaceContainerHighest = Color(0xD0222222).compositeOver(color),
        tertiaryContainer = Color(0xE8333333).compositeOver(color),
        outlineVariant = Color(0xE0555555).compositeOver(color),
        surfaceContainerHigh = Color(0xE0222222).compositeOver(color),
        background = Color(0xEE080808).compositeOver(color),
        surfaceContainer = color
    )

@Composable
fun animateColorSchemeAsState(
    targetScheme: ColorScheme,
    animationSpec: FiniteAnimationSpec<Color> = tween(500, easing = LinearEasing),
    label: String = "ColorSchemeTransition"
): ColorScheme {
    val transition = updateTransition(
        targetState = targetScheme,
        label = label
    )

    @Composable
    fun animate(
        getter: (ColorScheme) -> Color
    ) = transition.animateColor(
        transitionSpec = { animationSpec },
        label = ""
    ) { scheme -> getter(scheme) }

    return ColorScheme(
        primary = animate { it.primary }.value,
        onPrimary = animate { it.onPrimary }.value,
        primaryContainer = animate { it.primaryContainer }.value,
        onPrimaryContainer = animate { it.onPrimaryContainer }.value,
        inversePrimary = animate { it.inversePrimary }.value,

        secondary = animate { it.secondary }.value,
        onSecondary = animate { it.onSecondary }.value,
        secondaryContainer = animate { it.secondaryContainer }.value,
        onSecondaryContainer = animate { it.onSecondaryContainer }.value,

        tertiary = animate { it.tertiary }.value,
        onTertiary = animate { it.onTertiary }.value,
        tertiaryContainer = animate { it.tertiaryContainer }.value,
        onTertiaryContainer = animate { it.onTertiaryContainer }.value,

        background = animate { it.background }.value,
        onBackground = animate { it.onBackground }.value,

        surface = animate { it.surface }.value,
        onSurface = animate { it.onSurface }.value,
        surfaceVariant = animate { it.surfaceVariant }.value,
        onSurfaceVariant = animate { it.onSurfaceVariant }.value,
        surfaceTint = animate { it.surfaceTint }.value,

        inverseSurface = animate { it.inverseSurface }.value,
        inverseOnSurface = animate { it.inverseOnSurface }.value,

        error = animate { it.error }.value,
        onError = animate { it.onError }.value,
        errorContainer = animate { it.errorContainer }.value,
        onErrorContainer = animate { it.onErrorContainer }.value,

        outline = animate { it.outline }.value,
        outlineVariant = animate { it.outlineVariant }.value,
        scrim = animate { it.scrim }.value,

        surfaceBright = animate { it.surfaceBright }.value,
        surfaceDim = animate { it.surfaceDim }.value,
        surfaceContainer = animate { it.surfaceContainer }.value,
        surfaceContainerHigh = animate { it.surfaceContainerHigh }.value,
        surfaceContainerHighest = animate { it.surfaceContainerHighest }.value,
        surfaceContainerLow = animate { it.surfaceContainerLow }.value,
        surfaceContainerLowest = animate { it.surfaceContainerLowest }.value,
    )
}