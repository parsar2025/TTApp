package com.ttapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String = "#00D9E8"
)

@Entity(tableName = "tasks", foreignKeys = [
    ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.SET_NULL)
], indices = [Index("projectId")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val projectId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "labels")
data class LabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String = "#00D9E8"
)

@Entity(tableName = "task_labels", primaryKeys = ["taskId", "labelId"], foreignKeys = [
    ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = LabelEntity::class, parentColumns = ["id"], childColumns = ["labelId"], onDelete = ForeignKey.CASCADE)
], indices = [Index("labelId")])
data class TaskLabelEntity(val taskId: Long, val labelId: Long)

@Entity(tableName = "sessions", foreignKeys = [
    ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)
], indices = [Index("taskId")])
data class WorkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val startedAt: Long,
    val endedAt: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "intervals", foreignKeys = [
    ForeignKey(entity = WorkSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)
], indices = [Index("sessionId"), Index("taskId")])
data class TimeIntervalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val sessionId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationMs: Long,
    val notes: String = ""
)

/** A singleton, transactionally persisted; no ticking background process. */
@Entity(tableName = "tracking")
data class TrackingEntity(
    @PrimaryKey val id: Int = 1,
    val taskId: Long? = null,
    val sessionId: Long? = null,
    val running: Boolean = false,
    val startWallMs: Long = 0,
    val startElapsedMs: Long = 0,
    val startBootId: Int = -1
)

data class TaskTotal(val taskId: Long, val totalMs: Long)
data class SessionTotal(val sessionId: Long, val totalMs: Long)

@Dao
interface TrackerDao {
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun tasks(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    suspend fun allTasks(): List<TaskEntity>
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun task(id: Long): TaskEntity?
    @Insert suspend fun insertTask(item: TaskEntity): Long
    @Update suspend fun updateTask(item: TaskEntity)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM tracking WHERE id = 1")
    fun trackingFlow(): Flow<TrackingEntity?>
    @Query("SELECT * FROM tracking WHERE id = 1")
    suspend fun tracking(): TrackingEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun setTracking(item: TrackingEntity)

    @Insert suspend fun insertSession(item: WorkSessionEntity): Long
    @Update suspend fun updateSession(item: WorkSessionEntity)
    @Query("SELECT * FROM sessions WHERE id = :id") suspend fun session(id: Long): WorkSessionEntity?
    @Query("SELECT * FROM sessions WHERE taskId = :taskId AND endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun openSession(taskId: Long): WorkSessionEntity?
    @Query("SELECT * FROM sessions WHERE taskId = :taskId ORDER BY startedAt DESC")
    fun sessions(taskId: Long): Flow<List<WorkSessionEntity>>
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    suspend fun allSessions(): List<WorkSessionEntity>
    @Query("DELETE FROM sessions WHERE id = :id") suspend fun deleteSession(id: Long)

    @Insert suspend fun insertInterval(item: TimeIntervalEntity): Long
    @Update suspend fun updateInterval(item: TimeIntervalEntity)
    @Query("SELECT * FROM intervals WHERE id = :id") suspend fun interval(id: Long): TimeIntervalEntity?
    @Query("SELECT * FROM intervals WHERE sessionId = :sessionId ORDER BY startedAt DESC")
    fun intervals(sessionId: Long): Flow<List<TimeIntervalEntity>>
    @Query("SELECT * FROM intervals ORDER BY startedAt ASC") suspend fun allIntervals(): List<TimeIntervalEntity>
    @Query("DELETE FROM intervals WHERE id = :id") suspend fun deleteInterval(id: Long)
    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM intervals WHERE taskId = :taskId")
    suspend fun taskMs(taskId: Long): Long
    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM intervals WHERE sessionId = :sessionId")
    suspend fun sessionMs(sessionId: Long): Long
    @Query("SELECT taskId, COALESCE(SUM(durationMs), 0) AS totalMs FROM intervals GROUP BY taskId")
    fun totals(): Flow<List<TaskTotal>>

    @Query("SELECT * FROM projects ORDER BY name")
    fun projects(): Flow<List<ProjectEntity>>
    @Query("SELECT * FROM projects ORDER BY name") suspend fun allProjects(): List<ProjectEntity>
    @Insert suspend fun insertProject(item: ProjectEntity): Long
    @Query("SELECT * FROM labels ORDER BY name")
    fun labels(): Flow<List<LabelEntity>>
    @Query("SELECT * FROM labels ORDER BY name") suspend fun allLabels(): List<LabelEntity>
    @Insert suspend fun insertLabel(item: LabelEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addLabel(item: TaskLabelEntity)
    @Query("SELECT * FROM task_labels") fun taskLabels(): Flow<List<TaskLabelEntity>>
    @Query("SELECT * FROM task_labels") suspend fun allTaskLabels(): List<TaskLabelEntity>
    @Query("DELETE FROM task_labels WHERE taskId = :taskId AND labelId = :labelId")
    suspend fun removeLabel(taskId: Long, labelId: Long)
    @Query("SELECT * FROM projects WHERE id = :id") suspend fun project(id: Long): ProjectEntity?
}

@Database(entities = [
    TaskEntity::class, ProjectEntity::class, LabelEntity::class,
    TaskLabelEntity::class, WorkSessionEntity::class, TimeIntervalEntity::class, TrackingEntity::class
], version = 1, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun dao(): TrackerDao
}
