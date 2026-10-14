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
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules one "classes are over" alarm per child. Alarms survive the app being closed.
 *
 * When exact alarms are allowed (always on Android 11 and older; on Android 12+ once the user grants "Alarms &
 * reminders") the alarm is set with [AlarmManager.setAlarmClock]: it fires at the set minute, wakes the phone from
 * Doze and is not batched or deferred by battery savers. Without that permission Android only offers inexact alarms,
 * which phones (Samsung in particular) delay by 10-15 minutes, so the settings screen warns and links to the switch.
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
        if (canScheduleExact(context)) {
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
