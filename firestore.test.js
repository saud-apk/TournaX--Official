const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ALICE_FF_UID = "1234567890";
const BOB_FF_UID = "9876543210";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 1000);
}

function validProfile(uid, ffUid = ALICE_FF_UID) {
  const now = nowTs();
  return {
    userId: uid,
    fullName: "Aarav Sharma",
    phone: "9876543210",
    freeFireUid: ffUid,
    freeFireIgn: "TX_Aarav",
    termsAccepted: true,
    termsVersion: "2026-v1",
    verificationMethod: "google_auth",
    createdAt: now,
    updatedAt: now,
  };
}

function validTournament() {
  return {
    title: "Bermuda Pro Squad Cup",
    mode: "BR Squad",
    mapName: "Bermuda",
    dateTimeLabel: "Today • 8:00 PM IST",
    status: "upcoming",
    entryFee: 10,
    prizePool: 500,
    perKill: 10,
    prizeFirst: 300,
    prizeSecond: 120,
    prizeThird: 80,
    maxSlots: 12,
    filledSlots: 4,
    slotUnitLabel: "Teams",
    rulesSummary: "Mobile only. No emulators or hacks.",
    roomId: "88442190",
    roomPassword: "TX99",
    roomUnlocked: false,
    createdAt: nowTs(),
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

test("Unauthenticated user: cannot read or write users, uidLocks, tournaments, or leaderboard", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("tournaments").get());
  await assertFails(unauthDb.collection("leaderboard").get());
  await assertFails(unauthDb.collection("uidLocks").doc(ALICE_FF_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
});

test("Owner can create structured profile at 0 points, log +1/+2 spin points directly into walletDeposit, while inflated points/wallet and UID edits are blocked", async () => {
  const { doc, updateDoc, serverTimestamp } = require("/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules/@firebase/firestore");
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceUserRef = doc(aliceDb._delegate, "users", ALICE_UID);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfile(ALICE_UID),
      points: 250,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfile(ALICE_UID),
      points: 0,
      walletDeposit: 0,
      xp: 0,
      level: 1,
    })
  );
  await assertSucceeds(
    updateDoc(aliceUserRef, {
      walletDeposit: 2,
      lastDailySpinDate: "2026-11-13",
      lastDailySpinAt: serverTimestamp(),
      squadName: "Team Obsidian",
      squadTag: "OBS",
      squadUids: ["1234567890", "2234567890"],
      updatedAt: nowTs(),
    })
  );
  // Second spin within 24 hours MUST fail even on a different calendar date
  await assertFails(
    updateDoc(aliceUserRef, {
      walletDeposit: 4,
      lastDailySpinDate: "2026-11-14",
      lastDailySpinAt: serverTimestamp(),
      updatedAt: nowTs(),
    })
  );
  // Backdating lastDailySpinAt to bypass 24h cooldown MUST fail
  await assertFails(
    updateDoc(aliceUserRef, {
      walletDeposit: 4,
      lastDailySpinDate: "2026-11-14",
      lastDailySpinAt: new Date(Date.now() - 25 * 3600 * 1000),
      updatedAt: nowTs(),
    })
  );
  // Simulate 25 hours passing via admin context -> next spin with serverTimestamp MUST succeed
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).update({
      lastDailySpinAt: new Date(Date.now() - 25 * 3600 * 1000),
    });
  });
  await assertSucceeds(
    updateDoc(aliceUserRef, {
      walletDeposit: 4,
      lastDailySpinDate: "2026-11-14",
      lastDailySpinAt: serverTimestamp(),
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      points: 50,
      updatedAt: nowTs(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("spinHistory").doc("spin_1").set({
      userId: ALICE_UID,
      dateKey: "2026-11-13",
      outcomeLabel: "2 Points",
      pointsWon: 2,
      isRetry: false,
      createdAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("spinHistory").doc("spin_bad").set({
      userId: ALICE_UID,
      dateKey: "2026-11-13",
      outcomeLabel: "5 Points",
      pointsWon: 5,
      isRetry: false,
      createdAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      freeFireUid: "9999999999",
      updatedAt: nowTs(),
    })
  );
  // Real leaderboard: Alice can sync ONLY her own UID doc matching her /users stats; fake rows & kill inflation are blocked
  await assertSucceeds(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      ign: "TX_Aarav",
      kills: 0,
      booyahs: 0,
      winningsInr: 0,
      avatarIndex: 0,
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("leaderboard").doc("lb_01").set({
      ign: "Fake_Bot",
      kills: 999,
      booyahs: 50,
      winningsInr: 5000,
      avatarIndex: 0,
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("leaderboard").doc(ALICE_UID).set({
      ign: "TX_Aarav",
      kills: 50,
      booyahs: 5,
      winningsInr: 500,
      avatarIndex: 0,
      updatedAt: nowTs(),
    })
  );
});

test("Owner cannot read or write another user's profile, registrations, or notifications", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .set(validProfile(BOB_UID, BOB_FF_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("registrations").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("notifications").get()
  );
});

test("Tournaments allow atomic filledSlots +1 increment, but block client creation and prize/entry tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tRef = aliceDb.collection("tournaments").doc("tourn_1");

  // Clients MUST NOT be able to create fake/demo tournaments
  await assertFails(tRef.set(validTournament()));

  // Admin publishes real tournament
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("tournaments").doc("tourn_1").set(validTournament());
  });

  await assertSucceeds(tRef.update({ filledSlots: 5 }));
  await assertFails(tRef.update({ filledSlots: 9 }));
  await assertFails(tRef.update({ prizePool: 99999 }));
});

test("Owner can register for tournament and submit result screenshot kills", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const regRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("registrations")
    .doc("tourn_1");

  await assertSucceeds(
    regRef.set({
      userId: ALICE_UID,
      tournamentId: "tourn_1",
      tournamentTitle: "Bermuda Pro Squad Cup",
      mode: "BR Squad",
      mapName: "Bermuda",
      dateTimeLabel: "Today • 8:00 PM IST",
      slotNumber: 5,
      captainUid: ALICE_FF_UID,
      captainIgn: "TX_Aarav",
      teammateUids: ["2234567890"],
      screenshotSubmitted: false,
      submittedKills: 0,
      screenshotUri: "",
      resultStatus: "Pending",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    regRef.update({
      screenshotSubmitted: true,
      submittedKills: 7,
      screenshotUri: "content://media/picker/123",
      resultStatus: "Submitted for Review",
      updatedAt: nowTs(),
    })
  );
});

test("uidLocks enforces one account per Free Fire UID and blocks list queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("uidLocks").doc(ALICE_FF_UID).set({
      userId: ALICE_UID,
      createdAt: nowTs(),
    })
  );
  await assertFails(
    bobDb.collection("uidLocks").doc(ALICE_FF_UID).set({
      userId: BOB_UID,
      createdAt: nowTs(),
    })
  );
  await assertFails(bobDb.collection("uidLocks").get());
});

test("adminNotifications are read-only for signed-in users, blocked for clients on writes, and read receipts are owner-only", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("adminNotifications").doc("admin_msg_1").set({
      title: "Match Room Unlocked",
      body: "Bermuda Pro Squad Showdown Room ID is now live.",
      category: "match",
      priority: "high",
      targetUserId: "all",
      createdAt: nowTs(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Unauthenticated cannot read or write adminNotifications
  await assertFails(unauthDb.collection("adminNotifications").get());
  await assertFails(unauthDb.collection("adminNotifications").doc("admin_msg_1").get());

  // Signed-in user can read/list adminNotifications
  await assertSucceeds(aliceDb.collection("adminNotifications").doc("admin_msg_1").get());
  await assertSucceeds(aliceDb.collection("adminNotifications").get());

  // Client create/update/delete on adminNotifications MUST fail (Admin Panel only)
  await assertFails(
    aliceDb.collection("adminNotifications").doc("forged_1").set({
      title: "Fake Admin Broadcast",
      body: "Free diamonds",
      createdAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("adminNotifications").doc("admin_msg_1").update({ title: "Hijacked" })
  );
  await assertFails(
    aliceDb.collection("adminNotifications").doc("admin_msg_1").delete()
  );

  // Per-user read receipt in readAdminNotifications is owner-only
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("readAdminNotifications")
      .doc("admin_msg_1")
      .set({
        notifId: "admin_msg_1",
        readAt: nowTs(),
      })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("readAdminNotifications")
      .doc("admin_msg_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("readAdminNotifications")
      .doc("admin_msg_2")
      .set({
        notifId: "admin_msg_2",
        readAt: nowTs(),
      })
  );
});

test("UPI deposit requests enforce 10..500 bounds, 7378633612@fam, unique UTR locks, and atomic admin-approved wallet crediting", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfile(ALICE_UID),
      points: 0,
      xp: 0,
      walletDeposit: 0,
      walletWinning: 0,
      lastApprovedUtr: "",
    })
  );

  // Client cannot directly forge walletDeposit or walletWinning
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletDeposit: 500,
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletWinning: 500,
      updatedAt: nowTs(),
    })
  );

  const utr = "412345678901";

  // Invalid bounds (<10 or >500) or wrong UPI ID or non-pending status must fail
  await assertFails(
    aliceDb.collection("depositRequests").doc("412345678902").set({
      userId: ALICE_UID,
      utr: "412345678902",
      upiId: "7378633612@fam",
      amount: 9,
      status: "pending",
      credited: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("depositRequests").doc("412345678903").set({
      userId: ALICE_UID,
      utr: "412345678903",
      upiId: "7378633612@fam",
      amount: 501,
      status: "pending",
      credited: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("depositRequests").doc("412345678904").set({
      userId: ALICE_UID,
      utr: "412345678904",
      upiId: "other@upi",
      amount: 100,
      status: "pending",
      credited: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("depositRequests").doc("412345678905").set({
      userId: ALICE_UID,
      utr: "412345678905",
      upiId: "7378633612@fam",
      amount: 100,
      status: "approved",
      credited: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Valid atomic batch submit for utrLocks/{utr} + depositRequests/{utr}
  const batch1 = aliceDb.batch();
  batch1.set(aliceDb.collection("utrLocks").doc(utr), {
    userId: ALICE_UID,
    createdAt: nowTs(),
  });
  batch1.set(aliceDb.collection("depositRequests").doc(utr), {
    userId: ALICE_UID,
    utr,
    upiId: "7378633612@fam",
    amount: 150,
    status: "pending",
    credited: false,
    userIgn: "TX_Aarav",
    userPhone: "9876543210",
    userEmail: "alice@test.com",
    adminNote: "",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });
  await assertSucceeds(batch1.commit());

  // Duplicate UTR submission by same user OR another user MUST fail
  await assertFails(
    aliceDb.collection("utrLocks").doc(utr).set({
      userId: ALICE_UID,
      createdAt: nowTs(),
    })
  );
  await assertFails(
    bobDb.collection("utrLocks").doc(utr).set({
      userId: BOB_UID,
      createdAt: nowTs(),
    })
  );
  await assertFails(
    bobDb.collection("depositRequests").doc(utr).set({
      userId: BOB_UID,
      utr,
      upiId: "7378633612@fam",
      amount: 150,
      status: "pending",
      credited: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Bob cannot read Alice's deposit request or list depositRequests for Alice
  await assertFails(bobDb.collection("depositRequests").doc(utr).get());
  await assertFails(bobDb.collection("depositRequests").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("depositRequests").where("userId", "==", ALICE_UID).get());

  // Cannot claim while still pending
  await assertFails(
    aliceDb.collection("depositRequests").doc(utr).update({
      credited: true,
      updatedAt: nowTs(),
    })
  );

  // Admin approves the deposit via Admin SDK / Firebase Console (rules bypassed)
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("depositRequests").doc(utr).update({
      status: "approved",
      updatedAt: nowTs(),
    });
  });

  // Malicious partial update on users/{uid}.walletDeposit WITHOUT flipping credited=true MUST fail (getAfter check)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletDeposit: 150,
      lastApprovedUtr: utr,
      updatedAt: nowTs(),
    })
  );

  // Atomic claim batch (depositRequests/{utr}.credited = true + users/{uid}.walletDeposit += 150) SUCCEEDS
  const claimBatch = aliceDb.batch();
  claimBatch.update(aliceDb.collection("depositRequests").doc(utr), {
    credited: true,
    updatedAt: nowTs(),
  });
  claimBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    walletDeposit: 150,
    lastApprovedUtr: utr,
    updatedAt: nowTs(),
  });
  await assertSucceeds(claimBatch.commit());

  // Re-claiming the same approved UTR a second time MUST fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletDeposit: 300,
      lastApprovedUtr: utr,
      updatedAt: nowTs(),
    })
  );

  // Joining a paid tournament (25 Pts entry fee) MAY decrement walletDeposit (150 -> 125)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletDeposit: 125,
      updatedAt: nowTs(),
    })
  );

  // Client still CANNOT increase walletWinning directly
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletWinning: 200,
      updatedAt: nowTs(),
    })
  );
});

test("Withdrawal requests enforce min 50 Pts, selfie payload, atomic walletWinning deduction, and owner-only privacy", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const fakeSelfie = "A".repeat(250);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...validProfile(ALICE_UID),
      points: 0,
      xp: 0,
      walletDeposit: 0,
      walletWinning: 0,
    })
  );

  // Admin credits ₹120 winning balance
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).update({
      walletWinning: 120,
    });
  });

  // 1. Unauthenticated cannot read or write withdrawalRequests
  await assertFails(unauthDb.collection("withdrawalRequests").get());

  // 2. Less than 50 Pts MUST fail even with atomic deduction
  const lowBatch = aliceDb.batch();
  lowBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    walletWinning: 71,
    updatedAt: nowTs(),
  });
  lowBatch.set(aliceDb.collection("withdrawalRequests").doc("wd_low_1"), {
    userId: ALICE_UID,
    fullName: "Aarav Sharma",
    freeFireUid: ALICE_FF_UID,
    freeFireIgn: "TX_Aarav",
    userPhone: "9876543210",
    userEmail: "alice@test.com",
    upiId: "aarav@upi",
    amount: 49,
    selfieBase64: fakeSelfie,
    status: "pending",
    adminNote: "",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });
  await assertFails(lowBatch.commit());

  // 3. Missing/too short selfie MUST fail
  const noSelfieBatch = aliceDb.batch();
  noSelfieBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    walletWinning: 70,
    updatedAt: nowTs(),
  });
  noSelfieBatch.set(aliceDb.collection("withdrawalRequests").doc("wd_noselfie_1"), {
    userId: ALICE_UID,
    fullName: "Aarav Sharma",
    freeFireUid: ALICE_FF_UID,
    upiId: "aarav@upi",
    amount: 50,
    selfieBase64: "short",
    status: "pending",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });
  await assertFails(noSelfieBatch.commit());

  // 4. Forged withdrawal WITHOUT deducting walletWinning MUST fail (getAfter guard)
  await assertFails(
    aliceDb.collection("withdrawalRequests").doc("wd_forged_1").set({
      userId: ALICE_UID,
      fullName: "Aarav Sharma",
      freeFireUid: ALICE_FF_UID,
      upiId: "aarav@upi",
      amount: 50,
      selfieBase64: fakeSelfie,
      status: "pending",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // 5. Valid atomic withdrawal of ₹70 succeeds (120 -> 50)
  const validBatch = aliceDb.batch();
  validBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    walletWinning: 50,
    updatedAt: nowTs(),
  });
  validBatch.set(aliceDb.collection("withdrawalRequests").doc("wd_valid_1"), {
    userId: ALICE_UID,
    fullName: "Aarav Sharma",
    freeFireUid: ALICE_FF_UID,
    freeFireIgn: "TX_Aarav",
    userPhone: "9876543210",
    userEmail: "alice@test.com",
    upiId: "aarav@upi",
    amount: 70,
    selfieBase64: fakeSelfie,
    status: "pending",
    adminNote: "",
    createdAt: nowTs(),
    updatedAt: nowTs(),
  });
  await assertSucceeds(validBatch.commit());

  // 6. Owner can read/list own requests; Bob cannot read/list Alice's withdrawalRequests
  await assertSucceeds(
    aliceDb.collection("withdrawalRequests").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("withdrawalRequests").doc("wd_valid_1").get());
  await assertFails(
    bobDb.collection("withdrawalRequests").where("userId", "==", ALICE_UID).get()
  );

  // 7. Client cannot self-mark paid or delete withdrawalRequests
  await assertFails(
    aliceDb.collection("withdrawalRequests").doc("wd_valid_1").update({
      status: "paid",
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("withdrawalRequests").doc("wd_valid_1").delete()
  );
});

test("Tournament Points Table results are strictly Admin-only write and signed-in read", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("tournaments").doc("match_pts_1").set({
      ...validTournament(),
      status: "completed",
      resultsPublished: true,
      mvpIgn: "TX_Aarav",
    });
    await db
      .collection("tournaments")
      .doc("match_pts_1")
      .collection("results")
      .doc("slot_1")
      .set({
        tournamentId: "match_pts_1",
        rank: 1,
        playerOrTeamName: "Vipers",
        squadTag: "TX",
        freeFireUid: ALICE_FF_UID,
        userId: ALICE_UID,
        slotNumber: 1,
        booyahs: 1,
        placementPoints: 12,
        kills: 8,
        killPoints: 8,
        totalPoints: 20,
        prizeWon: 300,
        isMvp: true,
        updatedAt: nowTs(),
      });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Unauthenticated cannot read results
  await assertFails(
    unauthDb.collection("tournaments").doc("match_pts_1").collection("results").get()
  );

  // Signed-in players can read Points Table
  await assertSucceeds(
    aliceDb.collection("tournaments").doc("match_pts_1").collection("results").get()
  );
  await assertSucceeds(
    aliceDb.collection("tournaments").doc("match_pts_1").collection("results").doc("slot_1").get()
  );

  // Client create, update, delete MUST fail
  await assertFails(
    aliceDb.collection("tournaments").doc("match_pts_1").collection("results").doc("hacked").set({
      tournamentId: "match_pts_1",
      rank: 1,
      playerOrTeamName: "Hacker",
      placementPoints: 12,
      kills: 20,
      totalPoints: 32,
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("tournaments").doc("match_pts_1").collection("results").doc("slot_1").update({
      kills: 99,
    })
  );
  await assertFails(
    aliceDb.collection("tournaments").doc("match_pts_1").collection("results").doc("slot_1").delete()
  );
});

test("Profile owner can save fcmToken for direct push alerts while other users are blocked", async () => {
  const { doc, updateDoc, serverTimestamp } = require("/usr/local/lib/node_modules/@firebase/rules-unit-testing/node_modules/@firebase/firestore");
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID))
  );
  await assertSucceeds(
    updateDoc(doc(aliceDb._delegate, "users", ALICE_UID), {
      fcmToken: "fcm_test_token_abc123",
      updatedAt: serverTimestamp(),
    })
  );
  await assertFails(
    updateDoc(doc(bobDb._delegate, "users", ALICE_UID), {
      fcmToken: "hijacked_token",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Tournament participants roster allows signed-in read and owner-only create with valid FF UID, blocking spoofing and edits", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const participantPayload = {
    userId: ALICE_UID,
    tournamentId: "match_roster_1",
    slotNumber: 1,
    captainIgn: "TX_Aarav",
    captainUid: ALICE_FF_UID,
    squadTag: "TX",
    avatarIndex: 0,
    teammateUids: ["1122334455"],
    joinedAt: nowTs(),
  };

  // Owner can register their own participant roster doc
  await assertSucceeds(
    aliceDb
      .collection("tournaments")
      .doc("match_roster_1")
      .collection("participants")
      .doc(ALICE_UID)
      .set(participantPayload)
  );

  // Other signed-in players (Bob) can read real joined members list
  await assertSucceeds(
    bobDb.collection("tournaments").doc("match_roster_1").collection("participants").get()
  );

  // Unauthenticated cannot read or write
  await assertFails(
    unauthDb.collection("tournaments").doc("match_roster_1").collection("participants").get()
  );

  // Bob cannot spoof Alice's participant slot or overwrite/delete it
  await assertFails(
    bobDb
      .collection("tournaments")
      .doc("match_roster_1")
      .collection("participants")
      .doc(ALICE_UID)
      .set({ ...participantPayload, userId: BOB_UID })
  );
  await assertFails(
    aliceDb
      .collection("tournaments")
      .doc("match_roster_1")
      .collection("participants")
      .doc(ALICE_UID)
      .update({ slotNumber: 99 })
  );
  await assertFails(
    aliceDb
      .collection("tournaments")
      .doc("match_roster_1")
      .collection("participants")
      .doc(ALICE_UID)
      .delete()
  );
});

test("Admin-managed home banners allow signed-in reads and block client writes", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("banners").doc("promo_1").set({
      title: "Sunday Mega Cup",
      imageUrl: "https://example.com/banner1.png",
      linkUrl: "https://t.me/tournax_official",
      order: 1,
      active: true,
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(unauthDb.collection("banners").get());
  await assertSucceeds(aliceDb.collection("banners").get());
  await assertSucceeds(aliceDb.collection("banners").doc("promo_1").get());

  await assertFails(
    aliceDb.collection("banners").doc("hacked").set({
      imageUrl: "https://evil.com/x.png",
      linkUrl: "https://evil.com",
    })
  );
  await assertFails(
    aliceDb.collection("banners").doc("promo_1").update({ active: false })
  );
  await assertFails(
    aliceDb.collection("banners").doc("promo_1").delete()
  );
});

test("Admin-only ban and timed suspension lock writes, UID unlock, and deletion while preserving owner reads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  // Client CANNOT create a profile pre-populated as banned or suspended
  await assertFails(
    aliceRef.set({
      ...validProfile(ALICE_UID),
      accountStatus: "banned",
      banReason: "Forcing self ban",
    })
  );

  // Clean registration succeeds
  await assertSucceeds(
    aliceRef.set({
      ...validProfile(ALICE_UID),
      walletDeposit: 0,
      walletWinning: 0,
      accountStatus: "active",
      banReason: "",
    })
  );
  await assertSucceeds(
    aliceDb.collection("uidLocks").doc(ALICE_FF_UID).set({
      userId: ALICE_UID,
      createdAt: nowTs(),
    })
  );

  // Client CANNOT self-ban or tamper moderation fields
  await assertFails(
    aliceRef.update({
      accountStatus: "banned",
      updatedAt: nowTs(),
    })
  );

  // Admin permanently bans Alice via Admin SDK / Firebase Console
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      accountStatus: "banned",
      banReason: "Confirmed Auto-Aim ESP Hack",
      bannedAt: nowTs(),
      bannedBy: "admin_panel",
    });
  });

  // Owner CAN still read their own profile so the app shows ban reason & recovers on unban
  await assertSucceeds(aliceRef.get());

  // Banned user CANNOT self-unban, edit IGN, delete profile, or release Free Fire UID lock
  await assertFails(aliceRef.update({ accountStatus: "active", updatedAt: nowTs() }));
  await assertFails(aliceRef.update({ freeFireIgn: "BypassIGN", updatedAt: nowTs() }));
  await assertFails(aliceRef.delete());
  await assertFails(aliceDb.collection("uidLocks").doc(ALICE_FF_UID).delete());

  // Banned user CANNOT join matches, spin wheel, or submit deposits
  await assertFails(
    aliceRef.collection("registrations").doc("tourn_1").set({
      userId: ALICE_UID,
      tournamentId: "tourn_1",
      tournamentTitle: "Bermuda Cup",
      mode: "BR Solo",
      mapName: "Bermuda",
      dateTimeLabel: "Today • 8:00 PM IST",
      slotNumber: 1,
      captainUid: ALICE_FF_UID,
      captainIgn: "TX_Aarav",
      teammateUids: [],
      screenshotSubmitted: false,
      submittedKills: 0,
      screenshotUri: "",
      resultStatus: "Pending",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceRef.collection("spinHistory").doc("spin_ban_1").set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      outcomeLabel: "2 Points",
      pointsWon: 2,
      isRetry: false,
      createdAt: nowTs(),
    })
  );
  await assertFails(
    aliceDb.collection("utrLocks").doc("998877665544").set({
      userId: ALICE_UID,
      createdAt: nowTs(),
    })
  );

  // Admin switches to timed suspension (2 hours in future) -> writes still blocked
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      accountStatus: "suspended",
      banReason: "Temporary 2h Teaming Review",
      suspendedUntil: new Date(Date.now() + 2 * 3600 * 1000),
    });
  });
  await assertFails(
    aliceRef.update({ freeFireIgn: "SuspendedEdit", updatedAt: nowTs() })
  );

  // Once suspendedUntil timestamp has elapsed -> writes automatically unlock!
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      suspendedUntil: new Date(Date.now() - 60 * 1000),
    });
  });
  await assertSucceeds(
    aliceRef.update({ freeFireIgn: "TX_Aarav_Unlocked", updatedAt: nowTs() })
  );
});




