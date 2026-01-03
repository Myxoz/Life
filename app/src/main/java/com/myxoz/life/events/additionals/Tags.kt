package com.myxoz.life.events.additionals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import com.myxoz.life.R
import com.myxoz.life.api.forEach
import com.myxoz.life.dbwrapper.TagsDao
import com.myxoz.life.dbwrapper.TagsEntity
import com.myxoz.life.ui.theme.TagColors
import com.myxoz.life.utils.toSp
import org.json.JSONArray
import org.json.JSONObject

enum class EventTag(override val id: Int, val displayName: String, val queryString: List<String>, override val drawable: Int): TagLike {
    Party(1, "Feier", listOf("Party", "Feiern", "Geburtstagsfeier"), R.drawable.party),
    Eat(2, "Essen", listOf("Futtern", "Snack", "Snacken", "Frühstück", "Mittagessen", "Abendessen", "Fooden"), R.drawable.food),
    Cards(3, "Gesellschafsspiele", listOf("Karten", "Brettspiele", "Poker", "Uno", "Spieleabend"), R.drawable.poker_card),
    Shopping(4, "Shopping", listOf("Einkaufen", "Shoppen", "Mall", "Einkäufe"), R.drawable.shopping),
    Design(5, "Design", listOf("Concept Art", "Skizzieren", "Zeichnen", "Artwork", "Entwurf"), R.drawable.concept),
    Code(6, "Code", listOf("Programmieren", "Script", "Dev", "Software"), R.drawable.code),
    Watch(7,"Streaming", listOf("Netflix", "Serien", "Filme", "Film", "Twitch", "Binge", "Kino", "Stream"), R.drawable.watch),
    Alcohol(8, "Saufen", listOf("Alkohol", "Trinken", "Bier", "Wein", "Shots", "Betrinken", "Besaufen", "Pegel"), R.drawable.alc),
    Shower(9, "Hygiene", listOf("Duschen", "Baden", "Pflege", "Rasieren"), R.drawable.shower),
    P(10, "P", listOf(), R.drawable.p),
    S(11, "S", listOf(), R.drawable.s),
    Lecture(12, "Vorlesung", listOf("Präsentation", "Vorstellung"), R.drawable.lecture),
    Studium(13, "Uni", listOf("Universität", "Studium", "Studieren"), R.drawable.study),
    Android(14, "Android", listOf(), R.drawable.android),
    Clean(15, "Putzen", listOf("Saugen", "Schrubben", "Fegen", "Feudeln", "Wischen"), R.drawable.clean),
    Cook(16, "Kochen", listOf("Schneiden", "Mixen", "Zubereiten"), R.drawable.cook),
    ;
    fun matches(query: String) = displayName.contains(query, true) || queryString.any { it.contains(query, true) }
    // Always add to getResource and to getTagById!!!
    companion object {
        fun getTagById(id: Int) = EventTag.entries.firstOrNull {
            it.id == id
        }
    }
}
interface TagLike {
    val id: Int
    val drawable: Int
}
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
                            this
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                tags.filter { it!=EventTag.S  && it!=EventTag.P }.forEach {
                    Icon(
                        painterResource(it.drawable),
                        null,
                        Modifier.height(height * optimalScaling),
                        color.ICON
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
interface TagEvent {
    val eventTags: List<EventTag>
    suspend fun storeTags(tagsDao: TagsDao, id: Long){
        eventTags.forEach {
            tagsDao.insertTag(
                TagsEntity(
                    id,
                    it.id
                )
            )
        }
    }
    fun containsTagLike(query: String) = eventTags.any { it.matches(query) }
    fun JSONObject.addTags(): JSONObject = put("tags", JSONArray().apply {eventTags.forEach { put(it.id) }})
    companion object {
        fun JSONObject.getTagsFromJson() = getJSONArray("tags").run {
            val list = mutableListOf<EventTag>()
            forEach {
                val eventTag = EventTag.getTagById(it as Int)?:return@forEach
                list.add(eventTag)
            }
            list
        }
    }
}