package com.ttapp.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.room.withTransaction
import com.ttapp.domain.ClockSample
import com.ttapp.domain.TimeAnchor
import com.ttapp.domain.TimeMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface TimeSource { fun now(): ClockSample }
class AndroidTimeSource(private val context: Context) : TimeSource {
    override fun now(): ClockSample = ClockSample(
        System.currentTimeMillis(), SystemClock.elapsedRealtime(),
        try { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }
        catch (_: Exception) { -1 }
    )
}
data class TimeSnapshot(val sessionMs: Long, val taskMs: Long, val sessionStartMs: Long?)
data class TaskListItem(val task: TaskEntity, val totalMs: Long)

class TrackerRepository(val db: TrackerDatabase, private val clock: TimeSource) {
    val dao = db.dao()
    val tracking: Flow<TrackingEntity?> = dao.trackingFlow()
    val tasks: Flow<List<TaskListItem>> = combine(dao.tasks(), dao.totals()) { tasks, totals ->
        val map = totals.associate { it.taskId to it.totalMs }
        tasks.map { TaskListItem(it, map[it.id] ?: 0) }
    }
    suspend fun create(title: String, description: String = ""): Long {
        require(title.isNotBlank())
        val now = clock.now().wallMs
        return dao.insertTask(TaskEntity(title = title.trim(), description = description.trim(),
            createdAt = now, updatedAt = now))
    }
    suspend fun updateTask(taskId: Long, title: String, description: String, projectId: Long? = null) {
        require(title.isNotBlank())
        db.withTransaction {
            val old = dao.task(taskId) ?: return@withTransaction
            dao.updateTask(old.copy(title = title.trim(), description = description.trim(),
                projectId = projectId, updatedAt = clock.now().wallMs))
        }
    }
    suspend fun start(taskId: Long) {
        db.withTransaction {
            val now = clock.now()
            if (dao.task(taskId) == null) return@withTransaction
            val old = dao.tracking()
            if (old?.running == true && old.taskId == taskId) return@withTransaction
            if (old?.running == true) persistInterval(old, now)
            val session = dao.openSession(taskId)
            val sessionId = session?.id ?: dao.insertSession(
                WorkSessionEntity(taskId = taskId, startedAt = now.wallMs))
            dao.setTracking(TrackingEntity(taskId = taskId, sessionId = sessionId, running = true,
                startWallMs = now.wallMs, startElapsedMs = now.elapsedMs, startBootId = now.bootId))
        }
    }
    suspend fun pause() {
        db.withTransaction {
            val anchor = dao.tracking() ?: return@withTransaction
            if (!anchor.running) return@withTransaction
            persistInterval(anchor, clock.now())
            dao.setTracking(anchor.copy(running = false))
        }
    }
    suspend fun stop() {
        db.withTransaction {
            val anchor = dao.tracking() ?: return@withTransaction
            val now = clock.now()
            if (anchor.running) persistInterval(anchor, now)
            anchor.sessionId?.let { id ->
                dao.session(id)?.let { dao.updateSession(it.copy(endedAt = now.wallMs)) }
            }
            dao.setTracking(TrackingEntity())
        }
    }
    suspend fun checkTime(): TimeSnapshot = db.withTransaction {
        val anchor = dao.tracking() ?: return@withTransaction TimeSnapshot(0, 0, null)
        val live = if (anchor.running) elapsed(anchor, clock.now()) else 0L
        val sessionId = anchor.sessionId ?: return@withTransaction TimeSnapshot(0, 0, null)
        val taskId = anchor.taskId ?: return@withTransaction TimeSnapshot(0, 0, null)
        TimeSnapshot(dao.sessionMs(sessionId) + live, dao.taskMs(taskId) + live,
            dao.session(sessionId)?.startedAt)
    }
    private fun elapsed(anchor: TrackingEntity, now: ClockSample): Long =
        TimeMath.elapsedMs(TimeAnchor(anchor.startWallMs, anchor.startElapsedMs, anchor.startBootId), now)

    private suspend fun persistInterval(anchor: TrackingEntity, now: ClockSample) {
        val taskId = anchor.taskId ?: return
        val sessionId = anchor.sessionId ?: return
        val duration = elapsed(anchor, now)
        if (duration == 0L) return
        // Interval duration is monotonic; wall end is anchored to duration for clock-edit safety.
        val end = anchor.startWallMs + duration
        dao.insertInterval(TimeIntervalEntity(taskId = taskId, sessionId = sessionId,
            startedAt = anchor.startWallMs, endedAt = end, durationMs = duration))
    }
    suspend fun setSessionNotes(sessionId: Long, notes: String) {
        db.withTransaction { dao.session(sessionId)?.let { dao.updateSession(it.copy(notes = notes)) } }
    }
    suspend fun editInterval(id: Long, from: Long, to: Long, notes: String) {
        require(to >= from)
        db.withTransaction { dao.interval(id)?.let {
            dao.updateInterval(it.copy(startedAt = from, endedAt = to,
                durationMs = TimeMath.safeDuration(from, to), notes = notes))
        } }
    }
    suspend fun deleteInterval(id: Long) = db.withTransaction { dao.deleteInterval(id) }
    suspend fun deleteSession(id: Long) = db.withTransaction {
        val current = dao.tracking()
        if (current?.sessionId == id) {
            // No orphaned live anchor after deletion.
            dao.setTracking(TrackingEntity())
        }
        dao.deleteSession(id)
    }
    suspend fun setProject(taskId: Long, projectId: Long?) {
        db.withTransaction { dao.task(taskId)?.let {
            dao.updateTask(it.copy(projectId = projectId, updatedAt = clock.now().wallMs))
        } }
    }
}
