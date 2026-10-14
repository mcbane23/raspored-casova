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
import com.bane23.classschedule.data.ChildRepository
import com.bane23.classschedule.data.classesEndOn
import com.bane23.classschedule.ui.label
import java.time.LocalDate

/** Posts the "classes are over" notification for one child and schedules that child's next alarm. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val childId = intent.getStringExtra(AlarmScheduler.EXTRA_CHILD_ID) ?: return
        val child = ChildRepository.get(context).data.value.children.find { it.id == childId } ?: return
        if (!child.alarmEnabled) return

        val end = child.classesEndOn(LocalDate.now())
        if (end != null) {
            val text = if (child.alarmMinutesBefore == 0) {
                "Časovi su završeni u ${end.label()}."
            } else {
                "Časovi se završavaju u ${end.label()}."
            }
            showNotification(
                context,
                child.id.hashCode(),
                child.name,
                text,
                alarmSound = child.alarmSound,
                silent = child.alarmSilent,
            )
        }
        AlarmScheduler.schedule(context, child)
    }

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

        /**
         * Posts a notification; returns false when notifications are not allowed. With [alarmSound] it uses the
         * phone's alarm tone at alarm volume and repeats it until the notification is opened or dismissed,
         * with [silent] it makes no sound at all, otherwise it plays the app's school-bell chime once.
         */
        fun showNotification(
            context: Context,
            id: Int,
            title: String,
            text: String,
            alarmSound: Boolean,
            silent: Boolean,
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
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val ringing = alarmSound && !silent
            val channel = when {
                silent -> SILENT_CHANNEL_ID
                alarmSound -> ALARM_CHANNEL_ID
                else -> BELL_CHANNEL_ID
            }
            val notification = NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(if (ringing) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
                .build()
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
            manager.createNotificationChannel(
                NotificationChannel(BELL_CHANNEL_ID, "Kraj nastave (zvono)", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Obaveštenje sa zvukom školskog zvona kad se završe časovi"
                    setSound(bell, bellAudio)
                },
            )

            val alarmTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val alarmAudio = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            manager.createNotificationChannel(
                NotificationChannel(ALARM_CHANNEL_ID, "Kraj nastave (alarm)", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Glasan zvuk alarma kad se završe časovi"
                    setSound(alarmTone, alarmAudio)
                    enableVibration(true)
                },
            )

            manager.createNotificationChannel(
                NotificationChannel(SILENT_CHANNEL_ID, "Kraj nastave (bez zvuka)", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Obaveštenje bez zvuka kad se završe časovi"
                    setSound(null, null)
                },
            )
        }
    }
}
