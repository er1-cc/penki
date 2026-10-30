package lt.penki.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LessonAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val lessonNo = intent.getIntExtra(Notifications.EXTRA_LESSON, 0)
        val teacherId = intent.getStringExtra(Notifications.EXTRA_TEACHER).orEmpty()
        Notifications.ensureChannel(context)
        if (lessonNo > 0) {
            Notifications.show(context, lessonNo, teacherId)
        }
        // Pasitikriname, kad visada būtų suplanuota artimiausioms dienoms
        Notifications.scheduleUpcoming(context)
    }
}
