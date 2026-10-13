package com.bane23.classschedule.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.nextAlarm
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules one "classes are over" alarm per child. Alarms are inexact (a window of a few minutes) so the app needs
 * no exact-alarm permission; that precision is enough to tell a parent school is out.
 */
object AlarmScheduler {
    private const val WINDOW_MILLIS = 3 * 60 * 1000L
    const val EXTRA_CHILD_ID = "childId"

    fun scheduleAll(context: Context, data: AppData) {
        data.children.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, child: Child) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = pendingIntent(context, child.id)
        val next = child.nextAlarm(LocalDateTime.now())
        if (next == null) {
            alarmManager.cancel(intent)
            return
        }
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, WINDOW_MILLIS, intent)
    }

    private fun pendingIntent(context: Context, childId: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction("com.bane23.classschedule.CLASSES_OVER")
            // A distinct data URI keeps one PendingIntent per child.
            .setData(android.net.Uri.parse("classschedule://child/$childId"))
            .putExtra(EXTRA_CHILD_ID, childId)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
