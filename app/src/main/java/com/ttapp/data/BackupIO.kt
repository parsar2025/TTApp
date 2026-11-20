package com.ttapp.data

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.room.withTransaction
import com.ttapp.domain.ClockSample
import com.ttapp.domain.TimeAnchor
import com.ttapp.domain.TimeMath
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Versioned, local-only, human-readable backup. Restores active sessions as paused. */
object BackupIO {
    suspend fun write(context: Context, repo: TrackerRepository, uri: Uri) =
        withContext(Dispatchers.IO) {
            val root = repo.db.withTransaction {
                val dao = repo.dao
                val snapshot = JSONObject().put("format", "TTApp").put("version", 1)
                val projects = dao.allProjects()
                val labels = dao.allLabels()
                val tasks = dao.allTasks()
                val sessions = dao.allSessions()
                val taskLabels = dao.allTaskLabels()
                val intervals = dao.allIntervals().toMutableList()
                val tracking = dao.tracking()
                if (tracking?.running == true && tracking.taskId != null && tracking.sessionId != null) {
                    val boot = try {
                        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
                    } catch (_: Exception) { -1 }
                    val duration = TimeMath.elapsedMs(
                        TimeAnchor(tracking.startWallMs, tracking.startElapsedMs, tracking.startBootId),
                        ClockSample(System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot)
                    )
                    if (duration > 0) intervals.add(TimeIntervalEntity(
                        taskId = tracking.taskId, sessionId = tracking.sessionId,
                        startedAt = tracking.startWallMs, endedAt = tracking.startWallMs + duration,
                        durationMs = duration
                    ))
                }
                fun array(values: List<JSONObject>): JSONArray =
                    JSONArray().apply { values.forEach { put(it) } }
                snapshot.put("projects", array(projects.map { JSONObject()
                    .put("id", it.id).put("name", it.name).put("color", it.color) }))
                snapshot.put("labels", array(labels.map { JSONObject()
                    .put("id", it.id).put("name", it.name).put("color", it.color) }))
                snapshot.put("tasks", array(tasks.map { JSONObject()
                    .put("id", it.id).put("title", it.title).put("description", it.description)
                    .put("projectId", it.projectId).put("createdAt", it.createdAt)
                    .put("updatedAt", it.updatedAt) }))
                snapshot.put("taskLabels", array(taskLabels.map { JSONObject()
                    .put("taskId", it.taskId).put("labelId", it.labelId) }))
                snapshot.put("sessions", array(sessions.map { JSONObject()
                    .put("id", it.id).put("taskId", it.taskId).put("startedAt", it.startedAt)
                    .put("endedAt", it.endedAt).put("notes", it.notes) }))
                snapshot.put("intervals", array(intervals.map { JSONObject()
                    .put("id", it.id).put("taskId", it.taskId).put("sessionId", it.sessionId)
                    .put("startedAt", it.startedAt).put("endedAt", it.endedAt)
                    .put("durationMs", it.durationMs).put("notes", it.notes) }))
                snapshot.put("pausedTaskId", tracking?.taskId)
                snapshot.put("pausedSessionId", tracking?.sessionId)
                snapshot
            }
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(root.toString(2))
            } ?: error("Could not open backup destination")
        }

    suspend fun restore(context: Context, repo: TrackerRepository, uri: Uri) =
        withContext(Dispatchers.IO) {
            val data = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                it.readText()
            } ?: error("Could not open backup file")
            val root = JSONObject(data)
            require(root.getString("format") == "TTApp" && root.getInt("version") == 1) {
                "Not a supported TTApp backup"
            }
            fun items(key: String): List<JSONObject> {
                val array = root.getJSONArray(key)
                return (0 until array.length()).map { array.getJSONObject(it) }
            }
            fun nullableLong(obj: JSONObject, key: String): Long? =
                if (obj.isNull(key) || !obj.has(key)) null else obj.getLong(key)
            // Parse and validate everything before modifying existing local data.
            val projects = items("projects").map {
                ProjectEntity(it.getLong("id"), it.getString("name"), it.getString("color"))
            }
            val labels = items("labels").map {
                LabelEntity(it.getLong("id"), it.getString("name"), it.getString("color"))
            }
            val tasks = items("tasks").map {
                TaskEntity(it.getLong("id"), it.getString("title"), it.getString("description"),
                    nullableLong(it, "projectId"), it.getLong("createdAt"), it.getLong("updatedAt"))
            }
            val links = items("taskLabels").map {
                TaskLabelEntity(it.getLong("taskId"), it.getLong("labelId"))
            }
            val sessions = items("sessions").map {
                WorkSessionEntity(it.getLong("id"), it.getLong("taskId"), it.getLong("startedAt"),
                    nullableLong(it, "endedAt"), it.getString("notes"))
            }
            val intervals = items("intervals").map {
                TimeIntervalEntity(it.getLong("id"), it.getLong("taskId"), it.getLong("sessionId"),
                    it.getLong("startedAt"), it.getLong("endedAt"), it.getLong("durationMs"),
                    it.getString("notes"))
            }
            val taskIds = tasks.map { it.id }.toSet()
            val sessionsById = sessions.associateBy { it.id }
            require(sessions.all { it.taskId in taskIds })
            require(intervals.all { sessionsById[it.sessionId]?.taskId == it.taskId &&
                it.durationMs >= 0 && it.endedAt >= it.startedAt })
            val pausedTask = nullableLong(root, "pausedTaskId")?.takeIf { it in taskIds }
            val pausedSession = nullableLong(root, "pausedSessionId")
                ?.takeIf { sessionsById[it]?.let { s -> s.taskId == pausedTask && s.endedAt == null } == true }
            repo.db.withTransaction {
                val dao = repo.dao
                dao.setTracking(TrackingEntity())
                dao.clearTaskLabels()
                dao.clearIntervals()
                dao.clearSessions()
                dao.clearTasks()
                dao.clearLabels()
                dao.clearProjects()
                projects.forEach { dao.insertProject(it) }
                labels.forEach { dao.insertLabel(it) }
                tasks.forEach { dao.insertTask(it) }
                links.forEach { dao.addLabel(it) }
                sessions.forEach { dao.insertSession(it) }
                intervals.forEach { dao.insertInterval(it) }
                if (pausedTask != null && pausedSession != null)
                    dao.setTracking(TrackingEntity(taskId = pausedTask, sessionId = pausedSession, running = false))
            }
        }
}
