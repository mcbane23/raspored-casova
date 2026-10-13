package com.bane23.classschedule.ui

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Period
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.shiftOn
import java.time.LocalDate

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
    periods.forEachIndexed { index, period ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}. čas", Modifier.width(72.dp))
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
    TextButton(onClick = { onChange(shift, timetable.copy(periods = Timetable.defaultFor(shift).periods)) }) {
        Text("Vrati uobičajena zvona")
    }

    editing?.let { edit ->
        val period = periods[edit.index]
        TimeInputDialog(
            title = "${edit.index + 1}. čas, ${if (edit.isStart) "početak" else "kraj"}",
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

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}
