package app.letsdoit.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun TasksScreen(onFocusTask: (String) -> Unit) {
    val tasks by Repo.tasks.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    val open = tasks.filter { !it.done }
    val done = tasks.filter { it.done }

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
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Spacer(Modifier.height(18.dp))
                Eyebrow("Tasks")
                Spacer(Modifier.height(6.dp))
                Text("One thing at a time.", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Field(text, { text = it }, "Add a task", Modifier.weight(1f), onDone = { add() })
                    Spacer(Modifier.width(10.dp))
                    IconButton(
                        onClick = { add() },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Ink)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add task", tint = Cream)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (tasks.isEmpty()) {
            item {
                Text(
                    "A clear mind starts with a small plan. Add your first task.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        items(open, key = { it.id }) { t -> TaskRow(t, onFocusTask) }
        if (done.isNotEmpty()) {
            item {
                Eyebrow("Done · ${done.size}", Modifier.padding(top = 14.dp))
            }
            items(done, key = { it.id }) { t -> TaskRow(t, onFocusTask) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun TaskRow(t: Task, onFocus: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Sand)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
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
            IconButton(onClick = { onFocus(t.id) }) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Focus on this task", tint = Ink)
            }
        }
        IconButton(onClick = { Repo.deleteTask(t.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = Muted)
        }
    }
}
