package lt.penki.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LessonAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val lessonNo = intent.getIntExtra(Notifications.EXTRA_LESSON, 0)
        Notifications.ensureChannel(context)
        if (lessonNo > 0) {
            Notifications.show(context, lessonNo)
        }
        // Pasitikriname, kad visada būtų suplanuota artimiausioms dienoms
        Notifications.scheduleUpcoming(context)
    }
}
