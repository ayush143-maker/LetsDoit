package app.letsdoit.focus

// Sharp but useful pushback shown when a blocked app is opened during a focus session.
// Placeholders: {app} blocked app name, {task} the session purpose, {left} time left (mm:ss).
object ShieldMessages {
    data class Msg(val title: String, val body: String)

    private val titles = listOf(
        "{app}? Really?",
        "Again with {app}?",
        "Caught you.",
        "Not now.",
        "This isn't the work.",
        "Where did your focus go?",
        "Another excuse?",
        "Back to it.",
        "How many times?",
        "Not this again.",
        "Eyes up.",
        "Nice try."
    )

    private val withTask = listOf(
        "You said you'd work on {task}. Opening {app} isn't part of that. Go back.",
        "{task} is waiting, and you've wandered into {app}. That isn't rest, it's avoidance.",
        "{left} left. {task} won't finish itself, and {app} isn't going anywhere.",
        "Every time you open {app}, {task} drifts a little further away. Same story today?",
        "You make big plans, then lose twenty minutes to {app}. Get back to {task}.",
        "Be honest: what did you actually need in {app}? Nothing. {task} is what matters.",
        "Today's version of you wants {task} done, not another scroll through {app}.",
        "{task}. That's all you have to do. {left} left. Don't run from it.",
        "Opening {app} is just avoiding {task} with extra steps.",
        "Nobody ever felt proud of the hours they spent in {app}. Go finish {task}.",
        "{task} is still waiting. {app} will still be there once you've earned it.",
        "{app} feels comfortable, which is exactly why it's dangerous. {task} feels hard, which is exactly why it matters.",
        "This is the habit that holds you back. Return to {task}. {left} left."
    )

    private val generic = listOf(
        "You started this session to work, not to browse {app}. {left} left. Get back to it.",
        "{app} isn't what you came here to do. Go do what you planned.",
        "One more tab, one more scroll, and an hour is gone. Don't.",
        "Discipline means doing it even when you're not in the mood. This is that moment.",
        "You're running again. Stop running. Just {left} more.",
        "Your future self is watching you open {app} right now. Impressed? Probably not.",
        "Boredom isn't cured by {app}. It's cured by work.",
        "Distraction is the easiest thing in the world. That's exactly why it isn't impressive.",
        "{left} left. You can survive that without {app}. Get back to work.",
        "No one has ever wished they had scrolled more. Get to work.",
        "You turned this shield on because you knew you'd do exactly this. Prove yourself wrong.",
        "You worked hard to start this session. Don't throw it away on {app}. It can wait."
    )

    private var lastTitle = ""
    private var lastBody = ""

    private fun pickNew(list: List<String>, last: String): String {
        val choices = list.filter { it != last }
        return if (choices.isEmpty()) list.random() else choices.random()
    }

    private fun fill(s: String, app: String, task: String, left: String): String =
        s.replace("{app}", app).replace("{task}", task).replace("{left}", left)

    fun pick(app: String, task: String, left: String): Msg {
        val pool = if (task.isBlank()) generic else withTask + generic.take(3)
        val t = pickNew(titles, lastTitle)
        val b = pickNew(pool, lastBody)
        lastTitle = t
        lastBody = b
        return Msg(fill(t, app, task, left), fill(b, app, task, left))
    }
}
