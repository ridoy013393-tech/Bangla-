package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BanglaChatRepository
import com.example.model.BlockedUser
import com.example.model.FavoriteContact
import com.example.model.PublicProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatFullDate
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed
import com.example.viewmodel.UsernameSearchState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsTab(
    usernameSearchState: UsernameSearchState,
    knownProfiles: List<PublicProfile>,
    favorites: List<FavoriteContact>,
    blockedUsers: List<BlockedUser>,
    onSearchUsername: (String) -> Unit,
    onClearUsernameSearch: () -> Unit,
    onStartChat: (PublicProfile) -> Unit,
    onStartCall: (PublicProfile, String) -> Unit,
    onToggleFavorite: (PublicProfile, Boolean) -> Unit,
    onBlockUser: (PublicProfile) -> Unit,
    onUnblockUser: (String) -> Unit,
    onSubmitReport: (String, String, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedProfile by remember { mutableStateOf<PublicProfile?>(null) }
    var reportTarget by remember { mutableStateOf<PublicProfile?>(null) }

    val favoriteUids = remember(favorites) { favorites.map { it.favoriteUid }.toSet() }
    val blockedUids = remember(blockedUsers) { blockedUsers.map { it.blockedUid }.toSet() }

    // Debounced exact @username lookup while typing
    LaunchedEffect(query) {
        val clean = BanglaChatRepository.normalizeUsername(query)
        if (clean.isBlank()) {
            onClearUsernameSearch()
        } else if (clean.length >= 3) {
            delay(420L)
            onSearchUsername(clean)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // WhatsApp-style exact @username search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { raw ->
                    query = raw.trimStart('@').replace(" ", "").take(30)
                },
                placeholder = {
                    Text(
                        text = strings.searchExactUsernameHint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                prefix = {
                    Text(
                        text = "@",
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                query = ""
                                onClearUsernameSearch()
                            },
                            modifier = Modifier.testTag("contacts_clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.close
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        if (query.isNotBlank()) {
                            onSearchUsername(query)
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("contacts_search_input")
            )

            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (query.isNotBlank()) {
                        onSearchUsername(query)
                    }
                },
                enabled = query.isNotBlank(),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                modifier = Modifier.testTag("contacts_search_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = strings.searchButton,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("contacts_list"),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Saved favorites stay directly accessible
            if (favorites.isNotEmpty()) {
                item(key = "favorites_header") {
                    Text(
                        text = strings.favoritesTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = BrandPurple,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item(key = "favorites_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(favorites, key = { it.favoriteUid }) { fav ->
                            val fullProfile = knownProfiles.firstOrNull { it.uid == fav.favoriteUid }
                                ?: PublicProfile(
                                    uid = fav.favoriteUid,
                                    name = fav.name,
                                    username = fav.username,
                                    photoUrl = fav.photoUrl
                                )
                            Column(
                                modifier = Modifier
                                    .width(74.dp)
                                    .clickable { onStartChat(fullProfile) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                UserAvatar(
                                    name = fav.name,
                                    photoUrl = fav.photoUrl,
                                    size = 54.dp,
                                    showOnlineIndicator = true,
                                    isOnline = fullProfile.showOnlineStatus && fullProfile.online
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = fav.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (fav.username.isNotBlank()) {
                                    Text(
                                        text = "@${fav.username}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandBlue,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            when (usernameSearchState) {
                is UsernameSearchState.Idle -> {
                    item(key = "exact_username_guide") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(BrandBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AlternateEmail,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = strings.exactUsernameGuideTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = strings.exactUsernameGuideSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = BrandBlue.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "@ridoy1234",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                is UsernameSearchState.Searching -> {
                    item(key = "searching_indicator") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = BrandBlue)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "@${usernameSearchState.normalizedQuery}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                is UsernameSearchState.Error -> {
                    item(key = "search_error") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = usernameSearchState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is UsernameSearchState.NotFound -> {
                    item(key = "exact_not_found") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "@${usernameSearchState.normalizedQuery}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = strings.exactUsernameNotFound,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is UsernameSearchState.Found -> {
                    val profile = usernameSearchState.profile
                    val isFav = profile.uid in favoriteUids
                    val isBlocked = profile.uid in blockedUids

                    item(key = "match_header") {
                        Text(
                            text = strings.exactUsernameMatchTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    item(key = "match_${profile.uid}") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { selectedProfile = profile }
                                .testTag("contact_card_${profile.uid}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        name = profile.name,
                                        photoUrl = profile.visiblePhotoUrl(),
                                        size = 56.dp,
                                        showOnlineIndicator = true,
                                        isOnline = profile.showOnlineStatus && profile.online
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "@${profile.username}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = BrandBlue,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        val bio = profile.visibleAbout()
                                        if (bio.isNotBlank()) {
                                            Text(
                                                text = bio,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onToggleFavorite(profile, isFav) },
                                        modifier = Modifier.testTag("fav_btn_${profile.uid}")
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                            contentDescription = if (isFav) strings.removeFavorite else strings.addFavorite,
                                            tint = if (isFav) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!isBlocked) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onStartChat(profile) },
                                            shape = RoundedCornerShape(18.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = BrandBlue,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("msg_btn_${profile.uid}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Chat,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(strings.startChatAction)
                                        }
                                        OutlinedButton(
                                            onClick = { onStartCall(profile, "audio") },
                                            shape = RoundedCornerShape(18.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            modifier = Modifier.testTag("call_audio_btn_${profile.uid}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Call,
                                                contentDescription = strings.voiceCallAction,
                                                tint = BrandBlue,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onStartCall(profile, "video") },
                                            shape = RoundedCornerShape(18.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            modifier = Modifier.testTag("call_video_btn_${profile.uid}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Videocam,
                                                contentDescription = strings.videoCallAction,
                                                tint = BrandPurple,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Profile inspection bottom sheet
    selectedProfile?.let { profile ->
        val isFav = profile.uid in favoriteUids
        val isBlocked = profile.uid in blockedUids
        ModalBottomSheet(onDismissRequest = { selectedProfile = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(
                    name = profile.name,
                    photoUrl = profile.visiblePhotoUrl(),
                    size = 82.dp,
                    showOnlineIndicator = true,
                    isOnline = profile.showOnlineStatus && profile.online
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${profile.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandBlue
                )
                if (profile.phoneLookup.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = profile.phoneLookup,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val visibleBio = profile.visibleAbout()
                if (visibleBio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = visibleBio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${strings.accountCreatedLabel}: ${formatFullDate(profile.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))

                if (!isBlocked) {
                    Button(
                        onClick = {
                            selectedProfile = null
                            onStartChat(profile)
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sheet_start_chat_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.startChatAction)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedButton(
                    onClick = { onToggleFavorite(profile, isFav) },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        if (isFav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isFav) strings.removeFavorite else strings.addFavorite)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isBlocked) onUnblockUser(profile.uid) else onBlockUser(profile)
                            selectedProfile = null
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBlocked) strings.unblockUser else strings.blockUser)
                    }
                    OutlinedButton(
                        onClick = {
                            val target = profile
                            selectedProfile = null
                            reportTarget = target
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.reportUser)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    reportTarget?.let { target ->
        ReportUserDialog(
            targetName = target.name,
            onDismiss = { reportTarget = null },
            onSubmit = { reason, details ->
                onSubmitReport(target.uid, reason, details)
                reportTarget = null
            }
        )
    }
}

@Composable
fun ReportUserDialog(
    targetName: String,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val strings = LocalAppStrings.current
    val reasons = listOf(
        strings.reportReasonSpam,
        strings.reportReasonHarassment,
        strings.reportReasonFake,
        strings.reportReasonOther
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.reportTitle}: $targetName") },
        text = {
            Column {
                reasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it.take(400) },
                    placeholder = { Text(strings.reportDetailsHint) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, details) }) {
                Text(strings.submitReport)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
