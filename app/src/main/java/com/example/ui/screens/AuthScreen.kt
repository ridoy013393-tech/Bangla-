package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.data.BanglaChatRepository
import com.example.data.onGoogleSignInClicked
import com.example.model.AppLanguage
import com.example.ui.components.AuthHeroSceneBackdrop
import com.example.ui.components.BanglaChatLogoMark
import com.example.ui.components.BanglaChatWordmark
import com.example.ui.components.GoogleGLogo
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.OnlineGreen
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthEntryMode {
    LOGIN,
    REGISTER
}

private enum class AuthLoadingChannel {
    GOOGLE,
    EMAIL,
    REGISTER
}

private enum class UsernameCheckStatus {
    EMPTY_OR_SHORT,
    CHECKING,
    AVAILABLE,
    TAKEN,
    INVALID
}

@Composable
fun AuthScreen(
    currentLanguage: AppLanguage,
    onToggleLanguage: (AppLanguage) -> Unit,
    onSelectAuthMode: (AuthEntryMode) -> Unit,
    onPendingPhoneHint: (String) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var loadingChannel by remember { mutableStateOf<AuthLoadingChannel?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPhoneSheet by remember { mutableStateOf(false) }
    var showEmailSheet by remember { mutableStateOf(false) }
    var showCreateAccountSheet by remember { mutableStateOf(false) }

    val skyBackgroundBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D1429),
                Color(0xFF141D38),
                Color(0xFF162244),
                Color(0xFF101933)
            )
        )
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFFE3F0FF),
                0.28f to Color(0xFFF6F2FF),
                0.56f to Color(0xFFEAE8FF),
                0.76f to Color(0xFFDCEBFF),
                1.00f to Color(0xFFEAEFFF)
            )
        )
    }

    val titleInkColor = if (isDark) Color(0xFFEAF0FF) else Color(0xFF1A2444)
    val whitePillBg = if (isDark) Color(0xFF1D2744) else Color.White
    val whitePillContent = if (isDark) Color(0xFFF1F5FF) else Color(0xFF171E36)
    val whitePillBorder = if (isDark) Color(0xFF334470) else Color(0xFFDCE4F8)
    val createAccountText = if (isDark) Color(0xFFDCC6FF) else Color(0xFF2D196D)
    val createAccountBg = if (isDark) Color(0xFF182240).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.55f)
    val dividerLineColor = if (isDark) Color(0xFF455585) else Color(0xFFB3BDE0)
    val dividerTextColor = if (isDark) Color(0xFFD0DAF8) else Color(0xFF2B2259)

    fun launchGoogleAuth(mode: AuthEntryMode, channel: AuthLoadingChannel) {
        errorMessage = null
        loadingChannel = channel
        onSelectAuthMode(mode)
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                loadingChannel = null
                onAuthSuccess()
            },
            onAuthError = { msg ->
                loadingChannel = null
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                loadingChannel = null
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(skyBackgroundBrush)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Unobtrusive top-right language switcher
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = CircleShape,
            color = if (isDark) Color(0xFF1D2847).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF31416B) else Color(0xFFD6E2FA))
        ) {
            TextButton(
                onClick = {
                    val next = if (currentLanguage == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
                    onToggleLanguage(next)
                },
                modifier = Modifier.testTag("auth_language_toggle")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = strings.languageSetting,
                    tint = BrandBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (currentLanguage == AppLanguage.BANGLA) "English" else "বাংলা",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleInkColor
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Brand Centerpiece matching mockup
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .padding(top = 46.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BanglaChatLogoMark(size = 128.dp)
                Spacer(modifier = Modifier.height(10.dp))
                BanglaChatWordmark(fontSize = 40.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.taglineLatin,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp
                    ),
                    color = titleInkColor.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = strings.taglineLineOne,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = titleInkColor,
                    textAlign = TextAlign.Center
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = strings.taglineLineTwo,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = titleInkColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEF2B3A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Full-bleed atmospheric world map + looped paper airplane + royal-blue mosque skyline
            AuthHeroSceneBackdrop(
                isDarkTheme = isDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            )

            // Bottom Login & Create Account Actions matching mockup
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // 1. Solid Royal-Blue Pill: ফোন দিয়ে লগইন
                Button(
                    onClick = {
                        errorMessage = null
                        onSelectAuthMode(AuthEntryMode.LOGIN)
                        showPhoneSheet = true
                    },
                    enabled = loadingChannel == null,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0E6CF5),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("auth_phone_login_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = strings.loginWithPhone,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Clean White Pill: ইমেইল দিয়ে লগইন
                Surface(
                    onClick = {
                        if (loadingChannel == null) {
                            errorMessage = null
                            onSelectAuthMode(AuthEntryMode.LOGIN)
                            showEmailSheet = true
                        }
                    },
                    enabled = loadingChannel == null,
                    shape = CircleShape,
                    color = whitePillBg,
                    border = BorderStroke(1.dp, whitePillBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("auth_email_login_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (loadingChannel == AuthLoadingChannel.EMAIL) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BrandBlue,
                                strokeWidth = 2.2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.signingIn,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = whitePillContent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Email,
                                contentDescription = null,
                                tint = whitePillContent,
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = strings.loginWithEmail,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = whitePillContent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Clean White Pill: Google দিয়ে লগইন (authentic 4-color Google G)
                Surface(
                    onClick = {
                        if (loadingChannel == null) {
                            launchGoogleAuth(AuthEntryMode.LOGIN, AuthLoadingChannel.GOOGLE)
                        }
                    },
                    enabled = loadingChannel == null,
                    shape = CircleShape,
                    color = whitePillBg,
                    border = BorderStroke(1.dp, whitePillBorder),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("auth_google_login_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (loadingChannel == AuthLoadingChannel.GOOGLE) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BrandBlue,
                                strokeWidth = 2.2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.signingIn,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = whitePillContent
                            )
                        } else {
                            GoogleGLogo(size = 22.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = strings.loginWithGoogle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = whitePillContent
                            )
                        }
                    }
                }

                // Divider: ─────── অথবা ───────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = dividerLineColor
                    )
                    Text(
                        text = strings.orDivider,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = dividerTextColor,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = dividerLineColor
                    )
                }

                // 4. Gradient-Outlined Pill: নতুন অ্যাকাউন্ট তৈরি করুন
                val outlineGradient = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF4F6BF4), Color(0xFF8C46F2), Color(0xFF3E78F5))
                )
                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        showCreateAccountSheet = true
                    },
                    enabled = loadingChannel == null,
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, outlineGradient),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = createAccountBg,
                        contentColor = createAccountText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_register_button")
                ) {
                    if (loadingChannel == AuthLoadingChannel.REGISTER) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrandPurple,
                            strokeWidth = 2.2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.signingIn,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = createAccountText
                        )
                    } else {
                        Text(
                            text = strings.createNewAccount,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = createAccountText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showPhoneSheet) {
        PhoneLoginBottomSheet(
            onDismiss = { showPhoneSheet = false },
            onPhoneAuthSuccess = { normalizedPhone ->
                showPhoneSheet = false
                onPendingPhoneHint(normalizedPhone)
                onAuthSuccess()
            },
            onContinueWithGoogleFallback = { normalizedPhone ->
                showPhoneSheet = false
                onPendingPhoneHint(normalizedPhone)
                launchGoogleAuth(AuthEntryMode.LOGIN, AuthLoadingChannel.GOOGLE)
            }
        )
    }

    if (showEmailSheet) {
        EmailLoginBottomSheet(
            onDismiss = { showEmailSheet = false },
            onContinueEmailAuth = {
                showEmailSheet = false
                launchGoogleAuth(AuthEntryMode.LOGIN, AuthLoadingChannel.EMAIL)
            }
        )
    }

    if (showCreateAccountSheet) {
        CreateAccountChooserSheet(
            onDismiss = { showCreateAccountSheet = false },
            onRegisterWithGoogle = {
                showCreateAccountSheet = false
                launchGoogleAuth(AuthEntryMode.REGISTER, AuthLoadingChannel.REGISTER)
            },
            onRegisterWithPhone = {
                showCreateAccountSheet = false
                onSelectAuthMode(AuthEntryMode.REGISTER)
                showPhoneSheet = true
            }
        )
    }
}

@Composable
fun RegistrationCompletionScreen(
    firebaseUser: FirebaseUser,
    cameFromLoginAttempt: Boolean,
    initialPhoneHint: String = "",
    currentLanguage: AppLanguage,
    repository: BanglaChatRepository,
    onToggleLanguage: (AppLanguage) -> Unit,
    onRegistrationComplete: () -> Unit,
    onSwitchAccount: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    var displayName by rememberSaveable(firebaseUser.uid) {
        mutableStateOf(firebaseUser.displayName?.trim().orEmpty().take(60))
    }
    var phone by rememberSaveable(firebaseUser.uid) {
        mutableStateOf(
            firebaseUser.phoneNumber?.trim().orEmpty()
                .ifBlank { initialPhoneHint.trim() }
                .take(20)
        )
    }
    var username by rememberSaveable(firebaseUser.uid) {
        mutableStateOf("")
    }

    var usernameStatus by remember { mutableStateOf(UsernameCheckStatus.EMPTY_OR_SHORT) }
    var isSubmitting by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    // Live debounced global uniqueness verification against /usernames/{username}
    LaunchedEffect(username) {
        val clean = BanglaChatRepository.normalizeUsername(username)
        if (clean.length < 3) {
            usernameStatus = UsernameCheckStatus.EMPTY_OR_SHORT
            return@LaunchedEffect
        }
        if (!BanglaChatRepository.isValidUsernameFormat(clean)) {
            usernameStatus = UsernameCheckStatus.INVALID
            return@LaunchedEffect
        }
        usernameStatus = UsernameCheckStatus.CHECKING
        delay(400L)
        repository.isUsernameAvailable(clean, excludeUid = firebaseUser.uid)
            .onSuccess { available ->
                usernameStatus = if (available) {
                    UsernameCheckStatus.AVAILABLE
                } else {
                    UsernameCheckStatus.TAKEN
                }
            }
            .onFailure {
                usernameStatus = UsernameCheckStatus.INVALID
            }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
    ) {
        // Language toggle
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            tonalElevation = 2.dp
        ) {
            TextButton(
                onClick = {
                    val next = if (currentLanguage == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
                    onToggleLanguage(next)
                },
                modifier = Modifier.testTag("register_language_toggle")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = strings.languageSetting,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentLanguage == AppLanguage.BANGLA) "English" else "বাংলা",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            BanglaChatLogoMark(size = 76.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strings.completeRegistrationTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.completeRegistrationSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            val accountBadge = firebaseUser.email?.takeIf { it.isNotBlank() }
                ?: firebaseUser.phoneNumber?.takeIf { it.isNotBlank() }
                .orEmpty()
            if (accountBadge.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = BrandBlue.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = accountBadge,
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (cameFromLoginAttempt) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    color = BrandPurple.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = BrandPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.noAccountFoundRegisterPrompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Display Name
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it.take(60) },
                        label = { Text(strings.displayNameLabel) },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = BrandBlue) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_name_input")
                    )

                    // 2. Phone Number
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { raw ->
                            phone = raw.filter { it.isDigit() || it == '+' || it == '-' || it == ' ' }.take(20)
                        },
                        label = { Text(strings.phoneLabel) },
                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null, tint = BrandBlue) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_phone_input")
                    )

                    // 3. Unique @username
                    OutlinedTextField(
                        value = username,
                        onValueChange = { raw ->
                            username = raw
                                .trimStart('@')
                                .lowercase()
                                .filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }
                                .take(30)
                        },
                        label = { Text(strings.usernameLabel) },
                        placeholder = { Text(strings.usernamePlaceholder) },
                        prefix = {
                            Text(
                                text = "@",
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        },
                        trailingIcon = {
                            when (usernameStatus) {
                                UsernameCheckStatus.CHECKING -> CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = BrandBlue
                                )
                                UsernameCheckStatus.AVAILABLE -> Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = strings.usernameAvailable,
                                    tint = OnlineGreen
                                )
                                UsernameCheckStatus.TAKEN, UsernameCheckStatus.INVALID -> Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = strings.usernameTaken,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                UsernameCheckStatus.EMPTY_OR_SHORT -> Unit
                            }
                        },
                        supportingText = {
                            val (msg, color) = when (usernameStatus) {
                                UsernameCheckStatus.EMPTY_OR_SHORT ->
                                    strings.usernameFormatHint to MaterialTheme.colorScheme.onSurfaceVariant
                                UsernameCheckStatus.CHECKING ->
                                    strings.usernameChecking to BrandBlue
                                UsernameCheckStatus.AVAILABLE ->
                                    "${strings.usernameAvailable} (@$username)" to OnlineGreen
                                UsernameCheckStatus.TAKEN ->
                                    strings.usernameTaken to MaterialTheme.colorScheme.error
                                UsernameCheckStatus.INVALID ->
                                    strings.usernameInvalid to MaterialTheme.colorScheme.error
                            }
                            Text(text = msg, color = color, style = MaterialTheme.typography.labelSmall)
                        },
                        isError = usernameStatus == UsernameCheckStatus.TAKEN ||
                            usernameStatus == UsernameCheckStatus.INVALID,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_username_input")
                    )

                    if (formError != null) {
                        Text(
                            text = formError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            formError = null
                            val cleanName = displayName.trim()
                            val cleanPhone = phone.trim()
                            val cleanUsername = BanglaChatRepository.normalizeUsername(username)
                            when {
                                cleanName.isBlank() -> formError = strings.nameInvalid
                                cleanPhone.count { it.isDigit() } < 6 -> formError = strings.phoneInvalid
                                !BanglaChatRepository.isValidUsernameFormat(cleanUsername) ->
                                    formError = strings.usernameInvalid
                                usernameStatus == UsernameCheckStatus.TAKEN ->
                                    formError = strings.usernameTaken
                                else -> {
                                    isSubmitting = true
                                    scope.launch {
                                        repository.registerNewUserAccount(
                                            firebaseUser = firebaseUser,
                                            displayName = cleanName,
                                            phone = cleanPhone,
                                            rawUsername = cleanUsername
                                        ).onSuccess {
                                            isSubmitting = false
                                            onRegistrationComplete()
                                        }.onFailure { err ->
                                            isSubmitting = false
                                            if (err is BanglaChatRepository.UsernameAlreadyTakenException) {
                                                usernameStatus = UsernameCheckStatus.TAKEN
                                                formError = strings.usernameTaken
                                            } else {
                                                formError = err.localizedMessage ?: "Registration failed"
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting && usernameStatus != UsernameCheckStatus.CHECKING,
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("complete_registration_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(strings.creatingAccount, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.createAccountButton,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = onSwitchAccount,
                enabled = !isSubmitting,
                modifier = Modifier.testTag("switch_google_account_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.switchGoogleAccount,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
