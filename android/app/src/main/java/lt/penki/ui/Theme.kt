package lt.penki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * „penki“ paletė: šilta smėlio (beige) bazė ir gili tamsiai miško žalia.
 * Šviesi ir tamsi schemos naudoja tas pačias šeimas, todėl stiklo efektas
 * išlieka įskaitomas abiem atvejais.
 */
object PenkiPalette {
    val Cream = Color(0xFFFBF6EC)
    val Beige = Color(0xFFF3EADB)
    val BeigeDeep = Color(0xFFE9DDC9)
    val Sand = Color(0xFFDCC9A9)
    val SandDeep = Color(0xFFC8B491)
    val Forest = Color(0xFF132F26)
    val ForestMid = Color(0xFF1E4A3B)
    val ForestSoft = Color(0xFF2F6B55)
    val Moss = Color(0xFF5C8F76)
    val Ink = Color(0xFF15221C)
    val Error = Color(0xFF8C2F2F)
    val Night = Color(0xFF0E1D17)
    val NightSurface = Color(0xFF152A22)
}

private val LightColors = lightColorScheme(
    primary = PenkiPalette.Forest,
    onPrimary = PenkiPalette.Cream,
    primaryContainer = PenkiPalette.Sand,
    onPrimaryContainer = PenkiPalette.Forest,
    secondary = PenkiPalette.ForestMid,
    onSecondary = PenkiPalette.Cream,
    secondaryContainer = PenkiPalette.BeigeDeep,
    onSecondaryContainer = PenkiPalette.Ink,
    tertiary = PenkiPalette.ForestSoft,
    onTertiary = PenkiPalette.Cream,
    background = PenkiPalette.Beige,
    onBackground = PenkiPalette.Ink,
    surface = PenkiPalette.Cream,
    onSurface = PenkiPalette.Ink,
    surfaceVariant = PenkiPalette.BeigeDeep,
    onSurfaceVariant = Color(0xFF4C5A50),
    outline = Color(0xFFB3A48C),
    outlineVariant = Color(0xFFD9CDB8),
    error = PenkiPalette.Error,
    onError = PenkiPalette.Cream
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD9E7DC),
    onPrimary = PenkiPalette.Forest,
    primaryContainer = PenkiPalette.ForestMid,
    onPrimaryContainer = PenkiPalette.Cream,
    secondary = PenkiPalette.Sand,
    onSecondary = PenkiPalette.Forest,
    secondaryContainer = PenkiPalette.ForestMid,
    onSecondaryContainer = PenkiPalette.Cream,
    tertiary = PenkiPalette.Moss,
    onTertiary = PenkiPalette.Night,
    background = PenkiPalette.Night,
    onBackground = PenkiPalette.Cream,
    surface = PenkiPalette.NightSurface,
    onSurface = PenkiPalette.Cream,
    surfaceVariant = Color(0xFF21382F),
    onSurfaceVariant = Color(0xFFC3D2C8),
    outline = Color(0xFF7F948A),
    outlineVariant = Color(0xFF2C443A),
    error = Color(0xFFE8A5A5),
    onError = Color(0xFF3B0B0B)
)

private val PenkiShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

private val PenkiTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
)

@Composable
fun PenkiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PenkiTypography,
        shapes = PenkiShapes,
        content = content
    )
}

/**
 * Švelnus smėlio–miško gradientas su neryškiais „šviesos lopais“.
 * Nuo Android 12 `blur` sukuria tikrą sulietą foną; senesniuose – gradientas.
 */
@Composable
fun PenkiBackground(content: @Composable BoxScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        scheme.background,
                        scheme.surfaceVariant.copy(alpha = if (dark) 0.55f else 0.9f)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(x = (-120).dp, y = (-110).dp)
                .blur(80.dp)
                .background(
                    if (dark) PenkiPalette.ForestSoft.copy(alpha = 0.55f)
                    else PenkiPalette.Moss.copy(alpha = 0.28f),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 110.dp, y = 90.dp)
                .blur(90.dp)
                .background(
                    if (dark) PenkiPalette.Sand.copy(alpha = 0.18f)
                    else PenkiPalette.Sand.copy(alpha = 0.55f),
                    CircleShape
                )
        )
        content()
    }
}

/**
 * „Frosted glass“ paviršius: permatoma pluta, šviesus viršutinis atspindys,
 * plonas baltas rėmelis ir minkštas šešėlis. Naudojamas visur, kur reikia
 * išlaikyti fono gradientą po turiniu.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val top = if (dark) scheme.surface.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.72f)
    val bottom = if (dark) scheme.surface.copy(alpha = 0.70f) else scheme.surface.copy(alpha = 0.58f)
    val border = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.85f)

    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                clip = false,
                ambientColor = PenkiPalette.Forest.copy(alpha = 0.20f),
                spotColor = PenkiPalette.Forest.copy(alpha = 0.24f)
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(1.dp, border, shape)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
