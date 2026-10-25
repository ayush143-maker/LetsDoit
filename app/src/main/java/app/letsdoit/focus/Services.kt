package app.letsdoit.focus

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

class FocusService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var lastKey = ""
    private val tick = object : Runnable {
        override fun run() {
            onTick()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startFg(buildNotification(Repo.active.value))
        when (intent?.action) {
            ACTION_PAUSE -> Focus.pause()
            ACTION_RESUME -> Focus.resume()
            ACTION_END -> Focus.finish(this, false)
        }
        if (Repo.active.value == null) {
            shutdown()
            return START_NOT_STICKY
        }
        lastKey = ""
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun startFg(n: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun shutdown() {
        handler.removeCallbacks(tick)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTick() {
        val a = Repo.active.value
        if (a == null) {
            shutdown()
            return
        }
        val now = System.currentTimeMillis()
        if (a.running && a.remaining(now) <= 0L) {
            Focus.finish(this, true)
            notifyDone(a)
            shutdown()
            return
        }
        val key = "${a.running}-${a.remainingAtResumeMs}-${a.resumedAt}-${a.purpose}"
        if (key != lastKey) {
            lastKey = key
            getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(a))
        }
        handler.postDelayed(tick, 1000L)
    }

    private fun svc(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this, code,
            Intent(this, FocusService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun buildNotification(a: Active?): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val b = NotificationCompat.Builder(this, App.CH_TIMER)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (a == null) {
            b.setContentTitle("Let's Do It")
        } else {
            b.setContentTitle(if (a.purpose.isNotBlank()) a.purpose else "Focus session")
            if (a.running) {
                val end = a.resumedAt + a.remainingAtResumeMs
                b.setContentText("Focus in progress")
                    .setShowWhen(true)
                    .setWhen(end)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                b.addAction(0, "Pause", svc(ACTION_PAUSE, 1))
            } else {
                b.setContentText("Paused · " + fmtClock(a.remainingAtResumeMs) + " left")
                b.addAction(0, "Resume", svc(ACTION_RESUME, 2))
            }
            b.addAction(0, "End", svc(ACTION_END, 3))
        }
        return b.build()
    }

    private fun notifyDone(a: Active) {
        val prefs = Repo.prefs.value
        if (!prefs.alerts) return
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val q = Quotes.random()
        val what = if (a.purpose.isNotBlank()) a.purpose else "your focus session"
        val n = NotificationCompat.Builder(this, App.CH_ALERT)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("Focus complete")
            .setContentText("${a.plannedMin} min on $what. " + q.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${a.plannedMin} min on $what.\n" + q.text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_DONE, n)
        if (prefs.vibrate) {
            runCatching {
                val v = getSystemService(Vibrator::class.java)
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 220, 120, 220), -1))
            }
        }
    }

    companion object {
        const val NOTIF_ID = 11
        const val NOTIF_DONE = 12
        const val ACTION_PAUSE = "app.letsdoit.focus.PAUSE"
        const val ACTION_RESUME = "app.letsdoit.focus.RESUME"
        const val ACTION_END = "app.letsdoit.focus.END"
    }
}

class DistractionService : AccessibilityService() {
    private var lastBlockAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val a = Repo.active.value ?: return
        val p = Repo.prefs.value
        if (!a.running || !p.shieldOn || pkg !in p.blocked) return
        val now = System.currentTimeMillis()
        if (now - lastBlockAt < 800L) return
        lastBlockAt = now
        Repo.addBlock(pkg)
        // Show the shield right on top of the blocked app. Pressing HOME first made the
        // launcher race the shield screen and win, so the app was just cut with no message.
        val i = Intent(this, ShieldActivity::class.java)
            .putExtra("pkg", pkg)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        try {
            startActivity(i)
        } catch (e: Exception) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() {}
}
