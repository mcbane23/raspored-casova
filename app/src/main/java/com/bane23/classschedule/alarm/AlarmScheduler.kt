package com.bane23.classschedule.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.nextAlarm
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules one "classes are over" alarm per child. Alarms survive the app being closed and are allowed to fire in
 * Doze. They are exact where the phone allows it without asking (Android 11 and older, or when the user granted
 * "Alarms & reminders"); otherwise Android may delay them by a few minutes, which is fine for telling a parent
 * school is out.
 */
object AlarmScheduler {
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
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (exactAllowed) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
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
