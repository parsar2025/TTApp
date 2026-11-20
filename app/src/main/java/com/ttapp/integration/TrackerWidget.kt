package com.ttapp.integration

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.ttapp.MainActivity
import com.ttapp.R
import com.ttapp.TTApplication
import com.ttapp.domain.TimeMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class TrackerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { refresh(context) } finally { pending.finish() }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action !in listOf(ACTION_TOGGLE, ACTION_CHECK, ACTION_STOP)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = (context.applicationContext as TTApplication).repository
                when (intent.action) {
                    ACTION_TOGGLE -> {
                        val state = repo.dao.tracking()
                        if (state?.running == true) repo.pause()
                        else if (state?.taskId != null) repo.start(state.taskId)
                    }
                    ACTION_STOP -> repo.stop()
                    ACTION_CHECK -> TrackerNotification.showElapsed(context,
                        TimeMath.format(repo.checkTime().sessionMs))
                }
                refresh(context)
                TrackerNotification.sync(context)
            } finally { pending.finish() }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.ttapp.TOGGLE"
        const val ACTION_STOP = "com.ttapp.STOP"
        const val ACTION_CHECK = "com.ttapp.CHECK"

        fun pending(context: Context, action: String, requestCode: Int): PendingIntent =
            PendingIntent.getBroadcast(context, requestCode,
                Intent(context, TrackerWidget::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        suspend fun refresh(context: Context) {
            val repo = (context.applicationContext as TTApplication).repository
            val state = repo.dao.tracking()
            val title = state?.taskId?.let { repo.dao.task(it)?.title } ?: "No active task"
            val sessionStart = state?.sessionId?.let { repo.dao.session(it)?.startedAt }
            val started = if (state?.running == true)
                "Started " + DateFormat.getTimeInstance(DateFormat.SHORT)
                    .format(Date(sessionStart ?: state.startWallMs))
            else if (state?.taskId != null) "Paused" else "Ready"
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TrackerWidget::class.java)
            val ids = manager.getAppWidgetIds(component)
            for (id in ids) {
                val view = RemoteViews(context.packageName, R.layout.widget_tracker)
                view.setTextViewText(R.id.widget_title, title)
                view.setTextViewText(R.id.widget_start, started)
                view.setTextViewText(R.id.widget_toggle,
                    if (state?.running == true) "Pause" else "Resume")
                view.setOnClickPendingIntent(R.id.widget_toggle, pending(context, ACTION_TOGGLE, 1))
                view.setOnClickPendingIntent(R.id.widget_stop, pending(context, ACTION_STOP, 2))
                view.setOnClickPendingIntent(R.id.widget_check, pending(context, ACTION_CHECK, 3))
                view.setOnClickPendingIntent(R.id.widget_title, PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                view.setViewVisibility(R.id.widget_controls,
                    if (state?.taskId == null) View.GONE else View.VISIBLE)
                manager.updateAppWidget(id, view)
            }
        }
    }
}
