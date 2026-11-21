package com.example.ui.i18n

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.model.AppLanguage

/**
 * Interface-backed localization table implemented by singleton objects so it never
 * hits the JVM/DEX 255-parameter constructor limit regardless of string count.
 */
@Immutable
interface AppStrings {
    val appTitle: String
    val taglineLatin: String
    val taglineHeadline: String
    val continueWithGoogle: String
    val signingIn: String
    val secureAuthNotice: String
    val tabChats: String
    val tabContacts: String
    val tabCalls: String
    val tabSettings: String
    val onlineBannerTitle: String
    val onlineBannerSubtitle: String
    val offlineBanner: String
    val searchChatsHint: String
    val searchUsersHint: String
    val newChat: String
    val noChatsTitle: String
    val noChatsSubtitle: String
    val startFirstChat: String
    val loadingText: String
    val retry: String
    val photoMessagePreview: String
    val documentMessagePreview: String
    val messageDeletedPreview: String
    val typingIndicator: String
    val onlineStatus: String
    val offlineStatus: String
    val lastSeenPrefix: String
    val typeMessageHint: String
    val send: String
    val loadOlderMessages: String
    val reply: String
    val replyingTo: String
    val copy: String
    val copiedToClipboard: String
    val forward: String
    val forwardedLabel: String
    val deleteMessage: String
    val react: String
    val editMessage: String
    val editingMessage: String
    val editedLabel: String
    val starMessage: String
    val unstarMessage: String
    val starredMessagesTitle: String
    val noStarredMessages: String
    val pinMessage: String
    val unpinMessage: String
    val pinnedMessageLabel: String
    val searchInChat: String
    val searchInChatHint: String
    val matchesFound: String
    val disappearingMessages: String
    val disappearingOff: String
    val disappearing24h: String
    val disappearing7d: String
    val disappearingBanner24h: String
    val disappearingBanner7d: String
    val pinChat: String
    val unpinChat: String
    val muteChat: String
    val unmuteChat: String
    val archiveChat: String
    val unarchiveChat: String
    val archivedChatsTitle: String
    val chatWallpaperSetting: String
    val wallpaperDefault: String
    val wallpaperMint: String
    val wallpaperOcean: String
    val wallpaperViolet: String
    val wallpaperPeach: String
    val wallpaperMidnight: String
    val chatFontSizeSetting: String
    val fontSmall: String
    val fontMedium: String
    val fontLarge: String
    val showProfilePhotoSetting: String
    val showAboutSetting: String
    val screenSecuritySetting: String
    val screenSecurityDesc: String
    val appLockSetting: String
    val appLockDesc: String
    val appLockTitle: String
    val appLockSubtitle: String
    val appLockWrongPin: String
    val appLockSetPinTitle: String
    val appLockEnterPinHint: String
    val appLockConfirmPinHint: String
    val appLockPinMismatch: String
    val appLockEnabledToast: String
    val appLockDisabledToast: String
    val attachMediaTitle: String
    val pickFromGallery: String
    val takeCameraPhoto: String
    val shareDocumentPdf: String
    val uploadingMedia: String
    val uploadFailedRetry: String
    val blockedChatBanner: String
    val viewProfile: String
    val favoritesTitle: String
    val registeredUsersTitle: String
    val noUsersFound: String
    val startChatAction: String
    val addFavorite: String
    val removeFavorite: String
    val blockUser: String
    val unblockUser: String
    val reportUser: String
    val reportTitle: String
    val reportReasonSpam: String
    val reportReasonHarassment: String
    val reportReasonFake: String
    val reportReasonOther: String
    val reportDetailsHint: String
    val submitReport: String
    val reportSubmitted: String
    val callsUnavailableTitle: String
    val callsUnavailableDesc: String
    val callsFilterAll: String
    val callsFilterMissed: String
    val callsFilterIncoming: String
    val callsFilterOutgoing: String
    val noCallHistoryTitle: String
    val noCallHistorySubtitle: String
    val settingsAccountSection: String
    val settingsPreferencesSection: String
    val settingsPrivacySection: String
    val settingsSupportSection: String
    val editProfile: String
    val displayNameLabel: String
    val usernameLabel: String
    val phoneLabel: String
    val emailLabel: String
    val aboutLabel: String
    val saveChanges: String
    val changePhoto: String
    val languageSetting: String
    val languageBangla: String
    val languageEnglish: String
    val themeSetting: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val notificationSettings: String
    val enableNotifications: String
    val messagePreviewSetting: String
    val messagePreviewDesc: String
    val privacySettings: String
    val discoverableSetting: String
    val discoverableDesc: String
    val showOnlineSetting: String
    val showLastSeenSetting: String
    val showReadReceiptsSetting: String
    val blockedUsersSetting: String
    val noBlockedUsers: String
    val aboutApp: String
    val privacyPolicy: String
    val termsAndConditions: String
    val helpAndSupport: String
    val logout: String
    val logoutConfirmTitle: String
    val logoutConfirmMessage: String
    val deleteAccount: String
    val deleteAccountTitle: String
    val deleteAccountMessage: String
    val cancel: String
    val confirm: String
    val close: String
    val forwardToTitle: String
    val noChatsToForward: String
    val accountCreatedLabel: String
    val openAttachment: String
    val downloadAttachment: String
    val profileUpdated: String
    val chatStarted: String
    // Voice Notes
    val voiceMessagePreview: String
    val recordVoiceNote: String
    val recordingVoice: String
    val micPermissionRequired: String
    // Group Chat
    val newGroup: String
    val groupNameHint: String
    val selectGroupMembers: String
    val createGroupAction: String
    val groupMembersSuffix: String
    val groupInfoTitle: String
    val groupAdminBadge: String
    val renameGroup: String
    val addMembers: String
    val leaveGroup: String
    val groupCreatedToast: String
    // 24h Status Stories
    val myStatus: String
    val addStatus: String
    val textStatusTab: String
    val photoStatusTab: String
    val statusTextHint: String
    val postStatusAction: String
    val statusPostedToast: String
    val statusViewersLabel: String
    val deleteStatus: String
    // Smart Bangla AI (DeepSeek)
    val aiAssistantTitle: String
    val aiAssistantSubtitle: String
    val aiAssistantBannerBadge: String
    val aiAssistantMemoryBadge: String
    val aiAssistantNewChat: String
    val aiAssistantNewChatToast: String
    val aiAssistantInputHint: String
    val aiAssistantWelcomeTitle: String
    val aiAssistantWelcomeSubtitle: String
    val aiQuickPrompt1: String
    val aiQuickPrompt2: String
    val aiQuickPrompt3: String
    val aiQuickPrompt4: String
    val aiTranslateAction: String
    val aiTranslatedLabel: String
    val aiExplainAction: String
    val aiExplainTitle: String
    val aiExplainOriginalLabel: String
    val aiQuickRepliesAction: String
    val aiWriterAction: String
    val aiWriterTitle: String
    val aiWriterTabRewrite: String
    val aiWriterTabCompose: String
    val aiWriterRewriteHint: String
    val aiWriterIdeaHint: String
    val aiWriterGenerateAction: String
    val aiWriterInsertAction: String
    val aiWriterSendAction: String
    val aiTonePolite: String
    val aiToneCasual: String
    val aiToneProfessional: String
    val aiToneBanglaPolish: String
    val aiToneEnglish: String
    val aiIdeaPreset1: String
    val aiIdeaPreset2: String
    val aiIdeaPreset3: String
    val aiIdeaPreset4: String
    val aiSummarizeChatAction: String
    val aiSummaryTitle: String
    val aiGenerating: String
    val aiPoweredByDeepSeek: String
    // Live Audio/Video Calls
    val voiceCallAction: String
    val videoCallAction: String
    val liveCallsBannerTitle: String
    val liveCallsBannerDesc: String
    val quickCallFriends: String
    val incomingVoiceCall: String
    val incomingVideoCall: String
    val callingStatus: String
    val ringingStatus: String
    val callPeerOfflineNotice: String
    val callNoAnswerTimeout: String
    val connectingStatus: String
    val callEndedStatus: String
    val acceptCall: String
    val declineCall: String
    val endCall: String
    val muteMic: String
    val speakerAudio: String
    val toggleCamera: String
    val flipCamera: String
    val callPermissionsRequired: String
    // Auth Login & Registration + Unique @username
    val loginAction: String
    val registerAction: String
    val loginHintNotice: String
    val registerHintNotice: String
    val checkingAccount: String
    val noAccountFoundRegisterPrompt: String
    val completeRegistrationTitle: String
    val completeRegistrationSubtitle: String
    val usernamePlaceholder: String
    val usernameFormatHint: String
    val usernameChecking: String
    val usernameAvailable: String
    val usernameTaken: String
    val usernameInvalid: String
    val phoneInvalid: String
    val nameInvalid: String
    val createAccountButton: String
    val creatingAccount: String
    val switchGoogleAccount: String
    // Exact @username Search
    val searchExactUsernameHint: String
    val searchButton: String
    val exactUsernameGuideTitle: String
    val exactUsernameGuideSubtitle: String
    val exactUsernameNotFound: String
    val exactUsernameMatchTitle: String
    // WhatsApp-style Chat Filter Pills
    val chatFilterAll: String
    val chatFilterUnread: String
    val chatFilterGroups: String
    val chatFilterFavorites: String
    val noUnreadChatsTitle: String
    val noUnreadChatsSubtitle: String
    val noGroupChatsTitle: String
    val noGroupChatsSubtitle: String
    val noFavoriteChatsTitle: String
    val noFavoriteChatsSubtitle: String
    val showAllChats: String
    // Mockup Login Interface & Phone/Email Auth Sheets
    val taglineLineOne: String
    val taglineLineTwo: String
    val loginWithPhone: String
    val loginWithEmail: String
    val loginWithGoogle: String
    val orDivider: String
    val createNewAccount: String
    val phoneLoginSheetTitle: String
    val phoneLoginSheetSubtitle: String
    val sendOtpAction: String
    val sendingOtp: String
    val enterOtpTitle: String
    val enterOtpSubtitle: String
    val otpCodeLabel: String
    val verifyAndLogin: String
    val verifyingOtp: String
    val resendOtp: String
    val changePhoneNumber: String
    val continueWithGoogleUsingPhone: String
    val phoneOtpUnavailableNote: String
    val otpInvalid: String
    val emailLoginSheetTitle: String
    val emailLoginSheetSubtitle: String
    val emailInputHint: String
    val emailInvalid: String
    val continueWithEmailAction: String
    val createAccountSheetTitle: String
    val createAccountSheetSubtitle: String
    val registerWithGoogle: String
    val registerWithPhone: String
    // Delete for me / everyone
    val deleteForMe: String
    val deleteForEveryone: String
    val deletedForMeToast: String
    // In-chat call bubbles & history
    val callAudioLabel: String
    val callVideoLabel: String
    val callOutgoingAudio: String
    val callOutgoingVideo: String
    val callIncomingAudio: String
    val callIncomingVideo: String
    val callMissedAudio: String
    val callMissedVideo: String
    val callDeclinedLabel: String
    val callNoAnswerLabel: String
    val callTalkedSuffix: String
    val callTapToCallBack: String
    // Pencil FAB action sheet
    val newActionSheetTitle: String
    val newChatSubtitle: String
    val newGroupSubtitle: String
    // Reorganized Settings & Complete App Details
    val settingsAppearanceSection: String
    val settingsNotificationsSection: String
    val settingsSecuritySection: String
    val settingsAppDetailsSection: String
    val notificationActiveStatus: String
    val notificationTapToAllow: String
    val appDetailsTitle: String
    val appNameLabel: String
    val appVersionLabel: String
    val appDeveloperLabel: String
    val appPackageLabel: String
    val appFeaturesTitle: String
    val appFullDescription: String
    val viewFullAppDetails: String
    val privacyPolicyBody: String
    val termsBody: String
    val helpSupportBody: String
    // WhatsApp-style offline presence & message delivery status
    val peerOfflineBannerText: String
    val messageStatusLabel: String
    val messageStatusSentOffline: String
    val messageStatusDelivered: String
    val messageStatusRead: String
    // v1.5 Chat & Messaging: View Once, Polls, Shared Media, Export Chat
    val viewOncePhotoAction: String
    val viewOncePhotoLabel: String
    val viewOnceOpenedLabel: String
    val viewOnceViewerHint: String
    val viewOncePreviewSubtitle: String
    val createPollAction: String
    val createPollTitle: String
    val pollQuestionHint: String
    val pollOptionHintPrefix: String
    val pollAddOption: String
    val pollSendAction: String
    val pollPreviewPrefix: String
    val pollVotesSuffix: String
    val pollTapToVote: String
    val sharedMediaAction: String
    val sharedMediaTitle: String
    val sharedMediaPhotosTab: String
    val sharedMediaDocsTab: String
    val sharedMediaLinksTab: String
    val sharedMediaEmpty: String
    val exportChatAction: String
    val exportChatEmpty: String
    val exportChatChooserTitle: String
    // v1.5 Calling: Busy status & Minimized floating call bar
    val callBusyStatus: String
    val callBusySubtitle: String
    val minimizeCall: String
    val returnToCallHint: String
    // v1.5 Privacy & Smart Connect: Biometric, Chat Lock, QR Code, Group Admin Controls
    val biometricLockSetting: String
    val biometricLockDesc: String
    val biometricUnavailable: String
    val unlockWithBiometric: String
    val biometricPromptTitle: String
    val biometricPromptSubtitle: String
    val lockChatAction: String
    val unlockChatAction: String
    val lockedChatPreview: String
    val chatLockedToast: String
    val chatUnlockedToast: String
    val unlockChatPromptTitle: String
    val setPinOrBiometricFirst: String
    val myQrCodeAction: String
    val scanQrCodeAction: String
    val qrSheetTitle: String
    val qrMyCodeSubtitle: String
    val qrShareMyLink: String
    val qrScannerTitle: String
    val qrScannerHint: String
    val qrCameraPermissionRequired: String
    val qrInvalidCode: String
    val groupOnlyAdminsSendSetting: String
    val groupOnlyAdminsSendDesc: String
    val groupOnlyAdminsBanner: String
    val groupMakeAdmin: String
    val groupDismissAdmin: String
    val groupRemoveMember: String
    val groupMemberUpdatedToast: String
    // v1.5 Auto-Update Checker
    val checkForUpdatesAction: String
    val checkingForUpdates: String
    val updateAvailableTitle: String
    val updateAvailableDesc: String
    val updateDownloadNow: String
    val updateLater: String
    val appUpToDateToast: String
    // v1.6 Admin Moderation & Telegram Control
    val adminPanelTitle: String
    val adminPanelSubtitle: String
    val adminUsersTab: String
    val adminReportsTab: String
    val adminSearchHint: String
    val adminStatusActive: String
    val adminStatusBanned: String
    val adminStatusMuted: String
    val adminStatusDeleted: String
    val adminBanAction: String
    val adminUnbanAction: String
    val adminMuteAction: String
    val adminMute1h: String
    val adminMute24h: String
    val adminMute7d: String
    val adminDeleteUserAction: String
    val adminDismissReportAction: String
    val adminReasonHint: String
    val adminConfirmDeleteTitle: String
    val adminConfirmDeleteDesc: String
    val adminActionSuccessToast: String
    val adminNoReportsEmpty: String
    val adminCopyUidToast: String
    val accountSuspendedTitle: String
    val accountDeletedByAdminTitle: String
    val accountSuspendedDesc: String
    val moderationReasonLabel: String
    val mutedBannerTitle: String
    val mutedBannerRemainingPrefix: String
    // v1.7 Broadcast Banner, Verified Badge & Auto Profanity/Spam Filter
    val verifiedBadgeDesc: String
    val adminVerifyAction: String
    val adminUnverifyAction: String
    val adminVerifiedBadgeLabel: String
    val adminBroadcastFilterTab: String
    val adminBroadcastSectionTitle: String
    val adminBroadcastSectionDesc: String
    val adminBroadcastTitleHint: String
    val adminBroadcastMessageHint: String
    val adminBroadcastSeverityInfo: String
    val adminBroadcastSeverityUpdate: String
    val adminBroadcastSeverityWarning: String
    val adminBroadcastPublishAction: String
    val adminBroadcastClearAction: String
    val adminBroadcastActiveBadge: String
    val adminBroadcastInactiveBadge: String
    val adminBroadcastSavedToast: String
    val adminAutoFilterSectionTitle: String
    val adminAutoFilterSectionDesc: String
    val adminProfanityToggleTitle: String
    val adminProfanityToggleDesc: String
    val adminProfanityModeBlock: String
    val adminProfanityModeMask: String
    val adminSpamToggleTitle: String
    val adminSpamToggleDesc: String
    val adminTelegramAlertToggleTitle: String
    val adminTelegramAlertToggleDesc: String
    val adminCustomWordsLabel: String
    val adminCustomWordsHint: String
    val adminAddWordAction: String
    val adminSaveFilterAction: String
    val adminFilterSavedToast: String
    val adminFilterTesterLabel: String
    val adminFilterTesterHint: String
    val adminFilterTesterClean: String
    val adminFilterTesterFlagged: String
    val defaultBroadcastTitle: String
}

object BanglaStrings : AppStrings {
    override val appTitle = "Bangla Chat"
    override val taglineLatin = "Connect • Chat • Share"
    override val taglineHeadline = "সবার সাথে থাকুন\nসবসময়, সবখানে"
    override val continueWithGoogle = "Google দিয়ে লগইন"
    override val signingIn = "লগইন হচ্ছে..."
    override val secureAuthNotice = "নিরাপদ Firebase Authentication ও Cloud Firestore দ্বারা পরিচালিত"
    override val tabChats = "চ্যাট"
    override val tabContacts = "কন্টাক্ট"
    override val tabCalls = "কল"
    override val tabSettings = "সেটিংস"
    override val onlineBannerTitle = "আপনি এখন অনলাইনে আছেন"
    override val onlineBannerSubtitle = "শুভ চ্যাটিং করুন!"
    override val offlineBanner = "ইন্টারনেট সংযোগ বিচ্ছিন্ন — অফলাইন ক্যাশ চালু আছে"
    override val searchChatsHint = "চ্যাট বা নাম খুঁজুন..."
    override val searchUsersHint = "ইউজারনেম বা ফোন নম্বর দিয়ে খুঁজুন..."
    override val newChat = "নতুন চ্যাট"
    override val noChatsTitle = "এখনও কোনো কথোপকথন নেই"
    override val noChatsSubtitle = "কন্টাক্ট ট্যাব থেকে নিবন্ধিত বন্ধুদের খুঁজুন এবং রিয়েল-টাইম চ্যাট শুরু করুন।"
    override val startFirstChat = "বন্ধু খুঁজুন"
    override val loadingText = "লোড হচ্ছে..."
    override val retry = "আবার চেষ্টা করুন"
    override val photoMessagePreview = "ছবি পাঠানো হয়েছে"
    override val documentMessagePreview = "ডকুমেন্ট পাঠানো হয়েছে"
    override val messageDeletedPreview = "এই মেসেজটি মুছে ফেলা হয়েছে"
    override val typingIndicator = "টাইপ করছেন..."
    override val onlineStatus = "অনলাইন"
    override val offlineStatus = "অফলাইন"
    override val lastSeenPrefix = "সর্বশেষ দেখা"
    override val typeMessageHint = "মেসেজ লিখুন..."
    override val send = "পাঠান"
    override val loadOlderMessages = "পুরনো মেসেজ লোড করুন"
    override val reply = "রিপ্লাই"
    override val replyingTo = "রিপ্লাই দিচ্ছেন"
    override val copy = "কপি করুন"
    override val copiedToClipboard = "মেসেজ কপি করা হয়েছে"
    override val forward = "ফরওয়ার্ড"
    override val forwardedLabel = "ফরওয়ার্ড করা হয়েছে"
    override val deleteMessage = "মুছে ফেলুন"
    override val react = "রিঅ্যাক্ট"
    override val editMessage = "এডিট করুন"
    override val editingMessage = "মেসেজ এডিট করছেন"
    override val editedLabel = "সম্পাদিত"
    override val starMessage = "স্টার করুন"
    override val unstarMessage = "স্টার সরান"
    override val starredMessagesTitle = "স্টার করা মেসেজ"
    override val noStarredMessages = "এই চ্যাটে কোনো স্টার করা মেসেজ নেই।"
    override val pinMessage = "মেসেজ পিন করুন"
    override val unpinMessage = "পিন সরান"
    override val pinnedMessageLabel = "পিন করা মেসেজ"
    override val searchInChat = "চ্যাটে খুঁজুন"
    override val searchInChatHint = "মেসেজ খুঁজুন..."
    override val matchesFound = "টি মিল পাওয়া গেছে"
    override val disappearingMessages = "অটো-ডিলিট মেসেজ"
    override val disappearingOff = "বন্ধ"
    override val disappearing24h = "২৪ ঘণ্টা"
    override val disappearing7d = "৭ দিন"
    override val disappearingBanner24h = "অটো-ডিলিট চালু (২৪ ঘণ্টা)"
    override val disappearingBanner7d = "অটো-ডিলিট চালু (৭ দিন)"
    override val pinChat = "চ্যাট পিন করুন"
    override val unpinChat = "পিন সরান"
    override val muteChat = "নোটিফিকেশন মিউট"
    override val unmuteChat = "আনমিউট করুন"
    override val archiveChat = "আর্কাইভ করুন"
    override val unarchiveChat = "আর্কাইভ থেকে সরান"
    override val archivedChatsTitle = "আর্কাইভ করা চ্যাট"
    override val chatWallpaperSetting = "চ্যাট ওয়ালপেপার"
    override val wallpaperDefault = "ডিফল্ট"
    override val wallpaperMint = "মিন্ট"
    override val wallpaperOcean = "ওশান"
    override val wallpaperViolet = "ভায়োলেট"
    override val wallpaperPeach = "পীচ"
    override val wallpaperMidnight = "মিডনাইট"
    override val chatFontSizeSetting = "চ্যাট ফন্ট সাইজ"
    override val fontSmall = "ছোট"
    override val fontMedium = "মাঝারি"
    override val fontLarge = "বড়"
    override val showProfilePhotoSetting = "প্রোফাইল ছবি দেখান"
    override val showAboutSetting = "আমার সম্পর্কে (Bio) দেখান"
    override val screenSecuritySetting = "স্ক্রিনশট ও রেকর্ডিং ব্লক"
    override val screenSecurityDesc = "চ্যাট স্ক্রিনশট ও রিসেন্ট অ্যাপ প্রিভিউ সুরক্ষিত রাখুন"
    override val appLockSetting = "অ্যাপ লক (৪-ডিজিট পিন)"
    override val appLockDesc = "অ্যাপ খোলার সময় ৪-ডিজিটের গোপন পিন চাইবে"
    override val appLockTitle = "Bangla Chat লক করা আছে"
    override val appLockSubtitle = "প্রবেশ করতে আপনার ৪-ডিজিট পিন দিন"
    override val appLockWrongPin = "পিন সঠিক নয়, আবার চেষ্টা করুন"
    override val appLockSetPinTitle = "৪-ডিজিট অ্যাপ লক পিন সেট করুন"
    override val appLockEnterPinHint = "নতুন ৪-ডিজিট পিন"
    override val appLockConfirmPinHint = "পুনরায় ৪-ডিজিট পিন দিন"
    override val appLockPinMismatch = "দুইবারের পিন মেলেনি বা ৪ ডিজিট নয়"
    override val appLockEnabledToast = "অ্যাপ লক চালু করা হয়েছে"
    override val appLockDisabledToast = "অ্যাপ লক বন্ধ করা হয়েছে"
    override val attachMediaTitle = "মিডিয়া ও ফাইল পাঠান"
    override val pickFromGallery = "গ্যালারি থেকে ছবি নির্বাচন"
    override val takeCameraPhoto = "ক্যামেরা দিয়ে ছবি তুলুন"
    override val shareDocumentPdf = "ডকুমেন্ট বা PDF পাঠান"
    override val uploadingMedia = "আপলোড হচ্ছে..."
    override val uploadFailedRetry = "আপলোড ব্যর্থ হয়েছে"
    override val blockedChatBanner = "ব্লক করার কারণে এই কথোপকথনে মেসেজ পাঠানো বন্ধ রয়েছে।"
    override val viewProfile = "প্রোফাইল দেখুন"
    override val favoritesTitle = "প্রিয় কন্টাক্ট"
    override val registeredUsersTitle = "নিবন্ধিত ব্যবহারকারী"
    override val noUsersFound = "কোনো ব্যবহারকারী পাওয়া যায়নি"
    override val startChatAction = "মেসেজ পাঠান"
    override val addFavorite = "প্রিয় তালিকায় যোগ করুন"
    override val removeFavorite = "প্রিয় তালিকা থেকে সরান"
    override val blockUser = "ব্লক করুন"
    override val unblockUser = "আনব্লক করুন"
    override val reportUser = "রিপোর্ট করুন"
    override val reportTitle = "অ্যাকাউন্ট রিপোর্ট করুন"
    override val reportReasonSpam = "স্প্যাম বা অবাঞ্ছিত বার্তা"
    override val reportReasonHarassment = "হয়রানি বা অশালীন আচরণ"
    override val reportReasonFake = "ভুয়া অ্যাকাউন্ট"
    override val reportReasonOther = "অন্যান্য"
    override val reportDetailsHint = "অতিরিক্ত তথ্য লিখুন (ঐচ্ছিক)..."
    override val submitReport = "রিপোর্ট জমা দিন"
    override val reportSubmitted = "আপনার রিপোর্ট জমা দেওয়া হয়েছে"
    override val callsUnavailableTitle = "ভয়েস ও ভিডিও কলিং বর্তমানে উপলব্ধ নয়"
    override val callsUnavailableDesc = "লাইভ WebRTC ভয়েস/ভিডিও কলিং সার্ভার এখনও সংযুক্ত করা হয়নি। নিচে আপনার সংরক্ষিত কল ইতিহাস দেখা যাবে।"
    override val callsFilterAll = "সব"
    override val callsFilterMissed = "মিসড"
    override val callsFilterIncoming = "ইনকামিং"
    override val callsFilterOutgoing = "আউটগোয়িং"
    override val noCallHistoryTitle = "কোনো কল ইতিহাস নেই"
    override val noCallHistorySubtitle = "ভয়েস বা ভিডিও কল ইতিহাস এখানে প্রদর্শিত হবে।"
    override val settingsAccountSection = "অ্যাকাউন্ট ও প্রোফাইল"
    override val settingsPreferencesSection = "ভাষা, থিম ও চ্যাট সাজসজ্জা"
    override val settingsPrivacySection = "নোটিফিকেশন, গোপনীয়তা ও নিরাপত্তা"
    override val settingsSupportSection = "তথ্য ও সহায়তা"
    override val editProfile = "প্রোফাইল এডিট করুন"
    override val displayNameLabel = "প্রদর্শিত নাম"
    override val usernameLabel = "ইউজারনেম (a-z, 0-9, _)"
    override val phoneLabel = "ফোন নম্বর (+880...)"
    override val emailLabel = "ইমেইল"
    override val aboutLabel = "আমার সম্পর্কে (Bio)"
    override val saveChanges = "সংরক্ষণ করুন"
    override val changePhoto = "ছবি পরিবর্তন"
    override val languageSetting = "ভাষা"
    override val languageBangla = "বাংলা"
    override val languageEnglish = "English"
    override val themeSetting = "থিম মোড"
    override val themeSystem = "সিস্টেম"
    override val themeLight = "লাইট মোড"
    override val themeDark = "ডার্ক মোড"
    override val notificationSettings = "নোটিফিকেশন সেটিংস"
    override val enableNotifications = "পুশ নোটিফিকেশন অনুমতি"
    override val messagePreviewSetting = "মেসেজ প্রিভিউ দেখান"
    override val messagePreviewDesc = "নোটিফিকেশনে মেসেজের মূল লেখা প্রদর্শন করুন"
    override val privacySettings = "গোপনীয়তা সেটিংস"
    override val discoverableSetting = "সার্চে আমাকে খুঁজতে দিন"
    override val discoverableDesc = "অন্য ব্যবহারকারীরা ইউজারনেম বা ফোন দিয়ে আপনাকে খুঁজতে পারবে"
    override val showOnlineSetting = "অনলাইন স্ট্যাটাস দেখান"
    override val showLastSeenSetting = "সর্বশেষ দেখার সময় দেখান"
    override val showReadReceiptsSetting = "রিড রিসিপ্ট (নীল টিক) চালু রাখুন"
    override val blockedUsersSetting = "ব্লক করা ব্যবহারকারী"
    override val noBlockedUsers = "আপনি কাউকে ব্লক করেননি"
    override val aboutApp = "অ্যাপ সম্পর্কে"
    override val privacyPolicy = "গোপনীয়তা নীতি"
    override val termsAndConditions = "শর্তাবলী"
    override val helpAndSupport = "সাহায্য ও সাপোর্ট"
    override val logout = "লগআউট"
    override val logoutConfirmTitle = "লগআউট করতে চান?"
    override val logoutConfirmMessage = "আপনি এই ডিভাইস থেকে Bangla Chat অ্যাকাউন্ট থেকে লগআউট হয়ে যাবেন।"
    override val deleteAccount = "অ্যাকাউন্ট মুছে ফেলুন"
    override val deleteAccountTitle = "অ্যাকাউন্ট স্থায়ীভাবে মুছবেন?"
    override val deleteAccountMessage = "এতে আপনার প্রোফাইল ও অ্যাকাউন্ট তথ্য স্থায়ীভাবে মুছে যাবে। নিরাপত্তার জন্য পুনরায় Google যাচাইকরণ লাগতে পারে।"
    override val cancel = "বাতিল"
    override val confirm = "নিশ্চিত করুন"
    override val close = "বন্ধ করুন"
    override val forwardToTitle = "কোন চ্যাটে ফরওয়ার্ড করবেন?"
    override val noChatsToForward = "ফরওয়ার্ড করার মতো অন্য কোনো চ্যাট নেই।"
    override val accountCreatedLabel = "যোগদানের তারিখ"
    override val openAttachment = "খুলুন"
    override val downloadAttachment = "ডাউনলোড"
    override val profileUpdated = "প্রোফাইল সফলভাবে আপডেট হয়েছে"
    override val chatStarted = "চ্যাট খোলা হয়েছে"
    override val voiceMessagePreview = "ভয়েস মেসেজ"
    override val recordVoiceNote = "ভয়েস মেসেজ রেকর্ড করুন"
    override val recordingVoice = "রেকর্ডিং চলছে..."
    override val micPermissionRequired = "ভয়েস রেকর্ড করতে মাইক্রোফোন অনুমতি দিন"
    override val newGroup = "নতুন গ্রুপ"
    override val groupNameHint = "গ্রুপের নাম লিখুন..."
    override val selectGroupMembers = "সদস্য নির্বাচন করুন"
    override val createGroupAction = "গ্রুপ তৈরি করুন"
    override val groupMembersSuffix = "জন সদস্য"
    override val groupInfoTitle = "গ্রুপের তথ্য"
    override val groupAdminBadge = "এডমিন"
    override val renameGroup = "গ্রুপের নাম পরিবর্তন"
    override val addMembers = "সদস্য যোগ করুন"
    override val leaveGroup = "গ্রুপ ত্যাগ করুন"
    override val groupCreatedToast = "নতুন গ্রুপ তৈরি হয়েছে"
    override val myStatus = "আমার স্ট্যাটাস"
    override val addStatus = "স্ট্যাটাস দিন"
    override val textStatusTab = "টেক্সট স্ট্যাটাস"
    override val photoStatusTab = "ছবি স্ট্যাটাস"
    override val statusTextHint = "আপনার মনের কথা লিখুন (২৪ ঘণ্টা থাকবে)..."
    override val postStatusAction = "পোস্ট করুন"
    override val statusPostedToast = "স্ট্যাটাস পোস্ট করা হয়েছে"
    override val statusViewersLabel = "জন দেখেছে"
    override val deleteStatus = "স্ট্যাটাস মুছুন"
    override val aiAssistantTitle = "Bangla AI • DeepSeek"
    override val aiAssistantSubtitle = "যেকোনো প্রশ্ন, কোডিং, লেখা বা অনুবাদ — স্মৃতিসহ উত্তর"
    override val aiAssistantBannerBadge = "ফ্রি AI"
    override val aiAssistantMemoryBadge = "স্মৃতি চালু • DeepSeek"
    override val aiAssistantNewChat = "নতুন চ্যাট"
    override val aiAssistantNewChatToast = "নতুন AI কথোপকথন শুরু হয়েছে"
    override val aiAssistantInputHint = "DeepSeek AI-কে যেকোনো কিছু জিজ্ঞেস করুন..."
    override val aiAssistantWelcomeTitle = "আসসালামু আলাইকুম! আমি আপনার DeepSeek AI"
    override val aiAssistantWelcomeSubtitle = "বাংলা বা ইংরেজিতে যেকোনো প্রশ্ন করুন — আমি আগের কথোপকথন মনে রেখে উত্তর দিতে পারি।"
    override val aiQuickPrompt1 = "বাংলায় একটি সুন্দর স্ট্যাটাস লিখে দাও"
    override val aiQuickPrompt2 = "সহজ ভাষায় ইংরেজি শেখার ৫টি টিপস দাও"
    override val aiQuickPrompt3 = "একটি Python কোড লিখে দাও"
    override val aiQuickPrompt4 = "অফিসের জন্য ছুটির আবেদন লিখে দাও"
    override val aiTranslateAction = "AI অনুবাদ (বাংলা ↔ English)"
    override val aiTranslatedLabel = "AI অনুবাদ"
    override val aiExplainAction = "AI দিয়ে ব্যাখ্যা করুন"
    override val aiExplainTitle = "AI মেসেজ ব্যাখ্যা"
    override val aiExplainOriginalLabel = "মূল মেসেজ"
    override val aiQuickRepliesAction = "স্মার্ট রিপ্লাই"
    override val aiWriterAction = "AI লিখুন"
    override val aiWriterTitle = "AI মেসেজ রাইটার (DeepSeek)"
    override val aiWriterTabRewrite = "সাজিয়ে লিখুন"
    override val aiWriterTabCompose = "নতুন করে লিখুন"
    override val aiWriterRewriteHint = "মেসেজ বক্সে আগে কিছু লিখুন অথবা এখানে ড্রাফট লিখুন..."
    override val aiWriterIdeaHint = "কী নিয়ে মেসেজ লিখতে চান? (যেমন: জন্মদিনের শুভেচ্ছা)..."
    override val aiWriterGenerateAction = "AI দিয়ে তৈরি করুন"
    override val aiWriterInsertAction = "মেসেজ বক্সে বসান"
    override val aiWriterSendAction = "সরাসরি পাঠান"
    override val aiTonePolite = "ভদ্র ও সুন্দর"
    override val aiToneCasual = "বন্ধুসুলভ"
    override val aiToneProfessional = "প্রফেশনাল"
    override val aiToneBanglaPolish = "শুদ্ধ বাংলা"
    override val aiToneEnglish = "ইংরেজিতে"
    override val aiIdeaPreset1 = "বন্ধুকে জন্মদিনের সুন্দর শুভেচ্ছা"
    override val aiIdeaPreset2 = "দেরি হওয়ার জন্য আন্তরিকভাবে দুঃখ প্রকাশ"
    override val aiIdeaPreset3 = "সালাম দিয়ে কুশল বিনিময়"
    override val aiIdeaPreset4 = "ধন্যবাদ ও কৃতজ্ঞতা প্রকাশ"
    override val aiSummarizeChatAction = "চ্যাট সারসংক্ষেপ (AI)"
    override val aiSummaryTitle = "AI চ্যাট সারসংক্ষেপ"
    override val aiGenerating = "DeepSeek AI চিন্তা করছে..."
    override val aiPoweredByDeepSeek = "DeepSeek AI দ্বারা চালিত"
    override val voiceCallAction = "অডিও কল"
    override val videoCallAction = "ভিডিও কল"
    override val liveCallsBannerTitle = "লাইভ অডিও ও ভিডিও কল চালু আছে"
    override val liveCallsBannerDesc = "যেকোনো বন্ধুকে সরাসরি HD অডিও বা ভিডিও কল করুন (WebRTC)।"
    override val quickCallFriends = "দ্রুত কল করুন"
    override val incomingVoiceCall = "ইনকামিং অডিও কল..."
    override val incomingVideoCall = "ইনকামিং ভিডিও কল..."
    override val callingStatus = "কল হচ্ছে..."
    override val ringingStatus = "রিং হচ্ছে..."
    override val callPeerOfflineNotice = "অফলাইনে আছেন — অনলাইনে এলে রিং যাবে"
    override val callNoAnswerTimeout = "কোনো উত্তর পাওয়া যায়নি"
    override val connectingStatus = "সংযোগ হচ্ছে..."
    override val callEndedStatus = "কল শেষ হয়েছে"
    override val acceptCall = "রিসিভ"
    override val declineCall = "কাটুন"
    override val endCall = "কল কাটুন"
    override val muteMic = "মিউট"
    override val speakerAudio = "স্পিকার"
    override val toggleCamera = "ক্যামেরা"
    override val flipCamera = "ক্যামেরা ঘোরান"
    override val callPermissionsRequired = "কল করার জন্য মাইক্রোফোন ও ক্যামেরা অনুমতি প্রয়োজন"
    override val loginAction = "লগইন করুন"
    override val registerAction = "নতুন রেজিস্ট্রেশন"
    override val loginHintNotice = "আগে অ্যাকাউন্ট করা থাকলে সরাসরি ওই অ্যাকাউন্টেই লগইন হবে"
    override val registerHintNotice = "নতুন ব্যবহারকারী হলে নাম, ফোন নম্বর ও ইউনিক @username দিয়ে অ্যাকাউন্ট খুলুন"
    override val checkingAccount = "অ্যাকাউন্ট যাচাই হচ্ছে..."
    override val noAccountFoundRegisterPrompt = "এই Google অ্যাকাউন্টে আগে কোনো রেজিস্ট্রেশন পাওয়া যায়নি। নতুন করে অ্যাকাউন্ট তৈরি করতে নিচের তথ্য পূরণ করুন।"
    override val completeRegistrationTitle = "রেজিস্ট্রেশন সম্পন্ন করুন"
    override val completeRegistrationSubtitle = "আপনার নাম, ফোন নম্বর এবং একটি ইউনিক ইউজারনেম দিন (কারো সাথে মিলবে না)"
    override val usernamePlaceholder = "ridoy1234"
    override val usernameFormatHint = "কমপক্ষে ৩ অক্ষর (ছোট হাতের a-z, 0-9, _)"
    override val usernameChecking = "ইউজারনেম যাচাই হচ্ছে..."
    override val usernameAvailable = "এই ইউজারনেমটি নেওয়া যাবে!"
    override val usernameTaken = "এই ইউজারনেমটি আগেই অন্য কেউ নিয়েছে — অন্য একটি দিন"
    override val usernameInvalid = "সঠিক ইউজারনেম দিন (কমপক্ষে ৩ অক্ষর: a-z, 0-9, _)"
    override val phoneInvalid = "সঠিক ফোন নম্বর দিন (যেমন +8801XXXXXXXXX)"
    override val nameInvalid = "আপনার নাম লিখুন"
    override val createAccountButton = "রেজিস্ট্রেশন সম্পন্ন করুন"
    override val creatingAccount = "অ্যাকাউন্ট তৈরি হচ্ছে..."
    override val switchGoogleAccount = "অন্য Google অ্যাকাউন্ট দিন"
    override val searchExactUsernameHint = "@ridoy1234 এর মতো ইউজারনেম লিখুন..."
    override val searchButton = "খুঁজুন"
    override val exactUsernameGuideTitle = "ইউজারনেম দিয়ে আইডি খুঁজুন"
    override val exactUsernameGuideSubtitle = "যেমন @ridoy1234 লিখে খুঁজুন। এই ইউজারনেমে কারো অ্যাকাউন্ট থাকলে দেখাবে, না থাকলে দেখাবে না।"
    override val exactUsernameNotFound = "এই ইউজারনেমে কোনো আইডি পাওয়া যায়নি"
    override val exactUsernameMatchTitle = "পাওয়া গেছে"
    override val chatFilterAll = "সব"
    override val chatFilterUnread = "অপঠিত"
    override val chatFilterGroups = "গ্রুপ"
    override val chatFilterFavorites = "প্রিয়"
    override val noUnreadChatsTitle = "সব মেসেজ দেখা হয়েছে!"
    override val noUnreadChatsSubtitle = "আপনার কোনো অপঠিত মেসেজ বাকি নেই।"
    override val noGroupChatsTitle = "এখনও কোনো গ্রুপ নেই"
    override val noGroupChatsSubtitle = "নতুন গ্রুপ তৈরি করে বন্ধুদের সাথে একসাথে আড্ডা শুরু করুন।"
    override val noFavoriteChatsTitle = "কোনো প্রিয় চ্যাট নেই"
    override val noFavoriteChatsSubtitle = "বন্ধুদের প্রোফাইলে স্টার (★) মার্ক করলে তাদের চ্যাট এখানে দেখাবে।"
    override val showAllChats = "সব চ্যাট দেখুন"
    override val taglineLineOne = "সবার সাথে থাকুন"
    override val taglineLineTwo = "সবসময়, সবখানে"
    override val loginWithPhone = "ফোন দিয়ে লগইন"
    override val loginWithEmail = "ইমেইল দিয়ে লগইন"
    override val loginWithGoogle = "Google দিয়ে লগইন"
    override val orDivider = "অথবা"
    override val createNewAccount = "নতুন অ্যাকাউন্ট তৈরি করুন"
    override val phoneLoginSheetTitle = "ফোন দিয়ে লগইন"
    override val phoneLoginSheetSubtitle = "আপনার মোবাইল নম্বর দিন। SMS-এ ৬ ডিজিটের ভেরিফিকেশন কোড পাঠানো হবে।"
    override val sendOtpAction = "OTP কোড পাঠান"
    override val sendingOtp = "কোড পাঠানো হচ্ছে..."
    override val enterOtpTitle = "ভেরিফিকেশন কোড দিন"
    override val enterOtpSubtitle = "আপনার নম্বরে পাঠানো ৬ ডিজিটের OTP কোড লিখুন"
    override val otpCodeLabel = "৬ ডিজিটের OTP কোড"
    override val verifyAndLogin = "ভেরিফাই ও লগইন করুন"
    override val verifyingOtp = "যাচাই হচ্ছে..."
    override val resendOtp = "কোড আবার পাঠান"
    override val changePhoneNumber = "নম্বর পরিবর্তন করুন"
    override val continueWithGoogleUsingPhone = "এই নম্বর দিয়ে Google-এ লগইন করুন"
    override val phoneOtpUnavailableNote = "এই প্রজেক্টে Firebase Console-এ Phone SMS OTP এখনও চালু না থাকায় কোড পাঠানো যায়নি। নিচে ক্লিক করে এই নম্বরসহ সরাসরি Google দিয়ে নিরাপদে লগইন করুন।"
    override val otpInvalid = "সঠিক ৬ ডিজিটের OTP কোড লিখুন"
    override val emailLoginSheetTitle = "ইমেইল দিয়ে লগইন"
    override val emailLoginSheetSubtitle = "আপনার ইমেইল লিখুন এবং নিরাপদ Google অ্যাকাউন্ট যাচাইয়ের মাধ্যমে পাসওয়ার্ড ছাড়াই সরাসরি লগইন করুন।"
    override val emailInputHint = "example@gmail.com"
    override val emailInvalid = "সঠিক ইমেইল ঠিকানা লিখুন"
    override val continueWithEmailAction = "ইমেইল যাচাই করে লগইন করুন"
    override val createAccountSheetTitle = "নতুন অ্যাকাউন্ট তৈরি করুন"
    override val createAccountSheetSubtitle = "পছন্দের মাধ্যম বেছে নিন। এরপর আপনার নাম, ফোন নম্বর ও ইউনিক @username দিয়ে অ্যাকাউন্ট সম্পন্ন হবে।"
    override val registerWithGoogle = "Google দিয়ে নতুন অ্যাকাউন্ট"
    override val registerWithPhone = "ফোন নম্বর (OTP) দিয়ে নতুন অ্যাকাউন্ট"
    override val deleteForMe = "আমার জন্য মুছুন"
    override val deleteForEveryone = "সবার জন্য মুছুন"
    override val deletedForMeToast = "মেসেজটি শুধু আপনার চ্যাট থেকে মুছে ফেলা হয়েছে"
    override val callAudioLabel = "অডিও কল"
    override val callVideoLabel = "ভিডিও কল"
    override val callOutgoingAudio = "আউটগোয়িং অডিও কল"
    override val callOutgoingVideo = "আউটগোয়িং ভিডিও কল"
    override val callIncomingAudio = "ইনকামিং অডিও কল"
    override val callIncomingVideo = "ইনকামিং ভিডিও কল"
    override val callMissedAudio = "মিসড অডিও কল"
    override val callMissedVideo = "মিসড ভিডিও কল"
    override val callDeclinedLabel = "কল বাতিল হয়েছে"
    override val callNoAnswerLabel = "উত্তর পাওয়া যায়নি"
    override val callTalkedSuffix = "কথা হয়েছে"
    override val callTapToCallBack = "আবার কল করুন"
    override val newActionSheetTitle = "নতুন কথোপকথন শুরু করুন"
    override val newChatSubtitle = "ইউজারনেম খুঁজে ব্যক্তিগত চ্যাট শুরু করুন"
    override val newGroupSubtitle = "একাধিক বন্ধু নিয়ে একসাথে গ্রুপ আড্ডা খুলুন"
    override val settingsAppearanceSection = "থিম, ভাষা ও চ্যাট সাজসজ্জা"
    override val settingsNotificationsSection = "নোটিফিকেশন ও সতর্কতা"
    override val settingsSecuritySection = "গোপনীয়তা ও নিরাপত্তা"
    override val settingsAppDetailsSection = "অ্যাপের তথ্য ও সহায়তা"
    override val notificationActiveStatus = "চালু আছে"
    override val notificationTapToAllow = "অনুমতি দিতে ট্যাপ করুন"
    override val appDetailsTitle = "অ্যাপের সম্পূর্ণ তথ্য"
    override val appNameLabel = "অ্যাপের নাম"
    override val appVersionLabel = "ভার্সন"
    override val appDeveloperLabel = "ডেভেলপার"
    override val appPackageLabel = "প্যাকেজ আইডি"
    override val appFeaturesTitle = "অ্যাপের মূল ফিচারসমূহ"
    override val appFullDescription = "Bangla Chat একটি আধুনিক, দ্রুত ও নিরাপদ বাংলাদেশি মেসেজিং অ্যাপ — যেখানে রিয়েল-টাইম ব্যক্তিগত ও গ্রুপ চ্যাট, HD অডিও/ভিডিও কল, ভয়েস নোট, ছবি ও ফাইল শেয়ারিং, ২৪ ঘণ্টার স্ট্যাটাস স্টোরি এবং স্মার্ট DeepSeek AI সহায়তা একসাথে পাওয়া যায়।"
    override val viewFullAppDetails = "সম্পূর্ণ ডিটেইলস দেখুন"
    override val privacyPolicyBody = "Bangla Chat আপনার ব্যক্তিগত ইমেইল, ফোন নম্বর ও নোটিফিকেশন টোকেন একটি সুরক্ষিত owner-only Firestore ডকুমেন্টে আলাদা রাখে যা জিরো-ট্রাস্ট সিকিউরিটি রুলস দিয়ে সুরক্ষিত। পাবলিক প্রোফাইলে শুধু সেই তথ্যই দেখা যায় যা আপনি অনুমতি দিয়েছেন। সব মেসেজ TLS-এর মাধ্যমে Firebase Cloud Firestore-এ সুরক্ষিতভাবে আদান-প্রদান হয়।"
    override val termsBody = "Bangla Chat সম্মানজনকভাবে ব্যবহার করুন। হয়রানি, ভুয়া পরিচয় বা স্প্যামিং সম্পূর্ণ নিষিদ্ধ। প্রয়োজনে যেকোনো চ্যাট বা প্রোফাইল থেকে সরাসরি ব্লক বা রিপোর্ট করতে পারবেন।"
    override val helpSupportBody = "• বন্ধু খুঁজতে কন্টাক্ট ট্যাবে সঠিক @username লিখে সার্চ করুন।\n• নতুন চ্যাট বা নতুন গ্রুপ তৈরি করতে হোম স্ক্রিনের নিচের কলম (✏️) আইকনে ট্যাপ করুন।\n• অনলাইন স্ট্যাটাস, লাস্ট সিন, রিড রিসিপ্ট ও অ্যাপ লক পিন 'গোপনীয়তা ও নিরাপত্তা' সেকশন থেকে নিয়ন্ত্রণ করুন।\n• যেকোনো মেসেজ লং-প্রেস করে 'আমার জন্য মুছুন' দিয়ে শুধু নিজের চ্যাট থেকে সরাতে পারবেন।"
    override val peerOfflineBannerText = "অফলাইনে আছেন — মেসেজ পাঠানো থাকবে, অনলাইনে এলে পৌঁছে যাবে"
    override val messageStatusLabel = "মেসেজ স্ট্যাটাস"
    override val messageStatusSentOffline = "পাঠানো হয়েছে (✓) — এখনো ফোনে পৌঁছায়নি (অফলাইন)"
    override val messageStatusDelivered = "পৌঁছেছে (✓✓) — ফোনে ডেলিভার্ড হয়েছে (অনলাইন)"
    override val messageStatusRead = "দেখা হয়েছে (✓✓ নীল) — মেসেজটি পড়েছে"
    override val viewOncePhotoAction = "১ বার দেখার ছবি (View Once)"
    override val viewOncePhotoLabel = "১ বার দেখার ছবি"
    override val viewOnceOpenedLabel = "খোলা হয়েছে"
    override val viewOnceViewerHint = "এই ছবিটি শুধুমাত্র একবার দেখা যাবে • স্ক্রিনশট সুরক্ষিত"
    override val viewOncePreviewSubtitle = "ট্যাপ করে দেখুন"
    override val createPollAction = "পোল / ভোটিং তৈরি করুন"
    override val createPollTitle = "নতুন পোল তৈরি করুন"
    override val pollQuestionHint = "প্রশ্ন লিখুন (যেমন: আজ কোথায় দেখা হবে?)..."
    override val pollOptionHintPrefix = "অপশন"
    override val pollAddOption = "আরও অপশন যোগ করুন"
    override val pollSendAction = "পোল পাঠান"
    override val pollPreviewPrefix = "পোল:"
    override val pollVotesSuffix = "ভোট"
    override val pollTapToVote = "ভোট দিতে যেকোনো অপশনে ট্যাপ করুন"
    override val sharedMediaAction = "শেয়ার্ড মিডিয়া ও ফাইল"
    override val sharedMediaTitle = "শেয়ার্ড মিডিয়া, ফাইল ও লিংক"
    override val sharedMediaPhotosTab = "ছবি"
    override val sharedMediaDocsTab = "ডকুমেন্ট"
    override val sharedMediaLinksTab = "লিংক"
    override val sharedMediaEmpty = "এখানে কোনো আইটেম পাওয়া যায়নি"
    override val exportChatAction = "চ্যাট এক্সপোর্ট (.txt)"
    override val exportChatEmpty = "এক্সপোর্ট করার মতো কোনো মেসেজ নেই"
    override val exportChatChooserTitle = "চ্যাট ব্যাকআপ শেয়ার বা সেভ করুন"
    override val callBusyStatus = "অন্য কলে ব্যস্ত আছেন"
    override val callBusySubtitle = "এই মুহূর্তে তিনি অন্য একটি কলে কথা বলছেন"
    override val minimizeCall = "ছোট করুন"
    override val returnToCallHint = "কলে ফিরে যেতে ট্যাপ করুন"
    override val biometricLockSetting = "ফিঙ্গারপ্রিন্ট / ফেস আনলক"
    override val biometricLockDesc = "পিনের পাশাপাশি বায়োমেট্রিক দিয়ে দ্রুত আনলক করুন"
    override val biometricUnavailable = "এই ফোনে ফিঙ্গারপ্রিন্ট বা ফেস আনলক সেটআপ করা নেই"
    override val unlockWithBiometric = "ফিঙ্গারপ্রিন্ট দিয়ে আনলক করুন"
    override val biometricPromptTitle = "Bangla Chat আনলক"
    override val biometricPromptSubtitle = "আপনার ফিঙ্গারপ্রিন্ট বা ফেস দিয়ে পরিচয় নিশ্চিত করুন"
    override val lockChatAction = "চ্যাট লক করুন"
    override val unlockChatAction = "চ্যাট আনলক করুন"
    override val lockedChatPreview = "লক করা চ্যাট • আনলক করে দেখুন"
    override val chatLockedToast = "এই চ্যাটটি লক করা হয়েছে"
    override val chatUnlockedToast = "চ্যাটটি আনলক করা হয়েছে"
    override val unlockChatPromptTitle = "লক করা চ্যাট খুলুন"
    override val setPinOrBiometricFirst = "চ্যাট লক ব্যবহার করতে সেটিংস থেকে অ্যাপ লক পিন বা বায়োমেট্রিক চালু করুন"
    override val myQrCodeAction = "আমার QR কোড"
    override val scanQrCodeAction = "QR স্ক্যান করুন"
    override val qrSheetTitle = "QR কোড দিয়ে দ্রুত যুক্ত হন"
    override val qrMyCodeSubtitle = "বন্ধুদের এই QR কোড স্ক্যান করতে বলুন — সরাসরি আপনার @username খুঁজে পাবে"
    override val qrShareMyLink = "আমার প্রোফাইল লিংক শেয়ার"
    override val qrScannerTitle = "বন্ধুর QR কোড স্ক্যান করুন"
    override val qrScannerHint = "ক্যামেরার ফ্রেমের ভেতর Bangla Chat QR কোড ধরুন"
    override val qrCameraPermissionRequired = "QR কোড স্ক্যান করতে ক্যামেরার অনুমতি প্রয়োজন"
    override val qrInvalidCode = "এটি সঠিক Bangla Chat QR কোড নয়"
    override val groupOnlyAdminsSendSetting = "শুধু অ্যাডমিন মেসেজ পাঠাতে পারবে"
    override val groupOnlyAdminsSendDesc = "চালু করলে সাধারণ সদস্যরা শুধু মেসেজ পড়তে পারবে"
    override val groupOnlyAdminsBanner = "এই গ্রুপে শুধুমাত্র অ্যাডমিনরা মেসেজ পাঠাতে পারবেন"
    override val groupMakeAdmin = "অ্যাডমিন বানান"
    override val groupDismissAdmin = "অ্যাডমিন থেকে সরান"
    override val groupRemoveMember = "গ্রুপ থেকে রিমুভ করুন"
    override val groupMemberUpdatedToast = "গ্রুপ সদস্য আপডেট হয়েছে"
    override val checkForUpdatesAction = "নতুন আপডেট চেক করুন"
    override val checkingForUpdates = "আপডেট যাচাই হচ্ছে..."
    override val updateAvailableTitle = "নতুন আপডেট পাওয়া গেছে!"
    override val updateAvailableDesc = "Bangla Chat-এর নতুন ভার্সন এখন ডাউনলোডের জন্য প্রস্তুত।"
    override val updateDownloadNow = "এখনই ডাউনলোড করুন (APK)"
    override val updateLater = "পরে"
    override val appUpToDateToast = "আপনি সর্বশেষ ভার্সন ব্যবহার করছেন!"
    override val adminPanelTitle = "অ্যাডমিন মডারেশন প্যানেল"
    override val adminPanelSubtitle = "ইউজার ব্যান, মিউট, ডিলিট ও রিপোর্ট নিয়ন্ত্রণ (Telegram Sync)"
    override val adminUsersTab = "সকল ইউজার"
    override val adminReportsTab = "রিপোর্টসমূহ"
    override val adminSearchHint = "নাম, @ইউজারনেম, ফোন বা UID খুঁজুন..."
    override val adminStatusActive = "সক্রিয়"
    override val adminStatusBanned = "ব্যান করা"
    override val adminStatusMuted = "মিউট করা"
    override val adminStatusDeleted = "রিমুভ করা"
    override val adminBanAction = "স্থায়ী ব্যান"
    override val adminUnbanAction = "আনব্যান / আনমিউট"
    override val adminMuteAction = "মিউট করুন"
    override val adminMute1h = "১ ঘণ্টা"
    override val adminMute24h = "২৪ ঘণ্টা"
    override val adminMute7d = "৭ দিন"
    override val adminDeleteUserAction = "রিমুভ / ডিলিট"
    override val adminDismissReportAction = "রিপোর্ট সমাধান"
    override val adminReasonHint = "কারণ লিখুন (ঐচ্ছিক, যেমন: স্প্যাম / খারাপ আচরণ)..."
    override val adminConfirmDeleteTitle = "ইউজার স্থায়ীভাবে রিমুভ করবেন?"
    override val adminConfirmDeleteDesc = "ইউজারের প্রোফাইল ও ইউজারনেম মুছে যাবে এবং অ্যাকাউন্ট স্থায়ীভাবে ব্লক হয়ে যাবে।"
    override val adminActionSuccessToast = "মডারেশন অ্যাকশন সম্পন্ন হয়েছে"
    override val adminNoReportsEmpty = "বর্তমানে কোনো পেন্ডিং রিপোর্ট নেই"
    override val adminCopyUidToast = "UID কপি করা হয়েছে"
    override val accountSuspendedTitle = "আপনার অ্যাকাউন্ট স্থগিত করা হয়েছে"
    override val accountDeletedByAdminTitle = "আপনার অ্যাকাউন্ট অ্যাডমিন দ্বারা রিমুভ করা হয়েছে"
    override val accountSuspendedDesc = "অ্যাপের নীতিমালা লঙ্ঘনের কারণে আপনার অ্যাকাউন্টটি ব্লক বা বন্ধ করে দেওয়া হয়েছে। আপনি এই অ্যাকাউন্ট থেকে আর মেসেজ বা কল করতে পারবেন না।"
    override val moderationReasonLabel = "কারণ"
    override val mutedBannerTitle = "অ্যাডমিন আপনাকে সাময়িকভাবে মেসেজ পাঠানো থেকে বিরত রেখেছেন"
    override val mutedBannerRemainingPrefix = "বাকি সময়"
    override val verifiedBadgeDesc = "ভেরিফায়েড অ্যাকাউন্ট"
    override val adminVerifyAction = "ভেরিফাই (✓)"
    override val adminUnverifyAction = "ব্যাজ সরান"
    override val adminVerifiedBadgeLabel = "ভেরিফায়েড"
    override val adminBroadcastFilterTab = "নোটিশ ও ফিল্টার"
    override val adminBroadcastSectionTitle = "লাইভ ব্রডকাস্ট নোটিশ ব্যানার"
    override val adminBroadcastSectionDesc = "সব ইউজারের হোম স্ক্রিনে তাৎক্ষণিক গুরুত্বপূর্ণ নোটিশ বা আপডেট ব্যানার দেখান"
    override val adminBroadcastTitleHint = "নোটিশের শিরোনাম (যেমন: জরুরি আপডেট)"
    override val adminBroadcastMessageHint = "সব ইউজারের জন্য আপনার বার্তা লিখুন..."
    override val adminBroadcastSeverityInfo = "সাধারণ"
    override val adminBroadcastSeverityUpdate = "আপডেট"
    override val adminBroadcastSeverityWarning = "জরুরি সতর্কতা"
    override val adminBroadcastPublishAction = "নোটিশ প্রকাশ করুন"
    override val adminBroadcastClearAction = "নোটিশ বন্ধ করুন"
    override val adminBroadcastActiveBadge = "লাইভ আছে"
    override val adminBroadcastInactiveBadge = "বন্ধ আছে"
    override val adminBroadcastSavedToast = "ব্রডকাস্ট নোটিশ আপডেট হয়েছে"
    override val adminAutoFilterSectionTitle = "অটো ব্যাড-ওয়ার্ড ও স্প্যাম ফিল্টার"
    override val adminAutoFilterSectionDesc = "বাংলা ও ইংরেজি অশ্লীল শব্দ এবং স্প্যাম মেসেজ স্বয়ংক্রিয়ভাবে নিয়ন্ত্রণ করুন"
    override val adminProfanityToggleTitle = "অটো ব্যাড-ওয়ার্ড ফিল্টার"
    override val adminProfanityToggleDesc = "অশ্লীল বা নিষিদ্ধ শব্দ শনাক্ত করলে ব্লক বা স্টার (***) করবে"
    override val adminProfanityModeBlock = "মেসেজ ব্লক করুন"
    override val adminProfanityModeMask = "স্টার (***) করুন"
    override val adminSpamToggleTitle = "অ্যান্টি-স্প্যাম ফ্লাড প্রোটেকশন"
    override val adminSpamToggleDesc = "খুব দ্রুত বা একই মেসেজ বারবার পাঠানো স্বয়ংক্রিয়ভাবে আটকাবে"
    override val adminTelegramAlertToggleTitle = "টেলিগ্রাম অটো-অ্যালার্ট"
    override val adminTelegramAlertToggleDesc = "বাজে শব্দ বা স্প্যাম ধরা পড়লে আপনার টেলিগ্রাম বটে সতর্কবার্তা পাঠাবে"
    override val adminCustomWordsLabel = "কাস্টম নিষিদ্ধ শব্দ তালিকা"
    override val adminCustomWordsHint = "নতুন শব্দ লিখুন (বাংলা বা ইংরেজি)..."
    override val adminAddWordAction = "যোগ করুন"
    override val adminSaveFilterAction = "ফিল্টার সেটিংস সেভ করুন"
    override val adminFilterSavedToast = "অটো-ফিল্টার সেটিংস সংরক্ষণ হয়েছে"
    override val adminFilterTesterLabel = "লাইভ ফিল্টার টেস্ট"
    override val adminFilterTesterHint = "যেকোনো বাক্য লিখে টেস্ট করুন..."
    override val adminFilterTesterClean = "নিরাপদ — কোনো নিষিদ্ধ শব্দ পাওয়া যায়নি"
    override val adminFilterTesterFlagged = "শনাক্ত হয়েছে"
    override val defaultBroadcastTitle = "অ্যাডমিন নোটিশ"
}

object EnglishStrings : AppStrings {
    override val appTitle = "Bangla Chat"
    override val taglineLatin = "Connect • Chat • Share"
    override val taglineHeadline = "Stay connected with everyone\nAnytime, anywhere"
    override val continueWithGoogle = "Continue with Google"
    override val signingIn = "Signing in..."
    override val secureAuthNotice = "Powered by Firebase Authentication & Cloud Firestore"
    override val tabChats = "Chats"
    override val tabContacts = "Contacts"
    override val tabCalls = "Calls"
    override val tabSettings = "Settings"
    override val onlineBannerTitle = "You are online now"
    override val onlineBannerSubtitle = "Happy chatting!"
    override val offlineBanner = "No internet connection — offline cache active"
    override val searchChatsHint = "Search chats or people..."
    override val searchUsersHint = "Search by username or phone..."
    override val newChat = "New Chat"
    override val noChatsTitle = "No conversations yet"
    override val noChatsSubtitle = "Find registered people in the Contacts tab and start a real-time chat."
    override val startFirstChat = "Find Contacts"
    override val loadingText = "Loading..."
    override val retry = "Retry"
    override val photoMessagePreview = "Photo shared"
    override val documentMessagePreview = "Document shared"
    override val messageDeletedPreview = "This message was deleted"
    override val typingIndicator = "Typing..."
    override val onlineStatus = "Online"
    override val offlineStatus = "Offline"
    override val lastSeenPrefix = "Last seen"
    override val typeMessageHint = "Write a message..."
    override val send = "Send"
    override val loadOlderMessages = "Load older messages"
    override val reply = "Reply"
    override val replyingTo = "Replying to"
    override val copy = "Copy"
    override val copiedToClipboard = "Message copied"
    override val forward = "Forward"
    override val forwardedLabel = "Forwarded"
    override val deleteMessage = "Delete"
    override val react = "React"
    override val editMessage = "Edit"
    override val editingMessage = "Editing message"
    override val editedLabel = "Edited"
    override val starMessage = "Star Message"
    override val unstarMessage = "Unstar Message"
    override val starredMessagesTitle = "Starred Messages"
    override val noStarredMessages = "No starred messages in this conversation."
    override val pinMessage = "Pin Message"
    override val unpinMessage = "Unpin"
    override val pinnedMessageLabel = "Pinned message"
    override val searchInChat = "Search in Chat"
    override val searchInChatHint = "Search messages..."
    override val matchesFound = "matches found"
    override val disappearingMessages = "Disappearing Messages"
    override val disappearingOff = "Off"
    override val disappearing24h = "24 Hours"
    override val disappearing7d = "7 Days"
    override val disappearingBanner24h = "Disappearing messages: 24h"
    override val disappearingBanner7d = "Disappearing messages: 7d"
    override val pinChat = "Pin Chat"
    override val unpinChat = "Unpin Chat"
    override val muteChat = "Mute Notifications"
    override val unmuteChat = "Unmute"
    override val archiveChat = "Archive Chat"
    override val unarchiveChat = "Unarchive Chat"
    override val archivedChatsTitle = "Archived Chats"
    override val chatWallpaperSetting = "Chat Wallpaper"
    override val wallpaperDefault = "Default"
    override val wallpaperMint = "Mint"
    override val wallpaperOcean = "Ocean"
    override val wallpaperViolet = "Violet"
    override val wallpaperPeach = "Peach"
    override val wallpaperMidnight = "Midnight"
    override val chatFontSizeSetting = "Chat Font Size"
    override val fontSmall = "Small"
    override val fontMedium = "Medium"
    override val fontLarge = "Large"
    override val showProfilePhotoSetting = "Show Profile Photo"
    override val showAboutSetting = "Show About / Bio"
    override val screenSecuritySetting = "Block Screenshots"
    override val screenSecurityDesc = "Prevent screenshots and hide content in Recent Apps"
    override val appLockSetting = "App Lock (4-Digit PIN)"
    override val appLockDesc = "Require a 4-digit PIN when opening Bangla Chat"
    override val appLockTitle = "Bangla Chat Locked"
    override val appLockSubtitle = "Enter your 4-digit PIN to unlock"
    override val appLockWrongPin = "Incorrect PIN, please try again"
    override val appLockSetPinTitle = "Set 4-Digit App Lock PIN"
    override val appLockEnterPinHint = "New 4-digit PIN"
    override val appLockConfirmPinHint = "Confirm 4-digit PIN"
    override val appLockPinMismatch = "PINs must be 4 digits and match"
    override val appLockEnabledToast = "App Lock enabled"
    override val appLockDisabledToast = "App Lock disabled"
    override val attachMediaTitle = "Share Media & Files"
    override val pickFromGallery = "Choose Photo from Gallery"
    override val takeCameraPhoto = "Take Photo with Camera"
    override val shareDocumentPdf = "Share Document or PDF"
    override val uploadingMedia = "Uploading..."
    override val uploadFailedRetry = "Upload failed"
    override val blockedChatBanner = "Messaging is disabled because one participant has blocked the other."
    override val viewProfile = "View Profile"
    override val favoritesTitle = "Favorites"
    override val registeredUsersTitle = "Registered Users"
    override val noUsersFound = "No matching users found"
    override val startChatAction = "Message"
    override val addFavorite = "Add to Favorites"
    override val removeFavorite = "Remove Favorite"
    override val blockUser = "Block User"
    override val unblockUser = "Unblock"
    override val reportUser = "Report Account"
    override val reportTitle = "Report Account"
    override val reportReasonSpam = "Spam or unwanted messages"
    override val reportReasonHarassment = "Harassment or abuse"
    override val reportReasonFake = "Fake or impersonation account"
    override val reportReasonOther = "Other"
    override val reportDetailsHint = "Additional details (optional)..."
    override val submitReport = "Submit Report"
    override val reportSubmitted = "Report submitted"
    override val callsUnavailableTitle = "Voice & Video Calling Unavailable"
    override val callsUnavailableDesc = "Live WebRTC voice/video calling is not connected yet. Any saved call history records appear below."
    override val callsFilterAll = "All"
    override val callsFilterMissed = "Missed"
    override val callsFilterIncoming = "Incoming"
    override val callsFilterOutgoing = "Outgoing"
    override val noCallHistoryTitle = "No call history"
    override val noCallHistorySubtitle = "Call log records will appear here."
    override val settingsAccountSection = "Account & Profile"
    override val settingsPreferencesSection = "Language, Theme & Chat Appearance"
    override val settingsPrivacySection = "Notifications, Privacy & Security"
    override val settingsSupportSection = "About & Support"
    override val editProfile = "Edit Profile"
    override val displayNameLabel = "Display Name"
    override val usernameLabel = "Username (a-z, 0-9, _)"
    override val phoneLabel = "Phone Number (+880...)"
    override val emailLabel = "Email"
    override val aboutLabel = "About / Bio"
    override val saveChanges = "Save Changes"
    override val changePhoto = "Change Photo"
    override val languageSetting = "Language"
    override val languageBangla = "বাংলা"
    override val languageEnglish = "English"
    override val themeSetting = "Theme Mode"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val notificationSettings = "Notification Settings"
    override val enableNotifications = "Push Notification Permission"
    override val messagePreviewSetting = "Show Message Previews"
    override val messagePreviewDesc = "Display message text inside incoming notifications"
    override val privacySettings = "Privacy Settings"
    override val discoverableSetting = "Allow Search Discovery"
    override val discoverableDesc = "Let other users find you by username or phone"
    override val showOnlineSetting = "Show Online Status"
    override val showLastSeenSetting = "Show Last Seen"
    override val showReadReceiptsSetting = "Read Receipts"
    override val blockedUsersSetting = "Blocked Users"
    override val noBlockedUsers = "You haven't blocked anyone"
    override val aboutApp = "About Bangla Chat"
    override val privacyPolicy = "Privacy Policy"
    override val termsAndConditions = "Terms & Conditions"
    override val helpAndSupport = "Help & Support"
    override val logout = "Sign Out"
    override val logoutConfirmTitle = "Sign out of Bangla Chat?"
    override val logoutConfirmMessage = "You will be signed out of your account on this device."
    override val deleteAccount = "Delete Account"
    override val deleteAccountTitle = "Permanently delete account?"
    override val deleteAccountMessage = "This removes your profile and account records permanently. Recent Google sign-in verification may be required."
    override val cancel = "Cancel"
    override val confirm = "Confirm"
    override val close = "Close"
    override val forwardToTitle = "Forward message to..."
    override val noChatsToForward = "No other conversations available to forward to."
    override val accountCreatedLabel = "Joined"
    override val openAttachment = "Open"
    override val downloadAttachment = "Download"
    override val profileUpdated = "Profile updated successfully"
    override val chatStarted = "Conversation opened"
    override val voiceMessagePreview = "Voice message"
    override val recordVoiceNote = "Record voice note"
    override val recordingVoice = "Recording..."
    override val micPermissionRequired = "Microphone permission is needed for voice notes"
    override val newGroup = "New Group"
    override val groupNameHint = "Enter group name..."
    override val selectGroupMembers = "Select members"
    override val createGroupAction = "Create Group"
    override val groupMembersSuffix = "members"
    override val groupInfoTitle = "Group Info"
    override val groupAdminBadge = "Admin"
    override val renameGroup = "Rename Group"
    override val addMembers = "Add Members"
    override val leaveGroup = "Leave Group"
    override val groupCreatedToast = "Group created"
    override val myStatus = "My Status"
    override val addStatus = "Add Status"
    override val textStatusTab = "Text Status"
    override val photoStatusTab = "Photo Status"
    override val statusTextHint = "Share what's on your mind (visible for 24h)..."
    override val postStatusAction = "Post Status"
    override val statusPostedToast = "Status posted"
    override val statusViewersLabel = "views"
    override val deleteStatus = "Delete Status"
    override val aiAssistantTitle = "Bangla AI • DeepSeek"
    override val aiAssistantSubtitle = "Ask anything, code, write, or translate with memory"
    override val aiAssistantBannerBadge = "FREE AI"
    override val aiAssistantMemoryBadge = "Memory Active • DeepSeek"
    override val aiAssistantNewChat = "New Chat"
    override val aiAssistantNewChatToast = "Started a fresh AI conversation"
    override val aiAssistantInputHint = "Ask DeepSeek AI anything..."
    override val aiAssistantWelcomeTitle = "Hello! I'm your DeepSeek AI Assistant"
    override val aiAssistantWelcomeSubtitle = "Ask questions in Bangla or English, get coding help, or draft messages — I remember our chat context."
    override val aiQuickPrompt1 = "Write a thoughtful Bangla status caption"
    override val aiQuickPrompt2 = "Give me 5 quick tips to speak better English"
    override val aiQuickPrompt3 = "Write a clean Python helper script"
    override val aiQuickPrompt4 = "Draft a polite leave request message"
    override val aiTranslateAction = "AI Translate (Bangla ↔ English)"
    override val aiTranslatedLabel = "AI Translation"
    override val aiExplainAction = "Explain with AI"
    override val aiExplainTitle = "AI Message Explanation"
    override val aiExplainOriginalLabel = "Original message"
    override val aiQuickRepliesAction = "Smart Replies"
    override val aiWriterAction = "AI Write"
    override val aiWriterTitle = "AI Message Writer (DeepSeek)"
    override val aiWriterTabRewrite = "Polish Draft"
    override val aiWriterTabCompose = "Write from Idea"
    override val aiWriterRewriteHint = "Type your draft message here to polish or translate..."
    override val aiWriterIdeaHint = "What do you want to say? (e.g., Birthday wish)..."
    override val aiWriterGenerateAction = "Generate with AI"
    override val aiWriterInsertAction = "Insert in Box"
    override val aiWriterSendAction = "Send Now"
    override val aiTonePolite = "Polite"
    override val aiToneCasual = "Friendly"
    override val aiToneProfessional = "Professional"
    override val aiToneBanglaPolish = "Clean Bangla"
    override val aiToneEnglish = "In English"
    override val aiIdeaPreset1 = "Warm birthday wish for a friend"
    override val aiIdeaPreset2 = "Polite apology for being late"
    override val aiIdeaPreset3 = "Friendly greeting to check in"
    override val aiIdeaPreset4 = "Heartfelt thank you message"
    override val aiSummarizeChatAction = "AI Chat Summary"
    override val aiSummaryTitle = "AI Chat Summary"
    override val aiGenerating = "DeepSeek AI is thinking..."
    override val aiPoweredByDeepSeek = "Powered by DeepSeek AI"
    override val voiceCallAction = "Voice Call"
    override val videoCallAction = "Video Call"
    override val liveCallsBannerTitle = "Live HD Audio & Video Calling Ready"
    override val liveCallsBannerDesc = "Call any friend directly with real-time WebRTC audio and video."
    override val quickCallFriends = "Quick Call"
    override val incomingVoiceCall = "Incoming voice call..."
    override val incomingVideoCall = "Incoming video call..."
    override val callingStatus = "Calling..."
    override val ringingStatus = "Ringing..."
    override val callPeerOfflineNotice = "User is offline — will ring when back online"
    override val callNoAnswerTimeout = "No answer"
    override val connectingStatus = "Connecting..."
    override val callEndedStatus = "Call ended"
    override val acceptCall = "Accept"
    override val declineCall = "Decline"
    override val endCall = "End Call"
    override val muteMic = "Mute"
    override val speakerAudio = "Speaker"
    override val toggleCamera = "Camera"
    override val flipCamera = "Flip"
    override val callPermissionsRequired = "Microphone and Camera permissions are required to place a call"
    override val loginAction = "Login"
    override val registerAction = "Register New Account"
    override val loginHintNotice = "Existing users sign straight back into their account without creating a duplicate"
    override val registerHintNotice = "New users register with Name, Phone Number, and a globally unique @username"
    override val checkingAccount = "Checking account..."
    override val noAccountFoundRegisterPrompt = "No registered account was found for this Google account. Please complete registration below."
    override val completeRegistrationTitle = "Complete Registration"
    override val completeRegistrationSubtitle = "Enter your name, phone number, and a unique @username"
    override val usernamePlaceholder = "ridoy1234"
    override val usernameFormatHint = "3–30 chars (lowercase a-z, 0-9, _)"
    override val usernameChecking = "Checking availability..."
    override val usernameAvailable = "This @username is available!"
    override val usernameTaken = "This @username is already taken — try another"
    override val usernameInvalid = "Enter a valid username (3–30 chars: a-z, 0-9, _)"
    override val phoneInvalid = "Enter a valid phone number (e.g. +8801XXXXXXXXX)"
    override val nameInvalid = "Please enter your display name"
    override val createAccountButton = "Complete Registration"
    override val creatingAccount = "Creating account..."
    override val switchGoogleAccount = "Switch Google Account"
    override val searchExactUsernameHint = "Enter exact @username (e.g. @ridoy1234)..."
    override val searchButton = "Search"
    override val exactUsernameGuideTitle = "Find Friends by @username"
    override val exactUsernameGuideSubtitle = "Search by exact handle like @ridoy1234. Matching accounts appear immediately; unknown usernames stay hidden."
    override val exactUsernameNotFound = "No account found with this @username"
    override val exactUsernameMatchTitle = "Match Found"
    override val chatFilterAll = "All"
    override val chatFilterUnread = "Unread"
    override val chatFilterGroups = "Groups"
    override val chatFilterFavorites = "Favorites"
    override val noUnreadChatsTitle = "All caught up!"
    override val noUnreadChatsSubtitle = "You have no unread messages waiting."
    override val noGroupChatsTitle = "No groups yet"
    override val noGroupChatsSubtitle = "Create a group to chat with friends together."
    override val noFavoriteChatsTitle = "No favorite chats"
    override val noFavoriteChatsSubtitle = "Star friends in Contacts to quickly filter their chats here."
    override val showAllChats = "Show All Chats"
    override val taglineLineOne = "Stay connected with everyone"
    override val taglineLineTwo = "Anytime, anywhere"
    override val loginWithPhone = "Login with Phone"
    override val loginWithEmail = "Login with Email"
    override val loginWithGoogle = "Login with Google"
    override val orDivider = "Or"
    override val createNewAccount = "Create New Account"
    override val phoneLoginSheetTitle = "Login with Phone"
    override val phoneLoginSheetSubtitle = "Enter your mobile number to receive a 6-digit SMS verification code."
    override val sendOtpAction = "Send OTP Code"
    override val sendingOtp = "Sending code..."
    override val enterOtpTitle = "Enter Verification Code"
    override val enterOtpSubtitle = "Enter the 6-digit OTP code sent to your number"
    override val otpCodeLabel = "6-digit OTP code"
    override val verifyAndLogin = "Verify & Login"
    override val verifyingOtp = "Verifying..."
    override val resendOtp = "Resend Code"
    override val changePhoneNumber = "Change Number"
    override val continueWithGoogleUsingPhone = "Continue with Google using this number"
    override val phoneOtpUnavailableNote = "Phone SMS OTP is not enabled in Firebase Console yet or unavailable on this device. Tap below to sign in instantly with Google using this number."
    override val otpInvalid = "Please enter a valid 6-digit OTP code"
    override val emailLoginSheetTitle = "Login with Email"
    override val emailLoginSheetSubtitle = "Enter your email address and sign in securely without a password via verified Google account authentication."
    override val emailInputHint = "example@gmail.com"
    override val emailInvalid = "Please enter a valid email address"
    override val continueWithEmailAction = "Verify Email & Login"
    override val createAccountSheetTitle = "Create New Account"
    override val createAccountSheetSubtitle = "Choose your sign-up method, then complete your Name, Phone Number, and unique @username."
    override val registerWithGoogle = "Sign up with Google"
    override val registerWithPhone = "Sign up with Phone (OTP)"
    override val deleteForMe = "Delete for me"
    override val deleteForEveryone = "Delete for everyone"
    override val deletedForMeToast = "Message deleted for you"
    override val callAudioLabel = "Voice call"
    override val callVideoLabel = "Video call"
    override val callOutgoingAudio = "Outgoing voice call"
    override val callOutgoingVideo = "Outgoing video call"
    override val callIncomingAudio = "Incoming voice call"
    override val callIncomingVideo = "Incoming video call"
    override val callMissedAudio = "Missed voice call"
    override val callMissedVideo = "Missed video call"
    override val callDeclinedLabel = "Call declined"
    override val callNoAnswerLabel = "No answer"
    override val callTalkedSuffix = "talk time"
    override val callTapToCallBack = "Tap to call back"
    override val newActionSheetTitle = "Start Something New"
    override val newChatSubtitle = "Find someone by @username to start a private chat"
    override val newGroupSubtitle = "Create a group conversation with multiple friends"
    override val settingsAppearanceSection = "Appearance & Chat"
    override val settingsNotificationsSection = "Notifications & Alerts"
    override val settingsSecuritySection = "Privacy & Security"
    override val settingsAppDetailsSection = "App Details & Support"
    override val notificationActiveStatus = "Active"
    override val notificationTapToAllow = "Tap to allow"
    override val appDetailsTitle = "Complete App Details"
    override val appNameLabel = "App Name"
    override val appVersionLabel = "Version"
    override val appDeveloperLabel = "Developer"
    override val appPackageLabel = "Package ID"
    override val appFeaturesTitle = "Key Features"
    override val appFullDescription = "Bangla Chat is a fast, modern, and secure messaging app featuring real-time private and group chats, HD WebRTC audio & video calls, voice notes, photo & document sharing, 24-hour status stories, and built-in DeepSeek AI assistance."
    override val viewFullAppDetails = "View Full Details"
    override val privacyPolicyBody = "Bangla Chat isolates your private email, phone number, and notification tokens in an owner-only Firestore document protected by zero-trust Security Rules. Public discovery only exposes fields you explicitly choose to share. Messages are transmitted securely over TLS via Firebase Cloud Firestore."
    override val termsBody = "Use Bangla Chat respectfully. Harassment, impersonation, and spam are strictly prohibited. You can block or report abusive accounts directly from any profile or conversation."
    override val helpSupportBody = "• Find friends in the Contacts tab by searching their exact @username.\n• Tap the pencil (✏️) button on the Chats screen to start a New Chat or create a New Group.\n• Manage Online Status, Last Seen, Read Receipts, App Lock PIN, and Screenshot Protection under Privacy & Security.\n• Long-press any message and tap 'Delete for me' to remove it only from your view."
    override val peerOfflineBannerText = "Currently offline — messages will be delivered once online"
    override val messageStatusLabel = "Message Status"
    override val messageStatusSentOffline = "Sent (✓) — not delivered yet (offline)"
    override val messageStatusDelivered = "Delivered (✓✓) — reached recipient's phone"
    override val messageStatusRead = "Read (✓✓ blue) — seen by recipient"
    override val viewOncePhotoAction = "View Once Photo"
    override val viewOncePhotoLabel = "View Once Photo"
    override val viewOnceOpenedLabel = "Opened"
    override val viewOnceViewerHint = "Can only be viewed once • Screenshot protected"
    override val viewOncePreviewSubtitle = "Tap to open"
    override val createPollAction = "Create Poll"
    override val createPollTitle = "Create a Poll"
    override val pollQuestionHint = "Ask a question (e.g., Where should we meet?)..."
    override val pollOptionHintPrefix = "Option"
    override val pollAddOption = "Add option"
    override val pollSendAction = "Send Poll"
    override val pollPreviewPrefix = "Poll:"
    override val pollVotesSuffix = "votes"
    override val pollTapToVote = "Tap an option to vote"
    override val sharedMediaAction = "Shared Media & Files"
    override val sharedMediaTitle = "Shared Media, Docs & Links"
    override val sharedMediaPhotosTab = "Photos"
    override val sharedMediaDocsTab = "Docs"
    override val sharedMediaLinksTab = "Links"
    override val sharedMediaEmpty = "Nothing shared here yet"
    override val exportChatAction = "Export Chat (.txt)"
    override val exportChatEmpty = "No messages to export"
    override val exportChatChooserTitle = "Share or save chat transcript"
    override val callBusyStatus = "Busy on another call"
    override val callBusySubtitle = "They are currently speaking on another call"
    override val minimizeCall = "Minimize"
    override val returnToCallHint = "Tap to return to call"
    override val biometricLockSetting = "Fingerprint / Face Unlock"
    override val biometricLockDesc = "Unlock Bangla Chat & locked chats quickly using biometrics"
    override val biometricUnavailable = "Biometric unlock is not set up on this device"
    override val unlockWithBiometric = "Unlock with Biometrics"
    override val biometricPromptTitle = "Unlock Bangla Chat"
    override val biometricPromptSubtitle = "Verify with your fingerprint or face"
    override val lockChatAction = "Lock Chat"
    override val unlockChatAction = "Unlock Chat"
    override val lockedChatPreview = "Locked chat • Unlock to view"
    override val chatLockedToast = "Chat locked"
    override val chatUnlockedToast = "Chat unlocked"
    override val unlockChatPromptTitle = "Unlock Chat"
    override val setPinOrBiometricFirst = "Enable App Lock PIN or Biometric Unlock in Settings first"
    override val myQrCodeAction = "My QR Code"
    override val scanQrCodeAction = "Scan QR"
    override val qrSheetTitle = "Connect Instantly via QR"
    override val qrMyCodeSubtitle = "Let friends scan this code to find your @username instantly"
    override val qrShareMyLink = "Share My Handle"
    override val qrScannerTitle = "Scan Friend's QR Code"
    override val qrScannerHint = "Align a Bangla Chat QR code inside the frame"
    override val qrCameraPermissionRequired = "Camera permission is required to scan QR codes"
    override val qrInvalidCode = "Not a valid Bangla Chat QR code"
    override val groupOnlyAdminsSendSetting = "Only admins can send messages"
    override val groupOnlyAdminsSendDesc = "When enabled, regular members can only read messages"
    override val groupOnlyAdminsBanner = "Only group admins can send messages in this group"
    override val groupMakeAdmin = "Make Group Admin"
    override val groupDismissAdmin = "Dismiss as Admin"
    override val groupRemoveMember = "Remove from Group"
    override val groupMemberUpdatedToast = "Group membership updated"
    override val checkForUpdatesAction = "Check for Updates"
    override val checkingForUpdates = "Checking for updates..."
    override val updateAvailableTitle = "New Update Available!"
    override val updateAvailableDesc = "A newer version of Bangla Chat is ready to download."
    override val updateDownloadNow = "Download Now (APK)"
    override val updateLater = "Later"
    override val appUpToDateToast = "You are on the latest version!"
    override val adminPanelTitle = "Admin Moderation Panel"
    override val adminPanelSubtitle = "Ban, mute, delete users & review reports (Telegram Sync)"
    override val adminUsersTab = "All Users"
    override val adminReportsTab = "Reports"
    override val adminSearchHint = "Search name, @username, phone, or UID..."
    override val adminStatusActive = "Active"
    override val adminStatusBanned = "Banned"
    override val adminStatusMuted = "Muted"
    override val adminStatusDeleted = "Removed"
    override val adminBanAction = "Permanent Ban"
    override val adminUnbanAction = "Unban / Unmute"
    override val adminMuteAction = "Mute User"
    override val adminMute1h = "1 Hour"
    override val adminMute24h = "24 Hours"
    override val adminMute7d = "7 Days"
    override val adminDeleteUserAction = "Remove / Delete"
    override val adminDismissReportAction = "Dismiss Report"
    override val adminReasonHint = "Reason (optional, e.g., spam or abuse)..."
    override val adminConfirmDeleteTitle = "Permanently remove this user?"
    override val adminConfirmDeleteDesc = "This deletes their profile and username claim and permanently suspends their account."
    override val adminActionSuccessToast = "Moderation action applied"
    override val adminNoReportsEmpty = "No pending user reports right now"
    override val adminCopyUidToast = "UID copied to clipboard"
    override val accountSuspendedTitle = "Account Suspended"
    override val accountDeletedByAdminTitle = "Account Removed by Admin"
    override val accountSuspendedDesc = "Your account has been permanently suspended due to a policy violation. You can no longer send messages or place calls from this account."
    override val moderationReasonLabel = "Reason"
    override val mutedBannerTitle = "You are temporarily muted from sending messages"
    override val mutedBannerRemainingPrefix = "Time remaining"
    override val verifiedBadgeDesc = "Verified Account"
    override val adminVerifyAction = "Verify (✓)"
    override val adminUnverifyAction = "Unverify"
    override val adminVerifiedBadgeLabel = "Verified"
    override val adminBroadcastFilterTab = "Notice & Filter"
    override val adminBroadcastSectionTitle = "Live Broadcast Notice Banner"
    override val adminBroadcastSectionDesc = "Display an instant announcement or update banner on every user's Home screen"
    override val adminBroadcastTitleHint = "Notice title (e.g. Important Update)"
    override val adminBroadcastMessageHint = "Write your announcement for all users..."
    override val adminBroadcastSeverityInfo = "Info"
    override val adminBroadcastSeverityUpdate = "Update"
    override val adminBroadcastSeverityWarning = "Warning"
    override val adminBroadcastPublishAction = "Publish Notice"
    override val adminBroadcastClearAction = "Turn Off Notice"
    override val adminBroadcastActiveBadge = "LIVE"
    override val adminBroadcastInactiveBadge = "OFF"
    override val adminBroadcastSavedToast = "Broadcast notice updated"
    override val adminAutoFilterSectionTitle = "Auto Profanity & Anti-Spam Filter"
    override val adminAutoFilterSectionDesc = "Automatically guard chats against Bangla/English abusive words and spam floods"
    override val adminProfanityToggleTitle = "Auto Bad-Word Filter"
    override val adminProfanityToggleDesc = "Blocks or masks (***) abusive and custom restricted words"
    override val adminProfanityModeBlock = "Block Message"
    override val adminProfanityModeMask = "Mask with ***"
    override val adminSpamToggleTitle = "Anti-Spam Flood Protection"
    override val adminSpamToggleDesc = "Blocks rapid burst flooding and repeated identical spam messages"
    override val adminTelegramAlertToggleTitle = "Telegram Auto-Alerts"
    override val adminTelegramAlertToggleDesc = "Sends instant alert with Mute/Ban buttons to your Telegram bot on violations"
    override val adminCustomWordsLabel = "Custom Blocked Words"
    override val adminCustomWordsHint = "Add word or phrase (Bangla / English)..."
    override val adminAddWordAction = "Add"
    override val adminSaveFilterAction = "Save Filter Settings"
    override val adminFilterSavedToast = "Auto-filter settings saved"
    override val adminFilterTesterLabel = "Live Filter Tester"
    override val adminFilterTesterHint = "Type any sample message to test..."
    override val adminFilterTesterClean = "Clean — no restricted words detected"
    override val adminFilterTesterFlagged = "Flagged"
    override val defaultBroadcastTitle = "Admin Notice"
}

fun stringsForLanguage(language: AppLanguage): AppStrings =
    when (language) {
        AppLanguage.BANGLA -> BanglaStrings
        AppLanguage.ENGLISH -> EnglishStrings
    }

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { BanglaStrings }
