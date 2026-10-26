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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import lt.penki.data.Invite
import lt.penki.data.Profile
import lt.penki.data.Rating
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    state: UiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDismissNotice: () -> Unit,
    onSetActive: (String, Boolean) -> Unit,
    onSetAdmin: (String, Boolean) -> Unit,
    onApprove: (String) -> Unit,
    onCreateInvite: (String, (String) -> Unit) -> Unit,
    onDeleteInvite: (String) -> Unit,
    onAddClass: (String) -> Unit,
    onDeleteClass: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val titles = listOf("Mokytojai", "Kvietimai", "Klasės", "Statistika")

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
                    onApprove = onApprove
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

                else -> StatsTab(state.allRatings)
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
    onApprove: (String) -> Unit
) {
    val myId = state.profile?.id.orEmpty()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "Tik administratorius gali patvirtinti mokytoją arba suteikti administratoriaus teises. " +
                    "Naujas mokytojas prisiregistruoja su kvietimo kodu (skiltis „Kvietimai“).",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(state.teachers) { person ->
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (person.role) {
                            "pending" -> Button(
                                onClick = { onApprove(person.id) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) { Text("Patvirtinti mokytoju") }

                            "teacher" -> OutlinedButton(
                                onClick = { onSetAdmin(person.id, true) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) { Text("Suteikti adminą") }

                            "admin" -> if (person.id != myId) {
                                OutlinedButton(
                                    onClick = { onSetAdmin(person.id, false) },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) { Text("Nuimti adminą") }
                            }

                            else -> Unit
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
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
                "Klasės, kurias mokytojai gali vertinti po pamokos.",
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
