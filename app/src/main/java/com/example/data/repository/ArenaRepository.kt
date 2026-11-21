package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ArenaNotification
import com.example.data.model.DepositRequest
import com.example.data.model.HeroBanner
import com.example.data.model.LeaderboardEntry
import com.example.data.model.PlayerProfile
import com.example.data.model.Tournament
import com.example.data.model.TournamentParticipant
import com.example.data.model.TournamentRegistration
import com.example.data.model.TournamentResultEntry
import com.example.data.model.WalletAppConfig
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.model.toSafeArenaNotification
import com.example.data.model.toSafeDepositRequest
import com.example.data.model.toSafeHeroBanner
import com.example.data.model.toSafeLeaderboardEntry
import com.example.data.model.toSafeTournament
import com.example.data.model.toSafeTournamentParticipant
import com.example.data.model.toSafeTournamentRegistration
import com.example.data.model.toSafeTournamentResultEntry
import com.example.data.model.toSafeWalletAppConfig
import com.example.data.model.toSafeWalletTransaction
import com.example.data.model.toSafeWithdrawalRequest
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ArenaRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeTournaments(): Flow<List<Tournament>> =
        db.collection(TOURNAMENTS_COLLECTION)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeTournament() }
                    .filter { !isLegacyDemoTournament(it) }
                    .sortedWith(
                        compareBy<Tournament> {
                            when (it.status.lowercase()) {
                                "live" -> 0
                                "upcoming" -> 1
                                "completed" -> 2
                                else -> 3
                            }
                        }.thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, TOURNAMENTS_COLLECTION)
                throw error
            }

    fun observeRegistrations(userId: String): Flow<List<TournamentRegistration>> {
        val path = "${PlayerRepository.USERS_COLLECTION}/$userId/$REGISTRATIONS_SUBCOLLECTION"
        return db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(REGISTRATIONS_SUBCOLLECTION)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeTournamentRegistration() }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeNotifications(userId: String): Flow<List<ArenaNotification>> {
        val adminFlow = db.collection(ADMIN_NOTIFICATIONS_COLLECTION)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeArenaNotification() }
            }
        val readIdsFlow = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(READ_ADMIN_NOTIFICATIONS_SUBCOLLECTION)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.id }.toSet()
            }

        return combine(adminFlow, readIdsFlow) { broadcasts, readIds ->
            broadcasts
                .filter { item ->
                    item.title.isNotBlank() && (
                        item.targetUserId.isBlank() ||
                            item.targetUserId.equals("all", ignoreCase = true) ||
                            item.targetUserId == userId
                        )
                }
                .map { item ->
                    item.copy(isRead = readIds.contains(item.id))
                }
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }.catch { error ->
            logStreamError(error, OperationType.LIST, ADMIN_NOTIFICATIONS_COLLECTION)
            throw error
        }
    }

    fun observeBanners(): Flow<List<HeroBanner>> =
        db.collection(BANNERS_COLLECTION)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeHeroBanner() }
                    .filter { it.active && (it.imageUrl.isNotBlank() || it.title.isNotBlank()) }
                    .sortedWith(compareBy<HeroBanner> { it.order }.thenBy { it.id })
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, BANNERS_COLLECTION)
                throw error
            }

    fun observeWalletConfig(): Flow<WalletAppConfig> =
        db.collection(APP_CONFIG_COLLECTION)
            .document(WALLET_CONFIG_DOC)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) snapshot.toSafeWalletAppConfig() else WalletAppConfig()
            }
            .catch { error ->
                if (error is CancellationException) throw error
                logStreamError(error, OperationType.GET, "$APP_CONFIG_COLLECTION/$WALLET_CONFIG_DOC")
                emit(WalletAppConfig())
            }

    fun observeTransactions(userId: String): Flow<List<WalletTransaction>> {
        val path = "${PlayerRepository.USERS_COLLECTION}/$userId/$TRANSACTIONS_SUBCOLLECTION"
        return db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_SUBCOLLECTION)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeWalletTransaction() }
                    .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> =
        db.collection(LEADERBOARD_COLLECTION)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeLeaderboardEntry() }
                    .filter { entry ->
                        entry.ign.isNotBlank() && !LEGACY_FAKE_LEADERBOARD_ENTRIES.contains(entry.id to entry.ign)
                    }
                    .sortedWith(
                        compareByDescending<LeaderboardEntry> { it.kills }
                            .thenByDescending { it.booyahs }
                            .thenByDescending { it.winningsInr }
                            .thenBy { it.ign.lowercase() }
                    )
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, LEADERBOARD_COLLECTION)
                throw error
            }

    fun observeDepositRequests(userId: String): Flow<List<DepositRequest>> =
        db.collection(DEPOSIT_REQUESTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeDepositRequest() }
                    .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, DEPOSIT_REQUESTS_COLLECTION)
                throw error
            }

    fun observeWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>> =
        db.collection(WITHDRAWAL_REQUESTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeWithdrawalRequest() }
                    .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, WITHDRAWAL_REQUESTS_COLLECTION)
                throw error
            }

    fun observeTournamentResults(tournamentId: String): Flow<List<TournamentResultEntry>> {
        if (tournamentId.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        val path = "$TOURNAMENTS_COLLECTION/$tournamentId/$RESULTS_SUBCOLLECTION"
        return db.collection(TOURNAMENTS_COLLECTION)
            .document(tournamentId)
            .collection(RESULTS_SUBCOLLECTION)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeTournamentResultEntry() }
                    .filter { it.playerOrTeamName.isNotBlank() }
                    .sortedWith(
                        compareBy<TournamentResultEntry> {
                            if (it.rank > 0L) it.rank else Long.MAX_VALUE
                        }
                            .thenByDescending { it.effectiveTotalPoints }
                            .thenByDescending { it.booyahs }
                            .thenByDescending { it.placementPoints }
                            .thenByDescending { it.kills }
                            .thenBy { it.playerOrTeamName.lowercase() }
                    )
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeTournamentParticipants(tournamentId: String): Flow<List<TournamentParticipant>> {
        if (tournamentId.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        val path = "$TOURNAMENTS_COLLECTION/$tournamentId/$PARTICIPANTS_SUBCOLLECTION"
        return db.collection(TOURNAMENTS_COLLECTION)
            .document(tournamentId)
            .collection(PARTICIPANTS_SUBCOLLECTION)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeTournamentParticipant() }
                    .filter { it.captainIgn.isNotBlank() && it.captainUid.isNotBlank() }
                    .sortedWith(
                        compareBy<TournamentParticipant> { it.slotNumber }
                            .thenBy { it.captainIgn.lowercase() }
                    )
            }
            .catch { error ->
                logStreamError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun syncMissingParticipantIfJoined(
        userId: String,
        tournamentId: String,
        registration: TournamentRegistration,
        profile: PlayerProfile,
    ) {
        if (userId.isBlank() || tournamentId.isBlank() || registration.captainIgn.isBlank() || registration.captainUid.isBlank()) return
        val participantRef = db.collection(TOURNAMENTS_COLLECTION)
            .document(tournamentId)
            .collection(PARTICIPANTS_SUBCOLLECTION)
            .document(userId)
        val latestIgn = profile.freeFireIgn.trim().ifBlank { registration.captainIgn.trim() }.take(30)
        val latestTag = profile.squadTag.trim().take(10)
        val latestAvatar = profile.avatarIndex.coerceIn(0L, 3L)
        try {
            val existing = participantRef.get().await()
            if (!existing.exists()) {
                participantRef.set(
                    mapOf(
                        "userId" to userId,
                        "tournamentId" to tournamentId,
                        "slotNumber" to registration.slotNumber.coerceIn(1L, 200L),
                        "captainIgn" to latestIgn,
                        "captainUid" to registration.captainUid.trim(),
                        "squadTag" to latestTag,
                        "avatarIndex" to latestAvatar,
                        "teammateUids" to registration.teammateUids.filter { it.isNotBlank() }.take(4),
                        "joinedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                val oldIgn = existing.getString("captainIgn").orEmpty()
                val oldTag = existing.getString("squadTag").orEmpty()
                val oldAvatar = existing.getLong("avatarIndex") ?: -1L
                if (oldIgn != latestIgn || oldTag != latestTag || oldAvatar != latestAvatar) {
                    participantRef.update(
                        mapOf(
                            "captainIgn" to latestIgn,
                            "squadTag" to latestTag,
                            "avatarIndex" to latestAvatar,
                        )
                    ).await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-fatal backfill / sync
        }
    }

    suspend fun syncActiveParticipantsMetadata(
        userId: String,
        activeTournamentIds: List<String>,
        profile: PlayerProfile,
    ) {
        val cleanIgn = profile.freeFireIgn.trim().take(30)
        if (userId.isBlank() || cleanIgn.length < 2 || activeTournamentIds.isEmpty()) return
        val cleanTag = profile.squadTag.trim().take(10)
        val cleanAvatar = profile.avatarIndex.coerceIn(0L, 3L)
        activeTournamentIds.distinct().forEach { tid ->
            if (tid.isBlank()) return@forEach
            val participantRef = db.collection(TOURNAMENTS_COLLECTION)
                .document(tid)
                .collection(PARTICIPANTS_SUBCOLLECTION)
                .document(userId)
            try {
                val existing = participantRef.get().await()
                if (existing.exists()) {
                    val oldIgn = existing.getString("captainIgn").orEmpty()
                    val oldTag = existing.getString("squadTag").orEmpty()
                    val oldAvatar = existing.getLong("avatarIndex") ?: -1L
                    if (oldIgn != cleanIgn || oldTag != cleanTag || oldAvatar != cleanAvatar) {
                        participantRef.update(
                            mapOf(
                                "captainIgn" to cleanIgn,
                                "squadTag" to cleanTag,
                                "avatarIndex" to cleanAvatar,
                            )
                        ).await()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort sync (e.g. if tournament already completed)
            }
        }
    }

    suspend fun submitDepositRequest(
        userId: String,
        amountInr: Long,
        rawUtr: String,
        userIgn: String,
        userPhone: String,
        userEmail: String,
        merchantUpiId: String = MERCHANT_UPI_ID,
        minDepositInr: Long = 10L,
        maxDepositInr: Long = 500L,
    ): Result<String> {
        val liveConfig = fetchLiveWalletConfigOrNull()
        val effectiveMin = (liveConfig?.minDepositInr ?: minDepositInr).coerceIn(1L, 50_000L)
        val effectiveMax = (liveConfig?.maxDepositInr ?: maxDepositInr).coerceIn(effectiveMin, 50_000L)
        val normalizedUtr = rawUtr.trim().uppercase()
        val candidateUpi = liveConfig?.merchantUpiId ?: merchantUpiId
        val targetUpiId = candidateUpi.trim().takeIf { VALID_UPI_REGEX.matches(it) } ?: MERCHANT_UPI_ID
        if (amountInr !in effectiveMin..effectiveMax) {
            return Result.failure(
                IllegalArgumentException("Amount must be between ₹$effectiveMin and ₹$effectiveMax.")
            )
        }
        if (!normalizedUtr.matches(Regex("^[A-Z0-9]{10,22}$"))) {
            return Result.failure(
                IllegalArgumentException("Enter a valid 10–22 character UPI UTR / Ref number.")
            )
        }

        val lockRef = db.collection(UTR_LOCKS_COLLECTION).document(normalizedUtr)
        val depositRef = db.collection(DEPOSIT_REQUESTS_COLLECTION).document(normalizedUtr)

        return try {
            val existingLock = lockRef.get().await()
            if (existingLock.exists()) {
                val lockOwner = existingLock.getString("userId").orEmpty()
                if (lockOwner == userId) {
                    val existingDeposit = depositRef.get().await()
                    val existingStatus = existingDeposit.getString("status").orEmpty()
                    if (existingDeposit.exists() && existingStatus.equals("rejected", ignoreCase = true)) {
                        depositRef.update(
                            mapOf(
                                "upiId" to targetUpiId,
                                "amount" to amountInr,
                                "status" to "pending",
                                "credited" to false,
                                "adminNote" to "",
                                "userIgn" to userIgn.trim().take(40),
                                "userPhone" to userPhone.trim().take(20),
                                "userEmail" to userEmail.trim().take(120),
                                "updatedAt" to FieldValue.serverTimestamp(),
                            )
                        ).await()
                        return Result.success(normalizedUtr)
                    }
                }
                return Result.failure(
                    IllegalStateException("This UTR ($normalizedUtr) has already been submitted. Duplicate UTRs are not allowed.")
                )
            }
            val batch = db.batch()
            batch.set(
                lockRef,
                mapOf(
                    "userId" to userId,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            batch.set(
                depositRef,
                mapOf(
                    "userId" to userId,
                    "userIgn" to userIgn.trim().take(40),
                    "userPhone" to userPhone.trim().take(20),
                    "userEmail" to userEmail.trim().take(120),
                    "utr" to normalizedUtr,
                    "upiId" to targetUpiId,
                    "amount" to amountInr,
                    "status" to "pending",
                    "credited" to false,
                    "adminNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            batch.commit().await()
            Result.success(normalizedUtr)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, depositRef.path)
            val msg = if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                "Deposit verification blocked (duplicate UTR, or amount/UPI limit ₹$effectiveMin–₹$effectiveMax changed). Refresh Wallet and retry."
            } else {
                e.localizedMessage ?: "Failed to submit payment UTR."
            }
            Result.failure(IllegalStateException(msg, e))
        }
    }

    suspend fun claimApprovedDeposit(
        userId: String,
        utr: String,
    ): Result<Long> {
        val normalizedUtr = utr.trim().uppercase()
        if (normalizedUtr.isBlank()) {
            return Result.failure(IllegalArgumentException("UTR cannot be blank."))
        }
        val depositRef = db.collection(DEPOSIT_REQUESTS_COLLECTION).document(normalizedUtr)
        val userRef = db.collection(PlayerRepository.USERS_COLLECTION).document(userId)
        val txRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_SUBCOLLECTION)
            .document("dep_$normalizedUtr")

        return try {
            val creditedAmount = db.runTransaction { tx ->
                val depositSnap = tx.get(depositRef)
                val userSnap = tx.get(userRef)
                val existingTx = tx.get(txRef)

                if (!depositSnap.exists()) {
                    throw IllegalStateException("Deposit request not found.")
                }
                val ownerUid = depositSnap.getString("userId").orEmpty()
                if (ownerUid != userId) {
                    throw IllegalStateException("Unauthorized deposit request.")
                }
                val status = depositSnap.getString("status").orEmpty()
                val alreadyCredited = depositSnap.getBoolean("credited") ?: false
                if (!status.equals("approved", ignoreCase = true)) {
                    throw IllegalStateException("Deposit is not approved yet.")
                }
                val amount = (depositSnap.getLong("amount") ?: 0L).coerceAtLeast(0L)
                val txTitle = "UPI Deposit (+$amount Pts)".take(120)
                val txCategory = "₹$amount → $amount Pts • UTR: $normalizedUtr".take(120)
                if (alreadyCredited) {
                    if (amount > 0L && !existingTx.exists()) {
                        tx.set(
                            txRef,
                            mapOf(
                                "userId" to userId,
                                "title" to txTitle,
                                "category" to txCategory,
                                "amount" to amount,
                                "isCredit" to true,
                                "status" to "Credited",
                                "createdAt" to FieldValue.serverTimestamp(),
                            ),
                        )
                    }
                    return@runTransaction 0L
                }
                if (amount <= 0L) {
                    throw IllegalStateException("Invalid deposit amount.")
                }
                val prevDeposit = (userSnap.getLong("walletDeposit") ?: 0L).coerceAtLeast(0L)

                tx.update(
                    depositRef,
                    mapOf(
                        "credited" to true,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.update(
                    userRef,
                    mapOf(
                        "walletDeposit" to (prevDeposit + amount),
                        "lastApprovedUtr" to normalizedUtr,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                if (!existingTx.exists()) {
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to userId,
                            "title" to txTitle,
                            "category" to txCategory,
                            "amount" to amount,
                            "isCredit" to true,
                            "status" to "Credited",
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
                amount
            }.await()
            Result.success(creditedAmount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, depositRef.path)
            Result.failure(e)
        }
    }

    suspend fun ensureApprovedDepositTransactionLogged(
        userId: String,
        deposit: DepositRequest,
    ) {
        val normalizedUtr = deposit.utr.ifBlank { deposit.id }.trim().uppercase()
        if (userId.isBlank() || normalizedUtr.isBlank() || deposit.amount <= 0L) return
        val txRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_SUBCOLLECTION)
            .document("dep_$normalizedUtr")
        try {
            if (txRef.get().await().exists()) return
            txRef.set(
                mapOf(
                    "userId" to userId,
                    "title" to "UPI Deposit (+${deposit.amount} Pts)".take(120),
                    "category" to "₹${deposit.amount} → ${deposit.amount} Pts • UTR: $normalizedUtr".take(120),
                    "amount" to deposit.amount,
                    "isCredit" to true,
                    "status" to "Credited",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort ledger backfill when Admin sets credited=true directly
        }
    }

    suspend fun submitWithdrawalRequest(
        userId: String,
        fullName: String,
        freeFireUid: String,
        freeFireIgn: String,
        userPhone: String,
        userEmail: String,
        upiId: String,
        amountInr: Long,
        selfieBase64: String,
        minWithdrawalPoints: Long = MIN_WITHDRAWAL_POINTS,
    ): Result<String> {
        val liveConfig = fetchLiveWalletConfigOrNull()
        val effectiveMinWithdraw = (liveConfig?.minWithdrawalPoints ?: minWithdrawalPoints).coerceIn(10L, 10_000L)
        val cleanName = fullName.trim().take(80)
        val cleanUid = freeFireUid.trim()
        val cleanUpi = upiId.trim().take(80)
        val cleanSelfie = selfieBase64.trim()

        if (cleanName.length < 3) {
            return Result.failure(IllegalArgumentException("Enter your valid full name (at least 3 characters)."))
        }
        if (!cleanUid.matches(Regex("^[0-9]{8,12}$"))) {
            return Result.failure(IllegalArgumentException("Enter a valid 8–12 digit Free Fire UID."))
        }
        if (amountInr < effectiveMinWithdraw) {
            return Result.failure(
                IllegalArgumentException("Minimum $effectiveMinWithdraw Winning Points (₹$effectiveMinWithdraw) required to withdraw.")
            )
        }
        if (!VALID_UPI_REGEX.matches(cleanUpi)) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID (e.g. name@okaxis or 9876543210@ybl)."))
        }
        if (cleanSelfie.length < 100 || cleanSelfie.length > 350_000) {
            return Result.failure(IllegalArgumentException("Please capture a clear live selfie to verify your withdrawal."))
        }

        val requestId = "wd_${userId.take(8)}_${System.currentTimeMillis()}"
        val withdrawRef = db.collection(WITHDRAWAL_REQUESTS_COLLECTION).document(requestId)
        val userRef = db.collection(PlayerRepository.USERS_COLLECTION).document(userId)
        val txRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_SUBCOLLECTION)
            .document("tx_$requestId")

        return try {
            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                val prevWinning = (userSnap.getLong("walletWinning") ?: 0L).coerceAtLeast(0L)
                if (prevWinning < amountInr) {
                    throw IllegalStateException("Insufficient Winning Balance! You have ₹$prevWinning available.")
                }

                tx.update(
                    userRef,
                    mapOf(
                        "walletWinning" to (prevWinning - amountInr),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.set(
                    withdrawRef,
                    mapOf(
                        "userId" to userId,
                        "fullName" to cleanName,
                        "freeFireUid" to cleanUid,
                        "freeFireIgn" to freeFireIgn.trim().take(30),
                        "userPhone" to userPhone.trim().take(20),
                        "userEmail" to userEmail.trim().take(120),
                        "upiId" to cleanUpi,
                        "amount" to amountInr,
                        "selfieBase64" to cleanSelfie,
                        "status" to "pending",
                        "refunded" to false,
                        "adminNote" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to userId,
                        "title" to "Withdrawal Pending (-₹$amountInr)".take(120),
                        "category" to "10 PM Payout • $cleanUpi".take(120),
                        "amount" to amountInr,
                        "isCredit" to false,
                        "status" to "Pending (10 PM)",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }.await()
            Result.success(requestId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, withdrawRef.path)
            Result.failure(
                IllegalStateException(e.localizedMessage ?: "Failed to submit withdrawal request.", e)
            )
        }
    }

    suspend fun claimRejectedWithdrawalRefund(
        userId: String,
        requestId: String,
    ): Result<Long> {
        val cleanId = requestId.trim()
        if (cleanId.isBlank()) return Result.failure(IllegalArgumentException("Withdrawal ID cannot be blank."))
        val withdrawRef = db.collection(WITHDRAWAL_REQUESTS_COLLECTION).document(cleanId)
        val userRef = db.collection(PlayerRepository.USERS_COLLECTION).document(userId)
        val refundTxRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_SUBCOLLECTION)
            .document("wd_refund_$cleanId")

        return try {
            val refundedAmount = db.runTransaction { tx ->
                val wdSnap = tx.get(withdrawRef)
                val userSnap = tx.get(userRef)
                val existingRefundTx = tx.get(refundTxRef)

                if (!wdSnap.exists()) {
                    throw IllegalStateException("Withdrawal request not found.")
                }
                if (wdSnap.getString("userId").orEmpty() != userId) {
                    throw IllegalStateException("Unauthorized withdrawal request.")
                }
                val status = wdSnap.getString("status").orEmpty()
                if (!status.equals("rejected", ignoreCase = true)) {
                    throw IllegalStateException("Withdrawal is not declined.")
                }
                val amount = (wdSnap.getLong("amount") ?: 0L).coerceAtLeast(0L)
                val alreadyRefunded = wdSnap.getBoolean("refunded") ?: false
                if (alreadyRefunded) {
                    if (amount > 0L && !existingRefundTx.exists()) {
                        tx.set(
                            refundTxRef,
                            mapOf(
                                "userId" to userId,
                                "title" to "Withdrawal Refunded (+₹$amount)".take(120),
                                "category" to "Declined Payout Refund",
                                "amount" to amount,
                                "isCredit" to true,
                                "status" to "Refunded",
                                "createdAt" to FieldValue.serverTimestamp(),
                            ),
                        )
                    }
                    return@runTransaction 0L
                }
                if (amount <= 0L) {
                    throw IllegalStateException("Invalid withdrawal refund amount.")
                }
                val prevWinning = (userSnap.getLong("walletWinning") ?: 0L).coerceAtLeast(0L)

                tx.update(
                    withdrawRef,
                    mapOf(
                        "refunded" to true,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.update(
                    userRef,
                    mapOf(
                        "walletWinning" to (prevWinning + amount),
                        "lastRefundedWithdrawalId" to cleanId,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                if (!existingRefundTx.exists()) {
                    tx.set(
                        refundTxRef,
                        mapOf(
                            "userId" to userId,
                            "title" to "Withdrawal Refunded (+₹$amount)".take(120),
                            "category" to "Declined Payout Refund",
                            "amount" to amount,
                            "isCredit" to true,
                            "status" to "Refunded",
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
                amount
            }.await()
            Result.success(refundedAmount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, withdrawRef.path)
            Result.failure(e)
        }
    }

    suspend fun ensureRejectedWithdrawalRefundTxLogged(
        userId: String,
        request: WithdrawalRequest,
    ) {
        val cleanId = request.id.trim()
        if (userId.isBlank() || cleanId.isBlank() || request.amount <= 0L) return
        val refundTxRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_SUBCOLLECTION)
            .document("wd_refund_$cleanId")
        try {
            if (refundTxRef.get().await().exists()) return
            refundTxRef.set(
                mapOf(
                    "userId" to userId,
                    "title" to "Withdrawal Refunded (+₹${request.amount})".take(120),
                    "category" to "Declined Payout Refund",
                    "amount" to request.amount,
                    "isCredit" to true,
                    "status" to "Refunded",
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort ledger backfill
        }
    }

    suspend fun syncOwnLeaderboardEntry(profile: PlayerProfile) {
        val uid = profile.userId.trim()
        val ign = profile.freeFireIgn.trim()
        if (uid.isEmpty() || ign.length < 2) return
        val lbRef = db.collection(LEADERBOARD_COLLECTION).document(uid)
        try {
            lbRef.set(
                mapOf(
                    "ign" to ign.take(30),
                    "kills" to profile.totalKills.coerceAtLeast(0L),
                    "booyahs" to profile.booyahs.coerceAtLeast(0L),
                    "winningsInr" to profile.walletWinning.coerceAtLeast(0L),
                    "avatarIndex" to profile.avatarIndex.coerceIn(0L, 3L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, lbRef.path)
        }
    }

    suspend fun joinTournament(
        userId: String,
        tournament: Tournament,
        captainUid: String,
        captainIgn: String,
        teammateUids: List<String>,
    ): Result<Long> {
        val tournamentRef = db.collection(TOURNAMENTS_COLLECTION).document(tournament.id)
        val regRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(REGISTRATIONS_SUBCOLLECTION)
            .document(tournament.id)
        val participantRef = db.collection(TOURNAMENTS_COLLECTION)
            .document(tournament.id)
            .collection(PARTICIPANTS_SUBCOLLECTION)
            .document(userId)
        val userRef = db.collection(PlayerRepository.USERS_COLLECTION).document(userId)
        val entryTxRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_SUBCOLLECTION)
            .document("join_${tournament.id}")

        val cleanCaptainUid = captainUid.trim()
        if (!cleanCaptainUid.matches(Regex("^[0-9]{8,12}$"))) {
            return Result.failure(IllegalArgumentException("Captain Free Fire UID must be 8–12 digits."))
        }
        val cleanTeammates = teammateUids.map { it.trim() }.filter { it.isNotEmpty() }.take(4)
        if (cleanTeammates.any { !it.matches(Regex("^[0-9]{8,12}$")) }) {
            return Result.failure(IllegalArgumentException("Every teammate Free Fire UID must be 8–12 digits."))
        }
        if (cleanTeammates.any { it == cleanCaptainUid }) {
            return Result.failure(IllegalArgumentException("Teammate UID cannot match your own Captain UID."))
        }
        if (cleanTeammates.distinct().size != cleanTeammates.size) {
            return Result.failure(IllegalArgumentException("Duplicate teammate UIDs are not allowed."))
        }

        return try {
            val assignedSlot = db.runTransaction { tx ->
                val snap = tx.get(tournamentRef)
                val existingReg = tx.get(regRef)
                val userSnap = tx.get(userRef)
                val existingEntryTx = tx.get(entryTxRef)
                if (existingReg.exists()) {
                    throw IllegalStateException("You have already joined ${tournament.title}.")
                }
                if (!snap.exists()) {
                    throw IllegalStateException("Tournament no longer exists.")
                }
                val matchStatus = snap.getString("status").orEmpty().trim().lowercase()
                if (matchStatus !in listOf("upcoming", "live")) {
                    throw IllegalStateException("This tournament is $matchStatus and is closed for joining.")
                }
                val currentFilled = snap.getLong("filledSlots") ?: 0L
                val maxSlots = snap.getLong("maxSlots") ?: 48L
                if (currentFilled >= maxSlots) {
                    throw IllegalStateException("Lobby is full! No slots remaining.")
                }

                val entryFeePts = (snap.getLong("entryFee") ?: tournament.entryFee).coerceAtLeast(0L)
                val prevDeposit = (userSnap.getLong("walletDeposit") ?: 0L).coerceAtLeast(0L)
                val prevWinning = (userSnap.getLong("walletWinning") ?: 0L).coerceAtLeast(0L)
                val totalPlayable = prevDeposit + prevWinning

                if (totalPlayable < entryFeePts) {
                    throw IllegalStateException(
                        "Insufficient Points! Need $entryFeePts Pts to join (you have $totalPlayable Pts). Add money in Wallet."
                    )
                }

                // Deduct entry fee in strict 2-bucket esports wallet priority: Deposit -> Winnings
                var remaining = entryFeePts
                val deductDeposit = minOf(prevDeposit, remaining)
                remaining -= deductDeposit
                val deductWinning = minOf(prevWinning, remaining)

                val nextSlot = currentFilled + 1L
                val prevTournaments = userSnap.getLong("totalTournaments") ?: 0L
                val prevXp = userSnap.getLong("xp") ?: 0L
                val newXp = prevXp + 50L
                val newLevel = (newXp / 500L) + 1L

                val squadTag = (userSnap.getString("squadTag") ?: "").trim().take(10)
                val avatarIndex = (userSnap.getLong("avatarIndex") ?: 0L).coerceIn(0L, 3L)

                tx.update(tournamentRef, "filledSlots", nextSlot)

                val regPayload = mapOf(
                    "userId" to userId,
                    "tournamentId" to tournament.id,
                    "tournamentTitle" to tournament.title,
                    "mode" to tournament.mode,
                    "mapName" to tournament.mapName,
                    "dateTimeLabel" to tournament.dateTimeLabel,
                    "slotNumber" to nextSlot,
                    "captainUid" to cleanCaptainUid,
                    "captainIgn" to captainIgn.trim(),
                    "teammateUids" to cleanTeammates,
                    "screenshotSubmitted" to false,
                    "submittedKills" to 0L,
                    "screenshotUri" to "",
                    "resultStatus" to "Registered",
                    "entryFeePaid" to entryFeePts,
                    "depositPaid" to deductDeposit,
                    "winningPaid" to deductWinning,
                    "refunded" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                tx.set(regRef, regPayload)

                tx.set(
                    participantRef,
                    mapOf(
                        "userId" to userId,
                        "tournamentId" to tournament.id,
                        "slotNumber" to nextSlot,
                        "captainIgn" to captainIgn.trim().take(30),
                        "captainUid" to captainUid.trim(),
                        "squadTag" to squadTag,
                        "avatarIndex" to avatarIndex,
                        "teammateUids" to cleanTeammates,
                        "joinedAt" to FieldValue.serverTimestamp(),
                    )
                )

                tx.update(
                    userRef,
                    mapOf(
                        "walletDeposit" to (prevDeposit - deductDeposit),
                        "walletWinning" to (prevWinning - deductWinning),
                        "totalTournaments" to (prevTournaments + 1L),
                        "xp" to newXp,
                        "level" to newLevel,
                        "lastJoinedTournamentId" to tournament.id,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                if (entryFeePts > 0L && !existingEntryTx.exists()) {
                    tx.set(
                        entryTxRef,
                        mapOf(
                            "userId" to userId,
                            "title" to "Joined: ${tournament.title}".take(120),
                            "category" to "${tournament.mode} • Entry (-$entryFeePts Pts)".take(120),
                            "amount" to entryFeePts,
                            "isCredit" to false,
                            "status" to "Match Entry",
                            "createdAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
                nextSlot
            }.await()

            Result.success(assignedSlot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, tournamentRef.path)
            Result.failure(e)
        }
    }

    suspend fun claimCancelledTournamentRefund(
        userId: String,
        tournamentId: String,
    ): Result<Long> {
        val cleanTid = tournamentId.trim()
        if (cleanTid.isBlank()) return Result.failure(IllegalArgumentException("Tournament ID cannot be blank."))
        val tournRef = db.collection(TOURNAMENTS_COLLECTION).document(cleanTid)
        val regRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(REGISTRATIONS_SUBCOLLECTION)
            .document(cleanTid)
        val userRef = db.collection(PlayerRepository.USERS_COLLECTION).document(userId)
        val refundTxRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_SUBCOLLECTION)
            .document("refund_$cleanTid")

        return try {
            val refundedPts = db.runTransaction { tx ->
                val tournSnap = tx.get(tournRef)
                val regSnap = tx.get(regRef)
                val userSnap = tx.get(userRef)
                val existingTx = tx.get(refundTxRef)

                if (!tournSnap.exists() || !tournSnap.getString("status").orEmpty().equals("cancelled", ignoreCase = true)) {
                    throw IllegalStateException("Tournament is not marked cancelled.")
                }
                if (!regSnap.exists()) {
                    throw IllegalStateException("Registration record not found.")
                }
                val entryPaid = (regSnap.getLong("entryFeePaid") ?: 0L).coerceAtLeast(0L)
                if (entryPaid <= 0L) {
                    return@runTransaction 0L
                }
                val storedDepPaid = regSnap.getLong("depositPaid") ?: -1L
                val storedWinPaid = regSnap.getLong("winningPaid") ?: -1L
                val (refundDeposit, refundWinning) = if (
                    storedDepPaid >= 0L && storedWinPaid >= 0L && storedDepPaid + storedWinPaid == entryPaid
                ) {
                    storedDepPaid to storedWinPaid
                } else {
                    entryPaid to 0L
                }

                val title = regSnap.getString("tournamentTitle").orEmpty().ifBlank { cleanTid }
                val alreadyRefunded = regSnap.getBoolean("refunded") ?: false
                val txTitle = "Match Refund (+$entryPaid Pts)".take(120)
                val bucketSuffix = when {
                    refundDeposit > 0L && refundWinning > 0L -> " (${refundDeposit} Dep + ${refundWinning} Win)"
                    refundWinning > 0L -> " (→ Winning)"
                    else -> " (→ Deposit)"
                }
                val txCategory = "Cancelled Match • $title$bucketSuffix".take(120)
                if (alreadyRefunded) {
                    if (!existingTx.exists()) {
                        tx.set(
                            refundTxRef,
                            mapOf(
                                "userId" to userId,
                                "title" to txTitle,
                                "category" to txCategory,
                                "amount" to entryPaid,
                                "isCredit" to true,
                                "status" to "Refunded",
                                "createdAt" to FieldValue.serverTimestamp(),
                            ),
                        )
                    }
                    return@runTransaction 0L
                }

                val prevDeposit = (userSnap.getLong("walletDeposit") ?: 0L).coerceAtLeast(0L)
                val prevWinning = (userSnap.getLong("walletWinning") ?: 0L).coerceAtLeast(0L)
                val prevTournaments = (userSnap.getLong("totalTournaments") ?: 0L).coerceAtLeast(0L)
                val prevXp = (userSnap.getLong("xp") ?: 0L).coerceAtLeast(0L)
                val newTournaments = (prevTournaments - 1L).coerceAtLeast(0L)
                val newXp = (prevXp - 50L).coerceAtLeast(0L)
                val newLevel = (newXp / 500L) + 1L

                tx.update(
                    regRef,
                    mapOf(
                        "refunded" to true,
                        "resultStatus" to "Cancelled • Refunded",
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.update(
                    userRef,
                    mapOf(
                        "walletDeposit" to (prevDeposit + refundDeposit),
                        "walletWinning" to (prevWinning + refundWinning),
                        "totalTournaments" to newTournaments,
                        "xp" to newXp,
                        "level" to newLevel,
                        "lastRefundedTournamentId" to cleanTid,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                if (!existingTx.exists()) {
                    tx.set(
                        refundTxRef,
                        mapOf(
                            "userId" to userId,
                            "title" to txTitle,
                            "category" to txCategory,
                            "amount" to entryPaid,
                            "isCredit" to true,
                            "status" to "Refunded",
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                }
                entryPaid
            }.await()
            Result.success(refundedPts)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, regRef.path)
            Result.failure(e)
        }
    }

    suspend fun ensureCancelledTournamentRefundTxLogged(
        userId: String,
        reg: TournamentRegistration,
    ) {
        val cleanTid = reg.tournamentId.trim()
        if (userId.isBlank() || cleanTid.isBlank() || reg.entryFeePaid <= 0L) return
        val refundTxRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_SUBCOLLECTION)
            .document("refund_$cleanTid")
        try {
            if (refundTxRef.get().await().exists()) return
            refundTxRef.set(
                mapOf(
                    "userId" to userId,
                    "title" to "Match Refund (+${reg.entryFeePaid} Pts)".take(120),
                    "category" to "Cancelled Match • ${reg.tournamentTitle.ifBlank { cleanTid }}".take(120),
                    "amount" to reg.entryFeePaid,
                    "isCredit" to true,
                    "status" to "Refunded",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Best-effort ledger backfill
        }
    }

    suspend fun submitSupportTicket(
        userId: String,
        profile: PlayerProfile,
        userEmail: String,
        category: String,
        suspectUid: String,
        notes: String,
    ): Result<String> {
        val cleanCategory = category.trim().lowercase().takeIf {
            it in setOf("cheater_report", "payment_issue", "match_issue", "general")
        } ?: "general"
        val cleanSuspect = suspectUid.trim()
        if (cleanSuspect.isNotEmpty() && !cleanSuspect.matches(Regex("^[0-9]{8,12}$"))) {
            return Result.failure(IllegalArgumentException("Suspect Free Fire UID must be 8–12 digits if provided."))
        }
        val cleanNotes = notes.trim().take(500)
        if (cleanNotes.length < 5) {
            return Result.failure(IllegalArgumentException("Please enter at least 5 characters describing the issue."))
        }
        val ticketId = "tkt_${userId.take(8)}_${System.currentTimeMillis()}"
        val ticketRef = db.collection(SUPPORT_TICKETS_COLLECTION).document(ticketId)
        return try {
            ticketRef.set(
                mapOf(
                    "userId" to userId,
                    "userIgn" to profile.freeFireIgn.trim().take(40),
                    "userPhone" to profile.phone.trim().take(20),
                    "userEmail" to userEmail.trim().take(120),
                    "category" to cleanCategory,
                    "suspectUid" to cleanSuspect,
                    "notes" to cleanNotes,
                    "status" to "pending",
                    "adminNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(ticketId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, ticketRef.path)
            Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to submit support ticket.", e))
        }
    }

    suspend fun submitResultScreenshot(
        userId: String,
        tournamentId: String,
        kills: Long,
        screenshotUri: String,
    ): Result<Unit> {
        val cleanProof = screenshotUri.trim()
        if (cleanProof.isBlank() || cleanProof.length > 240_000) {
            return Result.failure(IllegalArgumentException("Invalid or oversized screenshot proof. Please re-select."))
        }
        if (kills !in 0L..100L) {
            return Result.failure(IllegalArgumentException("Kills count must be between 0 and 100."))
        }
        val tournRef = db.collection(TOURNAMENTS_COLLECTION).document(tournamentId)
        val regRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(REGISTRATIONS_SUBCOLLECTION)
            .document(tournamentId)
        return try {
            val tournSnap = tournRef.get().await()
            if (!tournSnap.exists()) {
                return Result.failure(
                    IllegalStateException("Tournament no longer exists or was removed by Admin.")
                )
            }
            val status = tournSnap.getString("status").orEmpty().trim().lowercase()
            if (status !in listOf("live", "completed")) {
                return Result.failure(
                    IllegalStateException("Match proof can only be submitted once the match is Live or Completed.")
                )
            }
            regRef.update(
                mapOf(
                    "screenshotSubmitted" to true,
                    "submittedKills" to kills,
                    "screenshotUri" to cleanProof,
                    "resultStatus" to "Submitted for Review",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, regRef.path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(userId: String, notifId: String) {
        val docRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(READ_ADMIN_NOTIFICATIONS_SUBCOLLECTION)
            .document(notifId)
        try {
            docRef.set(
                mapOf(
                    "notifId" to notifId,
                    "readAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun markAllNotificationsRead(userId: String, unreadIds: List<String>) {
        val distinctIds = unreadIds.filter { it.isNotBlank() }.distinct()
        if (distinctIds.isEmpty()) return
        try {
            distinctIds.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    val ref = db.collection(PlayerRepository.USERS_COLLECTION)
                        .document(userId)
                        .collection(READ_ADMIN_NOTIFICATIONS_SUBCOLLECTION)
                        .document(id)
                    batch.set(
                        ref,
                        mapOf(
                            "notifId" to id,
                            "readAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, READ_ADMIN_NOTIFICATIONS_SUBCOLLECTION)
        }
    }

    suspend fun updateAvatar(userId: String, avatarIndex: Long): Result<Unit> {
        val userRef = db.collection(PlayerRepository.USERS_COLLECTION).document(userId)
        return try {
            userRef.update(
                mapOf(
                    "avatarIndex" to avatarIndex.coerceIn(0L, 3L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSquad(
        userId: String,
        squadName: String,
        squadTag: String,
        squadUids: List<String>,
    ): Result<Unit> {
        val cleanUids = squadUids.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(4)
        if (cleanUids.any { !it.matches(Regex("^[0-9]{8,12}$")) }) {
            return Result.failure(IllegalArgumentException("All squad Free Fire UIDs must be 8–12 digits."))
        }
        val userRef = db.collection(PlayerRepository.USERS_COLLECTION).document(userId)
        return try {
            userRef.update(
                mapOf(
                    "squadName" to squadName.trim().take(40),
                    "squadTag" to squadTag.trim().uppercase().take(10),
                    "squadUids" to cleanUids,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    private fun logStreamError(error: Throwable, operationType: OperationType, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    private suspend fun fetchLiveWalletConfigOrNull(): WalletAppConfig? {
        return try {
            val snap = db.collection(APP_CONFIG_COLLECTION)
                .document(WALLET_CONFIG_DOC)
                .get()
                .await()
            if (snap.exists()) snap.toSafeWalletAppConfig() else null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val MERCHANT_UPI_ID = "7378633612@fam"
        const val MIN_WITHDRAWAL_POINTS = 50L
        val VALID_UPI_REGEX = Regex("^[a-zA-Z0-9._-]{2,64}@[a-zA-Z0-9.-]{2,32}$")
        const val APP_CONFIG_COLLECTION = "appConfig"
        const val WALLET_CONFIG_DOC = "wallet"
        const val TOURNAMENTS_COLLECTION = "tournaments"
        const val LEADERBOARD_COLLECTION = "leaderboard"
        const val ADMIN_NOTIFICATIONS_COLLECTION = "adminNotifications"
        const val BANNERS_COLLECTION = "banners"
        const val DEPOSIT_REQUESTS_COLLECTION = "depositRequests"
        const val WITHDRAWAL_REQUESTS_COLLECTION = "withdrawalRequests"
        const val SUPPORT_TICKETS_COLLECTION = "supportTickets"
        const val UTR_LOCKS_COLLECTION = "utrLocks"
        const val REGISTRATIONS_SUBCOLLECTION = "registrations"
        const val RESULTS_SUBCOLLECTION = "results"
        const val PARTICIPANTS_SUBCOLLECTION = "participants"
        const val NOTIFICATIONS_SUBCOLLECTION = "notifications"
        const val READ_ADMIN_NOTIFICATIONS_SUBCOLLECTION = "readAdminNotifications"
        const val TRANSACTIONS_SUBCOLLECTION = "transactions"

        private fun isLegacyDemoTournament(tournament: Tournament): Boolean {
            if (tournament.isDemo || tournament.title.isBlank() || tournament.mode.isBlank()) {
                return true
            }
            val idLower = tournament.id.trim().lowercase()
            if (LEGACY_DEMO_TOURNAMENT_ID_REGEX.matches(idLower)) {
                return true
            }
            return tournament.roomId.trim() in LEGACY_DEMO_ROOM_IDS
        }

        private val LEGACY_DEMO_TOURNAMENT_ID_REGEX = Regex(
            "^(tourn_demo|demo_|sample_|seed_|mock_|tourn_0).*|^tourn_[1-9]$"
        )

        private val LEGACY_DEMO_ROOM_IDS = setOf("88442190", "88442191", "88442101", "88442102")

        private val LEGACY_FAKE_LEADERBOARD_ENTRIES = setOf(
            "lb_01" to "TX_RaistarIND",
            "lb_02" to "OBS_ViperFF",
            "lb_03" to "IND_KobraOP",
            "lb_04" to "Riot_Aarav99",
            "lb_05" to "NX_Phantom",
            "lb_06" to "Delhi_Sniper",
            "lb_07" to "Mumbai_Rush",
            "lb_08" to "TX_ShadowX",
            "lb_09" to "Blaze_Rohan",
            "lb_10" to "PUNE_Hunter",
            "lb_11" to "Hyd_Assassin",
            "lb_12" to "BLR_Headshot",
            "lb_13" to "Jaipur_King",
            "lb_14" to "Kochi_Clutch",
            "lb_15" to "Patna_Booyah",
        )
    }
}
