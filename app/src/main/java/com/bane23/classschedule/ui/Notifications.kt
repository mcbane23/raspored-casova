package com.bane23.classschedule.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.core.content.ContextCompat
import com.bane23.classschedule.alarm.AlarmReceiver
import com.bane23.classschedule.alarm.AlarmScheduler
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.classesEndOn
import java.time.LocalDate

/** Fixed id so repeated test notifications replace each other instead of piling up. */
private const val TEST_NOTIFICATION_ID = 4242

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

/** Turns a child's parent alarm on or off, first asking for the notification permission when it is needed. */
@Composable
fun rememberAlarmSwitch(viewModel: AppViewModel): (childId: String, enabled: Boolean) -> Unit =
    rememberNotificationSwitch(viewModel::setAlarm)

/** Turns a child's packing reminder on or off, first asking for the notification permission when it is needed. */
@Composable
fun rememberPackReminderSwitch(viewModel: AppViewModel): (childId: String, enabled: Boolean) -> Unit =
    rememberNotificationSwitch(viewModel::setPackReminder)

/**
 * Turning a reminder on first asks for the notification permission when it is needed, then opens the "Alarms &
 * reminders" switch if exact alarms are not allowed yet, since without it the reminder comes late.
 */
@Composable
private fun rememberNotificationSwitch(set: (childId: String, enabled: Boolean) -> Unit): (String, Boolean) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.let { set(it, granted) }
        pending = null
        if (granted && !AlarmScheduler.canScheduleExact(context)) openExactAlarmSettings(context)
    }
    return { id, enabled ->
        if (enabled && needsNotificationPermission(context)) {
            pending = id
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            set(id, enabled)
            if (enabled && !AlarmScheduler.canScheduleExact(context)) openExactAlarmSettings(context)
        }
    }
}

/** Whether alarms can fire at the exact minute. Rechecked each time the screen comes back from the phone's settings. */
@Composable
fun rememberExactAlarmAllowed(): Boolean {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(true) }
    LifecycleResumeEffect(Unit) {
        allowed = AlarmScheduler.canScheduleExact(context)
        onPauseOrDispose { }
    }
    return allowed
}

/** Opens this app's "Alarms & reminders" switch (Android 12+). */
fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    try {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
        )
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

/** Sends a test notification for [child]; [blocked] is true when the phone refused to show it. */
class TestNotification(val send: () -> Unit, val blocked: Boolean)

/** A test of the parent's "classes are over" notification, or with [pack] of the child's packing reminder. */
@Composable
fun rememberTestNotification(child: Child, pack: Boolean = false): TestNotification {
    val context = LocalContext.current
    var blocked by remember { mutableStateOf(false) }

    fun show() {
        blocked = if (pack) {
            val content = packReminderContent(child, LocalDate.now())
            !AlarmReceiver.showNotification(
                context,
                AlarmReceiver.Reminder.PACK_BAG,
                child.id,
                TEST_NOTIFICATION_ID,
                content?.title ?: "🎒 Spakuj torbu",
                content?.text ?: "Probni podsetnik radi.",
                alarmSound = child.packReminderSound,
                silent = child.packReminderSilent,
                bigText = content?.bigText,
            )
        } else {
            val end = child.classesEndOn(LocalDate.now())
            val text = if (end != null) "Probno obaveštenje. Danas se časovi završavaju u ${end.label()}." else "Probno obaveštenje radi."
            !AlarmReceiver.showNotification(
                context,
                AlarmReceiver.Reminder.CLASSES_OVER,
                child.id,
                TEST_NOTIFICATION_ID,
                child.name.ifBlank { "Raspored časova" },
                text,
                alarmSound = child.alarmSound,
                silent = child.alarmSilent,
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) show() else blocked = true
    }
    return TestNotification(
        send = { if (needsNotificationPermission(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else show() },
        blocked = blocked,
    )
}

/**
 * Whether battery optimization may hold back the reminders. Rechecked each time the screen comes back, so the
 * warning disappears after the user changes the setting.
 */
@Composable
fun rememberBatteryRestricted(): Boolean {
    val context = LocalContext.current
    var restricted by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        val power = context.getSystemService(PowerManager::class.java)
        restricted = power != null && !power.isIgnoringBatteryOptimizations(context.packageName)
        onPauseOrDispose { }
    }
    return restricted
}

/** Opens the phone's battery optimization list, where the user can set the app to "not optimized". */
fun openBatterySettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
