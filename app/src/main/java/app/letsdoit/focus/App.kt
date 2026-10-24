package app.letsdoit.focus

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_TIMER, "Active focus timer", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the running focus timer with pause and end controls"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Session completion", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "An alert when your session ends"
            }
        )
    }

    companion object {
        const val CH_TIMER = "timer"
        const val CH_ALERT = "alerts"
    }
}
