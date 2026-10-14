package com.bane23.classschedule.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.nextAlarm
import com.bane23.classschedule.data.nextPackReminder
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules two alarms per child: "classes are over" for the parent and the evening "pack the bag" reminder. Alarms
 * survive the app being closed and are allowed to fire in Doze. They are exact where the phone allows it without
 * asking (Android 11 and older, or when the user granted "Alarms & reminders"); otherwise Android may delay them by a
 * few minutes, which is fine for both reminders.
 */
object AlarmScheduler {
    const val EXTRA_CHILD_ID = "childId"
    const val ACTION_CLASSES_OVER = "com.bane23.classschedule.CLASSES_OVER"
    const val ACTION_PACK_BAG = "com.bane23.classschedule.PACK_BAG"

    fun scheduleAll(context: Context, data: AppData) {
        data.children.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, child: Child) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val now = LocalDateTime.now()
        set(alarmManager, pendingIntent(context, ACTION_CLASSES_OVER, child.id), child.nextAlarm(now))
        set(alarmManager, pendingIntent(context, ACTION_PACK_BAG, child.id), child.nextPackReminder(now))
    }

    /** Sets [intent] to fire at [next], or cancels it when [next] is null. */
    private fun set(alarmManager: AlarmManager, intent: PendingIntent, next: LocalDateTime?) {
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

    private fun pendingIntent(context: Context, action: String, childId: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(action)
            // A distinct data URI keeps one PendingIntent per child (the action keeps the two reminders apart).
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
