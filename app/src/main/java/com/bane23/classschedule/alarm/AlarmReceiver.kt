package com.bane23.classschedule.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bane23.classschedule.MainActivity
import com.bane23.classschedule.R
import com.bane23.classschedule.data.ActivityReminder
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.ChildRepository
import com.bane23.classschedule.data.activityRemindersDue
import com.bane23.classschedule.data.classesEndOn
import com.bane23.classschedule.data.classesStartOn
import com.bane23.classschedule.ui.activityReminderContent
import com.bane23.classschedule.ui.label
import com.bane23.classschedule.ui.morningReminderContent
import com.bane23.classschedule.ui.packReminderContent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Posts a child's reminders when their alarm fires: "classes are over" for the parent, "pack the bag" for the child,
 * "an activity starts soon" and the morning "check today's plan", then schedules that child's next alarms.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val childId = intent.getStringExtra(AlarmScheduler.EXTRA_CHILD_ID) ?: return
        val child = ChildRepository.get(context).data.value.children.find { it.id == childId } ?: return
        try {
            when (intent.action) {
                AlarmScheduler.ACTION_PACK_BAG -> if (child.packReminderEnabled) showPackReminder(context, child)
                AlarmScheduler.ACTION_ACTIVITY -> showActivityReminders(context, child, intent.getLongExtra(AlarmScheduler.EXTRA_AT, 0))
                AlarmScheduler.ACTION_MORNING -> if (child.morningReminderEnabled) showMorningReminder(context, child)
                else -> if (child.alarmEnabled) showClassesOver(context, child)
            }
        } finally {
            // Even if a reminder could not be shown, the next ones stay scheduled.
            AlarmScheduler.schedule(context, child)
        }
    }

    private fun showClassesOver(context: Context, child: Child) {
        val end = child.classesEndOn(LocalDate.now()) ?: return
        val text = if (child.alarmMinutesBefore == 0) {
            "Časovi su završeni u ${end.label()}."
        } else {
            "Časovi se završavaju u ${end.label()}."
        }
        showNotification(
            context,
            Reminder.CLASSES_OVER,
            child.id,
            child.id.hashCode(),
            child.name,
            text,
            alarmSound = child.alarmSound,
            silent = child.alarmSilent,
        )
    }

    private fun showPackReminder(context: Context, child: Child) {
        val content = packReminderContent(child, LocalDate.now()) ?: return
        showNotification(
            context,
            Reminder.PACK_BAG,
            child.id,
            packNotificationId(child.id),
            content.title,
            content.text,
            alarmSound = child.packReminderSound,
            silent = child.packReminderSilent,
            bigText = content.bigText,
        )
    }

    /** Skipped when the alarm comes so late that the first class has already started; the plan is no use by then. */
    private fun showMorningReminder(context: Context, child: Child) {
        val today = LocalDate.now()
        val start = child.classesStartOn(today) ?: return
        if (!LocalDateTime.now().isBefore(today.atTime(start))) return
        val content = morningReminderContent(child, today) ?: return
        showNotification(
            context,
            Reminder.MORNING,
            child.id,
            morningNotificationId(child.id),
            content.title,
            content.text,
            alarmSound = child.morningReminderSound,
            silent = child.morningReminderSilent,
            bigText = content.bigText,
        )
    }

    /**
     * Posts every activity reminder from the time the alarm was set for ([at], epoch millis) until now, so a late
     * alarm still posts all the reminders it stood for.
     */
    private fun showActivityReminders(context: Context, child: Child, at: Long) {
        val now = LocalDateTime.now()
        val from = if (at > 0) LocalDateTime.ofInstant(Instant.ofEpochMilli(at), ZoneId.systemDefault()) else now.minusMinutes(1)
        child.activityRemindersDue(minOf(from, now), now).forEach { reminder ->
            val content = activityReminderContent(child, reminder, now)
            showNotification(
                context,
                Reminder.ACTIVITY,
                child.id,
                activityNotificationId(reminder),
                content.title,
                content.text,
                alarmSound = child.activityReminderSound,
                silent = child.activityReminderSilent,
            )
        }
    }

    /** Which reminder a notification is for; each has its own notification channels in the phone's settings. */
    enum class Reminder { CLASSES_OVER, PACK_BAG, ACTIVITY, MORNING }

    companion object {
        /**
         * Channels from earlier versions, removed so they no longer show in the phone's settings: the single channel
         * from before custom sounds, the first bell channel, whose sound URI used the resource id (which can change
         * between builds) instead of the resource name, and the channels from before every reminder vibrated
         * (a channel's vibration cannot be changed once it exists, so those got new ids).
         */
        private val OLD_CHANNEL_IDS = listOf(
            "classes_over",
            "classes_over_bell",
            "classes_over_bell_v2",
            "classes_over_alarm",
            "classes_over_silent",
            "pack_bag_bell",
            "pack_bag_alarm",
            "pack_bag_silent",
        )
        private const val BELL_CHANNEL_ID = "classes_over_bell_vibrate"
        private const val ALARM_CHANNEL_ID = "classes_over_alarm_vibrate"
        private const val SILENT_CHANNEL_ID = "classes_over_silent_vibrate"
        private const val PACK_BELL_CHANNEL_ID = "pack_bag_bell_vibrate"
        private const val PACK_ALARM_CHANNEL_ID = "pack_bag_alarm_vibrate"
        private const val PACK_SILENT_CHANNEL_ID = "pack_bag_silent_vibrate"
        private const val ACTIVITY_BELL_CHANNEL_ID = "activity_bell_vibrate"
        private const val ACTIVITY_ALARM_CHANNEL_ID = "activity_alarm_vibrate"
        private const val ACTIVITY_SILENT_CHANNEL_ID = "activity_silent_vibrate"
        private const val MORNING_BELL_CHANNEL_ID = "morning_bell_vibrate"
        private const val MORNING_ALARM_CHANNEL_ID = "morning_alarm_vibrate"
        private const val MORNING_SILENT_CHANNEL_ID = "morning_silent_vibrate"

        /** Off-on pattern in milliseconds (three long buzzes), shared by the channels and the direct vibration. */
        private val VIBRATION_PATTERN = longArrayOf(0, 600, 300, 600, 300, 600)

        /** Packing reminders get their own id so they do not replace a child's "classes are over" notification. */
        fun packNotificationId(childId: String): Int = "$childId:pack".hashCode()

        /** The morning reminder's own id, so it does not replace the child's other notifications. */
        fun morningNotificationId(childId: String): Int = "$childId:morning".hashCode()

        /**
         * One notification per activity session, so two activities starting together both show, and so do two
         * sessions of one activity on a day (when another day's session was moved there).
         */
        fun activityNotificationId(reminder: ActivityReminder): Int =
            "${reminder.activity.id}:${reminder.start}".hashCode()

        /**
         * Posts a notification; returns false when notifications are not allowed, for the app or for the reminder's
         * channel (then nothing vibrates either). With [alarmSound] it uses the
         * phone's alarm tone at alarm volume and repeats it until the notification is opened or dismissed,
         * with [silent] it makes no sound at all, otherwise it plays the app's school-bell chime once.
         * It always vibrates, also with [silent] and when the phone's sound is off (see [vibrateIfMuted]).
         * Tapping it opens the Today tab for the child [childId].
         */
        fun showNotification(
            context: Context,
            reminder: Reminder,
            childId: String,
            id: Int,
            title: String,
            text: String,
            alarmSound: Boolean,
            silent: Boolean,
            bigText: String? = null,
        ): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            ensureChannels(context)
            val open = PendingIntent.getActivity(
                context,
                id,
                MainActivity.openTodayIntent(context, childId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val ringing = alarmSound && !silent
            val channel = when (reminder) {
                Reminder.CLASSES_OVER -> when {
                    silent -> SILENT_CHANNEL_ID
                    alarmSound -> ALARM_CHANNEL_ID
                    else -> BELL_CHANNEL_ID
                }
                Reminder.PACK_BAG -> when {
                    silent -> PACK_SILENT_CHANNEL_ID
                    alarmSound -> PACK_ALARM_CHANNEL_ID
                    else -> PACK_BELL_CHANNEL_ID
                }
                Reminder.ACTIVITY -> when {
                    silent -> ACTIVITY_SILENT_CHANNEL_ID
                    alarmSound -> ACTIVITY_ALARM_CHANNEL_ID
                    else -> ACTIVITY_BELL_CHANNEL_ID
                }
                Reminder.MORNING -> when {
                    silent -> MORNING_SILENT_CHANNEL_ID
                    alarmSound -> MORNING_ALARM_CHANNEL_ID
                    else -> MORNING_BELL_CHANNEL_ID
                }
            }
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled() ||
                manager.getNotificationChannel(channel)?.importance == NotificationManager.IMPORTANCE_NONE
            ) {
                return false
            }
            val builder = NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(if (ringing) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            if (bigText != null) builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            val notification = builder.build()
            if (ringing) notification.flags = notification.flags or Notification.FLAG_INSISTENT
            manager.notify(id, notification)
            vibrateIfMuted(context)
            return true
        }

        /**
         * The channels vibrate with the ringer on sound or vibrate, but Android skips notification vibration when
         * the ringer is set to silent. Then the phone is buzzed directly as an alarm, which silent mode does not
         * block, so the reminder is still noticed on time. Nothing extra happens under Do Not Disturb, so the
         * user's own quiet hours are kept.
         */
        private fun vibrateIfMuted(context: Context) {
            val audio = context.getSystemService(AudioManager::class.java) ?: return
            val notifications = context.getSystemService(NotificationManager::class.java) ?: return
            if (audio.ringerMode != AudioManager.RINGER_MODE_SILENT) return
            if (notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
            if (vibrator == null || !vibrator.hasVibrator()) return
            val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, -1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    effect,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }

        fun ensureChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            OLD_CHANNEL_IDS.forEach { manager.deleteNotificationChannel(it) }

            val bell = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/school_bell")
            val bellAudio = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val alarmTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val alarmAudio = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            fun bellChannel(id: String, name: String, description: String) = manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    this.description = description
                    setSound(bell, bellAudio)
                    enableVibration(true)
                    vibrationPattern = VIBRATION_PATTERN
                },
            )
            fun alarmChannel(id: String, name: String, description: String) = manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    this.description = description
                    setSound(alarmTone, alarmAudio)
                    enableVibration(true)
                    vibrationPattern = VIBRATION_PATTERN
                },
            )
            fun silentChannel(id: String, name: String, description: String) = manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    this.description = description
                    setSound(null, null)
                    enableVibration(true)
                    vibrationPattern = VIBRATION_PATTERN
                },
            )

            bellChannel(BELL_CHANNEL_ID, "Kraj nastave (zvono)", "Obaveštenje sa zvukom školskog zvona kad se završe časovi")
            alarmChannel(ALARM_CHANNEL_ID, "Kraj nastave (alarm)", "Glasan zvuk alarma kad se završe časovi")
            silentChannel(SILENT_CHANNEL_ID, "Kraj nastave (bez zvuka)", "Obaveštenje bez zvuka, samo vibracija, kad se završe časovi")
            bellChannel(PACK_BELL_CHANNEL_ID, "Pakovanje torbe (zvono)", "Podsetnik sa zvukom školskog zvona da se spakuje torba")
            alarmChannel(PACK_ALARM_CHANNEL_ID, "Pakovanje torbe (alarm)", "Podsetnik sa glasnim zvukom alarma da se spakuje torba")
            silentChannel(PACK_SILENT_CHANNEL_ID, "Pakovanje torbe (bez zvuka)", "Podsetnik bez zvuka, samo vibracija, da se spakuje torba")
            bellChannel(ACTIVITY_BELL_CHANNEL_ID, "Aktivnosti (zvono)", "Podsetnik sa zvukom školskog zvona pred vanškolsku aktivnost")
            alarmChannel(ACTIVITY_ALARM_CHANNEL_ID, "Aktivnosti (alarm)", "Podsetnik sa glasnim zvukom alarma pred vanškolsku aktivnost")
            silentChannel(ACTIVITY_SILENT_CHANNEL_ID, "Aktivnosti (bez zvuka)", "Podsetnik bez zvuka, samo vibracija, pred vanškolsku aktivnost")
            bellChannel(MORNING_BELL_CHANNEL_ID, "Jutarnji podsetnik (zvono)", "Podsetnik sa zvukom školskog zvona da se pre škole proveri plan za danas")
            alarmChannel(MORNING_ALARM_CHANNEL_ID, "Jutarnji podsetnik (alarm)", "Podsetnik sa glasnim zvukom alarma da se pre škole proveri plan za danas")
            silentChannel(MORNING_SILENT_CHANNEL_ID, "Jutarnji podsetnik (bez zvuka)", "Podsetnik bez zvuka, samo vibracija, da se pre škole proveri plan za danas")
        }
    }
}
