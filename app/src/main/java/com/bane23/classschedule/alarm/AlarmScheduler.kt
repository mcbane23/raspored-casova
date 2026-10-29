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
import com.bane23.classschedule.data.nextMorningReminder
import com.bane23.classschedule.data.nextPackReminder
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules four alarms per child: "classes are over" for the parent, the evening "pack the bag" reminder, the next
 * extracurricular activity reminder and the morning "check today's plan" reminder before the first class. Alarms
 * survive the app being closed.
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
    const val ACTION_MORNING = "com.bane23.classschedule.MORNING"

    /** Epoch millis an activity alarm was set for, so the receiver knows which reminders it stands for. */
    const val EXTRA_AT = "at"

    fun scheduleAll(context: Context, data: AppData) {
        // One child's broken data must not leave the other children without their reminders.
        data.children.forEach { child -> runCatching { schedule(context, child) } }
    }

    /** Cancels every alarm of a deleted child, so none of them fires or shows as the phone's next alarm. */
    fun cancel(context: Context, childId: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        listOf(ACTION_CLASSES_OVER, ACTION_PACK_BAG, ACTION_ACTIVITY, ACTION_MORNING).forEach { alarmManager.cancel(pendingIntent(context, it, childId)) }
    }

    /**
     * Sets the child's alarms to their next times, or only the alarm for [only] (one of the ACTION_ constants). An alarm
     * that just fired passes its own action: the child's other alarms are left as they are, since one of them may be
     * due already but not delivered yet (inexact alarms can come late), and setting it again from now would skip it.
     */
    fun schedule(context: Context, child: Child, only: String? = null) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val now = LocalDateTime.now()
        fun wanted(action: String) = only == null || only == action
        if (wanted(ACTION_CLASSES_OVER)) {
            set(context, alarmManager, child.id, pendingIntent(context, ACTION_CLASSES_OVER, child.id), child.nextAlarm(now))
        }
        if (wanted(ACTION_PACK_BAG)) {
            set(context, alarmManager, child.id, pendingIntent(context, ACTION_PACK_BAG, child.id), child.nextPackReminder(now))
        }
        if (wanted(ACTION_ACTIVITY)) {
            val activity = child.nextActivityReminder(now)
            set(context, alarmManager, child.id, pendingIntent(context, ACTION_ACTIVITY, child.id, activity?.let(::epochMillis)), activity)
        }
        if (wanted(ACTION_MORNING)) {
            set(context, alarmManager, child.id, pendingIntent(context, ACTION_MORNING, child.id), child.nextMorningReminder(now))
        }
    }

    /**
     * Which of the child's alarms [intent] belongs to: its action, or the "classes are over" alarm for alarms set
     * before they had an action; null for anything else, so every alarm is set again.
     */
    fun actionOf(intent: Intent): String? = when (val action = intent.action) {
        null -> ACTION_CLASSES_OVER
        ACTION_CLASSES_OVER, ACTION_PACK_BAG, ACTION_ACTIVITY, ACTION_MORNING -> action
        else -> null
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
