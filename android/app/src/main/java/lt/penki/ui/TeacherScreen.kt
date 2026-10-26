package lt.penki.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.penki.data.Klass
import lt.penki.data.Lessons
import lt.penki.data.Rating
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherScreen(
    state: UiState,
    onRate: (Int) -> Unit,
    onAdmin: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    onDismissNotice: () -> Unit
) {
    val today = LocalDate.now().toString()
    val todaysRatings = state.myRatings.filter { it.rated_on == today }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("penki", fontWeight = FontWeight.Bold)
                        Text(
                            state.profile?.full_name?.takeIf { it.isNotBlank() } ?: "Mokytojas",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    if (state.isAdmin) {
                        TextButton(
                            onClick = onAdmin,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) { Text("Admin") }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atnaujinti")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Nustatymai")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            state.notice?.let { notice ->
                item {
                    NoticeBanner(notice, state.noticeIsError, onDismissNotice)
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            todayLongText().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Po kiekvienos pamokos įvertink klasės elgesį nuo 1 iki 5. " +
                                "Pranešimą gausi iškart pasibaigus pamokai.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item { SectionTitle("Šiandienos pamokos") }

            items((1..7).toList()) { lessonNo ->
                val rated = todaysRatings.filter { it.lesson_no == lessonNo }
                LessonRow(
                    lessonNo = lessonNo,
                    rated = rated,
                    onRate = { onRate(lessonNo) }
                )
            }

            item { SectionTitle("Paskutiniai vertinimai") }

            if (state.myRatings.isEmpty()) {
                item {
                    Text(
                        "Dar nėra vertinimų. Pasirink pamoką aukščiau ir įvertink klasę.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(state.myRatings.take(40)) { rating ->
                RatingRow(rating)
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun LessonRow(lessonNo: Int, rated: List<Rating>, onRate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$lessonNo-a pamoka", fontWeight = FontWeight.Medium)
                Text(
                    "Baigiasi ${Lessons.endText(lessonNo)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (rated.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rated.take(3).forEach { rating ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ScoreBadge(rating.score, size = 24.dp)
                                Text(
                                    rating.className,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
            Button(onClick = onRate) {
                Text(if (rated.isEmpty()) "Įvertinti" else "Keisti")
            }
        }
    }
}

@Composable
private fun RatingRow(rating: Rating) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreBadge(rating.score)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${rating.className} · ${rating.lesson_no}-a pamoka",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    formatDate(rating.rated_on),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (rating.comment.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(rating.comment, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                scoreLabel(rating.score),
                style = MaterialTheme.typography.labelSmall,
                color = scoreColor(rating.score)
            )
        }
    }
}

@Composable
fun RatingDialog(
    initialLesson: Int,
    classes: List<Klass>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (classId: String, lessonNo: Int, score: Int, comment: String) -> Unit
) {
    var lessonNo by remember { mutableIntStateOf(initialLesson) }
    var classId by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var classMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(classes) {
        if (classId.isBlank()) {
            classId = classes.firstOrNull()?.id.orEmpty()
        }
    }

    val selectedClass = classes.firstOrNull { it.id == classId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pamokos vertinimas") },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    when {
                        classId.isBlank() -> error = "Pasirinkite klasę."
                        score !in 1..5 -> error = "Pasirinkite balą nuo 1 iki 5."
                        else -> {
                            error = null
                            onSave(classId, lessonNo, score, comment)
                        }
                    }
                }
            ) { Text("Išsaugoti") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Atšaukti") }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Pamoka", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..7).forEach { number ->
                        val selected = number == lessonNo
                        if (selected) {
                            Button(
                                onClick = { lessonNo = number },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("$number") }
                        } else {
                            OutlinedButton(
                                onClick = { lessonNo = number },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("$number") }
                        }
                    }
                }
                Text(
                    "Baigiasi ${Lessons.endText(lessonNo)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))
                Text("Klasė", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))

                if (classes.isEmpty()) {
                    Text(
                        "Nėra klasių. Paprašyk administratorių pridėti klasių.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Box {
                        OutlinedButton(
                            onClick = { classMenuOpen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedClass?.let { "Klasė: ${it.name}" } ?: "Pasirinkite klasę")
                        }
                        DropdownMenu(
                            expanded = classMenuOpen,
                            onDismissRequest = { classMenuOpen = false }
                        ) {
                            classes.forEach { klass ->
                                DropdownMenuItem(
                                    text = { Text(klass.name) },
                                    onClick = {
                                        classId = klass.id
                                        classMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Klasės elgesys", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (1..5).forEach { value ->
                        val selected = value == score
                        if (selected) {
                            Button(
                                onClick = { score = value },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = scoreColor(value)),
                                modifier = Modifier.weight(1f)
                            ) { Text("$value") }
                        } else {
                            OutlinedButton(
                                onClick = { score = value },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("$value") }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        score == 0 -> "1 – labai blogas, 5 – labai geras"
                        else -> "$score – ${scoreLabel(score)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (score == 0) MaterialTheme.colorScheme.onSurfaceVariant else scoreColor(score)
                )

                Spacer(Modifier.height(16.dp))
                PenkiTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 300) comment = it },
                    label = "Komentaras (neprivalomas)",
                    singleLine = false,
                    minLines = 2
                )

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}
