package lt.penki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val todayDate = remember { LocalDate.now() }
    val today = todayDate.toString()
    val weekday = todayDate.dayOfWeek.value
    val lessons = remember(Lessons.count) { Lessons.numbers.toList() }

    // Sąrašai suskaičiuojami kartą, o ne kiekvieno perpiešimo metu.
    val todaysRatings = remember(state.myRatings, today) {
        state.myRatings.filter { it.rated_on == today }
    }
    val ratedByLesson = remember(todaysRatings) { todaysRatings.groupBy { it.lesson_no } }
    val plannedByLesson = remember(state.myTimetable, weekday) {
        state.myTimetable
            .filter { it.weekday == weekday }
            .groupBy({ it.lesson_no }, { it.class_name })
            .mapValues { (_, names) -> names.distinct() }
    }
    val ratedCount = remember(todaysRatings) { todaysRatings.map { it.lesson_no }.distinct().size }

    Scaffold(
        containerColor = Color.Transparent,
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
                        TextButton(onClick = onAdmin) { Text("Admin") }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atnaujinti")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Nustatymai")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
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
                item { NoticeBanner(notice, state.noticeIsError, onDismissNotice) }
            }

            item {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Text(
                        todayLongText().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Po kiekvienos pamokos įvertink klasės elgesį nuo 1 iki 5. " +
                            "Pranešimą gausi iškart pasibaigus pamokai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusChip(
                            label = "Šiandien įvertinta",
                            value = "$ratedCount/${lessons.size}"
                        )
                        StatusChip(
                            label = "Pamokų šiandien",
                            value = plannedByLesson.keys.distinct().size.toString()
                        )
                    }
                }
            }

            item { SectionTitle("Šiandienos pamokos") }

            items(lessons) { lessonNo ->
                val rated = ratedByLesson[lessonNo].orEmpty()
                val planned = plannedByLesson[lessonNo].orEmpty()
                LessonRow(
                    lessonNo = lessonNo,
                    planned = planned,
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
                        style = MaterialTheme.typography.bodySmall,
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
private fun StatusChip(label: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LessonRow(
    lessonNo: Int,
    planned: List<String>,
    rated: List<Rating>,
    onRate: () -> Unit
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PenkiPalette.ForestMid, PenkiPalette.Forest)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    lessonNo.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$lessonNo-a pamoka",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "Baigiasi ${Lessons.endText(lessonNo)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (planned.isNotEmpty()) {
                    Text(
                        "Tvarkaraštis: " + planned.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (rated.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rated.take(3).forEach { rating ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                ScoreBadge(rating.score, size = 22.dp)
                                Text(
                                    rating.className,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onRate,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(if (rated.isEmpty()) "Įvertinti" else "Keisti")
            }
        }
    }
}

@Composable
private fun RatingRow(rating: Rating) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreBadge(rating.score, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${rating.className} · ${rating.lesson_no}-a pamoka",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
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
            Spacer(Modifier.width(8.dp))
            Text(
                scoreLabel(rating.score),
                style = MaterialTheme.typography.labelSmall,
                color = scoreColor(rating.score)
            )
        }
    }
}

/**
 * Vertinimo langas: stiklo panelė su pamokos, klasės, balo ir komentaro
 * pasirinkimu. Klasė pasiūloma pagal tvarkaraštį, o pakeitus pamoką –
 * pasiūlymas atnaujinamas.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingDialog(
    initialLesson: Int,
    initialClass: String? = null,
    plannedClasses: Map<Int, String> = emptyMap(),
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

    fun classForLesson(no: Int): String {
        val planned = plannedClasses[no]?.trim().orEmpty()
        if (planned.isBlank()) return ""
        return classes.firstOrNull { it.name.equals(planned, ignoreCase = true) }?.id.orEmpty()
    }

    LaunchedEffect(classes, initialClass) {
        if (classId.isBlank()) {
            classId = classes.firstOrNull { it.name.equals(initialClass?.trim(), ignoreCase = true) }?.id
                ?: classes.firstOrNull()?.id.orEmpty()
        }
    }

    // Apsauga nuo pasenusio klasės ID (pvz., klasė ištrinta serveryje).
    val selectedClassId = remember(classId, classes) {
        classes.firstOrNull { it.id == classId }?.id.orEmpty()
    }
    val selectedClass = classes.firstOrNull { it.id == selectedClassId }
    val lessons = remember(Lessons.count) { Lessons.numbers.toList() }

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (!busy) onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .heightIn(max = 640.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { },
                shape = RoundedCornerShape(32.dp),
                contentPadding = PaddingValues(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(46.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Pamokos vertinimas",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildString {
                            append("$lessonNo-a pamoka")
                            selectedClass?.let { append(" · ${it.name}") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(18.dp))
                    Text("Pamoka", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        lessons.forEach { number ->
                            val selected = number == lessonNo
                            PillButton(
                                text = number.toString(),
                                selected = selected,
                                onClick = {
                                    lessonNo = number
                                    val planned = classForLesson(number)
                                    if (planned.isNotBlank()) classId = planned
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Baigiasi ${Lessons.endText(lessonNo)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(18.dp))
                    Text("Klasė", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))

                    if (classes.isEmpty()) {
                        Text(
                            "Nėra klasių. Paprašyk administratorių pridėti klasių.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            classes.forEach { klass ->
                                val selected = klass.id == selectedClassId
                                PillButton(
                                    text = klass.name,
                                    selected = selected,
                                    onClick = { classId = klass.id }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Text("Klasės elgesys", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        (1..5).forEach { value ->
                            val selected = value == score
                            val base = scoreColor(value)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) {
                                            Brush.linearGradient(listOf(base, base.copy(alpha = 0.72f)))
                                        } else {
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                                                )
                                            )
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) Color.Transparent
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    )
                                    .clickable { score = value },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    value.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            score == 0 -> "1 – labai blogas, 5 – labai geras"
                            else -> "$score – ${scoreLabel(score)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (score == 0) MaterialTheme.colorScheme.onSurfaceVariant else scoreColor(score),
                        fontWeight = if (score == 0) FontWeight.Normal else FontWeight.Medium
                    )

                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { if (it.length <= 300) comment = it },
                        label = { Text("Komentaras (neprivalomas)") },
                        minLines = 2,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)
                        )
                    )

                    if (error != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            error.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) { Text("Atšaukti") }

                        Button(
                            enabled = !busy,
                            onClick = {
                                when {
                                    selectedClassId.isBlank() -> error = "Pasirinkite klasę."
                                    score !in 1..5 -> error = "Pasirinkite balą nuo 1 iki 5."
                                    else -> {
                                        error = null
                                        onSave(selectedClassId, lessonNo, score, comment)
                                    }
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.weight(2f)
                        ) { Text("Išsaugoti vertinimą", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    Brush.linearGradient(listOf(PenkiPalette.ForestMid, PenkiPalette.Forest))
                } else {
                    Brush.linearGradient(
                        listOf(
                            scheme.surface.copy(alpha = 0.70f),
                            scheme.surface.copy(alpha = 0.45f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else scheme.outline.copy(alpha = 0.40f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else scheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
