package app.letsdoit.focus

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

object UiState {
    private var sp: SharedPreferences? = null
    val onboarded = MutableStateFlow(false)
    val dismissedId = MutableStateFlow<String?>(null)

    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.applicationContext.getSharedPreferences("letsdoit_ui", Context.MODE_PRIVATE)
        sp = p
        onboarded.value = p.getBoolean("onboarded", false)
    }

    fun finishOnboarding() {
        onboarded.value = true
        sp?.edit()?.putBoolean("onboarded", true)?.apply()
    }
}
