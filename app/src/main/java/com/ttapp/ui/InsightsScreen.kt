package com.ttapp.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.ttapp.data.*
import com.ttapp.integration.TrackerNotification
import com.ttapp.integration.TrackerWidget
import com.ttapp.domain.TimeMath
import java.time.*
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.launch

/** Split time at midnight in local time, rather than assigning all time to start date. */
fun perDay(intervals: List<TimeIntervalEntity>, zone: ZoneId): Map<LocalDate, Long> {
    val days = sortedMapOf<LocalDate, Long>()
    for (item in intervals) {
        var pos = item.startedAt
        while (pos < item.endedAt) {
            val date = Instant.ofEpochMilli(pos).atZone(zone).toLocalDate()
            val next = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = minOf(next, item.endedAt)
            if (end <= pos) break
            days[date] = (days[date] ?: 0) + (end - pos)
            pos = end
        }
    }
    return days
}

fun csvValue(value: String) = "\"" + value.replace("\"", "\"\"") + "\""

suspend fun exportCsv(context: Context, repo: TrackerRepository, uri: android.net.Uri) {
    val tasks = repo.dao.allTasks().associateBy { it.id }
    val sessions = repo.dao.allSessions().associateBy { it.id }
    val rows = repo.dao.allIntervals()
    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
        writer.write("task_id,task_title,task_description,session_id,start_ms,end_ms,duration_ms,notes\n")
        for (row in rows) {
            val task = tasks[row.taskId]
            val note = row.notes.ifBlank { sessions[row.sessionId]?.notes ?: "" }
            writer.write(listOf(row.taskId.toString(), csvValue(task?.title ?: ""),
                csvValue(task?.description ?: ""), row.sessionId.toString(),
                row.startedAt.toString(), row.endedAt.toString(),
                row.durationMs.toString(), csvValue(note)).joinToString(",") + "\n")
        }
    }
}

@Composable
fun InsightsScreen(repo: TrackerRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var feedback by remember { mutableStateOf("") }
    val intervals by repo.dao.intervalsFlow().collectAsState(initial = emptyList())
    val tasks by repo.dao.tasks().collectAsState(initial = emptyList())
    val projects by repo.dao.projects().collectAsState(initial = emptyList())
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    val daily = remember(intervals, zone) { perDay(intervals, zone) }
    val weekField = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
    val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val weekly = daily.filterKeys { !it.isBefore(weekStart) && !it.isAfter(today) }.values.sum()
    val taskTotals = intervals.groupBy { it.taskId }
        .mapValues { (_, records) -> records.sumOf { it.durationMs } }
    val backup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) scope.launch {
        try { BackupIO.write(context, repo, uri); feedback = "Backup saved" }
        catch (e: Exception) { feedback = "Backup failed: " + e.message }
    } }
    val restore = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingRestore = uri }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            // This callback runs on main; use lifecycle scope for IO instead in production.
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                exportCsv(context, repo, uri)
            }
        }
    }

    if (pendingRestore != null) AlertDialog(
        onDismissRequest = { pendingRestore = null },
        title = { Text("Replace local data?") },
        text = { Text("Restoring this backup permanently replaces existing tasks and history. Export a backup first if needed.") },
        confirmButton = { TextButton(onClick = {
            val uri = pendingRestore ?: return@TextButton
            pendingRestore = null
            scope.launch {
                try {
                    BackupIO.restore(context, repo, uri)
                    TrackerWidget.refresh(context)
                    TrackerNotification.sync(context)
                    feedback = "Backup restored"
                } catch (e: Exception) { feedback = "Restore failed: " + e.message }
            }
        }) { Text("Replace data") } },
        dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } }
    )
    Column {
        TextButton(onClick = onBack) { Text("Back to Home") }
        Text("Insights", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Today", color = Muted)
                Text(TimeMath.format(daily[today] ?: 0),
                    style = MaterialTheme.typography.headlineLarge, color = Cyan)
                Spacer(Modifier.height(12.dp))
                Text("This week", color = Muted)
                Text(TimeMath.format(weekly), style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { launcher.launch("TTApp-sessions.csv") }) {
            Text("Export sessions (CSV)")
        }
        Spacer(Modifier.height(10.dp))
        Row {
            OutlinedButton(onClick = { backup.launch("TTApp-backup.json") }) {
                Text("Backup")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { restore.launch(arrayOf("application/json", "*/*")) }) {
                Text("Restore")
            }
        }
        Text("Restore replaces local data. Running sessions resume paused.",
            style = MaterialTheme.typography.bodySmall, color = Muted)
        if (feedback.isNotBlank()) Text(feedback, color = Cyan)
        Spacer(Modifier.height(10.dp))
        Text("Task totals", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(tasks, key = { it.id }) { task ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text(task.title, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(TimeMath.format(taskTotals[task.id] ?: 0), color = Cyan)
                }
            }
            item {
                Spacer(Modifier.height(15.dp))
                Text("Recent days", style = MaterialTheme.typography.titleMedium)
            }
            items(daily.entries.toList().asReversed().take(14)) { (day, ms) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(day.toString(), modifier = Modifier.weight(1f))
                    Text(TimeMath.format(ms))
                }
            }
        }
    }
}
