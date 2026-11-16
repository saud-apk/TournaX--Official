package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val title: String = "",
    val mode: String = "",
    val mapName: String = "",
    val dateTimeLabel: String = "",
    val status: String = "upcoming",
    val entryFee: Long = 0L,
    val prizePool: Long = 0L,
    val perKill: Long = 0L,
    val prizeFirst: Long = 0L,
    val prizeSecond: Long = 0L,
    val prizeThird: Long = 0L,
    val maxSlots: Long = 48L,
    val filledSlots: Long = 0L,
    val slotUnitLabel: String = "Players",
    val rulesSummary: String = "",
    val roomId: String = "",
    val roomPassword: String = "",
    val roomUnlocked: Boolean = false,
    val resultsPublished: Boolean = false,
    val mvpIgn: String = "",
    @get:PropertyName("isDemo")
    @field:PropertyName("isDemo")
    val isDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TournamentRegistration(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val mode: String = "",
    val mapName: String = "",
    val dateTimeLabel: String = "",
    val slotNumber: Long = 1L,
    val captainUid: String = "",
    val captainIgn: String = "",
    val teammateUids: List<String> = emptyList(),
    val screenshotSubmitted: Boolean = false,
    val submittedKills: Long = 0L,
    val screenshotUri: String = "",
    val resultStatus: String = "Registered",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ArenaNotification(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "admin",
    val priority: String = "normal",
    val targetUserId: String = "all",
    val linkUrl: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "Deposit",
    val amount: Long = 0L,
    val isCredit: Boolean = true,
    val status: String = "Credited",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class DepositRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userIgn: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val utr: String = "",
    val upiId: String = "7378633612@fam",
    val amount: Long = 0L,
    val status: String = "pending",
    val credited: Boolean = false,
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val freeFireUid: String = "",
    val freeFireIgn: String = "",
    val userPhone: String = "",
    val userEmail: String = "",
    val upiId: String = "",
    val amount: Long = 0L,
    val selfieBase64: String = "",
    val status: String = "pending",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val ign: String = "",
    val kills: Long = 0L,
    val booyahs: Long = 0L,
    val winningsInr: Long = 0L,
    val avatarIndex: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TournamentResultEntry(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val rank: Long = 0L,
    val playerOrTeamName: String = "",
    val squadTag: String = "",
    val freeFireUid: String = "",
    val userId: String = "",
    val slotNumber: Long = 0L,
    val booyahs: Long = 0L,
    val placementPoints: Long = 0L,
    val kills: Long = 0L,
    val killPoints: Long = 0L,
    val totalPoints: Long = 0L,
    val prizeWon: Long = 0L,
    @get:PropertyName("isMvp")
    @field:PropertyName("isMvp")
    val isMvp: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    @get:Exclude
    val effectiveKillPoints: Long
        get() = if (killPoints > 0L) killPoints else kills

    @get:Exclude
    val effectiveTotalPoints: Long
        get() = if (totalPoints > 0L) totalPoints else (placementPoints + effectiveKillPoints)
}

@IgnoreExtraProperties
data class TournamentParticipant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tournamentId: String = "",
    val slotNumber: Long = 1L,
    val captainIgn: String = "",
    val captainUid: String = "",
    val squadTag: String = "",
    val avatarIndex: Long = 0L,
    val teammateUids: List<String> = emptyList(),
    @ServerTimestamp val joinedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class HeroBanner(
    @DocumentId val id: String = "",
    val badge: String = "",
    val title: String = "",
    val subtitle: String = "",
    val ctaLabel: String = "Open Link",
    val imageUrl: String = "",
    val linkUrl: String = "",
    val order: Long = 0L,
    val active: Boolean = true,
)
