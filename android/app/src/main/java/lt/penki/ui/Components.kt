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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun scoreColor(score: Int): Color = when (score) {
    1 -> Color(0xFFC62828)
    2 -> Color(0xFFEF6C00)
    3 -> Color(0xFFF9A825)
    4 -> Color(0xFF7CB342)
    else -> Color(0xFF2E7D32)
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
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(scoreColor(score)),
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isError) Color(0xFFFFE1E1) else Color(0xFFDDF3E6)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) Color(0xFF8C1D18) else Color(0xFF14532D)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Uždaryti",
                    tint = if (isError) Color(0xFF8C1D18) else Color(0xFF14532D)
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
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
