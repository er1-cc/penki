package lt.penki.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import lt.penki.data.Klass
import lt.penki.data.Lessons
import lt.penki.data.Rating
import java.time.LocalDate

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
    val ratedCount = remember(todaysRatings) {
        todaysRatings.map { it.lesson_no }.distinct().size
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        TeacherTopBar(
            name = state.profile?.full_name?.takeIf { it.isNotBlank() } ?: "Mokytojas",
            isAdmin = state.isAdmin,
            onAdmin = onAdmin,
            onRefresh = onRefresh,
            onSettings = onSettings
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            state.notice?.let { notice ->
                item { NoticeBanner(notice, state.noticeIsError, onDismissNotice) }
            }

            item {
                HeroCard(
                    ratedCount = ratedCount,
                    total = lessons.size,
                    plannedCount = plannedByLesson.keys.distinct().size
                )
            }

            item {
                SectionTitle(
                    text = "Šiandienos pamokos",
                    trailing = "$ratedCount/${lessons.size}"
                )
            }

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

            item {
                SectionTitle(
                    text = "Paskutiniai vertinimai",
                    trailing = if (state.myRatings.isEmpty()) null else state.myRatings.size.toString()
                )
            }

            if (state.myRatings.isEmpty()) {
                item {
                    Text(
                        "Dar nėra vertinimų. Pasirink pamoką aukščiau ir įvertink klasę – " +
                            "istorija čia atsiras iškart po pirmo įrašo.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(state.myRatings.take(40)) { rating ->
                RatingRow(rating)
            }
        }
    }
}

@Composable
private fun TeacherTopBar(
    name: String,
    isAdmin: Boolean,
    onAdmin: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandMark(size = 36.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "penki",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.primary
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isAdmin) {
            TextButton(onClick = onAdmin) {
                Text(
                    text = "Admin",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary
                )
            }
        }
        SoftIconButton(
            icon = Icons.Default.Refresh,
            contentDescription = "Atnaujinti",
            onClick = onRefresh
        )
        Spacer(modifier = Modifier.width(8.dp))
        SoftIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Nustatymai",
            onClick = onSettings
        )
    }
}

@Composable
private fun HeroCard(ratedCount: Int, total: Int, plannedCount: Int) {
    val scheme = MaterialTheme.colorScheme
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        contentPadding = PaddingValues(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ŠIANDIEN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = todayLongText().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Įvertink klasės elgesį po kiekvienos pamokos – tai užtrunka kelias sekundes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            ProgressRing(
                progress = if (total > 0) ratedCount.toFloat() / total else 0f,
                size = 82.dp,
                stroke = 9.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = ratedCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.primary
                    )
                    Text(
                        text = "iš $total",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill(label = "Tvarkaraštyje", value = plannedCount.toString())
            StatPill(label = "Liko", value = (total - ratedCount).coerceAtLeast(0).toString())
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
    val scheme = MaterialTheme.colorScheme
    val done = rated.isNotEmpty()

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (done) ForestGradient
                        else Brush.linearGradient(
                            listOf(
                                scheme.surface.copy(alpha = 0.85f),
                                scheme.surface.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = if (done) Color.White.copy(alpha = 0.30f)
                        else scheme.outline.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lessonNo.toString(),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 19.sp,
                    color = if (done) PenkiPalette.Cream else scheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$lessonNo-a pamoka",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    MetaChip(text = "iki ${Lessons.endText(lessonNo)}")
                }
                if (planned.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        planned.take(3).forEach { className ->
                            MetaChip(text = className, tint = PenkiPalette.ForestSoft)
                        }
                    }
                }
                if (done) {
                    Spacer(modifier = Modifier.height(7.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rated.take(3).forEach { rating ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                ScoreBadge(rating.score, size = 22.dp)
                                Text(
                                    text = rating.className,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            if (done) {
                GhostButton(
                    text = "Keisti",
                    onClick = onRate
                )
            } else {
                ForestButton(
                    text = "Įvertinti",
                    onClick = onRate
                )
            }
        }
    }
}

@Composable
private fun RatingRow(rating: Rating) {
    val scheme = MaterialTheme.colorScheme
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreBadge(rating.score, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${rating.className} · ${rating.lesson_no}-a pamoka",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = formatDate(rating.rated_on),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
                if (rating.comment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.primary.copy(alpha = 0.06f))
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = rating.comment,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = scoreLabel(rating.score),
                style = MaterialTheme.typography.labelSmall,
                color = scoreColor(rating.score)
            )
        }
    }
}

@Composable
private fun MetaChip(
    text: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
    var notesExpanded by remember { mutableStateOf(false) }

    // 5 balams pastabų laukas lieka sulenktas, 4 ir mažiau – išsiskleidžia automatiškai.
    LaunchedEffect(score) {
        notesExpanded = score in 1..4
    }

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
    val scheme = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
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
                    .heightIn(max = 660.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { },
                shape = RoundedCornerShape(34.dp),
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
                            .background(scheme.onSurface.copy(alpha = 0.16f))
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Pamokos vertinimas",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append("$lessonNo-a pamoka")
                            selectedClass?.let { append(" · ${it.name}") }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    FieldLabel("Pamoka")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        lessons.forEach { number ->
                            PenkiTab(
                                text = number.toString(),
                                selected = number == lessonNo,
                                onClick = {
                                    lessonNo = number
                                    val planned = classForLesson(number)
                                    if (planned.isNotBlank()) classId = planned
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Baigiasi ${Lessons.endText(lessonNo)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    FieldLabel("Klasė")
                    Spacer(modifier = Modifier.height(8.dp))

                    if (classes.isEmpty()) {
                        Text(
                            text = "Nėra klasių. Paprašyk administratorių pridėti klasių.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.error
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            classes.forEach { klass ->
                                PenkiTab(
                                    text = klass.name,
                                    selected = klass.id == selectedClassId,
                                    onClick = { classId = klass.id }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    FieldLabel("Klasės elgesys")
                    Spacer(modifier = Modifier.height(10.dp))
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
                                            Brush.linearGradient(listOf(base, base.copy(alpha = 0.74f)))
                                        } else {
                                            Brush.linearGradient(
                                                listOf(
                                                    scheme.surface.copy(alpha = 0.80f),
                                                    scheme.surface.copy(alpha = 0.55f)
                                                )
                                            )
                                        }
                                    )
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) Color.White.copy(alpha = 0.55f)
                                        else scheme.outline.copy(alpha = 0.45f),
                                        shape = CircleShape
                                    )
                                    .clickable { score = value },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = value.toString(),
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selected) Color.White else scheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            score == 0 -> "1 – labai blogas, 5 – labai geras"
                            score == 5 -> "5 – labai geras · pastabų neprivaloma"
                            else -> "$score – ${scoreLabel(score)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (score == 0) scheme.onSurfaceVariant else scoreColor(score),
                        fontWeight = if (score == 0) FontWeight.Normal else FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    NotesSection(
                        expanded = notesExpanded,
                        comment = comment,
                        enabled = !busy,
                        onToggle = { notesExpanded = !notesExpanded },
                        onCommentChange = { if (it.length <= 300) comment = it }
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = error.orEmpty(),
                            color = scheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostButton(
                            text = "Atšaukti",
                            onClick = onDismiss,
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        )
                        ForestButton(
                            text = "Išsaugoti vertinimą",
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
                            enabled = !busy,
                            modifier = Modifier.weight(1.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pastabų sekcija: 5 balams lieka sulenkta (pakanka balo), 4 ir mažiau –
 * atsiveria automatiškai. Vartotojas bet kada gali ją atidaryti ar uždaryti
 * rankiniu būdu, o įrašytas tekstas išsaugomas ir sulenkus.
 */
@Composable
private fun NotesSection(
    expanded: Boolean,
    comment: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    onCommentChange: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surface.copy(alpha = 0.38f))
            .border(1.dp, scheme.outline.copy(alpha = 0.28f), shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.Button, onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Pastabos", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        expanded -> "Neprivaloma – papasakok, kas nutiko."
                        comment.isNotBlank() -> comment
                        else -> "Neprivaloma – palik tuščias, jei viskas gerai."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Sutraukti pastabas" else "Išplėsti pastabas",
                tint = scheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            ) {
                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    label = { Text("Komentaras") },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outline.copy(alpha = 0.45f),
                        focusedContainerColor = scheme.surface.copy(alpha = 0.55f),
                        unfocusedContainerColor = scheme.surface.copy(alpha = 0.40f),
                        cursorColor = scheme.primary,
                        focusedLabelColor = scheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${comment.length}/300",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
    }
}
