package lt.penki.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiException(message: String, val status: Int = 0) : Exception(message)

/**
 * Minimalus Supabase klientas: GoTrue (prisijungimas) + PostgREST (duomenys).
 * Naudojamas tik "anon" raktas – duomenų prieigą riboja RLS taisyklės serveryje.
 */
class Supabase(val prefs: Prefs) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    val configured: Boolean
        get() = prefs.baseUrl.isNotBlank() && prefs.anonKey.isNotBlank()

    fun configure(url: String, key: String) {
        prefs.baseUrl = url
        prefs.anonKey = key
    }

    // ---------------------------------------------------------------- HTTP

    private suspend fun execute(
        method: String,
        path: String,
        body: String? = null,
        token: String? = null,
        prefer: String? = null
    ): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(prefs.baseUrl.trimEnd('/') + path)
            .header("apikey", prefs.anonKey)
            .header("Accept", "application/json")

        if (token != null && token.isNotBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        if (prefer != null) {
            builder.header("Prefer", prefer)
        }
        if (body != null) {
            builder.header("Content-Type", "application/json")
        }

        builder.method(method, body?.toRequestBody(jsonType))

        val response = try {
            http.newCall(builder.build()).execute()
        } catch (e: Exception) {
            throw ApiException("Nepavyko prisijungti prie serverio. Patikrinkite internetą.")
        }

        response.use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw ApiException(readError(text, res.code), res.code)
            }
            text
        }
    }

    private fun readError(raw: String, code: Int): String {
        if (raw.isNotBlank()) {
            try {
                val element = json.parseToJsonElement(raw)
                if (element is JsonObject) {
                    for (key in listOf("error_description", "msg", "message", "error", "hint", "details")) {
                        val value = try {
                            element[key]?.jsonPrimitive?.content
                        } catch (_: Exception) {
                            null
                        }
                        if (!value.isNullOrBlank()) return prettify(value, code)
                    }
                }
            } catch (_: Exception) {
                // ignoruojame – grąžinsime bendrą pranešimą
            }
        }
        return when (code) {
            401, 403 -> "Neteisingi prisijungimo duomenys arba baigėsi sesija."
            409 -> "Toks įrašas jau yra."
            404 -> "Serverio adresas neteisingas."
            else -> "Serverio klaida ($code)."
        }
    }

    private fun prettify(message: String, code: Int): String = when {
        message.contains("Invalid login credentials", true) -> "Neteisingas el. paštas arba slaptažodis."
        message.contains("Email not confirmed", true) -> "El. paštas dar nepatvirtintas. Patikrinkite paštą."
        message.contains("User already registered", true) -> "Šis el. paštas jau užregistruotas."
        message.contains("Password should be", true) -> "Slaptažodis per trumpas (bent 6 simboliai)."
        message.contains("duplicate key", true) -> "Toks įrašas jau yra."
        code == 403 -> "Neturite teisės atlikti šio veiksmo."
        else -> message
    }

    // ---------------------------------------------------------------- Auth

    private fun applySession(auth: AuthResponse, fallbackEmail: String) {
        val token = auth.access_token
            ?: throw ApiException("Nepavyko prisijungti. Bandykite dar kartą.")
        prefs.accessToken = token
        prefs.refreshToken = auth.refresh_token.orEmpty()
        prefs.expiresAt = System.currentTimeMillis() + ((auth.expires_in ?: 3600L) * 1000L)
        prefs.userId = auth.user?.id ?: auth.id ?: prefs.userId
        prefs.email = auth.user?.email ?: auth.email ?: fallbackEmail
    }

    suspend fun signIn(email: String, password: String) {
        val body = json.encodeToString(SignInRequest(email.trim(), password))
        val raw = execute("POST", "/auth/v1/token?grant_type=password", body)
        applySession(json.decodeFromString<AuthResponse>(raw), email.trim())
    }

    /** @return true, jei iš karto prisijungta; false, jei reikia patvirtinti el. paštą. */
    suspend fun signUp(email: String, password: String, fullName: String): Boolean {
        val payload = buildJsonObject {
            put("email", email.trim())
            put("password", password)
            put("data", buildJsonObject { put("full_name", fullName.trim()) })
        }.toString()
        val raw = execute("POST", "/auth/v1/signup", payload)
        val auth = json.decodeFromString<AuthResponse>(raw)
        if (auth.access_token.isNullOrBlank()) return false
        applySession(auth, email.trim())
        return true
    }

    suspend fun signOut() {
        val token = prefs.accessToken
        prefs.clearSession()
        if (token.isNotBlank()) {
            try {
                execute("POST", "/auth/v1/logout", token = token)
            } catch (_: Exception) {
                // sesija telefone jau ištrinta
            }
        }
    }

    private suspend fun freshToken(): String {
        if (prefs.accessToken.isBlank()) {
            throw ApiException("Prisijunkite iš naujo.", 401)
        }
        if (System.currentTimeMillis() < prefs.expiresAt - 90_000L) {
            return prefs.accessToken
        }
        if (prefs.refreshToken.isBlank()) {
            prefs.clearSession()
            throw ApiException("Sesija baigėsi. Prisijunkite iš naujo.", 401)
        }
        val raw = try {
            execute(
                "POST",
                "/auth/v1/token?grant_type=refresh_token",
                json.encodeToString(RefreshRequest(prefs.refreshToken))
            )
        } catch (e: ApiException) {
            prefs.clearSession()
            throw ApiException("Sesija baigėsi. Prisijunkite iš naujo.", 401)
        }
        applySession(json.decodeFromString<AuthResponse>(raw), prefs.email)
        return prefs.accessToken
    }

    // ---------------------------------------------------------------- Profiliai

    suspend fun profile(): Profile {
        val token = freshToken()
        val raw = execute("GET", "/rest/v1/profiles?select=*&id=eq.${prefs.userId}", token = token)
        return json.decodeFromString<List<Profile>>(raw).firstOrNull()
            ?: throw ApiException("Profilis nerastas. Susisiekite su administratoriumi.")
    }

    suspend fun updateMyName(name: String) {
        val token = freshToken()
        execute(
            "POST",
            "/rest/v1/rpc/set_my_name",
            json.encodeToString(NameRequest(name)),
            token
        )
    }

    suspend fun profiles(): List<Profile> {
        val token = freshToken()
        val raw = execute("GET", "/rest/v1/profiles?select=*&order=role.asc,full_name.asc", token = token)
        return json.decodeFromString(raw)
    }

    suspend fun setActive(id: String, active: Boolean) {
        val token = freshToken()
        execute(
            "PATCH",
            "/rest/v1/profiles?id=eq.$id",
            json.encodeToString(PatchActive(active)),
            token
        )
    }

    suspend fun setRole(id: String, role: String) {
        val token = freshToken()
        execute("PATCH", "/rest/v1/profiles?id=eq.$id", """{"role":"$role"}""", token)
    }

    // ---------------------------------------------------------------- Klasės

    suspend fun classes(): List<Klass> {
        val token = freshToken()
        val raw = execute("GET", "/rest/v1/classes?select=*&active=eq.true&order=name.asc", token = token)
        return json.decodeFromString(raw)
    }

    suspend fun addClass(name: String) {
        val token = freshToken()
        execute("POST", "/rest/v1/classes", json.encodeToString(NewClass(name.trim())), token)
    }

    suspend fun deleteClass(id: String) {
        val token = freshToken()
        execute("DELETE", "/rest/v1/classes?id=eq.$id", token = token)
    }

    // ------------------------------------------------- Mokytojo klasių priskyrimas

    /** Visi priskyrimai (matoma tik adminui). */
    suspend fun allTeacherClasses(): List<TeacherClass> {
        val token = freshToken()
        val raw = execute("GET", "/rest/v1/teacher_classes?select=teacher_id,class_id", token = token)
        return json.decodeFromString(raw)
    }

    /** Savo priskyrimai (mokytojo ekranui). */
    suspend fun myTeacherClasses(): List<TeacherClass> {
        val token = freshToken()
        val raw = execute(
            "GET",
            "/rest/v1/teacher_classes?select=teacher_id,class_id&teacher_id=eq.${prefs.userId}",
            token = token
        )
        return json.decodeFromString(raw)
    }

    suspend fun assignClass(teacherId: String, classId: String) {
        val token = freshToken()
        execute(
            "POST",
            "/rest/v1/teacher_classes",
            json.encodeToString(NewTeacherClass(teacherId, classId)),
            token,
            prefer = "resolution=ignore-duplicates"
        )
    }

    suspend fun unassignClass(teacherId: String, classId: String) {
        val token = freshToken()
        execute(
            "DELETE",
            "/rest/v1/teacher_classes?teacher_id=eq.$teacherId&class_id=eq.$classId",
            token = token
        )
    }

    // ---------------------------------------------------------------- Pamokų laikai

    suspend fun lessonTimes(teacherId: String = prefs.userId): List<LessonTime> {
        val token = freshToken()
        val raw = execute(
            "GET",
            "/rest/v1/lesson_times?select=teacher_id,lesson_no,ends_at&teacher_id=eq.$teacherId&order=lesson_no.asc",
            token = token
        )
        return json.decodeFromString(raw)
    }

    suspend fun saveLessonTime(teacherId: String, lessonNo: Int, endsAt: String) {
        val token = freshToken()
        execute(
            "POST",
            "/rest/v1/lesson_times?on_conflict=teacher_id,lesson_no",
            json.encodeToString(NewLessonTime(teacherId, lessonNo, endsAt)),
            token,
            prefer = "resolution=merge-duplicates"
        )
    }

    suspend fun deleteLessonTime(teacherId: String, lessonNo: Int) {
        val token = freshToken()
        execute(
            "DELETE",
            "/rest/v1/lesson_times?teacher_id=eq.$teacherId&lesson_no=eq.$lessonNo",
            token = token
        )
    }

    // ---------------------------------------------------------------- Vertinimai

    suspend fun myRatings(): List<Rating> {
        val token = freshToken()
        val raw = execute(
            "GET",
            "/rest/v1/ratings?select=*,classes(id,name)&order=created_at.desc&limit=200",
            token = token
        )
        return json.decodeFromString(raw)
    }

    /**
     * Visi vertinimai, puslapiuojant.
     *
     * Anksčiau čia buvo `limit=500`, todėl nuo 500-ojo vertinimo admino
     * statistika buvo neteisinga ir „iš viso“, ir vidurkiai būtų skaičiuojami
     * iš nutrūkusios imties.
     */
    suspend fun allRatings(): List<Rating> {
        val token = freshToken()
        val pageSize = 1000
        val maxRows = 10000
        val query = "select=*,classes(id,name),profiles(full_name)&order=created_at.desc,id.desc"
        val all = ArrayList<Rating>(pageSize)
        var offset = 0
        while (all.size < maxRows) {
            val raw = execute(
                "GET",
                "/rest/v1/ratings?$query&limit=$pageSize&offset=$offset",
                token = token
            )
            val page: List<Rating> = json.decodeFromString(raw)
            all.addAll(page)
            if (page.size < pageSize) break
            offset += pageSize
        }
        return all
    }

    suspend fun saveRating(
        classId: String,
        lessonNo: Int,
        score: Int,
        comment: String,
        date: String
    ): Rating = insertRating(prefs.userId, classId, lessonNo, score, comment, date)

    /**
     * Įrašo vertinimą nurodytam mokytojui. Paprastai teacherId = savininko ID,
     * bet adminas (debug režime) gali generuoti vertinimus ir kitiems mokytojams.
     */
    suspend fun insertRating(
        teacherId: String,
        classId: String,
        lessonNo: Int,
        score: Int,
        comment: String,
        date: String
    ): Rating {
        val token = freshToken()
        val body = json.encodeToString(
            NewRating(
                teacher_id = teacherId,
                class_id = classId,
                lesson_no = lessonNo,
                score = score,
                comment = comment.trim(),
                rated_on = date
            )
        )
        val raw = execute(
            "POST",
            "/rest/v1/ratings?on_conflict=teacher_id,class_id,rated_on,lesson_no",
            body,
            token,
            prefer = "resolution=merge-duplicates,return=representation"
        )
        return json.decodeFromString<List<Rating>>(raw).firstOrNull()
            ?: throw ApiException("Vertinimas neišsaugotas. Bandykite dar kartą.")
    }

    suspend fun appendRatingToGoogleSheets(ratingId: String): SheetsAppendResult {
        val token = freshToken()
        val body = json.encodeToString(SheetsAppendRequest(ratingId))
        val raw = execute("POST", "/functions/v1/export-rating-to-sheets", body, token)
        return json.decodeFromString(raw)
    }

    suspend fun saveGoogleSheetsConfig(enabled: Boolean, spreadsheetId: String, sheetName: String, sheetPerTeacher: Boolean) {
        val token = freshToken()
        execute(
            "POST",
            "/rest/v1/google_sheets_config?on_conflict=id",
            json.encodeToString(
                GoogleSheetsConfig(
                    id = true,
                    enabled = enabled,
                    spreadsheet_id = spreadsheetId.trim(),
                    sheet_name = sheetName.trim().ifBlank { "Vertinimai" },
                    sheet_per_teacher = sheetPerTeacher
                )
            ),
            token,
            prefer = "resolution=merge-duplicates"
        )
    }

    suspend fun googleSheetsConfig(): List<GoogleSheetsConfig> {
        val token = freshToken()
        val raw = execute(
            "GET",
            "/rest/v1/google_sheets_config?select=id,enabled,spreadsheet_id,sheet_name,sheet_per_teacher&id=eq.true",
            token = token
        )
        return json.decodeFromString(raw)
    }

    // ---------------------------------------------------------------- Kvietimai

    suspend fun invites(): List<Invite> {
        val token = freshToken()
        val raw = execute("GET", "/rest/v1/invites?select=*&order=created_at.desc", token = token)
        return json.decodeFromString(raw)
    }

    suspend fun createInvite(email: String, code: String): Invite {
        val token = freshToken()
        val body = json.encodeToString(NewInvite(email.trim(), code, prefs.userId))
        val raw = execute(
            "POST",
            "/rest/v1/invites",
            body,
            token,
            prefer = "return=representation"
        )
        return json.decodeFromString<List<Invite>>(raw).firstOrNull()
            ?: throw ApiException("Kvietimas nesukurtas.")
    }

    suspend fun deleteInvite(id: String) {
        val token = freshToken()
        execute("DELETE", "/rest/v1/invites?id=eq.$id", token = token)
    }

    suspend fun redeemInvite(code: String): RedeemResult {
        val token = freshToken()
        val raw = execute(
            "POST",
            "/rest/v1/rpc/redeem_invite",
            json.encodeToString(RedeemRequest(code.trim())),
            token
        )
        return json.decodeFromString(raw)
    }

    // ---------------------------------------------------------------- Debug (adminas)

    /**
     * Sukuria eksperimentinę paskyrą per viešą registracijos API ir grąžina jos ID.
     * Registracija laikinai pakeičia sesiją, todėl admino sesija atstatoma iškart.
     */
    suspend fun createTestAccount(fullName: String, email: String, password: String): String {
        val savedToken = prefs.accessToken
        val savedRefresh = prefs.refreshToken
        val savedExpires = prefs.expiresAt
        val savedUser = prefs.userId
        val savedEmail = prefs.email
        try {
            val payload = buildJsonObject {
                put("email", email.trim())
                put("password", password)
                put("data", buildJsonObject { put("full_name", fullName.trim()) })
            }.toString()
            val raw = execute("POST", "/auth/v1/signup", payload)
            val auth = json.decodeFromString<AuthResponse>(raw)
            // Be sesijos reiškia, kad paskyra nebuvo sukurta (toks el. paštas jau yra)
            // arba įjungtas el. pašto patvirtinimas – tokiu atveju rolės nekeičiame.
            val newId = auth.user?.id ?: auth.id
            if (auth.access_token.isNullOrBlank() || newId.isNullOrBlank()) {
                throw ApiException(
                    "Paskyra nesukurta: toks el. paštas jau užregistruotas arba Supabase reikalauja patvirtinti el. paštą."
                )
            }
            return newId
        } finally {
            prefs.accessToken = savedToken
            prefs.refreshToken = savedRefresh
            prefs.expiresAt = savedExpires
            prefs.userId = savedUser
            prefs.email = savedEmail
        }
    }

    suspend fun deleteAllRatings() {
        val token = freshToken()
        execute("DELETE", "/rest/v1/ratings?id=not.is.null", token = token)
    }

    suspend fun deleteRatingsOf(teacherId: String) {
        val token = freshToken()
        execute("DELETE", "/rest/v1/ratings?teacher_id=eq.$teacherId", token = token)
    }

    suspend fun deleteAllInvites() {
        val token = freshToken()
        execute("DELETE", "/rest/v1/invites?id=not.is.null", token = token)
    }

    // ---------------------------------------------------------------- Atsiliepimai (feedback)

    suspend fun sendFeedback(message: String): Feedback {
        val token = freshToken()
        val body = json.encodeToString(NewFeedback(prefs.userId, message.trim()))
        val raw = execute(
            "POST",
            "/rest/v1/feedback",
            body,
            token,
            prefer = "return=representation"
        )
        return json.decodeFromString<List<Feedback>>(raw).firstOrNull()
            ?: throw ApiException("Atsiliepimas neišsiųstas. Bandykite dar kartą.")
    }

    suspend fun feedback(): List<Feedback> {
        val token = freshToken()
        val raw = execute(
            "GET",
            "/rest/v1/feedback?select=*,profiles(id,email,full_name)&order=created_at.desc",
            token = token
        )
        return json.decodeFromString(raw)
    }

    suspend fun markFeedbackRead(id: String) {
        val token = freshToken()
        execute(
            "PATCH",
            "/rest/v1/feedback?id=eq.$id",
            json.encodeToString(PatchFeedbackRead(true)),
            token
        )
    }

    suspend fun deleteFeedback(id: String) {
        val token = freshToken()
        execute("DELETE", "/rest/v1/feedback?id=eq.$id", token = token)
    }
}
