package com.myxoz.life.screens.options

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.ui.getMaxTabletScreenWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.angledGradientBackground
import com.myxoz.life.utils.toShape
import com.myxoz.life.utils.windowPadding

@Composable
fun InformationComposable(){
    Column(
        Modifier
            .fillMaxSize()
            .background(Theme.background)
            // .verticalScroll(rememberScrollState())
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(windowPadding.calculateTopPadding() + 20.dp))
        val width = getMaxTabletScreenWidth()
        AnimatedAppIcon(width)
        Text("Life", style = TypoStyle(Theme.primary, FontSize.XXLARGE, FontFamily.Display))
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .height(IntrinsicSize.Min)
        ) {
            Text("Made with \u2665\ufe0f by ", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
            Icon(painterResource(R.drawable.myxoz_text), "myxoz", Modifier.fillMaxHeight().aspectRatio(3f), tint = Theme.secondary)
        }

        Spacer(Modifier.height(windowPadding.calculateBottomPadding()))
    }
}
@Composable
fun AnimatedAppIcon(width: Dp){
    val start = Color(0xFF9466FF)
    val end = Color(0xFF5517E6)
    val angle = 80f
    MorphingShapes(listOf(
        MaterialShapes.Flower,
        MaterialShapes.Clover8Leaf,
        MaterialShapes.Pill,
        MaterialShapes.Slanted,
        MaterialShapes.Cookie4Sided,
        MaterialShapes.Heart,
        MaterialShapes.Puffy,
    )) { shape ->
        Box(
            Modifier
                .size(width*.3f)
                .clip(shape)
                .angledGradientBackground(listOf(start, end), angle)
            ,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.app),
                "App icon",
                Modifier.size(width*.2f),
                Color.White
            )
        }
    }
}
@Composable
fun MorphingShapes(
    shapes: List<RoundedPolygon>,
    duration: Int = 2400,
    content: @Composable (Shape) -> Unit
) {
    require(shapes.size >= 2)

    var index by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            progress.animateTo(
                1f,
                animationSpec = tween(duration)
            )

            progress.snapTo(0f)
            index = (index + 1) % shapes.size
        }
    }

    val morph = remember(index) {
        Morph(
            shapes[index],
            shapes[(index + 1) % shapes.size]
        )
    }

    val path = remember(progress.value, morph) {
        morph.toPath(progress.value).asComposePath()
    }

    val shape = path.toShape()

    content(shape)
}
