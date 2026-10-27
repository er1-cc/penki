package lt.penki.ui

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lt.penki.BuildConfig
import lt.penki.data.ApiException
import lt.penki.data.Invite
import lt.penki.data.Klass
import lt.penki.data.LessonTime
import lt.penki.data.Lessons
import lt.penki.data.Prefs
import lt.penki.data.Profile
import lt.penki.data.Rating
import lt.penki.data.Supabase
import lt.penki.data.TeacherClass
import lt.penki.notify.Notifications
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class Screen { Setup, Login, Register, Activation, Home, Admin, Settings }

data class UiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val screen: Screen = Screen.Login,
    val profile: Profile? = null,
    val classes: List<Klass> = emptyList(),
    val myRatings: List<Rating> = emptyList(),
    val teachers: List<Profile> = emptyList(),
    val invites: List<Invite> = emptyList(),
    val assignments: List<TeacherClass> = emptyList(),
    val lessonTimes: List<LessonTime> = emptyList(),
    val allRatings: List<Rating> = emptyList(),
    val debugLog: List<String> = emptyList(),
    val notice: String? = null,
    val noticeIsError: Boolean = true
) {
    val isAdmin: Boolean get() = profile?.isAdmin == true

    /** Klasių ID, priskirtų konkrečiam mokytojui. */
    fun classIdsOf(teacherId: String): Set<String> =
        assignments.filter { it.teacher_id == teacherId }.map { it.class_id }.toSet()
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val api = Supabase(prefs)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        Lessons.applySaved(prefs)
        bootstrap()
    }

    // ------------------------------------------------------------ startas

    private fun bootstrap() {
        when {
            !api.configured -> _state.update { it.copy(loading = false, screen = Screen.Setup) }
            !prefs.loggedIn -> _state.update { it.copy(loading = false, screen = Screen.Login) }
            else -> loadProfile()
        }
    }

    fun saveServer(url: String, key: String) {
        if (url.isBlank() || key.isBlank()) {
            notice("Įrašykite projekto URL ir anon raktą.", true)
            return
        }
        if (!url.startsWith("http")) {
            notice("URL turi prasidėti https://", true)
            return
        }
        api.configure(url, key)
        _state.update { it.copy(screen = Screen.Login) }
    }

    // ------------------------------------------------------------ auth

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            notice("Įrašykite el. paštą ir slaptažodį.", true)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                api.signIn(email, password)
                loadProfile()
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun register(name: String, email: String, password: String, code: String) {
        when {
            name.isBlank() -> return notice("Įrašykite vardą ir pavardę.", true)
            !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                return notice("Įrašykite tinkamą el. paštą.", true)
            password.length < 6 -> return notice("Slaptažodis turi būti bent 6 simbolių.", true)
            code.isBlank() -> return notice("Įrašykite kvietimo kodą, kurį gavote iš admino.", true)
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                val loggedIn = api.signUp(email, password, name)
                if (!loggedIn) {
                    _state.update {
                        it.copy(
                            busy = false,
                            screen = Screen.Login,
                            notice = "Paskyra sukurta. Patvirtinkite el. paštą, tada prisijunkite.",
                            noticeIsError = false
                        )
                    }
                    return@launch
                }
                val result = api.redeemInvite(code)
                if (!result.ok) {
                    _state.update {
                        it.copy(
                            busy = false,
                            screen = Screen.Activation,
                            notice = result.error ?: "Kvietimo kodas netinkamas.",
                            noticeIsError = true
                        )
                    }
                    return@launch
                }
                loadProfile()
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun redeem(code: String) {
        if (code.isBlank()) return notice("Įrašykite kvietimo kodą.", true)
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                val result = api.redeemInvite(code)
                if (!result.ok) {
                    _state.update {
                        it.copy(busy = false, notice = result.error ?: "Kvietimas netinkamas.", noticeIsError = true)
                    }
                } else {
                    loadProfile()
                }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            Notifications.cancelAll(getApplication())
            try {
                api.signOut()
            } catch (_: Exception) {
                // nepavyko serveryje – vietinę sesiją vis tiek išvalome
            }
            _state.value = UiState(loading = false, screen = Screen.Login)
        }
    }

    // ------------------------------------------------------------ duomenys

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, notice = null) }
            try {
                val profile = api.profile()
                if (!profile.isTeacher) {
                    _state.update {
                        it.copy(loading = false, profile = profile, screen = Screen.Activation)
                    }
                    return@launch
                }
                val classes = api.classes()
                val mine = api.myRatings()
                val visible = if (profile.isAdmin) classes else {
                    val assigned = runCatching { api.myTeacherClasses() }.getOrDefault(emptyList())
                    if (assigned.isEmpty()) classes else classes.filter { klass -> assigned.any { it.class_id == klass.id } }
                }
                loadLessonTimes()
                _state.update {
                    it.copy(
                        loading = false,
                        profile = profile,
                        classes = visible,
                        myRatings = mine,
                        screen = Screen.Home
                    )
                }
                if (profile.isAdmin) loadAdminData()
                Notifications.scheduleUpcoming(getApplication())
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                val classes = api.classes()
                val mine = api.myRatings()
                val profile = _state.value.profile
                val visible = if (profile == null || profile.isAdmin) classes else {
                    val assigned = runCatching { api.myTeacherClasses() }.getOrDefault(emptyList())
                    if (assigned.isEmpty()) classes else classes.filter { klass -> assigned.any { it.class_id == klass.id } }
                }
                loadLessonTimes()
                _state.update { it.copy(busy = false, classes = visible, myRatings = mine) }
                if (_state.value.isAdmin) loadAdminData()
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    /** Nuskaito pamokų laikus iš serverio ir išsaugo telefone priminimams. */
    private suspend fun loadLessonTimes() {
        val times = runCatching { api.lessonTimes() }.getOrNull()
        if (times.isNullOrEmpty()) {
            Lessons.applySaved(prefs)
            return
        }
        Lessons.applyServer(prefs, times.map { it.ends_at })
        _state.update { it.copy(lessonTimes = times.sortedBy { time -> time.lesson_no }) }
    }

    fun saveRating(
        classId: String,
        lessonNo: Int,
        score: Int,
        comment: String,
        onSaved: () -> Unit
    ) {
        if (classId.isBlank()) return notice("Pasirinkite klasę.", true)
        if (score !in 1..5) return notice("Pasirinkite balą nuo 1 iki 5.", true)

        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                api.saveRating(classId, lessonNo, score, comment, LocalDate.now().toString())
                val mine = api.myRatings()
                _state.update {
                    it.copy(
                        busy = false,
                        myRatings = mine,
                        notice = "$lessonNo-a pamoka įvertinta: $score/5.",
                        noticeIsError = false
                    )
                }
                onSaved()
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    // ------------------------------------------------------------ adminas

    fun openAdmin() {
        _state.update { it.copy(screen = Screen.Admin, notice = null) }
        refreshAdmin()
    }

    fun openSettings() {
        _state.update { it.copy(screen = Screen.Settings, notice = null) }
    }

    fun showRegister() {
        _state.update { it.copy(screen = Screen.Register, notice = null) }
    }

    fun showLogin() {
        _state.update { it.copy(screen = Screen.Login, notice = null) }
    }

    fun goHome() {
        _state.update { it.copy(screen = Screen.Home, notice = null) }
    }

    fun refreshAdmin() {
        if (!_state.value.isAdmin) return
        viewModelScope.launch {
            try {
                loadAdminData()
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    private suspend fun loadAdminData() {
        val teachers = api.profiles()
        val invites = api.invites()
        val ratings = api.allRatings()
        val assignments = api.allTeacherClasses()
        _state.update {
            it.copy(
                teachers = teachers,
                invites = invites,
                allRatings = ratings,
                assignments = assignments
            )
        }
    }

    fun setTeacherActive(id: String, active: Boolean) {
        viewModelScope.launch {
            try {
                api.setActive(id, active)
                loadAdminData()
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    /** Nepatvirtintą vartotoją paverčia aktyviu mokytoju. */
    fun approveTeacher(id: String) {
        viewModelScope.launch {
            try {
                api.setRole(id, "teacher")
                api.setActive(id, true)
                loadAdminData()
                _state.update { it.copy(notice = "Mokytojas patvirtintas.", noticeIsError = false) }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun setAdminRole(id: String, admin: Boolean) {
        viewModelScope.launch {
            try {
                api.setRole(id, if (admin) "admin" else "teacher")
                loadAdminData()
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun createInvite(email: String, onCreated: (String) -> Unit) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            return notice("Įrašykite tinkamą mokytojo el. paštą.", true)
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                val code = newCode()
                api.createInvite(email.trim().lowercase(), code)
                loadAdminData()
                _state.update {
                    it.copy(
                        busy = false,
                        notice = "Kvietimas sukurtas. Nusiųskite kodą mokytojui.",
                        noticeIsError = false
                    )
                }
                onCreated(code)
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun deleteInvite(id: String) {
        viewModelScope.launch {
            try {
                api.deleteInvite(id)
                loadAdminData()
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun addClass(name: String) {
        if (name.isBlank()) return notice("Įrašykite klasės pavadinimą.", true)
        viewModelScope.launch {
            try {
                api.addClass(name)
                val classes = api.classes()
                _state.update { it.copy(classes = classes, notice = "Klasė pridėta.", noticeIsError = false) }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun deleteClass(id: String) {
        viewModelScope.launch {
            try {
                api.deleteClass(id)
                val classes = api.classes()
                _state.update { it.copy(classes = classes) }
                if (_state.value.isAdmin) loadAdminData()
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    // ------------------------------------------------------------ mokytojo klasės

    fun toggleTeacherClass(teacherId: String, classId: String, assigned: Boolean) {
        viewModelScope.launch {
            try {
                if (assigned) api.assignClass(teacherId, classId) else api.unassignClass(teacherId, classId)
                val assignments = api.allTeacherClasses()
                _state.update { it.copy(assignments = assignments) }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    // ------------------------------------------------------------ pamokų laikai

    fun saveLessonTime(lessonNo: Int, text: String) {
        val time = Lessons.parseOne(text)
            ?: return notice("Laikas turi būti HH:mm formatu (pvz. 09:40).", true)
        val formatted = time.format(Lessons.timeFormatter)
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                api.saveLessonTime(lessonNo, formatted)
                loadLessonTimes()
                Notifications.scheduleUpcoming(getApplication())
                logDebug("$lessonNo-a pamoka baigiasi $formatted")
                _state.update {
                    it.copy(
                        busy = false,
                        notice = "$lessonNo-a pamoka dabar baigiasi $formatted. Priminimai perplanuoti.",
                        noticeIsError = false
                    )
                }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun addLesson() {
        if (Lessons.count >= Lessons.MAX_LESSONS) {
            return notice("Daugiausia ${Lessons.MAX_LESSONS} pamokų.", true)
        }
        val last = Lessons.ends.lastOrNull() ?: LocalTime.of(8, 45)
        saveLessonTime(Lessons.count + 1, last.plusMinutes(55).format(Lessons.timeFormatter))
    }

    fun removeLesson(lessonNo: Int) {
        if (Lessons.count <= 1) return notice("Turi likti bent viena pamoka.", true)
        if (lessonNo != Lessons.count) return notice("Šalinti galima tik paskutinę pamoką.", true)
        viewModelScope.launch {
            try {
                api.deleteLessonTime(lessonNo)
                loadLessonTimes()
                Notifications.scheduleUpcoming(getApplication())
                logDebug("$lessonNo-a pamoka pašalinta")
                _state.update { it.copy(notice = "$lessonNo-a pamoka pašalinta.", noticeIsError = false) }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun resetLessonTimes() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                _state.value.lessonTimes
                    .filter { it.lesson_no > Lessons.defaults.size }
                    .forEach { api.deleteLessonTime(it.lesson_no) }
                Lessons.defaults.forEachIndexed { index, time ->
                    api.saveLessonTime(index + 1, time.format(Lessons.timeFormatter))
                }
                loadLessonTimes()
                Notifications.scheduleUpcoming(getApplication())
                logDebug("Pamokų laikai atstatyti į numatytuosius")
                _state.update {
                    it.copy(
                        busy = false,
                        notice = "Pamokų laikai atstatyti į numatytuosius.",
                        noticeIsError = false
                    )
                }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    // ------------------------------------------------------------ debug (adminui)

    fun debugCreateAccount(
        name: String,
        email: String,
        password: String,
        asAdmin: Boolean,
        ratingCount: Int
    ) {
        if (!_state.value.isAdmin) return notice("Debug meniu skirtas tik administratoriui.", true)
        if (password.length < 6) return notice("Slaptažodis turi būti bent 6 simbolių.", true)
        val cleanEmail = email.trim().ifBlank { "test${System.currentTimeMillis() % 100000}@example.com" }
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return notice("Įrašykite tinkamą el. paštą.", true)
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                val teacherId = api.createTestAccount(
                    name.ifBlank { "Testinis Mokytojas" },
                    cleanEmail,
                    password
                )
                api.setRole(teacherId, if (asAdmin) "admin" else "teacher")
                api.setActive(teacherId, true)
                val created = if (ratingCount > 0) seedRatings(teacherId, ratingCount) else 0
                logDebug("Sukurta paskyra $cleanEmail (${if (asAdmin) "admin" else "mokytojas"}), vertinimų: $created")
                loadAdminData()
                _state.update {
                    it.copy(
                        busy = false,
                        notice = "Sukurta: $cleanEmail / $password" +
                            if (created > 0) " (+$created vertinimų)" else "",
                        noticeIsError = false
                    )
                }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun debugSeedRatings(teacherId: String, count: Int) {
        if (!_state.value.isAdmin) return notice("Debug meniu skirtas tik administratoriui.", true)
        if (teacherId.isBlank()) return notice("Pasirinkite mokytoją.", true)
        if (count !in 1..200) return notice("Įrašykite skaičių nuo 1 iki 200.", true)
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                val created = seedRatings(teacherId, count)
                logDebug("Sugeneruota $created vertinimų")
                loadAdminData()
                _state.update {
                    it.copy(busy = false, notice = "Sugeneruota $created vertinimų.", noticeIsError = false)
                }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    private suspend fun seedRatings(teacherId: String, count: Int): Int {
        val classes = _state.value.classes
        if (classes.isEmpty()) throw ApiException("Nėra klasių – pirmiausia pridėkite klasių.")
        val lessonsCount = Lessons.count.coerceAtLeast(1)
        val comments = listOf(
            "Tyli klasė, geras darbas.",
            "Truputį triukšmo pamokos pabaigoje.",
            "Aktyviai dirbo, puiku.",
            "Vėlavo keli mokiniai.",
            ""
        )
        val today = LocalDate.now()
        var created = 0
        var attempts = 0
        while (created < count && attempts < count * 3) {
            attempts++
            val klass = classes.random()
            val date = today.minusDays((0..13).random().toLong())
            val lesson = (1..lessonsCount).random()
            val score = (1..5).random()
            runCatching {
                api.insertRating(
                    teacherId = teacherId,
                    classId = klass.id,
                    lessonNo = lesson,
                    score = score,
                    comment = comments.random(),
                    date = date.toString()
                )
            }.onSuccess { created++ }
        }
        return created
    }

    fun debugDeleteAllRatings() {
        if (!_state.value.isAdmin) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                api.deleteAllRatings()
                loadAdminData()
                logDebug("Ištrinti visi vertinimai")
                _state.update { it.copy(busy = false, notice = "Visi vertinimai ištrinti.", noticeIsError = false) }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun debugDeleteTeacherRatings(teacherId: String) {
        if (!_state.value.isAdmin) return
        if (teacherId.isBlank()) return notice("Pasirinkite mokytoją.", true)
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                api.deleteRatingsOf(teacherId)
                loadAdminData()
                logDebug("Ištrinti pasirinkto mokytojo vertinimai")
                _state.update {
                    it.copy(busy = false, notice = "Mokytojo vertinimai ištrinti.", noticeIsError = false)
                }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun debugDeleteInvites() {
        if (!_state.value.isAdmin) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                api.deleteAllInvites()
                loadAdminData()
                logDebug("Ištrinti visi kvietimai")
                _state.update { it.copy(busy = false, notice = "Visi kvietimai ištrinti.", noticeIsError = false) }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun debugTestNotification(seconds: Int) {
        val app = getApplication<Application>()
        if (!Notifications.canPost(app)) {
            return notice("Pranešimų leidimas nesuteiktas – įjunkite jį telefone.", true)
        }
        viewModelScope.launch {
            if (seconds > 0) {
                notice("Bandomasis pranešimas po $seconds s…", false)
                delay(seconds * 1000L)
            }
            Notifications.ensureChannel(app)
            Notifications.show(app, Lessons.count.coerceAtLeast(1))
            logDebug("Bandomasis pranešimas parodytas")
        }
    }

    fun debugRescheduleNotifications() {
        Notifications.scheduleUpcoming(getApplication())
        logDebug("Priminimai perplanuoti")
        notice("Priminimai perplanuoti.", false)
    }

    fun debugCancelNotifications() {
        Notifications.cancelAll(getApplication())
        logDebug("Visi priminimai atšaukti")
        notice("Visi priminimai atšaukti.", false)
    }

    /** Diagnostikos eilutės debug skirtukui (jokių slaptų raktų). */
    fun diagnostics(): List<Pair<String, String>> {
        val current = _state.value
        val minutesLeft = (prefs.expiresAt - System.currentTimeMillis()) / 60000L
        val ends = Lessons.ends.joinToString(", ") { it.format(Lessons.timeFormatter) }
        return listOf(
            "Serveris" to prefs.baseUrl,
            "Vartotojas" to (current.profile?.email ?: prefs.email),
            "Vaidmuo" to (current.profile?.role ?: "–"),
            "Vartotojo ID" to prefs.userId.ifBlank { "–" },
            "Sesija galioja" to if (prefs.expiresAt == 0L) "nežinoma" else "$minutesLeft min.",
            "Pamokų laikai" to ends.ifBlank { "–" },
            "Mokytojų" to current.teachers.size.toString(),
            "Klasių" to current.classes.size.toString(),
            "Kvietimų" to current.invites.size.toString(),
            "Vertinimų" to current.allRatings.size.toString(),
            "Pranešimų leidimas" to if (Notifications.canPost(getApplication())) "taip" else "ne",
            "Programos versija" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            "Debug kodas" to if (BuildConfig.DEBUG) "taip" else "ne"
        )
    }

    private fun logDebug(line: String) {
        val stamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        _state.update { it.copy(debugLog = (listOf("$stamp $line") + it.debugLog).take(30)) }
    }

    // ------------------------------------------------------------ nustatymai

    fun updateName(name: String) {
        if (name.isBlank()) return notice("Vardas negali būti tuščias.", true)
        viewModelScope.launch {
            _state.update { it.copy(busy = true, notice = null) }
            try {
                api.updateMyName(name)
                _state.update {
                    it.copy(
                        busy = false,
                        profile = it.profile?.copy(full_name = name.trim()),
                        notice = "Vardas atnaujintas.",
                        noticeIsError = false
                    )
                }
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun setNotifications(enabled: Boolean) {
        prefs.notificationsEnabled = enabled
        if (enabled) {
            Notifications.scheduleUpcoming(getApplication())
        } else {
            Notifications.cancelAll(getApplication())
        }
        _state.update {
            it.copy(
                notice = if (enabled) "Priminimai įjungti." else "Priminimai išjungti.",
                noticeIsError = false
            )
        }
    }

    fun notificationsEnabled(): Boolean = prefs.notificationsEnabled

    fun dismissNotice() {
        _state.update { it.copy(notice = null) }
    }

    // ------------------------------------------------------------ pagalbinės

    private fun notice(text: String, isError: Boolean) {
        _state.update { it.copy(notice = text, noticeIsError = isError, busy = false) }
    }

    private fun handle(e: Exception) {
        val apiError = e as? ApiException
        val expired = apiError?.status == 401
        if (expired) prefs.clearSession()
        _state.update {
            it.copy(
                loading = false,
                busy = false,
                notice = apiError?.message ?: "Įvyko klaida. Bandykite dar kartą.",
                noticeIsError = true,
                screen = if (expired) Screen.Login else it.screen
            )
        }
    }

    private fun newCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet[(0 until alphabet.length).random()] }.joinToString("")
    }
}
