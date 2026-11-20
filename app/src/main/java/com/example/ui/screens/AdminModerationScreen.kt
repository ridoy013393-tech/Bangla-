package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.ModerationStatus
import com.example.model.PublicProfile
import com.example.model.UserReport
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatFullDate
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OfflineAmber
import com.example.ui.theme.OnlineGreen

fun formatRemainingMuteDuration(remainingMillis: Long, language: AppLanguage): String {
    if (remainingMillis <= 0L) return ""
    val totalMinutes = ((remainingMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
    val days = totalMinutes / (60L * 24L)
    val hours = (totalMinutes % (60L * 24L)) / 60L
    val mins = totalMinutes % 60L
    return if (language == AppLanguage.BANGLA) {
        buildList {
            if (days > 0) add("$days দিন")
            if (hours > 0) add("$hours ঘণ্টা")
            if (mins > 0 && days == 0L) add("$mins মিনিট")
        }.joinToString(" ").ifBlank { "১ মিনিট" }
    } else {
        buildList {
            if (days > 0) add("${days}d")
            if (hours > 0) add("${hours}h")
            if (mins > 0 && days == 0L) add("${mins}m")
        }.joinToString(" ").ifBlank { "1m" }
    }
}

@Composable
fun AccountSuspendedScreen(
    status: ModerationStatus,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(DangerRed.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.GppBad,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(50.dp)
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = if (status.deletedByAdmin) strings.accountDeletedByAdminTitle else strings.accountSuspendedTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.accountSuspendedDesc,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (status.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.moderationReasonLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = status.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("suspended_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.logout, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private enum class AdminActionType { BAN, MUTE, DELETE }

private data class PendingAdminAction(
    val type: AdminActionType,
    val uid: String,
    val name: String,
    val username: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminModerationScreen(
    currentAdminUid: String,
    profiles: List<PublicProfile>,
    moderations: List<ModerationStatus>,
    reports: List<UserReport>,
    language: AppLanguage,
    onBanUser: (uid: String, name: String, username: String, reason: String) -> Unit,
    onMuteUser: (uid: String, name: String, username: String, hours: Long, reason: String) -> Unit,
    onUnbanUser: (uid: String, name: String, username: String) -> Unit,
    onDeleteUser: (uid: String, name: String, username: String, reason: String) -> Unit,
    onDismissReport: (reportId: String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<PendingAdminAction?>(null) }
    var actionReason by remember { mutableStateOf("") }
    var selectedMuteHours by remember { mutableLongStateOf(24L) }

    val moderationByUid = remember(moderations) {
        moderations.associateBy { it.uid.ifBlank { it.id } }
    }
    val profileByUid = remember(profiles) {
        profiles.associateBy { it.uid.ifBlank { it.id } }
    }

    // Include users who still have a profile AND users whose profile was removed but have a moderation record
    val combinedUserEntries = remember(profiles, moderations, searchQuery, currentAdminUid) {
        val allUids = linkedSetOf<String>()
        profiles.forEach { allUids.add(it.uid.ifBlank { it.id }) }
        moderations.forEach { allUids.add(it.uid.ifBlank { it.id }) }

        val q = searchQuery.trim().lowercase()
        allUids.filter { it != currentAdminUid }.map { uid ->
            val profile = profileByUid[uid]
            val mod = moderationByUid[uid]
            Triple(uid, profile, mod)
        }.filter { (uid, profile, _) ->
            if (q.isEmpty()) true
            else uid.lowercase().contains(q) ||
                profile?.name?.lowercase()?.contains(q) == true ||
                profile?.username?.lowercase()?.contains(q) == true ||
                profile?.phoneLookup?.lowercase()?.contains(q) == true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("admin_panel_back")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.close)
                        }
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.adminPanelTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.adminPanelSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("${strings.adminUsersTab} (${combinedUserEntries.size})") },
                            modifier = Modifier.testTag("admin_tab_users")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("${strings.adminReportsTab} (${reports.size})") },
                            modifier = Modifier.testTag("admin_tab_reports")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.adminSearchHint) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = strings.close)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("admin_search_input")
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(combinedUserEntries, key = { it.first }) { (uid, profile, mod) ->
                        AdminUserModerationCard(
                            uid = uid,
                            profile = profile,
                            moderation = mod,
                            language = language,
                            onCopyUid = {
                                clipboard.setText(AnnotatedString(uid))
                                Toast.makeText(context, strings.adminCopyUidToast, Toast.LENGTH_SHORT).show()
                            },
                            onMuteClick = {
                                actionReason = ""
                                selectedMuteHours = 24L
                                pendingAction = PendingAdminAction(
                                    type = AdminActionType.MUTE,
                                    uid = uid,
                                    name = profile?.name.orEmpty().ifBlank { uid },
                                    username = profile?.username.orEmpty()
                                )
                            },
                            onBanClick = {
                                actionReason = ""
                                pendingAction = PendingAdminAction(
                                    type = AdminActionType.BAN,
                                    uid = uid,
                                    name = profile?.name.orEmpty().ifBlank { uid },
                                    username = profile?.username.orEmpty()
                                )
                            },
                            onUnbanClick = {
                                onUnbanUser(uid, profile?.name.orEmpty().ifBlank { uid }, profile?.username.orEmpty())
                            },
                            onDeleteClick = {
                                actionReason = ""
                                pendingAction = PendingAdminAction(
                                    type = AdminActionType.DELETE,
                                    uid = uid,
                                    name = profile?.name.orEmpty().ifBlank { uid },
                                    username = profile?.username.orEmpty()
                                )
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            } else {
                if (reports.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = OnlineGreen,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = strings.adminNoReportsEmpty,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reports, key = { it.id }) { report ->
                            val reportedProfile = profileByUid[report.reportedUid]
                            val reporterProfile = profileByUid[report.reporterUid]
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Flag,
                                            contentDescription = null,
                                            tint = DangerRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = report.reason,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DangerRed,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatFullDate(report.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${reportedProfile?.name ?: report.reportedUid} ${
                                            reportedProfile?.username?.takeIf { it.isNotBlank() }?.let { "(@$it)" }.orEmpty()
                                        }",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!reportedProfile?.phoneLookup.isNullOrBlank()) {
                                        Text(
                                            text = "📱 ${reportedProfile?.phoneLookup}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = BrandBlue
                                        )
                                    }
                                    if (report.details.isNotBlank()) {
                                        Text(
                                            text = report.details,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Reporter: ${reporterProfile?.name ?: report.reporterUid}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                actionReason = report.reason
                                                selectedMuteHours = 24L
                                                pendingAction = PendingAdminAction(
                                                    type = AdminActionType.MUTE,
                                                    uid = report.reportedUid,
                                                    name = reportedProfile?.name.orEmpty().ifBlank { report.reportedUid },
                                                    username = reportedProfile?.username.orEmpty()
                                                )
                                            }
                                        ) {
                                            Icon(Icons.Rounded.VolumeOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.adminMuteAction)
                                        }
                                        Button(
                                            onClick = {
                                                actionReason = report.reason
                                                pendingAction = PendingAdminAction(
                                                    type = AdminActionType.BAN,
                                                    uid = report.reportedUid,
                                                    name = reportedProfile?.name.orEmpty().ifBlank { report.reportedUid },
                                                    username = reportedProfile?.username.orEmpty()
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                                        ) {
                                            Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(strings.adminBanAction)
                                        }
                                        OutlinedButton(onClick = { onDismissReport(report.id) }) {
                                            Text(strings.adminDismissReportAction)
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

    // Confirmation & Reason Dialog for Ban / Mute / Delete
    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = {
                Text(
                    when (action.type) {
                        AdminActionType.BAN -> "${strings.adminBanAction}: ${action.name}"
                        AdminActionType.MUTE -> "${strings.adminMuteAction}: ${action.name}"
                        AdminActionType.DELETE -> strings.adminConfirmDeleteTitle
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (action.type == AdminActionType.DELETE) {
                        Text(
                            text = strings.adminConfirmDeleteDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerRed
                        )
                    }
                    if (action.type == AdminActionType.MUTE) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                1L to strings.adminMute1h,
                                24L to strings.adminMute24h,
                                168L to strings.adminMute7d
                            ).forEach { (hours, label) ->
                                FilterChip(
                                    selected = selectedMuteHours == hours,
                                    onClick = { selectedMuteHours = hours },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it.take(250) },
                        placeholder = { Text(strings.adminReasonHint) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (action.type) {
                            AdminActionType.BAN -> onBanUser(action.uid, action.name, action.username, actionReason)
                            AdminActionType.MUTE -> onMuteUser(action.uid, action.name, action.username, selectedMuteHours, actionReason)
                            AdminActionType.DELETE -> onDeleteUser(action.uid, action.name, action.username, actionReason)
                        }
                        pendingAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (action.type == AdminActionType.MUTE) BrandPurple else DangerRed
                    )
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUserModerationCard(
    uid: String,
    profile: PublicProfile?,
    moderation: ModerationStatus?,
    language: AppLanguage,
    onCopyUid: () -> Unit,
    onMuteClick: () -> Unit,
    onBanClick: () -> Unit,
    onUnbanClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val nowMs = System.currentTimeMillis()
    val isSuspended = moderation?.isSuspended() == true
    val isMuted = moderation?.isMutedNow(nowMs) == true

    val (badgeLabel, badgeColor) = when {
        moderation?.deletedByAdmin == true -> strings.adminStatusDeleted to DangerRed
        moderation?.banned == true -> strings.adminStatusBanned to DangerRed
        isMuted -> "${strings.adminStatusMuted} (${formatRemainingMuteDuration(moderation!!.remainingMuteMillis(nowMs), language)})" to OfflineAmber
        else -> strings.adminStatusActive to OnlineGreen
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    name = profile?.name.orEmpty().ifBlank { uid.take(2) },
                    photoUrl = profile?.photoUrl.orEmpty(),
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile?.name.orEmpty().ifBlank { "Removed User" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = badgeColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (!profile?.username.isNullOrBlank()) {
                        Text(
                            text = "@${profile?.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandBlue
                        )
                    }
                    if (!profile?.phoneLookup.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = profile?.phoneLookup.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Copyable UID strip
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onCopyUid)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UID: $uid",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = strings.copy,
                        tint = BrandBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (!moderation?.reason.isNullOrBlank()) {
                Text(
                    text = "${strings.moderationReasonLabel}: ${moderation?.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isSuspended || isMuted) {
                    FilledTonalButton(onClick = onUnbanClick) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.adminUnbanAction)
                    }
                }
                if (!isSuspended) {
                    FilledTonalButton(onClick = onMuteClick) {
                        Icon(Icons.Rounded.VolumeOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.adminMuteAction)
                    }
                    OutlinedButton(onClick = onBanClick) {
                        Icon(Icons.Rounded.Block, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.adminBanAction, color = DangerRed)
                    }
                }
                if (moderation?.deletedByAdmin != true) {
                    TextButton(onClick = onDeleteClick) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.adminDeleteUserAction, color = DangerRed)
                    }
                }
            }
        }
    }
}
