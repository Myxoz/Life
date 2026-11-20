package com.myxoz.life.calendar.feed.topbar

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.myxoz.life.lifeui.providers.Theme
import com.myxoz.life.presentation.R
import java.time.LocalDate

@Composable
internal fun TopBarBirthday(topBarAggregator: TopBarAggregator, date: LocalDate){
    val birthdayPeople by topBarAggregator.getPeopleWithBirthdayAt(date).collectAsState()
    repeat(birthdayPeople.size) {
        Icon(
            painterResource(R.drawable.birthday),
            "Birthday",
            Modifier.fillMaxHeight(),
            Theme.secondary
        )
    }
}