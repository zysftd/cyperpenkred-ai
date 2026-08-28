package com.cyperpunkred.ai.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.cyperpunkred.ai.data.local.datastore.ThemeMode

private val RedDarkColorScheme = darkColorScheme(
    primary = MdRedPrimary80,
    onPrimary = MdRedPrimary20,
    primaryContainer = MdRedPrimary30,
    onPrimaryContainer = MdRedPrimary90,
    secondary = NeonPink,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF633B48),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = NeonBlue,
    onTertiary = Color(0xFF003547),
    tertiaryContainer = Color(0xFF004D61),
    onTertiaryContainer = Color(0xFFBDE9FF),
    error = MdRedError80,
    onError = MdRedError20,
    errorContainer = MdRedError30,
    onErrorContainer = MdRedError90,
    background = MdNeutral10,
    onBackground = MdNeutral90,
    surface = MdNeutral10,
    onSurface = MdNeutral90,
    surfaceVariant = MdNeutralVariant30,
    onSurfaceVariant = MdNeutralVariant80,
    outline = MdNeutralVariant60
)

private val RedLightColorScheme = lightColorScheme(
    primary = MdRedPrimary40,
    onPrimary = Color.White,
    primaryContainer = MdRedPrimary90,
    onPrimaryContainer = MdRedPrimary100,
    secondary = Color(0xFF775652),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF705C2E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCDFA6),
    onTertiaryContainer = Color(0xFF251A00),
    error = MdRedError40,
    onError = Color.White,
    errorContainer = MdRedError90,
    onErrorContainer = MdRedError100,
    background = MdNeutral99,
    onBackground = MdNeutral10,
    surface = MdNeutral99,
    onSurface = MdNeutral10,
    surfaceVariant = MdNeutralVariant90,
    onSurfaceVariant = MdNeutralVariant30,
    outline = MdNeutralVariant50
)

@Composable
fun CyberpunkRedTheme(
    themeMode: ThemeMode = ThemeMode.DYNAMIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) RedDarkColorScheme else RedLightColorScheme
            }
        }
        ThemeMode.RED -> if (darkTheme) RedDarkColorScheme else RedLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CyberpunkTypography,
        content = content
    )
}
