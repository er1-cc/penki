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

    suspend fun allRatings(): List<Rating> {
        val token = freshToken()
        val raw = execute(
            "GET",
            "/rest/v1/ratings?select=*,classes(id,name),profiles(full_name)&order=created_at.desc&limit=500",
            token = token
        )
        return json.decodeFromString(raw)
    }

    suspend fun saveRating(
        classId: String,
        lessonNo: Int,
        score: Int,
        comment: String,
        date: String
    ): Rating {
        val token = freshToken()
        val body = json.encodeToString(
            NewRating(
                teacher_id = prefs.userId,
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
}
