package lt.penki.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Vienas tvarkaraščio mokytojas, perskaitytas iš Google Sheets lentelės. */
data class ParsedTimetable(
    val teachers: List<TimetableSyncTeacher>,
    val lessons: List<TimetableSyncLesson>
)

/**
 * Skaito paskelbtą („Publish to web“) Google Sheets tvarkaraštį CSV formatu.
 *
 * Lentelės struktūra: pirmos eilutės yra antraštės, po jų – mokytojų eilutės.
 * Stulpeliuose „Dalykas“, „Aukl. klasė“ ir „Kab.“ yra mokytojo duomenys, o
 * penkių dienų blokuose (po 8 pamokas) – klasės kodai (pvz. `7b`, `6c1`, `5abcd`).
 * Ląstelėse gali būti kelių klasių kodai, todėl kiekvienas iš jų išskaitomas
 * atskirai, o žymos `K`, `G` ir brūkšniai praleidžiami.
 */
object TimetableParser {

    const val DEFAULT_URL =
        "https://docs.google.com/spreadsheets/d/e/2PACX-1vRCXK3XcBMt-Os59Nj-Gv3hXhRY-lz92nTmbm7jIzOR6eFLEXHsnQk-dl98-kFsZaoo9xUCIK2Rdggx/pub" +
            "?gid=1201117612&single=true&output=csv"

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    private val classPattern = Regex("([5-9])\\s*([a-dA-D]{1,4})")

    /** Paverčia „Publish to web“ nuorodą CSV eksporto nuoroda. */
    fun normalizeUrl(raw: String): String {
        val value = raw.trim().ifBlank { DEFAULT_URL }
        if (value.contains("output=")) return value
        val separator = if (value.contains('?')) "&" else "?"
        return when {
            value.contains("/pubhtml") -> value.replace("/pubhtml", "/pub") + separator + "output=csv"
            value.contains("/pub") -> value + separator + "output=csv"
            else -> value
        }
    }

    suspend fun download(url: String = DEFAULT_URL): String = withContext(Dispatchers.IO) {
        val target = normalizeUrl(url)
        val request = Request.Builder()
            .url(target)
            .header("Accept", "text/csv,text/plain,*/*")
            .build()
        val response = try {
            http.newCall(request).execute()
        } catch (e: Exception) {
            throw ApiException("Nepavyko atsisiųsti tvarkaraščio. Patikrinkite internetą.")
        }
        response.use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw ApiException(
                    "Google Sheets atsakė klaida (HTTP ${res.code}). Patikrinkite, ar lentelė paskelbta internetui."
                )
            }
            // Nepaskelbta (redagavimo) nuoroda grąžina HTML puslapį, o ne CSV –
            // be šios patikros parseris tik praneštų apie nerastą antraštę.
            val contentType = res.header("Content-Type").orEmpty()
            if (contentType.contains("html", ignoreCase = true)) {
                throw ApiException(
                    "Nuoroda grąžino ne CSV. Paskelbkite lentelę (File → Share → Publish to web) " +
                        "ir naudokite /pub nuorodą su output=csv."
                )
            }
            if (text.isBlank()) throw ApiException("Google Sheets grąžino tuščią lentelę.")
            text
        }
    }

    fun parse(csv: String): ParsedTimetable {
        val rows = parseCsv(csv)
        if (rows.isEmpty()) throw ApiException("Tuščia lentelė – tvarkaraščio perskaityti nepavyko.")

        val headerIndex = rows.indexOfFirst { row -> row.any { weekdayNumber(it) != null } }
        if (headerIndex < 0) throw ApiException("Lentelėje nerasta tvarkaraščio antraštė su savaitės dienomis.")

        val header = rows[headerIndex]
        val nameCol = columnOf(header, "Mokytojo", 1)
        val subjectCol = columnOf(header, "Dalykas", 2)
        val homeroomCol = columnOf(header, "Aukl", 3)
        val roomCol = columnOf(header, "Kab", 4)

        // Kiekvienam stulpeliui priskiriame savaitės dieną pagal antraštės žymes.
        val weekdayOf = IntArray(header.size)
        var day = 0
        for (index in header.indices) {
            weekdayNumber(header[index])?.let { day = it }
            if (index > roomCol) weekdayOf[index] = day
        }

        // Pamokos numeris imamas iš eilutės po antrašte (1..8 kiekvienai dienai).
        val numberRow = rows.getOrNull(headerIndex + 1).orEmpty()
        val lessonOf = IntArray(header.size)
        for (index in header.indices) {
            val value = numberRow.getOrNull(index)?.trim()?.toIntOrNull()
            if (value != null && value in 1..10) lessonOf[index] = value
        }
        // Atsarginis numeravimas, jei numerių eilutėje jų trūksta.
        val dayStart = HashMap<Int, Int>()
        for (index in header.indices) {
            val weekday = weekdayOf[index]
            if (weekday == 0) continue
            val start = dayStart.getOrPut(weekday) { index }
            if (lessonOf[index] == 0) lessonOf[index] = (index - start) + 1
        }

        val teachers = LinkedHashMap<String, TimetableSyncTeacher>()
        val lessons = ArrayList<TimetableSyncLesson>()

        for (rowIndex in (headerIndex + 2) until rows.size) {
            val row = rows[rowIndex]
            if (row.size <= nameCol) continue
            val name = clean(row.getOrNull(nameCol).orEmpty())
            if (!looksLikeTeacher(name)) continue

            val subject = clean(row.getOrNull(subjectCol).orEmpty())
            val homeroom = clean(row.getOrNull(homeroomCol).orEmpty())
            val room = clean(row.getOrNull(roomCol).orEmpty())
            teachers.getOrPut(name) { TimetableSyncTeacher(name, subject, homeroom, room) }

            for (column in (roomCol + 1) until row.size) {
                val weekday = weekdayOf.getOrElse(column) { 0 }
                val lessonNo = lessonOf.getOrElse(column) { 0 }
                if (weekday !in 1..5 || lessonNo !in 1..10) continue
                for (className in classNames(row[column])) {
                    lessons += TimetableSyncLesson(name, weekday, lessonNo, className, room)
                }
            }
        }

        if (teachers.isEmpty()) throw ApiException("Lentelėje nepavyko rasti nė vieno mokytojo.")
        // Apsauga nuo duomenų praradimo: tuščias sąrašas ištrintų visą tvarkaraštį serveryje.
        if (lessons.isEmpty()) {
            throw ApiException(
                "Lentelėje nerasta pamokų su klasių kodais (pvz. 7b). Patikrinkite, ar tai ta pati tvarkaraščio lentelė."
            )
        }
        return ParsedTimetable(teachers.values.toList(), lessons.distinct())
    }

    /** Iš ląstelės išskaito klasių kodus: `7b`, `6c1`, `5abcd`, `6bd6c3` ir pan. */
    fun classNames(cell: String): List<String> {
        if (cell.isBlank()) return emptyList()
        val result = LinkedHashSet<String>()
        classPattern.findAll(cell.replace('\n', ' ')).forEach { match ->
            val grade = match.groupValues[1]
            match.groupValues[2].lowercase().forEach { letter -> result += "$grade$letter" }
        }
        return result.toList()
    }

    private fun weekdayNumber(cell: String): Int? = when {
        cell.isBlank() -> null
        cell.contains("pirmadienis", true) -> 1
        cell.contains("antradienis", true) -> 2
        cell.contains("trečiadienis", true) -> 3
        cell.contains("ketvirtadienis", true) -> 4
        cell.contains("penktadienis", true) -> 5
        else -> null
    }

    private fun columnOf(header: List<String>, marker: String, fallback: Int): Int =
        header.indexOfFirst { it.contains(marker, true) }.takeIf { it >= 0 } ?: fallback

    private fun looksLikeTeacher(value: String): Boolean {
        if (value.length < 3 || value.length > 80) return false
        if (value.startsWith("-")) return false
        val lower = value.lowercase()
        if (lower.contains("žymėjimo") || lower.contains("tvarkaraštis") || lower.contains("pavardė")) return false
        return value.any { it.isLetter() }
    }

    private fun clean(value: String): String = value
        .replace('\u00a0', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    /** Minimalus CSV skaitytuvas su kabučių palaikymu (ląstelėse būna kablelių ir eilučių lūžių). */
    private fun parseCsv(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                inQuotes -> when {
                    char == '"' && index + 1 < text.length && text[index + 1] == '"' -> {
                        field.append('"')
                        index++
                    }
                    char == '"' -> inQuotes = false
                    else -> field.append(char)
                }
                char == '"' -> inQuotes = true
                char == ',' -> {
                    row.add(field.toString())
                    field.setLength(0)
                }
                char == '\r' -> Unit
                char == '\n' -> {
                    row.add(field.toString())
                    field.setLength(0)
                    rows.add(row)
                    row = ArrayList()
                }
                else -> field.append(char)
            }
            index++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }
}
