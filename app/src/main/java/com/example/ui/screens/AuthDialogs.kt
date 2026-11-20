package com.example.ui.screens

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.normalizePhoneNumber
import com.example.data.signInWithPhoneCredential
import com.example.data.startPhoneNumberVerification
import com.example.ui.components.GoogleGLogo
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPurple
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginBottomSheet(
    onDismiss: () -> Unit,
    onPhoneAuthSuccess: (normalizedPhone: String) -> Unit,
    onContinueWithGoogleFallback: (normalizedPhone: String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var phoneInput by rememberSaveable { mutableStateOf("+880") }
    var otpInput by rememberSaveable { mutableStateOf("") }
    var verificationId by rememberSaveable { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var resendSeconds by rememberSaveable { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var providerUnavailable by remember { mutableStateOf(false) }

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1000L)
            resendSeconds -= 1
        }
    }

    fun sendOtp(useResendToken: Boolean) {
        val normalized = normalizePhoneNumber(phoneInput)
        if (!normalized.startsWith("+") || normalized.count { it.isDigit() } < 10) {
            errorMessage = strings.phoneInvalid
            return
        }
        errorMessage = null
        providerUnavailable = false
        isLoading = true
        startPhoneNumberVerification(
            context = context,
            rawPhoneNumber = normalized,
            resendToken = if (useResendToken) resendToken else null,
            onCodeSent = { verId, token ->
                isLoading = false
                verificationId = verId
                resendToken = token
                resendSeconds = 60
            },
            onAutoVerified = { credential ->
                signInWithPhoneCredential(
                    credential = credential,
                    scope = scope,
                    onAuthSuccess = {
                        isLoading = false
                        onPhoneAuthSuccess(normalized)
                    },
                    onAuthError = { msg ->
                        isLoading = false
                        errorMessage = msg
                    }
                )
            },
            onVerificationFailed = { msg, unavailable ->
                isLoading = false
                providerUnavailable = unavailable
                errorMessage = if (unavailable) null else msg
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (verificationId == null) Icons.Rounded.Call else Icons.Rounded.Password,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (verificationId == null) strings.phoneLoginSheetTitle else strings.enterOtpTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (verificationId == null) {
                    strings.phoneLoginSheetSubtitle
                } else {
                    "${strings.enterOtpSubtitle} (${normalizePhoneNumber(phoneInput)})"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (verificationId == null) {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { raw ->
                        phoneInput = raw.filter { it.isDigit() || it == '+' || it == '-' || it == ' ' }.take(20)
                        errorMessage = null
                    },
                    label = { Text(strings.phoneLabel) },
                    placeholder = { Text("+88017XXXXXXXX") },
                    leadingIcon = { Icon(Icons.Rounded.Call, contentDescription = null, tint = BrandBlue) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phone_login_input")
                )
            } else {
                OutlinedTextField(
                    value = otpInput,
                    onValueChange = { raw ->
                        otpInput = raw.filter { it.isDigit() }.take(6)
                        errorMessage = null
                    },
                    label = { Text(strings.otpCodeLabel) },
                    placeholder = { Text("123456") },
                    leadingIcon = { Icon(Icons.Rounded.Password, contentDescription = null, tint = BrandBlue) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phone_otp_input")
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
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

            if (providerUnavailable) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = BrandBlue.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.phoneOtpUnavailableNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val normalized = normalizePhoneNumber(phoneInput)
                        onContinueWithGoogleFallback(normalized)
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPurple,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("phone_fallback_google_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        GoogleGLogo(size = 18.dp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.continueWithGoogleUsingPhone,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (verificationId == null) {
                Button(
                    onClick = { sendOtp(useResendToken = false) },
                    enabled = !isLoading,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("phone_send_otp_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.sendingOtp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(strings.sendOtpAction, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = {
                        val code = otpInput.trim()
                        val verId = verificationId.orEmpty()
                        if (code.length < 6) {
                            errorMessage = strings.otpInvalid
                            return@Button
                        }
                        errorMessage = null
                        isLoading = true
                        val credential = PhoneAuthProvider.getCredential(verId, code)
                        signInWithPhoneCredential(
                            credential = credential,
                            scope = scope,
                            onAuthSuccess = {
                                isLoading = false
                                onPhoneAuthSuccess(normalizePhoneNumber(phoneInput))
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("phone_verify_otp_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(strings.verifyingOtp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(strings.verifyAndLogin, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            verificationId = null
                            otpInput = ""
                            errorMessage = null
                        },
                        enabled = !isLoading
                    ) {
                        Text(strings.changePhoneNumber)
                    }
                    TextButton(
                        onClick = { sendOtp(useResendToken = true) },
                        enabled = !isLoading && resendSeconds == 0
                    ) {
                        Text(
                            if (resendSeconds > 0) "${strings.resendOtp} (${resendSeconds}s)"
                            else strings.resendOtp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailLoginBottomSheet(
    onDismiss: () -> Unit,
    onContinueEmailAuth: (email: String) -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var emailInput by rememberSaveable { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.emailLoginSheetTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.emailLoginSheetSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = emailInput,
                onValueChange = {
                    emailInput = it.trim().take(120)
                    errorMessage = null
                },
                label = { Text(strings.emailLabel) },
                placeholder = { Text(strings.emailInputHint) },
                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = BrandBlue) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = errorMessage != null,
                supportingText = errorMessage?.let { msg ->
                    { Text(msg, color = MaterialTheme.colorScheme.error) }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("email_login_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val clean = emailInput.trim()
                    if (clean.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(clean).matches()) {
                        errorMessage = strings.emailInvalid
                        return@Button
                    }
                    onContinueEmailAuth(clean)
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("email_continue_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    GoogleGLogo(size = 18.dp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.continueWithEmailAction,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountChooserSheet(
    onDismiss: () -> Unit,
    onRegisterWithGoogle: () -> Unit,
    onRegisterWithPhone: () -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BrandPurple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PersonAdd,
                    contentDescription = null,
                    tint = BrandPurple,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.createAccountSheetTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.createAccountSheetSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onRegisterWithGoogle,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("register_via_google_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    GoogleGLogo(size = 18.dp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = strings.registerWithGoogle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onRegisterWithPhone,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, BrandPurple),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("register_via_phone_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = strings.registerWithPhone,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
