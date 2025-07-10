package io.github.tommihonkanen.yolov4detector.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.tommihonkanen.yolov4detector.R
import io.github.tommihonkanen.yolov4detector.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun FABMenu(
    isPaused: Boolean,
    isFlashOn: Boolean,
    onPauseToggle: () -> Unit,
    onFlashToggle: () -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash FAB
        SmallFloatingActionButton(
            onClick = onFlashToggle,
            containerColor = md_theme_surfaceContainer,
            contentColor = if (isFlashOn) warning_yellow else md_theme_onSurface
        ) {
            Icon(
                painter = painterResource(
                    id = if (isFlashOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
                ),
                contentDescription = if (isFlashOn) "Flash On" else "Flash Off"
            )
        }
        
        // Flip Camera FAB
        SmallFloatingActionButton(
            onClick = onFlipCamera,
            containerColor = md_theme_surfaceContainer,
            contentColor = md_theme_onSurface
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_camera_flip),
                contentDescription = "Flip Camera"
            )
        }
        
        // Main FAB (Pause/Play)
        FloatingActionButton(
            onClick = onPauseToggle,
            containerColor = md_theme_primary,
            contentColor = md_theme_onPrimary
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
                ),
                contentDescription = if (isPaused) "Play" else "Pause"
            )
        }
    }
}

@Composable
private fun FABMenuItem(
    icon: Int,
    label: String,
    iconTint: Color = md_theme_onSurface,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = md_theme_surfaceContainer,
            shadowElevation = 2.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 12.sp,
                color = md_theme_onSurface
            )
        }
        
        // Mini FAB
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = md_theme_surfaceContainer,
            contentColor = iconTint
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label
            )
        }
    }
}

@Composable
fun SettingsFAB(
    onClick: () -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = md_theme_surfaceContainer,
            contentColor = md_theme_onSurface,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tune),
                contentDescription = "Settings"
            )
        }
    }
}