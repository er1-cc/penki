package lt.penki.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.penki.BuildConfig
import lt.penki.data.Lessons
import lt.penki.notify.Notifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    notificationsEnabled: Boolean,
    onBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onDismissNotice: () -> Unit,
    onSendFeedback: (String) -> Unit = {}
) {
    var name by remember(state.profile?.full_name) {
        mutableStateOf(state.profile?.full_name.orEmpty())
    }
    var feedbackOpen by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotifications(granted)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nustatymai", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atgal")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            state.notice?.let { notice ->
                NoticeBanner(notice, state.noticeIsError, onDismissNotice)
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Profilis", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.profile?.email.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    PenkiTextField(name, { name = it }, "Vardas ir pavardė")
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onUpdateName(name) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Išsaugoti vardą") }
                }
            }

            Spacer(Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Priminimai po pamokos", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Pranešimas ateina iškart pasibaigus pamokai.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !Notifications.canPost(context)
                                ) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onNotifications(enabled)
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Pamokų pabaigos laikai", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Laikus nustato administratorius (Admin → Laikai).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Lessons.ends.forEachIndexed { index, time ->
                        Text(
                            "${index + 1}-a pamoka – ${time.format(Lessons.timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Priminimai siunčiami tik darbo dienomis.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!state.isAdmin) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Atsiliepimai", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Siųskite pastabas, pasiūlymus ar pastebėtas klaidas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                feedbackText = ""
                                feedbackOpen = true
                            },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Rašyti atsiliepimą") }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Paskyra", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (state.isAdmin) "Administratorius" else "Mokytojas",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Atsijungti") }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "penki · ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (feedbackOpen && !state.isAdmin) {
        AlertDialog(
            onDismissRequest = { if (!state.busy) feedbackOpen = false },
            title = { Text("Atsiliepimas") },
            text = {
                Column {
                    Text(
                        "Parašykite savo atsiliepimą ar pasiūlymą.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    PenkiTextField(
                        value = feedbackText,
                        onValueChange = { if (it.length <= 1000) feedbackText = it },
                        label = "Žinutė",
                        singleLine = false,
                        minLines = 4
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${feedbackText.length}/1000",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.busy && feedbackText.isNotBlank(),
                    onClick = {
                        onSendFeedback(feedbackText)
                        feedbackOpen = false
                        feedbackText = ""
                    }
                ) { Text("Siųsti") }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.busy,
                    onClick = { feedbackOpen = false }
                ) { Text("Atšaukti") }
            }
        )
    }
}
