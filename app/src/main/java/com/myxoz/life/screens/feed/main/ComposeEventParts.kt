package com.myxoz.life.screens.feed.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.events.additionals.EventTag
import com.myxoz.life.events.additionals.TagLike
import com.myxoz.life.ui.theme.TagColors
import com.myxoz.life.utils.toSp

@Composable
fun RenderTagAndTitleBar(tags: List<TagLike>, title: String?, oneHourDp: Dp, blockHeight: Int, color: TagColors, textColor: Color){
    val mightNeedScaling = blockHeight in 2..4 && title != null && title.length > 10
    var actualWidth by remember { mutableIntStateOf(0) }
    Row(
        Modifier
            .fillMaxWidth()
    ) {
        val startPadding = when(blockHeight) {
            1 -> 0.dp
            2 -> 1.dp
            else -> 2.dp
        }
        val height = when(blockHeight) {
            1 -> oneHourDp/4f
            2 -> oneHourDp/2f
            3 -> oneHourDp/1.33f
            4 -> oneHourDp/1.75f
            else -> oneHourDp/1.5f
        }
        val fontHeight = (.7f*height).toSp()
        var optimalScaling by remember { mutableFloatStateOf(1f) }
        if(mightNeedScaling) {
            val textMessurer = rememberTextMeasurer()
            val textStyle = LocalTextStyle.current
            LaunchedEffect(actualWidth, title) {
                if(title.length < 14 || actualWidth == 0) return@LaunchedEffect
                val width = textMessurer.measure(title, textStyle.copy(fontSize = fontHeight)).size.width
                if(width == 0) return@LaunchedEffect
                optimalScaling = (actualWidth.toFloat() / width).coerceIn(.5f, 1f)
            }
        }
        if(tags.isNotEmpty()) {
            Row(
                Modifier
                    .padding(start = startPadding)
                    .height(height)
                    .run {
                        if (blockHeight != 1) {
                            padding(vertical = if (blockHeight > 2) 2.dp else 1.dp)
                                .background(color.CONTAINER, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 1.dp)
                        } else {
                            this.padding(start = 2.dp)
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                tags.filter { it!=EventTag.S }.forEach {
                    Icon(
                        painterResource(it.drawable),
                        null,
                        Modifier.height(height * optimalScaling),
                        if(blockHeight == 1) textColor else color.ICON
                    )
                }
            }
        }
        Text(
            title?:"",
            Modifier
                .padding(start = 4.dp, top = height*.1f, end = 4.dp)
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
                .onGloballyPositioned{
                    actualWidth = it.size.width
                }
            ,
            fontSize = fontHeight * optimalScaling,
            color = textColor,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RenderBasicEventContent(
    title: String,
    details: String?,
    smallText: String,
    isSmall: Boolean,
    tags: List<TagLike>?,
    fontColor: Color,
    secondaryColor: Color,
    tagColors: TagColors,
    oneHourDp: Dp,
    blockHeight: Int,
){
    if(isSmall){
        Text(
            smallText,
            Modifier
                .padding(all = 3.dp),
            fontSize = (oneHourDp / 4f).toSp(),
            color = fontColor,
            overflow = TextOverflow.Ellipsis
        )
    } else Column(
        Modifier
            .fillMaxSize()
    ) {
        RenderTagAndTitleBar(tags?:listOf(), title, oneHourDp, blockHeight, tagColors, fontColor)
        if(blockHeight>3 && details != null) {
            Text(
                details,
                Modifier
                    .padding(start = 10.dp),
                fontSize = (oneHourDp / 3f).toSp(),
                color = secondaryColor,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}