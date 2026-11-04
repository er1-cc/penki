package lt.penki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Balų spalvos: nuo gilios raudonos iki miško žalios. */
fun scoreColor(score: Int): Color = when (score) {
    1 -> Color(0xFF8C2F2F)
    2 -> Color(0xFFA95A2A)
    3 -> Color(0xFFB08627)
    4 -> PenkiPalette.Moss
    else -> PenkiPalette.ForestMid
}

fun scoreLabel(score: Int): String = when (score) {
    1 -> "Labai blogas"
    2 -> "Blogas"
    3 -> "Vidutinis"
    4 -> "Geras"
    else -> "Labai geras"
}

@Composable
fun ScoreBadge(score: Int, size: Dp = 40.dp) {
    val base = scoreColor(score)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(base, base.copy(alpha = 0.78f)))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.4f).sp
        )
    }
}

@Composable
fun NoticeBanner(text: String, isError: Boolean, onDismiss: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val background = when {
        isError && dark -> Color(0xFF4A2020)
        isError -> Color(0xFFF6E0DC)
        dark -> PenkiPalette.ForestMid
        else -> Color(0xFFE7EFE4)
    }
    val content = when {
        isError && dark -> Color(0xFFFFD9D3)
        isError -> Color(0xFF7A211C)
        dark -> PenkiPalette.Cream
        else -> PenkiPalette.Forest
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = background,
        contentColor = content,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Uždaryti",
                    tint = content
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
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
