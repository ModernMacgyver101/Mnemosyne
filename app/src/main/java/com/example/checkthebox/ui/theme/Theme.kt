package com.hardlinecoding.checkthebox.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.hardlinecoding.checkthebox.ui.ColorTheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Define color schemes for different themes
private fun getColorScheme(colorTheme: ColorTheme, darkTheme: Boolean): ColorScheme {
    return when (colorTheme) {
        ColorTheme.DEFAULT -> if (darkTheme) DarkColorScheme else LightColorScheme
        ColorTheme.RED -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFFF8A80),
                onPrimary = Color.Black,
                primaryContainer = Color(0xFFB71C1C),
                onPrimaryContainer = Color.White,
                secondary = Color(0xFFFF5252)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFD32F2F),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFCDD2),
                onPrimaryContainer = Color(0xFFB71C1C),
                secondary = Color(0xFFFF1744)
            )
        }
        ColorTheme.BLUE -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF82B1FF),
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF0D47A1),
                onPrimaryContainer = Color.White,
                secondary = Color(0xFF448AFF)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF1976D2),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFBBDEFB),
                onPrimaryContainer = Color(0xFF0D47A1),
                secondary = Color(0xFF2979FF)
            )
        }
        ColorTheme.BLACK_WHITE -> if (darkTheme) {
            darkColorScheme(
                primary = Color.White,
                onPrimary = Color.Black,
                primaryContainer = Color.White,
                onPrimaryContainer = Color.Black,
                secondary = Color.Gray,
                background = Color.Black,
                surface = Color.Black
            )
        } else {
            lightColorScheme(
                primary = Color.Black,
                onPrimary = Color.White,
                primaryContainer = Color.Black,
                onPrimaryContainer = Color.White,
                secondary = Color.Gray,
                background = Color.White,
                surface = Color.White
            )
        }
        ColorTheme.YELLOW -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFFFFF8D),
                onPrimary = Color.Black,
                primaryContainer = Color(0xFFFBC02D),
                onPrimaryContainer = Color.Black,
                secondary = Color(0xFFFFFF00)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFFBC02D),
                onPrimary = Color.Black,
                primaryContainer = Color(0xFFFFF9C4),
                onPrimaryContainer = Color(0xFFFBC02D),
                secondary = Color(0xFFFFEA00)
            )
        }
        ColorTheme.ORANGE -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFFFD180),
                onPrimary = Color.Black,
                primaryContainer = Color(0xFFE65100),
                onPrimaryContainer = Color.White,
                secondary = Color(0xFFFFAB40)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFF57C00),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFE0B2),
                onPrimaryContainer = Color(0xFFE65100),
                secondary = Color(0xFFFF9100)
            )
        }
        ColorTheme.GREEN -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFB9F6CA),
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF1B5E20),
                onPrimaryContainer = Color.White,
                secondary = Color(0xFF69F0AE)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF388E3C),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFC8E6C9),
                onPrimaryContainer = Color(0xFF1B5E20),
                secondary = Color(0xFF00E676)
            )
        }
    }
}

@Composable
fun MnemosyneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorTheme: ColorTheme = ColorTheme.DEFAULT,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        colorTheme == ColorTheme.DEFAULT && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getColorScheme(colorTheme, darkTheme)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}