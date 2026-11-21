package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class PlayerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun registerProfile_authenticatedOwner_createsProfileAndLocksUid() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("alice_$uniqueSuffix@tournax.test")
        val ffUid = "88$uniqueSuffix" // 8 digits

        val repository = PlayerRepository(firestore)
        val regResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerProfile(
                userId = aliceUid,
                fullName = "Aarav Sharma",
                phone = "9876543210",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Aarav",
            )
        }
        assertTrue("Expected registration success, got ${regResult.exceptionOrNull()}", regResult.isSuccess)

        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProfile(aliceUid).getOrThrow()
        }
        assertNotNull(fetched)
        assertEquals(aliceUid, fetched?.userId)
        assertEquals(ffUid, fetched?.freeFireUid)
        assertEquals("TX_Aarav", fetched?.freeFireIgn)
        assertTrue(fetched?.termsAccepted == true)
    }

    @Test
    fun observeProfile_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("observer_$uniqueSuffix@tournax.test")
        val ffUid = "77$uniqueSuffix"

        val repository = PlayerRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerProfile(
                userId = aliceUid,
                fullName = "Rohan Verma",
                phone = "9876501234",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Rohan",
            ).getOrThrow()
        }

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(aliceUid).first { it != null }
        }
        assertEquals("TX_Rohan", observed?.freeFireIgn)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateProfileDetails(
                userId = aliceUid,
                fullName = "Rohan Verma",
                phone = "9876501234",
                freeFireIgn = "TX_RohanOP",
            ).getOrThrow()
        }

        val updated = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(aliceUid).first { it?.freeFireIgn == "TX_RohanOP" }
        }
        assertEquals("TX_RohanOP", updated?.freeFireIgn)
    }

    @Test
    fun registerProfile_duplicateFreeFireUidByOtherUser_isBlocked() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("first_$uniqueSuffix@tournax.test")
        val ffUid = "66$uniqueSuffix"

        val repository = PlayerRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerProfile(
                userId = aliceUid,
                fullName = "Vikram Singh",
                phone = "9811122233",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Vikram",
            ).getOrThrow()
        }

        val bobUid = signInTestUser("second_$uniqueSuffix@tournax.test")
        val availableForBob = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.isFreeFireUidAvailable(ffUid, bobUid).getOrThrow()
        }
        assertFalse(availableForBob)

        val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerProfile(
                userId = bobUid,
                fullName = "Karan Patel",
                phone = "9822233344",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Karan",
            )
        }
        assertTrue(bobResult.isFailure)
    }

    @Test
    fun getProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("owner_$uniqueSuffix@tournax.test")
        val ffUid = "55$uniqueSuffix"

        val repository = PlayerRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerProfile(
                userId = aliceUid,
                fullName = "Nikhil Joshi",
                phone = "9833344455",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Nikhil",
            ).getOrThrow()
        }

        signInTestUser("intruder_$uniqueSuffix@tournax.test")
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getProfile(aliceUid)
        }
        assertTrue(crossResult.isFailure)
        val firestoreException = crossResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeProfile_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = PlayerRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeProfile("some_user_123").first()
            }
            fail("Expected unauthenticated observeProfile to fail with PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun bannedUser_observesRestriction_blocksProfileMutations_andRecoversWhenUnbanned() = runBlocking {
        val uniqueSuffix = Random.nextInt(100000, 999999).toString()
        val aliceUid = signInTestUser("banned_$uniqueSuffix@tournax.test")
        val ffUid = "44$uniqueSuffix"

        val repository = PlayerRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.registerProfile(
                userId = aliceUid,
                fullName = "Kabir Rao",
                phone = "9844455566",
                freeFireUid = ffUid,
                freeFireIgn = "TX_Kabir",
            ).getOrThrow()
        }

        // Admin bans the player on Firestore
        moderateUserOnEmulator(
            userId = aliceUid,
            accountStatus = "banned",
            banReason = "Confirmed Emulators / Hack Tool",
            bannedBy = "admin_panel",
        )

        val bannedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(aliceUid).first { it?.isBanned == true }
        }
        assertNotNull(bannedProfile)
        assertTrue(bannedProfile!!.isRestricted())
        assertEquals("Confirmed Emulators / Hack Tool", bannedProfile.banReason)

        // Banned user cannot mutate profile
        val blockedEdit = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateProfileDetails(
                userId = aliceUid,
                fullName = "Kabir Rao",
                phone = "9844455566",
                freeFireIgn = "TX_Bypass",
            )
        }
        assertTrue(blockedEdit.isFailure)
        val firestoreException = blockedEdit.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)

        // Admin unbans the player -> profile emits active state & edits work again
        moderateUserOnEmulator(
            userId = aliceUid,
            accountStatus = "active",
            banReason = "",
            bannedBy = "admin_panel",
        )

        val restoredProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeProfile(aliceUid).first { it != null && !it.isRestricted() }
        }
        assertFalse(restoredProfile!!.isBanned)

        val restoredEdit = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateProfileDetails(
                userId = aliceUid,
                fullName = "Kabir Rao",
                phone = "9844455566",
                freeFireIgn = "TX_KabirClean",
            )
        }
        assertTrue("Expected unbanned update success, got ${restoredEdit.exceptionOrNull()}", restoredEdit.isSuccess)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
