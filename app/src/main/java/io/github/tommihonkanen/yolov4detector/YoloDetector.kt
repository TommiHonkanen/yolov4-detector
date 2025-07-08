package io.github.tommihonkanen.yolov4detector

import android.util.Log
import org.opencv.core.*
import org.opencv.dnn.DetectionModel
import org.opencv.dnn.Dnn
import java.io.File

data class Detection(
    val classId: Int,
    val className: String,
    val confidence: Float,
    val boundingBox: Rect
)

class YoloDetector {
    private var model: DetectionModel? = null
    private var classNames: List<String> = emptyList()
    
    // These will be automatically extracted from cfg
    private var inputWidth = 0
    private var inputHeight = 0
    
    var confThreshold = 0.25f
    var nmsThreshold = 0.45f
    private val scale = 1.0 / 255.0
    
    companion object {
        private const val TAG = "YoloDetector"
    }
    
    fun loadModel(configPath: String, weightsPath: String, namesPath: String): Boolean {
        return try {
            // Read the network
            val net = Dnn.readNetFromDarknet(configPath, weightsPath)
            net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV)
            net.setPreferableTarget(Dnn.DNN_TARGET_CPU)
            
            // Create DetectionModel from the network
            model = DetectionModel(net)
            
            // Extract input dimensions from cfg file
            extractInputDimensionsFromCfg(configPath)
            
            // Set input parameters
            model?.apply {
                // swapRB=true because we're feeding BGR but YOLO expects RGB
                // crop=false to maintain aspect ratio
                setInputParams(scale, Size(inputWidth.toDouble(), inputHeight.toDouble()), 
                              Scalar(0.0, 0.0, 0.0), true, false)
                setNmsAcrossClasses(false) // YOLO uses per-class NMS
            }
            
            // Load class names
            classNames = File(namesPath).readLines().filter { it.isNotEmpty() }
            
            Log.d(TAG, "Model loaded successfully with ${classNames.size} classes")
            Log.d(TAG, "Input dimensions: ${inputWidth}x${inputHeight}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}", e)
            model = null
            classNames = emptyList()
            false
        }
    }
    
    private fun extractInputDimensionsFromCfg(configPath: String) {
        try {
            val cfgLines = File(configPath).readLines()
            var inNetSection = false
            var widthFound = false
            var heightFound = false
            
            for (line in cfgLines) {
                val trimmedLine = line.trim()
                
                if (trimmedLine == "[net]") {
                    inNetSection = true
                    continue
                }
                
                if (inNetSection && trimmedLine.startsWith("[")) {
                    // We've left the [net] section
                    break
                }
                
                if (inNetSection) {
                    when {
                        trimmedLine.startsWith("width=") -> {
                            inputWidth = trimmedLine.substringAfter("width=").trim().toIntOrNull() 
                                ?: throw Exception("Invalid width value in cfg file")
                            widthFound = true
                        }
                        trimmedLine.startsWith("height=") -> {
                            inputHeight = trimmedLine.substringAfter("height=").trim().toIntOrNull()
                                ?: throw Exception("Invalid height value in cfg file")
                            heightFound = true
                        }
                    }
                }
            }
            
            if (!widthFound || !heightFound) {
                throw Exception("Network dimensions (width/height) not found in cfg file")
            }
            
            if (inputWidth != inputHeight) {
                throw Exception("Network width ($inputWidth) and height ($inputHeight) must be equal")
            }
            
            Log.d(TAG, "Extracted dimensions from cfg: ${inputWidth}x${inputHeight}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing cfg file: ${e.message}")
            throw e
        }
    }
    
    fun detect(frame: Mat): List<Detection> {
        if (model == null || classNames.isEmpty()) {
            return emptyList()
        }
        
        return try {
            Log.d(TAG, "Processing frame: ${frame.width()}x${frame.height()}")
            
            val classIds = MatOfInt()
            val confidences = MatOfFloat()
            val boxes = MatOfRect()
            
            // Detect objects - DetectionModel handles NMS automatically
            model?.detect(frame, classIds, confidences, boxes, confThreshold, nmsThreshold)
            
            // Convert results to Detection objects
            val detections = mutableListOf<Detection>()
            
            // Check if any detections were found
            if (classIds.rows() > 0 && confidences.rows() > 0 && boxes.rows() > 0) {
                try {
                    val classIdArray = classIds.toArray()
                    val confidenceArray = confidences.toArray()
                    val boxArray = boxes.toArray()
                    
                    // Ensure all arrays have the same length
                    val numDetections = minOf(classIdArray.size, confidenceArray.size, boxArray.size)
                    
                    for (i in 0 until numDetections) {
                        val classId = classIdArray[i]
                        val confidence = confidenceArray[i]
                        val box = boxArray[i]
                        
                        val className = if (classId >= 0 && classId < classNames.size) {
                            classNames[classId]
                        } else {
                            "Unknown"
                        }
                        
                        detections.add(
                            Detection(
                                classId = classId,
                                className = className,
                                confidence = confidence,
                                boundingBox = box
                            )
                        )
                        
                        // Debug log
                        Log.d(TAG, "Detection: $className (${confidence}) at [${box.x}, ${box.y}, ${box.width}x${box.height}]")
                    }
                    
                    Log.d(TAG, "Detected ${detections.size} objects")
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting detection results: ${e.message}")
                }
            } else {
                Log.d(TAG, "No objects detected in frame")
            }
            
            // Release resources
            classIds.release()
            confidences.release()
            boxes.release()
            
            detections
        } catch (e: Exception) {
            Log.e(TAG, "Error during detection: ${e.message}", e)
            emptyList()
        }
    }
    
    fun isModelLoaded(): Boolean {
        return model != null && classNames.isNotEmpty()
    }
    
    fun release() {
        model = null
        classNames = emptyList()
    }
}