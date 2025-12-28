package com.myxoz.life.subscreens

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalSettings
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.API
import com.myxoz.life.api.PersonSyncable
import com.myxoz.life.calendar.InputField
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.MaterialShapes
import com.myxoz.life.utils.combinedRippleClick
import com.myxoz.life.utils.filteredWith
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.toDp
import com.myxoz.life.utils.toPx
import com.myxoz.life.utils.toShape
import com.myxoz.life.viewmodels.ContactsViewModel
import com.myxoz.life.viewmodels.ProfileInfoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun Contacts(contactsViewModel: ContactsViewModel, personViewModel: ProfileInfoModel){
    Scaffold(
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
            ,
            verticalArrangement = Arrangement.Bottom,
        ) {
            val context = LocalContext.current
            val db = LocalStorage.current
            val nav = LocalNavController.current
            val settings = LocalSettings.current
            val screenWidthPx = LocalConfiguration.current.screenWidthDp.dp.toPx(LocalDensity.current)
            val lifeContacts by contactsViewModel.lifeContacts.collectAsState()
            val deviceContacts by contactsViewModel.deviceContacts.collectAsState()
            val hasContactPermission by settings.features.addNewPerson.has.collectAsState()
            var search by remember { mutableStateOf("") }
            val showIcons by contactsViewModel.showIcons.collectAsState()
            val ordering = remember {
                arrayOf('F', '*', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'C')
            }
            val favoriteIds = remember {
                (db.prefs.getStringSet("favorite_people", setOf())  ?: setOf()).mapNotNull { it.toLongOrNull() }.toMutableStateList()
            }
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO){
                    contactsViewModel.refetchLifeContacts(db)
                    if(hasContactPermission) contactsViewModel.fetchDeviceContacts(db, context)
                }
            }
            val filteredLifeContacts = remember(search, lifeContacts.hashCode() /* Idk if needed but lists... */) {
                lifeContacts.filteredWith(search, {it.fullName?:""}) {it.name}
            }
            val filteredDeviceContacts = remember(search, deviceContacts.hashCode()) {
                deviceContacts
                    .filteredWith(search, {it.fullName?:""}) {it.name}
            }
            val scrollState = rememberScrollState(contactsViewModel.scrollDistance)
            contactsViewModel.scrollDistance = scrollState.value
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .edgeToEdgeGradient(Colors.BACKGROUND, innerPadding)
                ,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                reverseLayout = true
            ) {
                item {  //  End due to reverse layout
                    Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
                }
                ordering.forEach { letter ->
                    val items = when (letter) {
                        'F' -> filteredLifeContacts.filter { favoriteIds.contains(it.id) }
                        '*' -> filteredLifeContacts.filter { it.name.getOrNull(0)?.isLetter() == false && !favoriteIds.contains(it.id)}
                        'C' -> filteredDeviceContacts
                        else -> filteredLifeContacts.filter { it.name.getOrNull(0)?.lowercase()?.getOrNull(0) == letter && !favoriteIds.contains(it.id) }
                    }

                    if(items.isEmpty()) return@forEach

                    // Header item
                    items(items, key = { "${letter}_${it.id}_${it.name}_${it.phoneNumber}" }) { contact ->
                        val isFirst = contact == items.firstOrNull()
                        val isLast = contact == items.lastOrNull()
                        val shape = RoundedCornerShape( // Flip due to reverse Layout (tf)
                            if(isLast) 20.dp else 10.dp,
                            if(isLast) 20.dp else 10.dp,
                            if(isFirst) 20.dp else 10.dp,
                            if(isFirst) 20.dp else 10.dp,
                        )
                        val offsetX = remember { Animatable(0f) }
                        val coroutineScope = rememberCoroutineScope()
                        val swipedRight = offsetX.value > 0
                        val platform = if(!swipedRight) PersonSyncable.getOrderedSocials(contact.socials).getOrNull(0)?.platform else null
                        Box(
                            Modifier
                                .fillMaxWidth(.95f)
                                .padding(vertical = 1.dp)
                                .draggable(
                                    rememberDraggableState {
                                        coroutineScope.launch {
                                            offsetX.snapTo(offsetX.value + it)
                                        }
                                    },
                                    Orientation.Horizontal,
                                    onDragStopped = {
                                        if (swipedRight) {
                                            /* Empirically set this */
                                            if (it > 1000f && contact.phoneNumber != null) {
                                                val number = ("tel:" + contact.phoneNumber).toUri()
                                                val intent =
                                                    Intent(if (settings.features.callFromLife.hasAssured()) Intent.ACTION_CALL else Intent.ACTION_DIAL)
                                                intent.setData(number)
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            }
                                        } else {
                                            if (it < -1000f && platform != null) {
                                                platform.openPlatform(
                                                    context,
                                                    contact.socials[0].handle,
                                                    contact.phoneNumber
                                                )
                                                delay(1000)
                                            }
                                        }
                                        offsetX.animateTo(targetValue = 0f)
                                    }
                                )
                        ) {
                            Row(
                                Modifier
                                    .matchParentSize()
                                    .background(
                                        if (offsetX.value == 0f) Colors.SECONDARY else if (swipedRight) if (contact.phoneNumber != null) Colors.Transactions.PLUS else Colors.PRIMARYFONT else platform?.color
                                            ?: Colors.PRIMARYFONT, shape
                                    )
                                    .padding(horizontal = 10.dp)
                                ,
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp, if(swipedRight) Alignment.Start else Alignment.End)
                            ){
                                val fontSize = FontSize.LARGE.size
                                if(swipedRight) {
                                    val hasPhone = contact.phoneNumber!=null
                                    Icon(painterResource(if(hasPhone) R.drawable.phone else R.drawable.close), "Call", Modifier.size(fontSize.toDp()), tint = Colors.BACKGROUND)
                                    Text(if(hasPhone) "Anrufen" else "Keine Nummer hinterlegt", style = TextStyle.Default.copy(color = Colors.BACKGROUND, fontSize = fontSize))
                                } else {
                                    Icon(painterResource(platform?.icon?:R.drawable.close), "Call", Modifier.size(fontSize.toDp()), tint = Colors.BACKGROUND)
                                    Text(platform?.fullName?:"Keine Platform", style = TextStyle.Default.copy(color = Colors.BACKGROUND, fontSize = fontSize))
                                }
                            }
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                                    .alpha(
                                        1 - (abs(offsetX.value) / screenWidthPx * 2).coerceIn(
                                            0f,
                                            1f
                                        )
                                    )
                                    .background(Colors.SECONDARY, shape)
                            ) {
                                Row(
                                    Modifier
                                        .clip(shape)
                                        .fillMaxWidth()
                                        .rippleClick {
                                            if (contact.id != 0L) {
                                                coroutineScope.launch {
                                                    personViewModel.openPersonDetails(
                                                        contact.id,
                                                        nav,
                                                        db,
                                                        context
                                                    )
                                                }
                                            } else {
                                                // Open existing contact by number
                                                val phoneNumber =
                                                    contact.phoneNumber ?: return@rippleClick
                                                val uri = Uri.withAppendedPath(
                                                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                                                    Uri.encode(phoneNumber)
                                                )
                                                val cursor = context.contentResolver.query(
                                                    uri,
                                                    arrayOf(ContactsContract.PhoneLookup._ID),
                                                    null, null, null
                                                )
                                                if (cursor != null && cursor.moveToFirst()) {
                                                    val contactId = cursor.getLong(
                                                        cursor.getColumnIndexOrThrow(
                                                            ContactsContract.PhoneLookup._ID
                                                        )
                                                    )
                                                    cursor.close()
                                                    val contactUri = ContentUris.withAppendedId(
                                                        ContactsContract.Contacts.CONTENT_URI,
                                                        contactId
                                                    )
                                                    val intent = Intent(Intent.ACTION_VIEW)
                                                    intent.setData(contactUri)
                                                    context.startActivity(intent)
                                                }
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 10.dp)
                                    ,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val fontSize = FontSize.LARGE.size.toDp()
                                    Row(
                                        Modifier
                                            .fillMaxHeight()
                                            .weight(1f),
                                       verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            contact.name,
                                            Modifier,
                                            style = TypoStyle(FontColor.PRIMARY, FontSize.LARGE)
                                        )
                                        if(!showIcons) return@Row
                                        val textSize = FontSize.LARGE.size.toDp()
                                        val icons = remember {
                                            val list = mutableListOf(
                                                contact.home?.let { R.drawable.house },
                                                contact.iban?.let { R.drawable.pay_with_card },
                                                contact.phoneNumber?.let { R.drawable.phone },
                                                contact.fullName?.let { R.drawable.id_card },
                                                contact.birthday?.let { R.drawable.birthday },
                                            )
                                            list.addAll(contact.socials.map { it.platform.icon })
                                            list
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            for (i in icons) {
                                                if(i == null) continue
                                                Icon(painterResource(i), null, Modifier.size(textSize * .8f), tint = Colors.TERTIARYFONT)
                                            }
                                        }
                                    }
                                    if(letter!='C') {
                                        Box(Modifier
                                            .size(fontSize)
                                            .background(
                                                if (letter == 'F') Colors.PRIMARYFONT else Colors.TERTIARYFONT,
                                                MaterialShapes.Flower.toShape()
                                            )
                                            .clip(CircleShape)
                                            .rippleClick {
                                                if (letter != 'F' && !favoriteIds.contains(contact.id)) {
                                                    favoriteIds.add(contact.id)
                                                } else {
                                                    favoriteIds.remove(contact.id)
                                                }
                                                db.prefs.edit {
                                                    putStringSet(
                                                        "favorite_people",
                                                        favoriteIds.map { it.toString() }.toSet()
                                                    )
                                                }
                                            }
                                        )
                                    } else {
                                        Box(Modifier
                                            .size(fontSize)
                                            .clip(CircleShape)
                                            .rippleClick {
                                                coroutineScope.launch {
                                                    PersonSyncable(
                                                        API.generateId(),
                                                        contact.name,
                                                        null,
                                                        contact.phoneNumber,
                                                        null,
                                                        null,
                                                        null,
                                                        contact.socials
                                                    ).saveAndSync(db)
                                                    contactsViewModel.refetchLifeContacts(db)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Add, "New", Modifier.fillMaxSize(), Colors.PRIMARYFONT)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item(key = "header_$letter") { // Due to reverse  Layout
                        Text(
                            when(letter){ 'F' -> "Favoriten"; 'C' -> "Kontakte"; else -> letter.uppercase()},
                            Modifier
                                .fillMaxWidth(.95f)
                                .padding(top = 20.dp, bottom = 5.dp),
                            style = TypoStyle(FontColor.SECONDARY, FontSize.LARGE)
                        )
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                ,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                ) {
                    InputField(
                        null,
                        "Suchen",
                    ) {
                        search = it
                    }
                }
                val lineHeight = FontSize.LARGE.size.toDp() + 16.dp  * 2 /* TextFieldPadding * 2 */
                val shape = MaterialShapes.Cookie12Sided.toShape()
                val coroutineScope = rememberCoroutineScope()
                Box(
                    Modifier
                        .size(lineHeight)
                        .background(Colors.SECONDARY, shape)
                        .padding(5.dp)
                        .clip(shape)
                        .combinedRippleClick(
                            {
                                if (search.isNotBlank()) {
                                    coroutineScope.launch {
                                        PersonSyncable(
                                            API.generateId(),
                                            search,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            listOf(),
                                        ).saveAndSync(db)
                                        contactsViewModel.refetchLifeContacts(db)
                                        Toast.makeText(context, "Erstellt!", Toast.LENGTH_LONG)
                                            .show()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Gib einen Namen ein",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        ) {
                            Toast.makeText(
                                context,
                                "Halte gedrückt zum erstellen",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                ) {
                    Icon(Icons.Rounded.Add, "New", Modifier.fillMaxSize(), Colors.PRIMARYFONT)
                }
            }
        }
        Box(
            Modifier.fillMaxSize()
        ) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(innerPadding)
                    .padding(10.dp)
                    .background(Colors.SECONDARY, CircleShape)
                    .border(1.dp, Colors.TERTIARYFONT, CircleShape)
                    .clip(CircleShape)
                    .rippleClick {
                        contactsViewModel.showIcons.value = !contactsViewModel.showIcons.value
                    }
                    .padding(10.dp)
            ) {
                val visible by contactsViewModel.showIcons.collectAsState()
                Icon(
                    painterResource(if(visible) R.drawable.visible else R.drawable.visible_off),
                    "Toggle Icons",
                    Modifier.size(20.dp),
                    tint = Colors.SECONDARYFONT
                )
            }
        }
    }
}
