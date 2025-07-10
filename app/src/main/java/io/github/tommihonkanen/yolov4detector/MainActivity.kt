package io.github.tommihonkanen.yolov4detector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import io.github.tommihonkanen.yolov4detector.ui.screens.MainScreen
import io.github.tommihonkanen.yolov4detector.ui.theme.YoloDetectorTheme
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var yoloDetector: YoloDetector
    private lateinit var filePickerHelper: FilePickerHelper
    private lateinit var modelManager: ModelManager
    
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var processingJob: Job? = null
    
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    
    // Compose state
    private var currentFps by mutableStateOf(0)
    private var detectionCount by mutableStateOf(0)
    private var inferenceTimeMs by mutableStateOf(0L)
    private var detections by mutableStateOf(listOf<Detection>())
    private var isFlashOn by mutableStateOf(false)
    private var isPaused by mutableStateOf(false)
    private var currentCameraSelector by mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA)
    private var confidenceThreshold by mutableStateOf(0.25f)
    private var nmsThreshold by mutableStateOf(0.45f)
    private var modelName by mutableStateOf("Not loaded")
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageWidth by mutableStateOf(0)
    private var imageHeight by mutableStateOf(0)
    
    companion object {
        private const val TAG = "MainActivity"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
    
    private val modelManagerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Model was changed, reload it
            loadSelectedModel()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Unable to load OpenCV!")
            Toast.makeText(this, "OpenCV initialization failed", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        yoloDetector = YoloDetector()
        yoloDetector.confThreshold = confidenceThreshold
        yoloDetector.nmsThreshold = nmsThreshold
        
        filePickerHelper = FilePickerHelper(this)
        modelManager = ModelManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Setup image analyzer
        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, YoloImageAnalyzer())
            }
        
        // Load the selected model
        loadSelectedModel()
        
        setContent {
            YoloDetectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        modelName = modelName,
                        isPaused = isPaused,
                        isFlashOn = isFlashOn,
                        confidenceThreshold = confidenceThreshold,
                        nmsThreshold = nmsThreshold,
                        fps = currentFps,
                        inferenceTime = inferenceTimeMs,
                        detectionCount = detectionCount,
                        detections = detections,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        cameraSelector = currentCameraSelector,
                        onModelsClick = {
                            val intent = Intent(this, ModelManagerActivity::class.java)
                            modelManagerLauncher.launch(intent)
                            overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out)
                        },
                        onAboutClick = {
                            val intent = Intent(this, AboutActivity::class.java)
                            startActivity(intent)
                            overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out)
                        },
                        onPauseToggle = { togglePausePlay() },
                        onFlashToggle = { toggleFlash() },
                        onFlipCamera = { flipCamera() },
                        onConfidenceChange = { value ->
                            confidenceThreshold = value
                            yoloDetector.confThreshold = value
                        },
                        onNmsChange = { value ->
                            nmsThreshold = value
                            yoloDetector.nmsThreshold = value
                        },
                        onThresholdReset = {
                            confidenceThreshold = 0.25f
                            nmsThreshold = 0.45f
                            yoloDetector.confThreshold = 0.25f
                            yoloDetector.nmsThreshold = 0.45f
                            Toast.makeText(this, "Thresholds reset to defaults", Toast.LENGTH_SHORT).show()
                        },
                        onCameraReady = { cam ->
                            camera = cam
                        },
                        cameraExecutor = cameraExecutor,
                        imageAnalyzer = imageAnalyzer!!
                    )
                }
            }
        }
        
        if (allPermissionsGranted()) {
            // Camera will be started from the Compose UI
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }
    
    private fun togglePausePlay() {
        isPaused = !isPaused
    }
    
    private fun toggleFlash() {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                isFlashOn = !isFlashOn
                cam.cameraControl.enableTorch(isFlashOn)
            } else {
                Toast.makeText(this, "Flash not available", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun flipCamera() {
        currentCameraSelector = if (currentCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    
    private fun updateModelDisplay() {
        val model = modelManager.getSelectedModel()
        modelName = model?.name ?: "Not loaded"
    }
    
    private fun loadSelectedModel() {
        val model = modelManager.getSelectedModel() ?: return
        
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val success = yoloDetector.loadModel(model.configPath, model.weightsPath, model.namesPath)
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        updateModelDisplay()
                        Toast.makeText(this@MainActivity, "${model.name} loaded", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to load ${model.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    
    private inner class YoloImageAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            if (!yoloDetector.isModelLoaded() || isPaused) {
                image.close()
                return
            }
            
            processingJob?.cancel()
            processingJob = coroutineScope.launch {
                val inferenceTime = measureTimeMillis {
                    val mat = imageProxyToMat(image)
                    val rotatedMat = Mat()
                    
                    // Rotate image to correct orientation (camera is usually 90 degrees rotated)
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    when (rotationDegrees) {
                        90 -> Core.rotate(mat, rotatedMat, Core.ROTATE_90_CLOCKWISE)
                        180 -> Core.rotate(mat, rotatedMat, Core.ROTATE_180)
                        270 -> Core.rotate(mat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                        else -> mat.copyTo(rotatedMat)
                    }
                    
                    val newDetections = withContext(Dispatchers.IO) {
                        yoloDetector.detect(rotatedMat)
                    }
                    
                    withContext(Dispatchers.Main) {
                        // Update Compose state
                        detections = newDetections
                        detectionCount = newDetections.size
                        // Update image dimensions based on rotation
                        val width = if (rotationDegrees == 90 || rotationDegrees == 270) image.height else image.width
                        val height = if (rotationDegrees == 90 || rotationDegrees == 270) image.width else image.height
                        imageWidth = width
                        imageHeight = height
                    }
                    
                    mat.release()
                    rotatedMat.release()
                }
                
                inferenceTimeMs = inferenceTime
                updateStats()
                image.close()
            }
        }
        
        private fun imageProxyToMat(image: ImageProxy): Mat {
            val planes = image.planes
            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]
            
            val ySize = yPlane.buffer.remaining()
            val uSize = uPlane.buffer.remaining()
            val vSize = vPlane.buffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            yPlane.buffer.get(nv21, 0, ySize)
            
            val uvPixelStride = uPlane.pixelStride
            if (uvPixelStride == 1) {
                uPlane.buffer.get(nv21, ySize, uSize)
                vPlane.buffer.get(nv21, ySize + uSize, vSize)
            } else {
                var pos = ySize
                for (i in 0 until uSize / uvPixelStride) {
                    nv21[pos] = vPlane.buffer.get(i * uvPixelStride)
                    nv21[pos + 1] = uPlane.buffer.get(i * uvPixelStride)
                    pos += 2
                }
            }
            
            val mat = Mat(image.height + image.height / 2, image.width, org.opencv.core.CvType.CV_8UC1)
            mat.put(0, 0, nv21)
            
            val rgbMat = Mat()
            // Convert to BGR format (which is what OpenCV DNN expects)
            Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_YUV2BGR_NV21)
            mat.release()
            
            return rgbMat
        }
    }
    
    private fun updateStats() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastFpsTime
        
        if (timeDiff >= 1000) {
            currentFps = (frameCount * 1000.0 / timeDiff).toInt()
            frameCount = 0
            lastFpsTime = currentTime
        }
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                // Camera will be started from the Compose UI
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh model status in case models were changed
        updateModelDisplay()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        coroutineScope.cancel()
        yoloDetector.release()
    }
}