package com.ttapp.integration

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ttapp.MainActivity
import com.ttapp.TTApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

object TrackerNotification {
    const val CHANNEL = "tracking_state"
    const val TRACKING_ID = 20
    const val CHECK_ID = 21

    fun channel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL, "Time tracker", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Optional session status without a ticking chronometer"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
    }

    private fun allowed(context: Context) =
        Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun sync(context: Context) = withContext(Dispatchers.IO) {
        channel(context)
        val manager = NotificationManagerCompat.from(context)
        if (!allowed(context)) return@withContext
        val repo = (context.applicationContext as TTApplication).repository
        val state = repo.dao.tracking()
        if (state?.taskId == null) {
            manager.cancel(TRACKING_ID)
            return@withContext
        }
        val title = repo.dao.task(state.taskId)?.title ?: "Time tracking"
        val sessionStart = state.sessionId?.let { repo.dao.session(it)?.startedAt }
        val detail = if (state.running) "Started " +
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(sessionStart ?: state.startWallMs))
        else "Paused"
        val open = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title).setContentText(detail)
            .setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true).setSilent(true)
            .addAction(0, if (state.running) "Pause" else "Resume",
                TrackerWidget.pending(context, TrackerWidget.ACTION_TOGGLE, 1))
            .addAction(0, "Check Time",
                TrackerWidget.pending(context, TrackerWidget.ACTION_CHECK, 3))
            .addAction(0, "Stop",
                TrackerWidget.pending(context, TrackerWidget.ACTION_STOP, 2))
            .build()
        post(manager, TRACKING_ID, notification)
    }

    fun showElapsed(context: Context, duration: String) {
        channel(context)
        if (!allowed(context)) return
        val open = PendingIntent.getActivity(context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val note = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("Elapsed session time")
            .setContentText(duration).setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter(3000).setSilent(true).build()
        post(NotificationManagerCompat.from(context), CHECK_ID, note)
    }

    private fun post(manager: NotificationManagerCompat, id: Int, notification: Notification) {
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // Notification permission can be revoked between the permission check and posting.
        }
    }
}
