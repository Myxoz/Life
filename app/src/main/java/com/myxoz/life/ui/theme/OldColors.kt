package com.myxoz.life.ui.theme

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

    object SocialGraphColors {
        val NODE = Color(0xFFE1E1E1)
        val SELECTED_NODE = Color(0xFFFF7777)
        val RELEVANT_NODE = Color(0xFF994848)
        val IRRELEVANT_NODE = Color(0x801E1E1E)
        val EDGE = Color(0xFF7C7C7C)
        val RELEVANT_EDGE = Color(0xFF606060)
        val IRRELEVANT_EDGE = Color(0x802A2A2A)
    }

    object Myxoz {
        val MAIN = Color(0xFF2d2d66)
        val ACC = Color(0xFF40a8a8)
    }

    object Calendar {
        val semanticNull = Color(0x00000000)
        object Empty: EventColors {
            override val tagColors = null
            override val textColor = Color(0xFFFFFFFF)
            override val bg = Color(0xFFFF7777)
            override val secondary = semanticNull
        }
        object Timewaste: EventColors {
            override val textColor = Color(0xFFFFFFFF)
            override val tagColors = TagColors(textColor, Color(0xFF342D1E))
            override val bg = Color(0xFF4A412A)
            override val secondary = semanticNull
        }
        object Sleep: EventColors {
            override val tagColors = null
            override val textColor = Color(0xFFbfbfbf)
            override val bg = Color(0xFF666666)
            override val secondary = semanticNull
        }

        object Work: EventColors {
            override val textColor = Color.White
            override val tagColors = TagColors(
                Color.Black,
                textColor,
            )
            override val bg = Color(0xFF7A2E3A)
            override val secondary = Color(0xFFAAAAAA)
        }

        object Spont: EventColors {
            override val textColor = Color(0xFF444444)
            override val tagColors = TagColors(
                Color.White,
                textColor
            )
            override val bg = Color(0xFFFFEE6A)
            override val secondary = semanticNull
        }

        object Hobby: EventColors {
            override val tagColors = TagColors(
                Color.White,
                Color(0xFF3f258c)
            )
            override val textColor = Color(0xFFFFFFFF)
            override val bg = Color(0xFF5C4899)
            override val secondary = Color(0xFFAAAAAA)
        }

        object Learn: EventColors {
            override val tagColors = TagColors(
                Color.White,
                Color(0xFF266380)
            )
            override val textColor = Color(0xFFFFFFFF)
            override val bg = Color(0xFF368BB3)
            override val secondary = Color(0xFFDEDEDE)
        }

        object Social: EventColors {
            override val tagColors = TagColors(
                Color.White,
                Color(0xFF317e6b)
            )
            override val textColor = Color(0xFFFFFFFF)
            override val bg = Color(0xFF41a68d)
            override val secondary = Color(0xFFE3E3E3)
        }

        object Travel: EventColors {
            override val textColor = Color(0xFF363636)
            override val tagColors = null
            override val bg = Color(0xFFafa084)
            override val secondary = Color(0xFF585858)
        }

        object DigSoc: EventColors {
            override val textColor = Color(0xFF363636)
            override val bg = Color(0xFF94D16E)
            override val secondary = Color(0xFF585858)
            override val tagColors = TagColors(
                Color(0xFF363636),
                Color(0xFFE3E3E3)
            )
        }
    }
}

data class TagColors(val ICON: Color, val CONTAINER: Color)
interface EventColors {
    val tagColors: TagColors?
    val textColor: Color
    val bg: Color
    val secondary: Color
}
fun Color.invert() = Color(1-this.red, 1-this.green, 1-this.blue, this.alpha)
fun Color.makeContrasty(to: Color) = to.invert().copy(alpha = .2f).compositeOver(this)
