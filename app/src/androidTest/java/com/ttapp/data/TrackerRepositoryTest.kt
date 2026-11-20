package com.ttapp.data

import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ttapp.domain.ClockSample
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class TrackerRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var db: TrackerDatabase

    @Before
    fun openDatabase() {
        db = Room.inMemoryDatabaseBuilder(context, TrackerDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    @Test
    fun switchingAndResumingPreserveSeparateTaskTotals() = runBlocking {
        var now = ClockSample(1_700_000_000_000L, 1000, 1)
        val repo = TrackerRepository(db, object : TimeSource {
            override fun now() = now
        })
        fun advance(ms: Long) {
            now = now.copy(wallMs = now.wallMs + ms, elapsedMs = now.elapsedMs + ms)
        }
        val first = repo.create("First")
        val second = repo.create("Second")
        repo.start(first)
        advance(2000)
        repo.start(second)
        advance(3000)
        repo.pause()
        advance(9000)
        repo.start(first)
        advance(4000)
        assertEquals(6000L, repo.checkTime().sessionMs)
        repo.stop()
        assertEquals(6000L, repo.dao.taskMs(first))
        assertEquals(3000L, repo.dao.taskMs(second))
    }

    @Test
    fun backupRestoresPausedSessionAndRecordedTime() = runBlocking {
        var now = ClockSample(1_700_000_000_000L, 1000, 1)
        val repo = TrackerRepository(db, object : TimeSource {
            override fun now() = now
        })
        val task = repo.create("Backup test")
        repo.start(task)
        now = now.copy(wallMs = now.wallMs + 5000, elapsedMs = now.elapsedMs + 5000)
        repo.pause()
        val file = File.createTempFile("tracker", ".json", context.cacheDir)
        try {
            val uri = Uri.fromFile(file)
            BackupIO.write(context, repo, uri)
            repo.stop()
            repo.create("After backup")
            BackupIO.restore(context, repo, uri)
            assertEquals(listOf(task), repo.dao.allTasks().map { it.id })
            assertFalse(requireNotNull(repo.dao.tracking()).running)
            assertEquals(5000L, repo.checkTime().sessionMs)
        } finally {
            file.delete()
        }
    }
}
