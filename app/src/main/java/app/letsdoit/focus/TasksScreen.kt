package app.letsdoit.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun TasksScreen(onFocusTask: (String) -> Unit) {
    val tasks by Repo.tasks.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    val open = tasks.filter { !it.done }
    val done = tasks.filter { it.done }
    val focusedTotal = tasks.sumOf { it.focusedMs }

    fun add() {
        if (text.isNotBlank()) {
            Repo.addTask(text.trim())
            text = ""
        }
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PageHeader("Tasks", "One thing at a time. Add it, focus on it, finish it.")
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Field(text, { text = it }, "What needs doing?", Modifier.weight(1f), onDone = { add() })
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RBox)
                        .background(Ink)
                        .clickable { add() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add task", tint = Sand)
                }
            }
        }

        if (tasks.isNotEmpty()) {
            item {
                val frac = done.size.toFloat() / tasks.size.toFloat()
                Column(Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${done.size} of ${tasks.size} done",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            fmtMs(focusedTotal) + " focused",
                            style = MaterialTheme.typography.labelMedium,
                            color = Muted
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Line)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(frac)
                                .fillMaxHeight()
                                .background(Accent)
                        )
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LogoMark(64.dp)
                    Spacer(Modifier.height(18.dp))
                    Text("Nothing here yet", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Add your first task above. Small and specific works best.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (open.isNotEmpty()) {
            item { UpNextCard(open.first(), onFocusTask) }
            if (open.size > 1) {
                item { Eyebrow("Later · ${open.size - 1}", Modifier.padding(top = 12.dp)) }
                items(open.drop(1), key = { it.id }) { t -> TaskRow(t, onFocusTask) }
            }
        }
        if (done.isNotEmpty()) {
            item { Eyebrow("Done · ${done.size}", Modifier.padding(top = 12.dp)) }
            items(done, key = { it.id }) { t -> TaskRow(t, onFocusTask) }
        }
    }
}

@Composable
fun UpNextCard(t: Task, onFocus: (String) -> Unit) {
    val soft = Sand.copy(alpha = 0.6f)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RCard)
            .background(Ink)
            .padding(start = 20.dp, end = 10.dp, top = 12.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Up next", Modifier.weight(1f), color = soft)
            IconButton(onClick = { Repo.deleteTask(t.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = soft)
            }
        }
        Column(Modifier.padding(end = 10.dp)) {
            Text(t.title, color = Sand, style = MaterialTheme.typography.headlineMedium)
            if (t.focusedMs >= 60_000L) {
                Spacer(Modifier.height(4.dp))
                Text(
                    fmtMs(t.focusedMs) + " focused so far",
                    color = soft,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PButton("Start focus", Modifier.weight(1f), ink = Accent, paper = Sand) { onFocus(t.id) }
                PButton("Mark done", Modifier.weight(1f), filled = false, ink = Sand, paper = Ink) {
                    Repo.toggleTask(t.id)
                }
            }
        }
    }
}

@Composable
fun TaskRow(t: Task, onFocus: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RBox)
            .background(Sand)
            .border(1.dp, Line, RBox)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckCircle(t.done) { Repo.toggleTask(t.id) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                t.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (t.done) Muted else Ink,
                textDecoration = if (t.done) TextDecoration.LineThrough else null
            )
            if (t.focusedMs >= 60_000L) {
                Text(
                    fmtMs(t.focusedMs) + " focused",
                    style = MaterialTheme.typography.labelMedium,
                    color = Muted
                )
            }
        }
        if (!t.done) {
            Box(
                Modifier
                    .clip(RSmall)
                    .border(1.dp, Accent, RSmall)
                    .clickable { onFocus(t.id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text("Focus", color = Accent, style = MaterialTheme.typography.labelLarge)
            }
        }
        IconButton(onClick = { Repo.deleteTask(t.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = Muted)
        }
    }
}
