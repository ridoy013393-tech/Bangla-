package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CapturedLocation
import com.example.data.LocationHelper
import com.example.model.AppLanguage
import com.example.model.ChatMessage
import com.example.model.PublicProfile
import com.example.service.MessageReminderScheduler
import com.example.ui.components.UserAvatar
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OnlineGreen
import kotlinx.coroutines.launch

/**
 * Stylized inline Map Card bubble for shared GPS locations (`type == "location"`).
 */
@Composable
fun LocationMessageCard(
    message: ChatMessage,
    isMe: Boolean,
    textColor: Color,
    metaColor: Color,
    onOpenMap: () -> Unit
) {
    val strings = LocalAppStrings.current
    val coords = remember(message.latitude, message.longitude) {
        LocationHelper.formatCoordinates(message.latitude, message.longitude)
    }
    Column(
        modifier = Modifier
            .widthIn(min = 216.dp, max = 264.dp)
            .testTag("location_bubble_${message.id}")
    ) {
        // Mini stylized map canvas with grid roads + glowing pin
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(106.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0F766E), Color(0xFF0284C7))
                    )
                )
                .clickable(onClick = onOpenMap),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = Color.White.copy(alpha = 0.14f)
                val stepX = size.width / 6f
                val stepY = size.height / 4f
                for (i in 1..5) {
                    drawLine(gridColor, Offset(i * stepX, 0f), Offset(i * stepX, size.height), strokeWidth = 1.5f)
                }
                for (j in 1..3) {
                    drawLine(gridColor, Offset(0f, j * stepY), Offset(size.width, j * stepY), strokeWidth = 1.5f)
                }
                // Diagonal arterial road highlight
                drawLine(
                    color = Color(0xFFFDE047).copy(alpha = 0.45f),
                    start = Offset(0f, size.height * 0.78f),
                    end = Offset(size.width, size.height * 0.22f),
                    strokeWidth = 5f
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = strings.locationMessagePreview,
                    tint = Color(0xFFFFE4E6),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message.locationLabel.ifBlank { strings.locationMessagePreview },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = coords,
            style = MaterialTheme.typography.labelSmall,
            color = metaColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            onClick = onOpenMap,
            shape = RoundedCornerShape(12.dp),
            color = if (isMe) Color.White.copy(alpha = 0.22f) else BrandBlue.copy(alpha = 0.14f),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_maps_button_${message.id}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Map,
                    contentDescription = null,
                    tint = if (isMe) Color.White else BrandBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.openInGoogleMaps,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isMe) Color.White else BrandBlue
                )
            }
        }
    }
}

/**
 * Inline Contact Card bubble (`type == "contact"`) with 1-tap direct chat launch.
 */
@Composable
fun ContactMessageCard(
    message: ChatMessage,
    isMe: Boolean,
    textColor: Color,
    metaColor: Color,
    canOpenChat: Boolean,
    onMessageContact: () -> Unit
) {
    val strings = LocalAppStrings.current
    val name = message.sharedContactName.ifBlank { "Bangla Chat User" }
    val username = message.sharedContactUsername.trim().removePrefix("@")

    Column(
        modifier = Modifier
            .widthIn(min = 216.dp, max = 264.dp)
            .testTag("contact_bubble_${message.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
                name = name,
                photoUrl = message.sharedContactPhotoUrl,
                size = 44.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (username.isNotBlank()) {
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.labelMedium,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Rounded.ContactPage,
                contentDescription = null,
                tint = metaColor,
                modifier = Modifier.size(20.dp)
            )
        }
        if (canOpenChat) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                onClick = onMessageContact,
                shape = RoundedCornerShape(12.dp),
                color = if (isMe) Color.White.copy(alpha = 0.22f) else BrandBlue.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("message_shared_contact_${message.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubble,
                        contentDescription = null,
                        tint = if (isMe) Color.White else BrandBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.messageSharedContactAction,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMe) Color.White else BrandBlue
                    )
                }
            }
        }
    }
}

/**
 * Sheet to request foreground GPS permission, fetch current coordinates, and send a Location Card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLocationSheet(
    onSendLocation: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(LocationHelper.hasLocationPermission(context)) }
    var isFetching by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf<CapturedLocation?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var placeLabel by remember { mutableStateOf("") }

    fun fetchNow() {
        scope.launch {
            isFetching = true
            errorMsg = null
            val loc = LocationHelper.fetchCurrentLocation(context)
            isFetching = false
            if (loc != null) {
                captured = loc
            } else {
                errorMsg = strings.locationFetchFailed
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPermission = granted
        if (granted) {
            fetchNow()
        } else {
            errorMsg = strings.locationPermissionDenied
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && captured == null && !isFetching) {
            fetchNow()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(OnlineGreen.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        tint = OnlineGreen
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.shareLocationAction,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.shareLocationSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!hasPermission) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.locationPermissionExplain,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                permLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("grant_location_permission_button")
                        ) {
                            Text(strings.allowLocationAction)
                        }
                    }
                }
            } else if (isFetching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = BrandBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(strings.fetchingLocationStatus, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                captured?.let { loc ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = OnlineGreen.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = OnlineGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.locationReadyLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnlineGreen
                                )
                                Text(
                                    text = LocationHelper.formatCoordinates(loc.latitude, loc.longitude),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            OutlinedButton(onClick = { fetchNow() }) {
                                Text(strings.retry)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = placeLabel,
                        onValueChange = { placeLabel = it.take(120) },
                        label = { Text(strings.locationLabelHint) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("location_label_input")
                    )

                    Button(
                        onClick = {
                            onSendLocation(
                                loc.latitude,
                                loc.longitude,
                                placeLabel.trim().ifBlank { strings.currentLocationDefaultLabel }
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_location_confirm_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.sendLocationAction, fontWeight = FontWeight.Bold)
                    }
                }
            }

            errorMsg?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed
                )
                if (hasPermission && captured == null && !isFetching) {
                    OutlinedButton(
                        onClick = { fetchNow() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.retry)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * Sheet to pick a registered contact (or search exact @username) and send a Contact Card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareContactSheet(
    contacts: List<PublicProfile>,
    onLookupUsername: suspend (String) -> Result<PublicProfile?>,
    onSelectContact: (PublicProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var lookedUpProfile by remember { mutableStateOf<PublicProfile?>(null) }
    var notFound by remember { mutableStateOf(false) }

    val filtered = remember(contacts, query) {
        val q = query.trim().removePrefix("@").lowercase()
        if (q.isBlank()) contacts
        else contacts.filter {
            it.name.lowercase().contains(q) || it.username.lowercase().contains(q)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = strings.shareContactAction,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        notFound = false
                        lookedUpProfile = null
                    },
                    placeholder = { Text(strings.groupSearchMembersHint) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("share_contact_search_input")
                )
                Button(
                    onClick = {
                        val clean = query.trim()
                        if (clean.isNotEmpty()) {
                            scope.launch {
                                searching = true
                                notFound = false
                                val res = onLookupUsername(clean)
                                searching = false
                                lookedUpProfile = res.getOrNull()
                                notFound = lookedUpProfile == null
                            }
                        }
                    },
                    enabled = !searching && query.trim().length >= 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    if (searching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text(strings.searchButton)
                    }
                }
            }

            lookedUpProfile?.let { found ->
                ContactPickerRow(
                    profile = found,
                    onClick = {
                        onSelectContact(found)
                        onDismiss()
                    }
                )
                HorizontalDivider()
            }

            if (notFound) {
                Text(
                    text = strings.exactUsernameNotFound,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (filtered.isEmpty() && lookedUpProfile == null) {
                Text(
                    text = strings.shareContactEmptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 18.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.uid }) { contact ->
                        ContactPickerRow(
                            profile = contact,
                            onClick = {
                                onSelectContact(contact)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactPickerRow(
    profile: PublicProfile,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("pick_contact_${profile.uid}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(name = profile.name, photoUrl = profile.photoUrl, size = 42.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (profile.username.isNotBlank()) {
                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandBlue
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Quick Reply Templates Sheet (#8): built-in Bangla/English presets + custom user-saved templates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickReplyTemplatesSheet(
    language: AppLanguage,
    customTemplates: List<String>,
    onSendNow: (String) -> Unit,
    onInsertIntoInput: (String) -> Unit,
    onAddCustomTemplate: (String) -> Unit,
    onRemoveCustomTemplate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var newTemplate by remember { mutableStateOf("") }

    val defaultTemplates = remember(language) {
        if (language == AppLanguage.BANGLA) {
            listOf(
                "আসসালামু আলাইকুম, কেমন আছেন?",
                "একটু ব্যস্ত আছি, পরে কল দিচ্ছি।",
                "আমি রাস্তায় আছি, ৫ মিনিটে পৌঁছাচ্ছি।",
                "ঠিক আছে, ধন্যবাদ!",
                "পরে কথা হবে ইনশাআল্লাহ।",
                "আমাকে একটু লোকেশনটা পাঠাবেন?"
            )
        } else {
            listOf(
                "Assalamu Alaikum, how are you?",
                "Busy right now, I'll call you back soon.",
                "On my way, reaching in 5 minutes!",
                "Sounds good, thank you!",
                "Talk to you later, InshaAllah.",
                "Could you please share your location?"
            )
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFFF59E0B))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = strings.quickReplyTemplatesTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.quickReplyTemplatesSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Add new custom quick reply
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTemplate,
                    onValueChange = { newTemplate = it.take(180) },
                    placeholder = { Text(strings.quickReplyAddHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_quick_reply_input")
                )
                IconButton(
                    onClick = {
                        val clean = newTemplate.trim()
                        if (clean.isNotEmpty()) {
                            onAddCustomTemplate(clean)
                            newTemplate = ""
                        }
                    },
                    enabled = newTemplate.trim().isNotEmpty(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(BrandBlue)
                        .testTag("add_custom_quick_reply_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = strings.adminAddWordAction,
                        tint = Color.White
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (customTemplates.isNotEmpty()) {
                    item(key = "hdr_custom") {
                        Text(
                            text = strings.quickReplyCustomSection,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandPurple
                        )
                    }
                    items(customTemplates, key = { "custom_$it" }) { tpl ->
                        QuickReplyTemplateRow(
                            text = tpl,
                            isCustom = true,
                            onSend = {
                                onSendNow(tpl)
                                onDismiss()
                            },
                            onInsert = {
                                onInsertIntoInput(tpl)
                                onDismiss()
                            },
                            onDelete = { onRemoveCustomTemplate(tpl) }
                        )
                    }
                }

                item(key = "hdr_presets") {
                    Text(
                        text = strings.quickReplyPresetSection,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )
                }
                items(defaultTemplates, key = { "preset_$it" }) { tpl ->
                    QuickReplyTemplateRow(
                        text = tpl,
                        isCustom = false,
                        onSend = {
                            onSendNow(tpl)
                            onDismiss()
                        },
                        onInsert = {
                            onInsertIntoInput(tpl)
                            onDismiss()
                        },
                        onDelete = null
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickReplyTemplateRow(
    text: String,
    isCustom: Boolean,
    onSend: () -> Unit,
    onInsert: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val strings = LocalAppStrings.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSend)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onInsert) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = strings.editMessage,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (isCustom && onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = strings.deleteMessage,
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(onClick = onSend) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = strings.send,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Sheet to pick when to be reminded about a specific message (#8).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageReminderSheet(
    messagePreview: String,
    onPickTriggerMillis: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val now = System.currentTimeMillis()
    val options = listOf(
        Triple(strings.reminderIn10Min, now + 10L * 60_000L, "10m"),
        Triple(strings.reminderIn30Min, now + 30L * 60_000L, "30m"),
        Triple(strings.reminderIn1Hour, now + 60L * 60_000L, "1h"),
        Triple(strings.reminderIn3Hours, now + 180L * 60_000L, "3h"),
        Triple(strings.reminderTomorrow9Am, MessageReminderScheduler.tomorrowMorning9AmMillis(now), "tomorrow")
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BrandPurple.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Alarm, contentDescription = null, tint = BrandPurple)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.remindMeTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = messagePreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            options.forEach { (label, triggerAt, tag) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            onPickTriggerMillis(triggerAt, label)
                            onDismiss()
                        }
                        .testTag("reminder_option_$tag")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Alarm, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
