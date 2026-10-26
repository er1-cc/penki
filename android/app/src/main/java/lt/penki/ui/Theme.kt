package lt.penki.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PenkiColors = lightColorScheme(
    primary = Color(0xFF1B5E4B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EBD8),
    onPrimaryContainer = Color(0xFF002117),
    secondary = Color(0xFF4C6358),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE9DA),
    onSecondaryContainer = Color(0xFF092016),
    background = Color(0xFFF6F8F5),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF9),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDBE5DD),
    onSurfaceVariant = Color(0xFF404944),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    outline = Color(0xFF707974)
)

@Composable
fun PenkiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PenkiColors,
        typography = Typography(),
        content = content
    )
}
