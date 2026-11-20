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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.Chat
import com.example.model.PublicProfile
import com.example.ui.components.UserAvatar
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed

@Composable
fun CreateGroupDialog(
    availableContacts: List<PublicProfile>,
    onCreateGroup: (groupName: String, members: List<PublicProfile>, photoUri: Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var groupName by remember { mutableStateOf("") }
    var selectedUids by remember { mutableStateOf<Set<String>>(emptySet()) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) photoUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newGroup, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp)
                            )
                        } else {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = BrandBlue)
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

                Text(
                    text = "${strings.selectGroupMembers} (${selectedUids.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandBlue,
                    fontWeight = FontWeight.SemiBold
                )

                if (availableContacts.isEmpty()) {
                    Text(
                        text = strings.noUsersFound,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableContacts, key = { it.uid }) { user ->
                            val checked = user.uid in selectedUids
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedUids = if (checked) selectedUids - user.uid
                                        else selectedUids + user.uid
                                    }
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedUids = if (isChecked) selectedUids + user.uid
                                        else selectedUids - user.uid
                                    }
                                )
                                UserAvatar(
                                    name = user.name,
                                    photoUrl = user.visiblePhotoUrl(),
                                    size = 38.dp
                                )
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
        },
        confirmButton = {
            Button(
                enabled = groupName.trim().isNotEmpty() && selectedUids.isNotEmpty(),
                onClick = {
                    val members = availableContacts.filter { it.uid in selectedUids }
                    onCreateGroup(groupName.trim(), members, photoUri)
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
    onRenameGroup: (String) -> Unit,
    onAddMembers: (List<PublicProfile>) -> Unit,
    onLeaveGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val isAdmin = chat.isAdmin(currentUserId)
    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember(chat.groupName) { mutableStateOf(chat.groupName) }
    var showAddMemberPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (chat.groupPhotoUrl.isNotBlank()) {
                UserAvatar(name = chat.groupName, photoUrl = chat.groupPhotoUrl, size = 76.dp)
            } else {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Group, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(38.dp))
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
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = nameDraft.trim().isNotEmpty(),
                        onClick = {
                            onRenameGroup(nameDraft.trim())
                            editingName = false
                        }
                    ) {
                        Text(strings.saveChanges)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.groupName.ifBlank { strings.newGroup },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isAdmin) {
                        IconButton(onClick = { editingName = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = strings.renameGroup, tint = BrandBlue)
                        }
                    }
                }
            }

            Text(
                text = "${chat.participants.size} ${strings.groupMembersSuffix}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chat.participants, key = { it }) { uid ->
                    val name = chat.participantNames[uid].orEmpty().ifBlank { "Member" }
                    val photo = chat.participantPhotos[uid].orEmpty()
                    val memberIsAdmin = uid in chat.adminUids || uid == chat.createdBy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(name = name, photoUrl = photo, size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (uid == currentUserId) "$name (You)" else name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (memberIsAdmin) {
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

            Spacer(modifier = Modifier.height(14.dp))

            if (isAdmin) {
                val candidates = discoverableContacts.filter { it.uid !in chat.participants }
                if (candidates.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showAddMemberPicker = true },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.addMembers)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            OutlinedButton(
                onClick = {
                    onLeaveGroup()
                    onDismiss()
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.leaveGroup)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showAddMemberPicker) {
        val candidates = remember(discoverableContacts, chat.participants) {
            discoverableContacts.filter { it.uid !in chat.participants }
        }
        var picked by remember { mutableStateOf<Set<String>>(emptySet()) }
        AlertDialog(
            onDismissRequest = { showAddMemberPicker = false },
            title = { Text(strings.addMembers) },
            text = {
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(candidates, key = { it.uid }) { user ->
                        val checked = user.uid in picked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    picked = if (checked) picked - user.uid else picked + user.uid
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    picked = if (it) picked + user.uid else picked - user.uid
                                }
                            )
                            Text(user.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = picked.isNotEmpty(),
                    onClick = {
                        onAddMembers(candidates.filter { it.uid in picked })
                        showAddMemberPicker = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberPicker = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
