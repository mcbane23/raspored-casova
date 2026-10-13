package com.bane23.classschedule.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Period
import com.bane23.classschedule.data.Region
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.alarm.AlarmReceiver
import com.bane23.classschedule.data.ThemeMode
import com.bane23.classschedule.data.classesEndOn
import com.bane23.classschedule.data.nextAlarm
import com.bane23.classschedule.data.shiftOn
import java.time.LocalDate
import java.time.LocalDateTime

/** Which bell time is being edited: [isStart] picks the start or the end of period [index]. */
private data class BellEdit(val index: Int, val isStart: Boolean)

@Composable
fun SettingsScreen(data: AppData, child: Child, viewModel: AppViewModel, modifier: Modifier = Modifier) {
    var addingChild by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val childName = child.name.ifBlank { "dete" }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Podešavanja", "Sve što važi za: $childName")

        SettingsCard("👨‍👩‍👧", "Deca") {
            data.children.forEach { c ->
                val selected = c.id == child.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        .clickable { viewModel.selectChild(c.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChildAvatar(c.name, size = 40.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(c.name.ifBlank { "Bez imena" }, style = MaterialTheme.typography.titleMedium)
                        if (c.school.isNotBlank()) {
                            Text(c.school, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (selected) {
                        Icon(Icons.Filled.Check, contentDescription = "Izabrano", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            FilledTonalButton(onClick = { addingChild = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Dodaj dete")
            }
        }

        SettingsCard("🎒", "Podaci o detetu") {
            OutlinedTextField(
                value = child.name,
                onValueChange = { viewModel.renameChild(child.id, it) },
                label = { Text("Ime") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = child.school,
                onValueChange = { viewModel.setSchool(child.id, it) },
                label = { Text("Škola i odeljenje") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SettingsCard("🔔", "Roditeljski alarm") { AlarmSettings(child, viewModel) }

        SettingsCard("🔁", "Smene") {
            ShiftMode.entries.forEach { mode ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { viewModel.setShiftMode(child.id, mode) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = child.shiftMode == mode, onClick = { viewModel.setShiftMode(child.id, mode) })
                    Text(mode.label())
                }
            }
            if (child.shiftMode == ShiftMode.WEEKLY || child.shiftMode == ShiftMode.MONTHLY) {
                val current = child.shiftOn(LocalDate.now())
                Hint(if (child.shiftMode == ShiftMode.WEEKLY) "Ove nedelje dete ide:" else "Ovog meseca dete ide:")
                PillToggle(
                    options = Shift.entries,
                    selected = current,
                    label = { (if (it == Shift.MORNING) "☀️ " else "🌙 ") + it.shortLabel() },
                    onSelect = { viewModel.setCurrentShift(child.id, it) },
                )
            }
        }

        SettingsCard("🗓️", "Školski kalendar") {
            PillToggle(
                options = Region.entries,
                selected = child.region,
                label = { it.label() },
                onSelect = { viewModel.setRegion(child.id, it) },
            )
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.setFinalGrade(child.id, !child.finalGrade) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Osmi razred")
                    Hint("Ranije završava nastavu")
                }
                Switch(checked = child.finalGrade, onCheckedChange = { viewModel.setFinalGrade(child.id, it) })
            }
        }

        SettingsCard("⏰", "Zvona (vreme časova)") {
            BellScheduleEditor(child, onChange = { shift, timetable -> viewModel.setTimetable(child.id, shift, timetable) })
        }

        SettingsCard("📚", "Predmeti") { SubjectsEditor(child, viewModel) }

        SettingsCard("📲", "Deljenje rasporeda") { ShareSection(child, viewModel) }

        SettingsCard("🎨", "Izgled") {
            PillToggle(
                options = ThemeMode.entries,
                selected = data.theme,
                label = { it.label() },
                onSelect = { viewModel.setTheme(it) },
            )
        }

        if (data.children.size > 1) {
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Obriši dete ${child.name}", color = MaterialTheme.colorScheme.error)
            }
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
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Obrisati ${child.name}?") },
            text = { Text("Biće obrisani raspored i sva podešavanja za ovo dete.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteChild(child.id)
                    confirmDelete = false
                }) { Text("Obriši") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Odustani") } },
        )
    }
}

@Composable
private fun BellScheduleEditor(child: Child, onChange: (Shift, Timetable) -> Unit) {
    var shift by rememberSaveable(child.id) { mutableStateOf(child.shiftOn(LocalDate.now())) }
    var editing by remember { mutableStateOf<BellEdit?>(null) }
    val timetable = child.timetable(shift)
    val periods = timetable.periods

    PillToggle(
        options = Shift.entries,
        selected = shift,
        label = { (if (it == Shift.MORNING) "☀️ " else "🌙 ") + it.shortLabel() },
        onSelect = { shift = it },
    )
    Row(
        Modifier.fillMaxWidth().clickable { onChange(shift, timetable.withPreClass(!timetable.hasPreClass)) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Pretčas pre 1. časa", Modifier.weight(1f))
        Switch(checked = timetable.hasPreClass, onCheckedChange = { onChange(shift, timetable.withPreClass(it)) })
    }
    periods.forEachIndexed { index, period ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(timetable.periodLabel(index), Modifier.weight(1f))
            TimeChip(period.start.label()) { editing = BellEdit(index, isStart = true) }
            Text("–", Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TimeChip(period.end.label()) { editing = BellEdit(index, isStart = false) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (periods.size < 10) {
            FilledTonalButton(onClick = {
                val start = (periods.lastOrNull()?.endMinute?.plus(5)) ?: (8 * 60)
                if (start + 45 < 24 * 60) onChange(shift, timetable.copy(periods = periods + Period(start, start + 45)))
            }) { Text("Dodaj čas") }
        }
        if (periods.size > 1) {
            OutlinedButton(onClick = { onChange(shift, timetable.copy(periods = periods.dropLast(1))) }) {
                Text("Ukloni poslednji")
            }
        }
    }
    TextButton(onClick = {
        val reset = timetable.withPreClass(false).copy(periods = Timetable.defaultFor(shift).periods)
        onChange(shift, reset.withPreClass(timetable.hasPreClass))
    }) {
        Text("Vrati uobičajena zvona")
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
                    onChange(shift, timetable.copy(periods = periods.toMutableList().also { it[edit.index] = updated }))
                }
                editing = null
            },
        )
    }
}

private val ALARM_OFFSETS = listOf(0, 5, 10, 15)

/** Fixed id so repeated test notifications replace each other instead of piling up. */
private const val TEST_NOTIFICATION_ID = 4242

@Composable
private fun AlarmSettings(child: Child, viewModel: AppViewModel) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setAlarm(child.id, granted)
    }
    var testBlocked by remember { mutableStateOf(false) }

    fun sendTest() {
        val end = child.classesEndOn(LocalDate.now())
        val text = if (end != null) "Probno obaveštenje. Danas se časovi završavaju u ${end.label()}." else "Probno obaveštenje radi."
        testBlocked = !AlarmReceiver.showNotification(context, TEST_NOTIFICATION_ID, child.name.ifBlank { "Raspored časova" }, text)
    }

    val testPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendTest() else testBlocked = true
    }

    fun enable(enabled: Boolean) {
        val needsPermission = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setAlarm(child.id, enabled)
        }
    }

    Row(
        Modifier.fillMaxWidth().clickable { enable(!child.alarmEnabled) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Obavesti me kad se završe časovi")
            Hint("Stiže obaveštenje na ovaj telefon")
        }
        Switch(checked = child.alarmEnabled, onCheckedChange = { enable(it) })
    }
    if (child.alarmEnabled) {
        val today = LocalDate.now()
        child.nextAlarm(LocalDateTime.now())?.let { next ->
            val day = if (next.toLocalDate() == today) "danas" else next.toLocalDate().label().lowercase()
            Tag(
                "Sledeće: $day u ${next.toLocalTime().label()}",
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Hint("Koliko pre kraja poslednjeg časa:")
        PillToggle(
            options = ALARM_OFFSETS,
            selected = child.alarmMinutesBefore,
            label = { if (it == 0) "Na kraju" else "$it min" },
            onSelect = { viewModel.setAlarmMinutesBefore(child.id, it) },
        )
    }
    OutlinedButton(
        onClick = {
            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) testPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else sendTest()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("🔔  Pošalji probno obaveštenje")
    }
    if (testBlocked) {
        Text(
            "Obaveštenja su isključena za ovu aplikaciju. Uključi ih u podešavanjima telefona.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SubjectsEditor(child: Child, viewModel: AppViewModel) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Hint("${child.subjects.size} predmeta u padajućoj listi za raspored.", Modifier.weight(1f))
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Sakrij" else "Prikaži") }
    }
    if (expanded) {
        child.subjects.forEach { subject ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectBadge(subject, size = 36.dp)
                Text(subject, Modifier.weight(1f).padding(horizontal = 12.dp))
                IconButton(onClick = { viewModel.deleteSubject(child.id, subject) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Obriši $subject")
                }
            }
        }
        FilledTonalButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Dodaj predmet")
        }
        TextButton(onClick = { viewModel.resetSubjects(child.id) }) { Text("Vrati podrazumevani spisak") }
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
}

/** A titled white card grouping related settings. */
@Composable
private fun SettingsCard(emoji: String, title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Tappable bell time, e.g. "08:45". */
@Composable
private fun TimeChip(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}
