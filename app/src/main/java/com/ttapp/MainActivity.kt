package com.ttapp

import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.ttapp.integration.TrackerWidget
import com.ttapp.integration.TrackerNotification
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ttapp.data.*
import com.ttapp.domain.TimeMath
import com.ttapp.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as TTApplication).repository
        setContent { TTTheme { TTApp(repository) } }
    }
}

@Composable
fun TTApp(repo: TrackerRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) scope.launch { TrackerNotification.sync(context) }
    }
    suspend fun syncSystem() {
        TrackerWidget.refresh(context)
        TrackerNotification.sync(context)
    }
    val tasks by repo.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val labels by repo.dao.labels().collectAsStateWithLifecycle(initialValue = emptyList())
    val projects by repo.dao.projects().collectAsStateWithLifecycle(initialValue = emptyList())
    val assignments by repo.dao.taskLabels().collectAsStateWithLifecycle(initialValue = emptyList())
    val tracking by repo.tracking.collectAsStateWithLifecycle(initialValue = null)
    var tab by remember { mutableIntStateOf(0) }
    var newMode by remember { mutableStateOf(false) }
    var insights by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var shownTime by remember { mutableStateOf<String?>(null) }
    var revealCount by remember { mutableIntStateOf(0) }
    var historyId by remember { mutableStateOf<Long?>(null) }
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var sessionStartMs by remember { mutableStateOf<Long?>(null) }
    var homeSnapshot by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    val taskId = if (newMode) null else selectedId ?: tracking?.taskId
    val selected = tasks.firstOrNull { it.task.id == taskId }?.task
    val current = tracking?.taskId == taskId && taskId != null
    LaunchedEffect(tracking?.sessionId) {
        sessionStartMs = tracking?.sessionId?.let { repo.dao.session(it)?.startedAt }
    }
    LaunchedEffect(tab, tracking?.taskId, tracking?.running) {
        homeSnapshot = if (tab == 1 && tracking?.running == true && tracking?.taskId != null) {
            val id = tracking!!.taskId!!
            id to (repo.checkTime().taskMs - repo.dao.taskMs(id)).coerceAtLeast(0)
        } else null
    }

    LaunchedEffect(selected?.id) {
        if (selected != null) {
            title = selected.title
            description = selected.description
        }
    }

    Scaffold(containerColor = Carbon, bottomBar = {
        NavigationBar(containerColor = Graphite) {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0; historyId = null },
                icon = { Icon(Icons.Rounded.Timer, "Stopwatch") }, label = { Text("Stopwatch") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1; historyId = null },
                icon = { Icon(Icons.Rounded.Home, "Home") }, label = { Text("Home") })
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            if (insights) {
                InsightsScreen(repo, onBack = { insights = false })
            } else if (historyId != null) {
                HistoryScreen(repo, historyId!!, onBack = { historyId = null })
            } else if (tab == 0) {
                Text("STOPWATCH", style = MaterialTheme.typography.labelLarge, color = Cyan)
                Spacer(Modifier.height(20.dp))
                Text(if (tracking?.running == true && current) "Tracking quietly"
                     else if (current) "Paused" else "Ready whenever you are",
                     style = MaterialTheme.typography.titleMedium, color = Muted)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(value = title, onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("What are you working on?") })
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = description, onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3,
                    label = { Text("Description (optional)") })
                if (current && tracking?.running == true) {
                    Spacer(Modifier.height(12.dp))
                    Text("Started " + DateFormat.getTimeInstance(DateFormat.SHORT)
                        .format(Date(sessionStartMs ?: tracking!!.startWallMs)),
                        style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (shownTime != null) {
                        Text(shownTime!!, style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Light, color = Cyan)
                    } else {
                        Icon(Icons.Rounded.HourglassEmpty, null, tint = Muted,
                            modifier = Modifier.size(50.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Your time is being saved, not watched.", color = Muted)
                    }
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(onClick = {
                        revealCount++
                        val sequence = revealCount
                        scope.launch {
                            shownTime = TimeMath.format(repo.checkTime().sessionMs)
                            delay(3000)
                            if (revealCount == sequence) shownTime = null
                        }
                    }, enabled = tracking?.taskId != null && current,
                        border = BorderStroke(1.dp, Cyan)) {
                        Icon(Icons.Rounded.Visibility, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check Time")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    if (busy) return@Button
                    busy = true
                    scope.launch {
                        try {
                            val id = if (taskId != null && selected != null) {
                                repo.updateTask(taskId, title, description, selected.projectId)
                                taskId
                            } else repo.create(title, description)
                            selectedId = id
                            newMode = false
                            if (tracking?.running == true && tracking?.taskId == id) repo.pause()
                            else repo.start(id)
                            syncSystem()
                            if (Build.VERSION.SDK_INT >= 33 &&
                                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            shownTime = null
                        } finally { busy = false }
                    }
                }, enabled = title.isNotBlank() && !busy,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp)) {
                    Icon(if (current && tracking?.running == true) Icons.Rounded.Pause
                         else Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (current && tracking?.running == true) "Pause"
                         else if (current) "Resume" else "Start")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    scope.launch {
                        repo.stop()
                        syncSystem()
                        selectedId = null
                        newMode = false
                        title = ""; description = ""; shownTime = null
                    }
                }, enabled = current, modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Rounded.Stop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop session")
                }
                TextButton(onClick = {
                    selectedId = null; newMode = true; title = ""; description = ""; shownTime = null
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("New task", color = Muted)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your activities", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { insights = true }) {
                        Icon(Icons.Rounded.BarChart, "Insights", tint = Cyan)
                    }
                    IconButton(onClick = {
                        selectedId = null; newMode = true; title = ""; description = ""; tab = 0
                    }) { Icon(Icons.Rounded.Add, "New task", tint = Cyan) }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    label = { Text("Search title and description") })
                Spacer(Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tasks.filter { item ->
                        val tagIds = assignments.filter { it.taskId == item.task.id }.map { it.labelId }.toSet()
                        item.task.title.contains(query, ignoreCase = true) ||
                        item.task.description.contains(query, ignoreCase = true) ||
                        projects.any { it.id == item.task.projectId && it.name.contains(query, ignoreCase = true) } ||
                        labels.any { it.id in tagIds && it.name.contains(query, ignoreCase = true) }
                    }, key = { it.task.id }) { item ->
                        Surface(shape = RoundedCornerShape(18.dp), color = Graphite,
                            border = BorderStroke(1.dp, Cyan.copy(alpha = .1f)),
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedId = item.task.id
                                newMode = false
                                tab = 0
                            }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.task.title, fontWeight = FontWeight.SemiBold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (item.task.description.isNotBlank())
                                        Text(item.task.description, color = Muted, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(TimeMath.format(item.totalMs +
                                        if (homeSnapshot?.first == item.task.id) homeSnapshot!!.second else 0),
                                        color = Cyan)
                                    IconButton(onClick = { historyId = item.task.id },
                                        modifier = Modifier.size(40.dp)) {
                                        Icon(Icons.Rounded.History, "Session history", tint = Muted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
