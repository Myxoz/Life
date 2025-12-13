package com.myxoz.life

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.dp
import java.text.Normalizer

@Composable
fun LifeSearch(updateDays: (Int)->Unit){
    Row(
        Modifier
            .padding(vertical = 10.dp)
            .background(Colors.SECONDARY, CircleShape)
            .clip(CircleShape)
            .padding(horizontal = 10.dp)
            .fillMaxWidth(.95f),
        verticalAlignment = Alignment.CenterVertically
    ){
        var inputedValue: String? by remember { mutableStateOf(null) }
        TextField(
            inputedValue?:"",
            {inputedValue = it.ifEmpty { null }},
            Modifier
                .weight(1f),
            placeholder = @Composable{
                Text("Filtern / Finden", style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE))
            },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
            textStyle = TypoStyle(FontColor.PRIMARY, FontSize.LARGE)
        )
        var mode by remember { mutableIntStateOf(0) }
        Box(
            Modifier
                .clip(CircleShape)
                .rippleClick{
                    mode= (mode + 1) % 2
                }
                .padding(10.dp)
                .size(FontSize.LARGE.size.dp)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                mode == 0,
                enter = scaleIn(transformOrigin = TransformOrigin(0.5f, 1f)) + fadeIn(),
                exit = scaleOut(transformOrigin = TransformOrigin(0.5f, 0f)) + fadeOut())
            {
                Icon(painterResource(R.drawable.filter),
                    "Filter",
                    tint = Colors.PRIMARYFONT)
            }
            androidx.compose.animation.AnimatedVisibility(mode == 1,
                enter = scaleIn(transformOrigin = TransformOrigin(0.5f, 1f)) + fadeIn(),
                exit = scaleOut(transformOrigin = TransformOrigin(0.5f, 0f)) + fadeOut())
            {
                Icon(Icons.Rounded.Search,
                    "Search",
                    tint = Colors.PRIMARYFONT)
            }
        }
        var setwidth by remember { mutableIntStateOf(2) }
        val width by animateFloatAsState(setwidth.toFloat())
        Row(
            Modifier
                .clip(CircleShape)
                .padding(10.dp)
                .size(FontSize.LARGE.size.dp)
                .rippleClick{
                    setwidth = setwidth % 4 + 1
                    updateDays(setwidth)
                }
            ) {
            repeat(4){
                val widthPerElem = FontSize.LARGE.size.dp/width
                Box(Modifier
                    .fillMaxHeight()
                    .width(widthPerElem)
                    .padding(horizontal = (3f/width).dp)
                    .background(Colors.PRIMARYFONT, CircleShape)
                )
            }
        }
        val navController = LocalNavController.current
        Box(
            Modifier
                .clip(CircleShape)
                .padding(10.dp)
                .size(FontSize.LARGE.size.dp + 5.dp)
                .rippleClick{
                    navController.navigate("settings")
                }
        ) {
            Icon(painterResource(R.drawable.app), "Life", tint = Colors.PRIMARYFONT)
        }
    }
}
@Composable
fun Modifier.rippleClick(enabled: Boolean=true, function: ()->Unit): Modifier = this.clickable( remember { MutableInteractionSource() }, ripple(), onClick = function, enabled = enabled)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.combinedRippleClick(onHold: (()->Unit)?=null, enabled: Boolean = true, onClick: ()->Unit): Modifier = this.combinedClickable(
    remember { MutableInteractionSource() }, ripple(), onClick = onClick, onLongClick = onHold, enabled = enabled
)
private val diacriticsRegex = "\\p{Mn}+".toRegex()

private fun String.normalizeForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(diacriticsRegex, "")
        .lowercase()

private fun matchScore(text: String, queryNorm: String, queryRaw: String): Int {
    val norm = text.normalizeForSearch()
    val wordsNorm = norm.split(' ')
    val wordsRaw = text.split(' ')
    if (norm == queryNorm) return 0
    for ((wRaw, wNorm) in wordsRaw.zip(wordsNorm)) {
        if (wNorm == queryNorm &&
            wRaw.length == queryRaw.length &&
            wRaw.equals(queryRaw, true)
        ) return 1
    }
    if (wordsNorm.any { it.startsWith(queryNorm, true) }) return 2
    if (norm.contains(queryNorm)) return 3
    return Int.MAX_VALUE
}

fun <T> List<T>.filteredWith(
    query: String,
    keyB: (T) -> String,
    keyA: (T) -> String
): List<T> {
    if (query.isBlank()) return this

    val qNorm = query.normalizeForSearch()

    return this
        .mapNotNull { item ->
            val a = keyA(item)
            val b = keyB(item)

            val scoreA = matchScore(a, qNorm, query)
            val scoreB = matchScore(b, qNorm, query)
            if (scoreA == Int.MAX_VALUE && scoreB == Int.MAX_VALUE) return@mapNotNull null
            val (fieldRank, score, primaryText) =
                if (scoreA <= scoreB) {
                    Triple(0, scoreA, a)
                } else {
                    Triple(1, scoreB, b)
                }

            Triple(item, fieldRank, Pair(score, primaryText.normalizeForSearch()))
        }
        .sortedWith(
            compareBy<Triple<T, Int, Pair<Int, String>>> { it.second }
                .thenBy { it.third.first }
                .thenBy { it.third.second }
        )
        .map { it.first }
}
