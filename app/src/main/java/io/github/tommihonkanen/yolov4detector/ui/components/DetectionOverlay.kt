package io.github.tommihonkanen.yolov4detector.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tommihonkanen.yolov4detector.Detection
import io.github.tommihonkanen.yolov4detector.ui.theme.*
import kotlin.math.abs
import kotlin.math.min

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    sourceWidth: Int,
    sourceHeight: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokeWidth = with(density) { DetectionStrokeWidth.toPx() }
    val cornerRadius = with(density) { DetectionBoxCornerRadius.toPx() }
    val textSize = with(density) { DetectionTextSize.toPx() }
    val textPadding = with(density) { DetectionTextPadding.toPx() }
    
    val labelLeftPadding = with(density) { 8.dp.toPx() }
    val labelRightPadding = with(density) { 8.dp.toPx() }
    val labelTextSpacing = with(density) { 6.dp.toPx() }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        if (detections.isEmpty() || sourceWidth == 0 || sourceHeight == 0) {
            return@Canvas
        }
        
        val viewWidth = size.width
        val viewHeight = size.height
        
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
            
            val left = detection.boundingBox.x * scale + offsetX
            val top = detection.boundingBox.y * scale + offsetY
            val right = (detection.boundingBox.x + detection.boundingBox.width) * scale + offsetX
            val bottom = (detection.boundingBox.y + detection.boundingBox.height) * scale + offsetY
            
            val boxBounds = Rect(
                offset = Offset(left, top),
                size = Size(right - left, bottom - top)
            )
            
            // Draw main bounding box with rounded corners
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.cornerPathEffect(cornerRadius)
                )
            )
            
            // Calculate label position
            val labelRect = calculateLabelPosition(
                detection = detection,
                boxBounds = boxBounds,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                textSize = textSize,
                labelLeftPadding = labelLeftPadding,
                labelRightPadding = labelRightPadding,
                labelTextSpacing = labelTextSpacing
            )
            
            labelInfoList.add(LabelInfo(detection, labelRect, boxBounds, color))
        }
        
        // Sort labels by area (larger boxes first) to prioritize important detections
        labelInfoList.sortByDescending { it.boxBounds.width * it.boxBounds.height }
        
        // Resolve overlaps
        val finalLabels = resolveOverlaps(labelInfoList)
        
        // Draw labels
        finalLabels.forEach { labelInfo ->
            drawLabel(
                labelInfo = labelInfo,
                textSize = textSize,
                labelLeftPadding = labelLeftPadding,
                labelTextSpacing = labelTextSpacing
            )
        }
    }
}

private fun DrawScope.calculateLabelPosition(
    detection: Detection,
    boxBounds: Rect,
    viewWidth: Float,
    viewHeight: Float,
    textSize: Float,
    labelLeftPadding: Float,
    labelRightPadding: Float,
    labelTextSpacing: Float
): Rect {
    // Prepare label text
    val confidencePercent = (detection.confidence * 100).toInt()
    val label = detection.className
    val confidence = "$confidencePercent%"
    
    // Measure text dimensions
    val paint = android.graphics.Paint().apply {
        this.textSize = textSize
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val confPaint = android.graphics.Paint().apply {
        this.textSize = textSize * 0.85f
        typeface = android.graphics.Typeface.DEFAULT
    }
    
    val labelWidth = paint.measureText(label)
    val confWidth = confPaint.measureText(confidence)
    
    // Calculate total width
    val totalWidth = labelLeftPadding + labelWidth + labelTextSpacing + confWidth + labelRightPadding
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
    
    return Rect(
        offset = Offset(textLeft, textTop),
        size = Size(totalWidth, textHeight)
    )
}

private fun resolveOverlaps(labelInfoList: List<LabelInfo>): List<LabelInfo> {
    val resolvedLabels = mutableListOf<LabelInfo>()
    val occupiedRects = mutableListOf<Rect>()
    
    labelInfoList.forEach { labelInfo ->
        var currentRect = labelInfo.rect
        var attempts = 0
        
        // Check if current position overlaps with any existing labels
        while (attempts < 5 && hasOverlap(currentRect, occupiedRects)) {
            // Try alternative positions
            currentRect = when (attempts) {
                0 -> {
                    // Try below the box
                    Rect(
                        offset = Offset(labelInfo.boxBounds.left, labelInfo.boxBounds.bottom + 4),
                        size = currentRect.size
                    )
                }
                1 -> {
                    // Try to the right of the box
                    Rect(
                        offset = Offset(labelInfo.boxBounds.right + 4, labelInfo.boxBounds.top),
                        size = currentRect.size
                    )
                }
                2 -> {
                    // Try to the left of the box
                    Rect(
                        offset = Offset(labelInfo.boxBounds.left - currentRect.width - 4, labelInfo.boxBounds.top),
                        size = currentRect.size
                    )
                }
                3 -> {
                    // Try inside top-right corner
                    Rect(
                        offset = Offset(
                            labelInfo.boxBounds.right - currentRect.width - 4,
                            labelInfo.boxBounds.top + 4
                        ),
                        size = currentRect.size
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

private fun hasOverlap(rect: Rect, existingRects: List<Rect>): Boolean {
    return existingRects.any { existing ->
        rect.overlaps(existing)
    }
}

private fun DrawScope.drawLabel(
    labelInfo: LabelInfo,
    textSize: Float,
    labelLeftPadding: Float,
    labelTextSpacing: Float
) {
    val detection = labelInfo.detection
    val rect = labelInfo.rect
    val color = labelInfo.color
    
    // Draw background
    drawRoundRect(
        color = color,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(12f, 12f)
    )
    
    // Draw text using native canvas
    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        
        // Prepare label text
        val confidencePercent = (detection.confidence * 100).toInt()
        val label = detection.className
        val confidence = "$confidencePercent%"
        
        // Setup text paints
        val labelPaint = android.graphics.Paint().apply {
            this.color = Color.White.toArgb()
            this.textSize = textSize
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            letterSpacing = 0.02f
            isAntiAlias = true
        }
        
        val confPaint = android.graphics.Paint().apply {
            this.color = Color.White.copy(alpha = 0.9f).toArgb()
            this.textSize = textSize * 0.85f
            typeface = android.graphics.Typeface.DEFAULT
            isAntiAlias = true
        }
        
        // Center text vertically
        val textBounds = android.graphics.Rect()
        labelPaint.getTextBounds(label, 0, label.length, textBounds)
        val textY = rect.center.y + textBounds.height() / 2f - 2f
        
        // Draw label
        nativeCanvas.drawText(
            label,
            rect.left + labelLeftPadding,
            textY,
            labelPaint
        )
        
        // Draw confidence
        val labelWidth = labelPaint.measureText(label)
        val confX = rect.left + labelLeftPadding + labelWidth + labelTextSpacing
        
        nativeCanvas.drawText(
            confidence,
            confX,
            textY,
            confPaint
        )
    }
}

private fun getColorForDetection(detection: Detection): Color {
    return categoryColors[detection.className] 
        ?: defaultDetectionColors[detection.classId % defaultDetectionColors.size]
}

// Local type definitions to avoid conflicts
private data class LabelInfo(
    val detection: Detection,
    val rect: Rect,
    val boxBounds: Rect,
    val color: Color
)