package com.myxoz.life.screens.options

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.AISettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AISettings(aiSettingsViewModel: AISettingsViewModel){
    val insets = windowPadding
    val coroutineScope = rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxSize()
        ,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .background(Theme.background)
                .edgeToEdgeGradient(Theme.background, insets)
                .setMaxTabletWidth()
        ) {
            Spacer(Modifier.height(insets.calculateTopPadding() + 10.dp))
            Text("Transaktionsvorschläge", style = TypoStyle(Theme.secondary, FontSize.MEDIUM), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Theme.surfaceContainer, RoundedCornerShape(30.dp))
                    .padding(20.dp, 10.dp)
                ,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                var trainingProgress by remember { mutableFloatStateOf(0f) }
                Row(
                    Modifier
                        .height(IntrinsicSize.Min)
                        .fillMaxWidth()
                    ,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Theme.secondaryContainer)
                            .rippleClick{
                                coroutineScope.launch {
                                    aiSettingsViewModel.resetPaymentPredictorModel()
                                }
                            }
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .padding(10.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.delete),
                            "Delete",
                            Modifier.fillMaxSize()
                            ,
                            Theme.onSecondaryContainer
                        )
                    }
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Theme.primary)
                            .rippleClick{
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        val allData = aiSettingsViewModel.getTrainingData()
                                        trainingProgress = 0f
                                        aiSettingsViewModel.trainPayments(allData, 10_000){
                                            trainingProgress = it / 10_000f
                                        }
                                    }
                                }
                            }
                            .padding(10.dp)
                    ) {
                        Text("Trainieren", style = TypoStyle(Theme.onPrimary, FontSize.MEDIUM))
                    }
                }
                Text("Fortschritt: ", style = TypoStyle(Theme.primary, FontSize.MEDIUM))
                LinearProgressIndicator(
                    { trainingProgress },
                    Modifier.fillMaxWidth(),
                    Theme.primary,
                    Theme.surfaceContainerHighest
                )
                Text("Erstellt einen Vorschlag für Transaktionsnamen bei bisher namenslosen Transaktion. Klickt man nach dem bargeldlosen Bezahlen auf die Transaktion, wird der vorgeschlagene Titel angezeigt.", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
            }
            Spacer(Modifier.height(20.dp))
            Text("Jegliche Form von KI findet auf offline, lokal statt, benötigt somit auch keine Clouddienste. Es gilt wie immer: KI kann Fehler machen, es ist lediglich ein stochastisches Model welches die warscheinlichste Möglichkeit berechnet. KI-Vorschläge sind mit dem Funken-Icon gekennzeichnet.", style = TypoStyle(Theme.secondary, FontSize.SMALL))
            Spacer(Modifier.height(insets.calculateBottomPadding()))
        }
    }
}