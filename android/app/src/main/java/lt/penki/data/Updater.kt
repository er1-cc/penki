package lt.penki.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/** Repozitorijos šaknyje esantis version.json – vienintelis naujinimo šaltinis. */
@Serializable
data class UpdateManifest(
    val version: String = "",
    val apk: String = "",
    val notes: String = ""
)

/**
 * Automatinis naujinimo tikrintuvas ir atsisiųstuvė.
 *
 * Tikrinamas viešas `version.json` faile GitHub (raw) – jokių API raktų ir
 * rate limit'ų, kaip GitHub REST API. Jei ten esanti versija naujesnė už
 * `BuildConfig.VERSION_NAME`, programa parodo pranešimą, atsisiunčia APK į
 * talpyklą ir atidaro diegimo ekraną.
 */
class Updater {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Grąžina naujesnę versiją arba null (klaidos irgi null – tai nėra kritinė funkcija). */
    suspend fun check(currentVersion: String): UpdateManifest? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(MANIFEST_URL)
            .header("Accept", "application/json")
            .build()
        val raw = try {
            http.newCall(request).execute().use { res ->
                if (!res.isSuccessful) return@withContext null
                res.body?.string().orEmpty()
            }
        } catch (_: Exception) {
            return@withContext null
        }
        if (raw.isBlank()) return@withContext null
        val manifest = try {
            json.decodeFromString<UpdateManifest>(raw)
        } catch (_: Exception) {
            return@withContext null
        }
        if (manifest.version.isBlank() || manifest.apk.isBlank()) return@withContext null
        if (!isNewer(manifest.version, currentVersion)) return@withContext null
        manifest
    }

    /** Atsisiunčia APK į programos talpyklę. Grąžina failą. */
    suspend fun download(
        context: Context,
        manifest: UpdateManifest,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, "penki-${manifest.version}.apk")
        val url = if (manifest.apk.startsWith("http")) manifest.apk else RAW_BASE + manifest.apk.trimStart('/')

        val request = Request.Builder().url(url).build()
        http.newCall(request).execute().use { res ->
            if (!res.isSuccessful) {
                throw ApiException("Nepavyko atsisiųsti naujinimo (${res.code}).", res.code)
            }
            val body = res.body ?: throw ApiException("Nepavyko atsisiųsti naujinimo.")
            val total = body.contentLength()
            var read = 0L
            body.byteStream().use { input ->
                FileOutputStream(target).use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        read += n
                        if (total > 0) {
                            val percent = ((read * 100) / total).toInt().coerceIn(0, 100)
                            onProgress(percent)
                        }
                    }
                }
            }
        }
        if (!target.exists() || target.length() == 0L) {
            target.delete()
            throw ApiException("Atsisiųstas failas tuščias.")
        }
        target
    }

    /** Ar telefonas leidžia diegti programas iš šios šaltinio. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /** Atidaro nustatymus, kur leidžiama „leisti iš šio šaltinio“. */
    fun openInstallPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Atidaro Android diegimo ekraną pasirinktam APK. */
    fun install(context: Context, file: File) {
        if (!file.exists()) throw ApiException("Atsisiųstas failas dingo – atsisiųskite iš naujo.")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        const val MANIFEST_URL =
            "https://raw.githubusercontent.com/er1-cc/penki/main/version.json"
        const val RAW_BASE = "https://raw.githubusercontent.com/er1-cc/penki/main/"

        /**
         * "1.3" naujesnė už "1.2.1"; skaičiai lyginami po vieną, todėl
         * "1.10" teisingai laikoma naujesne už "1.9".
         */
        fun isNewer(candidate: String, current: String): Boolean =
            compare(candidate, current) > 0

        fun compare(a: String, b: String): Int {
            val left = a.trim().split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
            val right = b.trim().split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(left.size, right.size)) {
                val x = left.getOrElse(i) { 0 }
                val y = right.getOrElse(i) { 0 }
                if (x != y) return x.compareTo(y)
            }
            return 0
        }
    }
}
