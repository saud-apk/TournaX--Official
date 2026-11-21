package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.PlayerProfile
import com.example.data.model.toSafePlayerProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PlayerRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeProfile(userId: String): Flow<PlayerProfile?> {
        val docRef = db.collection(USERS_COLLECTION).document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toSafePlayerProfile()
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    suspend fun getProfile(userId: String): Result<PlayerProfile?> {
        val docRef = db.collection(USERS_COLLECTION).document(userId)
        return try {
            val snapshot = docRef.get().await()
            val profile = if (snapshot.exists()) {
                snapshot.toSafePlayerProfile()
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun isFreeFireUidAvailable(
        freeFireUid: String,
        currentUserId: String,
    ): Result<Boolean> {
        val lockRef = db.collection(UID_LOCKS_COLLECTION).document(freeFireUid)
        return try {
            val snapshot = lockRef.get().await()
            if (!snapshot.exists()) {
                Result.success(true)
            } else {
                val ownerUid = snapshot.getString("userId")
                Result.success(ownerUid == currentUserId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, lockRef.path)
            Result.failure(e)
        }
    }

    suspend fun registerProfile(
        userId: String,
        fullName: String,
        phone: String,
        freeFireUid: String,
        freeFireIgn: String,
    ): Result<Unit> {
        val trimmedUid = freeFireUid.trim()
        val userDocRef = db.collection(USERS_COLLECTION).document(userId)
        val lockDocRef = db.collection(UID_LOCKS_COLLECTION).document(trimmedUid)
        return try {
            db.runTransaction { tx ->
                val existingLock = tx.get(lockDocRef)
                val existingUser = tx.get(userDocRef)
                if (existingLock.exists() && existingLock.getString("userId") != userId) {
                    throw IllegalStateException("This Free Fire UID is already linked to another TournaX account.")
                }

                if (!existingLock.exists()) {
                    tx.set(
                        lockDocRef,
                        mapOf(
                            "userId" to userId,
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }

                if (!existingUser.exists()) {
                    val profilePayload = mapOf(
                        "userId" to userId,
                        "fullName" to fullName.trim(),
                        "phone" to phone.trim(),
                        "freeFireUid" to trimmedUid,
                        "freeFireIgn" to freeFireIgn.trim(),
                        "termsAccepted" to true,
                        "termsVersion" to TERMS_VERSION,
                        "verificationMethod" to VERIFICATION_METHOD,
                        "level" to 1L,
                        "xp" to 0L,
                        "points" to 0L,
                        "totalTournaments" to 0L,
                        "totalKills" to 0L,
                        "booyahs" to 0L,
                        "avatarIndex" to 0L,
                        "squadName" to "",
                        "squadTag" to "",
                        "squadUids" to listOf(trimmedUid),
                        "lastDailySpinDate" to "",
                        "referralCode" to "TX-${trimmedUid.takeLast(6)}",
                        "walletDeposit" to 0L,
                        "walletWinning" to 0L,
                        "trustScore" to 100L,
                        "kycStatus" to "Pending",
                        "accountStatus" to "active",
                        "banReason" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                    tx.set(userDocRef, profilePayload)
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateProfileDetails(
        userId: String,
        fullName: String,
        phone: String,
        freeFireIgn: String,
    ): Result<Unit> {
        val userDocRef = db.collection(USERS_COLLECTION).document(userId)
        val updates = mapOf(
            "fullName" to fullName.trim(),
            "phone" to phone.trim(),
            "freeFireIgn" to freeFireIgn.trim(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            userDocRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        val clean = token.trim()
        if (userId.isBlank() || clean.isBlank() || clean.length > 512) return Result.success(Unit)
        val userDocRef = db.collection(USERS_COLLECTION).document(userId)
        return try {
            userDocRef.update(
                mapOf(
                    "fcmToken" to clean,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDocRef.path)
            Result.failure(e)
        }
    }

    companion object {
        const val USERS_COLLECTION = "users"
        const val UID_LOCKS_COLLECTION = "uidLocks"
        const val TERMS_VERSION = "2026-v1"
        const val VERIFICATION_METHOD = "google_auth"
    }
}
