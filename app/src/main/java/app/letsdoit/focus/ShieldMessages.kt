package app.letsdoit.focus

// Harsh-but-useful pushback shown when a blocked app is opened during a focus session.
// Placeholders: {app} blocked app name, {task} the session purpose, {left} time left (mm:ss).
object ShieldMessages {
    data class Msg(val title: String, val body: String)

    private val titles = listOf(
        "{app}? Seriously?",
        "Phir se {app}?",
        "Pakda gaya.",
        "Nahi. Abhi nahi.",
        "Ye kaam nahi hai.",
        "Dhyan kahan hai?",
        "Ek aur bahana?",
        "Caught you.",
        "Wapas jao.",
        "Kitni baar?",
        "Not this again.",
        "Really, {app}?"
    )

    private val withTask = listOf(
        "Tumne kaha tha \"{task}\" karoge. {app} kholna us kaam ka hissa nahi hai. Wapas jao.",
        "{task} adhura pada hai aur tum {app} mein ghus gaye. Ye rest nahi, procrastination hai.",
        "{left} baaki hain. {task} khud nahi hoga, aur {app} kahin nahi ja raha.",
        "Har baar {app} kholte ho, {task} thoda aur door chala jata hai. Aaj bhi wahi kahani?",
        "Bade plan banate ho, phir {app} pe 20 minute gawa dete ho. {task} pe wapas aao.",
        "Sach bolo: {app} mein kya zaroori tha? Kuch nahi. {task} zaroori hai.",
        "Aaj ka tum {task} poora dekhna chahta hai, {app} ka scroll nahi.",
        "{task}. Bas itna. {left} bacha hai. Kaam se bhaago mat.",
        "You said you would do: {task}. Opening {app} is just avoiding it with extra steps.",
        "Nobody ever felt proud of the hours they spent in {app}. Go finish {task}.",
        "{task} is still waiting. {app} will still be there once you have earned it.",
        "{app} comfortable lagta hai, isiliye dangerous hai. {task} mushkil lagta hai, isiliye zaroori hai.",
        "Yahi wo aadat hai jo tumhe peeche rakhti hai. {task} pe wapas jao, {left} bacha hai."
    )

    private val generic = listOf(
        "Tumne focus shuru kiya tha, {app} dekhne ke liye nahi. {left} baaki hain. Kaam pe wapas.",
        "{app} tumhara kaam nahi hai. Jo karna tha wo karo.",
        "Ek aur tab, ek aur scroll, phir ek ghanta gaya. Mat karo.",
        "Discipline ka matlab hai mood na ho tab bhi karna. Abhi wahi moment hai.",
        "Tum phir bhaag rahe ho. Bhaagna band karo, bas {left} aur.",
        "Your future self is watching you open {app} right now. Impressed? Probably not.",
        "Boredom ka ilaaj {app} nahi hai. Kaam hai.",
        "Distraction is the easiest thing in the world. That is exactly why it is not impressive.",
        "{left} left. You can survive that without {app}. Get back to work.",
        "Kisi ne kabhi nahi kaha 'kaash maine aur scroll kiya hota'. Kaam pe lago.",
        "Ye shield tumne khud lagaya tha kyunki tumhe pata tha tum yahi karoge. Ab saabit karo ki galat the.",
        "Itni mehnat se focus shuru kiya aur ek tap mein barbaad? {app} baad mein."
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
