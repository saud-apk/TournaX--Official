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
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.repository.ArenaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArenaViewModel(
    private val repository: ArenaRepository,
    private val userId: String,
) : ViewModel() {

    private var lastSyncedLeaderboardSignature: String? = null

    val tournaments: StateFlow<List<Tournament>> = repository.observeTournaments()
        .catch { error ->
            _actionError.value = error.localizedMessage ?: "Unable to sync tournaments."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val registrations: StateFlow<List<TournamentRegistration>> = repository.observeRegistrations(userId)
        .catch { error ->
            _actionError.value = error.localizedMessage ?: "Unable to sync your registrations."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val notifications: StateFlow<List<ArenaNotification>> = repository.observeNotifications(userId)
        .catch { error ->
            _actionError.value = error.localizedMessage ?: "Unable to sync notifications."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val banners: StateFlow<List<HeroBanner>> = repository.observeBanners()
        .catch { /* Fall back silently to bundled TournaX banners if unavailable */ }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val transactions: StateFlow<List<WalletTransaction>> = repository.observeTransactions(userId)
        .catch { error ->
            _actionError.value = error.localizedMessage ?: "Unable to sync transactions."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val leaderboard: StateFlow<List<LeaderboardEntry>> = repository.observeLeaderboard()
        .catch { error ->
            _actionError.value = error.localizedMessage ?: "Unable to sync leaderboard."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val depositRequests: StateFlow<List<DepositRequest>> = repository.observeDepositRequests(userId)
        .catch { error ->
            _actionError.value = error.localizedMessage ?: "Unable to sync deposit verification status."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val withdrawalRequests: StateFlow<List<WithdrawalRequest>> = repository.observeWithdrawalRequests(userId)
        .catch { error ->
            _actionError.value = error.localizedMessage ?: "Unable to sync withdrawal requests."
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val claimingUtrs = mutableSetOf<String>()
    private val matchResultsFlows = mutableMapOf<String, StateFlow<List<TournamentResultEntry>>>()
    private val matchParticipantsFlows = mutableMapOf<String, StateFlow<List<TournamentParticipant>>>()

    fun observeMatchResults(tournamentId: String): StateFlow<List<TournamentResultEntry>> {
        return matchResultsFlows.getOrPut(tournamentId) {
            repository.observeTournamentResults(tournamentId)
                .catch { error ->
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
            depositRequests.collectLatest { list ->
                list.filter { it.status.equals("approved", ignoreCase = true) && !it.credited }
                    .forEach { approved ->
                        val docUtr = approved.utr.ifBlank { approved.id }.trim().uppercase()
                        if (docUtr.isNotBlank() && claimingUtrs.add(docUtr)) {
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

    fun clearFeedback() {
        _actionMessage.value = null
        _actionError.value = null
    }

    fun showInfo(message: String) {
        _actionError.value = null
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
            val result = repository.submitDepositRequest(
                userId = userId,
                amountInr = amountInr,
                rawUtr = utr,
                userIgn = profile.freeFireIgn.ifBlank { profile.fullName },
                userPhone = profile.phone,
                userEmail = userEmail.orEmpty(),
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
}
