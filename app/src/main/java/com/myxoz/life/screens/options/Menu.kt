package com.myxoz.life.screens.options

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.graphics.drawable.InsetDrawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import com.myxoz.life.LocalNavController
import com.myxoz.life.MainActivity
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.combinedRippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.windowPadding
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuComposable() {
    val context = LocalContext.current
    val nav = LocalNavController.current
    val currentDate = LocalDate.now()
    Box(
        Modifier
            .background(Theme.background)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ,
        Alignment.BottomCenter
    ) {
        FlowRow(
            Modifier
                .setMaxTabletWidth()
                .padding(windowPadding)
            ,
            maxItemsInEachRow = 2,
            verticalArrangement = Arrangement.Bottom,
            horizontalArrangement = Arrangement.End
        ) {
            val isWrapped = currentDate.dayOfYear < 20
            val all = arrayOf(
                SubOption(R.drawable.github, "Repositories", "commits/repos"),
                SubOption(R.drawable.info, "Informationen", "information"),
                SubOption(R.drawable.graph, "Social Graph", "social_graph"),
                SubOption(R.drawable.bank_transfer, "Transaktionen", "transactions"),
                SubOption(R.drawable.location, "Life Maps", "map"),
                SubOption(R.drawable.settings, "Settings", "settings"),
                SubOption(R.drawable.contacts, "Kontakte", "contacts"),
            )
            if(isWrapped) {
                val gradient = Brush.linearGradient(
                    listOf(Color(0xFF1BA1E3), Color(0xFF5489D6), Color(0xFF9B72CB), Color(0xFFD96570), Color(0xFFF49C46))
                )
                Column(
                    Modifier
                        .run{
                            if(all.size % 2 == 1) {
                                Modifier.weight(1f)
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        }
                        .padding(8.dp)
                        .border(1.dp, gradient, RoundedCornerShape(20.dp))
                        .background(Theme.surfaceContainer, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .combinedRippleClick{
                            nav.navigate("life_wrapped")
                        }
                        .padding(vertical = 20.dp)
                    ,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val fontSize = FontSize.DISPLAY.size.toDp()
                    Box {
                        val rotation = remember { Animatable(0f) }

                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(2000)
                                rotation.animateTo(
                                    targetValue = 180f,
                                    animationSpec = spring(
                                        Spring.DampingRatioMediumBouncy,
                                        Spring.StiffnessVeryLow
                                    )
                                )

                                delay(2000)

                                rotation.animateTo(
                                    targetValue = 360f,
                                    animationSpec = spring(
                                        Spring.DampingRatioMediumBouncy,
                                        Spring.StiffnessVeryLow
                                    )
                                )
                                rotation.snapTo(0f)
                            }
                        }
                        Column(
                            Modifier
                                .size(fontSize)
                                .rotate(rotation.value)
                            ,
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val style = TextStyle.Default.copy(
                                brush  = gradient,
                                fontSize = FontSize.DISPLAY.size/2,
                                fontFamily = FontFamily.Display.family
                            )
                            Text(
                                currentDate.year.minus(1).toString().take(2)
                                ,
                                style = style
                            )
                            Text(
                                currentDate.year.minus(1).toString().takeLast(2),
                                Modifier
                                    .rotate(180f)
                                ,
                                style = style
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("Wrapped", style = TypoStyle(Theme.primary, FontSize.LARGE, FontFamily.Display))
                }
            }
            all.forEachIndexed { i, it ->
                val mod = Modifier
                    .padding(8.dp)
                    .border(1.dp, Theme.outlineVariant, RoundedCornerShape(20.dp))
                    .background(Theme.surfaceContainer, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .combinedRippleClick(
                        onHold = {
                            it.createAndRequestShortcut(context)
                        }
                    ){
                        nav.navigate(it.route)
                    }
                    .padding(vertical = 20.dp)
                Column(
                    if(i==0 && !isWrapped && all.size % 2 != 0) {
                        mod.fillMaxWidth()
                    } else {
                        mod.weight(1f)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val fontSize = FontSize.DISPLAY.size.toDp()
                    Icon(painterResource(it.icon), it.text, Modifier.size(fontSize), Theme.secondary)
                    Spacer(Modifier.height(5.dp))
                    Text(it.text, style = TypoStyle(Theme.primary, FontSize.LARGE, FontFamily.Display))
                }
            }
        }
    }
}
data class SubOption(val icon: Int, val text: String, val route: String) {
    private fun adaptiveIconFromDrawables(context: Context, fg: Drawable): Icon {
        val size = context.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        val renderSize = size * 4

        val bg = ColorDrawable(OldColors.APPICONBG.toArgb())
        val tintedFg = DrawableCompat.wrap(fg.mutate()).apply {
            setTint(OldColors.PRIMARYFONT.toArgb())
        }

        // 25% padding total means foreground inset of 25% on each side [web:21]
        val pad = (renderSize * 0.48f).toInt()
        val insetFg = InsetDrawable(tintedFg, pad, pad, pad, pad)

        val adaptive = AdaptiveIconDrawable(bg, insetFg)

        val bitmap = createBitmap(renderSize, renderSize)
        val canvas = Canvas(bitmap)

        // Full-bleed draw so background fills the entire bitmap (no outline)
        adaptive.setBounds(0, 0, canvas.width, canvas.height)
        adaptive.draw(canvas)

        return Icon.createWithAdaptiveBitmap(bitmap)
    }
    fun createAndRequestShortcut(context: Context){
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("targetRoute", route)
        }

        val shortcut = ShortcutInfo.Builder(context, route+"v6")
            .setShortLabel(text)
            .setIcon(adaptiveIconFromDrawables(context, context.getDrawable(icon)?:return))
            .setIntent(intent)
            .build()

        val shortcutManager = context.getSystemService(ShortcutManager::class.java)

        if (shortcutManager.isRequestPinShortcutSupported) {
            shortcutManager.requestPinShortcut(shortcut, null)
        }
    }
}