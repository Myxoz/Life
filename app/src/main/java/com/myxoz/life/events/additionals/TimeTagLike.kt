package com.myxoz.life.events.additionals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.myxoz.life.Theme
import com.myxoz.life.ui.Chip
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.toDp
import org.json.JSONObject

data class TimedTagLikeContainer<T: TagLike>(val type: T, val durationMs: Long) {
    fun timedTagLikeToJson(): JSONObject = JSONObject().put("type", type.id).put("duration_ms", durationMs)

    companion object {
        @Composable
        @OptIn(ExperimentalLayoutApi::class)
        fun <T: TagLike> TimedTagLikeBar(tags: List<TimedTagLikeContainer<T>>, onClick: ((T)->Unit)? = null){
            val size = FontSize.MEDIUM.size.toDp()
            FlowRow {
                tags.forEach {
                    Box(
                        Modifier.padding(2.dp)
                        // Due to weirdness with vertical arrangement: last item ommited
                    ) {
                        Chip(
                            {
                                onClick?.invoke(it.type)
                            },
                            spacing = 5.dp
                        ) {
                            Icon(
                                painterResource(it.type.drawable),
                                null,
                                Modifier.run {
                                    if (it.type == Vehicle.Bus) width(2f * size).height(
                                        size
                                    ) else size(size)
                                },
                                Theme.onSecondaryContainer
                            )
                            val m = it.durationMs / 1000 / 60
                            Text(
                                "${if(m >= 60) "${m/60}h " else ""}${(m%60).toString().padStart(if(m>10) 2 else 1, '0')}m",
                                style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM)
                            )
                        }
                    }
                }
            }
        }
    }
}
