package com.ttapp

import android.app.Application
import androidx.room.Room
import com.ttapp.data.AndroidTimeSource
import com.ttapp.data.TrackerDatabase
import com.ttapp.data.TrackerRepository

class TTApplication : Application() {
    val database: TrackerDatabase by lazy {
        Room.databaseBuilder(applicationContext, TrackerDatabase::class.java, "ttapp.db")
            .build()
    }
    val repository: TrackerRepository by lazy { TrackerRepository(database, AndroidTimeSource(this)) }
}
