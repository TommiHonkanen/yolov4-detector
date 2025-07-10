package io.github.tommihonkanen.yolov4detector.ui.theme

import androidx.compose.ui.graphics.Color

// Material Design 3 Color System - Professional Dark Theme
// Primary Colors - Clean Blue
val md_theme_primary = Color(0xFF5B9FED)
val md_theme_onPrimary = Color(0xFF003258)
val md_theme_primaryContainer = Color(0xFF004880)
val md_theme_onPrimaryContainer = Color(0xFFD8E2FF)

// Secondary Colors - Neutral
val md_theme_secondary = Color(0xFFBCC7DC)
val md_theme_onSecondary = Color(0xFF263141)
val md_theme_secondaryContainer = Color(0xFF3C4758)
val md_theme_onSecondaryContainer = Color(0xFFD8E2F9)

// Tertiary Colors - Accent
val md_theme_tertiary = Color(0xFF4ECDC4)
val md_theme_onTertiary = Color(0xFF003733)
val md_theme_tertiaryContainer = Color(0xFF005048)
val md_theme_onTertiaryContainer = Color(0xFFB0F0E9)

// Surface Colors - Improved visibility
val md_theme_background = Color(0xFF1A1C23)
val md_theme_onBackground = Color(0xFFE1E2E8)
val md_theme_surface = Color(0xFF242730)
val md_theme_surfaceVariant = Color(0xFF42474E)
val md_theme_onSurface = Color(0xFFE1E2E8)
val md_theme_onSurfaceVariant = Color(0xFFC2C7CE)
val md_theme_surfaceTint = Color(0xFF5B9FED)

// Elevated Surfaces
val md_theme_surfaceContainer = Color(0xFF2A2D36)
val md_theme_surfaceContainerHigh = Color(0xFF32353F)
val md_theme_surfaceContainerHighest = Color(0xFF3A3D47)
val md_theme_surfaceContainerLow = Color(0xFF242730)
val md_theme_surfaceContainerLowest = Color(0xFF1A1C23)

// Error Colors
val md_theme_error = Color(0xFFFFB4AB)
val md_theme_onError = Color(0xFF690005)
val md_theme_errorContainer = Color(0xFF93000A)
val md_theme_onErrorContainer = Color(0xFFFFDAD6)

// Outline Colors
val md_theme_outline = Color(0xFF8C9198)
val md_theme_outlineVariant = Color(0xFF42474E)
val md_theme_scrim = Color(0xFF000000)

// App Specific Colors
val success_green = Color(0xFF4ECDC4)
val warning_yellow = Color(0xFFFFB74D)
val info_blue = Color(0xFF64B5F6)

// Detection Overlay Colors
val detection_box_stroke = Color(0xFF5B9FED)
val detection_box_fill = Color(0x1A5B9FED)
val detection_label_bg = Color(0xE61A1C23)

// Detection Category Colors
val categoryColors = mapOf(
    "person" to Color(0xFF5B9FED),
    "bicycle" to Color(0xFF4ECDC4),
    "car" to Color(0xFFFFB74D),
    "motorcycle" to Color(0xFFFF6B6B),
    "airplane" to Color(0xFF9B59B6),
    "bus" to Color(0xFFF39C12),
    "train" to Color(0xFFE74C3C),
    "truck" to Color(0xFF16A085),
    "boat" to Color(0xFF3498DB),
    "traffic light" to Color(0xFFE67E22),
    "stop sign" to Color(0xFFC0392B),
    "cat" to Color(0xFFF1C40F),
    "dog" to Color(0xFF8E44AD),
    "horse" to Color(0xFFD35400),
    "bird" to Color(0xFF27AE60)
)

val defaultDetectionColors = listOf(
    Color(0xFFFF6B6B),  // Soft Red
    Color(0xFF4ECDC4),  // Turquoise
    Color(0xFF45B7D1),  // Sky Blue
    Color(0xFFF7DC6F),  // Soft Yellow
    Color(0xFFBB8FCE),  // Soft Purple
    Color(0xFF85C1E2),  // Light Blue
    Color(0xFFF8B500),  // Orange
    Color(0xFF95E1D3),  // Mint
    Color(0xFFF38181),  // Coral
    Color(0xFFAA96DA)   // Lavender
)