package lt.penki.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Balų spalvos: nuo gilios plytų raudonos iki miško žalios. */
fun scoreColor(score: Int): Color = when (score) {
    1 -> Color(0xFF8C2F2F)
    2 -> Color(0xFFA8582F)
    3 -> Color(0xFFAD8A3C)
    4 -> Color(0xFF4C8468)
    else -> PenkiPalette.ForestMid
}

fun scoreLabel(score: Int): String = when (score) {
    1 -> "Labai blogas"
    2 -> "Blogas"
    3 -> "Vidutinis"
    4 -> "Geras"
    else -> "Labai geras"
}

/** Balų ženkliukas: gradientinis apskritimas su švelniu vidiniu rėmeliu. */
@Composable
fun ScoreBadge(score: Int, size: Dp = 40.dp) {
    val base = scoreColor(score)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(8.dp, CircleShape, clip = false, ambientColor = base.copy(alpha = 0.35f), spotColor = base.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(base.copy(alpha = 0.92f), base)
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = (size.value * 0.42f).sp
        )
    }
}

/**
 * Plonas progreso žiedas su gradientine eiga. Viduje galima dėti bet kokį
 * turinį (pvz. skaičių), todėl tinka ir dienos suvestinei.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    stroke: Dp = 9.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val sweep = (progress.coerceIn(0f, 1f)) * 360f
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = scheme.onSurface.copy(alpha = 0.10f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (sweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            PenkiPalette.Jade,
                            PenkiPalette.Moss,
                            PenkiPalette.ForestMid,
                            PenkiPalette.ForestSoft,
                            PenkiPalette.Jade
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}

/** Stiklinis pranešimas su spalvotu akcentu kairėje. */
@Composable
fun NoticeBanner(text: String, isError: Boolean, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminanceIsDark()
    val accent = if (isError) scheme.error else PenkiPalette.ForestSoft
    val container = when {
        isError && dark -> scheme.errorContainer.copy(alpha = 0.92f)
        isError -> Color(0xFFF7E6E1)
        dark -> scheme.surface.copy(alpha = 0.92f)
        else -> Color.White.copy(alpha = 0.88f)
    }
    val content = if (isError) scheme.error else scheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                12.dp,
                RoundedCornerShape(22.dp),
                clip = false,
                ambientColor = accent.copy(alpha = 0.30f),
                spotColor = accent.copy(alpha = 0.30f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(container)
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = content
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Uždaryti",
                tint = content.copy(alpha = 0.8f)
            )
        }
    }
}

/** Sekcijos antraštė: mažos didžiosios raidės, plona žalia linija ir skaitliukas. */
@Composable
fun SectionTitle(text: String, trailing: String? = null, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f)
                .size(width = 0.dp, height = 1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(scheme.outline.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
        if (trailing != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.primary.copy(alpha = 0.10f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary
                )
            }
        }
    }
}

/** Mažas suapvalintas ženkliukas su pavadinimu ir reikšme. */
@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.primary.copy(alpha = 0.08f))
            .border(1.dp, scheme.primary.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.primary
        )
    }
}

/** Pilis (pill) formos skirtuko mygtukas. */
@Composable
fun PenkiTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) ForestGradient
                else Brush.linearGradient(
                    listOf(
                        scheme.surface.copy(alpha = 0.80f),
                        scheme.surface.copy(alpha = 0.50f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else Color.White.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) PenkiPalette.Cream else scheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

/** Pagrindinis gradientinis mygtukas. */
@Composable
fun ForestButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .shadow(
                if (enabled) 14.dp else 0.dp,
                shape,
                clip = false,
                ambientColor = PenkiPalette.Forest.copy(alpha = 0.35f),
                spotColor = PenkiPalette.Forest.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                if (enabled) ForestGradient
                else Brush.linearGradient(listOf(PenkiPalette.Sand, PenkiPalette.SandDeep))
            )
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.22f else 0.30f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) PenkiPalette.Cream else PenkiPalette.Ink.copy(alpha = 0.55f)
        )
    }
}

/** Antrinis mygtukas: permatomas, su plonu žaliu rėmeliu. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(scheme.surface.copy(alpha = 0.55f))
            .border(1.dp, scheme.primary.copy(alpha = if (enabled) 0.30f else 0.15f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (enabled) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

/** Apvalus stiklinis ikonos mygtukas antraštėms. */
@Composable
fun SoftIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(scheme.surface.copy(alpha = 0.62f))
            .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = scheme.onSurface,
            modifier = Modifier.size(19.dp)
        )
    }
}

fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        LocalDate.parse(iso.take(10))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale("lt", "LT")))
    } catch (_: Exception) {
        iso.take(10)
    }
}

fun todayLongText(): String {
    return try {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale("lt", "LT")))
    } catch (_: Exception) {
        LocalDate.now().toString()
    }
}

fun monthDay(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        LocalDate.parse(iso.take(10))
            .format(DateTimeFormatter.ofPattern("MM-dd", Locale("lt", "LT")))
    } catch (_: Exception) {
        iso.take(10)
    }
}

/** Pagalbinė funkcija, kad nereikėtų importuoti luminance kiekviename faile. */
fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

/** Minkštas žalias šešėlis bet kokiai formai (logotipo ženklui, plūduriuojantiems elementams). */
fun Modifier.shadowSoft(shape: Shape, elevation: Dp = 14.dp): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = PenkiPalette.Forest.copy(alpha = 0.30f),
    spotColor = PenkiPalette.Forest.copy(alpha = 0.34f)
)
