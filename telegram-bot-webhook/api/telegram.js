const { initializeApp, cert, getApps } = require("firebase-admin/app");
const { getFirestore, Timestamp, FieldValue } = require("firebase-admin/firestore");

const DEFAULT_DATABASE_ID = "ai-studio-android-banglach-63aa7c60-ef42-45f3-b626-422fe5d420a5";

function getDb() {
  if (!getApps().length) {
    const rawJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
    if (!rawJson) {
      throw new Error("Missing FIREBASE_SERVICE_ACCOUNT_JSON environment variable");
    }
    const serviceAccount = JSON.parse(rawJson);
    initializeApp({ credential: cert(serviceAccount) });
  }
  const databaseId = process.env.FIRESTORE_DATABASE_ID || DEFAULT_DATABASE_ID;
  return getFirestore(getApps()[0], databaseId);
}

async function telegramApi(method, payload) {
  const token = process.env.TELEGRAM_BOT_TOKEN;
  if (!token) throw new Error("Missing TELEGRAM_BOT_TOKEN");
  const resp = await fetch(`https://api.telegram.org/bot${token}/${method}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return resp.json();
}

function escapeHtml(text = "") {
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

async function applyModeration(db, uid, action, extraReason = "") {
  const modRef = db.collection("moderation").doc(uid);
  const now = FieldValue.serverTimestamp();

  if (action === "ban") {
    await modRef.set({
      uid,
      banned: true,
      deletedByAdmin: false,
      mutedUntil: null,
      reason: (extraReason || "Banned via Telegram Bot").slice(0, 300),
      updatedBy: "telegram_bot",
      updatedAt: now,
    });
    return `🚫 ইউজার <code>${escapeHtml(uid)}</code> স্থায়ীভাবে <b>ব্যান</b> করা হয়েছে।`;
  }

  if (action === "unb") {
    await modRef.set({
      uid,
      banned: false,
      deletedByAdmin: false,
      mutedUntil: null,
      reason: "",
      updatedBy: "telegram_bot",
      updatedAt: now,
    });
    return `✅ ইউজার <code>${escapeHtml(uid)}</code> এর সব নিষেধাজ্ঞা (Ban/Mute) সরানো হয়েছে।`;
  }

  if (action === "del") {
    await modRef.set({
      uid,
      banned: true,
      deletedByAdmin: true,
      mutedUntil: null,
      reason: (extraReason || "Account removed by Admin via Telegram").slice(0, 300),
      updatedBy: "telegram_bot",
      updatedAt: now,
    });
    const profileSnap = await db.collection("profiles").doc(uid).get();
    if (profileSnap.exists) {
      const username = (profileSnap.data()?.username || "").trim().toLowerCase();
      if (username) {
        await db.collection("usernames").doc(username).delete().catch(() => {});
      }
      await db.collection("profiles").doc(uid).delete().catch(() => {});
    }
    await db.collection("users").doc(uid).delete().catch(() => {});
    return `🗑️ ইউজার <code>${escapeHtml(uid)}</code> এর অ্যাকাউন্ট স্থায়ীভাবে <b>রিমুভ ও ব্লক</b> করা হয়েছে।`;
  }

  if (action.startsWith("mute:")) {
    const hours = Math.max(1, Math.min(8760, parseInt(action.split(":")[1], 10) || 24));
    const untilDate = new Date(Date.now() + hours * 3600 * 1000);
    await modRef.set({
      uid,
      banned: false,
      deletedByAdmin: false,
      mutedUntil: Timestamp.fromDate(untilDate),
      reason: (extraReason || `Muted for ${hours} hours via Telegram Bot`).slice(0, 300),
      updatedBy: "telegram_bot",
      updatedAt: now,
    });
    return `🔇 ইউজার <code>${escapeHtml(uid)}</code> কে <b>${hours} ঘণ্টার</b> জন্য মিউট করা হয়েছে। এই সময়ে সে কাউকে মেসেজ বা কল করতে পারবে না।`;
  }

  return "❓ অজানা কমান্ড।";
}

module.exports = async (req, res) => {
  if (req.method !== "POST") {
    return res.status(200).json({ ok: true, service: "Bangla Chat Telegram Moderation Webhook" });
  }

  try {
    const adminChatId = String(process.env.TELEGRAM_ADMIN_CHAT_ID || "").trim();
    if (!adminChatId) {
      return res.status(500).json({ error: "TELEGRAM_ADMIN_CHAT_ID not configured" });
    }

    const update = req.body || {};
    const db = getDb();

    // 1. Handle Inline Keyboard Button Callbacks from Registration / Report Alerts
    if (update.callback_query) {
      const cq = update.callback_query;
      const fromChatId = String(cq.message?.chat?.id || cq.from?.id || "");
      if (fromChatId !== adminChatId) {
        await telegramApi("answerCallbackQuery", {
          callback_query_id: cq.id,
          text: "⛔ Unauthorized admin chat",
          show_alert: true,
        });
        return res.status(200).json({ ok: true });
      }

      const data = String(cq.data || "");
      const sepIdx = data.indexOf(":");
      const cmd = sepIdx > 0 ? data.slice(0, sepIdx) : "";
      const targetUid = sepIdx > 0 ? data.slice(sepIdx + 1) : "";

      let replyText = "❓ Invalid action";
      if (targetUid) {
        if (cmd === "m1") replyText = await applyModeration(db, targetUid, "mute:1");
        else if (cmd === "m24") replyText = await applyModeration(db, targetUid, "mute:24");
        else if (cmd === "m7d") replyText = await applyModeration(db, targetUid, "mute:168");
        else if (cmd === "ban") replyText = await applyModeration(db, targetUid, "ban");
        else if (cmd === "del") replyText = await applyModeration(db, targetUid, "del");
        else if (cmd === "unb") replyText = await applyModeration(db, targetUid, "unb");
      }

      await telegramApi("answerCallbackQuery", {
        callback_query_id: cq.id,
        text: "✅ সম্পন্ন হয়েছে!",
      });
      await telegramApi("sendMessage", {
        chat_id: adminChatId,
        text: replyText,
        parse_mode: "HTML",
      });
      return res.status(200).json({ ok: true });
    }

    // 2. Handle Slash Commands from Admin Chat
    if (update.message && update.message.text) {
      const msg = update.message;
      const chatId = String(msg.chat?.id || "");
      if (chatId !== adminChatId) {
        return res.status(200).json({ ok: true });
      }

      const parts = msg.text.trim().split(/\s+/);
      const command = parts[0].toLowerCase();
      const targetUid = parts[1] || "";

      let reply = "";

      if (command === "/start" || command === "/help") {
        reply =
          "🛡️ <b>Bangla Chat Admin Moderation Bot</b>\n\n" +
          "ব্যবহারযোগ্য কমান্ডসমূহ:\n" +
          "• <code>/users</code> — সর্বশেষ ১৫ জন ইউজারের নাম, নম্বর ও UID\n" +
          "• <code>/info &lt;uid&gt;</code> — ইউজারের বিস্তারিত ও স্ট্যাটাস\n" +
          "• <code>/mute &lt;uid&gt; &lt;hours&gt; [কারণ]</code> — নির্দিষ্ট সময়ের জন্য মেসেজ বন্ধ\n" +
          "• <code>/ban &lt;uid&gt; [কারণ]</code> — স্থায়ী ব্যান\n" +
          "• <code>/unban &lt;uid&gt;</code> — ব্যান/মিউট সরান\n" +
          "• <code>/delete &lt;uid&gt; [কারণ]</code> — অ্যাকাউন্ট সম্পূর্ণ রিমুভ ও ব্লক";
      } else if (command === "/users") {
        const snap = await db.collection("profiles").limit(15).get();
        if (snap.empty) {
          reply = "কোনো ইউজার পাওয়া যায়নি।";
        } else {
          const lines = snap.docs.map((doc, idx) => {
            const d = doc.data();
            return `${idx + 1}. <b>${escapeHtml(d.name || "Unknown")}</b> (@${escapeHtml(d.username || "-")})\n   📱 <code>${escapeHtml(d.phoneLookup || "N/A")}</code>\n   🆔 <code>${escapeHtml(doc.id)}</code>`;
          });
          reply = `👥 <b>নিবন্ধিত ইউজার তালিকা (${snap.size})</b>\n\n${lines.join("\n\n")}`;
        }
      } else if (command === "/info" && targetUid) {
        const [profSnap, modSnap] = await Promise.all([
          db.collection("profiles").doc(targetUid).get(),
          db.collection("moderation").doc(targetUid).get(),
        ]);
        const p = profSnap.exists ? profSnap.data() : null;
        const m = modSnap.exists ? modSnap.data() : null;
        reply =
          `🔎 <b>User Info</b>\n` +
          `🆔 <code>${escapeHtml(targetUid)}</code>\n` +
          `👤 নাম: ${escapeHtml(p?.name || "Removed/Not Found")}\n` +
          `🔖 ইউজারনেম: @${escapeHtml(p?.username || "-")}\n` +
          `📱 ফোন: <code>${escapeHtml(p?.phoneLookup || "N/A")}</code>\n` +
          `🚫 Banned: <b>${m?.banned ? "হ্যাঁ" : "না"}</b>\n` +
          `🗑️ Removed: <b>${m?.deletedByAdmin ? "হ্যাঁ" : "না"}</b>\n` +
          `🔇 Muted Until: ${m?.mutedUntil ? escapeHtml(m.mutedUntil.toDate().toISOString()) : "না"}`;
      } else if (command === "/ban" && targetUid) {
        const reason = parts.slice(2).join(" ");
        reply = await applyModeration(db, targetUid, "ban", reason);
      } else if (command === "/mute" && targetUid) {
        const hours = parseInt(parts[2], 10) || 24;
        const reason = parts.slice(3).join(" ");
        reply = await applyModeration(db, targetUid, `mute:${hours}`, reason);
      } else if (command === "/unban" && targetUid) {
        reply = await applyModeration(db, targetUid, "unb");
      } else if (command === "/delete" && targetUid) {
        const reason = parts.slice(2).join(" ");
        reply = await applyModeration(db, targetUid, "del", reason);
      } else {
        reply = "❓ সঠিক ফরম্যাট দিন। সাহায্যের জন্য <code>/help</code> লিখুন।";
      }

      await telegramApi("sendMessage", {
        chat_id: adminChatId,
        text: reply,
        parse_mode: "HTML",
      });
    }

    return res.status(200).json({ ok: true });
  } catch (err) {
    console.error("Webhook error:", err);
    return res.status(200).json({ ok: false, error: err.message });
  }
};
