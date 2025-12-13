package com.myxoz.life.options

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.R
import com.myxoz.life.rippleClick
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.ui.theme.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsComposable() {
    Scaffold(
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        FlowRow(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ,
            maxItemsInEachRow = 2,
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = Arrangement.End
        ) {
            val nav = LocalNavController.current
            val all = arrayOf(
                SubOption(R.drawable.info, "Informationen") {nav.navigate("information")},
                SubOption(R.drawable.stats, "Statistiken") {
                    nav.navigate("social_graph")
                },
                SubOption(R.drawable.pay_by_card, "Transaktionen") {nav.navigate("transactions")},
                SubOption(R.drawable.settings, "Berechtigungen") {nav.navigate("settings/permissions")},
                SubOption(R.drawable.contacts, "Kontakte") {nav.navigate("contacts")},
            )
            all.forEachIndexed { i, it ->
                val mod = Modifier
                    .padding(8.dp)
                    .border(1.dp, Colors.TERTIARY, RoundedCornerShape(20.dp))
                    .background(Colors.SECONDARY, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .rippleClick{
                        it.onClick()
                    }
                    .padding(vertical = 20.dp)
                Column(
                    if(i==0 && all.size % 2 != 0) {
                        mod.fillMaxWidth()
                    } else {
                        mod.weight(1f)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val fontSize = FontSize.DISPLAY.size.dp
                    Icon(painterResource(it.icon), it.text, Modifier.size(fontSize), Colors.SECONDARYFONT)
                    Spacer(Modifier.height(5.dp))
                    Text(it.text, style = TypoStyle(FontColor.PRIMARY, FontSize.LARGE, FontFamily.Display))
                }
            }
        }
    }
}
data class SubOption(val icon: Int, val text: String, val onClick: ()-> Unit)