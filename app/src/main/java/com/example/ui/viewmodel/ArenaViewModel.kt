package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.example.data.repository.ArenaRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private fun Throwable.rethrowIfPureCancellation() {
    val firestoreCause = generateSequence(this) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreCause == null && this is CancellationException) throw this
}

class ArenaViewModel(
    private val repository: ArenaRepository,
    private val userId: String,
) : ViewModel() {

    private var lastSyncedLeaderboardSignature: String? = null
    private var lastSyncedParticipantMetaSignature: String? = null

    val tournaments: StateFlow<List<Tournament>> = repository.observeTournaments()
        .catch { error ->
            error.rethrowIfPureCancellation()
            _actionError.value = error.localizedMessage ?: "Unable to sync tournaments."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val registrations: StateFlow<List<TournamentRegistration>> = repository.observeRegistrations(userId)
        .catch { error ->
            error.rethrowIfPureCancellation()
            _actionError.value = error.localizedMessage ?: "Unable to sync your registrations."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val notifications: StateFlow<List<ArenaNotification>> = repository.observeNotifications(userId)
        .catch { error ->
            error.rethrowIfPureCancellation()
            _actionError.value = error.localizedMessage ?: "Unable to sync notifications."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val banners: StateFlow<List<HeroBanner>> = repository.observeBanners()
        .catch { error ->
            error.rethrowIfPureCancellation()
            /* Fall back silently to bundled TournaX banners if unavailable */
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val transactions: StateFlow<List<WalletTransaction>> = repository.observeTransactions(userId)
        .catch { error ->
            error.rethrowIfPureCancellation()
            _actionError.value = error.localizedMessage ?: "Unable to sync transactions."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val leaderboard: StateFlow<List<LeaderboardEntry>> = repository.observeLeaderboard()
        .catch { error ->
            error.rethrowIfPureCancellation()
            _actionError.value = error.localizedMessage ?: "Unable to sync leaderboard."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val depositRequests: StateFlow<List<DepositRequest>> = repository.observeDepositRequests(userId)
        .catch { error ->
            error.rethrowIfPureCancellation()
            _actionError.value = error.localizedMessage ?: "Unable to sync deposit verification status."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val withdrawalRequests: StateFlow<List<WithdrawalRequest>> = repository.observeWithdrawalRequests(userId)
        .catch { error ->
            error.rethrowIfPureCancellation()
            _actionError.value = error.localizedMessage ?: "Unable to sync withdrawal requests."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val walletConfig: StateFlow<WalletAppConfig> = repository.observeWalletConfig()
        .catch { error ->
            error.rethrowIfPureCancellation()
            emit(WalletAppConfig())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = WalletAppConfig(),
        )

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _pendingWithdrawSelfieBase64 = MutableStateFlow("")
    val pendingWithdrawSelfieBase64: StateFlow<String> = _pendingWithdrawSelfieBase64.asStateFlow()

    private val _pendingWithdrawSummary = MutableStateFlow<WithdrawalRequest?>(null)
    val pendingWithdrawSummary: StateFlow<WithdrawalRequest?> = _pendingWithdrawSummary.asStateFlow()

    private val claimingUtrs = mutableSetOf<String>()
    private val refundingWithdrawalIds = mutableSetOf<String>()
    private val refundingTournamentIds = mutableSetOf<String>()
    private val backfilledDepositUtrs = mutableSetOf<String>()
    private val backfilledWithdrawalIds = mutableSetOf<String>()
    private val backfilledCancelledTids = mutableSetOf<String>()
    private val matchResultsFlows = mutableMapOf<String, StateFlow<List<TournamentResultEntry>>>()
    private val matchParticipantsFlows = mutableMapOf<String, StateFlow<List<TournamentParticipant>>>()

    fun observeMatchResults(tournamentId: String): StateFlow<List<TournamentResultEntry>> {
        return matchResultsFlows.getOrPut(tournamentId) {
            repository.observeTournamentResults(tournamentId)
                .catch { error ->
                    error.rethrowIfPureCancellation()
                    _actionError.value = error.localizedMessage ?: "Unable to sync match points table."
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = emptyList(),
                )
        }
    }

    fun observeTournamentParticipants(tournamentId: String): StateFlow<List<TournamentParticipant>> {
        return matchParticipantsFlows.getOrPut(tournamentId) {
            repository.observeTournamentParticipants(tournamentId)
                .catch { error ->
                    error.rethrowIfPureCancellation()
                    _actionError.value = error.localizedMessage ?: "Unable to load joined members."
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000L),
                    initialValue = emptyList(),
                )
        }
    }

    fun ensureOwnParticipantSynced(
        tournamentId: String,
        registration: TournamentRegistration?,
        profile: PlayerProfile,
    ) {
        if (registration == null) return
        viewModelScope.launch {
            repository.syncMissingParticipantIfJoined(
                userId = userId,
                tournamentId = tournamentId,
                registration = registration,
                profile = profile,
            )
        }
    }

    init {
        viewModelScope.launch {
            depositRequests.collect { list ->
                list.filter { it.status.equals("approved", ignoreCase = true) }
                    .forEach { approved ->
                        val docUtr = approved.utr.ifBlank { approved.id }.trim().uppercase()
                        if (docUtr.isBlank()) return@forEach
                        if (!approved.credited) {
                            if (claimingUtrs.add(docUtr)) {
                                viewModelScope.launch {
                                    try {
                                        val res = repository.claimApprovedDeposit(userId = userId, utr = docUtr)
                                        res.onSuccess { creditedAmount ->
                                            if (creditedAmount > 0L) {
                                                _actionMessage.value = "₹$creditedAmount verified → +$creditedAmount Deposit Pts added to Wallet ✓"
                                            }
                                        }.onFailure { err ->
                                            _actionError.value = err.localizedMessage ?: "Could not credit approved deposit."
                                        }
                                    } finally {
                                        claimingUtrs.remove(docUtr)
                                    }
                                }
                            }
                        } else if (transactions.value.any { it.id == "dep_$docUtr" }) {
                            backfilledDepositUtrs.add(docUtr)
                        } else if (backfilledDepositUtrs.add(docUtr)) {
                            viewModelScope.launch {
                                repository.ensureApprovedDepositTransactionLogged(userId = userId, deposit = approved)
                            }
                        }
                    }
            }
        }

        viewModelScope.launch {
            withdrawalRequests.collect { list ->
                list.filter { it.status.equals("rejected", ignoreCase = true) }
                    .forEach { declined ->
                        val reqId = declined.id.trim()
                        if (reqId.isBlank()) return@forEach
                        if (!declined.refunded) {
                            if (refundingWithdrawalIds.add(reqId)) {
                                viewModelScope.launch {
                                    try {
                                        val res = repository.claimRejectedWithdrawalRefund(userId = userId, requestId = reqId)
                                        res.onSuccess { refundedAmount ->
                                            if (refundedAmount > 0L) {
                                                _actionMessage.value = "Withdrawal declined • ₹$refundedAmount refunded to Winning Balance ✓"
                                            }
                                        }.onFailure { err ->
                                            _actionError.value = err.localizedMessage ?: "Could not refund declined withdrawal."
                                        }
                                    } finally {
                                        refundingWithdrawalIds.remove(reqId)
                                    }
                                }
                            }
                        } else if (transactions.value.any { it.id == "wd_refund_$reqId" }) {
                            backfilledWithdrawalIds.add(reqId)
                        } else if (backfilledWithdrawalIds.add(reqId)) {
                            viewModelScope.launch {
                                repository.ensureRejectedWithdrawalRefundTxLogged(userId = userId, request = declined)
                            }
                        }
                    }
            }
        }

        viewModelScope.launch {
            combine(tournaments, registrations) { tList, rList ->
                val cancelledById = tList
                    .filter { it.status.equals("cancelled", ignoreCase = true) }
                    .associateBy { it.id }
                rList.mapNotNull { reg ->
                    val cancelledMatch = cancelledById[reg.tournamentId]
                    if (cancelledMatch != null && reg.entryFeePaid > 0L) {
                        cancelledMatch to reg
                    } else {
                        null
                    }
                }
            }.collect { pairs ->
                pairs.forEach { (match, reg) ->
                    val tid = reg.tournamentId.trim()
                    if (tid.isBlank()) return@forEach
                    if (!reg.refunded) {
                        if (refundingTournamentIds.add(tid)) {
                            viewModelScope.launch {
                                try {
                                    val res = repository.claimCancelledTournamentRefund(userId = userId, tournamentId = tid)
                                    res.onSuccess { refundedPts ->
                                        if (refundedPts > 0L) {
                                            _actionMessage.value = "Match cancelled (${match.title}) • +$refundedPts Pts refunded to Wallet ✓"
                                        }
                                    }.onFailure { err ->
                                        _actionError.value = err.localizedMessage ?: "Could not refund cancelled match entry."
                                    }
                                } finally {
                                    refundingTournamentIds.remove(tid)
                                }
                            }
                        }
                    } else if (transactions.value.any { it.id == "refund_$tid" }) {
                        backfilledCancelledTids.add(tid)
                    } else if (backfilledCancelledTids.add(tid)) {
                        viewModelScope.launch {
                            repository.ensureCancelledTournamentRefundTxLogged(userId = userId, reg = reg)
                        }
                    }
                }
            }
        }
    }

    fun syncRealLeaderboard(profile: PlayerProfile) {
        if (profile.userId.isBlank() || profile.freeFireIgn.isBlank()) return
        val signature = "${profile.userId}|${profile.freeFireIgn}|${profile.totalKills}|${profile.booyahs}|${profile.walletWinning}|${profile.avatarIndex}"
        if (signature == lastSyncedLeaderboardSignature) return
        lastSyncedLeaderboardSignature = signature
        viewModelScope.launch {
            repository.syncOwnLeaderboardEntry(profile)
        }
    }

    fun syncActiveParticipantsMetadata(profile: PlayerProfile) {
        val cleanIgn = profile.freeFireIgn.trim()
        if (profile.userId.isBlank() || cleanIgn.length < 2) return
        val activeStatuses = setOf("upcoming", "live")
        val activeIds = tournaments.value
            .filter { it.status.trim().lowercase() in activeStatuses }
            .map { it.id }
            .toSet()
        val joinedActiveIds = registrations.value
            .map { it.tournamentId }
            .filter { it in activeIds }
            .sorted()
        if (joinedActiveIds.isEmpty()) return
        val sig = "$cleanIgn|${profile.squadTag.trim()}|${profile.avatarIndex}|${joinedActiveIds.joinToString(",")}"
        if (sig == lastSyncedParticipantMetaSignature) return
        lastSyncedParticipantMetaSignature = sig
        viewModelScope.launch {
            repository.syncActiveParticipantsMetadata(
                userId = userId,
                activeTournamentIds = joinedActiveIds,
                profile = profile,
            )
        }
    }

    fun retryDepositClaim(utr: String) {
        val normalizedUtr = utr.trim().uppercase()
        if (normalizedUtr.isBlank() || !claimingUtrs.add(normalizedUtr)) return
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            try {
                val res = repository.claimApprovedDeposit(userId = userId, utr = normalizedUtr)
                res.onSuccess { creditedAmount ->
                    if (creditedAmount > 0L) {
                        _actionMessage.value = "₹$creditedAmount verified → +$creditedAmount Deposit Pts added to Wallet ✓"
                    } else {
                        _actionMessage.value = "Deposit UTR $normalizedUtr is already credited ✓"
                    }
                }.onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Could not credit approved deposit."
                }
            } finally {
                claimingUtrs.remove(normalizedUtr)
                _isBusy.value = false
            }
        }
    }

    fun retryWithdrawalRefund(requestId: String) {
        val cleanId = requestId.trim()
        if (cleanId.isBlank() || !refundingWithdrawalIds.add(cleanId)) return
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            try {
                val res = repository.claimRejectedWithdrawalRefund(userId = userId, requestId = cleanId)
                res.onSuccess { refundedAmount ->
                    if (refundedAmount > 0L) {
                        _actionMessage.value = "Withdrawal declined • ₹$refundedAmount refunded to Winning Balance ✓"
                    } else {
                        _actionMessage.value = "Withdrawal refund already processed ✓"
                    }
                }.onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Could not refund declined withdrawal."
                }
            } finally {
                refundingWithdrawalIds.remove(cleanId)
                _isBusy.value = false
            }
        }
    }

    fun retryCancelledMatchRefund(tournamentId: String, tournamentTitle: String) {
        val cleanTid = tournamentId.trim()
        if (cleanTid.isBlank() || !refundingTournamentIds.add(cleanTid)) return
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            try {
                val res = repository.claimCancelledTournamentRefund(userId = userId, tournamentId = cleanTid)
                res.onSuccess { refundedPts ->
                    if (refundedPts > 0L) {
                        _actionMessage.value = "Match cancelled (${tournamentTitle.ifBlank { cleanTid }}) • +$refundedPts Pts refunded to Wallet ✓"
                    } else {
                        _actionMessage.value = "Match entry refund already processed ✓"
                    }
                }.onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Could not refund cancelled match entry."
                }
            } finally {
                refundingTournamentIds.remove(cleanTid)
                _isBusy.value = false
            }
        }
    }

    fun setPendingWithdrawSelfie(base64: String) {
        _pendingWithdrawSelfieBase64.value = base64
    }

    fun setPendingWithdrawSummary(request: WithdrawalRequest?) {
        _pendingWithdrawSummary.value = request
    }

    fun clearPendingWithdrawDraft() {
        _pendingWithdrawSelfieBase64.value = ""
        _pendingWithdrawSummary.value = null
    }

    fun clearFeedback() {
        _actionMessage.value = null
        _actionError.value = null
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun showInfo(message: String) {
        _actionMessage.value = message
    }

    fun joinTournament(
        tournament: Tournament,
        captainUid: String,
        captainIgn: String,
        teammateUids: List<String>,
        onJoined: (Long) -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            _actionMessage.value = null
            val result = repository.joinTournament(
                userId = userId,
                tournament = tournament,
                captainUid = captainUid,
                captainIgn = captainIgn,
                teammateUids = teammateUids,
            )
            _isBusy.value = false
            result
                .onSuccess { slot ->
                    _actionMessage.value = "Slot #$slot confirmed for ${tournament.title}!"
                    onJoined(slot)
                }
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Could not join tournament."
                }
        }
    }

    fun submitScreenshot(
        tournamentId: String,
        kills: Long,
        screenshotUri: String,
        onSubmitted: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            val result = repository.submitResultScreenshot(
                userId = userId,
                tournamentId = tournamentId,
                kills = kills,
                screenshotUri = screenshotUri,
            )
            _isBusy.value = false
            result
                .onSuccess {
                    _actionMessage.value = "Match proof submitted for spectator verification."
                    onSubmitted()
                }
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Failed to submit screenshot."
                }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(userId, notifId)
        }
    }

    fun markAllNotificationsRead(unreadIds: List<String>) {
        if (unreadIds.isEmpty()) return
        viewModelScope.launch {
            repository.markAllNotificationsRead(userId, unreadIds)
        }
    }

    fun updateAvatar(avatarIndex: Long) {
        viewModelScope.launch {
            val result = repository.updateAvatar(userId, avatarIndex)
            result
                .onSuccess { _actionMessage.value = "Avatar updated!" }
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Unable to update avatar."
                }
        }
    }

    fun updateSquad(
        squadName: String,
        squadTag: String,
        squadUids: List<String>,
        onSaved: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            val result = repository.updateSquad(
                userId = userId,
                squadName = squadName,
                squadTag = squadTag,
                squadUids = squadUids,
            )
            _isBusy.value = false
            result
                .onSuccess {
                    _actionMessage.value = "Squad roster saved!"
                    onSaved()
                }
                .onFailure { err ->
                    _actionError.value = err.localizedMessage ?: "Unable to save squad."
                }
        }
    }

    fun submitDepositRequest(
        amountInr: Long,
        utr: String,
        profile: PlayerProfile,
        userEmail: String?,
        onSubmitted: (normalizedUtr: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            val cfg = walletConfig.value
            val result = repository.submitDepositRequest(
                userId = userId,
                amountInr = amountInr,
                rawUtr = utr,
                userIgn = profile.freeFireIgn.ifBlank { profile.fullName },
                userPhone = profile.phone,
                userEmail = userEmail.orEmpty(),
                merchantUpiId = cfg.merchantUpiId,
                minDepositInr = cfg.minDepositInr,
                maxDepositInr = cfg.maxDepositInr,
            )
            _isBusy.value = false
            result
                .onSuccess { normalizedUtr ->
                    onSubmitted(normalizedUtr)
                }
                .onFailure { err ->
                    val message = err.localizedMessage ?: "Failed to submit UTR."
                    _actionError.value = message
                    onError(message)
                }
        }
    }

    fun submitWithdrawalRequest(
        fullName: String,
        freeFireUid: String,
        upiId: String,
        amountInr: Long,
        selfieBase64: String,
        profile: PlayerProfile,
        userEmail: String?,
        onSubmitted: (requestId: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            val cfg = walletConfig.value
            val result = repository.submitWithdrawalRequest(
                userId = userId,
                fullName = fullName,
                freeFireUid = freeFireUid,
                freeFireIgn = profile.freeFireIgn,
                userPhone = profile.phone,
                userEmail = userEmail.orEmpty(),
                upiId = upiId,
                amountInr = amountInr,
                selfieBase64 = selfieBase64,
                minWithdrawalPoints = cfg.minWithdrawalPoints,
            )
            _isBusy.value = false
            result
                .onSuccess { requestId ->
                    _actionMessage.value = "Withdrawal request pending • Host will pay at 10:00 PM!"
                    onSubmitted(requestId)
                }
                .onFailure { err ->
                    val message = err.localizedMessage ?: "Failed to submit withdrawal request."
                    _actionError.value = message
                    onError(message)
                }
        }
    }

    fun submitSupportTicket(
        profile: PlayerProfile,
        userEmail: String?,
        category: String,
        suspectUid: String,
        notes: String,
        onSubmitted: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _actionError.value = null
            val result = repository.submitSupportTicket(
                userId = userId,
                profile = profile,
                userEmail = userEmail.orEmpty(),
                category = category,
                suspectUid = suspectUid,
                notes = notes,
            )
            _isBusy.value = false
            result
                .onSuccess {
                    _actionMessage.value = "Support ticket submitted! Moderators will review shortly."
                    onSubmitted()
                }
                .onFailure { err ->
                    val message = err.localizedMessage ?: "Failed to submit support ticket."
                    _actionError.value = message
                    onError(message)
                }
        }
    }
}
