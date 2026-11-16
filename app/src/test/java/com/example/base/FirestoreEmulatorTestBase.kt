package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth
    protected lateinit var databaseId: String

    @Before
    open fun setUpFirebase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
        databaseId = if (resId != 0) context.getString(resId) else "demo-database"

        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            val newApp = FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.officialtournax")
                    .setProjectId(PROJECT_ID)
                    .setApiKey("fake-api-key-for-emulator")
                    .build()
            )
            val newFirestore = FirebaseFirestore.getInstance(newApp, databaseId)
            newFirestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
            newFirestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()

            val newAuth = FirebaseAuth.getInstance(newApp)
            newAuth.useEmulator(EMULATOR_HOST, AUTH_PORT)
            newApp
        } else {
            FirebaseApp.getInstance()
        }

        firestore = FirebaseFirestore.getInstance(app, databaseId)
        auth = FirebaseAuth.getInstance(app)
    }

    @After
    open fun tearDownFirebase() {
        auth.signOut()
    }

    protected suspend fun signInTestUser(email: String): String = withContext(Dispatchers.IO) {
        withTimeout(AUTH_TIMEOUT_MS) {
            val result = try {
                auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            } catch (unused: FirebaseAuthInvalidUserException) {
                auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
            }
            val user = checkNotNull(result.user) { "User auth failed" }
            user.getIdToken(true).await()
            kotlinx.coroutines.delay(200)
            user.uid
        }
    }

    protected suspend fun seedTournamentOnEmulator(
        tournamentId: String,
        title: String,
        mode: String = "BR Solo",
        entryFee: Long,
        prizePool: Long,
        perKill: Long = 5L,
        roomId: String,
        roomPassword: String,
        isDemo: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        val encodedDb = URLEncoder.encode(databaseId, "UTF-8")
        val encodedDoc = URLEncoder.encode(tournamentId, "UTF-8")
        val endpoint = "http://$EMULATOR_HOST:$FIRESTORE_PORT/v1/projects/$PROJECT_ID/databases/$encodedDb/documents/tournaments?documentId=$encodedDoc"
        val prizeFirst = (prizePool * 60L) / 100L
        val prizeSecond = (prizePool * 25L) / 100L
        val prizeThird = prizePool - prizeFirst - prizeSecond
        val payload = """
            {
              "fields": {
                "title": { "stringValue": "${escapeJson(title)}" },
                "mode": { "stringValue": "${escapeJson(mode)}" },
                "mapName": { "stringValue": "Bermuda" },
                "dateTimeLabel": { "stringValue": "Today • 8:00 PM IST" },
                "status": { "stringValue": "upcoming" },
                "entryFee": { "integerValue": "$entryFee" },
                "prizePool": { "integerValue": "$prizePool" },
                "perKill": { "integerValue": "$perKill" },
                "prizeFirst": { "integerValue": "$prizeFirst" },
                "prizeSecond": { "integerValue": "$prizeSecond" },
                "prizeThird": { "integerValue": "$prizeThird" },
                "maxSlots": { "integerValue": "48" },
                "filledSlots": { "integerValue": "4" },
                "slotUnitLabel": { "stringValue": "Players" },
                "rulesSummary": { "stringValue": "Mobile only. No hacks or emulators." },
                "roomId": { "stringValue": "${escapeJson(roomId)}" },
                "roomPassword": { "stringValue": "${escapeJson(roomPassword)}" },
                "roomUnlocked": { "booleanValue": false },
                "isDemo": { "booleanValue": $isDemo },
                "createdAt": { "timestampValue": "2026-11-14T00:00:00Z" }
              }
            }
        """.trimIndent()

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer owner")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
            }
            val code = connection.responseCode
            check(code in 200..299) {
                val errText = connection.errorStream?.bufferedReader()?.readText().orEmpty()
                "Failed to seed tournament on emulator ($code): $errText"
            }
        } finally {
            connection.disconnect()
        }
    }

    protected suspend fun creditWinningOnEmulator(
        userId: String,
        winningAmount: Long,
    ) = withContext(Dispatchers.IO) {
        val encodedDb = URLEncoder.encode(databaseId, "UTF-8")
        val encodedUid = URLEncoder.encode(userId, "UTF-8")
        val endpoint = "http://$EMULATOR_HOST:$FIRESTORE_PORT/v1/projects/$PROJECT_ID/databases/$encodedDb/documents/users/$encodedUid?updateMask.fieldPaths=walletWinning"
        val payload = """
            {
              "fields": {
                "walletWinning": { "integerValue": "$winningAmount" }
              }
            }
        """.trimIndent()

        val client = java.net.http.HttpClient.newHttpClient()
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(endpoint))
            .header("Authorization", "Bearer owner")
            .header("Content-Type", "application/json; charset=UTF-8")
            .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(payload, Charsets.UTF_8))
            .build()
        val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "Failed to credit winning balance on emulator (${response.statusCode()}): ${response.body()}"
        }
    }

    protected suspend fun moderateUserOnEmulator(
        userId: String,
        accountStatus: String,
        banReason: String,
        bannedBy: String = "admin_panel",
    ) = withContext(Dispatchers.IO) {
        val encodedDb = URLEncoder.encode(databaseId, "UTF-8")
        val encodedUid = URLEncoder.encode(userId, "UTF-8")
        val endpoint = "http://$EMULATOR_HOST:$FIRESTORE_PORT/v1/projects/$PROJECT_ID/databases/$encodedDb/documents/users/$encodedUid" +
            "?updateMask.fieldPaths=accountStatus&updateMask.fieldPaths=banReason&updateMask.fieldPaths=bannedBy"
        val payload = """
            {
              "fields": {
                "accountStatus": { "stringValue": "${escapeJson(accountStatus)}" },
                "banReason": { "stringValue": "${escapeJson(banReason)}" },
                "bannedBy": { "stringValue": "${escapeJson(bannedBy)}" }
              }
            }
        """.trimIndent()

        val client = java.net.http.HttpClient.newHttpClient()
        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(endpoint))
            .header("Authorization", "Bearer owner")
            .header("Content-Type", "application/json; charset=UTF-8")
            .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(payload, Charsets.UTF_8))
            .build()
        val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() in 200..299) {
            "Failed to moderate user on emulator (${response.statusCode()}): ${response.body()}"
        }
    }

    private fun escapeJson(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

    protected fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
        var current: Throwable? = this
        while (current != null) {
            if (current is FirebaseFirestoreException) return current
            current = current.cause
        }
        return null
    }

    private companion object {
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099
        const val PROJECT_ID = "demo-no-project"
        const val DEFAULT_PASSWORD = "password123"
        const val AUTH_TIMEOUT_MS = 5000L
    }
}
