package com.ttapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat.is24HourFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ttapp.data.*
import com.ttapp.domain.TimeMath
import com.ttapp.ui.Cyan
import com.ttapp.ui.OrganizationPanel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar
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
            item { OrganizationPanel(repo, taskId) }
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
    var start by remember(interval.startedAt) { mutableLongStateOf(interval.startedAt) }
    var end by remember(interval.endedAt) { mutableLongStateOf(interval.endedAt) }
    var notes by remember(interval.id) { mutableStateOf(interval.notes) }
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(TimeMath.format(interval.durationMs) + " | " +
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(interval.startedAt)))
        if (editing) {
            TimestampPicker("Start", start) { start = it }
            TimestampPicker("End", end) { end = it }
            if (end < start) {
                Text("End must be at or after start", color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") })
            TextButton(enabled = end >= start, onClick = {
                scope.launch {
                    repo.editInterval(interval.id, start, end, notes)
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

@Composable
private fun TimestampPicker(label: String, value: Long, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        val calendar = Calendar.getInstance().apply { timeInMillis = value }
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute)
                onChange(calendar.timeInMillis)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                is24HourFormat(context)).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)).show()
    }) {
        Text("$label · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(value)))
    }
}
