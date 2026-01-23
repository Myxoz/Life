package com.myxoz.life.screens.person.displayperson

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.LocalSettings
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.android.contacts.AndroidContacts
import com.myxoz.life.android.integration.HVV
import com.myxoz.life.api.syncables.Location
import com.myxoz.life.api.syncables.PersonSyncable
import com.myxoz.life.ui.Chip
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.AndroidUtils
import com.myxoz.life.utils.PhoneNumberParser
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toPx
import com.myxoz.life.viewmodels.LargeDataCache
import com.myxoz.life.viewmodels.ProfileInfoModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.roundToInt

private const val animationDuration = 300 // Default Compose Speed
@Composable
fun ProfileInfo(largeDataCache: LargeDataCache, profileInfoModel: ProfileInfoModel){
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val screens = LocalScreens.current
    val nav = LocalNavController.current
    val phoneParser: PhoneNumberParser? by produceState(null) {
        value = PhoneNumberParser(largeDataCache)
    }
    val isEditing by profileInfoModel.isEditing.collectAsState()
    val isExtended by profileInfoModel.isExtended.collectAsState()
    val focusManager = LocalFocusManager.current
    BackHandler(isEditing) {
        coroutineScope.launch {
            profileInfoModel.setStateToDb()
            profileInfoModel.isEditing.value = false
        }
        focusManager.clearFocus(true)
    }
    Box{
        Column(
            Modifier
                .fillMaxWidth(.95f)
                .background(Theme.surfaceContainerHigh, RoundedCornerShape(20.dp))
                .padding(10.dp),
        ) {
            val extendProgress by animateFloatAsState(if (isExtended) 1f else 0f, animationSpec = tween(animationDuration))
            val profileEntrySize =
                FontSize.MEDIUM.size.toDp() + 4.dp + 5.dp + FontSize.LARGE.size.toDp() + 5.dp + FontSize.SMALL.size.toDp() * 2 + 4.dp // Two line small  height
            val location by profileInfoModel.home.collectAsState()
            val navigationIconSize = FontSize.MEDIUM.size.toDp()
            val platforms by profileInfoModel.platforms.collectAsState()
            val iban by profileInfoModel.iban.collectAsState()
            val platformBarHeight = FontSize.MEDIUM.size.toDp() + 10.dp + 4.dp + FontSize.MEDIUM.size.toDp() + 10.dp
            val containerHight by animateDpAsState(
                (
                        if(isExtended)
                            (if(platforms.isNotEmpty() || isEditing) platformBarHeight + 20.dp else 0.dp) +
                                    (if(iban!=null || isEditing) profileEntrySize + 20.dp else 0.dp) +
                                    (if(location!=null && !isEditing) navigationIconSize + 10.dp + 10.dp + 5.dp else 0.dp ) +
                                    20.dp + profileEntrySize /* Location */
                        else
                            0.dp
                        ) +
                        profileEntrySize * 2 + 20.dp
                , animationSpec = tween(animationDuration)
            )
            Column(
                Modifier
                    .height(
                        containerHight
                    )
                    .clip(RectangleShape)
            ) {
                //val iban = "DE65430609673062465800"
                ListEntry(
                    "Voller Name",
                    painterResource(R.drawable.id_card)
                ) {
                    val fullNameState by profileInfoModel.fullName.collectAsState()
                    val displayText =
                        fullNameState?.let {
                            buildString {
                                val whiteSpaces = it.count { c-> c == ' ' }
                                when(whiteSpaces){
                                    0 -> append(it)
                                    1 -> {
                                        append(it.censorLast(
                                            (it.substringAfterLast(" ").length.minus(1) * (1 - extendProgress)).toInt(),
                                            " "
                                        ))
                                    }
                                    else  -> {
                                        append(it.substringBefore(" "))
                                        val between = " "+it.substringAfter(" ").substringBeforeLast(" ")
                                        val end = it.substringAfterLast(" ")
                                        val totalChars = between.length + end.length - 1
                                        val printedChars = (totalChars * extendProgress).roundToInt()
                                        append(between.take(printedChars))
                                        append(" ${end.take(1)}")
                                        if(printedChars>between.length-1) {
                                            append(end.substring(1).censorLast(end.length - (printedChars-between.length)-1, ""))
                                        }
                                    }
                                }
                            }
                        }
                    ListEditingField(
                        isEditing,
                        displayText ?: "???",
                        null,
                        profileInfoModel.fullName,
                        "Voller Name"
                    )
                }
                Spacer(Modifier.height(20.dp))
                ListEntry(
                    "Handynummer",
                    painterResource(R.drawable.phone)
                ) {
                    val phone by profileInfoModel.phone.collectAsState()
                    val formatedPhone = phone?.let { phoneParser?.parse(it) }
                    val censoredPlaces =
                        if (formatedPhone != null) (formatedPhone.substringAfter(" ").length * (1 - extendProgress)).roundToInt() else 3
                    val fullyFormatedNumber = formatedPhone?.censorLast(censoredPlaces, " •")
                    val savedInContacts by profileInfoModel.savedInContacts.collectAsState()
                    val subtext = if (!isExtended) null else phone?.let { phoneParser?.getPhoneInfo(it) }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier.weight(1f)
                        ) {
                            ListEditingField(
                                isEditing,
                                fullyFormatedNumber ?: "???",
                                subtext,
                                profileInfoModel.phone,
                                "Handynummer",
                                KeyboardType.Phone
                            )
                        }
                        if(phone != null && !savedInContacts) {
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                painterResource(R.drawable.save_contact),
                                "Save contact",
                                Modifier
                                    .clip(CircleShape)
                                    .background(Theme.primaryContainer)
                                    .rippleClick{
                                        AndroidContacts.openSaveContactIntent(context, phone, profileInfoModel.name.value)
                                        coroutineScope.launch {
                                            while (!profileInfoModel.savedInContacts.value) {
                                                delay(100)
                                                profileInfoModel.updateIsSavedInContacts(context)
                                            }
                                        }
                                    }
                                    .padding(7.dp)
                                    .size(20.dp)
                                ,
                                Theme.onPrimaryContainer
                            )
                        }
                    }
                }
                if(iban!=null || isEditing)  {
                    Spacer(Modifier.height(20.dp))
                    ListEntry(
                        "IBAN",
                        painterResource(R.drawable.pay_with_card)
                    ) {
                        val ibanInformation: String? =
                            iban?.let { if(it.length > 4) largeDataCache.getIbanInformation(it.substring(4)) else null }
                        ListEditingField(
                            isEditing,
                            iban?.chunked(4)?.joinToString(" ") ?: "???",
                            ibanInformation,
                            profileInfoModel.iban,
                            "IBAN"
                        )
                    }
                }
                if(platforms.isNotEmpty() || isEditing) {
                    Spacer(Modifier.height(20.dp))
                    Column(
                        Modifier
                            .height(platformBarHeight)
                        ,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("Platformen", style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
                        Row(
                            Modifier
                                .horizontalScroll(rememberScrollState())
                            ,
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val iconSize = FontSize.MEDIUM.size.toDp()
                            val height = iconSize + 10.dp + 4.dp
                            Row(
                                Modifier.height(height),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if(!isEditing) {
                                    PersonSyncable.getOrderedSocials(platforms).forEach {
                                        val context = LocalContext.current
                                        Row(
                                            Modifier
                                                .background(Theme.secondaryContainer, CircleShape)
                                                .clip(CircleShape)
                                                .rippleClick(isExtended) {
                                                    it.platform.openPlatform(
                                                        context,
                                                        it.handle,
                                                        profileInfoModel.phone.value
                                                    )
                                                }
                                                .padding(vertical = 5.dp, horizontal = 10.dp)
                                                .height(iconSize + 4.dp)
                                            ,
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                painterResource(it.platform.icon),
                                                it.platform.name,
                                                Modifier.size(iconSize),
                                                Theme.onSecondaryContainer,
                                            )
                                            Text(
                                                it.handle,
                                                style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM)
                                            )
                                        }
                                    }
                                } else {
                                    val platformInputs by profileInfoModel.platformInputs.collectAsState()
                                    platformInputs.forEachIndexed { i, it ->
                                        Row(
                                            Modifier
                                                .background(Theme.secondaryContainer, CircleShape)
                                                .padding(vertical = 5.dp, horizontal = 10.dp)
                                                .height(iconSize + 4.dp)
                                            ,
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            val focusManager = LocalFocusManager.current
                                            Icon(
                                                painterResource(R.drawable.close),
                                                "Delete Platform",
                                                Modifier
                                                    .size(iconSize)
                                                    .clip(CircleShape)
                                                    .rippleClick {
                                                        val cp =
                                                            profileInfoModel.platformInputs.value.toMutableList()
                                                        cp.removeAt(i)
                                                        profileInfoModel.platformInputs.value = cp
                                                    }
                                                ,
                                                Theme.onSecondaryContainer
                                            )
                                            BasicTextField(
                                                it,
                                                { new ->
                                                    val cp = profileInfoModel.platformInputs.value.toMutableList()
                                                    val newValue = if(new== PersonSyncable.Companion.Platform.WhatsApp.short) {focusManager.clearFocus(true); "wa@WhatsApp"} else new
                                                    cp[i] = newValue
                                                    profileInfoModel.platformInputs.value = cp
                                                },
                                                textStyle = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM),
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions{focusManager.clearFocus(true)},
                                                cursorBrush = SolidColor(Theme.onSecondaryContainer),
                                            )
                                        }
                                    }
                                    Row(
                                        Modifier
                                            .background(Theme.secondaryContainer, CircleShape)
                                            .clip(CircleShape)
                                            .rippleClick {
                                                val cp =
                                                    profileInfoModel.platformInputs.value.toMutableList()
                                                cp.add("")
                                                profileInfoModel.platformInputs.value = cp
                                            }
                                            .padding(vertical = 5.dp, horizontal = 10.dp)
                                            .height(iconSize + 4.dp)
                                        ,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Neu", style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                ListEntry(
                    "Adresse",
                    painterResource(R.drawable.location),
                ) {
                    if(!isEditing) {
                        ListEditingField(
                            false,
                            location?.toAddress(true) ?:"???",
                            location?.toCords(),
                            remember { MutableStateFlow(null) },
                            ""
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            location?.name?.let {
                                Text(it, style = TypoStyle(Theme.onSecondaryContainer, FontSize.LARGE))
                                Spacer(Modifier.width(10.dp))
                            }
                            Chip(
                                {
                                    nav.navigateForResult<String?>(
                                        "pick/existing/location",
                                        "pelocation"
                                    ) {
                                        if(it!=null){
                                            val parsedLoc = Location.fromJSON(JSONObject(it))
                                            profileInfoModel.home.value = parsedLoc
                                        } else {
                                            profileInfoModel.home.value = null
                                        }
                                    }
                                },
                                spacing = 5.dp,
                                color = Theme.secondaryContainer
                            ) {
                                Text(if(location!=null) "Ändern" else "Hinzufügen", style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM))
                                Icon(
                                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    "Continue",
                                    Modifier.size(FontSize.MEDIUM.size.toDp()),
                                    tint = Theme.onSecondaryContainer
                                )
                            }
                            if(location != null) {
                                Spacer(Modifier.width(5.dp))
                                Chip(
                                    {
                                        profileInfoModel.home.value = null
                                    },
                                    color = Theme.secondaryContainer
                                ) {
                                    Text("", style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM))
                                    Icon(
                                        painterResource(R.drawable.delete),
                                        "Remove",
                                        Modifier.size(FontSize.MEDIUM.size.toDp()),
                                        tint = Theme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val context = LocalContext.current
                    @Composable
                    fun NavigationOption(title: String, icon: Int, onClick: () -> Unit) {
                        Row (
                            Modifier
                                .border(2.dp, Theme.outlineVariant, CircleShape)
                                .clip(CircleShape)
                                .rippleClick(run = onClick)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                            ,
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ){
                            Icon(
                                painterResource(icon),
                                title,
                                Modifier.size(navigationIconSize),
                                Theme.primary
                            )
                            Text(title, style = TypoStyle(Theme.primary, FontSize.MEDIUM))
                        }
                    }
                    val screenWidthPx = LocalConfiguration.current.screenWidthDp.dp.toPx()
                    NavigationOption("Life Maps", R.drawable.gmaps) {
                        location?.also {
                            screens.openLocation(
                                it,
                                screenWidthPx
                            )
                        }
                    }
                    NavigationOption("HVV", R.drawable.hvv) {
                        AndroidUtils.openLink(
                            context,
                            HVV.constructLink(
                                null,
                                profileInfoModel.home.value
                            )
                        )
                    }
                    NavigationOption("Google Maps", R.drawable.gmaps) {
                        Location.openInGoogleMaps(context, profileInfoModel.home.value)
                    }
                }
            }
            val height = FontSize.MEDIUM.size.toDp() + 2.dp
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(height + 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val phoneNumber by profileInfoModel.phone.collectAsState()
                AnimatedVisibility(
                    !isEditing && phoneNumber!=null,
                    enter = fadeIn(tween(animationDuration)) + expandHorizontally(tween(animationDuration)),
                    exit = fadeOut(tween(animationDuration)) + shrinkHorizontally(tween(animationDuration))
                ) {
                    val features = LocalSettings.current.features
                    val context = LocalContext.current
                    Chip(
                        {
                            val number = ("tel:" + (profileInfoModel.phone.value ?: return@Chip)).toUri()
                            val intent = Intent(if(features.callFromLife.hasAssured()) Intent.ACTION_CALL else Intent.ACTION_DIAL)
                            intent.setData(number)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        color = Theme.secondaryContainer
                    ) {
                        Text(
                            "Anrufen",
                            Modifier.animateContentSize(tween(animationDuration)),
                            style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM)
                        )
                    }
                }
                AnimatedVisibility(
                    !isEditing,
                    enter = fadeIn(tween(animationDuration)) + expandHorizontally(tween(animationDuration)),
                    exit = fadeOut(tween(animationDuration)) + shrinkHorizontally(tween(animationDuration))
                ) {
                    val icon = platforms.getOrNull(0)?.platform?.icon ?: return@AnimatedVisibility
                    val context = LocalContext.current
                    Chip(
                        {
                            platforms.getOrNull(0)?.platform?.openPlatform(context, platforms.getOrNull(0)?.handle?:"", phoneNumber)
                        },
                        color = Theme.secondaryContainer
                    ) {
                        Box(
                            Modifier.animateContentSize(tween(animationDuration)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "",
                                style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM)
                            )
                            Icon(
                                painterResource(icon),
                                "Message",
                                Modifier.size(FontSize.MEDIUM.size.toDp()),
                                Theme.onSecondaryContainer
                            )
                        }
                    }
                }
                val lastInteraction by profileInfoModel.lastInteraction.collectAsState()
                AnimatedVisibility(
                    !isEditing,
                    enter = fadeIn(tween(animationDuration)) + expandHorizontally(tween(animationDuration)),
                    exit = fadeOut(tween(animationDuration)) + shrinkHorizontally(tween(animationDuration))
                ) {
                    if (lastInteraction != null) {
                        Chip(
                            {
                                screens.openSocialGraphWithNodeSelected(profileInfoModel.id.value, lastInteraction?.end)
                            },
                            color = Theme.secondaryContainer
                        ) {
                            Box(
                                Modifier.animateContentSize(tween(animationDuration)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("", style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM))
                                Icon(
                                    painterResource(R.drawable.graph),
                                    "Social Graph",
                                    Modifier.size(FontSize.MEDIUM.size.toDp()),
                                    Theme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                AnimatedVisibility(
                    !isEditing,
                    enter = fadeIn(tween(animationDuration)) + expandHorizontally(tween(animationDuration)),
                    exit = fadeOut(tween(animationDuration)) + shrinkHorizontally(tween(animationDuration))
                ) {
                    Chip(
                        {
                            profileInfoModel.isExtended.value = !isExtended
                        },
                        color = Theme.secondaryContainer
                    ) {
                        Text(
                            if (isExtended) "Weniger" else "Mehr",
                            Modifier.animateContentSize(tween(animationDuration)),
                            style = TypoStyle(Theme.onSecondaryContainer, FontSize.MEDIUM)
                        )
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            "Menu",
                            Modifier
                                .size(height)
                                .rotate(180f * extendProgress),
                            tint = Theme.onSecondaryContainer
                        )
                    }
                }
            }
        }
        Box( Modifier.align(Alignment.TopEnd)) {
            val offset by animateDpAsState(if(!isEditing) 0.dp else 30.dp+10.dp, tween(animationDuration))
            Box(
                Modifier
                    .offset(x = -offset)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .rippleClick {
                        coroutineScope.launch {
                            profileInfoModel.setStateToDb()
                            profileInfoModel.isEditing.value = false
                            focusManager.clearFocus(true)
                        }
                    }
                    .background(Theme.secondaryContainer, CircleShape)
                    .padding(5.dp)
                    .size(20.dp)
                ,
                contentAlignment = Alignment.Center
            ){
                Icon(
                    painterResource(R.drawable.close),
                    "Exit",
                    Modifier.fillMaxSize(),
                    Theme.onSecondaryContainer
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(CircleShape)
                .rippleClick {
                    if (isEditing) {
                        coroutineScope.launch {
                            profileInfoModel.saveAndSync()
                            focusManager.clearFocus(true)
                        }
                        profileInfoModel.isEditing.value = false
                    } else {
                        profileInfoModel.isEditing.value = true
                        profileInfoModel.isExtended.value = true
                    }
                }
                .background(Theme.secondaryContainer, CircleShape)
                .padding(5.dp)
                .size(20.dp)
            ,
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                !isEditing,
                exit = fadeOut(tween(animationDuration)) + scaleOut(tween(animationDuration)),
                enter = fadeIn(tween(animationDuration)) + scaleIn(tween(animationDuration)),
            ) {
                Icon(
                    painterResource(R.drawable.edit),
                    "Edit",
                    Modifier.fillMaxSize(),
                    Theme.onSecondaryContainer
                )
            }
            AnimatedVisibility(
                isEditing,
                exit = fadeOut(tween(animationDuration)) + scaleOut(tween(animationDuration)),
                enter = fadeIn(tween(animationDuration)) + scaleIn(tween(animationDuration)),
            ) {
                Icon(
                    painterResource(R.drawable.tick),
                    "Done",
                    Modifier.fillMaxSize(),
                    Theme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun ListEntry(title: String, icon: Painter, text: @Composable ()->Unit){
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(title, style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val height = FontSize.LARGE.size.toDp() + 5.dp + FontSize.SMALL.size.toDp()*2 + 4.dp // Text is not exactly fontsize small*2 for two line support
            Box(
                Modifier.height(height),
                contentAlignment = Alignment.Center
            )  {
                Icon(icon, "Icon", Modifier.size(30.dp), tint = Theme.secondary)
            }
            Column(
                Modifier.height(height),
                verticalArrangement = Arrangement.Center
            ) {
                text()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListEditingField(isEditing: Boolean, displayText: String, subtext: String?, text: MutableStateFlow<String?>, placeHolder: String, keyboardType: KeyboardType = KeyboardType.Text){
    if(isEditing) {
        val value by text.collectAsState()
        var hasFocus by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current
        BasicTextField(
            value?:"",
            {
                text.value = it
            },
            Modifier
                .onFocusChanged {
                    hasFocus = it.hasFocus
                }
                .fillMaxWidth(),
            textStyle = TypoStyle(Theme.primary, FontSize.LARGE),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
                keyboardType = keyboardType
            ),
            keyboardActions = KeyboardActions{
                focusManager.clearFocus()
            },
            cursorBrush = SolidColor(Theme.primary),
            decorationBox = {
                if(!hasFocus && value?.isBlank()?:true) Text(placeHolder, style = TypoStyle(Theme.secondary, FontSize.LARGE)) else it()
            }
        )
    } else {
        val clipboard = LocalClipboardManager.current
        Text(
            displayText,
            modifier = Modifier.combinedClickable(null, null, onLongClick = {
                if(displayText.isNotBlank() && displayText!="???") clipboard.setText(AnnotatedString(displayText))
            }){},
            style = TypoStyle(Theme.primary, FontSize.LARGE),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    val rememberedText = remember { Ref<String?>() }
    rememberedText.value = subtext ?: rememberedText.value
    Column {
        AnimatedVisibility(
            subtext!=null && !isEditing,
            enter = fadeIn(tween(animationDuration)) + expandVertically(tween(animationDuration)),
            exit = fadeOut(tween(animationDuration)) + shrinkVertically(tween(animationDuration)),
        ) {
            Spacer(Modifier.height(5.dp))
            Text(rememberedText.value?:"", style = TypoStyle(Theme.secondary, FontSize.SMALL))
        }
    }
}
fun String.censorLast(amount: Int, censor: String) = substring(0, (length - amount).coerceIn(0, length))+substring((length - amount).coerceIn(0, length)).map { c -> if(!c.isWhitespace()) censor else c }.joinToString("")

fun <T> NavController.navigateForResult(route: String, key: String, addAditonalContext: (NavigateForResultAditionalContext.()->Unit)? = null, onComplete: (T)->Unit){
    println("Navigating for result in $key")
    val handle = currentBackStackEntry?.savedStateHandle ?: return
    handle.remove<T>(key)
    val data = handle.getLiveData<T>(key)
    val additionalContext = if(addAditonalContext!=null) NavigateForResultAditionalContext(handle).apply { addAditonalContext() } else null
    navigate(route)
    val observer = object: Observer<T> {
        override fun onChanged(value: T) {
            println("Received result for $key")
            onComplete(value)
            data.removeObserver(this)
            handle.remove<T>(key)
            additionalContext?.clean()
        }
    }
    data.observeForever(observer)
}
class NavigateForResultAditionalContext(val savedStateHandle: SavedStateHandle){
    private val addedKeys = mutableSetOf<String>()
    fun set(key: String, value: String) {
        savedStateHandle[key] = value
        addedKeys.add(key)
    }
    fun remove(key: String){
        savedStateHandle.remove<String?>(key)
        addedKeys.remove(key)
    }
    fun setOrRemove(key: String, value: String?) = if(value == null) remove(key) else set(key, value)
    fun clean(){
        for (key in addedKeys) {
            savedStateHandle.remove<String>(key)
        }
    }
}