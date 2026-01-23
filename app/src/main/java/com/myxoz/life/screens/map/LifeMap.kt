package com.myxoz.life.screens.map

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.mapbox.maps.MapboxExperimental
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.viewmodels.MapViewModel

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@OptIn(MapboxExperimental::class, ExperimentalMaterial3Api::class,
    com.mapbox.annotation.MapboxExperimental::class
)
@Composable
fun MapBoxMap(mapViewModel: MapViewModel){
    Scaffold(
        containerColor = OldColors.BACKGROUND
    ) { innerPadding ->
        MapBoxMap(mapViewModel, innerPadding)
        MapSheet(mapViewModel, innerPadding)
    }
}