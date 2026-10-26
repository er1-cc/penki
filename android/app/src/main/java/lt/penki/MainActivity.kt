package lt.penki

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import lt.penki.notify.Notifications
import lt.penki.ui.AppViewModel
import lt.penki.ui.PenkiApp
import lt.penki.ui.PenkiTheme

class MainActivity : ComponentActivity() {

    private val openedLesson = MutableStateFlow<Int?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Notifications.ensureChannel(this)
        Notifications.scheduleUpcoming(this)
        readLesson(intent)
        askNotificationPermission()

        setContent {
            val vm: AppViewModel = viewModel()
            val lesson by openedLesson.collectAsState()

            PenkiTheme {
                PenkiApp(
                    viewModel = vm,
                    openLesson = lesson,
                    onLessonOpened = { openedLesson.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readLesson(intent)
    }

    private fun readLesson(intent: Intent?) {
        val lesson = intent?.getIntExtra(Notifications.EXTRA_LESSON, 0) ?: 0
        if (lesson > 0) {
            openedLesson.value = lesson
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(this)) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
