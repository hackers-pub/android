package pub.hackers.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF262626),
    onPrimary = Color(0xFFFAFAFA),
    primaryContainer = Color(0xFFF5F5F5),
    onPrimaryContainer = Color(0xFF262626),
    secondary = Color(0xFFF5F5F5),
    onSecondary = Color(0xFF262626),
    secondaryContainer = Color(0xFFF5F5F5),
    onSecondaryContainer = Color(0xFF737373),
    tertiary = Color(0xFF737373),
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF262626),
    surface = Color.White,
    onSurface = Color(0xFF262626),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF737373),
    outline = Color(0xFFE5E5E5),
    outlineVariant = Color(0xFFE5E5E5),
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFAFAFA),
    onPrimary = Color(0xFF262626),
    primaryContainer = Color(0xFF404040),
    onPrimaryContainer = Color(0xFFFAFAFA),
    secondary = Color(0xFF404040),
    onSecondary = Color(0xFFFAFAFA),
    secondaryContainer = Color(0xFF404040),
    onSecondaryContainer = Color(0xFFA3A3A3),
    tertiary = Color(0xFFA3A3A3),
    onTertiary = Color(0xFF262626),
    background = Color(0xFF262626),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF262626),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFA3A3A3),
    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF333333),
    error = Color(0xFFEF4444),
    onError = Color(0xFF262626)
)

@Composable
fun HackersPubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
