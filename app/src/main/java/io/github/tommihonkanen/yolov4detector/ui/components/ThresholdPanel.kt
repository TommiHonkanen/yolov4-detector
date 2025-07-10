package io.github.tommihonkanen.yolov4detector.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tommihonkanen.yolov4detector.ui.theme.*

@Composable
fun ThresholdPanel(
    isOpen: Boolean,
    confidenceThreshold: Float,
    nmsThreshold: Float,
    onConfidenceChange: (Float) -> Unit,
    onNmsChange: (Float) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offsetX by animateDpAsState(
        targetValue = if (isOpen) 0.dp else SettingsPanelWidth,
        animationSpec = tween(durationMillis = SettingsPanelAnimationDuration),
        label = "panel_slide"
    )
    
    if (isOpen || offsetX < SettingsPanelWidth) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrim when panel is open
            if (isOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onDismiss()
                        }
                )
            }
            
            // Panel positioned at the right edge, centered vertically
            Card(
                modifier = modifier
                    .width(SettingsPanelWidth)
                    .wrapContentHeight()
                    .align(Alignment.CenterEnd)
                    .offset(x = offsetX)
                    .alpha(0.95f),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = md_theme_surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Detection Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = md_theme_onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Confidence Threshold
                        ThresholdSlider(
                            label = "Confidence Threshold",
                            value = confidenceThreshold,
                            onValueChange = onConfidenceChange
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // NMS Threshold
                        ThresholdSlider(
                            label = "NMS Threshold",
                            value = nmsThreshold,
                            onValueChange = onNmsChange
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Reset Button
                        TextButton(
                            onClick = onReset,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Reset to Defaults",
                                fontSize = 14.sp,
                                color = md_theme_primary
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun ThresholdSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = md_theme_onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..0.9f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = md_theme_primary,
                    activeTrackColor = md_theme_primary,
                    inactiveTrackColor = md_theme_surfaceVariant
                )
            )
            
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 12.sp,
                color = md_theme_primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(45.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}