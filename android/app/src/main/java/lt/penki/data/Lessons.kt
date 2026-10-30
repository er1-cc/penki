package lt.penki.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Pamokų pabaigos laikai mokytojo paskyrai. */
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
    private var schedules: Map<String, List<LocalTime>> = emptyMap()
    private var currentTeacherId: String = ""
    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val ends: List<LocalTime> get() = scheduleFor(currentTeacherId)
    val count: Int get() = ends.size
    val numbers: IntRange get() = 1..count

    fun endOf(lessonNo: Int): LocalTime =
        ends.getOrElse(lessonNo - 1) { ends.lastOrNull() ?: LocalTime.of(8, 45) }

    fun endText(lessonNo: Int): String = endOf(lessonNo).format(timeFormatter)
    fun today(): LocalDate = LocalDate.now()

    fun useTeacher(prefs: Prefs, teacherId: String) {
        currentTeacherId = teacherId
        schedules = parseSchedules(prefs.lessonEndsByTeacher)
        if (schedules.isEmpty()) {
            val legacy = parse(prefs.lessonEnds)
            if (legacy.isNotEmpty()) schedules = mapOf(teacherId to legacy)
        }
        prefs.scheduleTeacherId = teacherId
        prefs.lessonEnds = scheduleFor(teacherId).joinToString(",") { it.format(timeFormatter) }
    }

    fun scheduleFor(teacherId: String): List<LocalTime> =
        schedules[teacherId].orEmpty().ifEmpty { defaults }

    fun setSchedule(prefs: Prefs, teacherId: String, raw: List<Pair<Int, String>>) {
        val ordered = raw.sortedBy { it.first }
        if (ordered.isEmpty()) return
        if (ordered.map { it.first } != (1..ordered.size).toList()) return
        val parsed = ordered.mapNotNull { parseOne(it.second) }
        if (parsed.size != ordered.size) return
        val updated = schedules.toMutableMap().apply { put(teacherId, parsed) }
        schedules = updated
        persistSchedules(prefs, updated)
        if (teacherId == currentTeacherId) {
            prefs.lessonEnds = parsed.joinToString(",") { it.format(timeFormatter) }
        }
    }

    fun applySaved(prefs: Prefs) {
        currentTeacherId = prefs.scheduleTeacherId
        schedules = parseSchedules(prefs.lessonEndsByTeacher)
        val defaultSaved = parse(prefs.lessonEnds)
        if (currentTeacherId.isNotBlank() && currentTeacherId !in schedules && defaultSaved.isNotEmpty()) {
            schedules = schedules + (currentTeacherId to defaultSaved)
        }
        if (currentTeacherId.isNotBlank()) {
            prefs.lessonEnds = scheduleFor(currentTeacherId).joinToString(",") { it.format(timeFormatter) }
        }
    }

    fun reset(prefs: Prefs, teacherId: String) {
        schedules = schedules.toMutableMap().apply { remove(teacherId) }
        persistSchedules(prefs, schedules)
        if (teacherId == currentTeacherId) {
            prefs.lessonEnds = defaults.joinToString(",") { it.format(timeFormatter) }
        }
    }

    fun parseOne(text: String): LocalTime? = try {
        LocalTime.parse(text.trim().take(5), timeFormatter)
    } catch (_: Exception) {
        null
    }

    private fun persistSchedules(prefs: Prefs, values: Map<String, List<LocalTime>>) {
        prefs.lessonEndsByTeacher = values.entries.joinToString(";") { (teacherId, times) ->
            "$teacherId=${times.joinToString(",") { it.format(timeFormatter) }}"
        }
        prefs.lessonEnds = scheduleFor(currentTeacherId).joinToString(",") { it.format(timeFormatter) }
    }

    private fun parseSchedules(raw: String): Map<String, List<LocalTime>> = raw
        .split(';')
        .mapNotNull { entry ->
            val separator = entry.indexOf('=')
            if (separator < 0) return@mapNotNull null
            val teacherId = entry.substring(0, separator)
            val times = parse(entry.substring(separator + 1))
            if (times.isEmpty()) null else teacherId to times
        }
        .toMap()

    private fun parse(text: String): List<LocalTime> =
        text.split(',').mapNotNull(::parseOne)
}
