package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.ScreenshotMonitor
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.AppLanguage
import com.example.model.BlockedUser
import com.example.model.ChatFontSize
import com.example.model.ChatWallpaper
import com.example.model.PrivateUser
import com.example.model.PublicProfile
import com.example.model.ThemePreference
import com.example.model.UploadState
import com.example.model.UserPreferences
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatFullDate
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.DangerRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    myProfile: PublicProfile?,
    myPrivateUser: PrivateUser?,
    preferences: UserPreferences,
    blockedUsers: List<BlockedUser>,
    avatarUploadState: UploadState,
    onUpdateProfile: (String, String, String, String) -> Unit,
    onUploadAvatar: (Uri) -> Unit,
    onUpdatePrivacy: (Boolean, Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onUpdateMessagePreview: (Boolean) -> Unit,
    onChangeLanguage: (AppLanguage) -> Unit,
    onChangeTheme: (ThemePreference) -> Unit,
    onChangeWallpaper: (ChatWallpaper) -> Unit,
    onChangeFontSize: (ChatFontSize) -> Unit,
    onToggleScreenSecurity: (Boolean) -> Unit,
    onEnableAppLock: (String) -> Unit,
    onDisableAppLock: () -> Unit,
    onUnblockUser: (String) -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current

    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showBlockedSheet by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var infoDialogContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    var notificationPermissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadAvatar(uri)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Profile Header Card
        item(key = "profile_card") {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            name = myProfile?.name.orEmpty().ifBlank { "Bangla Chat" },
                            photoUrl = myProfile?.photoUrl.orEmpty(),
                            size = 64.dp,
                            showOnlineIndicator = true,
                            isOnline = true
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = myProfile?.name.orEmpty().ifBlank { "Bangla Chat User" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${myProfile?.username.orEmpty()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandBlue
                            )
                            if (!myPrivateUser?.email.isNullOrBlank()) {
                                Text(
                                    text = myPrivateUser?.email.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${strings.accountCreatedLabel}: ${formatFullDate(myProfile?.createdAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!myProfile?.about.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = myProfile?.about.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showEditProfileSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.editProfile)
                    }
                }
            }
        }

        // Language & Theme Preferences
        item(key = "prefs_section") {
            SettingsCardSection(title = strings.settingsPreferencesSection) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Language, contentDescription = null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(strings.languageSetting, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = preferences.language == AppLanguage.BANGLA,
                            onClick = { onChangeLanguage(AppLanguage.BANGLA) },
                            label = { Text(strings.languageBangla) },
                            modifier = Modifier.testTag("lang_bangla_chip")
                        )
                        FilterChip(
                            selected = preferences.language == AppLanguage.ENGLISH,
                            onClick = { onChangeLanguage(AppLanguage.ENGLISH) },
                            label = { Text(strings.languageEnglish) },
                            modifier = Modifier.testTag("lang_english_chip")
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = BrandPurple)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(strings.themeSetting, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = preferences.theme == ThemePreference.SYSTEM,
                            onClick = { onChangeTheme(ThemePreference.SYSTEM) },
                            label = { Text(strings.themeSystem) },
                            modifier = Modifier.testTag("theme_system_chip")
                        )
                        FilterChip(
                            selected = preferences.theme == ThemePreference.LIGHT,
                            onClick = { onChangeTheme(ThemePreference.LIGHT) },
                            label = { Text(strings.themeLight) },
                            modifier = Modifier.testTag("theme_light_chip")
                        )
                        FilterChip(
                            selected = preferences.theme == ThemePreference.DARK,
                            onClick = { onChangeTheme(ThemePreference.DARK) },
                            label = { Text(strings.themeDark) },
                            modifier = Modifier.testTag("theme_dark_chip")
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Chat Wallpaper Picker
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Wallpaper, contentDescription = null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(strings.chatWallpaperSetting, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val wallpapers = listOf(
                        ChatWallpaper.DEFAULT to strings.wallpaperDefault,
                        ChatWallpaper.MINT to strings.wallpaperMint,
                        ChatWallpaper.OCEAN to strings.wallpaperOcean,
                        ChatWallpaper.VIOLET to strings.wallpaperViolet,
                        ChatWallpaper.PEACH to strings.wallpaperPeach,
                        ChatWallpaper.MIDNIGHT to strings.wallpaperMidnight
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(wallpapers, key = { it.first.name }) { (wp, label) ->
                            FilterChip(
                                selected = preferences.chatWallpaper == wp,
                                onClick = { onChangeWallpaper(wp) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("wallpaper_${wp.name.lowercase()}")
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Chat Font Size Picker
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FormatSize, contentDescription = null, tint = BrandPurple)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(strings.chatFontSizeSetting, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = preferences.chatFontSize == ChatFontSize.SMALL,
                            onClick = { onChangeFontSize(ChatFontSize.SMALL) },
                            label = { Text(strings.fontSmall) }
                        )
                        FilterChip(
                            selected = preferences.chatFontSize == ChatFontSize.MEDIUM,
                            onClick = { onChangeFontSize(ChatFontSize.MEDIUM) },
                            label = { Text(strings.fontMedium) }
                        )
                        FilterChip(
                            selected = preferences.chatFontSize == ChatFontSize.LARGE,
                            onClick = { onChangeFontSize(ChatFontSize.LARGE) },
                            label = { Text(strings.fontLarge) }
                        )
                    }
                }
            }
        }

        // Notifications & Privacy
        item(key = "privacy_section") {
            SettingsCardSection(title = strings.settingsPrivacySection) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.enableNotifications, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (notificationPermissionGranted) "Active" else "Tap to allow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!notificationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        TextButton(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        ) {
                            Text(strings.confirm)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.messagePreviewSetting, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = strings.messagePreviewDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = myPrivateUser?.messagePreviewEnabled ?: true,
                        onCheckedChange = onUpdateMessagePreview
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsActionRow(
                    icon = Icons.Rounded.Security,
                    title = strings.privacySettings,
                    onClick = { showPrivacySheet = true },
                    testTag = "privacy_settings_row"
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // 4-Digit App Lock Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.appLockSetting, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = strings.appLockDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.appLockEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                showPinSetupDialog = true
                            } else {
                                onDisableAppLock()
                            }
                        },
                        modifier = Modifier.testTag("app_lock_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                // Screenshot & Screen Recording Protection (FLAG_SECURE)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.ScreenshotMonitor, contentDescription = null, tint = BrandPurple)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.screenSecuritySetting, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = strings.screenSecurityDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.screenSecurityEnabled,
                        onCheckedChange = onToggleScreenSecurity,
                        modifier = Modifier.testTag("screen_security_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsActionRow(
                    icon = Icons.Rounded.Block,
                    title = "${strings.blockedUsersSetting} (${blockedUsers.size})",
                    onClick = { showBlockedSheet = true },
                    testTag = "blocked_users_row"
                )
            }
        }

        // About, Policy, Terms & Support
        item(key = "support_section") {
            SettingsCardSection(title = strings.settingsSupportSection) {
                SettingsActionRow(
                    icon = Icons.Rounded.Info,
                    title = strings.aboutApp,
                    onClick = {
                        infoDialogContent = strings.aboutApp to
                            "Bangla Chat v1.0\n\nNative Android messaging application built with Kotlin, Jetpack Compose, Material 3, Firebase Authentication, Cloud Firestore, Cloudinary Media Storage, and Firebase Cloud Messaging.\n\nPackage: com.mhridoy.banglacha"
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsActionRow(
                    icon = Icons.Rounded.PrivacyTip,
                    title = strings.privacyPolicy,
                    onClick = {
                        infoDialogContent = strings.privacyPolicy to
                            "Bangla Chat isolates your private email, phone number, and notification tokens in an owner-only Firestore document protected by zero-trust Security Rules. Public discovery only exposes fields you explicitly choose to share. Messages are transmitted over TLS via Firebase Cloud Firestore and are not claimed to be end-to-end encrypted."
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsActionRow(
                    icon = Icons.Rounded.Description,
                    title = strings.termsAndConditions,
                    onClick = {
                        infoDialogContent = strings.termsAndConditions to
                            "Use Bangla Chat respectfully. Harassment, impersonation, and spam are prohibited. You can block or report abusive accounts directly from any profile or conversation."
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsActionRow(
                    icon = Icons.AutoMirrored.Rounded.HelpOutline,
                    title = strings.helpAndSupport,
                    onClick = {
                        infoDialogContent = strings.helpAndSupport to
                            "Need help with Bangla Chat?\n• Start chats from the Contacts tab by searching @username or phone.\n• Manage Online Status, Last Seen, Read Receipts, and Search Discovery under Privacy Settings.\n• Export or delete your account anytime from Account settings."
                    }
                )
            }
        }

        // Account Danger Zone: Logout & Delete Account
        item(key = "account_actions") {
            SettingsCardSection(title = strings.settingsAccountSection) {
                SettingsActionRow(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    title = strings.logout,
                    tint = BrandBlue,
                    onClick = { showLogoutDialog = true },
                    testTag = "logout_button"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsActionRow(
                    icon = Icons.Rounded.DeleteForever,
                    title = strings.deleteAccount,
                    tint = DangerRed,
                    onClick = { showDeleteDialog = true },
                    testTag = "delete_account_button"
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }

    // Edit Profile Modal Sheet
    if (showEditProfileSheet) {
        var name by remember(myProfile) { mutableStateOf(myProfile?.name.orEmpty()) }
        var username by remember(myProfile) { mutableStateOf(myProfile?.username.orEmpty()) }
        var about by remember(myProfile) { mutableStateOf(myProfile?.about.orEmpty()) }
        var phone by remember(myPrivateUser) { mutableStateOf(myPrivateUser?.phone.orEmpty()) }

        ModalBottomSheet(onDismissRequest = { showEditProfileSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = strings.editProfile,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                UserAvatar(
                    name = name.ifBlank { "BC" },
                    photoUrl = myProfile?.photoUrl.orEmpty(),
                    size = 84.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        avatarPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.changePhoto)
                }

                if (avatarUploadState is UploadState.Uploading) {
                    LinearProgressIndicator(
                        progress = { avatarUploadState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text(strings.displayNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(30) },
                    label = { Text(strings.usernameLabel) },
                    prefix = { Text("@") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(20) },
                    label = { Text(strings.phoneLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = about,
                    onValueChange = { about = it.take(180) },
                    label = { Text(strings.aboutLabel) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        onUpdateProfile(name, username, about, phone)
                        showEditProfileSheet = false
                    },
                    enabled = name.trim().isNotEmpty() && username.trim().length >= 3,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button")
                ) {
                    Text(strings.saveChanges)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Privacy Settings Modal Sheet
    if (showPrivacySheet) {
        var discoverable by remember(myProfile) { mutableStateOf(myProfile?.discoverable ?: true) }
        var showOnline by remember(myProfile) { mutableStateOf(myProfile?.showOnlineStatus ?: true) }
        var showLastSeen by remember(myProfile) { mutableStateOf(myProfile?.showLastSeen ?: true) }
        var showReadReceipts by remember(myProfile) { mutableStateOf(myProfile?.showReadReceipts ?: true) }
        var showProfilePhoto by remember(myProfile) { mutableStateOf(myProfile?.showProfilePhoto ?: true) }
        var showAbout by remember(myProfile) { mutableStateOf(myProfile?.showAbout ?: true) }

        ModalBottomSheet(onDismissRequest = { showPrivacySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = strings.privacySettings,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                PrivacyToggleRow(
                    title = strings.discoverableSetting,
                    subtitle = strings.discoverableDesc,
                    checked = discoverable,
                    onCheckedChange = { discoverable = it }
                )
                PrivacyToggleRow(
                    title = strings.showProfilePhotoSetting,
                    subtitle = null,
                    checked = showProfilePhoto,
                    onCheckedChange = { showProfilePhoto = it }
                )
                PrivacyToggleRow(
                    title = strings.showAboutSetting,
                    subtitle = null,
                    checked = showAbout,
                    onCheckedChange = { showAbout = it }
                )
                PrivacyToggleRow(
                    title = strings.showOnlineSetting,
                    subtitle = null,
                    checked = showOnline,
                    onCheckedChange = { showOnline = it }
                )
                PrivacyToggleRow(
                    title = strings.showLastSeenSetting,
                    subtitle = null,
                    checked = showLastSeen,
                    onCheckedChange = { showLastSeen = it }
                )
                PrivacyToggleRow(
                    title = strings.showReadReceiptsSetting,
                    subtitle = null,
                    checked = showReadReceipts,
                    onCheckedChange = { showReadReceipts = it }
                )

                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = {
                        onUpdatePrivacy(
                            discoverable,
                            showOnline,
                            showLastSeen,
                            showReadReceipts,
                            showProfilePhoto,
                            showAbout
                        )
                        showPrivacySheet = false
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(strings.saveChanges)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showPinSetupDialog) {
        SetAppLockPinDialog(
            onDismiss = { showPinSetupDialog = false },
            onSavePin = { pin ->
                showPinSetupDialog = false
                onEnableAppLock(pin)
            }
        )
    }

    // Blocked Users Sheet
    if (showBlockedSheet) {
        ModalBottomSheet(onDismissRequest = { showBlockedSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = strings.blockedUsersSetting,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (blockedUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.noBlockedUsers,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(blockedUsers, key = { it.blockedUid }) { blocked ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = blocked.blockedName,
                                    photoUrl = blocked.blockedPhotoUrl,
                                    size = 44.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(blocked.blockedName, style = MaterialTheme.typography.titleSmall)
                                    if (blocked.blockedUsername.isNotBlank()) {
                                        Text(
                                            "@${blocked.blockedUsername}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                TextButton(onClick = { onUnblockUser(blocked.blockedUid) }) {
                                    Text(strings.unblockUser, color = BrandBlue)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(strings.logoutConfirmTitle) },
            text = { Text(strings.logoutConfirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(strings.logout)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(strings.deleteAccountTitle) },
            text = { Text(strings.deleteAccountMessage) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    }
                ) {
                    Text(strings.deleteAccount, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Info / Policy / Support Dialog
    infoDialogContent?.let { (title, body) ->
        AlertDialog(
            onDismissRequest = { infoDialogContent = null },
            title = { Text(title) },
            text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { infoDialogContent = null }) {
                    Text(strings.close)
                }
            }
        )
    }
}

@Composable
private fun SettingsCardSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = BrandPurple,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (tint == DangerRed) DangerRed else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PrivacyToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
