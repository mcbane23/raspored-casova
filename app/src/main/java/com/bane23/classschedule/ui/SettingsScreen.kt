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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.DEFAULT_CHILD_NAME
import com.bane23.classschedule.data.Period
import com.bane23.classschedule.data.Region
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.ThemeMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.nextAlarm
import com.bane23.classschedule.data.nextPackReminder
import com.bane23.classschedule.data.packNote
import com.bane23.classschedule.data.shiftOn
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
}

/** General settings: the list of children, QR import, theme and the app version. */
@Composable
fun SettingsHomeScreen(
    data: AppData,
    selected: Child,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenChild: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var addingChild by remember { mutableStateOf(false) }
    val scanQr = rememberQrImport(viewModel, replaceable = selected)
    val today = LocalDate.now()

    SettingsPageLayout("Raspored časova", "Podešavanja", onBack, modifier) {
        SettingsGroup("Deca") {
            data.children.forEach { c ->
                val shift = c.shiftOn(today)
                SettingsRow(
                    title = c.name.ifBlank { "Bez imena" },
                    subtitle = listOfNotNull(c.school.ifBlank { null }, "${shift.icon()} ${shift.shortLabel().lowercase()}").joinToString(" · "),
                    leading = { ChildAvatar(c.name, size = 36.dp) },
                    chevron = true,
                    onClick = { onOpenChild(c.id) },
                )
                RowDivider()
            }
            SettingsRow(title = "Dodaj dete", emoji = "➕", accent = true, onClick = { addingChild = true })
            RowDivider()
            SettingsRow(
                title = "Uvezi raspored sa QR koda",
                subtitle = "Sa telefona drugog roditelja ili deteta",
                emoji = "📷",
                onClick = scanQr,
            )
        }
        Hint("Dodirni dete da podesiš smene, zvona, predmete, alarm i kalendar.", Modifier.padding(horizontal = 6.dp))
        SettingsGroup("Izgled") {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tema", style = MaterialTheme.typography.titleSmall)
                PillToggle(ThemeMode.entries, data.theme, { it.label() }, { viewModel.setTheme(it) }, onCard = true)
            }
        }
        SettingsGroup("O aplikaciji") {
            SettingsRow(title = "Verzija", emoji = "ℹ️", value = appVersion(LocalContext.current))
        }
    }

    if (addingChild) {
        TextInputDialog(
            title = "Novo dete",
            label = "Ime",
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

private enum class AlarmTone(val label: String, val hint: String) {
    BELL("🛎️ Zvono", "Obaveštenje sa kratkim zvukom školskog zvona."),
    ALARM("⏰ Alarm", "Zvoni kao budilnik dok ne otvoriš obaveštenje."),
    SILENT("🔕 Bez zvuka", "Samo obaveštenje, bez zvuka."),
}

/** "danas u 12:35" or "utorak, 14. oktobar u 12:35" for the next alarm or reminder. */
private fun nextTimeLabel(next: LocalDateTime, today: LocalDate): String =
    if (next.toLocalDate() == today) "danas u ${next.toLocalTime().label()}"
    else "${next.toLocalDate().label().lowercase()} u ${next.toLocalTime().label()}"

/** "ove nedelje pre podne", "uvek popodne", ... */
private fun shiftSummary(child: Child, today: LocalDate): String {
    val shift = child.shiftOn(today).shortLabel().lowercase()
    return when (child.shiftMode) {
        ShiftMode.WEEKLY -> "ove nedelje $shift"
        ShiftMode.MONTHLY -> "ovog meseca $shift"
        else -> "uvek $shift"
    }
}

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
    var showQr by remember { mutableStateOf(false) }
    val problem = rememberReminderProblem()
    val today = LocalDate.now()
    val now = LocalDateTime.now()
    val shift = child.shiftOn(today)
    val firstBell = child.timetable(shift).periods.firstOrNull()?.start

    SettingsPageLayout("Podešavanja", child.name.ifBlank { "Dete" }, onBack, modifier) {
        SettingsGroup(null) {
            SettingsRow(
                title = "Ime, škola i smena",
                subtitle = listOfNotNull(child.school.ifBlank { null }, "${shift.icon()} ${shiftSummary(child, today)}").joinToString(" · "),
                leading = { ChildAvatar(child.name, size = 36.dp) },
                chevron = true,
                onClick = { onOpen(SettingsPage.Profile(child.id)) },
            )
        }

        SettingsGroup("Raspored") {
            SettingsRow(
                title = "Časovi po danima",
                subtitle = "Upiši ili promeni predmete za svaki dan",
                emoji = "📅",
                chevron = true,
                onClick = onEditTimetable,
            )
            RowDivider()
            SettingsRow(
                title = "Zvona",
                subtitle = firstBell?.let { "${shift.icon()} Prvi čas u ${it.label()} · posebno za svaku smenu" } ?: "Kad počinje i završava svaki čas",
                emoji = "⏰",
                chevron = true,
                onClick = { onOpen(SettingsPage.Bells(child.id)) },
            )
            RowDivider()
            SettingsRow(
                title = "Predmeti i torba",
                subtitle = "${child.subjects.size} predmeta · šta se nosi za koji",
                emoji = "📚",
                chevron = true,
                onClick = { onOpen(SettingsPage.Subjects(child.id)) },
            )
        }

        SettingsGroup("Obaveštenja") {
            val alarm = child.nextAlarm(now)
            val pack = child.nextPackReminder(now)
            val anyOn = child.alarmEnabled || child.packReminderEnabled
            SettingsRow(
                title = "Alarm i podsetnik za torbu",
                subtitle = when {
                    anyOn && problem != null -> "⚠️ ${problem.title}"
                    !anyOn -> "Isključeno · dodirni da uključiš"
                    else -> listOfNotNull(
                        if (!child.alarmEnabled) null else alarm?.let { "Alarm ${nextTimeLabel(it, today)}" } ?: "Alarm uključen",
                        if (!child.packReminderEnabled) null else pack?.let { "torba ${nextTimeLabel(it, today)}" } ?: "podsetnik uključen",
                    ).joinToString(" · ")
                },
                emoji = "🔔",
                chevron = true,
                onClick = { onOpen(SettingsPage.Notifications(child.id)) },
            )
        }

        SettingsGroup("Škola") {
            SettingsRow(
                title = "Školski kalendar",
                subtitle = child.region.label() + if (child.finalGrade) " · 8. razred" else "",
                emoji = "🗓️",
                chevron = true,
                onClick = { onOpen(SettingsPage.SchoolCalendar(child.id)) },
            )
            RowDivider()
            SettingsRow("Podeli raspored", subtitle = "QR kod za drugi telefon", emoji = "📤", chevron = true, onClick = { showQr = true })
        }

        if (data.children.size > 1) {
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Obriši dete ${child.name}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
            }
        }
    }

    if (showQr) QrCodeDialog(child, onDismiss = { showQr = false })
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Obrisati ${child.name}?") },
            text = { Text("Biće obrisani raspored i sva podešavanja za ovo dete.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onBack()
                    viewModel.deleteChild(child.id)
                }) { Text("Obriši") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Odustani") } },
        )
    }
}

/** Name, school and shift rotation. */
@Composable
fun ProfileScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPageLayout(child.name.ifBlank { "Dete" }, "Ime, škola i smena", onBack, modifier) {
        SettingsGroup(null) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NameAndSchoolFields(child, viewModel)
            }
        }
        SettingsGroup("Smene") {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShiftPicker(child, viewModel)
            }
        }
    }
}

/** Text fields for the child's name and school; the default name "Dete" shows as an empty field to type into. */
@Composable
fun NameAndSchoolFields(child: Child, viewModel: AppViewModel) {
    OutlinedTextField(
        value = if (child.name == DEFAULT_CHILD_NAME) "" else child.name,
        onValueChange = { viewModel.renameChild(child.id, it) },
        label = { Text("Ime") },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = child.school,
        onValueChange = { viewModel.setSchool(child.id, it) },
        label = { Text("Škola i odeljenje (npr. OŠ Vuk Karadžić, 3-2)") },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** How the shift rotates and which shift the child is in now. */
@Composable
fun ShiftPicker(child: Child, viewModel: AppViewModel) {
    val current = child.shiftOn(LocalDate.now())
    val fixed = child.shiftMode == ShiftMode.FIXED_MORNING || child.shiftMode == ShiftMode.FIXED_AFTERNOON
    Text("Kako se smenjuje", style = MaterialTheme.typography.titleSmall)
    PillToggle(
        options = listOf(ShiftMode.WEEKLY, ShiftMode.MONTHLY, null),
        selected = if (fixed) null else child.shiftMode,
        label = {
            when (it) {
                ShiftMode.WEEKLY -> "Svake nedelje"
                ShiftMode.MONTHLY -> "Svakog meseca"
                else -> "Uvek ista"
            }
        },
        onSelect = { mode ->
            viewModel.setShiftMode(child.id, mode ?: if (current == Shift.MORNING) ShiftMode.FIXED_MORNING else ShiftMode.FIXED_AFTERNOON)
        },
        onCard = true,
    )
    Text(
        when (child.shiftMode) {
            ShiftMode.WEEKLY -> "Ove nedelje ide:"
            ShiftMode.MONTHLY -> "Ovog meseca ide:"
            else -> "Uvek ide:"
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

/** The parent alarm and the packing reminder, each with its test button, and anything that keeps them from arriving. */
@Composable
fun NotificationsScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val setAlarm = rememberAlarmSwitch(viewModel)
    val setPackReminder = rememberPackReminderSwitch(viewModel)
    val alarmTest = rememberTestNotification(child)
    val packTest = rememberTestNotification(child, pack = true)
    var pickingPackTime by remember { mutableStateOf(false) }
    val batteryRestricted = rememberBatteryRestricted()
    val problem = rememberReminderProblem()
    val context = LocalContext.current
    val today = LocalDate.now()
    val blockedHint = "Obaveštenja su isključena za ovu aplikaciju. Uključi ih u podešavanjima telefona."

    SettingsPageLayout(child.name.ifBlank { "Dete" }, "Obaveštenja", onBack, modifier) {
        val anyOn = child.alarmEnabled || child.packReminderEnabled
        if (anyOn && (problem != null || batteryRestricted)) {
            SettingsGroup("Da stigne na vreme") {
                if (problem != null) {
                    SettingsRow(title = problem.title, subtitle = problem.detail, emoji = "⚠️", chevron = true, onClick = { openFix(context, problem) })
                }
                if (problem != null && batteryRestricted) RowDivider()
                if (batteryRestricted) {
                    SettingsRow(
                        title = "Isključi uštedu baterije",
                        subtitle = "Da obaveštenje stigne i kad je aplikacija zatvorena. Izaberi „Bez ograničenja”.",
                        emoji = "🔋",
                        chevron = true,
                        onClick = { openBatterySettings(context) },
                    )
                }
            }
        }

        SettingsGroup("Alarm za kraj nastave · za roditelja") {
            val next = child.nextAlarm(LocalDateTime.now())
            SettingsRow(
                title = "Obavesti me kad se završe časovi",
                subtitle = when {
                    !child.alarmEnabled -> "Stiže obaveštenje na ovaj telefon"
                    next == null -> "Uključeno"
                    else -> "Sledeće: ${nextTimeLabel(next, today)}"
                },
                emoji = "🔔",
                trailing = { Switch(checked = child.alarmEnabled, onCheckedChange = { setAlarm(child.id, it) }) },
                onClick = { setAlarm(child.id, !child.alarmEnabled) },
            )
            if (child.alarmEnabled) {
                RowDivider()
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Koliko pre kraja poslednjeg časa", style = MaterialTheme.typography.titleSmall)
                    PillToggle(
                        ALARM_OFFSETS,
                        child.alarmMinutesBefore,
                        { if (it == 0) "Na kraju" else "$it min" },
                        { viewModel.setAlarmMinutesBefore(child.id, it) },
                        onCard = true,
                    )
                    Text("Zvuk", style = MaterialTheme.typography.titleSmall)
                    val tone = when {
                        child.alarmSilent -> AlarmTone.SILENT
                        child.alarmSound -> AlarmTone.ALARM
                        else -> AlarmTone.BELL
                    }
                    PillToggle(
                        AlarmTone.entries,
                        tone,
                        { it.label },
                        { viewModel.setAlarmSound(child.id, alarmSound = it == AlarmTone.ALARM, silent = it == AlarmTone.SILENT) },
                        onCard = true,
                    )
                    Hint(tone.hint)
                }
                RowDivider()
                SettingsRow(
                    title = "Pošalji probno obaveštenje",
                    subtitle = if (alarmTest.blocked) blockedHint else "Da čuješ kako zvuči",
                    emoji = "🧪",
                    onClick = alarmTest.send,
                )
            }
        }

        SettingsGroup("Podsetnik za torbu · za dete") {
            val next = child.nextPackReminder(LocalDateTime.now())
            SettingsRow(
                title = "Podseti da se spakuje torba",
                subtitle = when {
                    !child.packReminderEnabled -> "Uveče pred školski dan, sa spiskom „Sutra u torbu”"
                    next == null -> "Uključeno"
                    else -> "Sledeće: ${nextTimeLabel(next, today)}"
                },
                emoji = "🎒",
                trailing = { Switch(checked = child.packReminderEnabled, onCheckedChange = { setPackReminder(child.id, it) }) },
                onClick = { setPackReminder(child.id, !child.packReminderEnabled) },
            )
            if (child.packReminderEnabled) {
                RowDivider()
                SettingsRow(
                    title = "Vreme",
                    emoji = "🕖",
                    value = LocalTime.of(child.packReminderMinute / 60, child.packReminderMinute % 60).label(),
                    chevron = true,
                    onClick = { pickingPackTime = true },
                )
                RowDivider()
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Zvuk", style = MaterialTheme.typography.titleSmall)
                    val tone = when {
                        child.packReminderSilent -> AlarmTone.SILENT
                        child.packReminderSound -> AlarmTone.ALARM
                        else -> AlarmTone.BELL
                    }
                    PillToggle(
                        AlarmTone.entries,
                        tone,
                        { it.label },
                        { viewModel.setPackReminderSound(child.id, alarmSound = it == AlarmTone.ALARM, silent = it == AlarmTone.SILENT) },
                        onCard = true,
                    )
                    Hint(tone.hint)
                }
                RowDivider()
                SettingsRow(
                    title = "Pošalji probni podsetnik",
                    subtitle = if (packTest.blocked) blockedHint else "Da vidiš kako izgleda i zvuči",
                    emoji = "🧪",
                    onClick = packTest.send,
                )
            }
        }
    }

    if (pickingPackTime) {
        TimeInputDialog(
            title = "Vreme podsetnika",
            initialMinute = child.packReminderMinute,
            onDismiss = { pickingPackTime = false },
            onConfirm = {
                viewModel.setPackReminderMinute(child.id, it)
                pickingPackTime = false
            },
        )
    }
}

/** Which official calendar applies (region, 8th grade); the school's own events are added on the Raspust tab. */
@Composable
fun SchoolCalendarScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPageLayout(child.name.ifBlank { "Dete" }, "Školski kalendar", onBack, modifier) {
        SettingsGroup("Region") {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillToggle(Region.entries, child.region, { it.label() }, { viewModel.setRegion(child.id, it) }, onCard = true)
                Hint("Vojvodina ima drugačije datume nekih raspusta.")
            }
        }
        SettingsGroup(null) {
            SettingsRow(
                title = "Osmi razred",
                subtitle = "Ranije završava nastavu",
                emoji = "🎓",
                trailing = { Switch(checked = child.finalGrade, onCheckedChange = { viewModel.setFinalGrade(child.id, it) }) },
                onClick = { viewModel.setFinalGrade(child.id, !child.finalGrade) },
            )
        }
        NoteBanner("🏫", "Dan škole, školsku slavu i izlete dodaješ na kartici Raspust.")
    }
}

/** Which bell time is being edited: [isStart] picks the start or the end of period [index]. */
private data class BellEdit(val index: Int, val isStart: Boolean)

/** Bell times for each shift, with the optional pre-class. */
@Composable
fun BellsScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPageLayout(child.name.ifBlank { "Dete" }, "Zvona", onBack, modifier) {
        BellsEditor(child, viewModel)
    }
}

/** The bells of one shift at a time (switched at the top), placed in the caller's column: pre-class, each period's times and add/remove/reset. */
@Composable
fun BellsEditor(child: Child, viewModel: AppViewModel) {
    var shift by rememberSaveable(child.id) { mutableStateOf(child.shiftOn(LocalDate.now())) }
    var editing by remember { mutableStateOf<BellEdit?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val timetable = child.timetable(shift)
    val periods = timetable.periods
    fun change(updated: Timetable) = viewModel.setTimetable(child.id, shift, updated)

    PillToggle(Shift.entries, shift, { "${it.icon()} ${it.shortLabel()}" }, { shift = it })
    SettingsGroup(null) {
        SettingsRow(
            title = "Pretčas",
            subtitle = "Čas pre 1. časa",
            emoji = "🌅",
            trailing = { Switch(checked = timetable.hasPreClass, onCheckedChange = { change(timetable.withPreClass(it)) }) },
            onClick = { change(timetable.withPreClass(!timetable.hasPreClass)) },
        )
    }
    SettingsGroup(null) {
        periods.forEachIndexed { index, period ->
            if (index > 0) {
                val gap = period.startMinute - periods[index - 1].endMinute
                if (gap >= 15) {
                    Text(
                        "☕ Veliki odmor · $gap min",
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
            PillButton("+ Dodaj čas", Modifier.weight(1f)) {
                val start = (periods.lastOrNull()?.endMinute?.plus(5)) ?: (8 * 60)
                if (start + 45 < 24 * 60) change(timetable.copy(periods = periods + Period(start, start + 45)))
            }
        }
        if (periods.size > 1) {
            PillButton("Ukloni poslednji", Modifier.weight(1f)) { change(timetable.copy(periods = periods.dropLast(1))) }
        }
    }
    PillButton("Vrati uobičajena zvona", Modifier.fillMaxWidth()) { confirmReset = true }
    NoteBanner("💡", "Dodirni vreme da ga promeniš. Zvona su posebna za svako dete i svaku smenu.")

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Vratiti uobičajena zvona?") },
            text = { Text("Vremena za ${shift.shortLabel().lowercase()} biće vraćena na 45 minuta po času, sa velikim odmorom posle 2. časa.") },
            confirmButton = {
                TextButton(onClick = {
                    val reset = timetable.withPreClass(false).copy(periods = Timetable.defaultFor(shift).periods)
                    change(reset.withPreClass(timetable.hasPreClass))
                    confirmReset = false
                }) { Text("Vrati") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Odustani") } },
        )
    }

    editing?.let { edit ->
        val period = periods[edit.index]
        TimeInputDialog(
            title = "${timetable.periodLabel(edit.index)}, ${if (edit.isStart) "početak" else "kraj"}",
            initialMinute = if (edit.isStart) period.startMinute else period.endMinute,
            onDismiss = { editing = null },
            onConfirm = { minute ->
                val duration = period.endMinute - period.startMinute
                val updated = if (edit.isStart) {
                    // Moving the start keeps the class length the same.
                    Period(minute, (minute + duration).coerceAtMost(24 * 60 - 1))
                } else {
                    Period(period.startMinute, minute)
                }
                if (updated.endMinute > updated.startMinute) {
                    change(timetable.copy(periods = periods.toMutableList().also { it[edit.index] = updated }))
                }
                editing = null
            },
        )
    }
}

/** The child's subject list with what to bring for each; the notes fill the "Sutra u torbu" list. */
@Composable
fun SubjectsScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    SettingsPageLayout(child.name.ifBlank { "Dete" }, "Predmeti i torba", onBack, modifier) {
        NoteBanner("🎒", "Ono što upišeš ovde pojavljuje se u listi „Sutra u torbu“.")
        SettingsGroup(null) {
            child.subjects.forEachIndexed { i, subject ->
                if (i > 0) RowDivider()
                val note = child.packNote(subject)
                SettingsRow(
                    title = subject,
                    subtitle = if (note.isBlank()) "Ništa posebno" else "Nosi: $note",
                    leading = { SubjectBadge(subject, size = 36.dp) },
                    chevron = true,
                    onClick = { editing = subject },
                )
            }
            RowDivider()
            SettingsRow(title = "Dodaj predmet", emoji = "➕", accent = true, onClick = { adding = true })
        }
        TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Vrati podrazumevani spisak predmeta")
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Vratiti podrazumevani spisak?") },
            text = { Text("Predmeti koje si dodao biće uklonjeni sa spiska. Raspored i beleške šta se nosi ostaju.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetSubjects(child.id)
                    confirmReset = false
                }) { Text("Vrati") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Odustani") } },
        )
    }

    if (adding) {
        TextInputDialog(
            title = "Novi predmet",
            label = "Naziv predmeta",
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
            title = subject,
            label = "Šta se nosi (npr. patike i majica)",
            initial = child.packNote(subject),
            onDismiss = { editing = null },
            onConfirm = {
                viewModel.setPackNote(child.id, subject, it)
                editing = null
            },
            extraAction = "Obriši predmet sa spiska" to {
                viewModel.deleteSubject(child.id, subject)
                editing = null
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

/** One settings row: an emoji tile (or [leading]), a title with an optional subtitle, and a value, switch or chevron. */
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
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leading != null -> leading()
            emoji != null -> Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 18.sp) }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (value != null) Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        trailing?.invoke()
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
