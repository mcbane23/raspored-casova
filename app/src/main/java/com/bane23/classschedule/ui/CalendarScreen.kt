package com.bane23.classschedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.CalendarEvent
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.calendarEvents
import com.bane23.classschedule.data.nextDayOff
import com.bane23.classschedule.data.schoolYearOf
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * The "Raspust" tab: a big countdown to the next break, then every coming break, day off and school event for the
 * selected child. Events the parent added can be deleted.
 */
@Composable
fun CalendarScreen(
    children: List<Child>,
    child: Child,
    onSelectChild: (String) -> Unit,
    onOpenSettings: () -> Unit,
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
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ChildTopRow(children, child.id, onSelectChild, title = "Raspust") { SettingsButton(onOpenSettings) }
        }
        val next = child.nextDayOff(today)
        if (next != null) {
            item { CountdownCard(next, today) }
        }
        if (year == null) {
            item { NoteBanner("ℹ️", "Kalendar za ovu školsku godinu još nije dodat u aplikaciju.") }
        }
        item { SectionLabel("Slobodni dani i događaji") }
        if (upcoming.isEmpty()) {
            item {
                AppCard {
                    EmptyState("📅", "Nema predstojećih događaja", "Dodaj Dan škole, školsku slavu ili izlet.", action = "+ Dodaj događaj", onAction = { adding = true })
                }
            }
        }
        items(upcoming, key = { "${it.id}-${it.startEpochDay}" }) { event ->
            EventTicket(event, today) {
                if (event.id in customIds) {
                    IconButton(onClick = { onDeleteEvent(event.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Obriši događaj", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { DashedButton("+ Dodaj događaj škole", onClick = { adding = true }) }
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

/** Big countdown to the next break: the number children care about most. */
@Composable
private fun CountdownCard(next: CalendarEvent, today: LocalDate) {
    val ongoing = next.covers(today)
    val days = ChronoUnit.DAYS.between(today, next.start)
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = heroColorOf(HolidayAmber)) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (ongoing) {
                Text("Sada je", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.9f))
                Text(eventEmoji(next.title), fontSize = 56.sp)
                Text(next.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
                Text("Traje do ${dateRangeLabel(next.end, next.end)} 🎉", style = MaterialTheme.typography.bodyLarge, color = Color.White)
            } else {
                Text("${eventEmoji(next.title)} ${next.title} za", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                Text("$days", fontSize = 72.sp, lineHeight = 76.sp, style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text(daysCount(days).removePrefix("$days "), style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(dateRangeLabel(next.start, next.end), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
        }
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
