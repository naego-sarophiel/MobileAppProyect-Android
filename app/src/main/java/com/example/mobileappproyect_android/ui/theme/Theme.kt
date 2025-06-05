package com.example.mobileappproyect_android.ui.theme // Ensure this matches your package

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define your dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = Purple80, // Example: A lighter purple for dark theme primary
    secondary = PurpleGrey80, // Example
    tertiary = Pink80, // Example
    background = Color(0xFF1C1B1F), // A common dark background
    surface = Color(0xFF1C1B1F), // Can be same as background or slightly different
    onPrimary = Color.Black, // Text/icon color on primary color
    onSecondary = Color.Black, // Text/icon color on secondary color
    onTertiary = Color.Black,
    onBackground = Color(0xFFE6E1E5), // Text/icon color on background
    onSurface = Color(0xFFE6E1E5), // Text/icon color on surface
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
    // Add other colors as needed (surfaceVariant, outline, etc.)
)

// Define your light theme colors (likely already there)
private val LightColorScheme = lightColorScheme(
    primary = Purple40, // Example
    secondary = PurpleGrey40, // Example
    tertiary = Pink40, // Example
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    error = Color(0xFFB3261E),
    onError = Color.White
    // Add other colors as needed
)

@Composable
fun MobileAppProyectAndroidTheme( // Your theme name
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic coloring is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb() // Or another appropriate color
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // You might also want to set the navigation bar color
            // window.navigationBarColor = colorScheme.surface.toArgb()
            // WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming you have Typography defined
        //shapes = Shapes,         // Assuming you have Shapes defined
        content = content
    )
}