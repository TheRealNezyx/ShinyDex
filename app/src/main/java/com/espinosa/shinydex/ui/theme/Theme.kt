package com.espinosa.shinydex.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val ShinyDexColors = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    primaryContainer = GoldDim,
    onPrimaryContainer = GoldBright,
    secondary = Bone,
    onSecondary = Ink,
    tertiary = GoldBright,
    onTertiary = Ink,
    background = Ink,
    onBackground = Bone,
    surface = InkSoft,
    onSurface = Bone,
    surfaceVariant = InkCard,
    onSurfaceVariant = Muted,
    outline = InkLine,
    outlineVariant = InkLine,
    error = ErrorRed,
    onError = Ink,
)

private val ShinyDexTypography = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp),
    titleLarge = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp),
)

@Composable
fun ShinyDexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShinyDexColors,
        typography = ShinyDexTypography,
        content = content,
    )
}
