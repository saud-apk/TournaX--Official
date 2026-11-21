package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

internal fun DocumentSnapshot.safeString(key: String, default: String = ""): String {
    val raw = try {
        get(key)
    } catch (_: Exception) {
        null
    } ?: return default
    return when (raw) {
        is String -> raw
        is Number, is Boolean -> raw.toString()
        else -> default
    }
}

internal fun DocumentSnapshot.safeLong(key: String, default: Long = 0L): Long {
    val raw = try {
        get(key)
    } catch (_: Exception) {
        null
    } ?: return default
    return when (raw) {
        is Number -> raw.toLong()
        is String -> raw.trim().toLongOrNull() ?: default
        else -> default
    }
}

internal fun DocumentSnapshot.safeBoolean(key: String, default: Boolean = false): Boolean {
    val raw = try {
        get(key)
    } catch (_: Exception) {
        null
    } ?: return default
    return when (raw) {
        is Boolean -> raw
        is Number -> raw.toLong() != 0L
        is String -> raw.trim().equals("true", ignoreCase = true)
        else -> default
    }
}

internal fun DocumentSnapshot.safeStringList(key: String): List<String> {
    val raw = try {
        get(key)
    } catch (_: Exception) {
        null
    }
    return (raw as? List<*>)?.mapNotNull { item ->
        (item as? String) ?: item?.toString()
    } ?: emptyList()
}

internal fun DocumentSnapshot.safeTimestamp(key: String): Timestamp? {
    val raw = try {
        get(key, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (_: Exception) {
        null
    } ?: return null
    return when (raw) {
        is Timestamp -> raw
        is Date -> Timestamp(raw)
        is Number -> {
            val ms = raw.toLong()
            if (ms > 0L) Timestamp(Date(ms)) else null
        }
        else -> null
    }
}

fun DocumentSnapshot.toSafePlayerProfile(): PlayerProfile = PlayerProfile(
    id = id,
    userId = safeString("userId", id),
    fullName = safeString("fullName"),
    phone = safeString("phone"),
    freeFireUid = safeString("freeFireUid"),
    freeFireIgn = safeString("freeFireIgn"),
    termsAccepted = safeBoolean("termsAccepted", false),
    termsVersion = safeString("termsVersion", "2026-v1"),
    verificationMethod = safeString("verificationMethod", "google_auth"),
    level = safeLong("level", 1L),
    xp = safeLong("xp", 0L),
    points = safeLong("points", 0L),
    totalTournaments = safeLong("totalTournaments", 0L),
    totalKills = safeLong("totalKills", 0L),
    booyahs = safeLong("booyahs", 0L),
    avatarIndex = safeLong("avatarIndex", 0L),
    squadName = safeString("squadName"),
    squadTag = safeString("squadTag"),
    squadUids = safeStringList("squadUids"),
    lastDailySpinDate = safeString("lastDailySpinDate"),
    lastDailySpinAt = safeTimestamp("lastDailySpinAt"),
    referralCode = safeString("referralCode"),
    walletDeposit = safeLong("walletDeposit", 0L),
    walletWinning = safeLong("walletWinning", 0L),
    lastApprovedUtr = safeString("lastApprovedUtr"),
    lastRefundedWithdrawalId = safeString("lastRefundedWithdrawalId"),
    lastRefundedTournamentId = safeString("lastRefundedTournamentId"),
    lastJoinedTournamentId = safeString("lastJoinedTournamentId"),
    trustScore = safeLong("trustScore", 100L),
    kycStatus = safeString("kycStatus", "Pending"),
    fcmToken = safeString("fcmToken"),
    accountStatus = safeString("accountStatus", "active").trim().lowercase().ifBlank { "active" },
    banReason = safeString("banReason"),
    suspendedUntil = safeTimestamp("suspendedUntil"),
    bannedAt = safeTimestamp("bannedAt"),
    bannedBy = safeString("bannedBy"),
    createdAt = safeTimestamp("createdAt"),
    updatedAt = safeTimestamp("updatedAt"),
)

fun DocumentSnapshot.toSafeTournament(): Tournament = Tournament(
    id = id,
    title = safeString("title"),
    mode = safeString("mode"),
    mapName = safeString("mapName"),
    dateTimeLabel = safeString("dateTimeLabel"),
    status = safeString("status", "upcoming"),
    entryFee = safeLong("entryFee", 0L),
    prizePool = safeLong("prizePool", 0L),
    perKill = safeLong("perKill", 0L),
    prizeFirst = safeLong("prizeFirst", 0L),
    prizeSecond = safeLong("prizeSecond", 0L),
    prizeThird = safeLong("prizeThird", 0L),
    maxSlots = safeLong("maxSlots", 48L),
    filledSlots = safeLong("filledSlots", 0L),
    slotUnitLabel = safeString("slotUnitLabel", "Players"),
    rulesSummary = safeString("rulesSummary"),
    roomId = safeString("roomId"),
    roomPassword = safeString("roomPassword"),
    roomUnlocked = safeBoolean("roomUnlocked", false),
    resultsPublished = safeBoolean("resultsPublished", false),
    mvpIgn = safeString("mvpIgn"),
    isDemo = safeBoolean("isDemo", false),
    createdAt = safeTimestamp("createdAt"),
)

fun DocumentSnapshot.toSafeTournamentRegistration(): TournamentRegistration = TournamentRegistration(
    id = id,
    userId = safeString("userId"),
    tournamentId = safeString("tournamentId", id),
    tournamentTitle = safeString("tournamentTitle"),
    mode = safeString("mode"),
    mapName = safeString("mapName"),
    dateTimeLabel = safeString("dateTimeLabel"),
    slotNumber = safeLong("slotNumber", 1L),
    captainUid = safeString("captainUid"),
    captainIgn = safeString("captainIgn"),
    teammateUids = safeStringList("teammateUids"),
    screenshotSubmitted = safeBoolean("screenshotSubmitted", false),
    submittedKills = safeLong("submittedKills", 0L),
    screenshotUri = safeString("screenshotUri"),
    resultStatus = safeString("resultStatus", "Registered"),
    entryFeePaid = safeLong("entryFeePaid", 0L),
    depositPaid = safeLong("depositPaid", 0L),
    winningPaid = safeLong("winningPaid", 0L),
    refunded = safeBoolean("refunded", false),
    createdAt = safeTimestamp("createdAt"),
    updatedAt = safeTimestamp("updatedAt"),
)

fun DocumentSnapshot.toSafeArenaNotification(): ArenaNotification = ArenaNotification(
    id = id,
    title = safeString("title"),
    body = safeString("body"),
    category = safeString("category", "admin"),
    priority = safeString("priority", "normal"),
    targetUserId = safeString("targetUserId", "all"),
    linkUrl = safeString("linkUrl"),
    isRead = safeBoolean("isRead", false),
    createdAt = safeTimestamp("createdAt"),
)

fun DocumentSnapshot.toSafeWalletTransaction(): WalletTransaction = WalletTransaction(
    id = id,
    userId = safeString("userId"),
    title = safeString("title"),
    category = safeString("category", "Deposit"),
    amount = safeLong("amount", 0L),
    isCredit = safeBoolean("isCredit", true),
    status = safeString("status", "Credited"),
    createdAt = safeTimestamp("createdAt"),
)

fun DocumentSnapshot.toSafeDepositRequest(): DepositRequest = DepositRequest(
    id = id,
    userId = safeString("userId"),
    userIgn = safeString("userIgn"),
    userPhone = safeString("userPhone"),
    userEmail = safeString("userEmail"),
    utr = safeString("utr", id),
    upiId = safeString("upiId", "7378633612@fam"),
    amount = safeLong("amount", 0L),
    status = safeString("status", "pending"),
    credited = safeBoolean("credited", false),
    adminNote = safeString("adminNote"),
    createdAt = safeTimestamp("createdAt"),
    updatedAt = safeTimestamp("updatedAt"),
)

fun DocumentSnapshot.toSafeWithdrawalRequest(): WithdrawalRequest = WithdrawalRequest(
    id = id,
    userId = safeString("userId"),
    fullName = safeString("fullName"),
    freeFireUid = safeString("freeFireUid"),
    freeFireIgn = safeString("freeFireIgn"),
    userPhone = safeString("userPhone"),
    userEmail = safeString("userEmail"),
    upiId = safeString("upiId"),
    amount = safeLong("amount", 0L),
    selfieBase64 = safeString("selfieBase64"),
    status = safeString("status", "pending"),
    refunded = safeBoolean("refunded", false),
    adminNote = safeString("adminNote"),
    createdAt = safeTimestamp("createdAt"),
    updatedAt = safeTimestamp("updatedAt"),
)

fun DocumentSnapshot.toSafeWalletAppConfig(): WalletAppConfig {
    val rawUpi = safeString("merchantUpiId").trim()
    val validUpi = if (UPI_HANDLE_REGEX.matches(rawUpi) && rawUpi.length in 5..80) {
        rawUpi
    } else {
        "7378633612@fam"
    }
    val minWithdraw = safeLong("minWithdrawalPoints", 50L).coerceIn(10L, 10_000L)
    val minDeposit = safeLong("minDepositInr", 10L).coerceIn(1L, 50_000L)
    val maxDeposit = safeLong("maxDepositInr", 500L).coerceIn(minDeposit, 50_000L)
    return WalletAppConfig(
        id = id.ifBlank { "wallet" },
        merchantUpiId = validUpi,
        minWithdrawalPoints = minWithdraw,
        minDepositInr = minDeposit,
        maxDepositInr = maxDeposit,
    )
}

private val UPI_HANDLE_REGEX = Regex("^[a-zA-Z0-9._-]{2,64}@[a-zA-Z0-9.-]{2,32}$")

fun DocumentSnapshot.toSafeLeaderboardEntry(): LeaderboardEntry = LeaderboardEntry(
    id = id,
    ign = safeString("ign"),
    kills = safeLong("kills", 0L),
    booyahs = safeLong("booyahs", 0L),
    winningsInr = safeLong("winningsInr", 0L),
    avatarIndex = safeLong("avatarIndex", 0L),
    updatedAt = safeTimestamp("updatedAt"),
)

fun DocumentSnapshot.toSafeTournamentResultEntry(): TournamentResultEntry = TournamentResultEntry(
    id = id,
    tournamentId = safeString("tournamentId"),
    rank = safeLong("rank", 0L),
    playerOrTeamName = safeString("playerOrTeamName"),
    squadTag = safeString("squadTag"),
    freeFireUid = safeString("freeFireUid"),
    userId = safeString("userId"),
    slotNumber = safeLong("slotNumber", 0L),
    booyahs = safeLong("booyahs", 0L),
    placementPoints = safeLong("placementPoints", 0L),
    kills = safeLong("kills", 0L),
    killPoints = safeLong("killPoints", 0L),
    totalPoints = safeLong("totalPoints", 0L),
    prizeWon = safeLong("prizeWon", 0L),
    isMvp = safeBoolean("isMvp", false),
    updatedAt = safeTimestamp("updatedAt"),
)

fun DocumentSnapshot.toSafeTournamentParticipant(): TournamentParticipant = TournamentParticipant(
    id = id,
    userId = safeString("userId", id),
    tournamentId = safeString("tournamentId"),
    slotNumber = safeLong("slotNumber", 1L),
    captainIgn = safeString("captainIgn"),
    captainUid = safeString("captainUid"),
    squadTag = safeString("squadTag"),
    avatarIndex = safeLong("avatarIndex", 0L).coerceIn(0L, 3L),
    teammateUids = safeStringList("teammateUids"),
    joinedAt = safeTimestamp("joinedAt"),
)

fun DocumentSnapshot.toSafeHeroBanner(): HeroBanner = HeroBanner(
    id = id,
    badge = safeString("badge"),
    title = safeString("title"),
    subtitle = safeString("subtitle"),
    ctaLabel = safeString("ctaLabel", "Open Link"),
    imageUrl = safeString("imageUrl"),
    linkUrl = safeString("linkUrl"),
    order = safeLong("order", 0L),
    active = safeBoolean("active", true),
)

fun DocumentSnapshot.toSafeSupportTicket(): SupportTicket = SupportTicket(
    id = id,
    userId = safeString("userId"),
    category = safeString("category", "general"),
    suspectUid = safeString("suspectUid"),
    notes = safeString("notes"),
    status = safeString("status", "pending"),
    userIgn = safeString("userIgn"),
    userPhone = safeString("userPhone"),
    userEmail = safeString("userEmail"),
    adminNote = safeString("adminNote"),
    createdAt = safeTimestamp("createdAt"),
    updatedAt = safeTimestamp("updatedAt"),
)
