# Bangla Chat (Android — Kotlin + Jetpack Compose + Firebase)

**Bangla Chat** is a native Android real-time messaging application built with Kotlin, Jetpack Compose, Material 3, Firebase Authentication, Cloud Firestore, Firebase Storage, and Firebase Cloud Messaging.

- **App Name:** Bangla Chat
- **Application ID (Package Name):** `com.mhridoy.banglacha`
- **Minimum Android:** Android 7.0 (API 24)
- **Languages:** Bangla (Default) and English (instant live toggle in Settings)

---

## Architecture & Key Features

- **Authentication:** Google Sign-In via Jetpack Credential Manager (`GetSignInWithGoogleOption` + silent `GetGoogleIdOption`), persistent authenticated sessions, safe sign-out (`NonCancellable`), and account deletion with automatic Google reauthentication.
- **PII-Isolated Firestore Schema:**
  - `/profiles/{uid}` — Publicly discoverable profile fields (no email/FCM token PII).
  - `/users/{uid}` — Strictly owner-only private account details, email, phone, and FCM token.
  - `/users/{uid}/blockedUsers/{blockedUid}` — Blocked user accounts (enforced inside Firestore Security Rules).
  - `/users/{uid}/favorites/{favoriteUid}` — Pinned favorite contacts.
  - `/users/{uid}/callLogs/{callId}` — Historical call log records (live WebRTC calling is transparently labeled as unavailable).
  - `/chats/{chatId}` & `/chats/{chatId}/messages/{messageId}` — Real-time two-way messaging, delivered/read receipts, unread counts, replies, emoji reactions, forwarding, soft-delete, typing indicators, and load-older pagination.
  - `/reports/{reportId}` — User account moderation reports.
- **Media Sharing:** Zero-permission Android Photo Picker (`PickVisualMedia`), Camera capture (`FileProvider`), and PDF/Document picker (`OpenDocument`) backed by Firebase Storage with file-size & MIME validation, upload progress, and retry recovery.
- **Push Notifications:** `BanglaChatMessagingService`, Android 13+ `POST_NOTIFICATIONS` permission flow, notification channels, preview privacy toggle, deep-link tap to open the exact chat, and server-side Cloud Function (`functions/index.js`).

---

## Firebase Console Setup Notes

AI Studio automatically provisioned **Cloud Firestore** (named database in `app/src/main/res/values/firebase_applet_config.xml`) and **Firebase Auth (Google Sign-In)** for `com.mhridoy.banglacha` with your debug signing fingerprints.

To enable additional Firebase services in the Firebase Console:

1. **Cloudinary Media Uploads (Free, No Credit Card — Avatar, Chat Photos & Documents):**
   - Configured with public `CLOUDINARY_CLOUD_NAME` (`hb8ukrw2`) and Unsigned Upload Preset (`bangla_chat`) in `app/build.gradle.kts` (`buildConfigField`).
   - No Cloudinary API Key or API Secret is stored in the APK.
   - To use a different Cloudinary account, update `CLOUDINARY_CLOUD_NAME` and `CLOUDINARY_UPLOAD_PRESET` in `app/build.gradle.kts`.
2. **Firebase Cloud Messaging (Server-Side Push Notifications):**
   - Deploy the Cloud Function in `/functions`:
     ```bash
     cd functions && npm install
     firebase deploy --only functions
     ```
   - Never embed Firebase Admin credentials or server keys in the Android app.
3. **Optional Additional Auth Providers (Phone OTP `+880` / Email & Password):**
   - Open Firebase Console → **Authentication → Sign-in method** to enable **Phone** (requires SHA-1 & SHA-256 registered in Project Settings → Your Android App, Play Integrity enabled, and Blaze billing for production SMS quotas) or **Email/Password**.
4. **Using a Different Firebase Project (`google-services.json`):**
   - Download `google-services.json` for `com.mhridoy.banglacha` from Firebase Console → **Project Settings** and replace `app/google-services.json`.
   - Update `app/src/main/res/values/firebase_applet_config.xml` to match your Firestore database ID (or `(default)` if using the default database in your own project).

---

## Building the APK in Android Studio

### 1. Debug APK
- Open the project in Android Studio and wait for Gradle sync.
- Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**, or run:
  ```bash
  ./gradlew :app:assembleDebug
  ```
- Output: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Release APK (Unsigned)
- Run:
  ```bash
  ./gradlew :app:assembleRelease
  ```

### 3. Signed Release APK / Play Store AAB
- In Android Studio, select **Build → Generate Signed App Bundle / APK...**
- Choose **APK** (direct device install) or **Android App Bundle (AAB)** (Google Play).
- Create or select your upload keystore (`.jks`), enter keystore password, key alias, and key password.
- Register the release keystore's **SHA-1** and **SHA-256** fingerprints in Firebase Console → **Project Settings → Your apps → `com.mhridoy.banglacha`** so Google Sign-In works in release builds.
- Select the `release` build variant and click **Create**.
