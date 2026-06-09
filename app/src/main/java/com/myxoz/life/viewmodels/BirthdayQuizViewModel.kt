package com.myxoz.life.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myxoz.life.repositories.AppRepositories
import com.myxoz.life.repositories.utils.subscribeToColdFlow
import com.myxoz.life.screens.quiz.BirthdayQuiz
import com.myxoz.life.utils.getAge
import com.myxoz.life.utils.syncToPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import java.lang.Math.floorMod
import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.nextInt

class BirthdayQuizViewModel(repos: AppRepositories): ViewModel() {
    var cachedValue: BirthdayQuiz? = null
    val allPeople = repos.peopleRepo.getAllPeople()
    private val changeFlow = MutableStateFlow(0)
    val currentStreak = MutableStateFlow(repos.prefs.getInt("birthday_streak", 0)).apply{
        syncToPrefs(viewModelScope, repos.prefs, "birthday_streak", Int::class)
    }
    val questionFlow = combine(allPeople, changeFlow) { people, _ ->
        val randomValue = Random.nextDouble()
        val allPeopleWithBirthdays = people.filter { it.birthday != null }
        if(allPeopleWithBirthdays.isEmpty()) return@combine null
        when {
            randomValue < 0.30 -> { // Age
                val person = allPeopleWithBirthdays.random()
                val offset = Random.nextInt(-5..0)
                if(randomValue < 0.1) {
                    val realAge = LocalDate.now().getAge(LocalDate.ofEpochDay(person.birthday!!))
                    BirthdayQuiz.PersonToAge(person, (0..5).map { BirthdayQuiz.BirthdayAge(realAge + offset + it) })
                } else {
                    val realMonth = LocalDate.ofEpochDay(person.birthday!!).monthValue - 1
                    BirthdayQuiz.PersonToMonth(person, (0..5).map {
                        BirthdayQuiz.BirthdayMonth(floorMod(realMonth + offset + it, 12)+1)
                    })
                }
            }
            else -> { // Person to Date
                val person = allPeopleWithBirthdays.random()
                val birthday = LocalDate.ofEpochDay(person.birthday!!)
                val candidates = allPeopleWithBirthdays
                    .filter {
                        val date = LocalDate.ofEpochDay(it.birthday!!)
                        date.monthValue != birthday.monthValue || date.dayOfMonth != birthday.dayOfMonth
                    }
                    .asSequence()
                    .map {
                        val candidateBirthday = LocalDate.ofEpochDay(it.birthday!!)
                        val diff = abs(candidateBirthday.dayOfYear - birthday.dayOfYear)
                        val dayDiff = if (diff > 182) 365 - diff else diff
                        val score = (5f - dayDiff / 365f * 10f) + // Value between 0-ca. 5 how close the birthday is
                                (if(candidateBirthday.dayOfMonth == birthday.dayOfMonth || candidateBirthday.month == birthday.month) 1 else 0) +
                                // Both is not possible
                                Random.nextDouble()
                        it to score
                    }
                    .sortedByDescending { it.second }
                    .distinctBy {
                        val date = LocalDate.ofEpochDay(it.first.birthday!!)
                        date.monthValue to date.dayOfMonth
                    }
                    .take(20)
                    .shuffled()
                    .map { it.first }
                    .take(5)
                    .plus(person)
                    .shuffled()
                    .toList()

                if(randomValue < 0.8)
                    BirthdayQuiz.PersonToDate(
                        person,
                        candidates.mapNotNull {
                            BirthdayQuiz.BirthdayDate.of(LocalDate.ofEpochDay(it.birthday?:return@mapNotNull null))
                        }.sortedBy { it.month * 100 + it.date }
                    )
                else
                    BirthdayQuiz.DateToPerson(
                        BirthdayQuiz.BirthdayDate.of(birthday),
                        candidates
                    )
            }
        }
    }.subscribeToColdFlow(viewModelScope, null)
    fun answered(correct: Boolean) {
        changeFlow.update { it+1 }
        currentStreak.update { if(correct) it+1 else 0 }
    }
}