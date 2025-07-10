package io.github.tommihonkanen.yolov4detector.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tommihonkanen.yolov4detector.R
import io.github.tommihonkanen.yolov4detector.ui.components.SimpleTopBar
import io.github.tommihonkanen.yolov4detector.ui.theme.*

@Composable
fun AboutScreen(
    versionName: String,
    onBackClick: () -> Unit,
    onLinkClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        SimpleTopBar(
            title = "About",
            onBackClick = onBackClick
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Logo
            Image(
                painter = painterResource(id = R.drawable.darknet_logo_blue),
                contentDescription = "Darknet Logo",
                modifier = Modifier
                    .width(200.dp)
                    .height(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Name
            Text(
                text = "YOLOv4 Detector",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = md_theme_onSurface
            )
            
            // Version
            Text(
                text = "Version $versionName",
                fontSize = 14.sp,
                color = md_theme_onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // About Card
            InfoCard(
                title = "About",
                content = "This app implements real-time object detection with the YOLOv4 (You Only Look Once) neural network using OpenCV. Use Darknet to train your own models. Import your own weights in the Model Manager."
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Technologies Card
            InfoCard(title = "Technologies") {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TechItem("• Darknet - Open source neural network framework")
                    TechItem("• YOLOv4 - State-of-the-art object detection model")
                    TechItem("• OpenCV - Computer vision library")
                    TechItem("• Android CameraX - Modern camera API")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Links Card
            InfoCard(title = "Learn More") {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    LinkItem(
                        text = "Darknet/YOLO FAQ",
                        onClick = { onLinkClick("https://www.ccoderun.ca/programming/darknet_faq") }
                    )
                    LinkItem(
                        text = "GitHub Repository (Darknet)",
                        onClick = { onLinkClick("https://github.com/hank-ai/darknet") }
                    )
                    LinkItem(
                        text = "GitHub Repository (YOLOv4 Detector)",
                        onClick = { onLinkClick("https://github.com/TommiHonkanen/yolov4-detector-android") }
                    )
                    LinkItem(
                        text = "OpenCV Library",
                        onClick = { onLinkClick("https://opencv.org/") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: String? = null,
    contentComposable: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = md_theme_surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = md_theme_onSurface
            )
            
            content?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = md_theme_onSurfaceVariant,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            contentComposable?.invoke()
        }
    }
}

@Composable
private fun TechItem(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = md_theme_onSurfaceVariant
    )
}

@Composable
private fun LinkItem(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = md_theme_primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    )
}