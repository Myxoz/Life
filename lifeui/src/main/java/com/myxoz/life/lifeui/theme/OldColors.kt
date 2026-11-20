package com.myxoz.life.lifeui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

object OldColors {
    val GITHUB  = Color(0xFFE2E9E4)
    val GITHUBBG = Color(0xFF1f6feb)
    val LIFFYCOLOR = Color(0xFFFFFFFF)
    val APPICONBG = Color(0xFF181818)
    val BACKGROUND = Color(0xFF202020)
    val SCREENTIME = Color(0xFF4A412A)
    val DAYPILLBG = Color(0xFF6f7d82)
    val DIVIDERS = Color(0xFFAAAAAA)
    val CALENDARBG = Color(0xFF0a0a0a)
    val SELECTED = Color(0xFFFF7777)
    val SECONDARY = Color(0xFF313131)
    val ONBACKGROUND = Color(0xFFbfbfbf)
    val PRIMARYFONT = Color(0xFFFFFFFF)
    val SECONDARYFONT = Color(0xFFbfbfbf)
    val ONSECONDARY = Color(0xFFbfbfbf)
    val TERTIARYFONT = Color(0xFF666666)
    val FLAGSLIDER = Color(0xFF5FFF5F)
    val STEPS = Color(0xFF4D993D)
    val HAPPYNESS = Color.hsl(170f, 0.78f, 0.25f)
    val STRESS = Color.hsl(244f, 0.78f, 0.25f)
    val PRODUCTIVITY = Color.hsl(301f, 0.78f, 0.25f)
    val TERTIARY = Color(0xFF4a4a4a)
    val ACCEPT = Color(0x2000FF00)
    val DECLINE = Color(0x20FF0000)

    object Transactions {
        val MINUS = Color(0xFFFF8080)
        val PLUS = Color(0xFF80FF80)
    }
    object Streaks {
        val MAIN = Color(0xFFE24A22)
        val NOT_YET_REACHED = Color(0xFFB36B59)
        val BURNED = Color(0xFF000000)
        val ENDED = Color(0xFF48403D)
    }
    object Commits {
        val DELETION = Color(0xFFFF8080)
        val ADDITION = Color(0xFF80FF80)
        val CHANGE = Color(0xFF80AFFF)
    }

    object Permissions {
        val REVOKED = Color(0x80FF8080)
        val GRANTED = Color(0x8080FF80)
    }

    object Myxoz {
        val MAIN = Color(0xFF2d2d66)
        val ACC = Color(0xFF40a8a8)
    }
}

// TODO Streak specific, move there
fun Color.invert() = Color(1-this.red, 1-this.green, 1-this.blue, this.alpha)
fun Color.makeContrasty(to: Color) = to.invert().copy(alpha = .2f).compositeOver(this)
