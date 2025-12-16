package com.myxoz.life.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.R
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.viewmodels.CalendarViewModel

@Composable
fun LifeBottomBar(calendarViewModel: CalendarViewModel){
    Row(
        Modifier
            .padding(vertical = 7.dp, horizontal = 10.dp)
            .fillMaxWidth()
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val mode by calendarViewModel.search.mode.collectAsState()
        val inputedValue by calendarViewModel.search.textInputValue.collectAsState()
        val setWidth by calendarViewModel.dayAmount.collectAsState()
        val width by animateFloatAsState(setWidth.toFloat())
        val rowHeight = FontSize.LARGE.size.toDp() + 20.dp
        Row(
            Modifier
                .clip(CircleShape)
                .size(rowHeight)
                .padding(7.dp)
                .rippleClick{
                    calendarViewModel.dayAmount.value = setWidth % 4 + 1
                }
        ) {
            repeat(4){
                val widthPerElem = (FontSize.LARGE.size.toDp() + 6.dp)/width
                Box(Modifier
                    .fillMaxHeight()
                    .width(widthPerElem)
                    .padding(horizontal = (6f/width).dp)
                    .background(Colors.PRIMARYFONT, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(7.dp))
        val focusManager = LocalFocusManager.current
        var hasFocus by remember { mutableStateOf(false) }
        BasicTextField(
            inputedValue?:"",
            {
                calendarViewModel.search.setText(it)
            },
            Modifier
                .weight(1f)
                .onFocusChanged{
                    hasFocus = it.hasFocus || it.isFocused  || it.isCaptured
                }
            ,
//            placeholder = @Composable{
//                Text("Filtern / Finden", style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE))
//            },
//            colors = TextFieldDefaults.colors(
//                unfocusedContainerColor = Color.Transparent,
//                focusedContainerColor = Color.Transparent,
//                focusedIndicatorColor = Color.Transparent,
//                unfocusedIndicatorColor = Color.Transparent
//            ),
            decorationBox = @Composable { innerTextField ->
                Box(
                    Modifier
                        .background(Colors.SECONDARY, RoundedCornerShape(50, 20, 20, 50))
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp)
                        .height(rowHeight)
                    ,
                    contentAlignment = Alignment.CenterStart
                ){
                    if(hasFocus || inputedValue!=null) {
                        Row(
                            Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .weight(1f)
                            ) {
                                innerTextField()
                            }
                            Icon(
                                painterResource(R.drawable.close),
                                "End",
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .rippleClick{
                                        focusManager.clearFocus()
                                        calendarViewModel.search.setText(null)
                                    }
                                ,
                                Colors.SECONDARYFONT
                            )
                        }
                    } else{
                        Text(
                            "Suchen / Finden",
                            Modifier.align(Alignment.Center),
                            style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE),
                        )
                    }
                }
            },
            singleLine = true,
            cursorBrush = SolidColor(Colors.PRIMARYFONT),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
            }),
            textStyle = TypoStyle(FontColor.PRIMARY, FontSize.LARGE)
        )
        Spacer(Modifier.width(2.dp))
        val navController = LocalNavController.current
        val targetShape = RoundedCornerShape(20, 50, 50, 20)
        Box(
            Modifier
                .clip(targetShape)
                .size(rowHeight)
                .background(Colors.SECONDARY, targetShape)
                .padding(7.dp)
                .rippleClick{
                    navController.navigate("settings")
                }
        ) {
            Icon(painterResource(R.drawable.target), "Search Specific", Modifier.fillMaxSize(), Colors.PRIMARYFONT)
        }
        Spacer(Modifier.width(5.dp))
        Box(
            Modifier
                .clip(CircleShape)
                .size(rowHeight)
                .background(Colors.SECONDARY, CircleShape)
                .padding(7.dp)
                .rippleClick{
                    navController.navigate("settings")
                }
        ) {
            Icon(painterResource(R.drawable.app), "Life", Modifier.fillMaxSize(), Colors.PRIMARYFONT)
        }
    }
}