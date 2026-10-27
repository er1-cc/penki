package lt.penki.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import lt.penki.data.Invite
import lt.penki.data.LessonTime
import lt.penki.data.Lessons
import lt.penki.data.Profile
import lt.penki.data.Rating
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    state: UiState,
    diagnostics: List<Pair<String, String>>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDismissNotice: () -> Unit,
    onSetActive: (String, Boolean) -> Unit,
    onSetAdmin: (String, Boolean) -> Unit,
    onApprove: (String) -> Unit,
    onCreateInvite: (String, (String) -> Unit) -> Unit,
    onDeleteInvite: (String) -> Unit,
    onAddClass: (String) -> Unit,
    onDeleteClass: (String) -> Unit,
    onToggleTeacherClass: (String, String, Boolean) -> Unit,
    onSaveLessonTime: (Int, String) -> Unit,
    onAddLesson: () -> Unit,
    onRemoveLesson: (Int) -> Unit,
    onResetLessonTimes: () -> Unit,
    onDebugCreateAccount: (String, String, String, Boolean, Int) -> Unit,
    onDebugSeedRatings: (String, Int) -> Unit,
    onDebugDeleteAllRatings: () -> Unit,
    onDebugDeleteTeacherRatings: (String) -> Unit,
    onDebugDeleteInvites: () -> Unit,
    onDebugTestNotification: (Int) -> Unit,
    onDebugRescheduleNotifications: () -> Unit,
    onDebugCancelNotifications: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Mokytojai", "Kvietimai", "Klasės", "Laikai", "Statistika", "Debug")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin panelė", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atgal")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atnaujinti")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            state.notice?.let { notice ->
                NoticeBanner(notice, state.noticeIsError, onDismissNotice)
            }

            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (tab) {
                0 -> TeachersTab(
                    state = state,
                    onSetActive = onSetActive,
                    onSetAdmin = onSetAdmin,
                    onApprove = onApprove,
                    onToggleClass = onToggleTeacherClass
                )

                1 -> InvitesTab(
                    state = state,
                    onCreateInvite = onCreateInvite,
                    onDeleteInvite = onDeleteInvite
                )

                2 -> ClassesTab(
                    state = state,
                    onAddClass = onAddClass,
                    onDeleteClass = onDeleteClass
                )

                3 -> LessonTimesTab(
                    state = state,
                    onSave = onSaveLessonTime,
                    onAdd = onAddLesson,
                    onRemove = onRemoveLesson,
                    onReset = onResetLessonTimes
                )

                4 -> StatsTab(state.allRatings)

                else -> DebugTab(
                    state = state,
                    diagnostics = diagnostics,
                    onCreateAccount = onDebugCreateAccount,
                    onSeedRatings = onDebugSeedRatings,
                    onDeleteAllRatings = onDebugDeleteAllRatings,
                    onDeleteTeacherRatings = onDebugDeleteTeacherRatings,
                    onDeleteInvites = onDebugDeleteInvites,
                    onTestNotification = onDebugTestNotification,
                    onReschedule = onDebugRescheduleNotifications,
                    onCancelNotifications = onDebugCancelNotifications
                )
            }
        }
    }
}

// ------------------------------------------------------------------ MOKYTOJAI

@Composable
private fun TeachersTab(
    state: UiState,
    onSetActive: (String, Boolean) -> Unit,
    onSetAdmin: (String, Boolean) -> Unit,
    onApprove: (String) -> Unit,
    onToggleClass: (String, String, Boolean) -> Unit
) {
    val myId = state.profile?.id.orEmpty()
    var editing by remember { mutableStateOf<Profile?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "Tik administratorius gali patvirtinti mokytoją, suteikti administratoriaus teises " +
                    "arba pakeisti, kurias klases mokytojas vertina. Naujas mokytojas prisiregistruoja " +
                    "su kvietimo kodu (skiltis „Kvietimai“).",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(state.teachers) { person ->
            val classIds = state.classIdsOf(person.id)
            val classNames = state.classes.filter { it.id in classIds }.map { it.name }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                person.full_name.takeIf { it.isNotBlank() } ?: "(be vardo)",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                person.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                roleLabel(person),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (person.active) scoreColor(5) else MaterialTheme.colorScheme.error
                            )
                        }
                        if (person.role != "pending" && person.id != myId) {
                            Switch(
                                checked = person.active,
                                onCheckedChange = { onSetActive(person.id, it) }
                            )
                        }
                    }

                    if (person.role != "pending") {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (classNames.isEmpty()) {
                                "Klasės: visos (nepriskirta)"
                            } else {
                                "Klasės: " + classNames.joinToString(", ")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (person.role) {
                            "pending" -> Button(
                                onClick = { onApprove(person.id) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) { Text("Patvirtinti mokytoju") }

                            "teacher" -> {
                                OutlinedButton(
                                    onClick = { editing = person },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) { Text("Klasės (${classNames.size})") }

                                OutlinedButton(
                                    onClick = { onSetAdmin(person.id, true) },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) { Text("Suteikti adminą") }
                            }

                            "admin" -> {
                                OutlinedButton(
                                    onClick = { editing = person },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) { Text("Klasės (${classNames.size})") }

                                if (person.id != myId) {
                                    OutlinedButton(
                                        onClick = { onSetAdmin(person.id, false) },
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) { Text("Nuimti adminą") }
                                }
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    val person = editing
    if (person != null) {
        val assigned = state.classIdsOf(person.id)
        AlertDialog(
            onDismissRequest = { editing = null },
            title = {
                Text("Klasės: " + (person.full_name.ifBlank { person.email }))
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Pažymėkite klases, kurias šis mokytojas vertina.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.classes.isEmpty()) {
                        Text(
                            "Nėra klasių – pirmiausia pridėkite jų skirtuke „Klasės“.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    state.classes.forEach { klass ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = klass.id in assigned,
                                onCheckedChange = { onToggleClass(person.id, klass.id, it) }
                            )
                            Text(klass.name)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Jei nieko nepažymėta, mokytojui rodomos visos klasės.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { editing = null }) { Text("Uždaryti") }
            }
        )
    }
}

private fun roleLabel(person: Profile): String = when {
    person.role == "admin" && person.active -> "Administratorius"
    person.role == "teacher" && person.active -> "Mokytojas"
    person.role == "pending" -> "Nepatvirtintas"
    else -> "Aktyvumo nėra"
}

// ------------------------------------------------------------------ KVIETIMAI

@Composable
private fun InvitesTab(
    state: UiState,
    onCreateInvite: (String, (String) -> Unit) -> Unit,
    onDeleteInvite: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var createdCode by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Kvietimas galioja 14 dienų ir yra vienkartinis. Kodą nusiųskite mokytojui – " +
                    "jis jo prireiks registracijai.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            PenkiTextField(email, { email = it }, "Mokytojo el. paštas")
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onCreateInvite(email) { code ->
                        createdCode = code
                        email = ""
                    }
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sukurti kvietimą") }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.invites) { invite ->
                InviteRow(invite, onDeleteInvite)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    createdCode?.let { code ->
        AlertDialog(
            onDismissRequest = { createdCode = null },
            title = { Text("Kvietimo kodas") },
            text = {
                Column {
                    Text("Nusiųskite šį kodą mokytojui:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        code,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(
                        android.content.ClipData.newPlainText("Kvietimo kodas", code)
                    )
                    createdCode = null
                }) { Text("Kopijuoti ir uždaryti") }
            },
            dismissButton = {
                TextButton(onClick = { createdCode = null }) { Text("Uždaryti") }
            }
        )
    }
}

@Composable
private fun InviteRow(invite: Invite, onDelete: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(invite.email, fontWeight = FontWeight.Medium)
                Text(
                    "Kodas: ${invite.code}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    if (invite.redeemed_at != null) "Panaudotas" else "Galioja iki ${formatDate(invite.expires_at)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (invite.redeemed_at != null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        scoreColor(4)
                    }
                )
            }
            if (invite.redeemed_at == null) {
                TextButton(onClick = { onDelete(invite.id) }) { Text("Atšaukti") }
            }
        }
    }
}

// ------------------------------------------------------------------ KLASĖS

@Composable
private fun ClassesTab(
    state: UiState,
    onAddClass: (String) -> Unit,
    onDeleteClass: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Klasės, kurias mokytojai gali vertinti.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            PenkiTextField(name, { name = it }, "Klasės pavadinimas (pvz. 7b)")
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onAddClass(name)
                    name = ""
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Pridėti klasę") }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.classes) { klass ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(klass.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        TextButton(onClick = { onDeleteClass(klass.id) }) { Text("Šalinti") }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ------------------------------------------------------------------ PAMOKŲ LAIKAI

@Composable
private fun LessonTimesTab(
    state: UiState,
    onSave: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onReset: () -> Unit
) {
    val rows = if (state.lessonTimes.isEmpty()) {
        Lessons.ends.mapIndexed { index, time ->
            LessonTime(index + 1, time.format(Lessons.timeFormatter))
        }
    } else {
        state.lessonTimes.sortedBy { it.lesson_no }
    }
    val lastLesson = rows.maxOfOrNull { it.lesson_no } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Pamokų pabaigos laikai. Nuo jų priklauso, kada mokytojams ateina priminimai. " +
                    "Pakeitimai iškart perplanuoja pranešimus šiame telefone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        rows.forEach { row ->
            LessonTimeRow(
                row = row,
                canRemove = rows.size > 1 && row.lesson_no == lastLesson,
                onSave = onSave,
                onRemove = onRemove
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onAdd,
                enabled = rows.size < Lessons.MAX_LESSONS && !state.busy,
                modifier = Modifier.weight(1f)
            ) { Text("Pridėti pamoką") }

            OutlinedButton(
                onClick = onReset,
                enabled = !state.busy,
                modifier = Modifier.weight(1f)
            ) { Text("Atstatyti") }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LessonTimeRow(
    row: LessonTime,
    canRemove: Boolean,
    onSave: (Int, String) -> Unit,
    onRemove: (Int) -> Unit
) {
    var text by remember(row.lesson_no, row.hhmm) { mutableStateOf(row.hhmm) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${row.lesson_no}-a",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(46.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(5) },
                    singleLine = true,
                    label = { Text("HH:mm") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(row.lesson_no, text) },
                    enabled = text != row.hhmm
                ) { Text("Išsaugoti") }
            }
            if (canRemove) {
                TextButton(onClick = { onRemove(row.lesson_no) }) { Text("Šalinti pamoką") }
            }
        }
    }
}

// ------------------------------------------------------------------ STATISTIKA

@Composable
private fun StatsTab(ratings: List<Rating>) {
    val today = LocalDate.now()
    val weekAgo = today.minusDays(7)

    val lastWeek = ratings.filter { rating ->
        rating.rated_on?.let {
            runCatching { LocalDate.parse(it.take(10)) >= weekAgo }.getOrDefault(false)
        } ?: false
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Suvestinė", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Iš viso vertinimų: ${ratings.size}")
                    Text(
                        "Vidurkis: " + if (ratings.isEmpty()) "–" else String.format("%.2f", ratings.map { it.score }.average())
                    )
                    Text(
                        "Per 7 dienas: ${lastWeek.size} vertinimų, vidurkis " +
                            if (lastWeek.isEmpty()) "–" else String.format("%.2f", lastWeek.map { it.score }.average())
                    )
                }
            }
        }

        item { SectionTitle("Pagal klases") }
        val byClass = ratings.groupBy { it.className }
            .map { (name, list) -> name to list }
            .sortedByDescending { it.second.size }
        if (byClass.isEmpty()) {
            item {
                Text(
                    "Kol kas nėra vertinimų.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(byClass) { entry ->
            StatsRow(
                title = entry.first,
                average = entry.second.map { it.score }.average(),
                count = entry.second.size
            )
        }

        item { SectionTitle("Pagal mokytojus") }
        val byTeacher = ratings.groupBy { it.teacherName }
            .map { (name, list) -> name to list }
            .sortedByDescending { it.second.size }
        items(byTeacher) { entry ->
            StatsRow(
                title = entry.first,
                average = entry.second.map { it.score }.average(),
                count = entry.second.size
            )
        }

        item { SectionTitle("Naujausi komentarai") }
        val comments = ratings.filter { it.comment.isNotBlank() }.take(25)
        if (comments.isEmpty()) {
            item {
                Text(
                    "Komentarų dar nėra.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(comments) { rating ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScoreBadge(rating.score, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${rating.className} · ${rating.teacherName}",
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(rating.comment, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${formatDate(rating.rated_on)} · ${rating.lesson_no}-a pamoka",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun StatsRow(title: String, average: Double, count: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(
                String.format("%.2f", average),
                fontWeight = FontWeight.Bold,
                color = scoreColor(average.roundToScore())
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "($count)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Double.roundToScore(): Int = kotlin.math.round(this).toInt().coerceIn(1, 5)

// ------------------------------------------------------------------ DEBUG

@Composable
private fun DebugTab(
    state: UiState,
    diagnostics: List<Pair<String, String>>,
    onCreateAccount: (String, String, String, Boolean, Int) -> Unit,
    onSeedRatings: (String, Int) -> Unit,
    onDeleteAllRatings: () -> Unit,
    onDeleteTeacherRatings: (String) -> Unit,
    onDeleteInvites: () -> Unit,
    onTestNotification: (Int) -> Unit,
    onReschedule: () -> Unit,
    onCancelNotifications: () -> Unit
) {
    var name by remember { mutableStateOf("Testinis Mokytojas") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("test1234") }
    var asAdmin by remember { mutableStateOf(false) }
    var ratingsCount by remember { mutableStateOf("10") }

    var teacherId by remember { mutableStateOf("") }
    var seedCount by remember { mutableStateOf("20") }
    var teacherMenuOpen by remember { mutableStateOf(false) }

    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val teachers = state.teachers.filter { it.role != "pending" }
    val selectedTeacher = teachers.firstOrNull { it.id == teacherId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Debug įrankiai skirti tik administratoriui. Sukurtos paskyros ir vertinimai – " +
                "tikri įrašai serveryje, todėl po testavimo juos ištrinkite.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Diagnostika", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                diagnostics.forEach { (label, value) ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(value, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Eksperimentinė paskyra", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Paskyra sukuriama iškart aktyvi. Jei el. pašto neįrašysite, jis bus sugeneruotas " +
                        "(pvz. test48213@example.com).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                PenkiTextField(name, { name = it }, "Vardas ir pavardė")
                Spacer(Modifier.height(8.dp))
                PenkiTextField(email, { email = it }, "El. paštas (nebūtinas)", KeyboardType.Email)
                Spacer(Modifier.height(8.dp))
                PenkiTextField(password, { password = it }, "Slaptažodis (bent 6 simboliai)")
                Spacer(Modifier.height(8.dp))
                PenkiTextField(
                    ratingsCount,
                    { ratingsCount = it.filter { char -> char.isDigit() }.take(3) },
                    "Kiek vertinimų sugeneruoti",
                    KeyboardType.Number
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = asAdmin, onCheckedChange = { asAdmin = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Suteikti administratoriaus teises")
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onCreateAccount(name, email, password, asAdmin, ratingsCount.toIntOrNull() ?: 0)
                    },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sukurti testinę paskyrą") }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Generuoti vertinimus", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Atsitiktiniai vertinimai per paskutines 14 dienų – tinka statistikai ir " +
                        "grafikams išbandyti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                androidx.compose.foundation.layout.Box {
                    OutlinedButton(
                        onClick = { teacherMenuOpen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedTeacher?.let { "Mokytojas: " + it.full_name.ifBlank { it.email } }
                            ?: "Pasirinkite mokytoją")
                    }
                    DropdownMenu(
                        expanded = teacherMenuOpen,
                        onDismissRequest = { teacherMenuOpen = false }
                    ) {
                        teachers.forEach { person ->
                            DropdownMenuItem(
                                text = { Text(person.full_name.ifBlank { person.email }) },
                                onClick = {
                                    teacherId = person.id
                                    teacherMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                PenkiTextField(
                    seedCount,
                    { seedCount = it.filter { char -> char.isDigit() }.take(3) },
                    "Kiek vertinimų (1–200)",
                    KeyboardType.Number
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSeedRatings(teacherId, seedCount.toIntOrNull() ?: 0) },
                        enabled = !state.busy && teacherId.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Generuoti") }

                    OutlinedButton(
                        onClick = { onDeleteTeacherRatings(teacherId) },
                        enabled = !state.busy && teacherId.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Ištrinti jo") }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Pranešimai", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Patikrinkite, ar priminimai ateina šiame telefone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onTestNotification(0) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Dabar") }

                    OutlinedButton(
                        onClick = { onTestNotification(5) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Po 5 s") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onReschedule,
                        modifier = Modifier.weight(1f)
                    ) { Text("Perplanuoti") }

                    OutlinedButton(
                        onClick = onCancelNotifications,
                        modifier = Modifier.weight(1f)
                    ) { Text("Atšaukti") }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Pavojingos operacijos", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Šie veiksmai negrįžtami. Naudokite tik testavimo metu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        confirm = "Ištrinti VISUS vertinimus?" to onDeleteAllRatings
                    },
                    enabled = !state.busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ištrinti visus vertinimus") }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { confirm = "Ištrinti visus kvietimus?" to onDeleteInvites },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ištrinti visus kvietimus") }
            }
        }

        if (state.debugLog.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Žurnalas", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    state.debugLog.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    val pending = confirm
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Patvirtinkite") },
            text = { Text(pending.first) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    pending.second()
                }) { Text("Taip, vykdyti") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Atšaukti") }
            }
        )
    }
}
