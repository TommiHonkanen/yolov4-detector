package io.github.tommihonkanen.yolov4detector

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.camera.view.PreviewView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DetectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        private val LABEL_LEFT_PADDING = 15f
        private val LABEL_RIGHT_PADDING = 15f
        private val LABEL_TEXT_SPACING = 15f
    }
    
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimension(R.dimen.detection_stroke_width)
        isAntiAlias = true
        pathEffect = CornerPathEffect(context.resources.getDimension(R.dimen.detection_box_corner_radius))
    }
    
    private val textPaint = Paint().apply {
        textSize = context.resources.getDimension(R.dimen.detection_text_size)
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        letterSpacing = 0.02f
        color = Color.WHITE
    }
    
    private val textBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val confidenceTextPaint = Paint().apply {
        textSize = 24f
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = Color.WHITE
        alpha = 230
    }
    
    private var detections = listOf<Detection>()
    private var sourceWidth = 0
    private var sourceHeight = 0
    private var previewView: PreviewView? = null
    
    // Data class to hold label information
    private data class LabelInfo(
        val detection: Detection,
        val rect: RectF,
        val boxBounds: RectF,
        val color: Int
    )
    
    // Enhanced color palette with category-specific colors
    private val categoryColors = mapOf(
        "person" to Color.parseColor("#5B9FED"),
        "bicycle" to Color.parseColor("#4ECDC4"),
        "car" to Color.parseColor("#FFB74D"),
        "motorcycle" to Color.parseColor("#FF6B6B"),
        "airplane" to Color.parseColor("#9B59B6"),
        "bus" to Color.parseColor("#F39C12"),
        "train" to Color.parseColor("#E74C3C"),
        "truck" to Color.parseColor("#16A085"),
        "boat" to Color.parseColor("#3498DB"),
        "traffic light" to Color.parseColor("#E67E22"),
        "stop sign" to Color.parseColor("#C0392B"),
        "cat" to Color.parseColor("#F1C40F"),
        "dog" to Color.parseColor("#8E44AD"),
        "horse" to Color.parseColor("#D35400"),
        "bird" to Color.parseColor("#27AE60")
    )
    
    private val defaultColors = listOf(
        Color.parseColor("#FF6B6B"),  // Soft Red
        Color.parseColor("#4ECDC4"),  // Turquoise
        Color.parseColor("#45B7D1"),  // Sky Blue
        Color.parseColor("#F7DC6F"),  // Soft Yellow
        Color.parseColor("#BB8FCE"),  // Soft Purple
        Color.parseColor("#85C1E2"),  // Light Blue
        Color.parseColor("#F8B500"),  // Orange
        Color.parseColor("#95E1D3"),  // Mint
        Color.parseColor("#F38181"),  // Coral
        Color.parseColor("#AA96DA")   // Lavender
    )
    
    fun setDetections(newDetections: List<Detection>, imgWidth: Int, imgHeight: Int, preview: PreviewView) {
        detections = newDetections
        sourceWidth = imgWidth
        sourceHeight = imgHeight
        previewView = preview
        postInvalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (detections.isEmpty() || sourceWidth == 0 || sourceHeight == 0) {
            return
        }
        
        // Get the actual preview dimensions and scale type
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        // Calculate scale based on preview view scale type (fit center)
        val scaleX = viewWidth / sourceWidth
        val scaleY = viewHeight / sourceHeight
        val scale = min(scaleX, scaleY) // Use min for FIT_CENTER scale type
        
        // Calculate offsets for centered image
        val scaledWidth = sourceWidth * scale
        val scaledHeight = sourceHeight * scale
        val offsetX = (viewWidth - scaledWidth) / 2
        val offsetY = (viewHeight - scaledHeight) / 2
        
        // First pass: draw all bounding boxes and collect label info
        val labelInfoList = mutableListOf<LabelInfo>()
        
        detections.forEach { detection ->
            val color = getColorForDetection(detection)
            boxPaint.color = color
            
            val left = detection.boundingBox.x * scale + offsetX
            val top = detection.boundingBox.y * scale + offsetY
            val right = (detection.boundingBox.x + detection.boundingBox.width) * scale + offsetX
            val bottom = (detection.boundingBox.y + detection.boundingBox.height) * scale + offsetY
            
            val boxBounds = RectF(left, top, right, bottom)
            
            // Draw main bounding box
            canvas.drawRect(left, top, right, bottom, boxPaint)
            
            // Calculate label position
            val labelRect = calculateLabelPosition(detection, boxBounds, viewWidth, viewHeight)
            labelInfoList.add(LabelInfo(detection, labelRect, boxBounds, color))
        }
        
        // Sort labels by area (larger boxes first) to prioritize important detections
        labelInfoList.sortByDescending { it.boxBounds.width() * it.boxBounds.height() }
        
        // Resolve overlaps
        val finalLabels = resolveOverlaps(labelInfoList)
        
        // Draw labels
        finalLabels.forEach { labelInfo ->
            drawLabel(canvas, labelInfo)
        }
    }
    
    private fun calculateLabelPosition(
        detection: Detection,
        boxBounds: RectF,
        viewWidth: Float,
        viewHeight: Float
    ): RectF {
        // Prepare label text
        val confidencePercent = (detection.confidence * 100).toInt()
        val label = detection.className
        val confidence = "$confidencePercent%"
        
        // Measure text dimensions using actual text width
        val labelWidth = textPaint.measureText(label)
        val confWidth = confidenceTextPaint.measureText(confidence)
        
        // Calculate total width to match the actual layout in drawLabel():
        // left padding + label + spacing + confidence + right padding
        val totalWidth = LABEL_LEFT_PADDING + labelWidth + LABEL_TEXT_SPACING + confWidth + LABEL_RIGHT_PADDING
        val textHeight = 44f
        
        // Try to position label above the box
        var textLeft = boxBounds.left
        var textTop = boxBounds.top - textHeight - 4
        
        // Adjust if label goes outside view
        if (textLeft + totalWidth > viewWidth) {
            textLeft = viewWidth - totalWidth - 4
        }
        if (textLeft < 4) {
            textLeft = 4f
        }
        
        // If label would be above the view, place it inside the box
        if (textTop < 4) {
            textTop = boxBounds.top + 4
        }
        
        return RectF(textLeft, textTop, textLeft + totalWidth, textTop + textHeight)
    }
    
    private fun resolveOverlaps(labelInfoList: List<LabelInfo>): List<LabelInfo> {
        val resolvedLabels = mutableListOf<LabelInfo>()
        val occupiedRects = mutableListOf<RectF>()
        
        labelInfoList.forEach { labelInfo ->
            var currentRect = labelInfo.rect
            var attempts = 0
            var foundPosition = false
            
            // Check if current position overlaps with any existing labels
            while (attempts < 5 && hasOverlap(currentRect, occupiedRects)) {
                // Try alternative positions
                currentRect = when (attempts) {
                    0 -> {
                        // Try below the box
                        RectF(
                            labelInfo.boxBounds.left,
                            labelInfo.boxBounds.bottom + 4,
                            labelInfo.boxBounds.left + currentRect.width(),
                            labelInfo.boxBounds.bottom + 4 + currentRect.height()
                        )
                    }
                    1 -> {
                        // Try to the right of the box
                        RectF(
                            labelInfo.boxBounds.right + 4,
                            labelInfo.boxBounds.top,
                            labelInfo.boxBounds.right + 4 + currentRect.width(),
                            labelInfo.boxBounds.top + currentRect.height()
                        )
                    }
                    2 -> {
                        // Try to the left of the box
                        RectF(
                            labelInfo.boxBounds.left - currentRect.width() - 4,
                            labelInfo.boxBounds.top,
                            labelInfo.boxBounds.left - 4,
                            labelInfo.boxBounds.top + currentRect.height()
                        )
                    }
                    3 -> {
                        // Try inside top-right corner
                        RectF(
                            labelInfo.boxBounds.right - currentRect.width() - 4,
                            labelInfo.boxBounds.top + 4,
                            labelInfo.boxBounds.right - 4,
                            labelInfo.boxBounds.top + 4 + currentRect.height()
                        )
                    }
                    else -> currentRect // Give up and use original position
                }
                attempts++
            }
            
            // Always add the label, even if it overlaps
            resolvedLabels.add(labelInfo.copy(rect = currentRect))
            occupiedRects.add(currentRect)
        }
        
        return resolvedLabels
    }
    
    private fun hasOverlap(rect: RectF, existingRects: List<RectF>): Boolean {
        return existingRects.any { existing ->
            RectF.intersects(rect, existing)
        }
    }
    
    private fun drawLabel(canvas: Canvas, labelInfo: LabelInfo) {
        val detection = labelInfo.detection
        val rect = labelInfo.rect
        val color = labelInfo.color
        
        textBackgroundPaint.color = color
        
        // Always draw full label
        canvas.drawRoundRect(rect, 12f, 12f, textBackgroundPaint)
        
        // Draw label text
        val confidencePercent = (detection.confidence * 100).toInt()
        val label = detection.className
        val confidence = "$confidencePercent%"
        
        // Center text vertically with proper baseline adjustment
        val textBounds = Rect()
        textPaint.getTextBounds(label, 0, label.length, textBounds)
        val textY = rect.centerY() + textBounds.height() / 2f - 2f
        
        canvas.drawText(
            label,
            rect.left + LABEL_LEFT_PADDING,
            textY,
            textPaint
        )
        
        // Draw confidence with proper spacing
        val labelWidth = textPaint.measureText(label)
        val confX = rect.left + LABEL_LEFT_PADDING + labelWidth + LABEL_TEXT_SPACING
        
        canvas.drawText(
            confidence,
            confX,
            textY,
            confidenceTextPaint
        )
    }
    
    private fun getColorForDetection(detection: Detection): Int {
        return categoryColors[detection.className] 
            ?: defaultColors[detection.classId % defaultColors.size]
    }
}