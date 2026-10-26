package app.letsdoit.focus

import org.json.JSONArray
import org.json.JSONObject

// Export / import of everything the app stores: tasks, sessions, blocked-app log and settings.
// The file is plain JSON, so it survives reinstalls and signing-key changes.
object Backup {
    private const val FORMAT = "letsdoit-backup"

    fun export(): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val p = Repo.prefs.value
        root.put(
            "prefs",
            JSONObject()
                .put("defaultMin", p.defaultMin)
                .put("goalMin", p.goalMin)
                .put("shieldOn", p.shieldOn)
                .put("alerts", p.alerts)
                .put("vibrate", p.vibrate)
                .put("blocked", JSONArray(p.blocked.toList()))
        )

        val ta = JSONArray()
        Repo.tasks.value.forEach {
            ta.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("done", it.done)
                    .put("createdAt", it.createdAt)
                    .put("focusedMs", it.focusedMs)
            )
        }
        root.put("tasks", ta)

        val sa = JSONArray()
        Repo.sessions.value.forEach {
            sa.put(
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
        root.put("sessions", sa)

        val ba = JSONArray()
        Repo.blocks.value.forEach { ba.put(JSONObject().put("ts", it.ts).put("pkg", it.pkg)) }
        root.put("blocks", ba)

        return root.toString(2)
    }

    // Returns null on success, or a short error message.
    fun restore(text: String): String? {
        try {
            val root = JSONObject(text)
            if (root.optString("format") != FORMAT) return "This is not a Let's Do It backup file."

            val ta = root.optJSONArray("tasks") ?: JSONArray()
            val tasks = List(ta.length()) { i ->
                val o = ta.getJSONObject(i)
                Task(
                    o.getString("id"),
                    o.getString("title"),
                    o.optBoolean("done", false),
                    o.optLong("createdAt", 0L),
                    o.optLong("focusedMs", 0L)
                )
            }

            val sa = root.optJSONArray("sessions") ?: JSONArray()
            val sessions = List(sa.length()) { i ->
                val o = sa.getJSONObject(i)
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

            val ba = root.optJSONArray("blocks") ?: JSONArray()
            val blocks = List(ba.length()) { i ->
                val o = ba.getJSONObject(i)
                Block(o.getLong("ts"), o.getString("pkg"))
            }

            val po = root.optJSONObject("prefs")
            val prefs = if (po == null) {
                Prefs()
            } else {
                val blockedList = ArrayList<String>()
                val bl = po.optJSONArray("blocked")
                if (bl != null) {
                    for (i in 0 until bl.length()) blockedList.add(bl.getString(i))
                }
                Prefs(
                    defaultMin = po.optInt("defaultMin", 25),
                    goalMin = po.optInt("goalMin", 240),
                    shieldOn = po.optBoolean("shieldOn", false),
                    blocked = blockedList.toSet(),
                    alerts = po.optBoolean("alerts", true),
                    vibrate = po.optBoolean("vibrate", true)
                )
            }

            Repo.restore(tasks, sessions, blocks, prefs)
            return null
        } catch (e: Exception) {
            return "Could not read this file."
        }
    }
}
