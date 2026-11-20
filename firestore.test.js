const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const CHARLIE_UID = "charlie_uid_303";
const CHAT_AB = `${ALICE_UID}_${BOB_UID}`;

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function validProfile(uid, overrides = {}) {
  const now = pastDate();
  return {
    uid,
    name: uid === ALICE_UID ? "Tania Rahman" : "Riyad Hasan",
    username: uid === ALICE_UID ? "tania_rahman" : "riyad_hasan",
    phoneLookup: "+8801712345678",
    photoUrl: "",
    about: "সবার সাথে থাকুন, সবসময়",
    online: true,
    discoverable: true,
    showOnlineStatus: true,
    showLastSeen: true,
    showReadReceipts: true,
    lastSeen: now,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validPrivateUser(uid, overrides = {}) {
  const now = pastDate();
  return {
    uid,
    email: `${uid}@example.com`,
    phone: "+8801712345678",
    messagePreviewEnabled: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validChat(overrides = {}) {
  const now = pastDate();
  return {
    participants: [ALICE_UID, BOB_UID],
    participantNames: { [ALICE_UID]: "Tania Rahman", [BOB_UID]: "Riyad Hasan" },
    participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "" },
    lastMessage: "আসসালামু আলাইকুম",
    lastMessageType: "text",
    lastSenderId: ALICE_UID,
    lastMessageTime: now,
    unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
    typingUids: [],
    createdBy: ALICE_UID,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validMessage(overrides = {}) {
  const now = pastDate();
  return {
    chatId: CHAT_AB,
    senderId: ALICE_UID,
    participants: [ALICE_UID, BOB_UID],
    text: "কি করছো?",
    type: "text",
    delivered: false,
    read: false,
    deleted: false,
    timestamp: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("profiles").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("chats").where("participants", "array-contains", ALICE_UID).get());
  await assertFails(unauthDb.collection("profiles").doc(ALICE_UID).set(validProfile(ALICE_UID)));
});

test("Public profiles: owner can create/update; search enforces discoverable filter; shadow & future-timestamp writes fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("profiles").doc(ALICE_UID).set(validProfile(ALICE_UID)));

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      name: "Tania Akter",
      about: "নতুন স্ট্যাটাস",
      updatedAt: pastDate(1000),
    })
  );

  // Presence heartbeat allowed
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      online: false,
      lastSeen: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Bob cannot modify Alice's profile
  await assertFails(
    bobDb.collection("profiles").doc(ALICE_UID).update({
      name: "Hijacked",
      updatedAt: pastDate(500),
    })
  );

  // Discoverable query succeeds, unfiltered query fails (Query Trust Test)
  await assertSucceeds(bobDb.collection("profiles").where("discoverable", "==", true).get());
  await assertFails(bobDb.collection("profiles").get());

  // Shadow field & @DocumentId 'id' rejected
  await assertFails(
    bobDb.collection("profiles").doc(BOB_UID).set(validProfile(BOB_UID, { isAdmin: true }))
  );
  await assertFails(
    bobDb.collection("profiles").doc(BOB_UID).set(validProfile(BOB_UID, { id: BOB_UID }))
  );

  // Future timestamp rejected
  await assertFails(
    bobDb.collection("profiles").doc(BOB_UID).set(
      validProfile(BOB_UID, { createdAt: new Date(Date.now() + 60000) })
    )
  );
});

test("Private users PII isolation: strictly owner-only read/write, list denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validPrivateUser(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fcmToken: "sample_fcm_token_xyz",
      updatedAt: pastDate(1000),
    })
  );

  // PII Blanket Test: Bob cannot read or modify Alice's private doc
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      phone: "+8801900000000",
      updatedAt: pastDate(1000),
    })
  );
});

test("User subcollections (blockedUsers, favorites, callLogs) enforce owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Favorites
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").doc(BOB_UID).set({
      favoriteUid: BOB_UID,
      name: "Riyad Hasan",
      username: "riyad_hasan",
      photoUrl: "",
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("favorites").get());

  // Cannot self-block
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("blockedUsers").doc(ALICE_UID).set({
      blockedUid: ALICE_UID,
      blockedName: "Self",
      createdAt: pastDate(),
    })
  );

  // Call logs
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("callLogs").doc("call_1").set({
      peerUid: BOB_UID,
      peerName: "Riyad Hasan",
      peerPhotoUrl: "",
      direction: "missed",
      callType: "audio",
      durationSeconds: 0,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("callLogs").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("callLogs").get());
});

test("Chats & messages: participants can chat, react, mark read, soft-delete; outsiders and blocked users denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // First-time getOrCreateChat existence check before chat exists:
  // participants in deterministic chatId succeed, outsiders and non-participant IDs fail
  await assertSucceeds(aliceDb.collection("chats").doc(CHAT_AB).get());
  await assertSucceeds(bobDb.collection("chats").doc(CHAT_AB).get());
  await assertFails(charlieDb.collection("chats").doc(CHAT_AB).get());
  await assertFails(aliceDb.collection("chats").doc("group_nonexistent").get());

  // Create chat (matching Kotlin getOrCreateChat payload with empty initial message)
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).set(
      validChat({ lastMessage: "", lastSenderId: "", unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 } })
    )
  );

  // Filtered participant list query succeeds; unfiltered fails
  await assertSucceeds(
    aliceDb.collection("chats").where("participants", "array-contains", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("chats").get());

  // Outsider Charlie cannot read or update chat
  await assertFails(charlieDb.collection("chats").doc(CHAT_AB).get());

  // Alice sends a message
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_1").set(validMessage())
  );

  // Filtered message query succeeds; outsider cannot read
  await assertSucceeds(
    bobDb.collection("chats").doc(CHAT_AB).collection("messages")
      .where("participants", "array-contains", BOB_UID).get()
  );
  await assertFails(
    charlieDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_1").get()
  );

  // Bob marks message delivered & read
  await assertSucceeds(
    bobDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_1").update({
      delivered: true,
      read: true,
      updatedAt: pastDate(1000),
    })
  );

  // Bob adds reaction
  await assertSucceeds(
    bobDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_1").update({
      reactions: { [BOB_UID]: "❤️" },
      updatedAt: pastDate(900),
    })
  );

  // Bob cannot soft-delete Alice's message
  await assertFails(
    bobDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_1").update({
      deleted: true,
      text: "",
      updatedAt: pastDate(800),
    })
  );

  // Alice can soft-delete her own message
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_1").update({
      deleted: true,
      text: "",
      updatedAt: pastDate(700),
    })
  );

  // Typing indicator update succeeds
  await assertSucceeds(
    bobDb.collection("chats").doc(CHAT_AB).update({
      typingUids: [BOB_UID],
      updatedAt: pastDate(600),
    })
  );

  // Blocking Bob prevents new messages in the chat
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("blockedUsers").doc(BOB_UID).set({
      blockedUid: BOB_UID,
      blockedName: "Riyad Hasan",
      createdAt: pastDate(500),
    })
  );

  await assertFails(
    bobDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_2").set(
      validMessage({ senderId: BOB_UID, text: "Blocked test" })
    )
  );
});

test("Reports: user can report another account, cannot self-report or read others' reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("reports").doc("rep_1").set({
      reporterUid: ALICE_UID,
      reportedUid: BOB_UID,
      reason: "Spam",
      details: "Unsolicited links",
      createdAt: pastDate(),
    })
  );

  await assertFails(
    aliceDb.collection("reports").doc("rep_self").set({
      reporterUid: ALICE_UID,
      reportedUid: ALICE_UID,
      reason: "Self",
      createdAt: pastDate(),
    })
  );

  await assertFails(bobDb.collection("reports").doc("rep_1").get());
  await assertSucceeds(aliceDb.collection("reports").where("reporterUid", "==", ALICE_UID).get());
});

test("WhatsApp chat & privacy actions: edit own text, star, pin, disappearing timer, mute/archive, photo/about privacy", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  // Profile photo & about privacy toggles
  await assertSucceeds(aliceDb.collection("profiles").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      showProfilePhoto: false,
      showAbout: false,
      updatedAt: pastDate(1000),
    })
  );

  // Create chat and message
  await assertSucceeds(aliceDb.collection("chats").doc(CHAT_AB).set(validChat()));
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_edit").set(validMessage())
  );

  // Alice can edit her own text message; Bob cannot
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_edit").update({
      text: "সম্পাদিত মেসেজ",
      edited: true,
      updatedAt: pastDate(900),
    })
  );
  await assertFails(
    bobDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_edit").update({
      text: "Hijacked edit",
      edited: true,
      updatedAt: pastDate(800),
    })
  );

  // Participant can star a message; outsider cannot
  await assertSucceeds(
    bobDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_edit").update({
      starredBy: [BOB_UID],
      updatedAt: pastDate(700),
    })
  );
  await assertFails(
    charlieDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_edit").update({
      starredBy: [CHARLIE_UID],
      updatedAt: pastDate(600),
    })
  );

  // Pin message, disappearing timer (24h), and mute/pin/archive flags
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).update({
      pinnedMessageId: "msg_edit",
      pinnedMessageText: "সম্পাদিত মেসেজ",
      pinnedMessageSender: "Tania Rahman",
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    bobDb.collection("chats").doc(CHAT_AB).update({
      disappearingHours: 24,
      updatedAt: pastDate(400),
    })
  );
  // Invalid disappearing duration rejected
  await assertFails(
    bobDb.collection("chats").doc(CHAT_AB).update({
      disappearingHours: 999,
      updatedAt: pastDate(350),
    })
  );
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).update({
      pinnedBy: [ALICE_UID],
      mutedBy: [ALICE_UID],
      archivedBy: [],
      updatedAt: pastDate(300),
    })
  );
});

test("Group chats, voice messages, 24h statuses, and WebRTC call signaling sessions enforce security rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate();

  // 1. Group Chat create + voice message + member leave + admin rename
  const groupId = "group_friends_1";
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).set({
      participants: [ALICE_UID, BOB_UID, CHARLIE_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob", [CHARLIE_UID]: "Charlie" },
      participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "", [CHARLIE_UID]: "" },
      isGroup: true,
      groupName: "আড্ডা গ্রুপ",
      groupPhotoUrl: "",
      adminUids: [ALICE_UID],
      lastMessage: "গ্রুপ তৈরি হয়েছে",
      lastMessageType: "text",
      lastSenderId: ALICE_UID,
      lastMessageTime: now,
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0, [CHARLIE_UID]: 0 },
      typingUids: [],
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Voice message inside group succeeds
  await assertSucceeds(
    bobDb.collection("chats").doc(groupId).collection("messages").doc("voice_1").set({
      chatId: groupId,
      senderId: BOB_UID,
      senderName: "Bob",
      participants: [ALICE_UID, BOB_UID, CHARLIE_UID],
      text: "",
      type: "audio",
      mediaUrl: "https://res.cloudinary.com/demo/video/upload/v1/voice.m4a",
      mediaName: "voice.m4a",
      mediaSizeBytes: 24000,
      audioDurationSec: 8,
      delivered: false,
      read: false,
      deleted: false,
      timestamp: now,
      updatedAt: now,
    })
  );

  // Admin can rename group; non-admin cannot
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).update({
      groupName: "নতুন আড্ডা গ্রুপ",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    bobDb.collection("chats").doc(groupId).update({
      groupName: "Bob Hijack",
      updatedAt: pastDate(400),
    })
  );

  // Member can leave group
  await assertSucceeds(
    charlieDb.collection("chats").doc(groupId).update({
      participants: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID],
      typingUids: [],
      updatedAt: pastDate(300),
    })
  );

  // 2. 24-Hour Status Stories
  const statusId = "status_alice_1";
  await assertSucceeds(
    aliceDb.collection("statuses").doc(statusId).set({
      uid: ALICE_UID,
      authorName: "Alice",
      authorPhotoUrl: "",
      type: "text",
      text: "শুভ সকাল সবাইকে!",
      backgroundColorHex: 0xFF2563EB,
      viewerUids: [ALICE_UID],
      expiresAt: new Date(Date.now() + 24 * 3600 * 1000),
      createdAt: now,
    })
  );
  // Bob can mark viewed; Bob cannot edit text or delete Alice's status
  await assertSucceeds(
    bobDb.collection("statuses").doc(statusId).update({
      viewerUids: [ALICE_UID, BOB_UID],
    })
  );
  await assertFails(
    bobDb.collection("statuses").doc(statusId).update({
      text: "Hacked status",
    })
  );
  await assertFails(bobDb.collection("statuses").doc(statusId).delete());
  await assertSucceeds(aliceDb.collection("statuses").doc(statusId).delete());

  // 3. WebRTC Call Sessions + ICE Candidates
  const callId = "call_ab_1";
  await assertSucceeds(
    aliceDb.collection("callSessions").doc(callId).set({
      callerUid: ALICE_UID,
      callerName: "Alice",
      callerPhotoUrl: "",
      calleeUid: BOB_UID,
      calleeName: "Bob",
      calleePhotoUrl: "",
      participants: [ALICE_UID, BOB_UID],
      callType: "video",
      status: "ringing",
      offerSdp: "v=0\r\no=- 1 2 IN IP4 127.0.0.1\r\n",
      createdAt: now,
      updatedAt: now,
    })
  );
  // Outsider Charlie cannot read or update call session
  await assertFails(charlieDb.collection("callSessions").doc(callId).get());
  // Bob accepts call & writes answerSdp + ICE candidate
  await assertSucceeds(
    bobDb.collection("callSessions").doc(callId).update({
      status: "accepted",
      answerSdp: "v=0\r\no=- 2 2 IN IP4 127.0.0.1\r\n",
      updatedAt: pastDate(200),
    })
  );
  await assertSucceeds(
    bobDb.collection("callSessions").doc(callId).collection("candidates").doc("ice_1").set({
      senderUid: BOB_UID,
      participants: [ALICE_UID, BOB_UID],
      sdpMid: "0",
      sdpMLineIndex: 0,
      sdp: "candidate:1 1 UDP 2122252543 127.0.0.1 50000 typ host",
      createdAt: pastDate(100),
    })
  );
  await assertFails(
    charlieDb.collection("callSessions").doc(callId).collection("candidates").doc("ice_hack").set({
      senderUid: CHARLIE_UID,
      participants: [ALICE_UID, BOB_UID],
      sdpMid: "0",
      sdpMLineIndex: 0,
      sdp: "candidate:fake",
      createdAt: pastDate(100),
    })
  );
});

test("Unique @username registry: enforces uniqueness, allows exact get lookup, blocks list scraping and hijacking", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate();

  // Unauthenticated denied
  await assertFails(unauthDb.collection("usernames").doc("ridoy1234").get());
  await assertFails(
    unauthDb.collection("usernames").doc("ridoy1234").set({ uid: ALICE_UID, createdAt: now })
  );

  // Invalid username formats rejected
  await assertFails(
    aliceDb.collection("usernames").doc("ab").set({ uid: ALICE_UID, createdAt: now })
  );
  await assertFails(
    aliceDb.collection("usernames").doc("Ridoy1234").set({ uid: ALICE_UID, createdAt: now })
  );

  // Alice claims @ridoy1234
  await assertSucceeds(
    aliceDb.collection("usernames").doc("ridoy1234").set({ uid: ALICE_UID, createdAt: now })
  );

  // Bob can perform exact get lookup, but cannot list all usernames
  await assertSucceeds(bobDb.collection("usernames").doc("ridoy1234").get());
  await assertFails(bobDb.collection("usernames").get());

  // Bob cannot claim or overwrite Alice's username or delete it
  await assertFails(
    bobDb.collection("usernames").doc("ridoy1234").set({ uid: BOB_UID, createdAt: pastDate(500) })
  );
  await assertFails(bobDb.collection("usernames").doc("ridoy1234").delete());

  // Alice releases @ridoy1234, then Bob can claim it
  await assertSucceeds(aliceDb.collection("usernames").doc("ridoy1234").delete());
  await assertSucceeds(
    bobDb.collection("usernames").doc("ridoy1234").set({ uid: BOB_UID, createdAt: pastDate(200) })
  );
});

test("In-chat call event messages & Delete-for-me (deletedFor) isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const chatId = `${ALICE_UID}_${BOB_UID}`;
  const now = pastDate(2000);

  // Create direct chat between Alice and Bob
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participants: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "" },
      lastMessage: "",
      lastMessageType: "text",
      lastSenderId: "",
      lastMessageTime: now,
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      typingUids: [],
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 1. Alice sends a regular message; Bob hides it ONLY for himself via deletedFor
  const msgRef = aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_peer_1");
  await assertSucceeds(
    msgRef.set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participants: [ALICE_UID, BOB_UID],
      text: "Hello Bob!",
      type: "text",
      delivered: false,
      read: false,
      deleted: false,
      deletedFor: [],
      edited: false,
      forwarded: false,
      reactions: {},
      starredBy: [],
      timestamp: now,
      updatedAt: now,
    })
  );

  // Bob CAN hide Alice's message for himself only
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_peer_1").update({
      deletedFor: [BOB_UID],
      updatedAt: pastDate(1000),
    })
  );

  // Bob CANNOT edit text while hiding message for himself
  await assertFails(
    bobDb.collection("chats").doc(chatId).collection("messages").doc("msg_peer_1").update({
      deletedFor: [BOB_UID],
      text: "Tampered text",
      updatedAt: pastDate(900),
    })
  );

  // Alice CANNOT strip Bob's UID out of deletedFor when adding herself
  await assertFails(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_peer_1").update({
      deletedFor: [ALICE_UID],
      updatedAt: pastDate(800),
    })
  );

  // Alice CAN add herself while preserving Bob's UID
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("msg_peer_1").update({
      deletedFor: [BOB_UID, ALICE_UID],
      updatedAt: pastDate(700),
    })
  );

  // Outsider Charlie CANNOT hide or modify deletedFor
  await assertFails(
    charlieDb.collection("chats").doc(chatId).collection("messages").doc("msg_peer_1").update({
      deletedFor: [BOB_UID, ALICE_UID, CHARLIE_UID],
      updatedAt: pastDate(600),
    })
  );

  // 2. Alice logs an in-chat call event message with duration & updates chat preview
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("call_msg_1").set({
      chatId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participants: [ALICE_UID, BOB_UID],
      text: "অডিও কল • ২ মিনিট ১৫ সেকেন্ড",
      type: "call",
      callType: "audio",
      callStatus: "answered",
      callDurationSec: 135,
      delivered: true,
      read: false,
      deleted: false,
      deletedFor: [],
      timestamp: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).update({
      lastMessage: "অডিও কল • ২ মিনিট ১৫ সেকেন্ড",
      lastMessageType: "call",
      lastSenderId: ALICE_UID,
      lastMessageTime: pastDate(500),
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      typingUids: [],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "" },
      updatedAt: pastDate(500),
    })
  );

  // Invalid call status or negative duration rejected
  await assertFails(
    aliceDb.collection("chats").doc(chatId).collection("messages").doc("call_msg_bad").set({
      chatId,
      senderId: ALICE_UID,
      participants: [ALICE_UID, BOB_UID],
      text: "Bad call",
      type: "call",
      callType: "audio",
      callStatus: "hacked",
      callDurationSec: -5,
      delivered: true,
      read: false,
      deleted: false,
      timestamp: pastDate(400),
      updatedAt: pastDate(400),
    })
  );
});

test("WhatsApp-style calling->ringing handshake & chat-level delivery/read receipt updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate(2000);

  // 1. Caller creates call session in 'calling' state (offline/unreached until callee acknowledges)
  const callId = "call_calling_handshake_1";
  await assertSucceeds(
    aliceDb.collection("callSessions").doc(callId).set({
      callerUid: ALICE_UID,
      callerName: "Alice",
      callerPhotoUrl: "",
      calleeUid: BOB_UID,
      calleeName: "Bob",
      calleePhotoUrl: "",
      participants: [ALICE_UID, BOB_UID],
      callType: "audio",
      status: "calling",
      offerSdp: "v=0\r\no=- 1 2 IN IP4 127.0.0.1\r\n",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Callee device receives it online and transitions status: 'calling' -> 'ringing'
  await assertSucceeds(
    bobDb.collection("callSessions").doc(callId).update({
      status: "ringing",
      updatedAt: pastDate(1500),
    })
  );

  // Outsider Charlie still cannot update call status
  await assertFails(
    charlieDb.collection("callSessions").doc(callId).update({
      status: "ringing",
      updatedAt: pastDate(1400),
    })
  );

  // 2. Chat-level lastMessageDelivered / lastMessageRead transitions
  const chatId = `${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    aliceDb.collection("chats").doc(chatId).set({
      participants: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "" },
      lastMessage: "তুমি কি অনলাইনে আছো?",
      lastMessageType: "text",
      lastSenderId: ALICE_UID,
      lastMessageTime: now,
      lastMessageDelivered: false,
      lastMessageRead: false,
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      typingUids: [],
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob acknowledges delivery in background without altering lastMessage text
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).update({
      lastMessageDelivered: true,
      updatedAt: pastDate(1200),
    })
  );

  // Bob opens chat and marks read + resets unread count
  await assertSucceeds(
    bobDb.collection("chats").doc(chatId).update({
      lastMessageDelivered: true,
      lastMessageRead: true,
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      updatedAt: pastDate(1000),
    })
  );

  // Outsider Charlie cannot mark delivered/read
  await assertFails(
    charlieDb.collection("chats").doc(chatId).update({
      lastMessageDelivered: true,
      updatedAt: pastDate(900),
    })
  );
});

test("v1.5 features: Polls voting, View Once opened receipt, Group admin-only send, and Busy call status", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate(3000);

  // 1. Busy call status transition
  const busyCallId = "call_busy_test_1";
  await assertSucceeds(
    aliceDb.collection("callSessions").doc(busyCallId).set({
      callerUid: ALICE_UID,
      callerName: "Alice",
      callerPhotoUrl: "",
      calleeUid: BOB_UID,
      calleeName: "Bob",
      calleePhotoUrl: "",
      participants: [ALICE_UID, BOB_UID],
      callType: "audio",
      status: "calling",
      offerSdp: "v=0",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    bobDb.collection("callSessions").doc(busyCallId).update({
      status: "busy",
      updatedAt: pastDate(2500),
    })
  );

  // 2. Group chat with admin-only send + Poll + View Once photo
  const groupId = "group_v15_test";
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).set({
      participants: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "" },
      isGroup: true,
      groupName: "Bangla Devs",
      groupPhotoUrl: "",
      adminUids: [ALICE_UID],
      onlyAdminsCanSend: false,
      lastMessage: "স্বাগতম",
      lastMessageType: "text",
      lastSenderId: ALICE_UID,
      lastMessageTime: now,
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      typingUids: [],
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Poll created by Alice
  const pollMsgRef = aliceDb.collection("chats").doc(groupId).collection("messages").doc("poll_1");
  await assertSucceeds(
    pollMsgRef.set({
      chatId: groupId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participants: [ALICE_UID, BOB_UID],
      text: "আজ কখন মিটিং?",
      type: "poll",
      pollQuestion: "আজ কখন মিটিং?",
      pollOptions: ["রাত ৮টা", "রাত ৯টা", "আগামীকাল"],
      pollVotes: {},
      delivered: true,
      read: false,
      deleted: false,
      timestamp: pastDate(2200),
      updatedAt: pastDate(2200),
    })
  );

  // Bob casts his own vote -> succeeds
  await assertSucceeds(
    bobDb.collection("chats").doc(groupId).collection("messages").doc("poll_1").update({
      pollVotes: { [BOB_UID]: "1" },
      updatedAt: pastDate(2000),
    })
  );

  // Bob tries to tamper with Alice's vote -> fails
  await assertFails(
    bobDb.collection("chats").doc(groupId).collection("messages").doc("poll_1").update({
      pollVotes: { [BOB_UID]: "1", [ALICE_UID]: "0" },
      updatedAt: pastDate(1900),
    })
  );

  // View Once photo sent by Alice
  const voRef = aliceDb.collection("chats").doc(groupId).collection("messages").doc("vo_1");
  await assertSucceeds(
    voRef.set({
      chatId: groupId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participants: [ALICE_UID, BOB_UID],
      text: "",
      type: "image",
      mediaUrl: "https://example.com/secret.jpg",
      mediaName: "secret.jpg",
      mediaSizeBytes: 12345,
      viewOnce: true,
      viewedBy: [],
      delivered: true,
      read: false,
      deleted: false,
      timestamp: pastDate(1800),
      updatedAt: pastDate(1800),
    })
  );

  // Bob opens View Once photo and appends his UID to viewedBy -> succeeds
  await assertSucceeds(
    bobDb.collection("chats").doc(groupId).collection("messages").doc("vo_1").update({
      viewedBy: [BOB_UID],
      updatedAt: pastDate(1700),
    })
  );

  // Alice enables onlyAdminsCanSend -> succeeds
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).update({
      onlyAdminsCanSend: true,
      updatedAt: pastDate(1600),
    })
  );

  // Non-admin Bob tries to send a message when onlyAdminsCanSend == true -> fails
  await assertFails(
    bobDb.collection("chats").doc(groupId).collection("messages").doc("bob_blocked_msg").set({
      chatId: groupId,
      senderId: BOB_UID,
      senderName: "Bob",
      participants: [ALICE_UID, BOB_UID],
      text: "হ্যালো",
      type: "text",
      delivered: false,
      read: false,
      deleted: false,
      timestamp: pastDate(1500),
      updatedAt: pastDate(1500),
    })
  );

  // Admin Alice can still send a message -> succeeds
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).collection("messages").doc("alice_admin_msg").set({
      chatId: groupId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participants: [ALICE_UID, BOB_UID],
      text: "শুধু অ্যাডমিন নোটিশ",
      type: "text",
      delivered: false,
      read: false,
      deleted: false,
      timestamp: pastDate(1400),
      updatedAt: pastDate(1400),
    })
  );

  // Non-admin Bob cannot promote himself or remove Alice
  await assertFails(
    bobDb.collection("chats").doc(groupId).update({
      adminUids: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(1300),
    })
  );

  // Outsider Charlie cannot vote on poll
  await assertFails(
    charlieDb.collection("chats").doc(groupId).collection("messages").doc("poll_1").update({
      pollVotes: { [BOB_UID]: "1", [CHARLIE_UID]: "0" },
      updatedAt: pastDate(1200),
    })
  );
});



