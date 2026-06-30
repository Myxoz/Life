package com.myxoz.life.ui

import androidx.navigation.NavType
import androidx.navigation.navArgument

object NavPath {
    const val HOME = "home"
    val FULLSCREEN_EVENT = SingleParamaterizedPath("event_id"){ "home/$it" }
    const val SUMMARIZE_DAY = "summarize_day"
    const val ADVANCED_SEARCH = "advanced_search"

    @Deprecated("Use map launched with a specific location instead")
    const val MODIFY_LOCATION = "modify_location"

    class SingleParamaterizedPath(val parameterName: String, val template: (String)->String){
        fun with(value: Any) = template(value.toString())
        val asTemplate = template("{${parameterName}}")
        fun asLongArg() = listOf(navArgument(parameterName) { type = NavType.LongType })
        fun asStringArg() = listOf(navArgument(parameterName) { type = NavType.StringType })
    }
    class SingleOptionalParamaterizedPath(val parameterName: String, val template: (String)->String){
        fun with(value: Any?) = if(value == null) template("") else template("${parameterName}="+value.toString())
        val asTemplate = template("${parameterName}={${parameterName}}")
        fun asLongActualStringArg() = listOf(navArgument(parameterName) { type = NavType.StringType; defaultValue = null; nullable = true })
    }
    object Pick {
        private const val PREFIX = "pick"
        const val LOCATION = "$PREFIX/location"
    }

    val DAY_OVERVIEW = SingleParamaterizedPath("epoch_day") {"day_overview/$it"}
    val INSTANT_EVENT_SELECTION = SingleParamaterizedPath("ie") {"instant_event_selection/$it" }
    object DayOverview {
        val SCREENTIME = SingleParamaterizedPath(DAY_OVERVIEW.parameterName) {"${DAY_OVERVIEW.with(it)}/screentime"}
        val TRANSACTIONS = SingleParamaterizedPath(DAY_OVERVIEW.parameterName) {"${DAY_OVERVIEW.with(it)}/transactions"}
    }
    private const val TRANSACTION = "transaction"
    object Transaction {
        val DETAILS = SingleParamaterizedPath("transactionId") { "$TRANSACTION/details/$it" }
        const val ME = "$TRANSACTION/me"
    }
    const val MENU = "menu"
    object Menu {
        const val LIFE_WRAPPED = "life_wrapped"
        const val BIRTHDAY_QUIZ = "birthday_quiz"
        const val TRANSACTION_FEED = "$MENU/transaction_feed"
        const val CONTACTS = "$MENU/contacts"
        object Contacts {
            val DISPLAY_PERSON = SingleParamaterizedPath("personId") { "display_person/$it" }
            val DEBT_DISPLAY = SingleParamaterizedPath("personId") { "display_person/$it/debt" }
        }
        const val SOCIAL_GRAPH = "$MENU/social_graph"
        const val ALARM = "$MENU/alarm"
        object Alarm {
            const val ALARM_SOUND_SETTINGS = "${ALARM}/soundsettings"
        }
        const val STREAK = "$MENU/streak"
        object Streak {
            val FULL_SCREEN_STREAK = SingleParamaterizedPath("streakId") { "$STREAK/$it" }
            const val EDIT_SCREEN_STREAK = "$STREAK/editing"
        }
        val MAP = SingleOptionalParamaterizedPath("place") {"map?$it"}
        const val MORE = "$MENU/more"
        const val REPOS = "repos"
        object Repos {
            val COMMIT = SingleParamaterizedPath("sha") {"$REPOS/commit/${it}"}
            val REPO = SingleParamaterizedPath("name") {"$REPOS/repo/${it}"}
        }
        object More {
            const val INFORMATION = "$MORE/information"
            const val SETTINGS = "$MORE/settings"
            const val AI = "$MORE/ai"
            const val DEBUG = "$MORE/debug"
            object Settings {
                const val PERMISSIONS = "$SETTINGS/permissions"
                const val PREFERENCES = "$SETTINGS/preferences"
            }
        }
        object Todo {
            const val MAIN = "$MENU/todo"
            val DETAILS = SingleParamaterizedPath("todo") { "$MAIN/details/$it" }
        }
    }
}