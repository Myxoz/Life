package com.myxoz.life.screens.quiz

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.utils.getAge
import java.time.LocalDate

sealed class BirthdayQuiz {
    abstract fun getQuestion(): AnnotatedString
    abstract fun answerList(): List<Boolean>
    abstract fun answerStrings(): List<String>
    class PersonToDate(val question: PersonSyncable, val answers: List<BirthdayDate>): BirthdayQuiz() {
        override fun getQuestion() = buildAnnotatedString {
            append("An welchem Datum hat ")
            marked()
            append(question.name)
            pop()
            append(" Geburtstag?")
        }

        override fun answerList(): List<Boolean> = answers.map { answer ->
            val date = LocalDate.ofEpochDay(question.birthday  ?: return@map false  /* This should never happen */)
            date.monthValue == answer.month && date.dayOfMonth == answer.date
        }

        override fun answerStrings(): List<String> = answers.map { it.toDisplay() }
    }
    class PersonToMonth(val question: PersonSyncable, val answers: List<BirthdayMonth>): BirthdayQuiz(){
        override fun getQuestion() = buildAnnotatedString {
            append("In welchem Monat hat ")
            marked()
            append(question.name)
            pop()
            append(" Geburtstag?")
        }
        override fun answerList(): List<Boolean> = answers.map { answer ->
            val date = LocalDate.ofEpochDay(question.birthday ?: return@map false /* This should never happen */)
            date.monthValue == answer.month
        }
        override fun answerStrings(): List<String> = answers.map { it.toDisplay() }
    }
    class PersonToAge(val question: PersonSyncable, val answers: List<BirthdayAge>): BirthdayQuiz(){
        override fun getQuestion() = buildAnnotatedString {
            append("Wie alt ist ")
            marked()
            append(question.name)
            pop()
            append("?")
        }
        override fun answerList(): List<Boolean> = answers.map { answer ->
            val birth = LocalDate.ofEpochDay(question.birthday ?: return@map false /* This should never happen */)
            answer.age == LocalDate.now().getAge(birth)
        }
        override fun answerStrings(): List<String> = answers.map { it.toDisplay() }
    }

    data class DateToPerson(val question: BirthdayDate, val answers: List<PersonSyncable>): BirthdayQuiz(){
        override fun getQuestion() = buildAnnotatedString {
            append("Wer hat am ")
            marked()
            append(question.toDisplay())
            pop()
            append(" Geburtstag?")
        }
        override fun answerList(): List<Boolean> = answers.map { answer ->
            val date = LocalDate.ofEpochDay(answer.birthday ?: return@map false /* This should never happen */)
            date.monthValue == question.month && date.dayOfMonth == question.date
        }
        override fun answerStrings(): List<String> = answers.map { it.name }
    }
    class BirthdayDate(val month: Int, val date: Int): QuestionResponse {
        override fun toDisplay(): String = "$date. ${months[month-1]}"
        companion object {
            fun of(date: LocalDate) = BirthdayDate(date.monthValue, date.dayOfMonth)
        }
    }
    class BirthdayMonth(val month: Int): QuestionResponse {
        override fun toDisplay(): String = months[month-1]
    }
    class BirthdayAge(val age: Int): QuestionResponse {
        override fun toDisplay(): String = age.toString()
    }
    interface QuestionResponse{
        fun toDisplay(): String
    }
    companion object {
        val months: Array<String> = arrayOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
        private fun AnnotatedString.Builder.marked(){
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
        }
    }
}
