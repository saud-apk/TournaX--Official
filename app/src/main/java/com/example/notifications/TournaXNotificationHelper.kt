package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.PlayerRepository
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicInteger

object TournaXNotificationHelper {

    const val CHANNEL_ARENA_ALERTS = "tournax_arena_alerts"
    const val CHANNEL_WALLET_PAYOUTS = "tournax_wallet_payouts"
    const val TOPIC_ALL_PLAYERS = "all_players"
    const val EXTRA_NOTIFICATION_CATEGORY = "tournax_notification_category"

    private val requestCodeGenerator = AtomicInteger(1000)

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val arenaChannel = NotificationChannel(
            CHANNEL_ARENA_ALERTS,
            "Match & Room Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Room ID & Password drops, match start reminders, and Points Table updates."
            enableVibration(true)
        }

        val walletChannel = NotificationChannel(
            CHANNEL_WALLET_PAYOUTS,
            "Wallet & 10 PM Payouts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "UPI deposit approvals, 10 PM withdrawal payouts, and prize settlements."
            enableVibration(true)
        }

        manager.createNotificationChannel(arenaChannel)
        manager.createNotificationChannel(walletChannel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showPushNotification(
        context: Context,
        title: String,
        body: String,
        category: String = "",
    ) {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return

        val normalizedCategory = category.trim().lowercase()
        val channelId = when (normalizedCategory) {
            "wallet", "deposit", "withdrawal", "payout", "prize" -> CHANNEL_WALLET_PAYOUTS
            else -> CHANNEL_ARENA_ALERTS
        }

        val notificationId = requestCodeGenerator.incrementAndGet()
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (normalizedCategory.isNotEmpty()) {
                putExtra(EXTRA_NOTIFICATION_CATEGORY, normalizedCategory)
            }
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            pendingFlags,
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_tournax_notification)
            .setColor(ContextCompat.getColor(context, R.color.tournax_orange))
            .setContentTitle(title.ifBlank { "TournaX Arena" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(
                notificationId,
                builder.build(),
            )
        } catch (_: SecurityException) {
            // Permission revoked mid-flight; safely ignore
        }
    }

    suspend fun syncFcmTokenAndTopic(
        playerRepository: PlayerRepository,
        userId: String,
    ) {
        if (userId.isBlank()) return
        try {
            Firebase.messaging.subscribeToTopic(TOPIC_ALL_PLAYERS).await()
        } catch (_: Exception) {
            // Safely ignore in offline/robolectric environments
        }
        try {
            val token = Firebase.messaging.token.await()
            if (!token.isNullOrBlank()) {
                playerRepository.updateFcmToken(userId = userId, token = token)
            }
        } catch (_: Exception) {
            // Safely ignore if Play Services or FCM is unavailable
        }
    }
}
