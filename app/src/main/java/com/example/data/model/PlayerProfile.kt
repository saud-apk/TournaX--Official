package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String = "",
    val freeFireUid: String = "",
    val freeFireIgn: String = "",
    val termsAccepted: Boolean = false,
    val termsVersion: String = "2026-v1",
    val verificationMethod: String = "google_auth",
    val level: Long = 1L,
    val xp: Long = 0L,
    val points: Long = 0L,
    val totalTournaments: Long = 0L,
    val totalKills: Long = 0L,
    val booyahs: Long = 0L,
    val avatarIndex: Long = 0L,
    val squadName: String = "",
    val squadTag: String = "",
    val squadUids: List<String> = emptyList(),
    val lastDailySpinDate: String = "",
    val lastDailySpinAt: Timestamp? = null,
    val referralCode: String = "",
    val walletDeposit: Long = 0L,
    val walletWinning: Long = 0L,
    val lastApprovedUtr: String = "",
    val trustScore: Long = 100L,
    val kycStatus: String = "Pending",
    val fcmToken: String = "",
    val accountStatus: String = "active",
    val banReason: String = "",
    val suspendedUntil: Timestamp? = null,
    val bannedAt: Timestamp? = null,
    val bannedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    @get:Exclude
    val isBanned: Boolean
        get() = accountStatus.trim().equals("banned", ignoreCase = true)

    @Exclude
    fun isActivelySuspended(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!accountStatus.trim().equals("suspended", ignoreCase = true)) return false
        val untilMillis = suspendedUntil?.toDate()?.time ?: return true
        return nowMillis < untilMillis
    }

    @Exclude
    fun isRestricted(nowMillis: Long = System.currentTimeMillis()): Boolean =
        isBanned || isActivelySuspended(nowMillis)
}
