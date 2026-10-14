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
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bane23.classschedule.MainActivity
import com.bane23.classschedule.R
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.ChildRepository
import com.bane23.classschedule.data.classesEndOn
import com.bane23.classschedule.ui.label
import com.bane23.classschedule.ui.packReminderContent
import java.time.LocalDate

/**
 * Posts a child's reminders when their alarm fires: "classes are over" for the parent and "pack the bag" for the
 * child, then schedules that child's next alarms.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val childId = intent.getStringExtra(AlarmScheduler.EXTRA_CHILD_ID) ?: return
        val child = ChildRepository.get(context).data.value.children.find { it.id == childId } ?: return
        when (intent.action) {
            AlarmScheduler.ACTION_PACK_BAG -> if (child.packReminderEnabled) showPackReminder(context, child)
            else -> if (child.alarmEnabled) showClassesOver(context, child)
        }
        AlarmScheduler.schedule(context, child)
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

    /** Which reminder a notification is for; each has its own notification channels in the phone's settings. */
    enum class Reminder { CLASSES_OVER, PACK_BAG }

    companion object {
        /**
         * Channels from earlier versions, removed so they no longer show in the phone's settings: the single channel
         * from before custom sounds, and the first bell channel, whose sound URI used the resource id (which can
         * change between builds) instead of the resource name.
         */
        private val OLD_CHANNEL_IDS = listOf("classes_over", "classes_over_bell")
        private const val BELL_CHANNEL_ID = "classes_over_bell_v2"
        private const val ALARM_CHANNEL_ID = "classes_over_alarm"
        private const val SILENT_CHANNEL_ID = "classes_over_silent"
        private const val PACK_BELL_CHANNEL_ID = "pack_bag_bell"
        private const val PACK_ALARM_CHANNEL_ID = "pack_bag_alarm"
        private const val PACK_SILENT_CHANNEL_ID = "pack_bag_silent"

        /** Packing reminders get their own id so they do not replace a child's "classes are over" notification. */
        fun packNotificationId(childId: String): Int = "$childId:pack".hashCode()

        /**
         * Posts a notification; returns false when notifications are not allowed. With [alarmSound] it uses the
         * phone's alarm tone at alarm volume and repeats it until the notification is opened or dismissed,
         * with [silent] it makes no sound at all, otherwise it plays the app's school-bell chime once.
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
            val pack = reminder == Reminder.PACK_BAG
            val channel = when {
                silent -> if (pack) PACK_SILENT_CHANNEL_ID else SILENT_CHANNEL_ID
                alarmSound -> if (pack) PACK_ALARM_CHANNEL_ID else ALARM_CHANNEL_ID
                else -> if (pack) PACK_BELL_CHANNEL_ID else BELL_CHANNEL_ID
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
            NotificationManagerCompat.from(context).notify(id, notification)
            return true
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
                },
            )
            fun alarmChannel(id: String, name: String, description: String) = manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    this.description = description
                    setSound(alarmTone, alarmAudio)
                    enableVibration(true)
                },
            )
            fun silentChannel(id: String, name: String, description: String) = manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    this.description = description
                    setSound(null, null)
                },
            )

            bellChannel(BELL_CHANNEL_ID, "Kraj nastave (zvono)", "Obaveštenje sa zvukom školskog zvona kad se završe časovi")
            alarmChannel(ALARM_CHANNEL_ID, "Kraj nastave (alarm)", "Glasan zvuk alarma kad se završe časovi")
            silentChannel(SILENT_CHANNEL_ID, "Kraj nastave (bez zvuka)", "Obaveštenje bez zvuka kad se završe časovi")
            bellChannel(PACK_BELL_CHANNEL_ID, "Pakovanje torbe (zvono)", "Podsetnik sa zvukom školskog zvona da se spakuje torba")
            alarmChannel(PACK_ALARM_CHANNEL_ID, "Pakovanje torbe (alarm)", "Podsetnik sa glasnim zvukom alarma da se spakuje torba")
            silentChannel(PACK_SILENT_CHANNEL_ID, "Pakovanje torbe (bez zvuka)", "Podsetnik bez zvuka da se spakuje torba")
        }
    }
}
