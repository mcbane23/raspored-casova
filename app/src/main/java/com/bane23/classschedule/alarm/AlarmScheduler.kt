package com.bane23.classschedule.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.bane23.classschedule.MainActivity
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.nextActivityReminder
import com.bane23.classschedule.data.nextAlarm
import com.bane23.classschedule.data.nextPackReminder
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules three alarms per child: "classes are over" for the parent, the evening "pack the bag" reminder and the
 * next extracurricular activity reminder. Alarms survive the app being closed.
 *
 * When exact alarms are allowed (always on Android 11 and older; on Android 12+ once the user grants "Alarms &
 * reminders") they are set with [AlarmManager.setAlarmClock]: they fire at the set minute, wake the phone from Doze
 * and are not batched or deferred by battery savers. Without that permission Android only offers inexact alarms,
 * which Android may deliver up to an hour late, so the settings screen warns and links to the switch.
 */
object AlarmScheduler {
    const val EXTRA_CHILD_ID = "childId"
    const val ACTION_CLASSES_OVER = "com.bane23.classschedule.CLASSES_OVER"
    const val ACTION_PACK_BAG = "com.bane23.classschedule.PACK_BAG"
    const val ACTION_ACTIVITY = "com.bane23.classschedule.ACTIVITY"

    /** Epoch millis an activity alarm was set for, so the receiver knows which reminders it stands for. */
    const val EXTRA_AT = "at"

    fun scheduleAll(context: Context, data: AppData) {
        // One child's broken data must not leave the other children without their reminders.
        data.children.forEach { child -> runCatching { schedule(context, child) } }
    }

    /** Cancels every alarm of a deleted child, so none of them fires or shows as the phone's next alarm. */
    fun cancel(context: Context, childId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        listOf(ACTION_CLASSES_OVER, ACTION_PACK_BAG, ACTION_ACTIVITY).forEach { alarmManager.cancel(pendingIntent(context, it, childId)) }
    }

    fun schedule(context: Context, child: Child) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val now = LocalDateTime.now()
        set(context, alarmManager, child.id, pendingIntent(context, ACTION_CLASSES_OVER, child.id), child.nextAlarm(now))
        set(context, alarmManager, child.id, pendingIntent(context, ACTION_PACK_BAG, child.id), child.nextPackReminder(now))
        val activity = child.nextActivityReminder(now)
        set(context, alarmManager, child.id, pendingIntent(context, ACTION_ACTIVITY, child.id, activity?.let(::epochMillis)), activity)
    }

    /** Sets [intent] to fire at [next], or cancels it when [next] is null. */
    private fun set(context: Context, alarmManager: AlarmManager, childId: String, intent: PendingIntent, next: LocalDateTime?) {
        if (next == null) {
            alarmManager.cancel(intent)
            return
        }
        val triggerAt = epochMillis(next)
        if (canScheduleExact(context)) {
            // Shown by the system for the upcoming alarm (e.g. on the lock screen); opens the child's Today tab in
            // the running app instead of starting a second copy of it.
            val show = PendingIntent.getActivity(
                context,
                childId.hashCode(),
                MainActivity.openTodayIntent(context, childId),
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

    fun epochMillis(time: LocalDateTime): Long = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun pendingIntent(context: Context, action: String, childId: String, at: Long? = null): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(action)
            // A distinct data URI keeps one PendingIntent per child (the action keeps the reminders apart).
            .setData(android.net.Uri.parse("classschedule://child/$childId"))
            .putExtra(EXTRA_CHILD_ID, childId)
        if (at != null) intent.putExtra(EXTRA_AT, at)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
