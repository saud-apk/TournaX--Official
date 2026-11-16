package com.example.ui.components

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FaceRetouchingNatural
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.AmberNotice
import com.example.ui.theme.AmberSurface
import com.example.ui.theme.ClaySurfaceBottom
import com.example.ui.theme.ClaySurfaceTop
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.EmeraldSurface
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDim
import com.example.ui.theme.NeonPurpleGlow
import com.example.ui.theme.NeonPurpleSoft
import com.example.ui.theme.NeonPurpleStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderBright
import com.example.ui.theme.ObsidianInput
import com.example.util.SelfieImageHelper
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs

enum class FaceLivenessStep(
    val stepIndex: Int,
    val shortLabel: String,
    val title: String,
    val instruction: String,
) {
    CENTER(
        stepIndex = 0,
        shortLabel = "Center",
        title = "Look Straight Ahead",
        instruction = "Position your face clearly inside the circle.",
    ),
    LEFT(
        stepIndex = 1,
        shortLabel = "Left",
        title = "Turn Head Left ←",
        instruction = "Slowly turn your face to the LEFT side.",
    ),
    RIGHT(
        stepIndex = 2,
        shortLabel = "Right",
        title = "Turn Head Right →",
        instruction = "Slowly turn your face to the RIGHT side.",
    ),
    UP(
        stepIndex = 3,
        shortLabel = "Up",
        title = "Look Up ↑",
        instruction = "Tilt your chin slightly UP toward the ceiling.",
    ),
    DOWN(
        stepIndex = 4,
        shortLabel = "Down",
        title = "Look Down ↓",
        instruction = "Tilt your chin slightly DOWN.",
    ),
    CAPTURING(
        stepIndex = 5,
        shortLabel = "Verify",
        title = "Look Straight • Capturing ✓",
        instruction = "Hold steady! Capturing verified live selfie...",
    ),
}

@Composable
fun InAppFaceVerificationDialog(
    onVerified: (selfieBase64: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(FaceLivenessStep.CENTER) }
    var faceDetected by remember { mutableStateOf(false) }
    var firstYawSign by remember { mutableFloatStateOf(0f) }
    var centerAlignedSinceMs by remember { mutableLongStateOf(0L) }
    var hasCaptured by remember { mutableStateOf(false) }
    var isEncodingCapture by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    fun triggerCapture() {
        if (hasCaptured || isEncodingCapture) return
        hasCaptured = true
        isEncodingCapture = true
        cameraError = null
        val mainExecutor = ContextCompat.getMainExecutor(context)
        try {
            imageCapture.takePicture(
                mainExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotation = image.imageInfo.rotationDegrees
                        val bitmap = try {
                            image.toBitmap()
                        } catch (t: Throwable) {
                            null
                        } finally {
                            image.close()
                        }
                        if (bitmap == null) {
                            hasCaptured = false
                            isEncodingCapture = false
                            cameraError = "Could not capture frame. Please tap Retry."
                            return
                        }
                        scope.launch {
                            SelfieImageHelper.encodeBitmapToBase64(
                                bitmap = bitmap,
                                rotationDegrees = rotation,
                                mirrorHorizontal = true,
                            ).onSuccess { base64 ->
                                isEncodingCapture = false
                                onVerified(base64)
                            }.onFailure { err ->
                                hasCaptured = false
                                isEncodingCapture = false
                                cameraError = err.localizedMessage ?: "Failed to verify selfie. Try again."
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        hasCaptured = false
                        isEncodingCapture = false
                        cameraError = exception.localizedMessage ?: "Capture failed. Please retry."
                    }
                },
            )
        } catch (t: Throwable) {
            hasCaptured = false
            isEncodingCapture = false
            cameraError = t.localizedMessage ?: "Unable to capture frame."
        }
    }

    // Fallback safety timer during CAPTURING phase in case user stays slightly tilted
    LaunchedEffect(currentStep) {
        if (currentStep == FaceLivenessStep.CAPTURING && !hasCaptured) {
            delay(1300L)
            if (!hasCaptured) {
                triggerCapture()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        var faceDetector: com.google.mlkit.vision.face.FaceDetector? = null
        var boundProvider: ProcessCameraProvider? = null

        try {
            val detectorOptions = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.24f)
                .build()
            val detector = FaceDetection.getClient(detectorOptions)
            faceDetector = detector

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                {
                    try {
                        val provider = cameraProviderFuture.get()
                        boundProvider = provider
                        val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        val preview = Preview.Builder().build().apply {
                            surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processFaceFrame(
                                imageProxy = imageProxy,
                                faceDetector = detector,
                                currentStepProvider = { currentStep },
                                firstYawSignProvider = { firstYawSign },
                                onFacePresenceChanged = { present ->
                                    if (faceDetected != present) faceDetected = present
                                },
                                onCenterAlignedTimestamp = { now ->
                                    if (centerAlignedSinceMs == 0L) {
                                        centerAlignedSinceMs = now
                                    }
                                    now - centerAlignedSinceMs >= 350L
                                },
                                onResetCenterAligned = { centerAlignedSinceMs = 0L },
                                onAdvanceStep = { next, recordedYawSign ->
                                    if (recordedYawSign != null) {
                                        firstYawSign = recordedYawSign
                                    }
                                    currentStep = next
                                    centerAlignedSinceMs = 0L
                                },
                                onReadyToSnap = {
                                    triggerCapture()
                                },
                            )
                        }

                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageCapture,
                            imageAnalysis,
                        )
                    } catch (t: Throwable) {
                        cameraError = t.localizedMessage ?: "Unable to start in-app front camera."
                    }
                },
                ContextCompat.getMainExecutor(context),
            )
        } catch (t: Throwable) {
            cameraError = t.localizedMessage ?: "Front camera service unavailable on this device."
        }

        onDispose {
            try {
                boundProvider?.unbindAll()
            } catch (_: Exception) {
            }
            try {
                faceDetector?.close()
            } catch (_: Exception) {
            }
            cameraExecutor.shutdown()
        }
    }

    val progressTarget = when (currentStep) {
        FaceLivenessStep.CENTER -> if (faceDetected) 0.16f else 0.05f
        FaceLivenessStep.LEFT -> 0.34f
        FaceLivenessStep.RIGHT -> 0.52f
        FaceLivenessStep.UP -> 0.72f
        FaceLivenessStep.DOWN -> 0.90f
        FaceLivenessStep.CAPTURING -> 1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 320),
        label = "face_liveness_progress",
    )
    val ringColor by animateColorAsState(
        targetValue = when {
            currentStep == FaceLivenessStep.CAPTURING -> EmeraldVerified
            faceDetected -> NeonPurpleSoft
            else -> AmberNotice
        },
        label = "face_ring_color",
    )

    Dialog(
        onDismissRequest = {
            if (!isEncodingCapture) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isEncodingCapture,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.2.dp, ObsidianBorderBright),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .claySurface(
                        cornerRadius = 26.dp,
                        topColor = ClaySurfaceTop,
                        bottomColor = ClaySurfaceBottom,
                        elevation = 14.dp,
                    )
                    .testTag("face_verification_dialog"),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonPurpleGlow)
                                    .border(1.dp, NeonPurpleStart.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FaceRetouchingNatural,
                                    contentDescription = null,
                                    tint = NeonPurpleSoft,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LIVE FACE VERIFICATION",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MetallicSilver,
                                        fontWeight = FontWeight.ExtraBold,
                                    ),
                                )
                                Text(
                                    text = "In-app anti-fraud check • Stays inside TournaX",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilverDim),
                                )
                            }
                        }

                        Surface(
                            color = ObsidianInput,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, ObsidianBorderBright),
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                enabled = !isEncodingCapture,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("face_verification_close_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close Face Verification",
                                    tint = MetallicSilver,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    // 5-Step Progress Pills (Center, Left, Right, Up, Down)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("face_verification_progress_pill"),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val challengeSteps = listOf(
                            FaceLivenessStep.CENTER,
                            FaceLivenessStep.LEFT,
                            FaceLivenessStep.RIGHT,
                            FaceLivenessStep.UP,
                            FaceLivenessStep.DOWN,
                        )
                        challengeSteps.forEach { stepItem ->
                            val isDone = currentStep.stepIndex > stepItem.stepIndex
                            val isActive = currentStep == stepItem
                            Surface(
                                color = when {
                                    isDone -> EmeraldSurface
                                    isActive -> NeonPurpleGlow
                                    else -> ObsidianInput
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isDone -> EmeraldVerified.copy(alpha = 0.6f)
                                        isActive -> NeonPurpleStart
                                        else -> ObsidianBorder
                                    },
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (isDone) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldVerified,
                                            modifier = Modifier.size(11.dp),
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = stepItem.shortLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = when {
                                                isDone -> EmeraldVerified
                                                isActive -> NeonPurpleSoft
                                                else -> MetallicSilverDim
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }

                    // Responsive Circular Camera Preview with Directional Indicators (Up / Down / Left / Right)
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val ringOuterSize = minOf(268.dp, maxWidth * 0.84f).coerceAtLeast(184.dp)
                        val arcSize = ringOuterSize * 0.78f
                        val previewSize = ringOuterSize * 0.71f
                        Box(
                            modifier = Modifier.size(ringOuterSize),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Directional Guidance Arrows
                            DirectionGuideChip(
                                icon = Icons.Rounded.ArrowUpward,
                                contentDesc = "Look Up",
                                active = currentStep == FaceLivenessStep.UP,
                                completed = currentStep.stepIndex > FaceLivenessStep.UP.stepIndex,
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                            DirectionGuideChip(
                                icon = Icons.Rounded.ArrowDownward,
                                contentDesc = "Look Down",
                                active = currentStep == FaceLivenessStep.DOWN,
                                completed = currentStep.stepIndex > FaceLivenessStep.DOWN.stepIndex,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                            DirectionGuideChip(
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDesc = "Turn Left",
                                active = currentStep == FaceLivenessStep.LEFT,
                                completed = currentStep.stepIndex > FaceLivenessStep.LEFT.stepIndex,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                            DirectionGuideChip(
                                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDesc = "Turn Right",
                                active = currentStep == FaceLivenessStep.RIGHT,
                                completed = currentStep.stepIndex > FaceLivenessStep.RIGHT.stepIndex,
                                modifier = Modifier.align(Alignment.CenterEnd),
                            )

                            // Circular Preview + Progress Arc
                            Box(
                                modifier = Modifier.size(arcSize),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(previewSize)
                                        .clip(CircleShape)
                                        .background(ObsidianInput),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AndroidView(
                                        factory = { previewView },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    if (isEncodingCapture) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0x990A0C10)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                color = EmeraldVerified,
                                                strokeWidth = 3.dp,
                                                modifier = Modifier.size(36.dp),
                                            )
                                        }
                                    }
                                }

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokePx = 6.dp.toPx()
                                    drawArc(
                                        color = ObsidianBorderBright,
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = strokePx),
                                    )
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(NeonPurpleStart, EmeraldVerified, ringColor, NeonPurpleStart),
                                        ),
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedProgress,
                                        useCenter = false,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                                    )
                                }
                            }
                        }
                    }

                    // Live Prompt Banner
                    Surface(
                        color = when {
                            cameraError != null -> CrimsonSurface
                            currentStep == FaceLivenessStep.CAPTURING -> EmeraldSurface
                            !faceDetected -> AmberSurface
                            else -> NeonPurpleGlow
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                cameraError != null -> CrimsonAlert.copy(alpha = 0.6f)
                                currentStep == FaceLivenessStep.CAPTURING -> EmeraldVerified.copy(alpha = 0.6f)
                                !faceDetected -> AmberNotice.copy(alpha = 0.55f)
                                else -> NeonPurpleStart.copy(alpha = 0.55f)
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("face_verification_step_label"),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = when {
                                    cameraError != null -> "Camera Notice"
                                    !faceDetected && currentStep != FaceLivenessStep.CAPTURING -> "Face Not Detected"
                                    else -> currentStep.title
                                },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = when {
                                        cameraError != null -> CrimsonAlert
                                        currentStep == FaceLivenessStep.CAPTURING -> EmeraldVerified
                                        !faceDetected -> AmberNotice
                                        else -> Color.White
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = cameraError ?: if (!faceDetected && currentStep != FaceLivenessStep.CAPTURING) {
                                    "Look at the front camera in good light so your face fits inside the ring."
                                } else {
                                    currentStep.instruction
                                },
                                style = MaterialTheme.typography.labelSmall.copy(color = MetallicSilver),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                currentStep = FaceLivenessStep.CENTER
                                firstYawSign = 0f
                                centerAlignedSinceMs = 0L
                                hasCaptured = false
                                cameraError = null
                            },
                            enabled = !isEncodingCapture,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ObsidianBorderBright),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MetallicSilver),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize(),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Restart Face Check",
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restart", fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            color = EmeraldSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.45f)),
                            modifier = Modifier.weight(1.3f),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = EmeraldVerified,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto-Captures Live",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = EmeraldVerified,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectionGuideChip(
    icon: ImageVector,
    contentDesc: String,
    active: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = when {
            completed -> EmeraldSurface
            active -> NeonPurpleStart
            else -> ObsidianInput
        },
        shape = CircleShape,
        border = BorderStroke(
            width = if (active) 1.5.dp else 1.dp,
            color = when {
                completed -> EmeraldVerified
                active -> Color.White.copy(alpha = 0.85f)
                else -> ObsidianBorderBright
            },
        ),
        shadowElevation = if (active) 8.dp else 2.dp,
        modifier = modifier.size(34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (completed) Icons.Rounded.CheckCircle else icon,
                contentDescription = contentDesc,
                tint = when {
                    completed -> EmeraldVerified
                    active -> Color.White
                    else -> MetallicSilverDim
                },
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun processFaceFrame(
    imageProxy: ImageProxy,
    faceDetector: com.google.mlkit.vision.face.FaceDetector,
    currentStepProvider: () -> FaceLivenessStep,
    firstYawSignProvider: () -> Float,
    onFacePresenceChanged: (Boolean) -> Unit,
    onCenterAlignedTimestamp: (Long) -> Boolean,
    onResetCenterAligned: () -> Unit,
    onAdvanceStep: (next: FaceLivenessStep, recordedYawSign: Float?) -> Unit,
    onReadyToSnap: () -> Unit,
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    faceDetector.process(inputImage)
        .addOnSuccessListener { faces ->
            val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            if (primaryFace == null) {
                onFacePresenceChanged(false)
                onResetCenterAligned()
                return@addOnSuccessListener
            }

            onFacePresenceChanged(true)
            val rotY = primaryFace.headEulerAngleY // Horizontal head turn
            val rotX = primaryFace.headEulerAngleX // Vertical pitch (+ = Up, - = Down)
            val step = currentStepProvider()

            when (step) {
                FaceLivenessStep.CENTER -> {
                    if (abs(rotY) < 12f && abs(rotX) < 12f) {
                        val ready = onCenterAlignedTimestamp(System.currentTimeMillis())
                        if (ready) {
                            onAdvanceStep(FaceLivenessStep.LEFT, null)
                        }
                    } else {
                        onResetCenterAligned()
                    }
                }

                FaceLivenessStep.LEFT -> {
                    if (abs(rotY) >= 13f) {
                        val sign = if (rotY >= 0f) 1f else -1f
                        onAdvanceStep(FaceLivenessStep.RIGHT, sign)
                    }
                }

                FaceLivenessStep.RIGHT -> {
                    val firstSign = firstYawSignProvider()
                    if (firstSign != 0f && rotY * firstSign < 0f && abs(rotY) >= 13f) {
                        onAdvanceStep(FaceLivenessStep.UP, null)
                    }
                }

                FaceLivenessStep.UP -> {
                    if (rotX >= 9f) {
                        onAdvanceStep(FaceLivenessStep.DOWN, null)
                    }
                }

                FaceLivenessStep.DOWN -> {
                    if (rotX <= -9f) {
                        onAdvanceStep(FaceLivenessStep.CAPTURING, null)
                    }
                }

                FaceLivenessStep.CAPTURING -> {
                    if (abs(rotY) < 14f && abs(rotX) < 14f) {
                        onReadyToSnap()
                    }
                }
            }
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}
