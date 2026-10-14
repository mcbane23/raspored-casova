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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import com.bane23.classschedule.data.Period
import com.bane23.classschedule.data.Region
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.ThemeMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.nextAlarm
import com.bane23.classschedule.data.packNote
import com.bane23.classschedule.data.shiftOn
import java.time.LocalDate
import java.time.LocalDateTime

/** Settings pages for parents, opened from the round button at the top right of the main tabs. */
sealed interface SettingsPage {
    data object Home : SettingsPage
    data class ChildPage(val childId: String) : SettingsPage
    data class Bells(val childId: String) : SettingsPage
    data class Subjects(val childId: String) : SettingsPage
}

/** General settings: theme, the list of children, QR import and a test notification. */
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
    val test = rememberTestNotification(selected)
    val today = LocalDate.now()

    SettingsPageLayout("Raspored časova", "Podešavanja", onBack, modifier) {
        SettingsGroup("Izgled") {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tema", style = MaterialTheme.typography.titleSmall)
                PillToggle(ThemeMode.entries, data.theme, { it.label() }, { viewModel.setTheme(it) }, onCard = true)
            }
        }
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
        SettingsGroup("Obaveštenja") {
            SettingsRow(
                title = "Pošalji probno obaveštenje",
                subtitle = if (test.blocked) "Obaveštenja su isključena za ovu aplikaciju. Uključi ih u podešavanjima telefona."
                else "Da čuješ kako zvuči alarm za ${selected.name.ifBlank { "dete" }}",
                emoji = "🔔",
                onClick = test.send,
            )
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

/** Everything about one child: name and school, shifts, the parent alarm, the calendar and links to bells and subjects. */
@Composable
fun ChildSettingsScreen(
    data: AppData,
    child: Child,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onOpenBells: () -> Unit,
    onOpenSubjects: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    val setAlarm = rememberAlarmSwitch(viewModel)
    val batteryRestricted = rememberBatteryRestricted()
    val exactAllowed = rememberExactAlarmAllowed()
    val context = LocalContext.current
    val today = LocalDate.now()
    val current = child.shiftOn(today)
    val fixed = child.shiftMode == ShiftMode.FIXED_MORNING || child.shiftMode == ShiftMode.FIXED_AFTERNOON

    SettingsPageLayout("Podešavanja", child.name.ifBlank { "Dete" }, onBack, modifier) {
        SettingsGroup(null) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = child.name,
                    onValueChange = { viewModel.renameChild(child.id, it) },
                    label = { Text("Ime") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = child.school,
                    onValueChange = { viewModel.setSchool(child.id, it) },
                    label = { Text("Škola i odeljenje") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SettingsGroup("Smene") {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    style = MaterialTheme.typography.bodyMedium,
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
        }

        SettingsGroup("Alarm za kraj nastave") {
            val next = if (child.alarmEnabled) child.nextAlarm(LocalDateTime.now()) else null
            SettingsRow(
                title = "Obavesti me kad se završe časovi",
                subtitle = when {
                    !child.alarmEnabled -> "Stiže obaveštenje na ovaj telefon"
                    next == null -> "Uključeno"
                    next.toLocalDate() == today -> "Sledeće: danas u ${next.toLocalTime().label()}"
                    else -> "Sledeće: ${next.toLocalDate().label().lowercase()} u ${next.toLocalTime().label()}"
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
                if (!exactAllowed) {
                    RowDivider()
                    SettingsRow(
                        title = "Alarm može da kasni",
                        subtitle = "Dozvoli „Alarmi i podsetnici” da obaveštenje stigne tačno na vreme.",
                        emoji = "⚠️",
                        chevron = true,
                        onClick = { openExactAlarmSettings(context) },
                    )
                }
                if (batteryRestricted) {
                    RowDivider()
                    SettingsRow(
                        title = "Isključi uštedu baterije",
                        subtitle = "Da alarm stigne i kad je aplikacija zatvorena. Izaberi „Bez ograničenja”.",
                        emoji = "🔋",
                        chevron = true,
                        onClick = { openBatterySettings(context) },
                    )
                }
            }
        }

        SettingsGroup("Raspored") {
            SettingsRow("Zvona", subtitle = "Kad počinje i završava svaki čas", emoji = "⏰", chevron = true, onClick = onOpenBells)
            RowDivider()
            SettingsRow("Predmeti i torba", subtitle = "Spisak predmeta i šta se nosi", emoji = "📚", chevron = true, onClick = onOpenSubjects)
            RowDivider()
            SettingsRow("Podeli raspored", subtitle = "QR kod za drugi telefon", emoji = "📤", chevron = true, onClick = { showQr = true })
        }

        SettingsGroup("Školski kalendar") {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PillToggle(Region.entries, child.region, { it.label() }, { viewModel.setRegion(child.id, it) }, onCard = true)
            }
            RowDivider()
            SettingsRow(
                title = "Osmi razred",
                subtitle = "Ranije završava nastavu",
                emoji = "🎓",
                trailing = { Switch(checked = child.finalGrade, onCheckedChange = { viewModel.setFinalGrade(child.id, it) }) },
                onClick = { viewModel.setFinalGrade(child.id, !child.finalGrade) },
            )
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

/** Which bell time is being edited: [isStart] picks the start or the end of period [index]. */
private data class BellEdit(val index: Int, val isStart: Boolean)

/** Bell times for each shift, with the optional pre-class. */
@Composable
fun BellsScreen(child: Child, viewModel: AppViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var shift by rememberSaveable(child.id) { mutableStateOf(child.shiftOn(LocalDate.now())) }
    var editing by remember { mutableStateOf<BellEdit?>(null) }
    val timetable = child.timetable(shift)
    val periods = timetable.periods
    fun change(updated: Timetable) = viewModel.setTimetable(child.id, shift, updated)

    SettingsPageLayout(child.name.ifBlank { "Dete" }, "Zvona", onBack, modifier) {
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
        PillButton("Vrati uobičajena zvona", Modifier.fillMaxWidth()) {
            val reset = timetable.withPreClass(false).copy(periods = Timetable.defaultFor(shift).periods)
            change(reset.withPreClass(timetable.hasPreClass))
        }
        NoteBanner("💡", "Dodirni vreme da ga promeniš. Zvona su posebna za svako dete i svaku smenu.")
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
        TextButton(onClick = { viewModel.resetSubjects(child.id) }, modifier = Modifier.fillMaxWidth()) {
            Text("Vrati podrazumevani spisak predmeta")
        }
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
private fun SettingsGroup(label: String?, content: @Composable ColumnScope.() -> Unit) {
    if (label != null) SectionLabel(label)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** One settings row: an emoji tile (or [leading]), a title with an optional subtitle, and a value, switch or chevron. */
@Composable
private fun SettingsRow(
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
        if (chevron) Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
