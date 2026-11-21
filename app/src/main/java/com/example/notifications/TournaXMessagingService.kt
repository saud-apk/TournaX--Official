package com.example.notifications

import com.example.data.repository.PlayerRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TournaXMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        try {
            Firebase.messaging.subscribeToTopic(TournaXNotificationHelper.TOPIC_ALL_PLAYERS)
        } catch (_: Exception) {
        }
        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        }
        val safeToken = token.trim()
        if (!uid.isNullOrBlank() && safeToken.isNotBlank()) {
            val repo = PlayerRepository(applicationContext)
            serviceScope.launch {
                repo.updateFcmToken(userId = uid, token = safeToken)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val signedIn = try {
            Firebase.auth.currentUser != null
        } catch (_: Exception) {
            false
        }
        if (!signedIn) return

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "TournaX Arena"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: return
        val category = message.data["category"].orEmpty()

        TournaXNotificationHelper.showPushNotification(
            context = applicationContext,
            title = title,
            body = body,
            category = category,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
