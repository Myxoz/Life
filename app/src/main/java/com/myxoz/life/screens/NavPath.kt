package com.myxoz.life.screens

object NavPath {
    const val HOME = "home"
    const val FULLSCREEN_EVENT = "fullscreen_event"
    const val SUMMARIZE_DAY = "summarize_day"
    const val ADVANCED_SEARCH = "advanced_search"

    @Deprecated("Use map launched with a specific location instead")
    const val MODIFY_LOCATION = "modify_location"

    class SingleParamaterizedPath(val parameterName: String, val template: (String)->String){
        fun with(value: Any) = template(value.toString())
        val asTemplate = template("{${parameterName}}")
    }
    object Pick {
        private const val PREFIX = "pick"
        const val LOCATION = "$PREFIX/location"
    }

    val DAY_OVERVIEW = SingleParamaterizedPath("epoch_day") {"day_overview/$it"}
    const val INSTANT_EVENT_SELECTION = "instant_event_selection"
    object DayOverview {
        val SCREENTIME = SingleParamaterizedPath(DAY_OVERVIEW.parameterName) {"${DAY_OVERVIEW.with(it)}/screentime"}
        val TRANSACTIONS = SingleParamaterizedPath(DAY_OVERVIEW.parameterName) {"${DAY_OVERVIEW.with(it)}/transactions"}
    }
    private const val TRANSACTION = "transaction"
    object Transaction {
        const val DETAILS = "$TRANSACTION/details"
        const val ME = "$TRANSACTION/me"
    }
    const val MENU = "menu"
    object Menu {
        const val LIFE_WRAPPED = "life_wrapped"
        const val TRANSACTION_FEED = "$MENU/transaction_feed"
        const val CONTACTS = "$MENU/contacts"
        object Contacts {
            val DISPLAY_PERSON = SingleParamaterizedPath("personId",) { "display_person/$it" }
        }
        const val SOCIAL_GRAPH = "$MENU/social_graph"
        const val ALARM = "$MENU/alarm"
        object Alarm {
            const val ALARM_SOUND_SETTINGS = "${ALARM}/soundsettings"
        }
        const val MAP = "$MENU/map"
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
            }
        }
        object Todo {
            const val MAIN = "$MENU/todo"
            val DETAILS = SingleParamaterizedPath("todo") { "$MAIN/details/$it" }
        }
    }
}