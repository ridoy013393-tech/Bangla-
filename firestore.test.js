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

test("v1.6 Super Admin moderation: ban/mute/delete enforcement and strict non-admin isolation", async () => {
  const ADMIN_UID = "super_admin_uid_001";
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "mehedi0169019@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const now = pastDate(3000);

  // Setup profiles, users, and chat
  await assertSucceeds(aliceDb.collection("profiles").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(bobDb.collection("profiles").doc(BOB_UID).set(validProfile(BOB_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validPrivateUser(ALICE_UID)));
  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).set(validPrivateUser(BOB_UID)));
  await assertSucceeds(aliceDb.collection("chats").doc(CHAT_AB).set(validChat()));

  // Non-admin cannot write moderation status or read someone else's moderation status
  await assertFails(
    aliceDb.collection("moderation").doc(BOB_UID).set({
      uid: BOB_UID,
      banned: true,
      deletedByAdmin: false,
      reason: "Unauthorized ban",
      updatedAt: now,
    })
  );
  await assertFails(aliceDb.collection("moderation").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("moderation").get());

  // Non-admin cannot list all private users
  await assertFails(aliceDb.collection("users").get());

  // Super Admin CAN list profiles, users, and reports
  await assertSucceeds(adminDb.collection("profiles").get());
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(adminDb.collection("reports").get());

  // Super Admin mutes Bob for 2 hours
  const futureMute = new Date(Date.now() + 2 * 3600 * 1000);
  await assertSucceeds(
    adminDb.collection("moderation").doc(BOB_UID).set({
      uid: BOB_UID,
      banned: false,
      deletedByAdmin: false,
      mutedUntil: futureMute,
      reason: "Spam warning",
      updatedBy: "mehedi0169019@gmail.com",
      updatedAt: pastDate(2000),
    })
  );

  // Bob CAN read his own moderation doc (for UI countdown banner)
  await assertSucceeds(bobDb.collection("moderation").doc(BOB_UID).get());
  // Alice CANNOT read Bob's moderation doc
  await assertFails(aliceDb.collection("moderation").doc(BOB_UID).get());

  // Muted Bob is DENIED creating messages, chats, calls, and statuses
  await assertFails(
    bobDb.collection("chats").doc(CHAT_AB).collection("messages").doc("muted_msg").set(
      validMessage({ senderId: BOB_UID, text: "I am muted" })
    )
  );
  await assertFails(
    bobDb.collection("statuses").doc("muted_status").set({
      uid: BOB_UID,
      authorName: "Bob",
      type: "text",
      text: "Status while muted",
      viewerUids: [BOB_UID],
      expiresAt: new Date(Date.now() + 3600 * 1000),
      createdAt: pastDate(1000),
    })
  );

  // Expired mute allows Bob to send messages again
  await assertSucceeds(
    adminDb.collection("moderation").doc(BOB_UID).set({
      uid: BOB_UID,
      banned: false,
      deletedByAdmin: false,
      mutedUntil: pastDate(60000),
      reason: "",
      updatedBy: "mehedi0169019@gmail.com",
      updatedAt: pastDate(1500),
    })
  );
  await assertSucceeds(
    bobDb.collection("chats").doc(CHAT_AB).collection("messages").doc("unmuted_msg").set(
      validMessage({ senderId: BOB_UID, text: "Mute expired!" })
    )
  );

  // Super Admin permanently bans Bob -> Bob is DENIED sending messages & starting calls
  await assertSucceeds(
    adminDb.collection("moderation").doc(BOB_UID).set({
      uid: BOB_UID,
      banned: true,
      deletedByAdmin: false,
      mutedUntil: null,
      reason: "Policy violation",
      updatedBy: "telegram_bot",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    bobDb.collection("chats").doc(CHAT_AB).collection("messages").doc("banned_msg").set(
      validMessage({ senderId: BOB_UID, text: "Banned message" })
    )
  );
  await assertFails(
    bobDb.collection("callSessions").doc("banned_call").set({
      callerUid: BOB_UID,
      callerName: "Bob",
      calleeUid: ALICE_UID,
      calleeName: "Alice",
      participants: [BOB_UID, ALICE_UID],
      callType: "audio",
      status: "calling",
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Super Admin can delete a banned user's profile and private user doc
  await assertSucceeds(adminDb.collection("profiles").doc(BOB_UID).delete());
  await assertSucceeds(adminDb.collection("users").doc(BOB_UID).delete());
});

test("v1.7 Admin Broadcast, Verified Badge isolation, and Auto-Moderation config rules", async () => {
  const adminDb = testEnv.authenticatedContext("super_admin_uid", {
    email: "mehedi0169019@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  // Regular user CANNOT self-register with verified: true
  await assertFails(
    aliceDb.collection("profiles").doc(ALICE_UID).set(validProfile(ALICE_UID, { verified: true }))
  );

  // Regular user registers normally (verified defaults/false)
  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );

  // Regular user CANNOT self-update verified to true
  await assertFails(
    aliceDb.collection("profiles").doc(ALICE_UID).update({
      verified: true,
      updatedAt: pastDate(1000),
    })
  );

  // Super Admin CAN grant and revoke verified badge on Alice's profile
  await assertSucceeds(
    adminDb.collection("profiles").doc(ALICE_UID).update({
      verified: true,
      updatedAt: pastDate(1000),
    })
  );
  // Super Admin CANNOT tamper with Alice's name via the verified-only rule
  await assertFails(
    adminDb.collection("profiles").doc(ALICE_UID).update({
      verified: true,
      name: "Hijacked",
      updatedAt: pastDate(500),
    })
  );

  // Regular user CANNOT write global broadcast or moderation config
  const broadcastPayload = {
    active: true,
    broadcastId: "notice_2026_1",
    title: "গুরুত্বপূর্ণ আপডেট",
    message: "Bangla Chat v1.7 এখন লাইভ!",
    severity: "update",
    updatedBy: "mehedi0169019@gmail.com",
    updatedAt: pastDate(1000),
  };
  await assertFails(aliceDb.collection("appConfig").doc("broadcast").set(broadcastPayload));

  // Super Admin CAN publish broadcast & all signed-in users CAN read it
  await assertSucceeds(adminDb.collection("appConfig").doc("broadcast").set(broadcastPayload));
  await assertSucceeds(aliceDb.collection("appConfig").doc("broadcast").get());
  await assertSucceeds(bobDb.collection("appConfig").doc("broadcast").get());

  // Super Admin CAN update auto-moderation filter settings & signed-in users CAN read it
  const modConfigPayload = {
    profanityFilterEnabled: true,
    profanityMode: "mask",
    spamProtectionEnabled: true,
    telegramAlertsEnabled: true,
    customBlockedWords: ["scamlink", "ফ্রি_টাকা"],
    updatedBy: "mehedi0169019@gmail.com",
    updatedAt: pastDate(1000),
  };
  await assertFails(bobDb.collection("appConfig").doc("moderation").set(modConfigPayload));
  await assertSucceeds(adminDb.collection("appConfig").doc("moderation").set(modConfigPayload));
  await assertSucceeds(bobDb.collection("appConfig").doc("moderation").get());

  // Invalid severity or mode is rejected even for Super Admin
  await assertFails(
    adminDb.collection("appConfig").doc("broadcast").set({
      ...broadcastPayload,
      severity: "invalid_level",
    })
  );
  await assertFails(
    adminDb.collection("appConfig").doc("moderation").set({
      ...modConfigPayload,
      profanityMode: "unknown_mode",
    })
  );
});

test("v1.8 Group management, system events, admin moderation, deletion, and per-user clear/delete chat", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate(5000);
  const groupId = "group_v18_full_test";

  // 1. Create group with groupDescription & hiddenBy
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).set({
      participants: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "" },
      isGroup: true,
      groupName: "বাংলা চ্যাট টিম",
      groupDescription: "অফিশিয়াল আলোচনা গ্রুপ",
      groupPhotoUrl: "",
      adminUids: [ALICE_UID],
      onlyAdminsCanSend: false,
      hiddenBy: [],
      lastMessage: "Alice গ্রুপ তৈরি করেছেন",
      lastMessageType: "system",
      lastSenderId: ALICE_UID,
      lastMessageTime: now,
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      typingUids: [],
      createdBy: ALICE_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // 2. Initial message by Bob before Charlie joins
  await assertSucceeds(
    bobDb.collection("chats").doc(groupId).collection("messages").doc("bob_old_msg").set({
      chatId: groupId,
      senderId: BOB_UID,
      senderName: "Bob",
      participants: [ALICE_UID, BOB_UID],
      text: "পুরনো মেসেজ",
      type: "text",
      delivered: true,
      read: false,
      deleted: false,
      timestamp: pastDate(4500),
      updatedAt: pastDate(4500),
    })
  );

  // Outsider Charlie cannot read messages before joining
  await assertFails(
    charlieDb.collection("chats").doc(groupId).collection("messages").orderBy("timestamp", "desc").limit(50).get()
  );

  // 3. Admin Alice updates groupDescription & groupPhotoUrl; non-admin Bob is denied
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).update({
      groupDescription: "আপডেটেড গ্রুপ বিবরণ",
      groupPhotoUrl: "https://res.cloudinary.com/demo/image/upload/v1/group.jpg",
      updatedAt: pastDate(4000),
    })
  );
  await assertFails(
    bobDb.collection("chats").doc(groupId).update({
      groupDescription: "Bob hijack description",
      updatedAt: pastDate(3900),
    })
  );

  // 4. Non-admin Bob is denied pinning message or changing disappearing timer in group; Admin Alice succeeds
  await assertFails(
    bobDb.collection("chats").doc(groupId).update({
      pinnedMessageId: "bob_old_msg",
      pinnedMessageText: "পুরনো মেসেজ",
      pinnedMessageSender: "Bob",
      updatedAt: pastDate(3800),
    })
  );
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).update({
      pinnedMessageId: "bob_old_msg",
      pinnedMessageText: "পুরনো মেসেজ",
      pinnedMessageSender: "Bob",
      updatedAt: pastDate(3750),
    })
  );
  await assertFails(
    bobDb.collection("chats").doc(groupId).update({
      disappearingHours: 24,
      updatedAt: pastDate(3700),
    })
  );
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).update({
      disappearingHours: 24,
      updatedAt: pastDate(3650),
    })
  );

  // 5. Admin Alice adds Charlie -> Charlie can immediately read older messages with orderBy("timestamp", "desc")
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).update({
      participants: [ALICE_UID, BOB_UID, CHARLIE_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob", [CHARLIE_UID]: "Charlie" },
      participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "", [CHARLIE_UID]: "" },
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0, [CHARLIE_UID]: 0 },
      updatedAt: pastDate(3500),
    })
  );
  await assertSucceeds(
    charlieDb.collection("chats").doc(groupId).collection("messages").orderBy("timestamp", "desc").limit(50).get()
  );

  // 6. System event message allowed even when onlyAdminsCanSend == true
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).update({
      onlyAdminsCanSend: true,
      updatedAt: pastDate(3400),
    })
  );
  await assertSucceeds(
    charlieDb.collection("chats").doc(groupId).collection("messages").doc("sys_leave_event").set({
      chatId: groupId,
      senderId: CHARLIE_UID,
      senderName: "Charlie",
      participants: [ALICE_UID, BOB_UID, CHARLIE_UID],
      text: "Charlie গ্রুপ ত্যাগ করেছেন",
      type: "system",
      delivered: true,
      read: true,
      deleted: false,
      timestamp: pastDate(3300),
      updatedAt: pastDate(3300),
    })
  );

  // 7. Group Admin Alice can soft-delete Bob's message for everyone; non-admin Charlie cannot
  await assertFails(
    charlieDb.collection("chats").doc(groupId).collection("messages").doc("bob_old_msg").update({
      deleted: true,
      text: "",
      updatedAt: pastDate(3200),
    })
  );
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).collection("messages").doc("bob_old_msg").update({
      deleted: true,
      text: "",
      updatedAt: pastDate(3100),
    })
  );

  // 8. Admin Alice removes Charlie -> Charlie immediately loses read access to messages
  await assertSucceeds(
    aliceDb.collection("chats").doc(groupId).update({
      participants: [ALICE_UID, BOB_UID],
      adminUids: [ALICE_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "" },
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 0 },
      updatedAt: pastDate(3000),
    })
  );
  await assertFails(
    charlieDb.collection("chats").doc(groupId).collection("messages").orderBy("timestamp", "desc").limit(50).get()
  );

  // 9. Non-admin Bob CANNOT delete another member's message or the group; Admin Alice CAN delete all messages + entire group
  await assertFails(bobDb.collection("chats").doc(groupId).collection("messages").doc("sys_leave_event").delete());
  await assertFails(bobDb.collection("chats").doc(groupId).delete());
  await assertSucceeds(aliceDb.collection("chats").doc(groupId).collection("messages").doc("bob_old_msg").delete());
  await assertSucceeds(aliceDb.collection("chats").doc(groupId).collection("messages").doc("sys_leave_event").delete());
  await assertSucceeds(aliceDb.collection("chats").doc(groupId).delete());

  // 10. Per-user Clear Chat History & Delete Chat (hiddenBy + clearedAt) on 1-on-1 chat
  await assertSucceeds(aliceDb.collection("chats").doc(CHAT_AB).set(validChat({ hiddenBy: [] })));

  // Alice clears history & deletes chat for herself only
  const clearTs = pastDate(2000);
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).update({
      hiddenBy: [ALICE_UID],
      clearedAt: { [ALICE_UID]: clearTs },
      pinnedBy: [],
      archivedBy: [],
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      updatedAt: clearTs,
    })
  );

  // Bob CANNOT tamper with Alice's clearedAt entry
  await assertFails(
    bobDb.collection("chats").doc(CHAT_AB).update({
      clearedAt: { [ALICE_UID]: pastDate(500), [BOB_UID]: pastDate(500) },
      updatedAt: pastDate(500),
    })
  );

  // Bob CAN set his own clearedAt while preserving Alice's entry
  await assertSucceeds(
    bobDb.collection("chats").doc(CHAT_AB).update({
      clearedAt: { [ALICE_UID]: clearTs, [BOB_UID]: pastDate(1000) },
      updatedAt: pastDate(1000),
    })
  );

  // Sending a new message resets hiddenBy to [] so chat reappears in Home list
  await assertSucceeds(
    bobDb.collection("chats").doc(CHAT_AB).update({
      lastMessage: "আবার কথা বলি!",
      lastMessageType: "text",
      lastSenderId: BOB_UID,
      lastMessageTime: pastDate(500),
      lastMessageDelivered: false,
      lastMessageRead: false,
      unreadCounts: { [ALICE_UID]: 1, [BOB_UID]: 0 },
      typingUids: [],
      hiddenBy: [],
      participantNames: { [ALICE_UID]: "Tania Rahman", [BOB_UID]: "Riyad Hasan" },
      participantPhotos: { [ALICE_UID]: "", [BOB_UID]: "" },
      updatedAt: pastDate(500),
    })
  );
});

test("v1.9 Live Force-Update config (appConfig/appUpdate): Super Admin write-only, signed-in users read, strict schema", async () => {
  const adminDb = testEnv.authenticatedContext("super_admin_uid", {
    email: "mehedi0169019@gmail.com",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const validUpdatePayload = {
    active: true,
    latestVersionCode: 10,
    latestVersionName: "2.0",
    forceUpdate: true,
    apkUrl: "https://github.com/ridoy013393-tech/Bangla-/releases/download/latest-apk/BanglaChat.apk",
    releaseUrl: "https://github.com/ridoy013393-tech/Bangla-/releases/latest",
    releaseNotes: "Bangla Chat v2.0 — বাধ্যতামূলক আপডেট",
    updatedBy: "mehedi0169019@gmail.com",
    updatedAt: pastDate(1000),
  };

  // Regular user & unauthenticated user CANNOT write appUpdate config
  await assertFails(aliceDb.collection("appConfig").doc("appUpdate").set(validUpdatePayload));
  await assertFails(unauthDb.collection("appConfig").doc("appUpdate").set(validUpdatePayload));

  // Super Admin CAN publish appUpdate config
  await assertSucceeds(adminDb.collection("appConfig").doc("appUpdate").set(validUpdatePayload));

  // Signed-in user CAN read appUpdate config; unauthenticated CANNOT
  await assertSucceeds(aliceDb.collection("appConfig").doc("appUpdate").get());
  await assertFails(unauthDb.collection("appConfig").doc("appUpdate").get());

  // Invalid versionCode (0) or missing apkUrl rejected even for Super Admin
  await assertFails(
    adminDb.collection("appConfig").doc("appUpdate").set({
      ...validUpdatePayload,
      latestVersionCode: 0,
    })
  );
  await assertFails(
    adminDb.collection("appConfig").doc("appUpdate").set({
      ...validUpdatePayload,
      apkUrl: "bad",
    })
  );
});

test("v2.1 features: Saved Messages owner-only self-chat, Daily Streaks, Stickers, Location & Contact cards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(3000);

  // 1. Saved Messages self-chat (saved_<uid>) — owner-only creation & strict isolation
  const savedChatId = `saved_${ALICE_UID}`;
  const validSavedChat = {
    participants: [ALICE_UID],
    participantNames: { [ALICE_UID]: "Saved Messages" },
    participantPhotos: { [ALICE_UID]: "" },
    isGroup: false,
    isSelfChat: true,
    lastMessage: "",
    lastMessageType: "text",
    lastSenderId: ALICE_UID,
    lastMessageTime: now,
    unreadCounts: { [ALICE_UID]: 0 },
    typingUids: [],
    createdBy: ALICE_UID,
    createdAt: now,
    updatedAt: now,
  };

  // Bob CANNOT create Alice's saved messages chat
  await assertFails(bobDb.collection("chats").doc(savedChatId).set(validSavedChat));

  // Alice CANNOT create a single-participant chat without isSelfChat: true & saved_<uid> doc ID
  await assertFails(
    aliceDb.collection("chats").doc("random_solo_chat").set({
      ...validSavedChat,
      isSelfChat: false,
    })
  );

  // Alice CAN create her own saved messages self-chat
  await assertSucceeds(aliceDb.collection("chats").doc(savedChatId).set(validSavedChat));

  // Alice CAN save a note in her self-chat
  await assertSucceeds(
    aliceDb.collection("chats").doc(savedChatId).collection("messages").doc("note_1").set({
      chatId: savedChatId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participants: [ALICE_UID],
      text: "বাজারের তালিকা: চাল, ডাল",
      type: "text",
      delivered: true,
      read: true,
      deleted: false,
      timestamp: pastDate(2500),
      updatedAt: pastDate(2500),
    })
  );

  // Bob CANNOT read or write Alice's saved messages chat or its messages
  await assertFails(bobDb.collection("chats").doc(savedChatId).get());
  await assertFails(bobDb.collection("chats").doc(savedChatId).collection("messages").doc("note_1").get());
  await assertFails(
    bobDb.collection("chats").doc(savedChatId).collection("messages").doc("intruder").set({
      chatId: savedChatId,
      senderId: BOB_UID,
      senderName: "Bob",
      participants: [ALICE_UID],
      text: "Sneaking in",
      type: "text",
      delivered: false,
      read: false,
      deleted: false,
      timestamp: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // 2. Regular chat: Sticker, Location, Contact messages + Daily Chat Streak updates
  await assertSucceeds(aliceDb.collection("chats").doc(CHAT_AB).set(validChat()));

  // Sticker message
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_sticker_1").set({
      ...validMessage({ senderId: ALICE_UID }),
      type: "sticker",
      text: "🔥 খেলা হবে!",
      stickerId: "khela_hobe",
    })
  );

  // Valid Location message
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_loc_1").set({
      ...validMessage({ senderId: ALICE_UID }),
      type: "location",
      text: "📍 মিরপুর ১০, ঢাকা",
      latitude: 23.8069,
      longitude: 90.3687,
      locationLabel: "মিরপুর ১০, ঢাকা",
    })
  );

  // Invalid Latitude (> 90) must FAIL
  await assertFails(
    aliceDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_loc_bad").set({
      ...validMessage({ senderId: ALICE_UID }),
      type: "location",
      text: "📍 Bad Coords",
      latitude: 120.5,
      longitude: 90.3687,
      locationLabel: "Invalid",
    })
  );

  // Valid Contact Card message
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).collection("messages").doc("msg_contact_1").set({
      ...validMessage({ senderId: ALICE_UID }),
      type: "contact",
      text: "👤 Hasan (@hasan99)",
      sharedContactUid: CHARLIE_UID,
      sharedContactName: "Hasan",
      sharedContactUsername: "hasan99",
      sharedContactPhotoUrl: "",
    })
  );

  // Daily Chat Streak update on chat doc succeeds
  await assertSucceeds(
    aliceDb.collection("chats").doc(CHAT_AB).update({
      lastMessage: "🔥 খেলা হবে!",
      lastMessageType: "sticker",
      lastSenderId: ALICE_UID,
      lastMessageTime: pastDate(1500),
      lastMessageDelivered: false,
      lastMessageRead: false,
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      typingUids: [],
      hiddenBy: [],
      streakCount: 3,
      lastStreakDate: "2026-11-21",
      updatedAt: pastDate(1500),
    })
  );

  // Negative streakCount must FAIL
  await assertFails(
    aliceDb.collection("chats").doc(CHAT_AB).update({
      lastMessage: "Bad streak",
      lastMessageType: "text",
      lastSenderId: ALICE_UID,
      lastMessageTime: pastDate(1000),
      streakCount: -1,
      lastStreakDate: "2026-11-21",
      updatedAt: pastDate(1000),
    })
  );
});



