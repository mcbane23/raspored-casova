package com.bane23.classschedule.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Period
import com.bane23.classschedule.data.Region
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import androidx.core.content.ContextCompat
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

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle("Deca")
        data.children.forEach { c ->
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.selectChild(c.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = c.id == child.id, onClick = { viewModel.selectChild(c.id) })
                Column {
                    Text(c.name.ifBlank { "Bez imena" })
                    if (c.school.isNotBlank()) {
                        Text(c.school, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        OutlinedButton(onClick = { addingChild = true }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Dodaj dete")
        }

        HorizontalDivider()
        SectionTitle("Podaci za: ${child.name.ifBlank { "dete" }}")
        OutlinedTextField(
            value = child.name,
            onValueChange = { viewModel.renameChild(child.id, it) },
            label = { Text("Ime") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = child.school,
            onValueChange = { viewModel.setSchool(child.id, it) },
            label = { Text("Škola i odeljenje") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Školski kalendar")
        Region.entries.forEach { region ->
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.setRegion(child.id, region) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = child.region == region, onClick = { viewModel.setRegion(child.id, region) })
                Text(region.label())
            }
        }
        Row(
            Modifier.fillMaxWidth().clickable { viewModel.setFinalGrade(child.id, !child.finalGrade) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = child.finalGrade, onCheckedChange = { viewModel.setFinalGrade(child.id, it) })
            Text("Osmi razred (ranije završava nastavu)")
        }

        HorizontalDivider()
        ShareSection(child, viewModel)

        HorizontalDivider()
        AlarmSettings(child, viewModel)

        HorizontalDivider()
        SectionTitle("Smene")
        ShiftMode.entries.forEach { mode ->
            Row(
                Modifier.fillMaxWidth().clickable { viewModel.setShiftMode(child.id, mode) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = child.shiftMode == mode, onClick = { viewModel.setShiftMode(child.id, mode) })
                Text(mode.label())
            }
        }
        if (child.shiftMode == ShiftMode.WEEKLY || child.shiftMode == ShiftMode.MONTHLY) {
            val current = child.shiftOn(LocalDate.now())
            Text(if (child.shiftMode == ShiftMode.WEEKLY) "Ove nedelje dete ide:" else "Ovog meseca dete ide:")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Shift.entries.forEach { s ->
                    FilterChip(
                        selected = current == s,
                        onClick = { viewModel.setCurrentShift(child.id, s) },
                        label = { Text(s.shortLabel()) },
                    )
                }
            }
        }

        HorizontalDivider()
        SubjectsEditor(child, viewModel)

        HorizontalDivider()
        BellScheduleEditor(child, onChange = { shift, timetable -> viewModel.setTimetable(child.id, shift, timetable) })

        if (data.children.size > 1) {
            HorizontalDivider()
            TextButton(onClick = { confirmDelete = true }) {
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

    SectionTitle("Zvona (vreme časova)")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Shift.entries.forEach { s ->
            FilterChip(selected = shift == s, onClick = { shift = s }, label = { Text(s.shortLabel()) })
        }
    }
    Row(
        Modifier.fillMaxWidth().clickable { onChange(shift, timetable.withPreClass(!timetable.hasPreClass)) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Pretčas pre 1. časa", Modifier.weight(1f))
        Switch(checked = timetable.hasPreClass, onCheckedChange = { onChange(shift, timetable.withPreClass(it)) })
    }
    periods.forEachIndexed { index, period ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(timetable.periodLabel(index), Modifier.width(72.dp))
            TextButton(onClick = { editing = BellEdit(index, isStart = true) }) { Text(period.start.label()) }
            Text("–")
            TextButton(onClick = { editing = BellEdit(index, isStart = false) }) { Text(period.end.label()) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (periods.size < 10) {
            OutlinedButton(onClick = {
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

@Composable
private fun AlarmSettings(child: Child, viewModel: AppViewModel) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setAlarm(child.id, granted)
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

    SectionTitle("Roditeljski alarm")
    Row(
        Modifier.fillMaxWidth().clickable { enable(!child.alarmEnabled) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Obavesti me kad se završe časovi", Modifier.weight(1f))
        Switch(checked = child.alarmEnabled, onCheckedChange = { enable(it) })
    }
    if (child.alarmEnabled) {
        val today = LocalDate.now()
        child.nextAlarm(LocalDateTime.now())?.let { next ->
            val day = if (next.toLocalDate() == today) "danas" else next.toLocalDate().label().lowercase()
            Text(
                "Sledeće obaveštenje: $day u ${next.toLocalTime().label()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("Koliko pre kraja poslednjeg časa:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ALARM_OFFSETS.forEach { minutes ->
                FilterChip(
                    selected = child.alarmMinutesBefore == minutes,
                    onClick = { viewModel.setAlarmMinutesBefore(child.id, minutes) },
                    label = { Text(if (minutes == 0) "Na kraju" else "$minutes min") },
                )
            }
        }
    }
}

@Composable
private fun SubjectsEditor(child: Child, viewModel: AppViewModel) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            SectionTitle("Predmeti (${child.subjects.size})")
            Text(
                "Nude se u padajućoj listi kad popunjavaš raspored.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Sakrij" else "Prikaži") }
    }
    if (expanded) {
        child.subjects.forEach { subject ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(subject, Modifier.weight(1f))
                IconButton(onClick = { viewModel.deleteSubject(child.id, subject) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Obriši $subject")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { adding = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Dodaj predmet")
            }
            TextButton(onClick = { viewModel.resetSubjects(child.id) }) { Text("Vrati podrazumevani spisak") }
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
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}
