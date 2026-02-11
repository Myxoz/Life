package com.myxoz.life.screens.pick

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.BOTTOMSEARCHBARHEIGHT
import com.myxoz.life.ui.BottomSearchBar
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.filteredWith
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.MapViewModel
import kotlinx.coroutines.launch

@Composable
fun PickExistingLocation(mapViewModel: MapViewModel){
    val nav = LocalNavController.current
    var search by remember { mutableStateOf(nav.previousBackStackEntry?.savedStateHandle?.get("pequery")?:"") }
    val allLocations by mapViewModel.getAllLocations.collectAsState()
    val filtered = remember(search, allLocations) {
        allLocations.filteredWith(search, {it.toAddress(true)}) { it.name }
    }
    val coroutineScope = rememberCoroutineScope()
    BackHandler(true) {
        nav.previousBackStackEntry?.savedStateHandle?.set("pelocation", null)
        nav.navigateUp()
    }
    Box(
        Modifier
            .fillMaxSize()
        ,
        Alignment.BottomCenter
    ) {
        val innerPadding = windowPadding
        LazyColumn(
            Modifier
                .fillMaxSize()
                .edgeToEdgeGradient(Theme.background, innerPadding)
            ,
            reverseLayout = true, // IMPORTANT
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { // At the bottom
                Spacer(Modifier.height(innerPadding.calculateBottomPadding() + BOTTOMSEARCHBARHEIGHT))
            }
            itemsIndexed(filtered, { _, it -> it.id}){ i, location ->
                val location = location
                Box(
                    Modifier
                        .setMaxTabletWidth()
                ) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(15.dp))
                            .rippleClick{
                                coroutineScope.launch {
                                    nav.previousBackStackEntry?.savedStateHandle?.set("pelocation", location.toJson().toString())
                                    nav.navigateUp()
                                }
                            }
                            .padding(horizontal = 15.dp, vertical = 15.dp)
                            .fillMaxWidth()
                        ,
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painterResource(R.drawable.location),
                            "Location",
                            Modifier.size(20.dp),
                            Theme.primary
                        )
                        Column{
                            Text(
                                location.name,
                                style = TypoStyle(Theme.primary, FontSize.LARGE)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                location.toAddress(false),
                                style = TypoStyle(Theme.secondary, FontSize.SMALLM)
                            )
                        }
                    }
                    if(i != filtered.size-1) HorizontalDivider(color = Theme.outlineVariant)
                }
            }
            item { // At the start
                Spacer(Modifier.height(innerPadding.calculateTopPadding()))
            }
        }
        Box(
            Modifier
                .setMaxTabletWidth()
        ) {
            BottomSearchBar(
                Theme.background,
                innerPadding.calculateBottomPadding(),
                { filter ->
                    search = filter
                }
            )
        }
    }
}