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
                                else -> 2
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

    fun observeTransactions(userId: String): Flow<List<WalletTransaction>> {
        val path = "${PlayerRepository.USERS_COLLECTION}/$userId/$TRANSACTIONS_SUBCOLLECTION"
        return db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(TRANSACTIONS_SUBCOLLECTION)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.toSafeWalletTransaction() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
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
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
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
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
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
        try {
            val existing = participantRef.get().await()
            if (existing.exists()) return
            participantRef.set(
                mapOf(
                    "userId" to userId,
                    "tournamentId" to tournamentId,
                    "slotNumber" to registration.slotNumber.coerceIn(1L, 200L),
                    "captainIgn" to registration.captainIgn.trim().take(30),
                    "captainUid" to registration.captainUid.trim(),
                    "squadTag" to profile.squadTag.trim().take(10),
                    "avatarIndex" to profile.avatarIndex.coerceIn(0L, 3L),
                    "teammateUids" to registration.teammateUids.filter { it.isNotBlank() }.take(4),
                    "joinedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-fatal backfill for pre-update registrations
        }
    }

    suspend fun submitDepositRequest(
        userId: String,
        amountInr: Long,
        rawUtr: String,
        userIgn: String,
        userPhone: String,
        userEmail: String,
    ): Result<String> {
        val normalizedUtr = rawUtr.trim().uppercase()
        if (amountInr !in 10L..500L) {
            return Result.failure(IllegalArgumentException("Amount must be between ₹10 and ₹500."))
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
                    "upiId" to MERCHANT_UPI_ID,
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
                "This UTR has already been used or cannot be verified. Duplicate UTRs are blocked."
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
                if (alreadyCredited) {
                    return@runTransaction 0L
                }

                val amount = depositSnap.getLong("amount") ?: 0L
                val prevDeposit = userSnap.getLong("walletDeposit") ?: 0L

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
                            "title" to "UPI Deposit (+$amount Pts)",
                            "category" to "₹$amount → $amount Pts • UTR: $normalizedUtr",
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
    ): Result<String> {
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
        if (amountInr < MIN_WITHDRAWAL_POINTS) {
            return Result.failure(IllegalArgumentException("Minimum $MIN_WITHDRAWAL_POINTS Winning Points (₹$MIN_WITHDRAWAL_POINTS) required to withdraw."))
        }
        if (cleanUpi.length < 5) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID or mobile number for 10 PM payout."))
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
                        "adminNote" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to userId,
                        "title" to "Withdrawal Pending (-₹$amountInr)",
                        "category" to "10 PM Payout • $cleanUid",
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

        return try {
            val assignedSlot = db.runTransaction { tx ->
                val snap = tx.get(tournamentRef)
                val existingReg = tx.get(regRef)
                val userSnap = tx.get(userRef)
                val existingEntryTx = tx.get(entryTxRef)
                if (existingReg.exists()) {
                    throw IllegalStateException("You have already joined ${tournament.title}.")
                }
                val currentFilled = snap.getLong("filledSlots") ?: 0L
                val maxSlots = snap.getLong("maxSlots") ?: 48L
                if (currentFilled >= maxSlots) {
                    throw IllegalStateException("Lobby is full! No slots remaining.")
                }

                val entryFeePts = (snap.getLong("entryFee") ?: tournament.entryFee).coerceAtLeast(0L)
                val prevDeposit = (userSnap.getLong("walletDeposit") ?: 0L).coerceAtLeast(0L)
                val prevBonusPoints = (userSnap.getLong("points") ?: 0L).coerceAtLeast(0L)
                val prevWinning = (userSnap.getLong("walletWinning") ?: 0L).coerceAtLeast(0L)
                val totalPlayable = prevDeposit + prevBonusPoints + prevWinning

                if (totalPlayable < entryFeePts) {
                    throw IllegalStateException(
                        "Insufficient Points! Need $entryFeePts Pts to join (you have $totalPlayable Pts). Add money in Wallet."
                    )
                }

                // Deduct entry fee in esports wallet priority: Deposit -> Spin Bonus -> Winnings
                var remaining = entryFeePts
                val deductDeposit = minOf(prevDeposit, remaining)
                remaining -= deductDeposit
                val deductBonus = minOf(prevBonusPoints, remaining)
                remaining -= deductBonus
                val deductWinning = minOf(prevWinning, remaining)

                val nextSlot = currentFilled + 1L
                val prevTournaments = userSnap.getLong("totalTournaments") ?: 0L
                val prevXp = userSnap.getLong("xp") ?: 0L
                val newXp = prevXp + 50L
                val newLevel = (newXp / 500L) + 1L

                val cleanTeammates = teammateUids.filter { it.isNotBlank() }.take(4)
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
                    "captainUid" to captainUid,
                    "captainIgn" to captainIgn,
                    "teammateUids" to cleanTeammates,
                    "screenshotSubmitted" to false,
                    "submittedKills" to 0L,
                    "screenshotUri" to "",
                    "resultStatus" to "Registered",
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
                        "points" to (prevBonusPoints - deductBonus),
                        "walletWinning" to (prevWinning - deductWinning),
                        "totalTournaments" to (prevTournaments + 1L),
                        "xp" to newXp,
                        "level" to newLevel,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                if (entryFeePts > 0L && !existingEntryTx.exists()) {
                    tx.set(
                        entryTxRef,
                        mapOf(
                            "userId" to userId,
                            "title" to "Joined: ${tournament.title}",
                            "category" to "${tournament.mode} • Entry (-$entryFeePts Pts)",
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

    suspend fun submitResultScreenshot(
        userId: String,
        tournamentId: String,
        kills: Long,
        screenshotUri: String,
    ): Result<Unit> {
        val regRef = db.collection(PlayerRepository.USERS_COLLECTION)
            .document(userId)
            .collection(REGISTRATIONS_SUBCOLLECTION)
            .document(tournamentId)
        return try {
            regRef.update(
                mapOf(
                    "screenshotSubmitted" to true,
                    "submittedKills" to kills,
                    "screenshotUri" to screenshotUri.take(450),
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
        if (unreadIds.isEmpty()) return
        try {
            val batch = db.batch()
            unreadIds.forEach { id ->
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
        val userRef = db.collection(PlayerRepository.USERS_COLLECTION).document(userId)
        return try {
            userRef.update(
                mapOf(
                    "squadName" to squadName.trim().take(40),
                    "squadTag" to squadTag.trim().uppercase().take(10),
                    "squadUids" to squadUids.map { it.trim() }.filter { it.isNotEmpty() }.take(4),
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

    companion object {
        const val MERCHANT_UPI_ID = "7378633612@fam"
        const val MIN_WITHDRAWAL_POINTS = 50L
        const val TOURNAMENTS_COLLECTION = "tournaments"
        const val LEADERBOARD_COLLECTION = "leaderboard"
        const val ADMIN_NOTIFICATIONS_COLLECTION = "adminNotifications"
        const val BANNERS_COLLECTION = "banners"
        const val DEPOSIT_REQUESTS_COLLECTION = "depositRequests"
        const val WITHDRAWAL_REQUESTS_COLLECTION = "withdrawalRequests"
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
            "^(tourn_|demo_|sample_|seed_|mock_|br_solo|br_duo|br_squad|cs_4v4|cs_clash|lw_|lone_wolf|lonewolf_).*"
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
