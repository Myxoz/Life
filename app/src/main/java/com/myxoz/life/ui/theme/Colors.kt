package com.myxoz.life.ui.theme

import androidx.compose.ui.graphics.Color

object Colors {
    val LIFFYCOLOR = Color(0xFFFFFFFF)
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
        object Sleep {
            val BG = Color(0xFF666666)
            val FG = Color(0xFFbfbfbf)
        }

        object Spont {
            val TEXT = Color(0xFF444444)
            val Tag = TagColors(
                Color.White,
                TEXT
            )
            val BG = Color(0xFFFFEE6A)
        }

        object Hobby {
            val TEXT = Color(0xFFFFFFFF)
            val Tag = TagColors(
                Color.White,
                Color(0xFF3f258c)
                //Color(0xFF3f258c)
            )
            val BG = Color(0xFF5C4899)

            //val BG = Color(0xFF54428B)
            val SECONDARY = Color(0xFFAAAAAA)
        }

        object Learn {
            val TEXT = Color(0xFFFFFFFF)
            val Tag = TagColors(
                Color.White,
                Color(0xFF266380)
            )
            val BG = Color(0xFF368BB3)
            val SECONDARY = Color(0xFFDEDEDE)
        }

        object Social {
            val TEXT = Color(0xFFFFFFFF)
            val Tag = TagColors(
                Color.White,
                Color(0xFF317e6b)
            )
            val BG = Color(0xFF41a68d)
            val SECONDARY = Color(0xFFE3E3E3)
        }

        object Travel {
            val TEXT = Color(0xFF363636)
            val BG = Color(0xFFafa084)
            val SECONDARY = Color(0xFF585858)
        }

        object DigSoc {
            val TEXT = Color(0xFF363636)
            val BG = Color(0xFF94D16E)
            val SECONDARY = Color(0xFF585858)
            val Tag = TagColors(
                Color(0xFF363636),
                Color(0xFFE3E3E3)
            )
        }

    }
}

data class TagColors(val ICON: Color, val CONTAINER: Color)
