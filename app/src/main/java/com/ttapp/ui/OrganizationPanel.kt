package com.ttapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ttapp.data.*
import kotlinx.coroutines.launch

/** Optional and retroactive: never interrupts timer start. */
@Composable
fun OrganizationPanel(repo: TrackerRepository, taskId: Long) {
    val scope = rememberCoroutineScope()
    val projects by repo.dao.projects().collectAsState(initial = emptyList())
    val labels by repo.dao.labels().collectAsState(initial = emptyList())
    val assignments by repo.dao.taskLabels().collectAsState(initial = emptyList())
    var projectName by remember { mutableStateOf("") }
    var labelName by remember { mutableStateOf("") }
    var showProjects by remember { mutableStateOf(false) }
    val selectedLabels = assignments.filter { it.taskId == taskId }.map { it.labelId }.toSet()

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    Text("Optional organization", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = { showProjects = !showProjects }) {
        Text("Assign project / category")
    }
    if (showProjects) {
        TextButton(onClick = { scope.launch { repo.setProject(taskId, null) } }) {
            Text("No project")
        }
        for (project in projects) {
            TextButton(onClick = { scope.launch { repo.setProject(taskId, project.id) } }) {
                Text(project.name)
            }
        }
    }
    OutlinedTextField(value = projectName, onValueChange = { projectName = it },
        singleLine = true, label = { Text("New project name") })
    TextButton(enabled = projectName.isNotBlank(), onClick = {
        scope.launch {
            val projectId = repo.dao.insertProject(ProjectEntity(name = projectName.trim()))
            repo.setProject(taskId, projectId)
            projectName = ""
        }
    }) { Text("Create and assign project") }
    Spacer(Modifier.height(8.dp))
    Text("Labels")
    labels.forEach { label ->
        Row(Modifier.fillMaxWidth()) {
            Checkbox(checked = label.id in selectedLabels, onCheckedChange = { checked ->
                scope.launch {
                    if (checked) repo.dao.addLabel(TaskLabelEntity(taskId, label.id))
                    else repo.dao.removeLabel(taskId, label.id)
                }
            })
            Text(label.name, modifier = Modifier.padding(top = 12.dp))
        }
    }
    OutlinedTextField(value = labelName, onValueChange = { labelName = it },
        singleLine = true, label = { Text("New label") })
    TextButton(enabled = labelName.isNotBlank(), onClick = {
        scope.launch {
            val labelId = repo.dao.insertLabel(LabelEntity(name = labelName.trim()))
            repo.dao.addLabel(TaskLabelEntity(taskId, labelId))
            labelName = ""
        }
    }) { Text("Create and apply label") }
}
