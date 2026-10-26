package com.bane23.classschedule.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.bane23.classschedule.data.ChildRepository
import com.bane23.classschedule.data.nextWidgetRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Redraws the home-screen widget when the data changes, and wakes it again when it would change by itself: when the
 * day's classes end (it moves on to the next school day) and at midnight. Uses an inexact, non-waking alarm: a few
 * minutes late is fine for a widget, and it needs no permission.
 */
object WidgetUpdater {
    const val ACTION_REFRESH = "com.bane23.classschedule.REFRESH_WIDGET"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Redraws the widget and plans the next refresh. Pass [pending] from a receiver's goAsync() to keep it alive. */
    fun refresh(context: Context, pending: BroadcastReceiver.PendingResult? = null) {
        val app = context.applicationContext
        scope.launch {
            try {
                update(app)
            } finally {
                pending?.finish()
            }
        }
    }

    /** Redraws the widget (if one is placed) and plans the next refresh. */
    suspend fun update(context: Context) {
        if (GlanceAppWidgetManager(context).getGlanceIds(ScheduleWidget::class.java).isEmpty()) return
        ScheduleWidget().updateAll(context)
        scheduleNext(context)
    }

    /** Sets the alarm for the next time the widget changes on its own. */
    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val data = ChildRepository.get(context).data.value
        val child = data.children.find { it.id == data.selectedChildId } ?: data.children.firstOrNull() ?: return
        val at = child.nextWidgetRefresh(LocalDateTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ScheduleWidgetReceiver::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // A plain set() may be delivered up to an hour late on Android 12+; a 10-minute window needs no permission.
        alarmManager.setWindow(AlarmManager.RTC, at, 10 * 60 * 1000L, intent)
    }
}
