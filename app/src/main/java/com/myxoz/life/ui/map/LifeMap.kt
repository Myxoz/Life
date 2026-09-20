package com.myxoz.life.ui.map

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.mapbox.maps.MapboxExperimental
import com.myxoz.life.ui.theme.OldColors

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@OptIn(MapboxExperimental::class, ExperimentalMaterial3Api::class,
    com.mapbox.annotation.MapboxExperimental::class
)
@Composable
fun MapBoxMap(mapRepo: MapRepo){
    Scaffold(
        containerColor = OldColors.BACKGROUND
    ) { innerPadding ->
        MapBoxMap(mapRepo, innerPadding)
        MapSheet(mapRepo, innerPadding)
    }
}