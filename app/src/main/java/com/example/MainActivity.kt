package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.repository.ArenaRepository
import com.example.data.repository.PlayerRepository
import com.example.notifications.TournaXNotificationHelper
import com.example.ui.components.TournaXGradientButton
import com.example.ui.components.TournaXViewportFrame
import com.example.ui.screens.AccountRestrictedScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainAppShell
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SplashScreen
import kotlinx.coroutines.delay
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.TournaXTheme
import com.example.ui.viewmodel.ArenaViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private val pendingDeepLinkCategory = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TournaXNotificationHelper.createNotificationChannels(applicationContext)
        extractDeepLinkCategory(intent)?.let { category ->
            pendingDeepLinkCategory.value = category
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            val deepLinkCategory by pendingDeepLinkCategory.collectAsStateWithLifecycle()
            TournaXTheme {
                TournaXViewportFrame {
                    TournaXAppRoot(
                        deepLinkCategory = deepLinkCategory,
                        onConsumeDeepLink = {
                            pendingDeepLinkCategory.value = null
                            intent?.removeExtra(TournaXNotificationHelper.EXTRA_NOTIFICATION_CATEGORY)
                            intent?.removeExtra("category")
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLinkCategory(intent)?.let { category ->
            pendingDeepLinkCategory.value = category
        }
    }

    private fun extractDeepLinkCategory(intent: Intent?): String? {
        if (intent == null) return null
        val direct = intent.getStringExtra(TournaXNotificationHelper.EXTRA_NOTIFICATION_CATEGORY)
            ?: intent.getStringExtra("category")
        if (!direct.isNullOrBlank()) return direct.trim().lowercase()
        val hasFcmTitle = !intent.getStringExtra("google.message_id").isNullOrBlank() ||
            !intent.getStringExtra("gcm.notification.title").isNullOrBlank()
        return if (hasFcmTitle) "announcement" else null
    }
}

@Composable
fun TournaXAppRoot(
    deepLinkCategory: String? = null,
    onConsumeDeepLink: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var splashDone by remember { mutableStateOf(false) }
    var notificationPermissionGranted by remember {
        mutableStateOf(TournaXNotificationHelper.hasNotificationPermission(context))
    }
    var permissionStepDone by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                notificationPermissionGranted
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionGranted = granted
        permissionStepDone = true
    }
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        if (!permissionStepDone && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(permissionStepDone) {
        if (permissionStepDone && Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope,
            )
        }
    }

    AnimatedContent(
        targetState = Pair(splashDone, currentUser?.uid),
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "root_flow",
    ) { (isSplashComplete, uid) ->
        if (!isSplashComplete) {
            SplashScreen(onSplashFinished = { splashDone = true })
        } else if (uid == null || currentUser == null) {
            AuthScreen(onAuthSuccess = {})
        } else {
            key(uid) {
                AuthenticatedArenaFlow(
                    user = currentUser!!,
                    notificationPermissionGranted = notificationPermissionGranted,
                    deepLinkCategory = deepLinkCategory,
                    onConsumeDeepLink = onConsumeDeepLink,
                    onSignOut = {
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedArenaFlow(
    user: FirebaseUser,
    notificationPermissionGranted: Boolean,
    deepLinkCategory: String?,
    onConsumeDeepLink: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val db = remember(context) {
        val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }
    val playerRepository = remember(db) { PlayerRepository(db) }
    val arenaRepository = remember(db) { ArenaRepository(db) }

    val viewModel: PlayerViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                PlayerViewModel(playerRepository, user.uid)
            }
        },
    )

    val arenaViewModel: ArenaViewModel = viewModel(
        key = "arena_${user.uid}",
        factory = viewModelFactory {
            initializer {
                ArenaViewModel(arenaRepository, user.uid)
            }
        },
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    when (val state = profileState) {
        UiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = NeonPurpleStart)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Syncing Arena Profile...",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MetallicSilverDim),
                )
            }
        }

        is UiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = CrimsonAlert,
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(modifier = Modifier.height(16.dp))
                TournaXGradientButton(
                    text = "Sign Out & Retry",
                    onClick = onSignOut,
                )
            }
        }

        is UiState.Success -> {
            val profile = state.data
            if (profile != null) {
                var nowMillis by remember(profile.userId, profile.suspendedUntil) {
                    mutableLongStateOf(System.currentTimeMillis())
                }
                LaunchedEffect(profile.accountStatus, profile.suspendedUntil) {
                    if (profile.accountStatus.trim().equals("suspended", ignoreCase = true) &&
                        profile.suspendedUntil != null
                    ) {
                        while (true) {
                            nowMillis = System.currentTimeMillis()
                            if (!profile.isActivelySuspended(nowMillis)) break
                            delay(1000L)
                        }
                    }
                }

                if (profile.isRestricted(nowMillis)) {
                    AccountRestrictedScreen(
                        profile = profile,
                        nowMillis = nowMillis,
                        onSignOut = onSignOut,
                    )
                } else {
                    LaunchedEffect(profile.userId, notificationPermissionGranted) {
                        TournaXNotificationHelper.syncFcmTokenAndTopic(
                            playerRepository = playerRepository,
                            userId = profile.userId,
                        )
                    }
                    MainAppShell(
                        profile = profile,
                        userEmail = user.email,
                        arenaViewModel = arenaViewModel,
                        isUpdatingProfile = isSubmitting,
                        profileActionError = actionError,
                        onClearProfileError = viewModel::clearActionError,
                        onUpdateProfile = { fullName, phone, ign, onDone ->
                            viewModel.updateProfile(fullName, phone, ign, onDone)
                        },
                        onLogout = onSignOut,
                        deepLinkCategory = deepLinkCategory,
                        onConsumeDeepLink = onConsumeDeepLink,
                    )
                }
            } else {
                OnboardingScreen(
                    userEmail = user.email,
                    defaultFullName = user.displayName.orEmpty(),
                    isSubmitting = isSubmitting,
                    serverError = actionError,
                    onClearError = viewModel::clearActionError,
                    onSubmitRegistration = { fullName, phone, ffUid, ign, accepted ->
                        viewModel.saveRegistration(
                            fullName = fullName,
                            phone = phone,
                            freeFireUid = ffUid,
                            freeFireIgn = ign,
                            termsAccepted = accepted,
                        )
                    },
                    onSignOut = onSignOut,
                )
            }
        }
    }
}
