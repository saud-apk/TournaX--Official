package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class ArenaRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun seedArenaDataAndJoinTournament_authenticatedUser_assignsSlotAndUpdatesRegistration() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("arena_alice_$uniqueSuffix@tournax.test")
        val ffUid = "91$uniqueSuffix"

        val playerRepo = PlayerRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            playerRepo.registerProfile(
                userId = aliceUid,
                fullName = "Arjun Nair",
                phone = "9876543210",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Arjun",
            ).getOrThrow()
        }

        val initialProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            playerRepo.getProfile(aliceUid).getOrThrow()
        }
        assertEquals(0L, initialProfile?.points)
        assertEquals(0L, initialProfile?.xp)
        assertEquals(0L, initialProfile?.walletDeposit)
        assertEquals("Pending", initialProfile?.kycStatus)

        val arenaRepo = ArenaRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.syncOwnLeaderboardEntry(initialProfile!!)
        }
        val realBoard = withTimeout(FLOW_TIMEOUT_MS) {
            arenaRepo.observeLeaderboard().first { list -> list.any { it.id == aliceUid } }
        }
        assertTrue("Leaderboard must NOT contain fake demo rows", realBoard.none { it.id.startsWith("lb_") })

        // Client attempt to create a fake/demo tournament MUST be rejected by rules
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(ArenaRepository.TOURNAMENTS_COLLECTION)
                    .document("fake_tourn_$uniqueSuffix")
                    .set(
                        mapOf(
                            "title" to "Fake Client Match",
                            "mode" to "BR Solo",
                            "entryFee" to 10L,
                            "prizePool" to 500L,
                            "maxSlots" to 48L,
                            "filledSlots" to 0L,
                        )
                    )
                    .await()
            }
            fail("Expected client write to tournaments to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }

        // Seed a legacy/demo tournament and a real Admin-published BR Solo tournament on emulator
        val legacyDemoId = "tourn_demo_$uniqueSuffix"
        val flaggedDemoId = "real_flagged_demo_$uniqueSuffix"
        val brSoloId = "real_br_solo_$uniqueSuffix"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            seedTournamentOnEmulator(
                tournamentId = legacyDemoId,
                title = "Legacy Demo Clash",
                entryFee = 10L,
                prizePool = 500L,
                roomId = "88442190",
                roomPassword = "DEMO1",
            )
            seedTournamentOnEmulator(
                tournamentId = flaggedDemoId,
                title = "Flagged Demo Match",
                entryFee = 10L,
                prizePool = 500L,
                roomId = "99110011",
                roomPassword = "DEMO2",
                isDemo = true,
            )
            seedTournamentOnEmulator(
                tournamentId = brSoloId,
                title = "Bermuda BR Solo Cup",
                entryFee = 10L,
                prizePool = 500L,
                roomId = "99110022",
                roomPassword = "SOLO9",
            )
        }

        val tournaments = withTimeout(FLOW_TIMEOUT_MS) {
            arenaRepo.observeTournaments().first { list -> list.any { it.id == brSoloId } }
        }
        assertTrue(
            "observeTournaments must filter out legacy tourn_* demo docs",
            tournaments.none { it.id.startsWith("tourn_") || it.id == flaggedDemoId || it.isDemo }
        )
        val target = tournaments.first { it.id == brSoloId }

        // Joining a 10-Pt tournament with 0 Points MUST fail with Insufficient Points
        val insufficientJoin = withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.joinTournament(
                userId = aliceUid,
                tournament = target,
                captainUid = ffUid,
                captainIgn = "TX_Arjun",
                teammateUids = listOf("88776655"),
            )
        }
        assertTrue("Expected join with 0 Pts on 10-Pt match to fail", insufficientJoin.isFailure)

        // Seed a free tournament (0 Pts) and verify successful join + screenshot submission
        val freeMatchId = "real_br_free_$uniqueSuffix"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            seedTournamentOnEmulator(
                tournamentId = freeMatchId,
                title = "Bermuda Free Solo Clash",
                entryFee = 0L,
                prizePool = 100L,
                roomId = "99110033",
                roomPassword = "SOLO2",
            )
        }

        val updatedTournaments = withTimeout(FLOW_TIMEOUT_MS) {
            arenaRepo.observeTournaments().first { list -> list.any { it.id == freeMatchId } }
        }
        val freeTarget = updatedTournaments.first { it.id == freeMatchId }

        val joinResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.joinTournament(
                userId = aliceUid,
                tournament = freeTarget,
                captainUid = ffUid,
                captainIgn = "TX_Arjun",
                teammateUids = listOf("88776655"),
            )
        }
        assertTrue("Expected join success on free match, got ${joinResult.exceptionOrNull()}", joinResult.isSuccess)
        val assignedSlot = joinResult.getOrThrow()
        assertEquals(freeTarget.filledSlots + 1L, assignedSlot)

        val registrations = withTimeout(FLOW_TIMEOUT_MS) {
            arenaRepo.observeRegistrations(aliceUid).first { list ->
                list.any { it.tournamentId == freeTarget.id }
            }
        }
        val registration = registrations.first { it.tournamentId == freeTarget.id }
        assertEquals(assignedSlot, registration.slotNumber)
        assertEquals("TX_Arjun", registration.captainIgn)

        val proofResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.submitResultScreenshot(
                userId = aliceUid,
                tournamentId = freeTarget.id,
                kills = 7L,
                screenshotUri = "content://media/picker/0/com.android.providers.media.photopicker/media/101",
            )
        }
        assertTrue("Expected screenshot submission success, got ${proofResult.exceptionOrNull()}", proofResult.isSuccess)
    }

    @Test
    fun squadUpdate_authenticatedOwner_persistsSquadDetails() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("squad_$uniqueSuffix@tournax.test")
        val ffUid = "92$uniqueSuffix"

        val playerRepo = PlayerRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            playerRepo.registerProfile(
                userId = aliceUid,
                fullName = "Kabir Mehta",
                phone = "9812345678",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Kabir",
            ).getOrThrow()
        }

        val arenaRepo = ArenaRepository(firestore)
        val squadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.updateSquad(
                userId = aliceUid,
                squadName = "Obsidian Kings",
                squadTag = "TX",
                squadUids = listOf(ffUid, "12345678"),
            )
        }
        assertTrue("Expected squad update success, got ${squadResult.exceptionOrNull()}", squadResult.isSuccess)

        val updatedProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            playerRepo.getProfile(aliceUid).getOrThrow()
        }
        assertEquals("Obsidian Kings", updatedProfile?.squadName)
        assertEquals("TX", updatedProfile?.squadTag)
    }

    @Test
    fun observeRegistrations_crossUserAndUnauthenticated_failsWithPermissionDenied() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("iso_alice_$uniqueSuffix@tournax.test")
        signInTestUser("iso_bob_$uniqueSuffix@tournax.test")

        val arenaRepo = ArenaRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                arenaRepo.observeRegistrations(aliceUid).first()
            }
            fail("Expected cross-user registrations read to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }

        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                arenaRepo.observeTournaments().first()
            }
            fail("Expected unauthenticated observeTournaments to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun adminNotifications_startsEmptyForClient_rejectsClientWrite_andPersistsReadReceipt() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("notif_alice_$uniqueSuffix@tournax.test")
        val ffUid = "93$uniqueSuffix"

        val playerRepo = PlayerRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            playerRepo.registerProfile(
                userId = aliceUid,
                fullName = "Rohan Das",
                phone = "9876501234",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Rohan",
            ).getOrThrow()
        }

        val arenaRepo = ArenaRepository(firestore)
        val rohanProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            playerRepo.getProfile(aliceUid).getOrThrow()!!
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.syncOwnLeaderboardEntry(rohanProfile)
        }

        val initialNotifications = withTimeout(FLOW_TIMEOUT_MS) {
            arenaRepo.observeNotifications(aliceUid).first()
        }
        assertTrue(
            "Client seeding must NOT create fake notifications; expected empty list",
            initialNotifications.isEmpty()
        )

        // Attempted client write to adminNotifications MUST fail with PERMISSION_DENIED
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(ArenaRepository.ADMIN_NOTIFICATIONS_COLLECTION)
                    .document("client_forged_$uniqueSuffix")
                    .set(
                        mapOf(
                            "title" to "Fake",
                            "body" to "Client forged broadcast",
                        )
                    )
                    .await()
            }
            fail("Expected client write to adminNotifications to be rejected by rules")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }

        // Mark-read persists receipt under users/{uid}/readAdminNotifications/{notifId}
        withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.markNotificationRead(aliceUid, "admin_broadcast_$uniqueSuffix")
        }
    }

    @Test
    fun upiDepositRequest_enforcesBoundsAndDuplicateUtrLock_andBlocksClientWalletTampering() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("dep_alice_$uniqueSuffix@tournax.test")
        val ffUid = "94$uniqueSuffix"

        val playerRepo = PlayerRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            playerRepo.registerProfile(
                userId = aliceUid,
                fullName = "Vikram Singh",
                phone = "9876509876",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Vikram",
            ).getOrThrow()
        }

        val arenaRepo = ArenaRepository(firestore)

        // Amount out of 10..500 bounds must fail locally
        val tooLow = arenaRepo.submitDepositRequest(
            userId = aliceUid,
            amountInr = 5L,
            rawUtr = "412345$uniqueSuffix",
            userIgn = "TX_Vikram",
            userPhone = "9876509876",
            userEmail = "dep_alice@tournax.test",
        )
        assertTrue("Expected < ₹10 deposit to fail", tooLow.isFailure)

        val tooHigh = arenaRepo.submitDepositRequest(
            userId = aliceUid,
            amountInr = 501L,
            rawUtr = "412345$uniqueSuffix",
            userIgn = "TX_Vikram",
            userPhone = "9876509876",
            userEmail = "dep_alice@tournax.test",
        )
        assertTrue("Expected > ₹500 deposit to fail", tooHigh.isFailure)

        // Valid ₹100 deposit with 12-digit UTR must succeed and create pending request for 7378633612@fam
        val validUtr = "412345$uniqueSuffix"
        val submitOk = withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.submitDepositRequest(
                userId = aliceUid,
                amountInr = 100L,
                rawUtr = validUtr,
                userIgn = "TX_Vikram",
                userPhone = "9876509876",
                userEmail = "dep_alice@tournax.test",
            )
        }
        assertTrue("Expected valid UTR deposit submission to succeed, got ${submitOk.exceptionOrNull()}", submitOk.isSuccess)
        assertEquals(validUtr, submitOk.getOrThrow())

        val deposits = withTimeout(FLOW_TIMEOUT_MS) {
            arenaRepo.observeDepositRequests(aliceUid).first { list -> list.any { it.utr == validUtr } }
        }
        val createdDep = deposits.first { it.utr == validUtr }
        assertEquals("pending", createdDep.status)
        assertEquals(100L, createdDep.amount)
        assertEquals(ArenaRepository.MERCHANT_UPI_ID, createdDep.upiId)

        // Duplicate UTR by same user must fail
        val dupSameUser = withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.submitDepositRequest(
                userId = aliceUid,
                amountInr = 100L,
                rawUtr = validUtr,
                userIgn = "TX_Vikram",
                userPhone = "9876509876",
                userEmail = "dep_alice@tournax.test",
            )
        }
        assertTrue("Expected duplicate UTR by same user to fail", dupSameUser.isFailure)

        // Direct client walletDeposit tampering without Admin approval must fail with PERMISSION_DENIED
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection(PlayerRepository.USERS_COLLECTION)
                    .document(aliceUid)
                    .update("walletDeposit", 500L)
                    .await()
            }
            fail("Expected direct walletDeposit write to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }

        // Another user (Bob) trying the same UTR must fail AND cannot read Alice's depositRequests
        signInTestUser("dep_bob_$uniqueSuffix@tournax.test")
        val dupOtherUser = withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.submitDepositRequest(
                userId = auth.currentUser!!.uid,
                amountInr = 100L,
                rawUtr = validUtr,
                userIgn = "TX_Bob",
                userPhone = "9876501111",
                userEmail = "dep_bob@tournax.test",
            )
        }
        assertTrue("Expected duplicate UTR by Bob to fail", dupOtherUser.isFailure)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                arenaRepo.observeDepositRequests(aliceUid).first()
            }
            fail("Expected cross-user depositRequests query to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun withdrawalRequest_min50AndSelfie_submitsPendingAndDeductsWinningBalance() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("wd_alice_$uniqueSuffix@tournax.test")
        val ffUid = "95$uniqueSuffix"

        val playerRepo = PlayerRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            playerRepo.registerProfile(
                userId = aliceUid,
                fullName = "Siddharth Rao",
                phone = "9876512345",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Sid",
            ).getOrThrow()
            creditWinningOnEmulator(aliceUid, 150L)
        }

        val arenaRepo = ArenaRepository(firestore)
        val fakeSelfie = "S".repeat(250)

        // Below 50 Pts must fail
        val belowMin = withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.submitWithdrawalRequest(
                userId = aliceUid,
                fullName = "Siddharth Rao",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Sid",
                userPhone = "9876512345",
                userEmail = "wd_alice@tournax.test",
                upiId = "sid@upi",
                amountInr = 40L,
                selfieBase64 = fakeSelfie,
            )
        }
        assertTrue("Expected < 50 Pts withdrawal to fail", belowMin.isFailure)

        // Missing selfie must fail
        val noSelfie = withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.submitWithdrawalRequest(
                userId = aliceUid,
                fullName = "Siddharth Rao",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Sid",
                userPhone = "9876512345",
                userEmail = "wd_alice@tournax.test",
                upiId = "sid@upi",
                amountInr = 50L,
                selfieBase64 = "",
            )
        }
        assertTrue("Expected empty selfie withdrawal to fail", noSelfie.isFailure)

        // Valid ₹100 withdrawal with selfie succeeds and deducts walletWinning 150 -> 50
        val submitOk = withTimeout(DEFAULT_TIMEOUT_MS) {
            arenaRepo.submitWithdrawalRequest(
                userId = aliceUid,
                fullName = "Siddharth Rao",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Sid",
                userPhone = "9876512345",
                userEmail = "wd_alice@tournax.test",
                upiId = "sid@upi",
                amountInr = 100L,
                selfieBase64 = fakeSelfie,
            )
        }
        assertTrue("Expected valid withdrawal to succeed, got ${submitOk.exceptionOrNull()}", submitOk.isSuccess)
        val reqId = submitOk.getOrThrow()

        val updatedProfile = withTimeout(DEFAULT_TIMEOUT_MS) {
            playerRepo.getProfile(aliceUid).getOrThrow()
        }
        assertEquals(50L, updatedProfile?.walletWinning)

        val requests = withTimeout(FLOW_TIMEOUT_MS) {
            arenaRepo.observeWithdrawalRequests(aliceUid).first { list -> list.any { it.id == reqId } }
        }
        val savedReq = requests.first { it.id == reqId }
        assertEquals("pending", savedReq.status)
        assertEquals(100L, savedReq.amount)
        assertEquals(ffUid, savedReq.freeFireUid)
        assertEquals(fakeSelfie, savedReq.selfieBase64)

        val txs = withTimeout(FLOW_TIMEOUT_MS) {
            arenaRepo.observeTransactions(aliceUid).first { list -> list.any { it.id == "tx_$reqId" } }
        }
        assertEquals(100L, txs.first { it.id == "tx_$reqId" }.amount)

        // Bob cannot read Alice's withdrawalRequests
        signInTestUser("wd_bob_$uniqueSuffix@tournax.test")
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                arenaRepo.observeWithdrawalRequests(aliceUid).first()
            }
            fail("Expected cross-user withdrawalRequests query to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}

