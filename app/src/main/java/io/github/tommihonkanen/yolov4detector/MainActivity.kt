package io.github.tommihonkanen.yolov4detector

import android.Manifest
import android.animation.ObjectAnimator
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import io.github.tommihonkanen.yolov4detector.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var yoloDetector: YoloDetector
    private lateinit var filePickerHelper: FilePickerHelper
    private lateinit var modelManager: ModelManager
    
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var processingJob: Job? = null
    
    private var frameCount = 0
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0.0
    private var detectionCount = 0
    
    private var isFlashOn = false
    private var isPaused = false
    private var isFabMenuOpen = false
    private var isSettingsPanelOpen = false
    private var currentCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null
    
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "Unable to load OpenCV!")
            Toast.makeText(this, "OpenCV initialization failed", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        yoloDetector = YoloDetector()
        filePickerHelper = FilePickerHelper(this)
        modelManager = ModelManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setupUI()
        
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        
        // Load the selected model
        loadSelectedModel()
    }
    
    private fun setupUI() {
        // Model selection button
        binding.btnModels.setOnClickListener {
            val intent = Intent(this, ModelManagerActivity::class.java)
            modelManagerLauncher.launch(intent)
            overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out)
        }
        
        // About button
        binding.btnAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out)
        }
        
        // FAB Menu setup
        setupFABMenu()
        
        // Settings panel setup
        setupSettingsPanel()
        
        // Threshold controls
        setupThresholdControls()
        
        updateModelDisplay()
    }
    
    private fun setupFABMenu() {
        // Main FAB click - toggle pause/play
        binding.fabMain.setOnClickListener {
            togglePausePlay()
        }
        
        // Long press to open menu
        binding.fabMain.setOnLongClickListener {
            toggleFABMenu()
            true
        }
        
        // Flash FAB
        binding.fabFlash.setOnClickListener {
            toggleFlash()
        }
        
        // Flip camera FAB
        binding.fabFlipCamera.setOnClickListener {
            flipCamera()
        }
        
        // Close menu when clicking elsewhere
        binding.root.setOnClickListener {
            if (isFabMenuOpen) {
                toggleFABMenu()
            }
            if (isSettingsPanelOpen) {
                toggleSettingsPanel()
            }
        }
    }
    
    private fun setupSettingsPanel() {
        binding.fabSettings.setOnClickListener {
            toggleSettingsPanel()
        }
    }
    
    private fun toggleFABMenu() {
        isFabMenuOpen = !isFabMenuOpen
        
        val flashContainer = binding.flashFabContainer
        val flipContainer = binding.flipFabContainer
        
        if (isFabMenuOpen) {
            // Show menu items with animation
            flashContainer.visibility = View.VISIBLE
            flipContainer.visibility = View.VISIBLE
            
            flashContainer.alpha = 0f
            flipContainer.alpha = 0f
            
            flashContainer.animate().alpha(1f).setDuration(200).start()
            flipContainer.animate().alpha(1f).setDuration(200).setStartDelay(50).start()
            
            // Rotate main FAB
            binding.fabMain.animate().rotation(45f).setDuration(200).start()
        } else {
            // Hide menu items
            flashContainer.animate().alpha(0f).setDuration(200).withEndAction {
                flashContainer.visibility = View.GONE
            }.start()
            flipContainer.animate().alpha(0f).setDuration(200).withEndAction {
                flipContainer.visibility = View.GONE
            }.start()
            
            // Reset main FAB rotation
            binding.fabMain.animate().rotation(0f).setDuration(200).start()
        }
    }
    
    private fun toggleSettingsPanel() {
        isSettingsPanelOpen = !isSettingsPanelOpen
        
        val panel = binding.thresholdPanel
        val targetTranslation = if (isSettingsPanelOpen) -resources.getDimension(R.dimen.settings_panel_width) else 0f
        
        ObjectAnimator.ofFloat(panel, "translationX", targetTranslation).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
        
        // Hide/show settings button with fade animation
        binding.fabSettings.animate()
            .alpha(if (isSettingsPanelOpen) 0f else 1f)
            .setDuration(300)
            .withEndAction {
                if (isSettingsPanelOpen) {
                    binding.fabSettings.visibility = View.INVISIBLE
                } else {
                    binding.fabSettings.visibility = View.VISIBLE
                }
            }
            .start()
    }
    
    private fun togglePausePlay() {
        isPaused = !isPaused
        binding.fabMain.setImageResource(
            if (isPaused) android.R.drawable.ic_media_play
            else android.R.drawable.ic_media_pause
        )
        
        if (isPaused) {
            cameraProvider?.unbindAll()
        } else {
            startCamera()
        }
        
        // Close FAB menu if open
        if (isFabMenuOpen) {
            toggleFABMenu()
        }
    }
    
    private fun toggleFlash() {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                isFlashOn = !isFlashOn
                cam.cameraControl.enableTorch(isFlashOn)
                binding.fabFlash.setImageResource(
                    if (isFlashOn) R.drawable.ic_flash_on
                    else R.drawable.ic_flash_off
                )
                binding.fabFlash.imageTintList = ContextCompat.getColorStateList(this, 
                    if (isFlashOn) R.color.warning_yellow
                    else R.color.md_theme_onSurface
                )
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
        startCamera()
        
        // Close FAB menu
        if (isFabMenuOpen) {
            toggleFABMenu()
        }
    }
    
    private fun setupThresholdControls() {
        // Confidence threshold
        binding.confidenceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 100f
                binding.confidenceValue.text = "${progress}%"
                yoloDetector.confThreshold = value
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // NMS threshold
        binding.nmsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 100f
                binding.nmsValue.text = "${progress}%"
                yoloDetector.nmsThreshold = value
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Set initial values
        binding.confidenceSeekBar.progress = (yoloDetector.confThreshold * 100).toInt()
        binding.nmsSeekBar.progress = (yoloDetector.nmsThreshold * 100).toInt()
        
        // Reset button
        binding.btnResetThresholds.setOnClickListener {
            binding.confidenceSeekBar.progress = 25 // Default 0.25
            binding.nmsSeekBar.progress = 45 // Default 0.45
            yoloDetector.confThreshold = 0.25f
            yoloDetector.nmsThreshold = 0.45f
            Toast.makeText(this, "Thresholds reset to defaults", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateModelDisplay() {
        val model = modelManager.getSelectedModel()
        binding.currentModelName.text = model?.name ?: "Not loaded"
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
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            // Set scale type to FIT_CENTER for consistent coordinate mapping
            binding.cameraPreview.scaleType = PreviewView.ScaleType.FIT_CENTER
            
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }
            
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, YoloImageAnalyzer())
                }
            
            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    this, currentCameraSelector, preview!!, imageAnalyzer!!
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
            
        }, ContextCompat.getMainExecutor(this))
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
                    
                    val detections = withContext(Dispatchers.IO) {
                        yoloDetector.detect(rotatedMat)
                    }
                    
                    withContext(Dispatchers.Main) {
                        // Pass the rotated dimensions and rotation info
                        val width = if (rotationDegrees == 90 || rotationDegrees == 270) image.height else image.width
                        val height = if (rotationDegrees == 90 || rotationDegrees == 270) image.width else image.height
                        binding.detectionOverlay.setDetections(detections, width, height, binding.cameraPreview)
                        
                        // Update detection count
                        detectionCount = detections.size
                    }
                    
                    mat.release()
                    rotatedMat.release()
                }
                
                updateStats(inferenceTime)
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
    
    private fun updateStats(inferenceTime: Long) {
        frameCount++
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastFpsTime
        
        if (timeDiff >= 1000) {
            currentFps = frameCount * 1000.0 / timeDiff
            frameCount = 0
            lastFpsTime = currentTime
        }
        
        runOnUiThread {
            binding.fpsText.text = "${currentFps.toInt()} FPS"
            binding.inferenceText.text = "${inferenceTime}ms"
            binding.detectionCountText.text = "$detectionCount objects"
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
                startCamera()
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