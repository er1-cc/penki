package lt.penki.ui

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lt.penki.data.ApiException
import lt.penki.data.Invite
import lt.penki.data.Klass
import lt.penki.data.Prefs
import lt.penki.data.Profile
import lt.penki.data.Rating
import lt.penki.data.Supabase
import lt.penki.notify.Notifications
import java.time.LocalDate

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
    val allRatings: List<Rating> = emptyList(),
    val notice: String? = null,
    val noticeIsError: Boolean = true
) {
    val isAdmin: Boolean get() = profile?.isAdmin == true
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val api = Supabase(prefs)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
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
                _state.update {
                    it.copy(
                        loading = false,
                        profile = profile,
                        classes = classes,
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
                _state.update { it.copy(busy = false, classes = classes, myRatings = mine) }
                if (_state.value.isAdmin) loadAdminData()
            } catch (e: Exception) {
                handle(e)
            }
        }
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
        _state.update {
            it.copy(teachers = teachers, invites = invites, allRatings = ratings)
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
