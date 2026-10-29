package app.letsdoit.focus

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

data class AppUse(val pkg: String, val label: String, val ms: Long, val distracting: Boolean)

data class DayUse(val date: LocalDate, val totalMs: Long, val distractMs: Long)

// days: last 7 days, oldest first, today last.
data class UsageReport(val days: List<DayUse>, val today: List<AppUse>, val week: List<AppUse>)

val DISTRACTING_PKGS = setOf(
    "com.instagram.android", "com.instagram.barcelona", "com.google.android.youtube",
    "com.facebook.katana", "com.facebook.lite", "com.twitter.android", "com.snapchat.android",
    "com.reddit.frontpage", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
    "com.netflix.mediaclient", "com.pinterest", "com.discord", "in.mohalla.sharechat",
    "com.amazon.avod.thirdpartyclient", "tv.twitch.android.app", "in.startv.hotstar",
    "com.jio.media.ondemand"
)

// Reads how long each app was on screen, using Android's Usage Access permission.
// Everything stays on the device.
object UsageTracker {

    fun hasAccess(ctx: Context): Boolean {
        return try {
            val aom = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= 29) {
                aom.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            } else {
                aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            }
            if (mode == AppOpsManager.MODE_DEFAULT) {
                ctx.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                mode == AppOpsManager.MODE_ALLOWED
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openSettings(ctx: Context) {
        val withPkg = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + ctx.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val plain = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(withPkg)
        } catch (e: Exception) {
            runCatching { ctx.startActivity(plain) }
        }
    }

    private fun homePackages(ctx: Context): Set<String> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return ctx.packageManager.queryIntentActivities(i, 0).map { it.activityInfo.packageName }.toSet()
    }

    private fun isDistracting(ctx: Context, pkg: String): Boolean {
        if (pkg in DISTRACTING_PKGS) return true
        return runCatching {
            val cat = ctx.packageManager.getApplicationInfo(pkg, 0).category
            cat == ApplicationInfo.CATEGORY_GAME ||
                cat == ApplicationInfo.CATEGORY_SOCIAL ||
                cat == ApplicationInfo.CATEGORY_VIDEO
        }.getOrDefault(false)
    }

    // Foreground time per package between start and end, from the raw event stream.
    private fun usage(usm: UsageStatsManager, start: Long, end: Long): Map<String, Long> {
        val events = usm.queryEvents(start, end)
        val ev = UsageEvents.Event()
        val fg = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(ev)
            val pkg = ev.packageName ?: continue
            when (ev.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> fg[pkg] = ev.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val st = fg.remove(pkg)
                    if (st != null && ev.timeStamp > st) {
                        total[pkg] = (total[pkg] ?: 0L) + (ev.timeStamp - st)
                    }
                }
            }
        }
        val cap = 4L * 3_600_000L
        for ((pkg, st) in fg) {
            if (end > st) total[pkg] = (total[pkg] ?: 0L) + minOf(end - st, cap)
        }
        return total
    }

    fun load(ctx: Context): UsageReport {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val zone = ZoneId.systemDefault()
        val launchable = loadApps(ctx).map { it.pkg }.toSet()
        val home = homePackages(ctx)
        val now = System.currentTimeMillis()
        val today = LocalDate.now()

        val labels = HashMap<String, String>()
        val bad = HashMap<String, Boolean>()
        fun label(p: String): String = labels.getOrPut(p) { appLabel(ctx, p) }
        fun distract(p: String): Boolean = bad.getOrPut(p) { isDistracting(ctx, p) }

        val dayMaps = ArrayList<Pair<LocalDate, Map<String, Long>>>()
        for (i in 6 downTo 0) {
            val d = today.minusDays(i.toLong())
            val s = d.atStartOfDay(zone).toInstant().toEpochMilli()
            val e = minOf(d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), now)
            val raw = if (e > s) usage(usm, s, e) else emptyMap()
            dayMaps.add(d to raw.filter { it.value > 0L && it.key in launchable && it.key !in home })
        }

        val days = dayMaps.map { (d, m) ->
            DayUse(d, m.values.sum(), m.filter { distract(it.key) }.values.sum())
        }
        val todayApps = dayMaps.last().second
            .map { AppUse(it.key, label(it.key), it.value, distract(it.key)) }
            .sortedByDescending { it.ms }

        val agg = HashMap<String, Long>()
        dayMaps.forEach { (_, m) ->
            m.forEach { (k, v) -> agg[k] = (agg[k] ?: 0L) + v }
        }
        val weekApps = agg
            .map { AppUse(it.key, label(it.key), it.value, distract(it.key)) }
            .sortedByDescending { it.ms }

        return UsageReport(days, todayApps, weekApps)
    }
}
