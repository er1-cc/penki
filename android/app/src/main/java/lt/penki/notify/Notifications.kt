package lt.penki.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import lt.penki.MainActivity
import lt.penki.R
import lt.penki.data.Lessons
import lt.penki.data.Prefs
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Priminimai po kiekvienos pamokos.
 * Naudojamas AlarmManager, todėl pranešimai ateina ir tada, kai programa uždaryta
 * arba telefonas užrakintas (nereikia interneto).
 */
object Notifications {

    const val CHANNEL_ID = "pamokos"
    const val EXTRA_LESSON = "lesson_no"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pamokų priminimai",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Priminimas įvertinti klasės elgesį po kiekvienos pamokos"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun show(context: Context, lessonNo: Int) {
        if (!canPost(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_LESSON, lessonNo)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            9000 + lessonNo,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Baigėsi ${lessonNo}-a pamoka")
            .setContentText("Įvertink klasės elgesį nuo 1 iki 5.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        try {
            manager.notify(1000 + lessonNo, notification)
        } catch (_: SecurityException) {
            // leidimas atšauktas – pranešimo nerodome
        }
    }

    /** Suplanuoja artimiausių 7 dienų priminimus (praleidžia savaitgalį). */
    fun scheduleUpcoming(context: Context) {
        val prefs = Prefs(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return

        if (!prefs.notificationsEnabled) {
            cancelAll(context)
            return
        }

        val now = LocalDateTime.now()
        for (offset in 0..6) {
            val day = now.toLocalDate().plusDays(offset.toLong())
            if (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) continue

            Lessons.ends.forEachIndexed { index, end ->
                val at = LocalDateTime.of(day, end)
                if (at.isBefore(now.plusMinutes(1))) return@forEachIndexed

                val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                scheduleAlarm(context, alarmManager, offset * 100 + (index + 1), triggerAt, index + 1)
            }
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        triggerAt: Long,
        lessonNo: Int
    ) {
        val intent = Intent(context, LessonAlarmReceiver::class.java)
            .putExtra(EXTRA_LESSON, lessonNo)

        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (canBeExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        for (requestCode in 0..800) {
            val intent = Intent(context, LessonAlarmReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.cancel(pending)
        }
        context.getSystemService(NotificationManager::class.java)?.cancelAll()
    }
}
