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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.penki.data.TimetableParser

/**
 * Admino skirtukas „Tvarkaraštis“: vienu paspaudimu iš naujo nuskaito paskelbtą
 * Google Sheets lentelę ir ja atnaujina mokytojus, pamokas bei klases serveryje.
 */
@Composable
internal fun TimetableTab(
    state: UiState,
    onRefetch: (String) -> Unit,
    onAssign: (String, String?) -> Unit,
    onMatch: () -> Unit,
    onApply: () -> Unit
) {
    var url by remember { mutableStateOf(TimetableParser.DEFAULT_URL) }
    var edited by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    val profiles = state.teachers.filter { it.isTeacher }

    // Serveryje išsaugota nuoroda įrašoma tada, kai duomenys ateina – bet neperrašome
    // to, ką administratorius jau pradėjo redaguoti.
    LaunchedEffect(state.ttConfig) {
        val saved = state.ttConfig?.url?.takeIf { it.isNotBlank() }
        if (!edited && saved != null) url = saved
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Tvarkaraštis iš Google Sheets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Paspaudus „Atsiųsti iš naujo“ programa nuskaito paskelbtą lentelę, " +
                        "sukuria mokytojus ir jų pamokas bei klases. Paskyrų priskyrimai išlieka.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        edited = true
                    },
                    label = { Text("Paskelbtos lentelės nuoroda") },
                    supportingText = { Text("Nuoroda su /pubhtml arba /pub ir output=csv") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onRefetch(url) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Atsiųsti iš naujo")
                }

                val config = state.ttConfig
                if (config?.last_synced_at != null || !config?.last_summary.isNullOrBlank()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            config?.last_synced_at?.let { synced ->
                                Text(
                                    "Paskutinis atsiuntimas: " + synced.take(16).replace('T', ' '),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (!config?.last_summary.isNullOrBlank()) {
                                Text(
                                    config?.last_summary.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onMatch,
                        enabled = !state.busy && state.ttTeachers.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Priskirti pagal vardą") }

                    OutlinedButton(
                        onClick = onApply,
                        enabled = !state.busy && state.ttTeachers.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Taikyti klases") }
                }
            }
        }

        item { SectionTitle("Mokytojai (${state.ttTeachers.size})") }

        if (state.ttTeachers.isEmpty()) {
            item {
                Text(
                    "Tvarkaraštis dar neatsiųstas. Paspauskite „Atsiųsti iš naujo“.",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(state.ttTeachers) { teacher ->
            val account = profiles.firstOrNull { it.id == teacher.profile_id }
            val details = listOfNotNull(
                teacher.subject.takeIf { it.isNotBlank() },
                teacher.homeroom.takeIf { it.isNotBlank() }?.let { "auklėja $it" },
                teacher.room.takeIf { it.isNotBlank() }?.let { "kab. $it" }
            ).joinToString(" · ")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(teacher.name, fontWeight = FontWeight.Medium)
                    if (details.isNotBlank()) {
                        Text(
                            details,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${teacher.lesson_count} pamokų" +
                            if (teacher.class_names.isNotBlank()) " · ${teacher.class_names}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Box {
                        OutlinedButton(
                            onClick = { menuFor = teacher.id },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                account?.let { "Paskyra: ${it.full_name.ifBlank { it.email }}" }
                                    ?: "Priskirti paskyrą"
                            )
                        }
                        DropdownMenu(
                            expanded = menuFor == teacher.id,
                            onDismissRequest = { menuFor = null }
                        ) {
                            profiles.forEach { profile ->
                                DropdownMenuItem(
                                    text = { Text(profile.full_name.ifBlank { profile.email }) },
                                    onClick = {
                                        menuFor = null
                                        onAssign(teacher.id, profile.id)
                                    }
                                )
                            }
                            if (teacher.hasAccount) {
                                DropdownMenuItem(
                                    text = { Text("Atšaukti priskyrimą") },
                                    onClick = {
                                        menuFor = null
                                        onAssign(teacher.id, null)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
