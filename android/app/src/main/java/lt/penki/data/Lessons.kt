package lt.penki.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Standartiniai lietuviškos mokyklos pamokų laikai.
 * Pranešimas rodomas iškart po pamokos pabaigos.
 */
object Lessons {

    val ends: List<LocalTime> = listOf(
        LocalTime.of(8, 45),
        LocalTime.of(9, 40),
        LocalTime.of(10, 35),
        LocalTime.of(11, 30),
        LocalTime.of(12, 25),
        LocalTime.of(13, 20),
        LocalTime.of(14, 15)
    )

    val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun endOf(lessonNo: Int): LocalTime =
        ends.getOrElse(lessonNo - 1) { ends.last() }

    fun endText(lessonNo: Int): String = endOf(lessonNo).format(timeFormatter)

    fun today(): LocalDate = LocalDate.now()
}
