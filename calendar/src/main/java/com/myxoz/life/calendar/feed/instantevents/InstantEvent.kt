package com.myxoz.life.calendar.feed.instantevents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.lifecore.instantevents.InstantEvent
import com.myxoz.life.lifeui.providers.Theme
import com.myxoz.life.lifeui.reusedcompose.rippleClick
import com.myxoz.life.lifeui.theme.FontSize
import com.myxoz.life.lifeui.theme.OldColors
import com.myxoz.life.lifeui.theme.TypoStyle
import com.myxoz.life.presentation.R

@Composable
fun InstantEvent.RenderContent(oneHourDp: Dp, isEditing: Boolean = false, onClick: ()->Unit){
    val instantEventDisplaySize = INSTANTEVENTSIZE * oneHourDp
    Column(
        Modifier
            .size(instantEventDisplaySize)
            .background((if(isEditing) OldColors.SELECTED else OldColors.SECONDARY).copy(.8f), CircleShape)
            .clip(CircleShape)
            .rippleClick {
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painterResource(icon),
            subText,
            Modifier.height(oneHourDp / 2f),
            tint = Theme.primary
        )
        Spacer(Modifier.height((if (icon == R.drawable.pay_with_card) 2.dp else 4.dp)))
        Text(subText, style = TypoStyle(Theme.primary, FontSize.XSMALL))
    }
    Box(
        Modifier
            .padding(end = instantEventDisplaySize * .9f)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        OldColors.PRIMARYFONT
                    )
                ), CircleShape
            )
            .width(oneHourDp)
            .padding(vertical = 1.dp),
    )
}
internal const val INSTANTEVENTSIZE = 1.5