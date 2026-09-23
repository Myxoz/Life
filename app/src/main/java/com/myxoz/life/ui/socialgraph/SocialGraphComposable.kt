package com.myxoz.life.ui.socialgraph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.myxoz.life.LocalScreens
import kotlinx.coroutines.launch

@Composable
fun SocialGraph(socialGraphRepo: SocialGraphRepo){
    LaunchedEffect(Unit) {
        socialGraphRepo.keepNodesUpdated()
    }
    val screens = LocalScreens.current
    val coroutineScope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        SocialGraphGraph(socialGraphRepo.socialGraph, {
            socialGraphRepo.allPeopleForIds(it)
        }) { personId ->
            coroutineScope.launch {
                screens.setProfileInfoChartScale(socialGraphRepo.controlPanel.chartScale)
                screens.openPersonDetails(personId)
            }
        }
        SocialGraphButtons(socialGraphRepo)
    }
}