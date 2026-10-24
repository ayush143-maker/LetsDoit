package app.letsdoit.focus

import java.time.LocalDate

data class Quote(val text: String, val by: String = "")

object Quotes {
    val all = listOf(
        Quote("Start small. Start now."),
        Quote("One focused hour beats a distracted day."),
        Quote("Chhota step, roz ka. Wahi asli speed hai."),
        Quote("Kal kabhi nahi aata. Aaj hi shuru karo."),
        Quote("Silence the noise. Do the work."),
        Quote("Phone neeche. Kaam upar."),
        Quote("Progress is quiet. Keep showing up."),
        Quote("Discipline is a gift you give tomorrow."),
        Quote("Ek kaam, poora dhyan."),
        Quote("Don't wait for motivation. Build momentum."),
        Quote("Small sessions, stacked daily, change everything."),
        Quote("Boredom is the doorway to deep work."),
        Quote("Mann bhatke to wapas lao. Bas yahi practice hai."),
        Quote("You don't need more time. You need fewer tabs."),
        Quote("Finish one thing today."),
        Quote("Effort compounds like interest."),
        Quote("Waqt kam nahi hai, dhyan bikhra hua hai."),
        Quote("Make it boring, make it daily, make it count."),
        Quote("Today's work is tomorrow's relief."),
        Quote("Be where your hands are."),
        Quote("Dheere chalo, par chalte raho."),
        Quote("Your attention is the most valuable thing you own."),
        Quote("Ten more minutes is how every big thing gets built."),
        Quote("Scroll later. Build now."),
        Quote("Well begun is half done.", "Aristotle"),
        Quote("It is not that we have a short time to live, but that we waste much of it.", "Seneca"),
        Quote("A journey of a thousand miles begins with a single step.", "Lao Tzu"),
        Quote("Do what you can, with what you have, where you are.", "Theodore Roosevelt")
    )

    fun today(): Quote = all[(LocalDate.now().toEpochDay() % all.size).toInt()]

    fun random(): Quote = all.random()
}
