package com.bane23.classschedule.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
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
import com.bane23.classschedule.data.tr
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
    // Follows the clock, so the countdown moves on at midnight while the app stays open.
    val today = rememberNow().toLocalDate()
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<CalendarEvent?>(null) }
    val customIds = child.customEvents.map { it.id }.toSet()
    val upcoming = child.calendarEvents().filter { it.endEpochDay >= today.toEpochDay() }
    val year = schoolYearOf(today)

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ChildTopRow(children, child.id, onSelectChild, title = tr("Raspust", "Breaks")) { SettingsButton(onOpenSettings) }
        }
        val next = child.nextDayOff(today)
        if (next != null) {
            item { CountdownCard(next, today) }
        }
        if (year == null) {
            item { NoteBanner("ℹ️", tr("Kalendar za ovu školsku godinu još nije dodat u aplikaciju.", "The calendar for this school year has not been added to the app yet.")) }
        }
        item { SectionLabel(tr("Slobodni dani i događaji", "Days off and events")) }
        if (upcoming.isEmpty()) {
            item {
                AppCard {
                    EmptyState(
                        "📅",
                        tr("Nema predstojećih događaja", "No upcoming events"),
                        tr("Dodaj Dan škole, školsku slavu ili izlet.", "Add the School Day, the school’s slava or a trip."),
                        action = tr("+ Dodaj događaj", "+ Add event"),
                        onAction = { adding = true },
                    )
                }
            }
        }
        items(upcoming, key = { "${it.id}-${it.startEpochDay}" }) { event ->
            EventTicket(event, today) {
                if (event.id in customIds) {
                    IconButton(onClick = { deleting = event }) {
                        Icon(Icons.Filled.Delete, contentDescription = tr("Obriši događaj", "Delete event"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { DashedButton(tr("+ Dodaj događaj škole", "+ Add school event"), onClick = { adding = true }) }
    }

    deleting?.let { event ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(tr("Obrisati „${event.title}”?", "Delete “${event.title}”?")) },
            confirmButton = {
                TextButton(colors = accentTextButton(), onClick = {
                    onDeleteEvent(event.id)
                    deleting = null
                }) { Text(tr("Obriši", "Delete")) }
            },
            dismissButton = { TextButton(colors = accentTextButton(), onClick = { deleting = null }) { Text(tr("Odustani", "Cancel")) } },
        )
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
                Text(tr("Sada je", "Now it’s"), style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.9f))
                Text(eventEmoji(next.title), fontSize = 56.sp)
                Text(next.title, style = MaterialTheme.typography.headlineSmall, color = Color.White, textAlign = TextAlign.Center)
                Text(tr("Traje do ${next.end.genitiveLabel()} 🎉", "Until ${next.end.genitiveLabel()} 🎉"), style = MaterialTheme.typography.bodyLarge, color = Color.White)
            } else {
                Text(tr("${eventEmoji(next.title)} ${next.title} za", "${eventEmoji(next.title)} ${next.title} in"), style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                Text("$days", fontSize = 72.sp, lineHeight = 76.sp, style = MaterialTheme.typography.displaySmall, color = Color.White)
                Text(daysCount(days).removePrefix("$days "), style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(dateRangeLabel(next.start, next.end), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

/** Common school events offered as one-tap chips: title and whether there are classes that day. */
private fun quickEvents(): List<Pair<String, Boolean>> = listOf(
    tr("🏫 Dan škole", "🏫 School Day") to true,
    tr("🕯️ Školska slava", "🕯️ School slava") to true,
    tr("🚌 Izlet", "🚌 Trip") to false,
    tr("🎭 Priredba", "🎭 School show") to false,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddEventDialog(onDismiss: () -> Unit, onConfirm: (CalendarEvent) -> Unit) {
    val today = LocalDate.now()
    var title by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(today) }
    var end by remember { mutableStateOf(today) }
    var noClasses by remember { mutableStateOf(true) }
    var picking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Novi događaj", "New event")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickEvents().forEach { (label, off) ->
                        val name = label.substringAfter(' ')
                        FilterChip(
                            selected = title == name,
                            onClick = {
                                title = name
                                noClasses = off
                            },
                            label = { Text(label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(tr("Naziv", "Name")) },
                    singleLine = true,
                )
                Surface(
                    onClick = { picking = true },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr("Datum", "Date"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(dateRangeLabel(start, end), style = MaterialTheme.typography.titleSmall)
                        }
                        Text(tr("Promeni", "Change"), style = MaterialTheme.typography.labelLarge, color = accentText())
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = noClasses, onCheckedChange = { noClasses = it })
                    Text(tr("Nema nastave tog dana", "No classes that day"))
                }
            }
        },
        confirmButton = {
            TextButton(
                colors = accentTextButton(),
                enabled = title.isNotBlank(),
                onClick = {
                    onConfirm(CalendarEvent(start.toEpochDay(), maxOf(start, end).toEpochDay(), title.trim(), noClasses))
                },
            ) { Text(tr("Sačuvaj", "Save")) }
        },
        dismissButton = { TextButton(colors = accentTextButton(), onClick = onDismiss) { Text(tr("Odustani", "Cancel")) } },
    )

    if (picking) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = start.toEpochDay() * MILLIS_PER_DAY,
            initialSelectedEndDateMillis = end.toEpochDay() * MILLIS_PER_DAY,
            // Past days are not offered: the list shows only coming events, so a past one would just disappear.
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis / MILLIS_PER_DAY >= LocalDate.now().toEpochDay()
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    colors = accentTextButton(),
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        // The picker reports UTC midnight of the chosen days; one tapped day is a one-day event.
                        state.selectedStartDateMillis?.let { millis ->
                            start = LocalDate.ofEpochDay(millis / MILLIS_PER_DAY)
                            end = state.selectedEndDateMillis?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY) } ?: start
                        }
                        picking = false
                    },
                ) { Text(tr("U redu", "OK")) }
            },
            dismissButton = { TextButton(colors = accentTextButton(), onClick = { picking = false }) { Text(tr("Odustani", "Cancel")) } },
        ) {
            DateRangePicker(
                state = state,
                title = { Text(tr("Izaberi dan ili više dana", "Pick one or more days"), Modifier.padding(start = 24.dp, top = 16.dp)) },
                showModeToggle = false,
                modifier = Modifier.height(480.dp),
            )
        }
    }
}
