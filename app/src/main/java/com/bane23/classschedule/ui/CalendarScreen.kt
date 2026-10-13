package com.bane23.classschedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bane23.classschedule.data.CalendarEvent
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.calendarEvents
import com.bane23.classschedule.data.nextDayOff
import com.bane23.classschedule.data.schoolYearOf
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/** Upcoming breaks, holidays and school events for the selected child, plus the child's own events. */
@Composable
fun CalendarScreen(
    child: Child,
    onAddEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    var adding by remember { mutableStateOf(false) }
    val customIds = child.customEvents.map { it.id }.toSet()
    val upcoming = child.calendarEvents().filter { it.endEpochDay >= today.toEpochDay() }
    val year = schoolYearOf(today)

    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (year != null) "Školska godina ${year.label}" else "Kalendar za ovu školsku godinu još nije dodat",
                    style = MaterialTheme.typography.titleMedium,
                )
                child.nextDayOff(today)?.let { next ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Text(
                            if (next.covers(today)) "Sada: ${next.title}, do ${dateRangeLabel(next.end, next.end)}"
                            else "Sledeće: ${next.title}, ${inDaysLabel(ChronoUnit.DAYS.between(today, next.start))}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        )
                    }
                }
                OutlinedButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Dodaj događaj škole")
                }
            }
        }
        items(upcoming, key = { it.id }) { event ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(event.title, style = MaterialTheme.typography.titleMedium)
                        Text(dateRangeLabel(event.start, event.end), style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (event.noClasses) "Nema nastave" else "Nastava se održava",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (event.noClasses) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (event.id in customIds) {
                        IconButton(onClick = { onDeleteEvent(event.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Obriši događaj")
                        }
                    } else {
                        Spacer(Modifier.width(16.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.padding(bottom = 8.dp)) }
    }

    if (adding) {
        AddEventDialog(
            onDismiss = { adding = false },
            onConfirm = {
                onAddEvent(it)
                adding = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventDialog(onDismiss: () -> Unit, onConfirm: (CalendarEvent) -> Unit) {
    val today = LocalDate.now()
    var title by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(today) }
    var end by remember { mutableStateOf(today) }
    var noClasses by remember { mutableStateOf(true) }
    var picking by remember { mutableStateOf<Boolean?>(null) } // true = start date, false = end date

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novi događaj") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Naziv (npr. Dan škole)") },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Od:", Modifier.width(40.dp))
                    TextButton(onClick = { picking = true }) { Text(start.label()) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Do:", Modifier.width(40.dp))
                    TextButton(onClick = { picking = false }) { Text(end.label()) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = noClasses, onCheckedChange = { noClasses = it })
                    Text("Nema nastave")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onConfirm(CalendarEvent(start.toEpochDay(), maxOf(start, end).toEpochDay(), title.trim(), noClasses))
                },
            ) { Text("Sačuvaj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Odustani") } },
    )

    picking?.let { isStart ->
        val initial = if (isStart) start else end
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.toEpochDay() * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    // The picker reports UTC midnight of the chosen day.
                    state.selectedDateMillis?.let { millis ->
                        val date = LocalDate.ofEpochDay(millis / MILLIS_PER_DAY)
                        if (isStart) {
                            start = date
                            if (end < date) end = date
                        } else {
                            end = date
                        }
                    }
                    picking = null
                }) { Text("U redu") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Odustani") } },
        ) {
            DatePicker(state = state)
        }
    }
}
