package lt.penki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import lt.penki.data.Lessons

@Composable
fun PenkiApp(viewModel: AppViewModel, openLesson: Int?, onLessonOpened: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var ratingLesson by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(openLesson, state.screen) {
        if (openLesson != null && state.screen == Screen.Home) {
            ratingLesson = openLesson
            onLessonOpened()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (state.screen) {
            Screen.Setup -> SetupScreen(
                busy = state.busy,
                notice = state.notice,
                noticeIsError = state.noticeIsError,
                onDismissNotice = viewModel::dismissNotice,
                onSave = viewModel::saveServer
            )

            Screen.Login -> LoginScreen(
                busy = state.busy,
                notice = state.notice,
                noticeIsError = state.noticeIsError,
                onDismissNotice = viewModel::dismissNotice,
                onSignIn = viewModel::signIn,
                onRegister = { viewModel.dismissNotice(); viewModel.showRegister() }
            )

            Screen.Register -> RegisterScreen(
                busy = state.busy,
                notice = state.notice,
                noticeIsError = state.noticeIsError,
                onDismissNotice = viewModel::dismissNotice,
                onRegister = viewModel::register,
                onBack = { viewModel.dismissNotice(); viewModel.showLogin() }
            )

            Screen.Activation -> ActivationScreen(
                email = state.profile?.email ?: "",
                name = state.profile?.full_name ?: "",
                busy = state.busy,
                notice = state.notice,
                noticeIsError = state.noticeIsError,
                onDismissNotice = viewModel::dismissNotice,
                onRedeem = viewModel::redeem,
                onLogout = viewModel::logout
            )

            Screen.Home -> TeacherScreen(
                state = state,
                onRate = { ratingLesson = it },
                onAdmin = viewModel::openAdmin,
                onSettings = viewModel::openSettings,
                onRefresh = viewModel::refresh,
                onDismissNotice = viewModel::dismissNotice
            )

            Screen.Admin -> AdminScreen(
                state = state,
                diagnostics = viewModel.diagnostics(),
                onBack = viewModel::goHome,
                onRefresh = viewModel::refreshAdmin,
                onDismissNotice = viewModel::dismissNotice,
                onSetActive = viewModel::setTeacherActive,
                onSetAdmin = viewModel::setAdminRole,
                onCreateInvite = viewModel::createInvite,
                onDeleteInvite = viewModel::deleteInvite,
                onApprove = viewModel::approveTeacher,
                onAddClass = viewModel::addClass,
                onDeleteClass = viewModel::deleteClass,
                onToggleTeacherClass = viewModel::toggleTeacherClass,
                onSaveLessonTime = viewModel::saveLessonTime,
                onAddLesson = viewModel::addLesson,
                onRemoveLesson = viewModel::removeLesson,
                onResetLessonTimes = viewModel::resetLessonTimes,
                onSaveSheetsConfig = viewModel::saveSheetsConfig,
                onLoadTeacherLessonTimes = viewModel::loadTeacherLessonTimes,
                onDebugCreateAccount = viewModel::debugCreateAccount,
                onDebugSeedRatings = viewModel::debugSeedRatings,
                onDebugDeleteAllRatings = viewModel::debugDeleteAllRatings,
                onDebugDeleteTeacherRatings = viewModel::debugDeleteTeacherRatings,
                onDebugDeleteInvites = viewModel::debugDeleteInvites,
                onDebugTestNotification = viewModel::debugTestNotification,
                onDebugRescheduleNotifications = viewModel::debugRescheduleNotifications,
                onDebugCancelNotifications = viewModel::debugCancelNotifications,
                onMarkFeedbackRead = viewModel::markFeedbackRead,
                onDeleteFeedback = viewModel::deleteFeedback
            )

            Screen.Settings -> SettingsScreen(
                state = state,
                notificationsEnabled = viewModel.notificationsEnabled(),
                onBack = viewModel::goHome,
                onUpdateName = viewModel::updateName,
                onNotifications = viewModel::setNotifications,
                onLogout = viewModel::logout,
                onDismissNotice = viewModel::dismissNotice,
                onSendFeedback = viewModel::sendFeedback
            )
        }

        if (state.loading || state.busy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000))
                    .clickable(enabled = true) { },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        state.update?.let { info ->
            UpdateBanner(
                info = info,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                onAction = viewModel::downloadUpdate,
                onDismiss = viewModel::dismissUpdate
            )
        }
    }

    val lesson = ratingLesson
    if (lesson != null) {
        RatingDialog(
            initialLesson = lesson,
            classes = state.classes,
            busy = state.busy,
            onDismiss = { ratingLesson = null },
            onSave = { classId, lessonNo, score, comment ->
                viewModel.saveRating(classId, lessonNo, score, comment) { ratingLesson = null }
            }
        )
    }
}

// ------------------------------------------------------------ NAUJINIMAS

/**
 * Neintrusyvus naujinio pranešimas viršuje. Nieko neužblokuoja ir
 * savaime išnyksta po 10 sekundžių, jei vartotojas nieko nepasirenka.
 */
@Composable
private fun UpdateBanner(
    info: UpdateInfo,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tik tuščią (nieko nevykstančią) būseną automatiškai uždarome.
    // Kol atsisiunčiama arba failas paruoštas – pranešimas lieka.
    val idle = !info.downloading && info.file == null
    LaunchedEffect(info.version, idle) {
        if (idle) {
            delay(10_000)
            onDismiss()
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (info.file != null) "Naujinys paruoštas" else "Yra nauja versija ${info.version}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (info.notes.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = info.notes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (info.downloading) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { info.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Atsisiunčiama… ${info.progress}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onAction, enabled = !info.downloading) {
                Text(if (info.file != null) "Įdiegti" else "Atnaujinti")
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Slėpti naujinio pranešimą"
                )
            }
        }
    }
}

// ------------------------------------------------------------------ SETUP

@Composable
private fun SetupScreen(
    busy: Boolean,
    notice: String?,
    noticeIsError: Boolean,
    onDismissNotice: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp)
        ) {
            BrandHeader(subtitle = "Pirmas paleidimas")
            Spacer(Modifier.height(24.dp))
            Text(
                "Įveskite savo Supabase projekto duomenis. Juos rasite Supabase → Settings → API.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            PenkiTextField(url, { url = it }, "Projekto URL (https://...supabase.co)")
            Spacer(Modifier.height(12.dp))
            PenkiTextField(key, { key = it }, "anon / public raktas")
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onSave(url, key) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Išsaugoti") }
            Spacer(Modifier.height(16.dp))
            Notice(notice, noticeIsError, onDismissNotice)
        }
    }
}

// ------------------------------------------------------------------ LOGIN

@Composable
private fun LoginScreen(
    busy: Boolean,
    notice: String?,
    noticeIsError: Boolean,
    onDismissNotice: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(40.dp))
            BrandHeader(subtitle = "Klasės elgesio vertinimas po pamokos")
            Spacer(Modifier.height(32.dp))
            PenkiTextField(email, { email = it }, "El. paštas", KeyboardType.Email)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Slaptažodis") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onSignIn(email, password) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Prisijungti") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRegister, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Neturite paskyros? Registruotis su kvietimo kodu")
            }
            Spacer(Modifier.height(12.dp))
            Notice(notice, noticeIsError, onDismissNotice)
        }
    }
}

// ------------------------------------------------------------------ REGISTRACIJA

@Composable
private fun RegisterScreen(
    busy: Boolean,
    notice: String?,
    noticeIsError: Boolean,
    onDismissNotice: () -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Text("Registracija", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Mokytoją gali pakviesti tik administratorius. Įveskite savo duomenis ir gautą kvietimo kodą.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(20.dp))
            PenkiTextField(name, { name = it }, "Vardas ir pavardė")
            Spacer(Modifier.height(12.dp))
            PenkiTextField(email, { email = it }, "El. paštas", KeyboardType.Email)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Slaptažodis (bent 6 simboliai)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            PenkiTextField(code, { code = it.uppercase() }, "Kvietimo kodas")
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onRegister(name, email, password, code) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sukurti paskyrą") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBack, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Atgal į prisijungimą")
            }
            Spacer(Modifier.height(12.dp))
            Notice(notice, noticeIsError, onDismissNotice)
        }
    }
}

// ------------------------------------------------------------------ AKTYVAVIMAS

@Composable
private fun ActivationScreen(
    email: String,
    name: String,
    busy: Boolean,
    notice: String?,
    noticeIsError: Boolean,
    onDismissNotice: () -> Unit,
    onRedeem: (String) -> Unit,
    onLogout: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Text("penki", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(
                "Paskyra dar neaktyvi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (name.isBlank()) email else "$name ($email)",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Įveskite kvietimo kodą, kurį išdavė administratorius.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            PenkiTextField(code, { code = it.uppercase() }, "Kvietimo kodas")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onRedeem(code) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Aktyvuoti paskyrą") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onLogout, enabled = !busy) { Text("Atsijungti") }
            Spacer(Modifier.height(12.dp))
            Notice(notice, noticeIsError, onDismissNotice)
        }
    }
}

// ------------------------------------------------------------------ BENDRi

@Composable
fun BrandHeader(subtitle: String) {
    Column {
        Text(
            "penki",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Notice(text: String?, isError: Boolean, onDismiss: () -> Unit) {
    if (text != null) {
        NoticeBanner(text = text, isError = isError, onDismiss = onDismiss)
    }
}

@Composable
fun PenkiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth()
    )
}
