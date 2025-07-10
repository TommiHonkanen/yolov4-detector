package io.github.tommihonkanen.yolov4detector.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tommihonkanen.yolov4detector.R
import io.github.tommihonkanen.yolov4detector.ui.theme.*

@Composable
fun StatsCard(
    fps: Int,
    inferenceTime: Long,
    detectionCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .alpha(0.9f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = md_theme_surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // FPS Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_speed),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = md_theme_primary
                )
                Text(
                    text = "$fps FPS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = md_theme_onSurface
                )
            }
            
            // Inference Time Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_timer),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = md_theme_secondary
                )
                Text(
                    text = "${inferenceTime}ms",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = md_theme_onSurfaceVariant
                )
            }
            
            // Detection Count Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_detection),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = md_theme_tertiary
                )
                Text(
                    text = "$detectionCount objects",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = md_theme_onSurfaceVariant
                )
            }
        }
    }
}