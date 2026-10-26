package app.letsdoit.focus

import java.time.LocalDate

data class Quote(val text: String, val by: String = "")

object Quotes {
    val all = listOf(
        Quote("Start small. Start now."),
        Quote("One focused hour beats a distracted day."),
        Quote("Small steps, taken daily, outrun big plans."),
        Quote("Tomorrow never arrives on its own. Begin today."),
        Quote("Silence the noise. Do the work."),
        Quote("Phone down. Work up."),
        Quote("Progress is quiet. Keep showing up."),
        Quote("Discipline is a gift you give your future self."),
        Quote("One task. Full attention."),
        Quote("Don't wait for motivation. Build momentum."),
        Quote("Small sessions, stacked daily, change everything."),
        Quote("Boredom is the doorway to deep work."),
        Quote("When your mind wanders, bring it back. That is the practice."),
        Quote("You don't need more time. You need fewer tabs."),
        Quote("Finish one thing today."),
        Quote("Effort compounds, just like interest."),
        Quote("Time isn't scarce. Attention is scattered."),
        Quote("Make it simple, make it daily, make it count."),
        Quote("Today's work is tomorrow's relief."),
        Quote("Be where your hands are."),
        Quote("Go slowly, but never stop."),
        Quote("Your attention is the most valuable thing you own."),
        Quote("Ten more minutes is how every big thing gets built."),
        Quote("Scroll later. Build now."),
        Quote("What you postpone today becomes tomorrow's weight."),
        Quote("Motivation fades. Systems stay."),
        Quote("Your phone is a tool, not your owner."),
        Quote("One hour of deep work can outweigh a week of busyness."),
        Quote("Hard things first. Easy things can wait."),
        Quote("Where attention goes, life follows."),
        Quote("Don't trade what you want most for what you want right now."),
        Quote("A little discomfort now buys a lot of peace later."),
        Quote("Your habits are voting for who you become."),
        Quote("Focus is a muscle. Today is training day."),
        Quote("Showing up counts, even on the days it feels like nothing."),
        Quote("Clear mind, clear desk, one clear goal."),
        Quote("You can do hard things. Start with the next ten minutes."),
        Quote("Every minute you protect is a minute you own."),
        Quote("Well begun is half done.", "Aristotle"),
        Quote("It is not that we have a short time to live, but that we waste much of it.", "Seneca"),
        Quote("A journey of a thousand miles begins with a single step.", "Lao Tzu"),
        Quote("Do what you can, with what you have, where you are.", "Theodore Roosevelt")
    )

    fun today(): Quote = all[(LocalDate.now().toEpochDay() % all.size).toInt()]

    fun random(): Quote = all.random()
}
