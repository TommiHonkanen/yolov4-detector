package io.github.tommihonkanen.yolov4detector.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tommihonkanen.yolov4detector.R
import io.github.tommihonkanen.yolov4detector.ui.theme.*

@Composable
fun MainTopBar(
    modelName: String,
    onModelsClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .alpha(0.95f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = md_theme_surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "YOLOv4 Detector",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = md_theme_onSurface
                )
                
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Model: ",
                        fontSize = 12.sp,
                        color = md_theme_onSurfaceVariant
                    )
                    Text(
                        text = modelName,
                        fontSize = 12.sp,
                        color = md_theme_primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            TextButton(
                onClick = onModelsClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_model),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = md_theme_primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Models",
                    fontSize = 12.sp
                )
            }
            
            IconButton(
                onClick = onAboutClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_info),
                    contentDescription = "About",
                    tint = md_theme_onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SimpleTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .alpha(0.95f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = md_theme_surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ToolbarHeight)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = md_theme_onSurface
                )
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = md_theme_onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}