package com.example.data

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Silent Auto-Sign-In on startup using GetGoogleIdOption (one credential option per request).
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val activity = context.findActivity() ?: run {
                onUnauthenticated()
                return@launch
            }
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Google Sign-In using GetSignInWithGoogleOption (one credential option per request).
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("Unable to locate Activity context for Google Sign-In")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

/**
 * Safe Sign-Out inside NonCancellable: runs pre-signout cleanup (e.g. offline presence)
 * and clears Credential Manager state BEFORE signing out of FirebaseAuth.
 */
fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onBeforeSignOut: suspend () -> Unit = {},
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                onBeforeSignOut()
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Pre-signout cleanup warning: ${e.message}")
            }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

/**
 * Account deletion with automatic Google reauthentication when Firebase requires recent login.
 */
suspend fun deleteCurrentAccountWithReauth(
    context: Context,
    credentialManager: CredentialManager,
    onBeforeAuthDelete: suspend (String) -> Unit
): Result<Unit> {
    val user = Firebase.auth.currentUser
        ?: return Result.failure(IllegalStateException("No authenticated user"))

    return try {
        withContext(NonCancellable) {
            onBeforeAuthDelete(user.uid)
            try {
                user.delete().await()
            } catch (_: FirebaseAuthRecentLoginRequiredException) {
                val clientId = context.getString(R.string.default_web_client_id)
                val activity = context.findActivity()
                    ?: throw IllegalStateException("Activity context required for reauthentication")
                val option = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                val req = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val cred = credentialManager.getCredential(activity, req).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    user.reauthenticate(GoogleAuthProvider.getCredential(token, null)).await()
                    user.delete().await()
                } else {
                    throw IllegalStateException("Google reauthentication failed")
                }
            }
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
                // Non-fatal
            }
        }
        Result.success(Unit)
    } catch (e: GetCredentialCancellationException) {
        Log.w(AUTH_TAG, "Account deletion reauthentication cancelled: ${e.message}", e)
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Account deletion failed", e)
        Result.failure(e)
    }
}

/**
 * Normalizes a Bangladesh or international phone number into E.164 format.
 * Examples: "01712345678" -> "+8801712345678", "8801712345678" -> "+8801712345678".
 */
fun normalizePhoneNumber(raw: String): String {
    val cleaned = raw.trim().replace(Regex("[\\s\\-()]"), "")
    return when {
        cleaned.startsWith("+") -> cleaned
        cleaned.startsWith("880") && cleaned.length >= 13 -> "+$cleaned"
        cleaned.startsWith("01") && cleaned.length == 11 -> "+88$cleaned"
        cleaned.startsWith("1") && cleaned.length == 10 -> "+880$cleaned"
        else -> cleaned
    }
}

/**
 * Initiates real Firebase PhoneAuthProvider SMS OTP verification.
 */
fun startPhoneNumberVerification(
    context: Context,
    rawPhoneNumber: String,
    resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
    onAutoVerified: (PhoneAuthCredential) -> Unit,
    onVerificationFailed: (message: String, isProviderUnavailable: Boolean) -> Unit
) {
    val activity = context.findActivity()
    if (activity == null) {
        onVerificationFailed("Unable to locate Activity context for Phone Verification", false)
        return
    }
    val e164Phone = normalizePhoneNumber(rawPhoneNumber)
    if (!e164Phone.startsWith("+") || e164Phone.count { it.isDigit() } < 10) {
        onVerificationFailed("Invalid phone number format", false)
        return
    }

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            onAutoVerified(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.w(AUTH_TAG, "Phone verification failed: ${e.message}", e)
            val errorCode = (e as? FirebaseAuthException)?.errorCode.orEmpty()
            val msg = e.localizedMessage.orEmpty()
            val providerUnavailable = errorCode.equals("ERROR_OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                msg.contains("operation is not allowed", ignoreCase = true) ||
                msg.contains("BILLING_NOT_ENABLED", ignoreCase = true) ||
                msg.contains("not authorized", ignoreCase = true) ||
                msg.contains("reCAPTCHA", ignoreCase = true) ||
                msg.contains("missing a valid app identifier", ignoreCase = true)
            onVerificationFailed(
                msg.ifBlank { "Phone verification failed" },
                providerUnavailable
            )
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            onCodeSent(verificationId, token)
        }
    }

    try {
        val builder = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(e164Phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (resendToken != null) {
            builder.setForceResendingToken(resendToken)
        }
        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Unable to launch PhoneAuthProvider", e)
        onVerificationFailed(e.localizedMessage ?: "Phone verification failed", true)
    }
}

/**
 * Completes sign-in with a PhoneAuthCredential (either auto-verified or built from OTP code).
 */
fun signInWithPhoneCredential(
    credential: PhoneAuthCredential,
    scope: CoroutineScope,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit
) {
    scope.launch {
        try {
            Firebase.auth.signInWithCredential(credential).await()
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Phone credential sign-in failed", e)
            onAuthError(e.localizedMessage ?: "Verification failed")
        }
    }
}

