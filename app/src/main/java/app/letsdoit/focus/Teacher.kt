package app.letsdoit.focus

data class Order(
    val title: String,
    val body: String,
    val actionLabel: String? = null,
    val pkg: String? = null,
    val kind: Int = 0 // 1 = block pkg, 2 = start a focus session
)

// A very strict teacher: grades your phone habits, roasts them, and gives orders.
object Teacher {

    fun grade(distractMinPerDay: Long): String = when {
        distractMinPerDay <= 30L -> "A"
        distractMinPerDay <= 60L -> "B"
        distractMinPerDay <= 120L -> "C"
        distractMinPerDay <= 180L -> "D"
        else -> "F"
    }

    fun verdict(g: String): String = when (g) {
        "A" -> "Acceptable. Barely."
        "B" -> "Average. You can do better."
        "C" -> "Mediocre. And you know it."
        "D" -> "Poor. Sit up straight."
        else -> "Failing. See me after class."
    }

    private val remarksA = listOf(
        "Acceptable. Don't mistake this for praise. {dist} on distracting apps is the minimum I expect, not an achievement.",
        "Good. There is disappointingly little to criticise today. Keep it that way tomorrow.",
        "Only {dist} wasted. Fine. Now turn that discipline into more focus time."
    )

    private val remarksB = listOf(
        "{dist} on distracting apps. Not terrible, not good. {app} took {appTime}. Cut that in half.",
        "You're hovering around average. Average people don't reach their goals. Tighten up.",
        "{app}: {appTime}. I noticed. You can do better, so stop pretending you can't."
    )

    private val remarksC = listOf(
        "{dist} gone to distractions. {app} alone took {appTime}. That time is not coming back, and I am not impressed.",
        "You gave {app} {appTime} and gave your goals {focus}. Explain that to me.",
        "This is mediocre. You know it, I know it. Fix it today, not tomorrow."
    )

    private val remarksD = listOf(
        "{dist} on distractions. That is a part-time job with no salary. {app} is your manager now, apparently.",
        "{total} on your phone, {focus} on real focus. Do the arithmetic. Then feel bad about it. Then change.",
        "You are not tired, you are distracted. {app} took {appTime}. Put the phone down."
    )

    private val remarksF = listOf(
        "{dist} on distracting apps and {focus} of real focus. {app} took {appTime} all by itself. This is not a habit, it is a surrender.",
        "Stand up. Look at your screen time: {total}. You don't have a time problem. You have a discipline problem, and today it won.",
        "Failing grade. {app}: {appTime}. See me after class, and bring a plan."
    )

    fun remark(g: String, app: String, appTime: String, dist: String, total: String, focus: String): String {
        val list = when (g) {
            "A" -> remarksA
            "B" -> remarksB
            "C" -> remarksC
            "D" -> remarksD
            else -> remarksF
        }
        return list.random()
            .replace("{app}", app)
            .replace("{appTime}", appTime)
            .replace("{dist}", dist)
            .replace("{total}", total)
            .replace("{focus}", focus)
    }

    // Per-day averages are passed in for the weekly view.
    fun orders(
        apps: List<AppUse>,
        totalMs: Long,
        distractMs: Long,
        focusMs: Long,
        blocked: Set<String>
    ): List<Order> {
        val out = ArrayList<Order>()
        val bad = apps.filter { it.distracting && it.ms >= 15L * 60_000L }.sortedByDescending { it.ms }
        bad.take(2).forEach { a ->
            if (a.pkg !in blocked) {
                out.add(
                    Order(
                        "Block " + a.label,
                        "You gave it " + fmtMs(a.ms) + ". Put it on your shield and stop negotiating with yourself.",
                        "Block it",
                        a.pkg,
                        1
                    )
                )
            } else {
                out.add(
                    Order(
                        a.label + " is blocked and still won",
                        "It took " + fmtMs(a.ms) + " even with the shield on. The shield only works during a focus session, so start one.",
                        "Start focus",
                        null,
                        2
                    )
                )
            }
        }
        if (distractMs > focusMs && distractMs >= 30L * 60_000L) {
            out.add(
                Order(
                    "Earn your scrolling",
                    "Distractions (" + fmtMs(distractMs) + ") beat focus (" + fmtMs(focusMs) + "). No more scrolling until one full 45 minute session is done.",
                    "Start focus",
                    null,
                    2
                )
            )
        }
        if (totalMs > 5L * 3_600_000L) {
            out.add(
                Order(
                    "Cap your screen time",
                    fmtMs(totalMs) + " on the phone. Set a hard limit of four hours. Everything above it is stolen from your goals."
                )
            )
        }
        if (out.isEmpty()) {
            out.add(Order("Raise the bar", "Nothing to punish. So raise your daily goal and prove this wasn't luck."))
        }
        return out.take(4)
    }
}
