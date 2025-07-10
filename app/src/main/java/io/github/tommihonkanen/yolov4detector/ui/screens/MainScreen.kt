package io.github.tommihonkanen.yolov4detector.ui.screens

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import io.github.tommihonkanen.yolov4detector.Detection
import io.github.tommihonkanen.yolov4detector.ui.components.*
import io.github.tommihonkanen.yolov4detector.ui.theme.*
import java.util.concurrent.ExecutorService

@Composable
fun MainScreen(
    modelName: String,
    isPaused: Boolean,
    isFlashOn: Boolean,
    confidenceThreshold: Float,
    nmsThreshold: Float,
    fps: Int,
    inferenceTime: Long,
    detectionCount: Int,
    detections: List<Detection>,
    imageWidth: Int,
    imageHeight: Int,
    cameraSelector: CameraSelector,
    onModelsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onPauseToggle: () -> Unit,
    onFlashToggle: () -> Unit,
    onFlipCamera: () -> Unit,
    onConfidenceChange: (Float) -> Unit,
    onNmsChange: (Float) -> Unit,
    onThresholdReset: () -> Unit,
    onCameraReady: ((androidx.camera.core.Camera) -> Unit)? = null,
    cameraExecutor: ExecutorService,
    imageAnalyzer: androidx.camera.core.ImageAnalysis
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isSettingsPanelOpen by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Camera Preview
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    if (isPaused) {
                        cameraProvider.unbindAll()
                    } else {
                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(view.surfaceProvider)
                            }
                        
                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer
                            )
                            onCameraReady?.invoke(camera)
                        } catch (exc: Exception) {
                            // Handle error
                        }
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )
        
        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
                            androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                )
        )
        
        // Detection Overlay
        if (imageWidth > 0 && imageHeight > 0 && detections.isNotEmpty()) {
            DetectionOverlay(
                detections = detections,
                sourceWidth = imageWidth,
                sourceHeight = imageHeight,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Top Bar
        MainTopBar(
            modelName = modelName,
            onModelsClick = onModelsClick,
            onAboutClick = onAboutClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Stats Card
        StatsCard(
            fps = fps,
            inferenceTime = inferenceTime,
            detectionCount = detectionCount,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 90.dp, end = 16.dp)
        )
        
        // Settings FAB
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            SettingsFAB(
                onClick = { isSettingsPanelOpen = !isSettingsPanelOpen },
                isVisible = !isSettingsPanelOpen
            )
        }
        
        // FAB Menu
        FABMenu(
            isPaused = isPaused,
            isFlashOn = isFlashOn,
            onPauseToggle = onPauseToggle,
            onFlashToggle = onFlashToggle,
            onFlipCamera = onFlipCamera,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
        
    }
    
    // Threshold Panel - outside the main Box to ensure it overlays everything
    ThresholdPanel(
        isOpen = isSettingsPanelOpen,
        confidenceThreshold = confidenceThreshold,
        nmsThreshold = nmsThreshold,
        onConfidenceChange = onConfidenceChange,
        onNmsChange = onNmsChange,
        onReset = onThresholdReset,
        onDismiss = { isSettingsPanelOpen = false }
    )
}

@Composable
fun rememberCameraPreviewState(): CameraPreviewState {
    return remember { CameraPreviewState() }
}

class CameraPreviewState {
    var previewView: PreviewView? = null
    var imageWidth: Int = 0
    var imageHeight: Int = 0
}