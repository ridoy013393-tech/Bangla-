const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const DATABASE_ID = "ai-studio-android-banglach-63aa7c60-ef42-45f3-b626-422fe5d420a5";

/**
 * Sends a real-time FCM push notification whenever a new message document
 * is created in /chats/{chatId}/messages/{messageId}.
 * Respects each recipient's private messagePreviewEnabled preference and
 * never exposes server credentials to the Android client.
 */
exports.notifyOnNewMessage = onDocumentCreated(
  {
    document: "chats/{chatId}/messages/{messageId}",
    database: DATABASE_ID,
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const message = snap.data();
    const chatId = event.params.chatId;
    const senderId = message.senderId;
    const participants = Array.isArray(message.participants) ? message.participants : [];
    const recipientIds = participants.filter((uid) => uid && uid !== senderId);
    if (recipientIds.length === 0) return;

    const db = getFirestore(DATABASE_ID);
    const senderProfileSnap = await db.collection("profiles").doc(senderId).get();
    const senderName = senderProfileSnap.exists
      ? senderProfileSnap.data().name || "Bangla Chat"
      : "Bangla Chat";

    for (const recipientUid of recipientIds) {
      const privateSnap = await db.collection("users").doc(recipientUid).get();
      if (!privateSnap.exists) continue;

      const privateData = privateSnap.data();
      const token = privateData.fcmToken;
      if (!token || typeof token !== "string") continue;

      const showPreview = privateData.messagePreviewEnabled !== false;
      let bodyText = "নতুন মেসেজ এসেছে";
      if (showPreview) {
        if (message.type === "image") {
          bodyText = message.text ? `📷 ${message.text}` : "📷 ছবি পাঠানো হয়েছে";
        } else if (message.type === "document") {
          bodyText = `📎 ${message.mediaName || "ডকুমেন্ট পাঠানো হয়েছে"}`;
        } else {
          bodyText = (message.text || "").slice(0, 140) || "নতুন মেসেজ";
        }
      }

      await getMessaging().send({
        token,
        data: {
          chatId,
          senderId,
          senderName,
          previewText: bodyText,
          showPreview: String(showPreview),
        },
        android: {
          priority: "high",
          notification: {
            channelId: "bangla_chat_messages",
            title: senderName,
            body: bodyText,
            clickAction: "OPEN_CHAT",
          },
        },
      });
    }
  }
);
