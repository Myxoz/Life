package com.myxoz.life.screens.quiz

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.matchInstrinsicHeight
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.BirthdayQuizViewModel
import kotlinx.coroutines.flow.drop

const val animationSpeed = 700
@Composable
fun BirthdayGuesser(birthdayQuizViewModel: BirthdayQuizViewModel) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(windowPadding),
        contentAlignment = Alignment.Center
    ){
        var questionOne: BirthdayQuiz? by remember { mutableStateOf(birthdayQuizViewModel.cachedValue) }
        var questionTwo: BirthdayQuiz? by remember { mutableStateOf(null) }
        var useOne by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            if(birthdayQuizViewModel.cachedValue != null) {
                birthdayQuizViewModel.questionFlow.drop(1)
            } else {
                birthdayQuizViewModel.questionFlow
            }.collect {
                useOne = !useOne
                birthdayQuizViewModel.cachedValue = it
                if(useOne)
                    questionOne = it
                else
                    questionTwo = it
            }
        }
        Column(
            Modifier.setMaxTabletWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    Modifier
                        .padding(15.dp)
                        .border(1.dp, Theme.outline, RoundedCornerShape(25))
                        .padding(15.dp, 5.dp)
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    val streak by birthdayQuizViewModel.currentStreak.collectAsState()
                    Text(streak.toString(), style = TypoStyle(Theme.primary, FontSize.MEDIUM))
                    Image(
                        painterResource(R.drawable.streak_flame),
                        "Streak",
                        Modifier
                            .aspectRatio(1f)
                            .matchInstrinsicHeight()
                    )
                }
            }
            Spacer(Modifier.height(50.dp))
            Box{
                androidx.compose.animation.AnimatedVisibility(
                    useOne,
                    Modifier.zIndex(if(useOne) 1f else 0f),
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { -it } + fadeOut()
                ) {
                    ReponseBlock(questionOne?:return@AnimatedVisibility) {
                        birthdayQuizViewModel.answered(it)
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    !useOne,
                    Modifier.zIndex(if(!useOne) 1f else 0f),
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { -it } + fadeOut()
                ) {
                    ReponseBlock(questionTwo?:return@AnimatedVisibility) {
                        birthdayQuizViewModel.answered(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReponseBlock(question: BirthdayQuiz, nextQuestion: (correct: Boolean)-> Unit) {
    val answers = remember(question) { question.answerList() }
    var responseIndex: Int? by remember(question) { mutableStateOf(null) }
    Column(
        Modifier
            .fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(question.getQuestion(), style = TypoStyle(Theme.primary, FontSize.XLARGE))
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            question.answerStrings().forEachIndexed { index, answerText ->
                val bupResponseIndex = responseIndex
                BirthdayGuess(
                    answerText,
                    if(bupResponseIndex != null) answers[index] else null
                ) {
                    if(responseIndex == null) responseIndex = index
                    if(answers[index]) nextQuestion(responseIndex == index)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
@Composable
private fun BirthdayGuess(answer: String, correct: Boolean?, onRespond: ()->Unit) {
    val bgColor by animateColorAsState(
        if(correct == true)
            Theme.primary
        else
            if(correct == false)
                Theme.background.copy(.5f).compositeOver(Theme.secondaryContainer)
            else
                Theme.secondaryContainer
        ,
        tween(animationSpeed)
    )
    val textColor by animateColorAsState(
        if(correct == true)
            Theme.onPrimary
        else
            if(correct == false)
                Theme.background.copy(.5f).compositeOver(Theme.onSecondaryContainer)
            else
                Theme.onSecondaryContainer,
        tween(animationSpeed)
    )
    val scale by animateFloatAsState(
        if(correct != null) if(correct) 1.01f else 0.95f else 1f,
        tween(animationSpeed)
    )
    Row(
        Modifier
            .scale(scale)
            .fillMaxWidth()
            .background(bgColor, CircleShape)
            .clip(CircleShape)
            .rippleClick(run = onRespond)
            .padding(15.dp)
            .height(IntrinsicSize.Min)
        ,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically // Unnecessary, same height
    ) {
        Text(
            answer,
            style = TypoStyle(textColor, FontSize.MLARGE)
        )
        Box{
            androidx.compose.animation.AnimatedVisibility(
                correct == null,
                enter = fadeIn(tween(animationSpeed)),
                exit = fadeOut(tween(animationSpeed)),
            ) {
                Box(
                    Modifier
                        .size(FontSize.MLARGE.size.toDp())
                        .border(3.dp, textColor, CircleShape)
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                correct != null,
                enter = fadeIn(tween(animationSpeed)),
                exit = fadeOut(tween(animationSpeed)),
            ) {
                Icon(
                    painterResource(if(correct == true) R.drawable.tick else R.drawable.close),
                    if(correct == true) "Correct" else "Wrong",
                    Modifier.size(FontSize.MLARGE.size.toDp()),
                    textColor
                )
            }
        }
    }
}