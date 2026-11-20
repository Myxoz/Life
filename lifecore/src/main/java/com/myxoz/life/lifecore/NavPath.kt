package com.myxoz.life.lifecore

import com.myxoz.life.lifecore.instantevents.InstantEvent
import com.myxoz.life.storage.interfaces.BankingDisplayEntityKey
import kotlinx.serialization.Serializable
import java.time.LocalDate

object NavPath {
    @Serializable
    object Home

    @Serializable
    data class FullScreenEvent(val eventId: Long)

    @Serializable
    object SummarizeDay

    @Serializable
    object AdvancedSearch

    object Pick {
        @Serializable
        object Location
    }

    @Serializable
    sealed class FullDay(val dateElseToday: LocalDate?) {
        @Serializable
        class Overview(dateElseToday: LocalDate?): FullDay(dateElseToday)

        @Serializable
        class ScreenTime(dateElseToday: LocalDate?): FullDay(dateElseToday)

        @Serializable
        class Transactions(dateElseToday: LocalDate?): FullDay(dateElseToday)
    }

    @Serializable
    class InstantEventSelection(val list: List<InstantEvent>)
    object Transactions {
        @Serializable
        object Feed

        @Serializable
        data class Details(val transaction: BankingDisplayEntityKey)

        @Serializable
        object Me
    }
    object Menu {
        @Serializable
        object MenuItems

        @Serializable
        object Wrapped

        @Serializable
        object BirthdayQuiz

        object Contacts {
            @Serializable
            object List
            @Serializable
            sealed class SpecificPerson(val personId: Long) {
                @Serializable
                class FullScreen(personId: Long): SpecificPerson(personId)
                @Serializable
                class DebtFeed(personId: Long): SpecificPerson(personId)
            }
        }
        @Serializable
        object SocialGraph

        object Alarm {
            @Serializable
            object AlarmClock

            @Serializable
            object SoundSettings
        }

        object Streak {
            @Serializable
            object AllStreaks

            @Serializable
            sealed class SpecificStreak(val streakId: Long) {
                @Serializable
                class FullScreen(streakId: Long): SpecificStreak(streakId)

                @Serializable
                class EditStreak(streakId: Long): SpecificStreak(streakId)
            }
        }

        @Serializable
        object Map

        object Repos {
            @Serializable
            object AllRepos

            @Serializable
            data class Commit(val sha: String)

            @Serializable
            data class Repo(val name: String)
        }

        object More {
            @Serializable
            object MoreList

            @Serializable
            object Information

            object Settings {
                @Serializable
                object MainSettings

                @Serializable
                object Permissions

                @Serializable
                object Preferences
            }

            @Serializable
            object AI

            @Serializable
            object Debug
        }

        object Todo {
            @Serializable
            object TodoList

            @Serializable
            data class SpecificTodo(val id: Long)
        }
    }
}