package lt.penki.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lt.penki.BuildConfig
import lt.penki.data.Lessons
import lt.penki.notify.Notifications

@OptIn(ExperimentalLayoutApi::class)
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
    val scheme = MaterialTheme.colorScheme

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotifications(granted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SoftIconButton(
                icon = Icons.Default.ArrowBack,
                contentDescription = "Atgal",
                onClick = onBack
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Nustatymai",
                    style = MaterialTheme.typography.titleLarge,
                    color = scheme.onBackground
                )
                Text(
                    text = "Profilis, priminimai ir paskyra",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            state.notice?.let { notice ->
                NoticeBanner(notice, state.noticeIsError, onDismissNotice)
            }

            SectionTitle("Profilis")
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ForestGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials(state.profile?.full_name.orEmpty()),
                            color = PenkiPalette.Cream,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.profile?.full_name?.takeIf { it.isNotBlank() } ?: "Mokytojas",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = state.profile?.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                PenkiTextField(name, { name = it }, "Vardas ir pavardė")
                Spacer(modifier = Modifier.height(12.dp))
                ForestButton(
                    text = "Išsaugoti vardą",
                    onClick = { onUpdateName(name) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionTitle("Priminimai")
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Priminimai po pamokos",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pranešimas ateina iškart pasibaigus pamokai, veikia ir be interneto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
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

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PAMOKŲ PABAIGA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Lessons.ends.forEachIndexed { index, time ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(scheme.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${index + 1}. ${time.format(Lessons.timeFormatter)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Laikus nustato administratorius (Admin → Laikai). Priminimai siunčiami tik darbo dienomis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }

            if (!state.isAdmin) {
                SectionTitle("Atsiliepimai")
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Text(
                        text = "Pasidalink pastebėjimais",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Siųskite pastabas, pasiūlymus ar pastebėtas klaidas – jos pasieks administratorių.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    GhostButton(
                        text = "Rašyti atsiliepimą",
                        onClick = {
                            feedbackText = ""
                            feedbackOpen = true
                        },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SectionTitle("Paskyra")
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.isAdmin) "Administratorius" else "Mokytojas",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "penki · versija ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                GhostButton(
                    text = "Atsijungti",
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "penki · klasės elgesio vertinimas",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        text = "Parašykite savo atsiliepimą ar pasiūlymą.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    PenkiTextField(
                        value = feedbackText,
                        onValueChange = { if (it.length <= 1000) feedbackText = it },
                        label = "Žinutė",
                        singleLine = false,
                        minLines = 4
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${feedbackText.length}/1000",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
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

/** Vardo inicialai avatarui (iki dviejų raidžių). */
private fun initials(name: String): String =
    name.trim()
        .split(' ', '\t')
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }
