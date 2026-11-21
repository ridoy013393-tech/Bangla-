package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.BanglaChatRepository
import com.example.model.Chat
import com.example.model.PublicProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OnlineGreen
import kotlinx.coroutines.launch

@Composable
fun CreateGroupDialog(
    availableContacts: List<PublicProfile>,
    onLookupUsername: suspend (String) -> Result<PublicProfile?> = { Result.success(null) },
    onCreateGroup: (groupName: String, groupDescription: String, members: List<PublicProfile>, photoUri: Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var lookupLoading by remember { mutableStateOf(false) }
    var lookupFeedback by remember { mutableStateOf<String?>(null) }
    var discoveredExtras by remember { mutableStateOf<List<PublicProfile>>(emptyList()) }
    var selectedMap by remember { mutableStateOf<Map<String, PublicProfile>>(emptyMap()) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val maxSelectable = Chat.MAX_GROUP_MEMBERS - 1 // Creator + up to 49 others = 50

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) photoUri = uri }

    val allCandidates = remember(availableContacts, discoveredExtras) {
        (discoveredExtras + availableContacts).distinctBy { it.uid }
    }
    val filteredCandidates = remember(allCandidates, searchQuery) {
        val q = searchQuery.trim().trimStart('@').lowercase()
        if (q.isBlank()) allCandidates
        else allCandidates.filter {
            it.name.lowercase().contains(q) || it.username.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newGroup, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("create_group_photo_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = strings.changeGroupPhoto,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(58.dp)
                            )
                        } else {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = strings.changeGroupPhoto, tint = BrandBlue)
                        }
                    }
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it.take(80) },
                        placeholder = { Text(strings.groupNameHint) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_group_name_input")
                    )
                }

                OutlinedTextField(
                    value = groupDescription,
                    onValueChange = { groupDescription = it.take(300) },
                    placeholder = { Text(strings.groupDescriptionHint) },
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_group_description_input")
                )

                // Search contacts or look up any registered user by exact @username
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it.take(40)
                            lookupFeedback = null
                        },
                        placeholder = { Text(strings.groupAddByUsernameHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("create_group_member_search")
                    )
                    val cleanHandle = BanglaChatRepository.normalizeUsername(searchQuery)
                    if (BanglaChatRepository.isValidUsernameFormat(cleanHandle)) {
                        IconButton(
                            enabled = !lookupLoading,
                            onClick = {
                                scope.launch {
                                    lookupLoading = true
                                    lookupFeedback = null
                                    onLookupUsername(cleanHandle)
                                        .onSuccess { profile ->
                                            if (profile != null) {
                                                discoveredExtras = (listOf(profile) + discoveredExtras).distinctBy { it.uid }
                                                if (selectedMap.size < maxSelectable) {
                                                    selectedMap = selectedMap + (profile.uid to profile)
                                                }
                                            } else {
                                                lookupFeedback = strings.exactUsernameNotFound
                                            }
                                        }
                                        .onFailure { err ->
                                            lookupFeedback = err.localizedMessage ?: strings.exactUsernameNotFound
                                        }
                                    lookupLoading = false
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BrandBlue.copy(alpha = 0.14f))
                                .testTag("create_group_lookup_username_button")
                        ) {
                            if (lookupLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = strings.searchButton, tint = BrandBlue)
                            }
                        }
                    }
                }

                lookupFeedback?.let { feedback ->
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.labelSmall,
                        color = DangerRed
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.selectGroupMembers} (${selectedMap.size}/$maxSelectable)",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (selectedMap.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedMap.values.toList(), key = { "chip_${it.uid}" }) { member ->
                            InputChip(
                                selected = true,
                                onClick = { selectedMap = selectedMap - member.uid },
                                label = {
                                    Text(
                                        text = member.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = strings.cancel,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                if (filteredCandidates.isEmpty()) {
                    Text(
                        text = strings.noUsersFound,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredCandidates, key = { it.uid }) { user ->
                            val checked = user.uid in selectedMap
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedMap = if (checked) {
                                            selectedMap - user.uid
                                        } else if (selectedMap.size < maxSelectable) {
                                            selectedMap + (user.uid to user)
                                        } else {
                                            selectedMap
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedMap = if (isChecked && selectedMap.size < maxSelectable) {
                                            selectedMap + (user.uid to user)
                                        } else {
                                            selectedMap - user.uid
                                        }
                                    }
                                )
                                UserAvatar(
                                    name = user.name,
                                    photoUrl = user.visiblePhotoUrl(),
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = user.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (user.verified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            VerifiedBadge(contentDescription = strings.verifiedBadgeDesc, size = 14.dp)
                                        }
                                    }
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = groupName.trim().isNotEmpty() && selectedMap.isNotEmpty(),
                onClick = {
                    onCreateGroup(
                        groupName.trim(),
                        groupDescription.trim(),
                        selectedMap.values.toList(),
                        photoUri
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_create_group_button")
            ) {
                Text(strings.createGroupAction)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoSheet(
    chat: Chat,
    currentUserId: String,
    discoverableContacts: List<PublicProfile>,
    isUploadingPhoto: Boolean = false,
    onLookupUsername: suspend (String) -> Result<PublicProfile?> = { Result.success(null) },
    onRenameGroup: (String) -> Unit,
    onUpdateDescription: (String) -> Unit = {},
    onChangeGroupPhoto: (Uri) -> Unit = {},
    onAddMembers: (List<PublicProfile>) -> Unit,
    onToggleOnlyAdminsCanSend: (Boolean) -> Unit = {},
    onToggleMemberAdmin: (String) -> Unit = {},
    onRemoveMember: (String) -> Unit = {},
    onMessageMember: (PublicProfile) -> Unit = {},
    onOpenSharedMedia: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isAdmin = chat.isAdmin(currentUserId)
    val canDeleteGroup = isAdmin || chat.participants == listOf(currentUserId)
    val nowMillis = remember(chat.updatedAt) { System.currentTimeMillis() }
    val profileByUid = remember(discoverableContacts) {
        discoverableContacts.associateBy { it.uid }
    }

    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember(chat.groupName) { mutableStateOf(chat.groupName) }
    var editingDescription by remember { mutableStateOf(false) }
    var descriptionDraft by remember(chat.groupDescription) { mutableStateOf(chat.groupDescription) }
    var memberSearchQuery by remember { mutableStateOf("") }
    var showAddMemberPicker by remember { mutableStateOf(false) }
    var manageMemberUid by remember { mutableStateOf<String?>(null) }
    var pendingRemoveUid by remember { mutableStateOf<String?>(null) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showDeleteGroupConfirm by remember { mutableStateOf(false) }
    var showClearChatConfirm by remember { mutableStateOf(false) }

    val groupPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onChangeGroupPhoto(uri)
    }

    // Sort members: Creator first -> Admins -> Current user -> Alphabetical
    val sortedMemberUids = remember(chat.participants, chat.adminUids, chat.createdBy, chat.participantNames, memberSearchQuery) {
        val q = memberSearchQuery.trim().trimStart('@').lowercase()
        chat.participants
            .filter { uid ->
                if (q.isBlank()) true
                else {
                    val name = chat.participantNames[uid].orEmpty().lowercase()
                    val uname = profileByUid[uid]?.username.orEmpty().lowercase()
                    name.contains(q) || uname.contains(q)
                }
            }
            .sortedWith(
                compareByDescending<String> { it == chat.createdBy }
                    .thenByDescending { chat.isAdmin(it) }
                    .thenByDescending { it == currentUserId }
                    .thenBy { chat.participantNames[it].orEmpty().lowercase() }
            )
    }

    val adminsCount = remember(chat.participants, chat.adminUids, chat.createdBy) {
        chat.participants.count { chat.isAdmin(it) }
    }
    val onlineCount = remember(chat.participants, profileByUid, nowMillis) {
        chat.participants.count { uid ->
            uid == currentUserId || profileByUid[uid]?.isVisiblyOnline(nowMillis) == true
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("group_info_sheet_list"),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Avatar (clickable for admin photo change) + Name + Stats
            item(key = "group_header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .clickable(enabled = isAdmin && !isUploadingPhoto) {
                                groupPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("group_info_photo_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (chat.groupPhotoUrl.isNotBlank()) {
                            UserAvatar(name = chat.groupName, photoUrl = chat.groupPhotoUrl, size = 86.dp)
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(BrandBlue, BrandPurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Group,
                                    contentDescription = chat.groupName,
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        if (isUploadingPhoto) {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else if (isAdmin) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CameraAlt,
                                    contentDescription = strings.changeGroupPhoto,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (editingName) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = nameDraft,
                                onValueChange = { nameDraft = it.take(80) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("group_rename_input")
                            )
                            IconButton(
                                enabled = nameDraft.trim().isNotEmpty(),
                                onClick = {
                                    onRenameGroup(nameDraft.trim())
                                    editingName = false
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlue)
                                    .testTag("group_rename_save_button")
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = strings.saveChanges, tint = Color.White)
                            }
                            IconButton(
                                onClick = {
                                    nameDraft = chat.groupName
                                    editingName = false
                                }
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = strings.cancel)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = chat.groupName.ifBlank { strings.newGroup },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (isAdmin) {
                                IconButton(
                                    onClick = { editingName = true },
                                    modifier = Modifier.testTag("group_rename_edit_button")
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = strings.renameGroup, tint = BrandBlue)
                                }
                            }
                        }
                    }

                    // Live Stats Chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GroupStatPill("${chat.participants.size}/${Chat.MAX_GROUP_MEMBERS} ${strings.groupMembersSuffix}", BrandBlue)
                        GroupStatPill("$adminsCount ${strings.groupAdminsCountLabel}", BrandPurple)
                        GroupStatPill("$onlineCount ${strings.groupOnlineCountLabel}", OnlineGreen)
                    }
                }
            }

            // Group Description Card
            item(key = "group_description") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.groupDescriptionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandBlue,
                                fontWeight = FontWeight.Bold
                            )
                            if (isAdmin && !editingDescription) {
                                TextButton(
                                    onClick = { editingDescription = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.testTag("group_edit_description_button")
                                ) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.editGroupDescription, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        if (editingDescription) {
                            OutlinedTextField(
                                value = descriptionDraft,
                                onValueChange = { descriptionDraft = it.take(300) },
                                placeholder = { Text(strings.groupDescriptionHint) },
                                maxLines = 4,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group_description_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        descriptionDraft = chat.groupDescription
                                        editingDescription = false
                                    }
                                ) {
                                    Text(strings.cancel)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        onUpdateDescription(descriptionDraft.trim())
                                        editingDescription = false
                                    },
                                    modifier = Modifier.testTag("group_description_save_button")
                                ) {
                                    Text(strings.saveChanges)
                                }
                            }
                        } else {
                            Text(
                                text = chat.groupDescription.ifBlank { strings.groupNoDescription },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (chat.groupDescription.isBlank()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // Quick Action Row: Shared Media + Add Members (Admin)
            item(key = "group_quick_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenSharedMedia()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("group_shared_media_button")
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.sharedMediaAction, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (isAdmin) {
                        Button(
                            enabled = chat.participants.size < Chat.MAX_GROUP_MEMBERS,
                            onClick = { showAddMemberPicker = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("group_add_members_button")
                        ) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.addMembers, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Admin Controls or Regular Member Hint
            item(key = "group_admin_controls") {
                if (isAdmin) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = null,
                                tint = BrandPurple
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.groupOnlyAdminsSendSetting,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = strings.groupOnlyAdminsSendDesc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = chat.onlyAdminsCanSend,
                                onCheckedChange = onToggleOnlyAdminsCanSend,
                                modifier = Modifier.testTag("group_only_admins_switch")
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.groupOnlyAdminsCanManageHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Members Header + Search (when > 5 members)
            item(key = "members_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    Text(
                        text = "${chat.participants.size} ${strings.groupMembersSuffix}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )
                    if (chat.participants.size > 5) {
                        OutlinedTextField(
                            value = memberSearchQuery,
                            onValueChange = { memberSearchQuery = it.take(40) },
                            placeholder = { Text(strings.groupSearchMembersHint) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Member Rows
            items(sortedMemberUids, key = { "member_$it" }) { uid ->
                val profile = profileByUid[uid]
                val name = profile?.name?.ifBlank { chat.participantNames[uid].orEmpty() }
                    ?: chat.participantNames[uid].orEmpty().ifBlank { "Member" }
                val photo = profile?.visiblePhotoUrl()?.ifBlank { chat.participantPhotos[uid].orEmpty() }
                    ?: chat.participantPhotos[uid].orEmpty()
                val isCreator = uid == chat.createdBy
                val memberIsAdmin = chat.isAdmin(uid)
                val memberOnline = uid == currentUserId || profile?.isVisiblyOnline(nowMillis) == true

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = uid != currentUserId) { manageMemberUid = uid }
                        .testTag("group_member_row_$uid")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = name,
                            photoUrl = photo,
                            size = 42.dp,
                            showOnlineIndicator = true,
                            isOnline = memberOnline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (uid == currentUserId) "$name (${strings.groupYouLabel})" else name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (profile?.verified == true) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    VerifiedBadge(contentDescription = strings.verifiedBadgeDesc, size = 15.dp)
                                }
                            }
                            profile?.username?.takeIf { it.isNotBlank() }?.let { uname ->
                                Text(
                                    text = "@$uname",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isCreator) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandBlue.copy(alpha = 0.14f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = strings.groupCreatorBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (memberIsAdmin) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BrandPurple.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = strings.groupAdminBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandPurple,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Danger Zone: Clear History, Leave Group, Delete Group (Admin/Creator)
            item(key = "group_danger_zone") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(2.dp))

                    OutlinedButton(
                        onClick = { showClearChatConfirm = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("group_clear_history_button")
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.clearChatAction)
                    }

                    OutlinedButton(
                        onClick = { showLeaveConfirm = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("group_leave_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.leaveGroup)
                    }

                    if (canDeleteGroup) {
                        Button(
                            onClick = { showDeleteGroupConfirm = true },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("group_delete_button")
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.deleteGroupAction)
                        }
                    }
                }
            }
        }
    }

    // Member Action Dialog (Direct Message + Admin Role Toggle + Remove Member)
    manageMemberUid?.let { targetUid ->
        val profile = profileByUid[targetUid]
        val memberName = profile?.name?.ifBlank { chat.participantNames[targetUid].orEmpty() }
            ?: chat.participantNames[targetUid].orEmpty().ifBlank { "Member" }
        val memberPhoto = profile?.visiblePhotoUrl()?.ifBlank { chat.participantPhotos[targetUid].orEmpty() }
            ?: chat.participantPhotos[targetUid].orEmpty()
        val memberIsAdmin = targetUid in chat.adminUids
        val canManageRoleOrRemove = isAdmin && targetUid != currentUserId && targetUid != chat.createdBy

        AlertDialog(
            onDismissRequest = { manageMemberUid = null },
            title = { Text(memberName, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val targetProfile = profile ?: PublicProfile(
                                uid = targetUid,
                                name = memberName,
                                photoUrl = memberPhoto
                            )
                            manageMemberUid = null
                            onDismiss()
                            onMessageMember(targetProfile)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.groupMessageMemberDirect)
                    }

                    if (canManageRoleOrRemove) {
                        OutlinedButton(
                            onClick = {
                                onToggleMemberAdmin(targetUid)
                                manageMemberUid = null
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (memberIsAdmin) strings.groupDismissAdmin else strings.groupMakeAdmin)
                        }
                        OutlinedButton(
                            onClick = {
                                manageMemberUid = null
                                pendingRemoveUid = targetUid
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Rounded.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.groupRemoveMember)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { manageMemberUid = null }) {
                    Text(strings.close)
                }
            }
        )
    }

    // Confirm Remove Member
    pendingRemoveUid?.let { targetUid ->
        val memberName = chat.participantNames[targetUid].orEmpty().ifBlank { "Member" }
        AlertDialog(
            onDismissRequest = { pendingRemoveUid = null },
            title = { Text("${strings.removeMemberConfirmTitle} ($memberName)", fontWeight = FontWeight.Bold) },
            text = { Text(strings.removeMemberConfirmMessage) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    onClick = {
                        onRemoveMember(targetUid)
                        pendingRemoveUid = null
                    }
                ) {
                    Text(strings.groupRemoveMember)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveUid = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Confirm Clear Chat History
    if (showClearChatConfirm) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirm = false },
            title = { Text(strings.clearChatConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.clearChatConfirmMessage) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    onClick = {
                        showClearChatConfirm = false
                        onClearChat()
                        onDismiss()
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Confirm Leave Group
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text(strings.leaveGroupConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.leaveGroupConfirmMessage) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    onClick = {
                        showLeaveConfirm = false
                        onLeaveGroup()
                        onDismiss()
                    }
                ) {
                    Text(strings.leaveGroup)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Confirm Permanent Delete Entire Group
    if (showDeleteGroupConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirm = false },
            title = { Text(strings.deleteGroupConfirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.deleteGroupConfirmMessage) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    onClick = {
                        showDeleteGroupConfirm = false
                        onDeleteGroup()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("confirm_delete_group_button")
                ) {
                    Text(strings.deleteGroupAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGroupConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Add Members Dialog (with contact list filter + exact @username lookup)
    if (showAddMemberPicker) {
        AddGroupMembersDialog(
            existingParticipantUids = chat.participants.toSet(),
            discoverableContacts = discoverableContacts,
            remainingSlots = (Chat.MAX_GROUP_MEMBERS - chat.participants.size).coerceAtLeast(0),
            onLookupUsername = onLookupUsername,
            onConfirmAdd = { members ->
                onAddMembers(members)
                showAddMemberPicker = false
            },
            onDismiss = { showAddMemberPicker = false }
        )
    }
}

@Composable
private fun GroupStatPill(label: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun AddGroupMembersDialog(
    existingParticipantUids: Set<String>,
    discoverableContacts: List<PublicProfile>,
    remainingSlots: Int,
    onLookupUsername: suspend (String) -> Result<PublicProfile?>,
    onConfirmAdd: (List<PublicProfile>) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var lookupLoading by remember { mutableStateOf(false) }
    var lookupError by remember { mutableStateOf<String?>(null) }
    var extraProfiles by remember { mutableStateOf<List<PublicProfile>>(emptyList()) }
    var pickedMap by remember { mutableStateOf<Map<String, PublicProfile>>(emptyMap()) }

    val allCandidates = remember(discoverableContacts, extraProfiles, existingParticipantUids) {
        (extraProfiles + discoverableContacts)
            .distinctBy { it.uid }
            .filter { it.uid !in existingParticipantUids }
    }
    val filtered = remember(allCandidates, query) {
        val q = query.trim().trimStart('@').lowercase()
        if (q.isBlank()) allCandidates
        else allCandidates.filter {
            it.name.lowercase().contains(q) || it.username.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addMembers, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (remainingSlots <= 0) {
                    Text(
                        text = strings.groupMemberLimitReached,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DangerRed
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = {
                                query = it.take(40)
                                lookupError = null
                            },
                            placeholder = { Text(strings.groupAddByUsernameHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        val clean = BanglaChatRepository.normalizeUsername(query)
                        if (BanglaChatRepository.isValidUsernameFormat(clean)) {
                            IconButton(
                                enabled = !lookupLoading,
                                onClick = {
                                    scope.launch {
                                        lookupLoading = true
                                        lookupError = null
                                        onLookupUsername(clean)
                                            .onSuccess { found ->
                                                if (found == null) {
                                                    lookupError = strings.exactUsernameNotFound
                                                } else if (found.uid in existingParticipantUids) {
                                                    lookupError = "@${found.username}"
                                                } else {
                                                    extraProfiles = (listOf(found) + extraProfiles).distinctBy { it.uid }
                                                    if (pickedMap.size < remainingSlots) {
                                                        pickedMap = pickedMap + (found.uid to found)
                                                    }
                                                }
                                            }
                                            .onFailure { err ->
                                                lookupError = err.localizedMessage ?: strings.exactUsernameNotFound
                                            }
                                        lookupLoading = false
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlue.copy(alpha = 0.14f))
                            ) {
                                if (lookupLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Rounded.PersonAdd, contentDescription = strings.searchButton, tint = BrandBlue)
                                }
                            }
                        }
                    }

                    lookupError?.let { err ->
                        Text(err, style = MaterialTheme.typography.labelSmall, color = DangerRed)
                    }

                    if (filtered.isEmpty()) {
                        Text(
                            text = strings.noUsersFound,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filtered, key = { it.uid }) { user ->
                                val checked = user.uid in pickedMap
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            pickedMap = if (checked) {
                                                pickedMap - user.uid
                                            } else if (pickedMap.size < remainingSlots) {
                                                pickedMap + (user.uid to user)
                                            } else {
                                                pickedMap
                                            }
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            pickedMap = if (isChecked && pickedMap.size < remainingSlots) {
                                                pickedMap + (user.uid to user)
                                            } else {
                                                pickedMap - user.uid
                                            }
                                        }
                                    )
                                    UserAvatar(name = user.name, photoUrl = user.visiblePhotoUrl(), size = 36.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = "@${user.username}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = pickedMap.isNotEmpty() && remainingSlots > 0,
                onClick = { onConfirmAdd(pickedMap.values.toList()) }
            ) {
                Text("${strings.confirm} (${pickedMap.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
