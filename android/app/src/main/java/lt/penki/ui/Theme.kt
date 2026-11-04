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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * „penki“ paletė: šiltas smėlis (beige) kaip pagrindas ir gili tamsiai miško
 * žalia kaip akcentas. Auksinė žalvaros (brass) detalė naudojama tik labai
 * saikingai – smulkiems ženkliukams ir akcentams.
 */
object PenkiPalette {
    val Cream = Color(0xFFFDF8EF)
    val Paper = Color(0xFFF8F1E3)
    val Beige = Color(0xFFF1E7D6)
    val BeigeDeep = Color(0xFFE7DAC4)
    val Sand = Color(0xFFDCC9A8)
    val SandDeep = Color(0xFFC8B189)

    val Brass = Color(0xFFB08A4A)
    val BrassSoft = Color(0xFFE3CB9E)

    val Forest = Color(0xFF10291F)
    val ForestMid = Color(0xFF1B4636)
    val ForestSoft = Color(0xFF2F6B53)
    val Moss = Color(0xFF5C8F76)
    val Jade = Color(0xFF93C1A9)
    val Mist = Color(0xFFE7F0E9)

    val Ink = Color(0xFF14211B)
    val InkSoft = Color(0xFF4C5C52)
    val Error = Color(0xFF8C2F2F)
    val ErrorInk = Color(0xFF6E2424)

    val Night = Color(0xFF0A1913)
    val NightSurface = Color(0xFF11291F)
    val NightLine = Color(0xFF284337)
}

/** Tamsiai žalios „šviesos“ gradientas – mygtukams, ženkliukams ir aktyviems elementams. */
val ForestGradient: Brush = Brush.linearGradient(
    listOf(PenkiPalette.ForestSoft, PenkiPalette.ForestMid, PenkiPalette.Forest)
)

private val LightColors = lightColorScheme(
    primary = PenkiPalette.Forest,
    onPrimary = PenkiPalette.Cream,
    primaryContainer = PenkiPalette.Mist,
    onPrimaryContainer = PenkiPalette.Forest,
    secondary = PenkiPalette.ForestSoft,
    onSecondary = PenkiPalette.Cream,
    secondaryContainer = PenkiPalette.Mist,
    onSecondaryContainer = PenkiPalette.Forest,
    tertiary = PenkiPalette.Brass,
    onTertiary = PenkiPalette.Cream,
    background = PenkiPalette.Beige,
    onBackground = PenkiPalette.Ink,
    surface = PenkiPalette.Cream,
    onSurface = PenkiPalette.Ink,
    surfaceVariant = PenkiPalette.BeigeDeep,
    onSurfaceVariant = PenkiPalette.InkSoft,
    surfaceTint = PenkiPalette.Forest,
    outline = PenkiPalette.SandDeep,
    outlineVariant = Color(0xFFD9CCB6),
    error = PenkiPalette.Error,
    onError = PenkiPalette.Cream,
    errorContainer = Color(0xFFF4E1DA),
    onErrorContainer = PenkiPalette.ErrorInk
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD8E8DD),
    onPrimary = PenkiPalette.Forest,
    primaryContainer = PenkiPalette.ForestMid,
    onPrimaryContainer = PenkiPalette.Cream,
    secondary = PenkiPalette.Jade,
    onSecondary = PenkiPalette.Night,
    secondaryContainer = PenkiPalette.NightLine,
    onSecondaryContainer = PenkiPalette.Cream,
    tertiary = PenkiPalette.BrassSoft,
    onTertiary = PenkiPalette.Night,
    background = PenkiPalette.Night,
    onBackground = PenkiPalette.Cream,
    surface = PenkiPalette.NightSurface,
    onSurface = PenkiPalette.Cream,
    surfaceVariant = PenkiPalette.NightLine,
    onSurfaceVariant = Color(0xFFC0D2C7),
    surfaceTint = PenkiPalette.Jade,
    outline = Color(0xFF6F8A7C),
    outlineVariant = PenkiPalette.NightLine,
    error = Color(0xFFE9A8A8),
    onError = Color(0xFF3B0B0B),
    errorContainer = Color(0xFF4A2020),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val PenkiShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Serifinė šeima antraštėms – suteikia šiltą, „žurnalinį“ charakterį, o
 * tekstas ir sąsaja lieka sausiški ir lengvai skaitomi.
 */
private val Display = FontFamily.Serif

private val PenkiTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 46.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.8).sp
    ),
    displaySmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 27.sp
    ),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.5.sp, letterSpacing = 0.6.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.5.sp, letterSpacing = 1.2.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp)
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
 * Fonas: trys sluoksniai – šiltas vertikalus gradientas, sulieti šviesos lopai
 * (mėlyna–samana viršuje, gili žalia apačioje, smėlis dešinėje) ir švelnus
 * šviesus atspindys viršuje. Nuo Android 12 lopai tikrai suliejami, senesniuose
 * įrenginiuose lieka tik gradientas.
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
                    0f to if (dark) PenkiPalette.Night else PenkiPalette.Cream,
                    0.55f to scheme.background,
                    1f to if (dark) PenkiPalette.Night else PenkiPalette.BeigeDeep
                )
            )
    ) {
        // Šiltas rytinis atspindys viršuje.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .background(
                    Brush.verticalGradient(
                        0f to if (dark) PenkiPalette.ForestSoft.copy(alpha = 0.22f)
                        else Color.White.copy(alpha = 0.70f),
                        1f to Color.Transparent
                    )
                )
        )

        GlowBlob(
            color = if (dark) PenkiPalette.ForestSoft.copy(alpha = 0.55f)
            else PenkiPalette.Moss.copy(alpha = 0.34f),
            size = 380.dp,
            offsetX = (-140).dp,
            offsetY = (-150).dp,
            blurRadius = 110.dp,
            alignment = Alignment.TopStart
        )
        GlowBlob(
            color = if (dark) PenkiPalette.ForestMid.copy(alpha = 0.45f)
            else PenkiPalette.Sand.copy(alpha = 0.65f),
            size = 340.dp,
            offsetX = 130.dp,
            offsetY = 190.dp,
            blurRadius = 120.dp,
            alignment = Alignment.BottomEnd
        )
        GlowBlob(
            color = if (dark) PenkiPalette.Jade.copy(alpha = 0.12f)
            else PenkiPalette.BrassSoft.copy(alpha = 0.30f),
            size = 260.dp,
            offsetX = 90.dp,
            offsetY = (-60).dp,
            blurRadius = 90.dp,
            alignment = Alignment.CenterEnd
        )
        GlowBlob(
            color = if (dark) PenkiPalette.ForestSoft.copy(alpha = 0.30f)
            else PenkiPalette.ForestSoft.copy(alpha = 0.14f),
            size = 320.dp,
            offsetX = (-110).dp,
            offsetY = 120.dp,
            blurRadius = 100.dp,
            alignment = Alignment.BottomStart
        )

        content()
    }
}

@Composable
private fun BoxScope.GlowBlob(
    color: Color,
    size: Dp,
    offsetX: Dp,
    offsetY: Dp,
    blurRadius: Dp,
    alignment: Alignment
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .size(size)
            .blur(blurRadius)
            .background(color, CircleShape)
    )
}

/**
 * „Frosted glass“ paviršius: permatoma pluta, šviesus viršutinis atspindys,
 * plonas baltas rėmelis ir minkštas žalias šešėlis. Visas turinys lieka
 * skaitomas tiek šviesioje, tiek tamsioje temoje.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(30.dp),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < 0.5f
    val top = if (dark) scheme.surface.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.80f)
    val bottom = if (dark) scheme.surface.copy(alpha = 0.66f) else Color.White.copy(alpha = 0.44f)
    val border = if (dark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.92f)
    val sheen = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.34f)

    Box(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = PenkiPalette.Forest.copy(alpha = 0.18f),
                spotColor = PenkiPalette.Forest.copy(alpha = 0.24f)
            )
            .clip(shape)
            .background(
                when {
                    tint != null -> Brush.verticalGradient(
                        listOf(tint.copy(alpha = 0.92f), tint.copy(alpha = 0.70f))
                    )
                    else -> Brush.verticalGradient(listOf(top, bottom))
                }
            )
            .border(1.dp, border, shape)
    ) {
        // Stiklo atspindys: šviesi juosta viršuje, ištirpstanti į permatomą.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to sheen,
                        0.30f to Color.Transparent,
                        1f to Color.Transparent
                    )
                )
        )
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

/** Stiklinė kortelė – ta pati [GlassPanel], tik su kitokiu numatytuoju formatu. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    GlassPanel(
        modifier = modifier,
        shape = shape,
        contentPadding = contentPadding,
        content = content
    )
}
