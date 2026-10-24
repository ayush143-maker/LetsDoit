package app.letsdoit.focus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Task(
    val id: String,
    val title: String,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val focusedMs: Long = 0L
)

data class Session(
    val id: String,
    val start: Long,
    val end: Long,
    val focusedMs: Long,
    val plannedMin: Int,
    val completed: Boolean,
    val purpose: String,
    val taskId: String?
)

data class Active(
    val plannedMin: Int,
    val purpose: String,
    val taskId: String?,
    val startedAt: Long,
    val running: Boolean,
    val remainingAtResumeMs: Long,
    val resumedAt: Long,
    val focusedBeforeMs: Long
) {
    fun remaining(now: Long): Long =
        if (running) maxOf(0L, remainingAtResumeMs - (now - resumedAt)) else remainingAtResumeMs

    fun focused(now: Long): Long {
        val extra = if (running) (now - resumedAt) else 0L
        return minOf(plannedMin * 60_000L, focusedBeforeMs + extra)
    }
}

data class Block(val ts: Long, val pkg: String)

data class Prefs(
    val defaultMin: Int = 25,
    val goalMin: Int = 60,
    val shieldOn: Boolean = false,
    val blocked: Set<String> = emptySet(),
    val alerts: Boolean = true,
    val vibrate: Boolean = true
)

data class AppInfo(val pkg: String, val label: String)

object Repo {
    private lateinit var sp: SharedPreferences
    private var ready = false

    val tasks = MutableStateFlow<List<Task>>(emptyList())
    val sessions = MutableStateFlow<List<Session>>(emptyList())
    val blocks = MutableStateFlow<List<Block>>(emptyList())
    val active = MutableStateFlow<Active?>(null)
    val prefs = MutableStateFlow(Prefs())
    val lastResult = MutableStateFlow<Session?>(null)

    fun init(ctx: Context) {
        if (ready) return
        ready = true
        sp = ctx.applicationContext.getSharedPreferences("letsdoit", Context.MODE_PRIVATE)
        tasks.value = readTasks()
        sessions.value = readSessions()
        blocks.value = readBlocks()
        active.value = readActive()
        prefs.value = readPrefs()
    }

    // ---------- read ----------
    private fun readTasks(): List<Task> = runCatching {
        val arr = JSONArray(sp.getString("tasks", null) ?: "[]")
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Task(
                o.getString("id"),
                o.getString("title"),
                o.optBoolean("done", false),
                o.optLong("createdAt", 0L),
                o.optLong("focusedMs", 0L)
            )
        }
    }.getOrDefault(emptyList())

    private fun readSessions(): List<Session> = runCatching {
        val arr = JSONArray(sp.getString("sessions", null) ?: "[]")
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Session(
                o.getString("id"),
                o.getLong("start"),
                o.getLong("end"),
                o.getLong("focusedMs"),
                o.getInt("plannedMin"),
                o.getBoolean("completed"),
                o.optString("purpose", ""),
                if (o.isNull("taskId")) null else o.getString("taskId")
            )
        }
    }.getOrDefault(emptyList())

    private fun readBlocks(): List<Block> = runCatching {
        val arr = JSONArray(sp.getString("blocks", null) ?: "[]")
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Block(o.getLong("ts"), o.getString("pkg"))
        }
    }.getOrDefault(emptyList())

    private fun readActive(): Active? = runCatching {
        val s = sp.getString("active", null)
        if (s == null) {
            null
        } else {
            val o = JSONObject(s)
            Active(
                o.getInt("plannedMin"),
                o.optString("purpose", ""),
                if (o.isNull("taskId")) null else o.getString("taskId"),
                o.getLong("startedAt"),
                o.getBoolean("running"),
                o.getLong("remainingAtResumeMs"),
                o.getLong("resumedAt"),
                o.getLong("focusedBeforeMs")
            )
        }
    }.getOrNull()

    private fun readPrefs(): Prefs = Prefs(
        defaultMin = sp.getInt("defaultMin", 25),
        goalMin = sp.getInt("goalMin", 60),
        shieldOn = sp.getBoolean("shieldOn", false),
        blocked = (sp.getStringSet("blocked", null) ?: emptySet<String>()).toSet(),
        alerts = sp.getBoolean("alerts", true),
        vibrate = sp.getBoolean("vibrate", true)
    )

    // ---------- write ----------
    private fun saveTasks() {
        val arr = JSONArray()
        tasks.value.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("done", it.done)
                    .put("createdAt", it.createdAt)
                    .put("focusedMs", it.focusedMs)
            )
        }
        sp.edit().putString("tasks", arr.toString()).apply()
    }

    private fun saveSessions() {
        val arr = JSONArray()
        sessions.value.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("start", it.start)
                    .put("end", it.end)
                    .put("focusedMs", it.focusedMs)
                    .put("plannedMin", it.plannedMin)
                    .put("completed", it.completed)
                    .put("purpose", it.purpose)
                    .put("taskId", it.taskId ?: JSONObject.NULL)
            )
        }
        sp.edit().putString("sessions", arr.toString()).apply()
    }

    private fun saveBlocks() {
        val arr = JSONArray()
        blocks.value.forEach { arr.put(JSONObject().put("ts", it.ts).put("pkg", it.pkg)) }
        sp.edit().putString("blocks", arr.toString()).apply()
    }

    fun setActive(a: Active?) {
        active.value = a
        if (a == null) {
            sp.edit().remove("active").apply()
        } else {
            val o = JSONObject()
                .put("plannedMin", a.plannedMin)
                .put("purpose", a.purpose)
                .put("taskId", a.taskId ?: JSONObject.NULL)
                .put("startedAt", a.startedAt)
                .put("running", a.running)
                .put("remainingAtResumeMs", a.remainingAtResumeMs)
                .put("resumedAt", a.resumedAt)
                .put("focusedBeforeMs", a.focusedBeforeMs)
            sp.edit().putString("active", o.toString()).apply()
        }
    }

    fun updatePrefs(f: (Prefs) -> Prefs) {
        val p = f(prefs.value)
        prefs.value = p
        sp.edit()
            .putInt("defaultMin", p.defaultMin)
            .putInt("goalMin", p.goalMin)
            .putBoolean("shieldOn", p.shieldOn)
            .putStringSet("blocked", HashSet(p.blocked))
            .putBoolean("alerts", p.alerts)
            .putBoolean("vibrate", p.vibrate)
            .apply()
    }

    fun addTask(title: String) {
        tasks.value = listOf(Task(UUID.randomUUID().toString(), title)) + tasks.value
        saveTasks()
    }

    fun toggleTask(id: String) {
        tasks.value = tasks.value.map { if (it.id == id) it.copy(done = !it.done) else it }
        saveTasks()
    }

    fun deleteTask(id: String) {
        tasks.value = tasks.value.filter { it.id != id }
        saveTasks()
    }

    fun addTaskTime(id: String, ms: Long) {
        tasks.value = tasks.value.map { if (it.id == id) it.copy(focusedMs = it.focusedMs + ms) else it }
        saveTasks()
    }

    fun addSession(s: Session) {
        sessions.value = (sessions.value + s).takeLast(1000)
        saveSessions()
    }

    fun addBlock(pkg: String) {
        blocks.value = (blocks.value + Block(System.currentTimeMillis(), pkg)).takeLast(500)
        saveBlocks()
    }
}

object Focus {
    fun start(ctx: Context, minutes: Int, purpose: String, taskId: String?) {
        if (Repo.active.value != null) return
        val now = System.currentTimeMillis()
        Repo.lastResult.value = null
        Repo.setActive(
            Active(minutes, purpose.trim(), taskId, now, true, minutes * 60_000L, now, 0L)
        )
        ContextCompat.startForegroundService(ctx, Intent(ctx, FocusService::class.java))
    }

    fun pause() {
        val a = Repo.active.value ?: return
        if (!a.running) return
        val now = System.currentTimeMillis()
        Repo.setActive(
            a.copy(
                running = false,
                remainingAtResumeMs = a.remaining(now),
                focusedBeforeMs = a.focusedBeforeMs + (now - a.resumedAt)
            )
        )
    }

    fun resume() {
        val a = Repo.active.value ?: return
        if (a.running) return
        Repo.setActive(a.copy(running = true, resumedAt = System.currentTimeMillis()))
    }

    fun finish(ctx: Context, completed: Boolean) {
        val a = Repo.active.value ?: return
        val now = System.currentTimeMillis()
        val focused = if (completed) a.plannedMin * 60_000L else a.focused(now)
        Repo.setActive(null)
        if (focused >= 10_000L) {
            val s = Session(
                UUID.randomUUID().toString(), a.startedAt, now, focused,
                a.plannedMin, completed, a.purpose, a.taskId
            )
            Repo.addSession(s)
            Repo.lastResult.value = s
            val tid = a.taskId
            if (tid != null) Repo.addTaskTime(tid, focused)
        }
    }
}

object Stats {
    fun zone(): ZoneId = ZoneId.systemDefault()

    fun day(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone()).toLocalDate()

    fun minutesByDay(s: List<Session>): Map<LocalDate, Long> =
        s.groupBy { day(it.end) }.mapValues { e -> e.value.sumOf { it.focusedMs } / 60_000L }

    fun streak(map: Map<LocalDate, Long>, goal: Int, today: LocalDate): Int {
        var d = if ((map[today] ?: 0L) >= goal) today else today.minusDays(1)
        var n = 0
        while ((map[d] ?: 0L) >= goal) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    fun bestHour(s: List<Session>): Int? {
        if (s.size < 3) return null
        val m = HashMap<Int, Long>()
        s.forEach {
            val h = Instant.ofEpochMilli(it.start).atZone(zone()).hour
            m[h] = (m[h] ?: 0L) + it.focusedMs
        }
        return m.maxByOrNull { it.value }?.key
    }

    fun hourLabel(h: Int): String {
        val h12 = if (h % 12 == 0) 12 else h % 12
        return h12.toString() + (if (h < 12) " AM" else " PM")
    }

    fun whenLabel(ms: Long): String {
        val z = Instant.ofEpochMilli(ms).atZone(zone())
        val t = z.format(DateTimeFormatter.ofPattern("HH:mm"))
        return if (z.toLocalDate() == LocalDate.now()) "Today · $t"
        else z.format(DateTimeFormatter.ofPattern("MMM d")) + " · " + t
    }
}

fun fmtClock(ms: Long): String {
    val s = (ms + 999L) / 1000L
    val m = s / 60L
    val ss = s % 60L
    return String.format("%02d:%02d", m, ss)
}

fun fmtMin(min: Long): String =
    if (min >= 60L) "${min / 60L}h ${min % 60L}m" else "$min min"

fun fmtMs(ms: Long): String = fmtMin(ms / 60_000L)

fun accessibilityOn(ctx: Context): Boolean {
    val enabled = Settings.Secure.getString(
        ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val me = ComponentName(ctx, DistractionService::class.java)
    return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
}

fun loadApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .filter { it.pkg != ctx.packageName }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

fun appLabel(ctx: Context, pkg: String): String = runCatching {
    val pm = ctx.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
}.getOrDefault(pkg)
