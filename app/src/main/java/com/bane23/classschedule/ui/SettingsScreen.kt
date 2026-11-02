package com.bane23.classschedule.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.alarm.AlarmReceiver
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.isDefaultChildName
import com.bane23.classschedule.data.Extracurricular
import com.bane23.classschedule.data.Period
import com.bane23.classschedule.data.Region
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.ThemeMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.currentShiftDate
import com.bane23.classschedule.data.nextActivityReminder
import com.bane23.classschedule.data.nextAlarm
import com.bane23.classschedule.data.nextMorningReminder
import com.bane23.classschedule.data.nextPackReminder
import com.bane23.classschedule.data.packNote
import com.bane23.classschedule.data.remindsOf
import com.bane23.classschedule.data.shiftOn
import com.bane23.classschedule.data.subjectLabel
import com.bane23.classschedule.data.tr
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Settings pages for parents, opened from the round button at the top right of the main tabs. Each child has a short
 * hub ([ChildPage]) leading to one page per topic, so every option has one obvious place.
 */
sealed interface SettingsPage {
    data object Home : SettingsPage

    /** A page about one child; every page except [Home] is one. */
    sealed interface ForChild : SettingsPage { val childId: String }

    data class ChildPage(override val childId: String) : ForChild
    data class Profile(override val childId: String) : ForChild
    data class Notifications(override val childId: String) : ForChild
    data class SchoolCalendar(override val childId: String) : ForChild
    data class Bells(override val childId: String) : ForChild
    data class Subjects(override val childId: String) : ForChild
    data class Activities(override val childId: String) : ForChild
}

/**
 * General settings: the list of children, QR import, theme and the app version. With only one child its topics come
 * first, so every option is one level less deep; [onOpen] and [onEditTimetable] serve those topics.
 */
@Composable
fun SettingsHomeScreen(
    data: AppData,
    selected: Child,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenChild: (String) -> Unit,
    onOpen: (SettingsPage) -> Unit,
    onEditTimetable: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var addingChild by remember { mutableStateOf(false) }
    val scanQr = rememberQrImport(viewModel, replaceable = selected)
    val today = LocalDate.now()
    val only = data.children.singleOrNull()

    SettingsPageLayout(only?.name?.ifBlank { null } ?: tr("Raspored časova", "Class schedule"), tr("Podešavanja", "Settings"), onBack, modifier) {
        if (only != null) {
            ChildTopics(only, onOpen, onEditTimetable = { onEditTimetable(only.id) })
            SettingsGroup(tr("Deca", "Children")) {
                SettingsRow(title = tr("Dodaj još jedno dete", "Add another child"), emoji = "➕", accent = true, onClick = { addingChild = true })
                RowDivider()
                SettingsRow(
                    title = tr("Uvezi raspored sa QR koda", "Import a timetable from a QR code"),
                    subtitle = tr("Sa telefona drugog roditelja ili deteta", "From the other parent’s or the child’s phone"),
                    emoji = "📷",
                    onClick = scanQr,
                )
            }
        } else {
            SettingsGroup(tr("Deca", "Children")) {
                data.children.forEach { c ->
                    val shift = c.shiftOn(c.currentShiftDate(today))
                    SettingsRow(
                        title = c.name.ifBlank { tr("Bez imena", "No name") },
                        subtitle = listOfNotNull(c.school.ifBlank { null }, "${shift.icon()} ${shift.shortLabel().lowercase()}").joinToString(" · "),
                        leading = { ChildAvatar(c.name, size = 36.dp) },
                        chevron = true,
                        onClick = { onOpenChild(c.id) },
                    )
                    RowDivider()
                }
                SettingsRow(title = tr("Dodaj dete", "Add child"), emoji = "➕", accent = true, onClick = { addingChild = true })
                RowDivider()
                SettingsRow(
                    title = tr("Uvezi raspored sa QR koda", "Import a timetable from a QR code"),
                    subtitle = tr("Sa telefona drugog roditelja ili deteta", "From the other parent’s or the child’s phone"),
                    emoji = "📷",
                    onClick = scanQr,
                )
            }
        }
        if (only == null) {
            Hint(tr("Dodirni dete da podesiš smene, zvona, predmete, alarm i kalendar.", "Tap a child to set up shifts, bells, subjects, the alarm and the calendar."), Modifier.padding(horizontal = 6.dp))
        }
        SettingsGroup(tr("Izgled", "Appearance")) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Tema", "Theme"), style = MaterialTheme.typography.titleSmall)
                PillToggle(ThemeMode.entries, data.theme, { it.label() }, { viewModel.setTheme(it) }, onCard = true)
            }
        }
        LanguageGroup()
        SettingsGroup(tr("O aplikaciji", "About")) {
            SettingsRow(title = tr("Verzija", "Version"), emoji = "ℹ️", value = appVersion(LocalContext.current))
        }
    }

    if (addingChild) {
        TextInputDialog(
            title = tr("Novo dete", "New child"),
            label = tr("Ime", "Name"),
            initial = "",
            onDismiss = { addingChild = false },
            onConfirm = {
                viewModel.addChild(it)
                addingChild = false
            },
        )
    }
}

private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

private val ALARM_OFFSETS = listOf(0, 5, 10, 15)

/** Choices for how long before the first class the morning reminder comes, in minutes. */
private val MORNING_OFFSETS = listOf(30, 45, 60, 90, 120)

private enum class AlarmTone(val emoji: String) {
    BELL("🛎️"),
    ALARM("⏰"),
    SILENT("🔕"),
    ;

    val label: String get() = when (this) {
        BELL -> tr("Zvono", "Bell")
        ALARM -> "Alarm"
        SILENT -> tr("Bez zvuka", "Silent")
    }

    val hint: String get() = when (this) {
        BELL -> tr("Obaveštenje sa kratkim zvukom školskog zvona.", "A notification with a short school bell sound.")
        ALARM -> tr("Zvoni kao budilnik dok ne otvoriš obaveštenje.", "Rings like an alarm clock until you open the notification.")
        SILENT -> tr("Obaveštenje sa vibracijom, bez zvuka.", "A notification that vibrates, without sound.")
    }
}

private fun toneOf(alarmSound: Boolean, silent: Boolean): AlarmTone = when {
    silent -> AlarmTone.SILENT
    alarmSound -> AlarmTone.ALARM
    else -> AlarmTone.BELL
}

/** "Zvuk" with the chosen tone as its value; tapping it opens a short list of the three tones with what each does. */
@Composable
private fun SoundRow(tone: AlarmTone, onSelect: (AlarmTone) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    SettingsRow(title = tr("Zvuk", "Sound"), subtitle = tone.hint, emoji = tone.emoji, value = tone.label, chevron = true, onClick = { picking = true })
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(tr("Zvuk", "Sound")) },
            text = {
                Column {
                    AlarmTone.entries.forEach { option ->
                        SettingsRow(
                            title = "${option.emoji} ${option.label}",
                            subtitle = option.hint,
                            trailing = { RadioButton(selected = option == tone, onClick = null) },
                            onClick = {
                                onSelect(option)
                                picking = false
                            },
                        )
                    }
                }
            },
            confirmButton = { TextButton(colors = accentTextButton(), onClick = { picking = false }) { Text(tr("Zatvori", "Close")) } },
        )
    }
}

/** "danas u 12:35" or "utorak, 14. oktobar u 12:35" ("today at 12:35") for the next alarm or reminder. */
private fun nextTimeLabel(next: LocalDateTime, today: LocalDate): String =
    if (next.toLocalDate() == today) tr("danas u ${next.toLocalTime().label()}", "today at ${next.toLocalTime().label()}")
    else tr("${next.toLocalDate().label().lowercase()} u ${next.toLocalTime().label()}", "${next.toLocalDate().label()} at ${next.toLocalTime().label()}")

/**
 * One child's settings hub: a short list of topics, each with a live summary, so a parent sees at a glance how the
 * child is set up and where to change it. [onEditTimetable] leaves settings for the Week tab in edit mode.
 */
@Composable
fun ChildSettingsScreen(
    data: AppData,
    child: Child,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpen: (SettingsPage) -> Unit,
    onEditTimetable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    SettingsPageLayout(tr("Podešavanja", "Settings"), child.name.ifBlank { tr("Dete", "Child") }, onBack, modifier) {
        ChildTopics(child, onOpen, onEditTimetable)

        if (data.children.size > 1) {
            TextButton(colors = accentTextButton(), onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Obriši dete ${child.name}", "Delete child ${child.name}"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("Obrisati ${child.name}?", "Delete ${child.name}?")) },
            text = { Text(tr("Biće obrisani raspored i sva podešavanja za ovo dete.", "The timetable and all settings for this child will be deleted.")) },
            confirmButton = {
                TextButton(colors = accentTextButton(), onClick = {
                    confirmDelete = false
                    onBack()
                    viewModel.deleteChild(child.id)
                }) { Text(tr("Obriši", "Delete")) }
            },
            dismissButton = { TextButton(colors = accentTextButton(), onClick = { confirmDelete = false }) { Text(tr("Odustani", "Cancel")) } },
        )
    }
}

/**
 * One child's topics, each with a live summary: profile, timetable, bells, subjects, activities, notifications and
 * the school calendar. Shown on the child's hub, or right on the Settings home when there is only one child.
 */
@Composable
private fun ChildTopics(child: Child, onOpen: (SettingsPage) -> Unit, onEditTimetable: () -> Unit) {
    var showQr by remember { mutableStateOf(false) }
    val problem = rememberReminderProblem()
    val today = LocalDate.now()
    val now = LocalDateTime.now()
    // On a weekend a weekly rotation already shows the coming week, as on the shift and bells pages.
    val shift = child.shiftOn(child.currentShiftDate(today))
    // The 1st class, not the pre-class before it.
    val firstBell = child.timetable(shift).let { it.periods.getOrNull(if (it.hasPreClass) 1 else 0) }?.start

    SettingsGroup(null) {
        SettingsRow(
            title = tr("Ime, škola i smena", "Name, school and shift"),
            subtitle = listOfNotNull(child.school.ifBlank { null }, "${shift.icon()} ${shiftSummary(child, today)}").joinToString(" · "),
            leading = { ChildAvatar(child.name, size = 36.dp) },
            chevron = true,
            onClick = { onOpen(SettingsPage.Profile(child.id)) },
        )
    }

    SettingsGroup(tr("Raspored", "Timetable")) {
        SettingsRow(
            title = tr("Časovi po danima", "Classes by day"),
            subtitle = tr("Upiši ili promeni predmete za svaki dan", "Enter or change the subjects for each day"),
            emoji = "📅",
            chevron = true,
            onClick = onEditTimetable,
        )
        RowDivider()
        SettingsRow(
            title = tr("Zvona", "Bells"),
            subtitle = firstBell?.let { "${shift.icon()} " + tr("Prvi čas u ${it.label()} · posebno za svaku smenu", "First class at ${it.label()} · separate for each shift") }
                ?: tr("Kad počinje i završava svaki čas", "When each class starts and ends"),
            emoji = "⏰",
            chevron = true,
            onClick = { onOpen(SettingsPage.Bells(child.id)) },
        )
        RowDivider()
        SettingsRow(
            title = tr("Predmeti i torba", "Subjects and school bag"),
            subtitle = "${subjectsCount(child.subjects.size)} · " + tr("šta se nosi za koji", "what to bring for each"),
            emoji = "📚",
            chevron = true,
            onClick = { onOpen(SettingsPage.Subjects(child.id)) },
        )
        RowDivider()
        SettingsRow(
            title = tr("Vanškolske aktivnosti", "Extracurricular activities"),
            subtitle = when (val count = child.activities.size) {
                0 -> tr("Sport, hor, folklor, izleti… sa podsetnikom", "Sport, choir, dance, trips… with a reminder")
                else -> child.activities.take(3).joinToString(", ") { "${it.kind.emoji()} ${it.title}" } +
                    if (count > 3) tr(" i još ${count - 3}", " and ${count - 3} more") else ""
            },
            emoji = "🏅",
            chevron = true,
            onClick = { onOpen(SettingsPage.Activities(child.id)) },
        )
    }

    SettingsGroup(tr("Obaveštenja", "Notifications")) {
        val alarm = child.nextAlarm(now)
        val pack = child.nextPackReminder(now)
        val morning = child.nextMorningReminder(now)
        val activity = child.nextActivityReminder(now)
        val anyOn = child.hasReminderOn()
        SettingsRow(
            title = tr("Alarm i podsetnici", "Alarm and reminders"),
            subtitle = when {
                anyOn && problem != null -> "⚠️ ${problem.title}"
                !anyOn -> tr("Isključeno · dodirni da uključiš", "Off · tap to turn on")
                else -> listOfNotNull(
                    if (!child.alarmEnabled) null else alarm?.let { "Alarm ${nextTimeLabel(it, today)}" } ?: tr("Alarm uključen", "Alarm on"),
                    if (!child.morningReminderEnabled) null
                    else morning?.let { tr("jutro ${nextTimeLabel(it, today)}", "morning ${nextTimeLabel(it, today)}") } ?: tr("jutarnji podsetnik uključen", "morning reminder on"),
                    if (!child.packReminderEnabled) null
                    else pack?.let { tr("torba ${nextTimeLabel(it, today)}", "bag ${nextTimeLabel(it, today)}") } ?: tr("podsetnik uključen", "reminder on"),
                    activity?.let { tr("aktivnost ${nextTimeLabel(it, today)}", "activity ${nextTimeLabel(it, today)}") },
                ).joinToString(" · ").replaceFirstChar { it.uppercase() }.ifEmpty { tr("Uključeno", "On") }
            },
            emoji = "🔔",
            chevron = true,
            onClick = { onOpen(SettingsPage.Notifications(child.id)) },
        )
    }

    SettingsGroup(tr("Škola", "School")) {
        SettingsRow(
            title = tr("Školski kalendar", "School calendar"),
            subtitle = child.region.label() + if (child.finalGrade) tr(" · 8. razred", " · 8th grade") else "",
            emoji = "🗓️",
            chevron = true,
            onClick = { onOpen(SettingsPage.SchoolCalendar(child.id)) },
        )
        RowDivider()
        SettingsRow(tr("Podeli raspored", "Share timetable"), subtitle = tr("QR kod za drugi telefon", "A QR code for another phone"), emoji = "📤", chevron = true, onClick = { showQr = true })
    }

    if (showQr) QrCodeDialog(child, onDismiss = { showQr = false })
}

/** Name, school and shift rotation. */
@Composable
fun ProfileScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPageLayout(child.name.ifBlank { tr("Dete", "Child") }, tr("Ime, škola i smena", "Name, school and shift"), onBack, modifier) {
        SettingsGroup(null) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NameAndSchoolFields(child, viewModel)
            }
        }
        SettingsGroup(tr("Smene", "Shifts")) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShiftPicker(child, viewModel)
            }
        }
    }
}

/** Text fields for the child's name and school; the default name "Dete" shows as an empty field to type into. */
@Composable
fun NameAndSchoolFields(child: Child, viewModel: AppViewModel) {
    // Kept as typed, so a name that passes through "Dete" while typing (e.g. "Detelina") does not vanish.
    var name by remember(child.id) { mutableStateOf(if (isDefaultChildName(child.name)) "" else child.name) }
    OutlinedTextField(
        value = name,
        onValueChange = {
            name = it
            viewModel.renameChild(child.id, it)
        },
        label = { Text(tr("Ime", "Name")) },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = child.school,
        onValueChange = { viewModel.setSchool(child.id, it) },
        label = { Text(tr("Škola i odeljenje (npr. OŠ Vuk Karadžić, 3-2)", "School and class (e.g. OŠ Vuk Karadžić, 3-2)")) },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** How the shift rotates and which shift the child is in now. */
@Composable
fun ShiftPicker(child: Child, viewModel: AppViewModel) {
    val today = LocalDate.now()
    val current = child.shiftOn(child.currentShiftDate(today))
    val fixed = child.shiftMode == ShiftMode.FIXED_MORNING || child.shiftMode == ShiftMode.FIXED_AFTERNOON
    Text(tr("Kako se smenjuje", "How shifts alternate"), style = MaterialTheme.typography.titleSmall)
    PillToggle(
        options = listOf(ShiftMode.WEEKLY, ShiftMode.MONTHLY, null),
        selected = if (fixed) null else child.shiftMode,
        label = {
            when (it) {
                ShiftMode.WEEKLY -> tr("Svake nedelje", "Every week")
                ShiftMode.MONTHLY -> tr("Svakog meseca", "Every month")
                else -> tr("Uvek ista", "Always the same")
            }
        },
        onSelect = { mode ->
            viewModel.setShiftMode(child.id, mode ?: if (current == Shift.MORNING) ShiftMode.FIXED_MORNING else ShiftMode.FIXED_AFTERNOON)
        },
        onCard = true,
    )
    Text(
        when (child.shiftMode) {
            ShiftMode.WEEKLY -> if (child.currentShiftDate(today) != today) tr("Od ponedeljka ide:", "From Monday:") else tr("Ove nedelje ide:", "This week:")
            ShiftMode.MONTHLY -> tr("Ovog meseca ide:", "This month:")
            else -> tr("Uvek ide:", "Always:")
        },
        style = MaterialTheme.typography.titleSmall,
    )
    PillToggle(
        options = Shift.entries,
        selected = current,
        label = { "${it.icon()} ${it.shortLabel()}" },
        onSelect = {
            if (fixed) viewModel.setShiftMode(child.id, if (it == Shift.MORNING) ShiftMode.FIXED_MORNING else ShiftMode.FIXED_AFTERNOON)
            else viewModel.setCurrentShift(child.id, it)
        },
        onCard = true,
    )
}

/**
 * The parent alarm, the morning reminder, the packing reminder and the activity reminders, each with its test button,
 * and anything that keeps them from arriving.
 */
@Composable
fun NotificationsScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val setAlarm = rememberAlarmSwitch(viewModel)
    val setPackReminder = rememberPackReminderSwitch(viewModel)
    val setMorningReminder = rememberMorningReminderSwitch(viewModel)
    val setActivityReminders = rememberActivityRemindersSwitch(viewModel)
    val saveActivity = rememberActivitySaver(viewModel)
    val alarmTest = rememberTestNotification(child)
    val morningTest = rememberTestNotification(child, AlarmReceiver.Reminder.MORNING)
    val packTest = rememberTestNotification(child, AlarmReceiver.Reminder.PACK_BAG)
    val activityTest = rememberTestNotification(child, AlarmReceiver.Reminder.ACTIVITY)
    var pickingPackTime by remember { mutableStateOf(false) }
    val batteryRestricted = rememberBatteryRestricted()
    val problem = rememberReminderProblem()
    val context = LocalContext.current
    val today = LocalDate.now()
    val blockedHint = tr("Obaveštenja su isključena za ovu aplikaciju. Uključi ih u podešavanjima telefona.", "Notifications are turned off for this app. Turn them on in the phone settings.")

    SettingsPageLayout(child.name.ifBlank { tr("Dete", "Child") }, tr("Obaveštenja", "Notifications"), onBack, modifier) {
        val anyOn = child.hasReminderOn()
        if (anyOn && (problem != null || batteryRestricted)) {
            SettingsGroup(tr("Da stigne na vreme", "To arrive on time")) {
                if (problem != null) {
                    SettingsRow(title = problem.title, subtitle = problem.detail, emoji = "⚠️", chevron = true, onClick = { openFix(context, problem) })
                }
                if (problem != null && batteryRestricted) RowDivider()
                if (batteryRestricted) {
                    SettingsRow(
                        title = tr("Isključi uštedu baterije", "Turn off battery saving"),
                        subtitle = tr("Da obaveštenje stigne i kad je aplikacija zatvorena. Izaberi „Bez ograničenja”.", "So the notification arrives even when the app is closed. Choose “Unrestricted”."),
                        emoji = "🔋",
                        chevron = true,
                        onClick = { openBatterySettings(context) },
                    )
                }
            }
        }

        SettingsGroup(tr("Alarm za kraj nastave · za roditelja", "Alarm when school ends · for the parent")) {
            val next = child.nextAlarm(LocalDateTime.now())
            SettingsRow(
                title = tr("Obavesti me kad se završe časovi", "Notify me when classes are over"),
                subtitle = when {
                    !child.alarmEnabled -> tr("Stiže obaveštenje na ovaj telefon", "A notification comes to this phone")
                    next == null -> tr("Uključeno", "On")
                    else -> tr("Sledeće: ${nextTimeLabel(next, today)}", "Next: ${nextTimeLabel(next, today)}")
                },
                emoji = "🔔",
                checked = child.alarmEnabled,
                onCheckedChange = { setAlarm(child.id, it) },
            )
            if (child.alarmEnabled) {
                RowDivider()
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Koliko pre kraja poslednjeg časa", "How long before the last class ends"), style = MaterialTheme.typography.titleSmall)
                    PillToggle(
                        ALARM_OFFSETS,
                        child.alarmMinutesBefore,
                        { if (it == 0) tr("Na kraju", "At the end") else "$it min" },
                        { viewModel.setAlarmMinutesBefore(child.id, it) },
                        onCard = true,
                    )
                }
                RowDivider()
                SoundRow(toneOf(child.alarmSound, child.alarmSilent)) {
                    viewModel.setAlarmSound(child.id, alarmSound = it == AlarmTone.ALARM, silent = it == AlarmTone.SILENT)
                }
                RowDivider()
                SettingsRow(
                    title = tr("Pošalji probno obaveštenje", "Send a test notification"),
                    subtitle = if (alarmTest.blocked) blockedHint else tr("Da čuješ kako zvuči", "To hear how it sounds"),
                    emoji = "🧪",
                    onClick = alarmTest.send,
                )
            }
        }

        SettingsGroup(tr("Jutarnji podsetnik · pre škole", "Morning reminder · before school")) {
            val next = child.nextMorningReminder(LocalDateTime.now())
            SettingsRow(
                title = tr("Podseti me da proverim plan za danas", "Remind me to check today’s plan"),
                subtitle = when {
                    !child.morningReminderEnabled -> tr("Pre prvog časa, da na vreme upišeš pomeren ili otkazan čas", "Before the first class, so you can enter a moved or cancelled class in time")
                    next == null -> tr("Uključeno", "On")
                    else -> tr("Sledeće: ${nextTimeLabel(next, today)}", "Next: ${nextTimeLabel(next, today)}")
                },
                emoji = "☀️",
                checked = child.morningReminderEnabled,
                onCheckedChange = { setMorningReminder(child.id, it) },
            )
            if (child.morningReminderEnabled) {
                RowDivider()
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Koliko pre prvog časa", "How long before the first class"), style = MaterialTheme.typography.titleSmall)
                    PillToggle(
                        MORNING_OFFSETS,
                        child.morningReminderMinutesBefore,
                        { if (it == 90) "1½ h" else durationLabel(it.toLong()) },
                        { viewModel.setMorningReminderMinutesBefore(child.id, it) },
                        onCard = true,
                    )
                }
                RowDivider()
                SoundRow(toneOf(child.morningReminderSound, child.morningReminderSilent)) {
                    viewModel.setMorningReminderSound(child.id, alarmSound = it == AlarmTone.ALARM, silent = it == AlarmTone.SILENT)
                }
                RowDivider()
                SettingsRow(
                    title = tr("Pošalji probni podsetnik", "Send a test reminder"),
                    subtitle = if (morningTest.blocked) blockedHint else tr("Da vidiš kako izgleda i zvuči", "To see how it looks and sounds"),
                    emoji = "🧪",
                    onClick = morningTest.send,
                )
            }
        }

        SettingsGroup(tr("Podsetnik za torbu · za dete", "School bag reminder · for the child")) {
            val next = child.nextPackReminder(LocalDateTime.now())
            SettingsRow(
                title = tr("Podseti da se spakuje torba", "Remind to pack the school bag"),
                subtitle = when {
                    !child.packReminderEnabled -> tr("Uveče pred školski dan, sa spiskom „Sutra u torbu”", "The evening before a school day, with the “Pack for tomorrow” list")
                    next == null -> tr("Uključeno", "On")
                    else -> tr("Sledeće: ${nextTimeLabel(next, today)}", "Next: ${nextTimeLabel(next, today)}")
                },
                emoji = "🎒",
                checked = child.packReminderEnabled,
                onCheckedChange = { setPackReminder(child.id, it) },
            )
            if (child.packReminderEnabled) {
                RowDivider()
                SettingsRow(
                    title = tr("Vreme", "Time"),
                    emoji = "🕖",
                    value = LocalTime.of(child.packReminderMinute / 60, child.packReminderMinute % 60).label(),
                    chevron = true,
                    onClick = { pickingPackTime = true },
                )
                RowDivider()
                SoundRow(toneOf(child.packReminderSound, child.packReminderSilent)) {
                    viewModel.setPackReminderSound(child.id, alarmSound = it == AlarmTone.ALARM, silent = it == AlarmTone.SILENT)
                }
                RowDivider()
                SettingsRow(
                    title = tr("Pošalji probni podsetnik", "Send a test reminder"),
                    subtitle = if (packTest.blocked) blockedHint else tr("Da vidiš kako izgleda i zvuči", "To see how it looks and sounds"),
                    emoji = "🧪",
                    onClick = packTest.send,
                )
            }
        }

        SettingsGroup(tr("Podsetnici za aktivnosti", "Activity reminders")) {
            if (child.activities.isEmpty()) {
                SettingsRow(
                    title = tr("Još nema aktivnosti", "No activities yet"),
                    subtitle = tr("Kad dodaš aktivnost u „Vanškolske aktivnosti”, ovde možeš da uključiš njen podsetnik", "Once you add an activity under “Extracurricular activities”, you can turn on its reminder here"),
                    emoji = "🏅",
                )
            } else {
                ActivityReminderRows(child, setActivityReminders, saveActivity, activityTest, blockedHint, viewModel)
            }
        }
    }

    if (pickingPackTime) {
        TimeInputDialog(
            title = tr("Vreme podsetnika", "Reminder time"),
            initialMinute = child.packReminderMinute,
            onDismiss = { pickingPackTime = false },
            onConfirm = {
                viewModel.setPackReminderMinute(child.id, it)
                pickingPackTime = false
            },
        )
    }
}

/**
 * The switch for all of a child's activity reminders and, while it is on, one switch per activity. Each activity keeps
 * its lead time while its reminder is off, so turning it back on restores the reminder as it was.
 */
@Composable
private fun ActivityReminderRows(
    child: Child,
    setActivityReminders: (String, Boolean) -> Unit,
    saveActivity: (String, Extracurricular) -> Unit,
    activityTest: TestNotification,
    blockedHint: String,
    viewModel: AppViewModel,
) {
    val next = child.nextActivityReminder(LocalDateTime.now())
    SettingsRow(
        title = tr("Podsetnik pred aktivnost", "Reminder before an activity"),
        subtitle = when {
            !child.activityRemindersEnabled -> tr("Isključeno za sve aktivnosti; svaka čuva svoje podešavanje", "Off for all activities; each keeps its own setting")
            next != null -> tr("Sledeći: ${nextTimeLabel(next, LocalDate.now())}", "Next: ${nextTimeLabel(next, LocalDate.now())}")
            child.activities.none { it.reminderEnabled } -> tr("Uključi podsetnik za aktivnost ispod", "Turn on the reminder for an activity below")
            else -> tr("Uključeno", "On")
        },
        emoji = "🏅",
        checked = child.activityRemindersEnabled,
        onCheckedChange = { setActivityReminders(child.id, it) },
    )
    if (!child.activityRemindersEnabled) return
    child.activities.forEach { activity ->
        RowDivider()
        SettingsRow(
            title = activity.title,
            subtitle = when {
                !activity.reminderEnabled -> tr("Bez podsetnika", "No reminder")
                activity.reminderMinutesBefore == 0 -> tr("Na početku", "At the start")
                else -> tr("${reminderOffsetLabel(activity.reminderMinutesBefore)} početka", "${reminderOffsetLabel(activity.reminderMinutesBefore)} the start")
            },
            leading = { ActivityTile(activity, size = 36.dp) },
            checked = activity.reminderEnabled,
            onCheckedChange = { saveActivity(child.id, activity.copy(reminderEnabled = it)) },
        )
    }
    if (child.activities.any { it.reminderEnabled }) {
        RowDivider()
        SoundRow(toneOf(child.activityReminderSound, child.activityReminderSilent)) {
            viewModel.setActivityReminderSound(child.id, alarmSound = it == AlarmTone.ALARM, silent = it == AlarmTone.SILENT)
        }
        RowDivider()
        SettingsRow(
            title = tr("Pošalji probni podsetnik", "Send a test reminder"),
            subtitle = if (activityTest.blocked) blockedHint else tr("Da vidiš kako izgleda i zvuči", "To see how it looks and sounds"),
            emoji = "🧪",
            onClick = activityTest.send,
        )
    }
}

/** Which official calendar applies (region, 8th grade); the school's own events are added on the Raspust tab. */
@Composable
fun SchoolCalendarScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPageLayout(child.name.ifBlank { tr("Dete", "Child") }, tr("Školski kalendar", "School calendar"), onBack, modifier) {
        SettingsGroup("Region") {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillToggle(Region.entries, child.region, { it.label() }, { viewModel.setRegion(child.id, it) }, onCard = true)
                Hint(tr("Vojvodina ima drugačije datume nekih raspusta.", "Vojvodina has different dates for some breaks."))
            }
        }
        SettingsGroup(null) {
            SettingsRow(
                title = tr("Osmi razred", "8th grade"),
                subtitle = tr("Ranije završava nastavu", "Finishes classes earlier"),
                emoji = "🎓",
                checked = child.finalGrade,
                onCheckedChange = { viewModel.setFinalGrade(child.id, it) },
            )
        }
        NoteBanner("🏫", tr("Dan škole, školsku slavu i izlete dodaješ na kartici Raspust.", "Add the School Day, the school’s slava and trips on the Breaks tab."))
    }
}

/** Which bell time is being edited: [isStart] picks the start or the end of period [index]. */
private data class BellEdit(val index: Int, val isStart: Boolean)

/** Bell times for each shift, with the optional pre-class. */
@Composable
fun BellsScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPageLayout(child.name.ifBlank { tr("Dete", "Child") }, tr("Zvona", "Bells"), onBack, modifier) {
        BellsEditor(child, viewModel)
    }
}

/** The bells of one shift at a time (switched at the top), placed in the caller's column: pre-class, each period's times and add/remove/reset. */
@Composable
fun BellsEditor(child: Child, viewModel: AppViewModel) {
    var shift by rememberSaveable(child.id) { mutableStateOf(child.shiftOn(child.currentShiftDate(LocalDate.now()))) }
    var editing by remember { mutableStateOf<BellEdit?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val offerUndo = LocalUndo.current
    val timetable = child.timetable(shift)
    val periods = timetable.periods
    fun change(updated: Timetable) = viewModel.setTimetable(child.id, shift, updated)

    PillToggle(Shift.entries, shift, { "${it.icon()} ${it.shortLabel()}" }, { shift = it })
    SettingsGroup(null) {
        SettingsRow(
            title = tr("Pretčas", "Pre-class"),
            subtitle = tr("Čas pre 1. časa", "A class before the 1st class"),
            emoji = "🌅",
            checked = timetable.hasPreClass,
            onCheckedChange = { change(timetable.withPreClass(it)) },
        )
    }
    SettingsGroup(null) {
        periods.forEachIndexed { index, period ->
            if (index > 0) {
                val gap = period.startMinute - periods[index - 1].endMinute
                if (gap >= 15) {
                    Text(
                        tr("☕ Veliki odmor · $gap min", "☕ Long break · $gap min"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                } else {
                    RowDivider()
                }
            }
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(timetable.periodLabel(index), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TimeChip(period.start.label()) { editing = BellEdit(index, isStart = true) }
                Text("–", Modifier.padding(horizontal = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TimeChip(period.end.label()) { editing = BellEdit(index, isStart = false) }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (periods.size < 10) {
            PillButton(tr("+ Dodaj čas", "+ Add class"), Modifier.weight(1f)) {
                val start = (periods.lastOrNull()?.endMinute?.plus(5)) ?: (8 * 60)
                if (start + 45 < 24 * 60) change(timetable.copy(periods = periods + Period(start, start + 45)))
            }
        }
        if (periods.size > 1) {
            PillButton(tr("Ukloni poslednji", "Remove last"), Modifier.weight(1f)) {
                val label = timetable.periodLabel(periods.lastIndex)
                change(timetable.withoutLastPeriod())
                offerUndo(tr("Uklonjen je $label", "$label removed")) { change(timetable) }
            }
        }
    }
    PillButton(tr("Vrati uobičajena zvona", "Restore usual bells"), Modifier.fillMaxWidth()) { confirmReset = true }
    NoteBanner(
        "💡",
        tr(
            "Dodirni vreme da ga promeniš. Kad pomeriš početak časa, za isto vreme se pomeraju i časovi posle njega. Zvona su posebna za svako dete i svaku smenu.",
            "Tap a time to change it. Moving the start of a class moves the classes after it by the same amount. Bells are separate for each child and each shift.",
        ),
    )

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(tr("Vratiti uobičajena zvona?", "Restore the usual bells?")) },
            text = { Text(tr("Vremena za ${shift.shortLabel().lowercase()} biće vraćena na 45 minuta po času, sa velikim odmorom posle 2. časa.", "The ${shift.shortLabel().lowercase()} times will go back to 45-minute classes, with a long break after the 2nd class.")) },
            confirmButton = {
                TextButton(colors = accentTextButton(), onClick = {
                    change(timetable.withDefaultBells(shift))
                    confirmReset = false
                }) { Text(tr("Vrati", "Restore")) }
            },
            dismissButton = { TextButton(colors = accentTextButton(), onClick = { confirmReset = false }) { Text(tr("Odustani", "Cancel")) } },
        )
    }

    editing?.let { edit ->
        val period = periods[edit.index]
        TimeInputDialog(
            title = "${timetable.periodLabel(edit.index)}, ${if (edit.isStart) tr("početak", "start") else tr("kraj", "end")}",
            initialMinute = if (edit.isStart) period.startMinute else period.endMinute,
            onDismiss = { editing = null },
            onConfirm = { minute ->
                bellChange(timetable, edit, minute)?.let { change(it) }
                editing = null
            },
            error = { minute ->
                when {
                    bellChange(timetable, edit, minute) != null -> null
                    !edit.isStart && minute <= period.startMinute ->
                        tr("Kraj mora biti posle početka (${period.start.label()}).", "The end must be after the start (${period.start.label()}).")
                    edit.isStart && edit.index > 0 && minute < periods[edit.index - 1].endMinute ->
                        tr("${timetable.periodLabel(edit.index - 1)} se završava u ${periods[edit.index - 1].end.label()}.", "${timetable.periodLabel(edit.index - 1)} ends at ${periods[edit.index - 1].end.label()}.")
                    !edit.isStart && edit.index < periods.lastIndex ->
                        tr("${timetable.periodLabel(edit.index + 1)} počinje u ${periods[edit.index + 1].start.label()}.", "${timetable.periodLabel(edit.index + 1)} starts at ${periods[edit.index + 1].start.label()}.")
                    edit.isStart && edit.index == 0 && timetable.hasPreClass && periods.size > 1 ->
                        tr("Pretčas mora da se završi pre 1. časa (${periods[1].start.label()}).", "The pre-class must end before the 1st class (${periods[1].start.label()}).")
                    else -> tr("Časovi bi prešli u sledeći dan.", "The classes would run into the next day.")
                }
            },
        )
    }
}

/** The timetable with [edit] set to [minute], or null when the bells would overlap. */
private fun bellChange(timetable: Timetable, edit: BellEdit, minute: Int): Timetable? =
    if (edit.isStart) timetable.withPeriodStart(edit.index, minute) else timetable.withPeriodEnd(edit.index, minute)

/** The child's subject list with what to bring for each; the notes fill the "Sutra u torbu" list. */
@Composable
fun SubjectsScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val offerUndo = LocalUndo.current

    SettingsPageLayout(child.name.ifBlank { tr("Dete", "Child") }, tr("Predmeti i torba", "Subjects and school bag"), onBack, modifier) {
        NoteBanner("🎒", tr("Ono što upišeš ovde pojavljuje se u listi „Sutra u torbu“.", "What you enter here shows up in the “Pack for tomorrow” list."))
        SettingsGroup(null) {
            child.subjects.forEachIndexed { i, subject ->
                if (i > 0) RowDivider()
                val note = child.packNote(subject)
                SettingsRow(
                    title = subjectLabel(subject),
                    subtitle = if (note.isBlank()) tr("Ništa posebno", "Nothing special") else tr("Nosi: $note", "Bring: $note"),
                    leading = { SubjectBadge(subject, size = 36.dp) },
                    chevron = true,
                    onClick = { editing = subject },
                )
            }
            RowDivider()
            SettingsRow(title = tr("Dodaj predmet", "Add subject"), emoji = "➕", accent = true, onClick = { adding = true })
        }
        TextButton(colors = accentTextButton(), onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Vrati podrazumevani spisak predmeta", "Restore the default subject list"))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(tr("Vratiti podrazumevani spisak?", "Restore the default list?")) },
            text = { Text(tr("Dodati predmeti biće uklonjeni sa spiska. Raspored i beleške šta se nosi ostaju.", "Added subjects will be removed from the list. The timetable and the notes on what to bring stay.")) },
            confirmButton = {
                TextButton(colors = accentTextButton(), onClick = {
                    viewModel.resetSubjects(child.id)
                    confirmReset = false
                }) { Text(tr("Vrati", "Restore")) }
            },
            dismissButton = { TextButton(colors = accentTextButton(), onClick = { confirmReset = false }) { Text(tr("Odustani", "Cancel")) } },
        )
    }

    if (adding) {
        TextInputDialog(
            title = tr("Novi predmet", "New subject"),
            label = tr("Naziv predmeta", "Subject name"),
            initial = "",
            onDismiss = { adding = false },
            onConfirm = {
                viewModel.addSubject(child.id, it)
                adding = false
            },
        )
    }
    editing?.let { subject ->
        TextInputDialog(
            title = subjectLabel(subject),
            label = tr("Šta se nosi (npr. patike i majica)", "What to bring (e.g. sneakers and a T-shirt)"),
            initial = child.packNote(subject),
            onDismiss = { editing = null },
            onConfirm = {
                viewModel.setPackNote(child.id, subject, it)
                editing = null
            },
            extraAction = tr("Obriši predmet sa spiska", "Remove the subject from the list") to {
                val before = child.subjects
                viewModel.deleteSubject(child.id, subject)
                editing = null
                offerUndo(tr("Predmet „${subjectLabel(subject)}” je obrisan", "Subject “${subjectLabel(subject)}” removed")) { viewModel.setSubjects(child.id, before) }
            },
        )
    }
}

/**
 * The child's extracurricular activities (sport, choir, folklore, trips, school camps): weekly ones first, then the
 * one-offs by date. Each opens the editor; reminders use the same delivery as the other notifications.
 */
@Composable
fun ActivitiesScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val saveActivity = rememberActivitySaver(viewModel)
    var editing by remember { mutableStateOf<Extracurricular?>(null) }
    var adding by remember { mutableStateOf(false) }
    val offerUndo = LocalUndo.current
    val weekly = child.activities.filter { it.weekly }.sortedWith(compareBy({ it.days.minOrNull() }, { it.startMinute }))
    val once = child.activities.filterNot { it.weekly }.sortedWith(compareBy({ it.startEpochDay }, { it.startMinute }))

    SettingsPageLayout(child.name.ifBlank { tr("Dete", "Child") }, tr("Vanškolske aktivnosti", "Extracurricular activities"), onBack, modifier) {
        NoteBanner("🏅", tr("Sport, hor, folklor, izleti, rekreativna nastava… Prikazuju se na Danas i Nedelja, uz podsetnik pre početka.", "Sport, choir, dance, trips, school camps… They show on Today and Week, with a reminder before they start."))
        if (weekly.isNotEmpty()) {
            SettingsGroup(tr("Svake nedelje", "Every week")) {
                weekly.forEachIndexed { i, activity ->
                    if (i > 0) RowDivider()
                    SettingsRow(
                        title = activity.title,
                        subtitle = listOfNotNull(activity.scheduleLabel(), activity.place.ifBlank { null }).joinToString(" · "),
                        leading = { ActivityTile(activity, size = 36.dp) },
                        value = if (child.remindsOf(activity)) "🔔" else null,
                        chevron = true,
                        onClick = { editing = activity },
                    )
                }
            }
        }
        if (once.isNotEmpty()) {
            SettingsGroup(tr("Jednom", "Once")) {
                once.forEachIndexed { i, activity ->
                    if (i > 0) RowDivider()
                    SettingsRow(
                        title = activity.title,
                        subtitle = listOfNotNull(
                            activity.scheduleLabel(),
                            activity.place.ifBlank { null },
                            if (activity.noClasses) tr("nema nastave", "no classes") else null,
                        ).joinToString(" · "),
                        leading = { ActivityTile(activity, size = 36.dp) },
                        value = if (child.remindsOf(activity)) "🔔" else null,
                        chevron = true,
                        onClick = { editing = activity },
                    )
                }
            }
        }
        DashedButton(tr("+ Dodaj aktivnost", "+ Add activity"), onClick = { adding = true })
    }

    if (adding || editing != null) {
        val initial = editing
        ActivityEditorSheet(
            child = child,
            initial = initial,
            defaultDate = LocalDate.now(),
            onDismiss = {
                adding = false
                editing = null
            },
            onSave = {
                saveActivity(child.id, it)
                adding = false
                editing = null
            },
            onDelete = initial?.let { activity ->
                {
                    viewModel.deleteActivity(child.id, activity.id)
                    editing = null
                    offerUndo(tr("Aktivnost „${activity.title}” je obrisana", "Activity “${activity.title}” deleted")) { viewModel.saveActivity(child.id, activity) }
                }
            },
        )
    }
}

/** A settings page: back button, a small line over the title, and the scrolling content below. */
@Composable
private fun SettingsPageLayout(
    overline: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            ScreenHeader(title, overline = overline, modifier = Modifier.weight(1f).padding(start = 10.dp))
        }
        content()
    }
}

/** A labelled white card holding settings rows. */
@Composable
fun SettingsGroup(label: String?, content: @Composable ColumnScope.() -> Unit) {
    if (label != null) SectionLabel(label)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * One settings row: an emoji tile (or [leading]), a title with an optional subtitle, and a value, switch or chevron.
 * With [checked] the whole row is one switch, so a screen reader announces it once, with its state.
 */
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    emoji: String? = null,
    leading: (@Composable () -> Unit)? = null,
    value: String? = null,
    chevron: Boolean = false,
    accent: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                when {
                    checked != null && onCheckedChange != null ->
                        Modifier.toggleable(value = checked, role = Role.Switch) {
                            haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                            onCheckedChange(it)
                        }
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    else -> Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leading != null -> leading()
            emoji != null -> Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.size(36.dp).clearAndSetSemantics {},
            ) {
                Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 18.sp) }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (accent) accentText() else MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        trailing?.invoke()
        if (checked != null) Switch(checked = checked, onCheckedChange = null)
        if (chevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Tappable bell time, e.g. "08:45". */
@Composable
private fun TimeChip(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.background) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun PillButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = modifier.heightIn(min = 48.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(PaddingValues(horizontal = 12.dp, vertical = 10.dp))) {
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}
