package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryDarkContainer,
    onPrimaryContainer = OnPrimaryDarkContainer,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = PrimaryLightContainer,
    onPrimaryContainer = OnPrimaryLightContainer,
    background = BackgroundLight,
    onBackground = Color(0xFF1F2937),
    surface = SurfaceLight,
    onSurface = Color(0xFF1F2937),
    surfaceVariant = CardLight,
    onSurfaceVariant = Color(0xFF4B5563)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  accentColor: Long? = null,
  // Dynamic color is disabled to enforce modern consistent theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val baseColorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val colorScheme = if (accentColor != null) {
      baseColorScheme.copy(
          primary = Color(accentColor),
          primaryContainer = Color(accentColor).copy(alpha = 0.2f)
      )
  } else baseColorScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
      
      try {
          val activity = view.context as androidx.activity.ComponentActivity
          val style = if (darkTheme) {
              androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
          } else {
              androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
          }
          activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
      } catch (e: Exception) {
          // Fallback if context is not a ComponentActivity
      }
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
