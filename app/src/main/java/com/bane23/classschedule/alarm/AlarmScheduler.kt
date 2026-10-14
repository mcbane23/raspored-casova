package com.bane23.classschedule.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bane23.classschedule.MainActivity
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.nextAlarm
import com.bane23.classschedule.data.nextPackReminder
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules two alarms per child: "classes are over" for the parent and the evening "pack the bag" reminder. Alarms
 * survive the app being closed.
 *
 * When exact alarms are allowed (always on Android 11 and older; on Android 12+ once the user grants "Alarms &
 * reminders") they are set with [AlarmManager.setAlarmClock]: they fire at the set minute, wake the phone from Doze
 * and are not batched or deferred by battery savers. Without that permission Android only offers inexact alarms,
 * which phones (Samsung in particular) delay by 10-15 minutes, so the settings screen warns and links to the switch.
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
        set(context, alarmManager, pendingIntent(context, ACTION_CLASSES_OVER, child.id), child.nextAlarm(now))
        set(context, alarmManager, pendingIntent(context, ACTION_PACK_BAG, child.id), child.nextPackReminder(now))
    }

    /** Sets [intent] to fire at [next], or cancels it when [next] is null. */
    private fun set(context: Context, alarmManager: AlarmManager, intent: PendingIntent, next: LocalDateTime?) {
        if (next == null) {
            alarmManager.cancel(intent)
            return
        }
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (canScheduleExact(context)) {
            // Shown by the system for the upcoming alarm (e.g. on the lock screen); opens the app.
            val show = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            try {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), intent)
                return
            } catch (_: SecurityException) {
                // The permission was revoked between the check and the call; fall through to the inexact alarm.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
    }

    /** Whether alarms can fire at the exact minute; false on Android 12+ until the user allows "Alarms & reminders". */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
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
