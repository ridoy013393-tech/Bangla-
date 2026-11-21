package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.AppUpdateChecker
import com.example.data.ContentModerationEngine
import com.example.model.AppBroadcast
import com.example.model.AppUpdateConfig
import com.example.model.ModerationSettings
import com.example.ui.components.AdminBroadcastBanner
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OfflineAmber
import com.example.ui.theme.OnlineGreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminBroadcastAndFilterTab(
    currentBroadcast: AppBroadcast?,
    currentModerationSettings: ModerationSettings,
    currentAppUpdateConfig: AppUpdateConfig? = null,
    onPublishBroadcast: (active: Boolean, title: String, message: String, severity: String) -> Unit,
    onSaveModerationSettings: (ModerationSettings) -> Unit,
    onSaveAppUpdateConfig: (AppUpdateConfig) -> Unit = {}
) {
    val strings = LocalAppStrings.current

    // App Update & Force Update Control state synced with Firestore
    var updateGateActive by remember(currentAppUpdateConfig) {
        mutableStateOf(currentAppUpdateConfig?.active == true)
    }
    var updateForceMandatory by remember(currentAppUpdateConfig) {
        mutableStateOf(currentAppUpdateConfig?.forceUpdate ?: true)
    }
    var updateVersionCode by remember(currentAppUpdateConfig) {
        mutableStateOf(
            currentAppUpdateConfig?.latestVersionCode?.takeIf { it > 0 }?.toString()
                ?: BuildConfig.VERSION_CODE.toString()
        )
    }
    var updateVersionName by remember(currentAppUpdateConfig) {
        mutableStateOf(
            currentAppUpdateConfig?.latestVersionName?.ifBlank { BuildConfig.VERSION_NAME }
                ?: BuildConfig.VERSION_NAME
        )
    }
    var updateApkUrl by remember(currentAppUpdateConfig) {
        mutableStateOf(
            currentAppUpdateConfig?.apkUrl?.ifBlank { AppUpdateChecker.FALLBACK_APK_URL }
                ?: AppUpdateChecker.FALLBACK_APK_URL
        )
    }
    var updateNotes by remember(currentAppUpdateConfig) {
        mutableStateOf(currentAppUpdateConfig?.releaseNotes.orEmpty())
    }

    // Broadcast form state synced with Firestore
    var broadcastTitle by remember { mutableStateOf(currentBroadcast?.title.orEmpty()) }
    var broadcastMessage by remember { mutableStateOf(currentBroadcast?.message.orEmpty()) }
    var broadcastSeverity by remember {
        mutableStateOf(currentBroadcast?.severity?.ifBlank { "info" } ?: "info")
    }

    LaunchedEffect(currentBroadcast?.broadcastId) {
        if (currentBroadcast != null) {
            if (broadcastTitle.isBlank()) broadcastTitle = currentBroadcast.title
            if (broadcastMessage.isBlank()) broadcastMessage = currentBroadcast.message
            if (currentBroadcast.severity.isNotBlank()) broadcastSeverity = currentBroadcast.severity
        }
    }

    // Auto-moderation filter state synced with Firestore
    var profanityEnabled by remember(currentModerationSettings) {
        mutableStateOf(currentModerationSettings.profanityFilterEnabled)
    }
    var profanityMode by remember(currentModerationSettings) {
        mutableStateOf(if (currentModerationSettings.isMaskMode()) "mask" else "block")
    }
    var spamEnabled by remember(currentModerationSettings) {
        mutableStateOf(currentModerationSettings.spamProtectionEnabled)
    }
    var telegramAlertsEnabled by remember(currentModerationSettings) {
        mutableStateOf(currentModerationSettings.telegramAlertsEnabled)
    }
    var customWords by remember(currentModerationSettings) {
        mutableStateOf(currentModerationSettings.customBlockedWords)
    }
    var newWordInput by remember { mutableStateOf("") }
    var testerInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_broadcast_filter_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live Admin Broadcast Notice Publisher Card
        item(key = "broadcast_card") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Campaign,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.adminBroadcastSectionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.adminBroadcastSectionDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val isLive = currentBroadcast?.active == true && currentBroadcast.message.isNotBlank()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = (if (isLive) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = if (isLive) strings.adminBroadcastActiveBadge else strings.adminBroadcastInactiveBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isLive) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Severity selector
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "info" to strings.adminBroadcastSeverityInfo,
                            "update" to strings.adminBroadcastSeverityUpdate,
                            "warning" to strings.adminBroadcastSeverityWarning
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = broadcastSeverity == key,
                                onClick = { broadcastSeverity = key },
                                label = { Text(label) },
                                modifier = Modifier.testTag("broadcast_severity_$key")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it.take(120) },
                        placeholder = { Text(strings.adminBroadcastTitleHint) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_title_input")
                    )

                    OutlinedTextField(
                        value = broadcastMessage,
                        onValueChange = { broadcastMessage = it.take(600) },
                        placeholder = { Text(strings.adminBroadcastMessageHint) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_message_input")
                    )

                    // Live banner preview
                    if (broadcastMessage.isNotBlank()) {
                        AdminBroadcastBanner(
                            broadcast = AppBroadcast(
                                active = true,
                                broadcastId = "preview",
                                title = broadcastTitle,
                                message = broadcastMessage,
                                severity = broadcastSeverity
                            ),
                            defaultTitle = strings.defaultBroadcastTitle,
                            dismissContentDescription = strings.close,
                            onDismiss = {}
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onPublishBroadcast(
                                    true,
                                    broadcastTitle.ifBlank { strings.defaultBroadcastTitle },
                                    broadcastMessage.trim(),
                                    broadcastSeverity
                                )
                            },
                            enabled = broadcastMessage.isNotBlank(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("broadcast_publish_button")
                        ) {
                            Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.adminBroadcastPublishAction, fontWeight = FontWeight.SemiBold)
                        }

                        if (currentBroadcast?.active == true) {
                            OutlinedButton(
                                onClick = {
                                    onPublishBroadcast(false, "", "", "info")
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("broadcast_clear_button")
                            ) {
                                Icon(
                                    Icons.Rounded.NotificationsOff,
                                    contentDescription = strings.adminBroadcastClearAction,
                                    tint = DangerRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Auto Bad-Word & Anti-Spam Filter Card
        item(key = "auto_filter_card") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.adminAutoFilterSectionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.adminAutoFilterSectionDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Toggle: Profanity Filter
                    AdminFilterSwitchRow(
                        title = strings.adminProfanityToggleTitle,
                        subtitle = strings.adminProfanityToggleDesc,
                        checked = profanityEnabled,
                        onCheckedChange = { profanityEnabled = it },
                        testTag = "filter_profanity_switch"
                    )

                    if (profanityEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = profanityMode == "block",
                                onClick = { profanityMode = "block" },
                                label = { Text(strings.adminProfanityModeBlock) },
                                modifier = Modifier.testTag("filter_mode_block")
                            )
                            FilterChip(
                                selected = profanityMode == "mask",
                                onClick = { profanityMode = "mask" },
                                label = { Text(strings.adminProfanityModeMask) },
                                modifier = Modifier.testTag("filter_mode_mask")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    // Toggle: Anti-Spam Protection
                    AdminFilterSwitchRow(
                        title = strings.adminSpamToggleTitle,
                        subtitle = strings.adminSpamToggleDesc,
                        checked = spamEnabled,
                        onCheckedChange = { spamEnabled = it },
                        testTag = "filter_spam_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    // Toggle: Telegram Auto-Alerts
                    AdminFilterSwitchRow(
                        title = strings.adminTelegramAlertToggleTitle,
                        subtitle = strings.adminTelegramAlertToggleDesc,
                        checked = telegramAlertsEnabled,
                        onCheckedChange = { telegramAlertsEnabled = it },
                        testTag = "filter_telegram_alert_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                    // Custom Blocked Words list
                    Text(
                        text = "${strings.adminCustomWordsLabel} (${customWords.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newWordInput,
                            onValueChange = { newWordInput = it.take(40) },
                            placeholder = { Text(strings.adminCustomWordsHint) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_blocked_word_input")
                        )
                        Button(
                            onClick = {
                                val cleaned = newWordInput.trim()
                                if (cleaned.length >= 2 && customWords.none { it.equals(cleaned, ignoreCase = true) }) {
                                    customWords = (customWords + cleaned).take(200)
                                    newWordInput = ""
                                }
                            },
                            enabled = newWordInput.trim().length >= 2,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                            modifier = Modifier.testTag("add_custom_word_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = strings.adminAddWordAction, modifier = Modifier.size(20.dp))
                        }
                    }

                    if (customWords.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            customWords.forEach { word ->
                                InputChip(
                                    selected = false,
                                    onClick = { customWords = customWords.filterNot { it == word } },
                                    label = { Text(word) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = strings.close,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Live Filter Tester
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = strings.adminFilterTesterLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedTextField(
                                value = testerInput,
                                onValueChange = { testerInput = it.take(200) },
                                placeholder = { Text(strings.adminFilterTesterHint) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("filter_tester_input")
                            )
                            if (testerInput.isNotBlank()) {
                                val result = remember(testerInput, customWords) {
                                    ContentModerationEngine.scanProfanity(testerInput, customWords)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (result.hasProfanity) Icons.Rounded.GppMaybe else Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = if (result.hasProfanity) OfflineAmber else OnlineGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (result.hasProfanity) {
                                            "${strings.adminFilterTesterFlagged}: ${result.matchedWords.joinToString(", ")} → ${result.maskedText}"
                                        } else {
                                            strings.adminFilterTesterClean
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (result.hasProfanity) DangerRed else OnlineGreen
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onSaveModerationSettings(
                                ModerationSettings(
                                    profanityFilterEnabled = profanityEnabled,
                                    profanityMode = profanityMode,
                                    spamProtectionEnabled = spamEnabled,
                                    telegramAlertsEnabled = telegramAlertsEnabled,
                                    customBlockedWords = customWords
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPurple),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_filter_settings_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.adminSaveFilterAction, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 3. Live App Update & Mandatory Force Update Control Card
        item(key = "app_update_control_card") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = OnlineGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.adminAppUpdateSectionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.adminAppUpdateSectionDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AdminFilterSwitchRow(
                        title = strings.adminAppUpdateActiveToggle,
                        subtitle = "${strings.forceUpdateCurrentVersionLabel}: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        checked = updateGateActive,
                        onCheckedChange = { updateGateActive = it },
                        testTag = "admin_update_active_switch"
                    )

                    AdminFilterSwitchRow(
                        title = strings.adminAppUpdateForceToggle,
                        subtitle = strings.forceUpdateDesc,
                        checked = updateForceMandatory,
                        onCheckedChange = { updateForceMandatory = it },
                        testTag = "admin_update_force_switch"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = updateVersionCode,
                            onValueChange = { updateVersionCode = it.filter(Char::isDigit).take(6) },
                            placeholder = { Text(strings.adminAppUpdateVersionCodeHint) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_update_version_code_input")
                        )
                        OutlinedTextField(
                            value = updateVersionName,
                            onValueChange = { updateVersionName = it.take(32) },
                            placeholder = { Text(strings.adminAppUpdateVersionNameHint) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_update_version_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = updateApkUrl,
                        onValueChange = { updateApkUrl = it.take(1000) },
                        placeholder = { Text(strings.adminAppUpdateApkUrlHint) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_update_apk_url_input")
                    )

                    OutlinedTextField(
                        value = updateNotes,
                        onValueChange = { updateNotes = it.take(1000) },
                        placeholder = { Text(strings.adminAppUpdateNotesHint) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_update_notes_input")
                    )

                    Button(
                        onClick = {
                            val parsedCode = updateVersionCode.toIntOrNull()?.coerceAtLeast(1)
                                ?: BuildConfig.VERSION_CODE
                            onSaveAppUpdateConfig(
                                AppUpdateConfig(
                                    active = updateGateActive,
                                    latestVersionCode = parsedCode,
                                    latestVersionName = updateVersionName.trim().ifBlank { BuildConfig.VERSION_NAME },
                                    forceUpdate = updateForceMandatory,
                                    apkUrl = updateApkUrl.trim().ifBlank { AppUpdateChecker.FALLBACK_APK_URL },
                                    releaseUrl = AppUpdateChecker.FALLBACK_RELEASE_URL,
                                    releaseNotes = updateNotes.trim()
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OnlineGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_save_app_update_button")
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.adminAppUpdateSaveAction, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun AdminFilterSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
