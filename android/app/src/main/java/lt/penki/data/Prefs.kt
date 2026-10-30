package lt.penki.data

import android.content.Context
import android.content.SharedPreferences
import lt.penki.BuildConfig

/**
 * Saugo serverio adresą ir prisijungimo sesiją telefone.
 * Access/refresh tokenai laikomi programos vidiniame saugykloje.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("penki", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = sp.getString(KEY_URL, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.SUPABASE_URL.trim()
        set(value) = sp.edit().putString(KEY_URL, value.trim().trimEnd('/')).apply()

    var anonKey: String
        get() = sp.getString(KEY_KEY, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.SUPABASE_ANON_KEY.trim()
        set(value) = sp.edit().putString(KEY_KEY, value.trim()).apply()

    var accessToken: String
        get() = sp.getString(KEY_ACCESS, "") ?: ""
        set(value) = sp.edit().putString(KEY_ACCESS, value).apply()

    var refreshToken: String
        get() = sp.getString(KEY_REFRESH, "") ?: ""
        set(value) = sp.edit().putString(KEY_REFRESH, value).apply()

    var expiresAt: Long
        get() = sp.getLong(KEY_EXPIRES, 0L)
        set(value) = sp.edit().putLong(KEY_EXPIRES, value).apply()

    var userId: String
        get() = sp.getString(KEY_USER_ID, "") ?: ""
        set(value) = sp.edit().putString(KEY_USER_ID, value).apply()

    var email: String
        get() = sp.getString(KEY_EMAIL, "") ?: ""
        set(value) = sp.edit().putString(KEY_EMAIL, value).apply()

    var notificationsEnabled: Boolean
        get() = sp.getBoolean(KEY_NOTIF, true)
        set(value) = sp.edit().putBoolean(KEY_NOTIF, value).apply()

    var preferredClassId: String
        get() = sp.getString(KEY_CLASS, "") ?: ""
        set(value) = sp.edit().putString(KEY_CLASS, value).apply()

    /** Pamokų pabaigos laikai, pvz. "08:45,09:40,10:35". Naudojami priminimams be interneto. */
    var lessonEnds: String
        get() = sp.getString(KEY_LESSON_ENDS, "") ?: ""
        set(value) = sp.edit().putString(KEY_LESSON_ENDS, value.trim()).apply()

    /** Vietinė mokytojo pamokų tvarkaraščio kopija, kad priminimai veiktų be ryšio. */
    var lessonEndsByTeacher: String
        get() = sp.getString(KEY_LESSON_ENDS_BY_TEACHER, "") ?: ""
        set(value) = sp.edit().putString(KEY_LESSON_ENDS_BY_TEACHER, value.trim()).apply()

    var scheduleTeacherId: String
        get() = sp.getString(KEY_SCHEDULE_TEACHER_ID, "") ?: ""
        set(value) = sp.edit().putString(KEY_SCHEDULE_TEACHER_ID, value).apply()

    val loggedIn: Boolean get() = accessToken.isNotBlank()

    fun clearSession() {
        sp.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_EXPIRES)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .apply()
    }

    private companion object {
        const val KEY_URL = "base_url"
        const val KEY_KEY = "anon_key"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES = "expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_NOTIF = "notifications_enabled"
        const val KEY_CLASS = "preferred_class_id"
        const val KEY_LESSON_ENDS = "lesson_ends"
        const val KEY_LESSON_ENDS_BY_TEACHER = "lesson_ends_by_teacher"
        const val KEY_SCHEDULE_TEACHER_ID = "schedule_teacher_id"
    }
}
