package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PelletFlame,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF431407),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFD1D5DB),
    onSecondary = Color(0xFF1F2937),
    background = PelletBgDark,
    surface = PelletSurfaceDark,
    surfaceVariant = PelletSurfaceHighlightDark,
    outline = PelletBorderDark,
    outlineVariant = Color(0xFF262C38),
    onBackground = PelletTextPrimaryDark,
    onSurface = PelletTextPrimaryDark,
    onSurfaceVariant = PelletTextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PelletFlameDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF7C2D12),
    secondary = Color(0xFF4B5563),
    onSecondary = Color.White,
    background = PelletBgLight,
    surface = PelletSurfaceLight,
    surfaceVariant = Color(0xFFF3F4F6),
    outline = PelletBorderLight,
    outlineVariant = Color(0xFFE5E7EB),
    onBackground = PelletTextPrimaryLight,
    onSurface = PelletTextPrimaryLight,
    onSurfaceVariant = PelletTextSecondaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our sleek custom Apple-style theme by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
