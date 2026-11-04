package lt.penki.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

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

    PenkiBackground {
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
                diagnostics = remember(
                    state.profile?.id,
                    state.profile?.role,
                    state.teachers.size,
                    state.classes.size,
                    state.invites.size,
                    state.allRatings.size,
                    state.lessonTimes,
                    state.update,
                    state.notice
                ) { viewModel.diagnostics() },
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
                onRefetchTimetable = viewModel::refetchTimetable,
                onAssignTimetableAccount = viewModel::assignTimetableAccount,
                onMatchTimetableAccounts = viewModel::matchTimetableAccounts,
                onApplyTimetableClasses = viewModel::applyTimetableClasses,
                onLoadTeacherLessonTimes = viewModel::loadTeacherLessonTimes,
                onDebugCreateAccount = viewModel::debugCreateAccount,
                onDebugSeedRatings = viewModel::debugSeedRatings,
                onDebugDeleteAllRatings = viewModel::debugDeleteAllRatings,
                onDebugDeleteTeacherRatings = viewModel::debugDeleteTeacherRatings,
                onDebugDeleteInvites = viewModel::debugDeleteInvites,
                onDebugTestNotification = viewModel::debugTestNotification,
                onDebugRescheduleNotifications = viewModel::debugRescheduleNotifications,
                onDebugCancelNotifications = viewModel::debugCancelNotifications,
                onDebugCheckUpdate = viewModel::checkUpdateNow,
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
                    .background(Color.Black.copy(alpha = 0.16f))
                    .clickable(enabled = true) { },
                contentAlignment = Alignment.Center
            ) {
                GlassPanel(
                    shape = RoundedCornerShape(26.dp),
                    contentPadding = PaddingValues(26.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
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
            initialClass = state.myTimetable
                .firstOrNull {
                    it.weekday == java.time.LocalDate.now().dayOfWeek.value && it.lesson_no == lesson
                }
                ?.class_name,
            plannedClasses = state.myTimetable
                .filter { it.weekday == java.time.LocalDate.now().dayOfWeek.value }
                .groupBy { it.lesson_no }
                .mapValues { (_, lessons) -> lessons.first().class_name },
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
 * Neintrusyvus naujinio pranešimas viršuje: stiklo kortelė su gradientiniu
 * ženkliuku. Nieko neužblokuoja ir savaime išnyksta po 10 sekundžių.
 */
@Composable
private fun UpdateBanner(
    info: UpdateInfo,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val idle = !info.downloading && info.file == null
    LaunchedEffect(info.version, idle) {
        if (idle) {
            delay(10_000)
            onDismiss()
        }
    }

    GlassPanel(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ForestGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "↑",
                    color = PenkiPalette.Cream,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (info.file != null) "Naujinys paruoštas" else "Nauja versija ${info.version}",
                    style = MaterialTheme.typography.titleSmall
                )
                if (info.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = info.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            SoftIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Slėpti naujinio pranešimą",
                onClick = onDismiss
            )
        }

        if (info.downloading) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { info.progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Atsisiunčiama… ${info.progress}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        ForestButton(
            text = when {
                info.downloading -> "Atsisiunčiama…"
                info.file != null -> "Įdiegti atnaujinimą"
                else -> "Atnaujinti dabar"
            },
            onClick = onAction,
            enabled = !info.downloading,
            modifier = Modifier.fillMaxWidth()
        )
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

    AuthShell(
        subtitle = "Pirmas paleidimas",
        notice = notice,
        noticeIsError = noticeIsError,
        onDismissNotice = onDismissNotice
    ) {
        Text("Serverio nustatymas", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Įveskite Supabase projekto duomenis. Juos rasite Supabase → Settings → API.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        PenkiTextField(url, { url = it }, "Projekto URL (https://…supabase.co)")
        Spacer(modifier = Modifier.height(10.dp))
        PenkiTextField(key, { key = it }, "anon / public raktas")
        Spacer(modifier = Modifier.height(18.dp))
        ForestButton(
            text = "Išsaugoti ir tęsti",
            onClick = { onSave(url, key) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
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

    AuthShell(
        subtitle = "Klasės elgesio vertinimas po pamokos",
        notice = notice,
        noticeIsError = noticeIsError,
        onDismissNotice = onDismissNotice
    ) {
        Text("Sveiki atgal", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Prisijunkite su mokyklos suteikta paskyra.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        PenkiTextField(email, { email = it }, "El. paštas", KeyboardType.Email)
        Spacer(modifier = Modifier.height(10.dp))
        PenkiTextField(
            password,
            { password = it },
            "Slaptažodis",
            KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(18.dp))
        ForestButton(
            text = "Prisijungti",
            onClick = { onSignIn(email, password) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        GhostButton(
            text = "Registruotis su kvietimo kodu",
            onClick = onRegister,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
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

    AuthShell(
        subtitle = "Nauja mokytojo paskyra",
        notice = notice,
        noticeIsError = noticeIsError,
        onDismissNotice = onDismissNotice
    ) {
        Text("Registracija", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Mokytoją gali pakviesti tik administratorius. Įveskite savo duomenis ir gautą kvietimo kodą.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        PenkiTextField(name, { name = it }, "Vardas ir pavardė")
        Spacer(modifier = Modifier.height(10.dp))
        PenkiTextField(email, { email = it }, "El. paštas", KeyboardType.Email)
        Spacer(modifier = Modifier.height(10.dp))
        PenkiTextField(
            password,
            { password = it },
            "Slaptažodis (bent 6 simboliai)",
            KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(10.dp))
        PenkiTextField(code, { code = it.uppercase() }, "Kvietimo kodas")
        Spacer(modifier = Modifier.height(18.dp))
        ForestButton(
            text = "Sukurti paskyrą",
            onClick = { onRegister(name, email, password, code) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        GhostButton(
            text = "Atgal į prisijungimą",
            onClick = onBack,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
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

    AuthShell(
        subtitle = "Paskyros aktyvavimas",
        notice = notice,
        noticeIsError = noticeIsError,
        onDismissNotice = onDismissNotice
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark(size = 60.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Paskyra dar neaktyvi",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (name.isBlank()) email else "$name ($email)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Įveskite kvietimo kodą, kurį išdavė administratorius.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        PenkiTextField(code, { code = it.uppercase() }, "Kvietimo kodas")
        Spacer(modifier = Modifier.height(18.dp))
        ForestButton(
            text = "Aktyvuoti paskyrą",
            onClick = { onRedeem(code) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        GhostButton(
            text = "Atsijungti",
            onClick = onLogout,
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ------------------------------------------------------------------ BENDRi

/** Bendra autentifikacijos ekranų struktūra: prekės ženklas ir stiklo kortelė. */
@Composable
private fun AuthShell(
    subtitle: String,
    notice: String?,
    noticeIsError: Boolean,
    onDismissNotice: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(44.dp))
        BrandHeader(subtitle)
        Spacer(modifier = Modifier.height(22.dp))
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            contentPadding = PaddingValues(20.dp)
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(8.dp))
        Notice(notice, noticeIsError, onDismissNotice)
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/** Logotipo ženklas: žalias kvadratas su serifiniu „5“. */
@Composable
fun BrandMark(size: Dp = 54.dp) {
    val corner = RoundedCornerShape(size * 0.32f)
    Box(
        modifier = Modifier
            .size(size)
            .shadowSoft(corner)
            .clip(corner)
            .background(ForestGradient)
            .border(1.dp, Color.White.copy(alpha = 0.35f), corner),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "5",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            color = PenkiPalette.Cream,
            fontSize = (size.value * 0.50f).sp
        )
    }
}

@Composable
fun BrandHeader(subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "penki",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun Notice(text: String?, isError: Boolean, onDismiss: () -> Unit) {
    if (text != null) {
        NoticeBanner(text = text, isError = isError, onDismiss = onDismiss)
    }
}

/** Stiklinis teksto laukas su švelniu rėmeliu ir permatomu fonu. */
@Composable
fun PenkiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminanceIsDark()
    val container = if (dark) scheme.surface.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.58f)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = scheme.primary,
            unfocusedBorderColor = scheme.outline.copy(alpha = 0.45f),
            focusedContainerColor = container,
            unfocusedContainerColor = container.copy(alpha = container.alpha * 0.75f),
            disabledContainerColor = container,
            cursorColor = scheme.primary,
            focusedLabelColor = scheme.primary,
            unfocusedLabelColor = scheme.onSurfaceVariant,
            focusedTextColor = scheme.onSurface,
            unfocusedTextColor = scheme.onSurface
        ),
        modifier = modifier.fillMaxWidth()
    )
}
