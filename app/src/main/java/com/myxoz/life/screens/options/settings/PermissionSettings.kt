package com.myxoz.life.screens.options.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalSettings
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.screens.person.displayperson.switchColors
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.copy
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.CalendarViewModel
import com.myxoz.life.viewmodels.Settings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Composable
fun SettingsPermissionComposable(calendarViewModel: CalendarViewModel) {
    val innerPadding = windowPadding
    Box(
        Modifier
            .background(Theme.background)
            .fillMaxSize()
            .edgeToEdgeGradient(Theme.background, innerPadding)
            .verticalScroll(rememberScrollState())
        ,
        Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .setMaxTabletWidth()
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val settings = LocalSettings.current
            Spacer(Modifier.height(innerPadding.calculateTopPadding() + 10.dp))
            Text("Berechtigungen", style = TypoStyle(Theme.secondary, FontSize.MEDIUM), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .background(Theme.surfaceContainer, RoundedCornerShape(30.dp))
                ,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                settings.permissions.all.forEachIndexed { i, c ->
                    if(i != 0){
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = Theme.outlineVariant)
                    }
                    PermissionComposable(c) { c.set(it) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Features", style = TypoStyle(Theme.secondary, FontSize.MEDIUM), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier
                    .background(Theme.surfaceContainer, RoundedCornerShape(30.dp)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                settings.features.all.forEach {
                    FeatureItem(it) { c-> it.set(c) }
                    HorizontalDivider(Modifier.padding(horizontal = 15.dp), color = Theme.outlineVariant)
                }
                HorizontalDivider(Modifier.padding(horizontal = 15.dp), color = Theme.outlineVariant)
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                val clipboard = LocalClipboard.current
                FeatureItem(settings.features.syncWithServer) {
                    coroutineScope.launch {
                        if(it){
                            val check = calendarViewModel.testSign()
                            if(check != null) {
                                Toast.makeText(context, "Verifiziert", Toast.LENGTH_SHORT).show()
                                settings.features.syncWithServer.set(true)
                            } else {
                                Toast.makeText(context, check, Toast.LENGTH_LONG).show()
                                coroutineScope.launch {
                                    clipboard.copy(calendarViewModel.getBase64Public())
                                }
                            }
                        } else {
                            settings.features.syncWithServer.set(false)
                        }
                    }
                }
            }
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}


@Composable
fun PermissionComposable(permission: Settings.Permissions.Permission, toggle: (new: Boolean)->Unit) {
    val state by permission.has.collectAsState()
    val isUseless by permission.useless.collectAsState()
    Row(
        Modifier
            .clip(CircleShape)
            .rippleClick{
                toggle(!state)
            }
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(permission.name, Modifier.weight(1f), style = TypoStyle(if(!isUseless) Theme.secondary else Theme.tertiary, FontSize.LARGE))
        Switch(state, {
            toggle(it)
        }, colors = switchColors())
    }
}
@Composable
fun FeatureItem(feature: Settings.Features.Feature, setTo: (Boolean)->Unit) {
    val state by feature.has.collectAsState()
    val isEnablable by remember{combine(flows = feature.reliesOn.map { it.has }){ flowResults ->
        flowResults.all { it } //  This is scuffed, I admit it
    }}.collectAsState(false)
    LaunchedEffect(Unit) {
        if(!feature.hasAssured()) feature.set(false)
        // Disables the feature if it's relied on permissions aren't granted, could else lead to undisablable features
    }
    Column(
        Modifier
            .padding(15.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(feature.name, Modifier.weight(1f), style = TypoStyle(Theme.primary, FontSize.LARGE))
            Switch(
                state,
                { setTo(!state) },
                enabled = isEnablable,
                colors = switchColors()
            )
        }
        Column(
            Modifier.padding(start = 10.dp)
        ) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                feature.reliesOn.forEach {
                    val has by it.has.collectAsState()
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .rippleClick{
                                if(!has) it.set(true)
                            }
                            .background(if(has) OldColors.Permissions.GRANTED else OldColors.Permissions.REVOKED)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(painterResource(if(has) R.drawable.tick else R.drawable.close), "Needs", Modifier.size(FontSize.SMALL.size.value.dp*.8f), Theme.onPrimary)
                        Text(it.name, style = TypoStyle(Theme.onPrimary, FontSize.SMALL))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(feature.description, style = TypoStyle(Theme.secondary, FontSize.MEDIUM), modifier = Modifier.padding(end = 52.dp))
        }
    }
}
