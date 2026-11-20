package com.ttapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttapp.data.*
import com.ttapp.domain.TimeMath
import com.ttapp.ui.Cyan
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(repo: TrackerRepository, taskId: Long, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sessions by repo.dao.sessions(taskId).collectAsState(initial = emptyList())
    var selectedSession by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    Column {
        Row {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Text("Session history", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(8.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(sessions, key = { it.id }) { session ->
                val isExpanded = selectedSession == session.id
                val intervals by repo.dao.intervals(session.id).collectAsState(initial = emptyList())
                Card(onClick = {
                    selectedSession = if (isExpanded) null else session.id
                    note = session.notes
                }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(DateFormat.getDateTimeInstance().format(Date(session.startedAt)))
                        Text(if (session.endedAt == null) "Open session" else "Ended " +
                            DateFormat.getTimeInstance().format(Date(session.endedAt)),
                            color = Cyan)
                        if (isExpanded) {
                            Spacer(Modifier.height(8.dp))
                            Text("Recorded: " + TimeMath.format(intervals.sumOf { it.durationMs }))
                            OutlinedTextField(value = note, onValueChange = { note = it },
                                label = { Text("Session notes") })
                            TextButton(onClick = {
                                scope.launch { repo.setSessionNotes(session.id, note) }
                            }) { Text("Save notes") }
                            intervals.forEach { interval ->
                                IntervalRow(repo, interval)
                            }
                            TextButton(onClick = {
                                scope.launch { repo.deleteSession(session.id); selectedSession = null }
                            }) { Text("Delete session", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntervalRow(repo: TrackerRepository, interval: TimeIntervalEntity) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var start by remember(interval.id) { mutableStateOf(interval.startedAt.toString()) }
    var end by remember(interval.id) { mutableStateOf(interval.endedAt.toString()) }
    var notes by remember(interval.id) { mutableStateOf(interval.notes) }
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(TimeMath.format(interval.durationMs) + " | " +
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(interval.startedAt)))
        if (editing) {
            Text("Edit Unix timestamps in milliseconds (advanced)")
            OutlinedTextField(start, { start = it }, label = { Text("Start (ms)") })
            OutlinedTextField(end, { end = it }, label = { Text("End (ms)") })
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
            TextButton(onClick = {
                val a = start.toLongOrNull()
                val b = end.toLongOrNull()
                if (a != null && b != null && b >= a) scope.launch {
                    repo.editInterval(interval.id, a, b, notes)
                    editing = false
                }
            }) { Text("Save correction") }
            TextButton(onClick = {
                scope.launch { repo.deleteInterval(interval.id) }
            }) { Text("Delete interval", color = MaterialTheme.colorScheme.error) }
        } else TextButton(onClick = { editing = true }) { Text("Correct interval") }
        HorizontalDivider()
    }
}
