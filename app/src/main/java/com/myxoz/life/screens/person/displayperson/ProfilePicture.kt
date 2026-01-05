package com.myxoz.life.screens.person.displayperson

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.times
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import com.myxoz.life.LocalStorage
import com.myxoz.life.api.syncables.ProfilePictureSyncable
import com.myxoz.life.dbwrapper.ProfilePictureStored
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.toShape
import com.myxoz.life.viewmodels.ProfileInfoModel
import kotlinx.coroutines.flow.drop
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfilePictureWithText(photoPicker: PhotoPicker, profileInfoViewModel: ProfileInfoModel, personId: Long, scrollLength: Dp, fontSize: Dp, topBarHeight: Dp, updateText: (String)-> Unit){
    val context = LocalContext.current
    val processedBitmap: Bitmap? by profileInfoViewModel.picture.collectAsState()
    val db = LocalStorage.current
    val smallPbPadding = 10.dp
    @Suppress("UnnecessaryVariable", "RedundantSuppression") // LOL
    val smallPbSize = fontSize
    val maxHeight = topBarHeight-smallPbSize-smallPbPadding*2
    val progress = 1-min(maxHeight, scrollLength)/maxHeight
    val fontPadding = 20.dp
    val style = TypoStyle(FontColor.PRIMARY, FontSize.XLARGE, FontFamily.Display)
    val textMessurer = rememberTextMeasurer()
    val name by profileInfoViewModel.name.collectAsState()
    val textWidth = with(LocalDensity.current){
        textMessurer.measure(
            name?:"Name",
            style
        ).size.width.toDp()
    }
    LaunchedEffect(Unit) {
        photoPicker.processedBitmap
            .drop(1)
            .collect {
                println("Image selected / already passed bitmap, will be synced now $it")
                if(it==null){
                    ProfilePictureSyncable.deleteBitmapFile(context, personId)
                } else {
                    ProfilePictureSyncable.saveBitmapToFile(context, it, personId)
                    profileInfoViewModel.picture.value = it
                }
                db.profilePictureDao.insertProfilePicture(
                    ProfilePictureStored(
                        personId,
                        it != null
                    )
                )
                ProfilePictureSyncable(personId).addToWaitingSyncDao(db)
            }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .background(Colors.BACKGROUND)
            .height(progress*(maxHeight)+smallPbSize+smallPbPadding*2)
    ){
        val conf = LocalConfiguration.current
        val smallerScreenDimension = min(conf.screenWidthDp, conf.screenHeightDp).dp
        val isProfilePictureFullScreen by profileInfoViewModel.isProfilePictureFullScreen.collectAsState()
        val fullScreenProgress by animateFloatAsState(if(isProfilePictureFullScreen) 1f else 0f)
        val maxPbSize = smallerScreenDimension * (fullScreenProgress *.45f + 0.5f)
        val largePbPadding = 50.dp
        val topLeftPbX = progress*((LocalConfiguration.current.screenWidthDp.dp-maxPbSize)/2 - smallPbPadding)+smallPbPadding
        val topLeftPbY = progress*(largePbPadding-smallPbPadding)+smallPbPadding
        val pbSize = progress*(maxPbSize-smallPbSize)+smallPbSize
        val radius = progress*(maxPbSize+fontPadding-smallPbPadding)+smallPbPadding
        val degree = PI/2*(progress.pow(.75f))
        val textOffsetY = sin(degree)*radius + topLeftPbY
        val textOffsetX = cos(degree)*radius + topLeftPbX + (if(processedBitmap!=null) (1-progress)*pbSize else 0.dp) + progress*(pbSize - textWidth)/2
        Box(
            Modifier
                .offset(
                    x = topLeftPbX,
                    y = topLeftPbY
                )
                .size(pbSize)
        ){
            val bitmap = processedBitmap?.asImageBitmap()
            val morph = remember {
                Morph(MaterialShapes.Cookie9Sided, MaterialShapes.Square)
            }
            val path = remember(fullScreenProgress, progress) {
                morph.toPath((fullScreenProgress - 1 + progress).coerceIn(0f, 1f)).asComposePath()
            }
            val shape = path.toShape()
            if(bitmap!=null) {
                val infiniteTransition = rememberInfiniteTransition()
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1.25f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(10000),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Image( // Blured
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale * ((1f-fullScreenProgress)*.1f+.9f))
                        .blur(radius = 25.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        .clip(shape)
                    ,
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Colors.SECONDARY.copy(progress), shape)
                    .clip(shape)
                    .combinedClickable(null, null, true, onLongClick = {
                        photoPicker.pickPhoto()
                    }){
                        profileInfoViewModel.isProfilePictureFullScreen.value = !profileInfoViewModel.isProfilePictureFullScreen.value
                    }
            ){
                if(bitmap!=null) Image(
                    bitmap = bitmap,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            Modifier
                .offset(
                    x = textOffsetX,
                    y = textOffsetY
                )
                .height(fontSize),
            contentAlignment = Alignment.Center
        ) {
            val focusManager = LocalFocusManager.current
            BasicTextField(
                name?:"",
                {
                    updateText(it)
                },
                Modifier
                    .width(textWidth),
                textStyle = style.copy(textAlign = TextAlign.Center),
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions{
                    focusManager.clearFocus()
                },
                cursorBrush = SolidColor(Colors.PRIMARYFONT),
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 3.dp)
                .fillMaxWidth(.95f)
                .background(Colors.TERTIARY.copy(alpha = 1 - progress), CircleShape)
                .height(2.dp),
        )
    }
}