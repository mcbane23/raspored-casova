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

/** Upcoming breaks, holidays and school events for the selected child, grouped by month, plus the child's own events. */
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
    val byMonth = upcoming.groupBy { it.start.withDayOfMonth(1) }
    val year = schoolYearOf(today)

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Kalendar",
                subtitle = if (year != null) "Školska godina ${year.label}" else "Kalendar za ovu školsku godinu još nije dodat",
            )
        }
        child.nextDayOff(today)?.let { next ->
            item { CountdownCard(next, today) }
        }
        item {
            FilledTonalButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Dodaj događaj škole")
            }
        }
        if (upcoming.isEmpty()) {
            item { EmptyState("📅", "Nema predstojećih događaja", "Dodaj Dan škole, školsku slavu ili izlet.") }
        }
        byMonth.forEach { (month, events) ->
            item(key = "month-${month.toEpochDay()}") { SectionLabel(month.monthYearLabel()) }
            items(events, key = { it.id }) { event ->
                EventRow(
                    event = event,
                    isCustom = event.id in customIds,
                    isNow = event.covers(today),
                    onDelete = { onDeleteEvent(event.id) },
                )
            }
        }
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

/** Big, friendly countdown to the next break: the number children care about most. */
@Composable
private fun CountdownCard(next: CalendarEvent, today: LocalDate) {
    val colors = MaterialTheme.colorScheme
    val ongoing = next.covers(today)
    val days = ChronoUnit.DAYS.between(today, next.start)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.tertiary,
        shadowElevation = 4.dp,
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (ongoing) "Sada je" else "Do sledećeg odmora",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onTertiary.copy(alpha = 0.8f),
                )
                Text(next.title, style = MaterialTheme.typography.headlineSmall, color = colors.onTertiary)
                Text(
                    if (ongoing) "Traje do ${dateRangeLabel(next.end, next.end)} 🎉" else dateRangeLabel(next.start, next.end),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onTertiary.copy(alpha = 0.85f),
                )
            }
            if (ongoing) {
                Text(eventEmoji(next.title), fontSize = 48.sp)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$days", fontSize = 48.sp, lineHeight = 52.sp, style = MaterialTheme.typography.headlineMedium, color = colors.onTertiary)
                    Text(
                        inDaysLabel(days).removePrefix("za $days "),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onTertiary.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, isCustom: Boolean, isNow: Boolean, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppCard(contentPadding = PaddingValues(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateBadge(event.start, highlighted = isNow)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${eventEmoji(event.title)} ${event.title}", style = MaterialTheme.typography.titleMedium)
                Text(
                    dateRangeLabel(event.start, event.end),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                if (event.noClasses) {
                    Tag("Nema nastave", colors.tertiaryContainer, colors.onTertiaryContainer)
                } else {
                    Tag("Nastava se održava", colors.surfaceVariant, colors.onSurfaceVariant)
                }
            }
            if (isCustom) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Obriši događaj", tint = colors.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

/** Day number over a short month name, like a tear-off calendar page. */
@Composable
private fun DateBadge(date: LocalDate, highlighted: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.size(56.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (highlighted) colors.primary else colors.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${date.dayOfMonth}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (highlighted) colors.onPrimary else colors.onPrimaryContainer,
                )
                Text(
                    date.monthShortLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlighted) colors.onPrimary else colors.onPrimaryContainer,
                )
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
