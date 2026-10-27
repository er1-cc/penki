package lt.penki.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Pamokų pabaigos laikai.
 *
 * Numatytosios reikšmės naudojamos pirmą kartą paleidus programą. Administratorius
 * laikus keičia admin panelėje – jie saugomi serveryje (`lesson_times`) ir telefone,
 * kad priminimai veiktų ir be interneto.
 */
object Lessons {

    val defaults: List<LocalTime> = listOf(
        LocalTime.of(8, 45),
        LocalTime.of(9, 40),
        LocalTime.of(10, 35),
        LocalTime.of(11, 30),
        LocalTime.of(12, 25),
        LocalTime.of(13, 20),
        LocalTime.of(14, 15)
    )

    const val MAX_LESSONS = 10

    private var times: List<LocalTime> = defaults

    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    /** Galiojantys pamokų pabaigos laikai (ilgis = pamokų skaičius). */
    val ends: List<LocalTime> get() = times

    val count: Int get() = times.size

    /** Pamokų numeriai, pvz. 1..7. */
    val numbers: IntRange get() = 1..times.size

    fun endOf(lessonNo: Int): LocalTime =
        times.getOrElse(lessonNo - 1) { times.lastOrNull() ?: LocalTime.of(8, 45) }

    fun endText(lessonNo: Int): String = endOf(lessonNo).format(timeFormatter)

    fun today(): LocalDate = LocalDate.now()

    /** Nuskaito laikus iš telefono atminties (reikia po telefono perkrovimo). */
    fun applySaved(prefs: Prefs) {
        times = parse(prefs.lessonEnds).ifEmpty { defaults }
    }

    /** Įrašo laikus gautus iš serverio ir išsaugo telefone priminimams. */
    fun applyServer(prefs: Prefs, raw: List<String>) {
        val parsed = raw.mapNotNull(::parseOne)
        if (parsed.isEmpty()) return
        times = parsed
        prefs.lessonEnds = parsed.joinToString(",") { it.format(timeFormatter) }
    }

    /** Grąžina numatytuosius laikus (admin → debug / Laikai → Atstatyti). */
    fun reset(prefs: Prefs) {
        times = defaults
        prefs.lessonEnds = defaults.joinToString(",") { it.format(timeFormatter) }
    }

    /** "14:15" arba "14:15:00" → LocalTime; netinkamas tekstas → null. */
    fun parseOne(text: String): LocalTime? = try {
        LocalTime.parse(text.trim().take(5), timeFormatter)
    } catch (_: Exception) {
        null
    }

    private fun parse(text: String): List<LocalTime> =
        text.split(',').mapNotNull(::parseOne)
}
