package lt.penki.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun GoogleSheetsTab(
    state: UiState,
    onSave: (Boolean, String, String, Boolean) -> Unit
) {
    var enabled by remember { mutableStateOf(false) }
    var spreadsheetId by remember { mutableStateOf("") }
    var sheetName by remember { mutableStateOf("Vertinimai") }
    var sheetPerTeacher by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(state.sheetsConfig) {
        state.sheetsConfig?.let { config ->
            enabled = config.enabled
            spreadsheetId = config.spreadsheet_id
            sheetName = config.sheet_name
            sheetPerTeacher = config.sheet_per_teacher
            initialized = true
        } ?: run {
            enabled = false
            spreadsheetId = ""
            sheetName = "Vertinimai"
            sheetPerTeacher = false
            initialized = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Google Sheets eksportas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Įjungus, po įvertinimo duomenys siunčiami į pasirinktą Google skaičiuoklės lapą: klasė, balas, komentaras, mokytojo vardas, data ir pamoka.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Įjungti eksportą", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = spreadsheetId,
                    onValueChange = { spreadsheetId = it.trim() },
                    label = { Text("Google Sheets dokumento ID") },
                    supportingText = { Text("ID yra URL dalis tarp /d/ ir /edit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = sheetName,
                    onValueChange = { sheetName = it },
                    label = { Text("Lapo pavadinimas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Atskiras lapas kiekvienam mokytojui", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Switch(checked = sheetPerTeacher, onCheckedChange = { sheetPerTeacher = it })
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onSave(enabled, spreadsheetId, sheetName, sheetPerTeacher) },
                    enabled = !state.busy && initialized && (!enabled || spreadsheetId.isNotBlank()),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Išsaugoti") }
            }
        }
        Text(
            "Svarbu: pasidalykite dokumentu su Google service account el. paštu kaip redaktoriumi. Jo raktas laikomas tik Supabase serveryje, ne APK.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
