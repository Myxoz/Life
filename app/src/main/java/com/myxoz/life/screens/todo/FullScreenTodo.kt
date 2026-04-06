package com.myxoz.life.screens.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalScreens
import com.myxoz.life.Theme
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.TodoViewModel

@Composable
fun FullScreenTodo(id: Long, todoViewModel: TodoViewModel){
    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.background)
            .padding(windowPadding)
    ) {
        val rawTodo by todoViewModel.getTodo(id).collectAsState()
        val todo = rawTodo?.value ?: return@Column
        Text(todo.short, style = TypoStyle(Theme.primary, FontSize.MEDIUM))
        Text(todo.details.orEmpty(), style = TypoStyle(Theme.primary, FontSize.MEDIUM))
        val screens = LocalScreens.current
        Box(
            Modifier
                .background(Theme.primary, CircleShape)
                .clip(CircleShape)
                .rippleClick{
                    screens.editSyncable(todo)
                }
                .padding(horizontal = 15.dp, vertical = 10.dp)
        ) {
            Text("Bearbeiten", style = TypoStyle(Theme.onPrimary, FontSize.MEDIUM))
        }
    }
}