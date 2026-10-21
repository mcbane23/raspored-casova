package com.bane23.classschedule.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.ActivityKind
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.DEFAULT_ACTIVITY_REMINDER_MINUTES
import com.bane23.classschedule.data.Extracurricular
import com.bane23.classschedule.data.activitiesOn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Color of extracurricular activities, so they never look like a school class. */
val ActivityViolet = Color(0xFF8B5CF6)

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/** How long before the start an activity reminder can come. */
private val REMINDER_OFFSETS = listOf(15, 30, 60, 120)

/** The activity's emoji on a violet tile; [done] fades it and shows a check. */
@Composable
fun ActivityTile(activity: Extracurricular, size: Dp = 40.dp, done: Boolean = false) {
    Box(
        Modifier
            .size(size)
            .alpha(if (done) 0.5f else 1f)
            .clip(MaterialTheme.shapes.small)
            .background(tintOf(ActivityViolet, 0.22f, 0.38f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (done) "✓" else activity.kind.emoji(), fontSize = (size.value * 0.45f).sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** One activity in a list: tile, title, [time] and place, and the reminder time when it is on. */
@Composable
fun ActivityRow(activity: Extracurricular, time: String, done: Boolean = false, now: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, shape = MaterialTheme.shapes.small) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .semantics(mergeDescendants = true) { contentDescription = "${activity.title}, $time" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActivityTile(activity, done = done)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp).alpha(if (done) 0.55f else 1f)) {
                Text(activity.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(time, activity.place.ifBlank { null }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                now -> Tag("Sada", tintOf(ActivityViolet, 0.25f, 0.45f), MaterialTheme.colorScheme.onSurface)
                activity.reminderEnabled && !done -> Text("🔔", fontSize = 14.sp)
            }
        }
    }
}

/**
 * The child's activities on [date] for the Today tab; with [clock] (today only) finished ones are faded and the one
 * in progress is marked. Tapping one opens it for editing.
 */
@Composable
fun ActivitiesCard(child: Child, date: LocalDate, clock: LocalTime?, onOpen: (Extracurricular) -> Unit, onAdd: () -> Unit) {
    val activities = child.activitiesOn(date)
    if (activities.isEmpty()) return
    AppCard(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Text("🏅 Aktivnosti", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Tag("Vanškolske", tintOf(ActivityViolet, 0.18f, 0.35f), MaterialTheme.colorScheme.onSurface)
        }
        activities.forEach { activity ->
            val lastDay = activity.weekly || date == activity.lastDay
            val firstDay = activity.weekly || date == activity.firstDay
            val done = clock != null && lastDay && clock >= activity.end
            val now = clock != null && !done && (!firstDay || clock >= activity.start)
            ActivityRow(activity, activity.timeLabelOn(date), done = done, now = now) { onOpen(activity) }
        }
        TextButton(onClick = onAdd, modifier = Modifier.align(Alignment.End)) { Text("+ Dodaj aktivnost") }
    }
}

/** The first activity on [date] that has not started by [clock], for "next up" lines. */
fun Child.nextActivityOn(date: LocalDate, clock: LocalTime): Extracurricular? =
    activitiesOn(date).firstOrNull { (it.weekly || date == it.firstDay) && it.start > clock }

/**
 * Adds or edits an extracurricular activity: kind, title, every week on chosen days or once on a date (or several
 * days, e.g. a school camp), times, place, a note and the reminder. [initial] null adds a new activity whose day is
 * [defaultDate]; [onDelete] is offered only when editing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityEditorSheet(
    initial: Extracurricular?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Extracurricular) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val start = remember {
        initial ?: Extracurricular(
            title = "",
            days = listOf(defaultDate.dayOfWeek.value),
            startEpochDay = defaultDate.toEpochDay(),
            endEpochDay = defaultDate.toEpochDay(),
            startMinute = 17 * 60,
            endMinute = 18 * 60,
        )
    }
    var kind by remember { mutableStateOf(start.kind) }
    var title by remember { mutableStateOf(start.title) }
    var weekly by remember { mutableStateOf(initial?.weekly ?: true) }
    var days by remember { mutableStateOf(start.days.ifEmpty { listOf(defaultDate.dayOfWeek.value) }.toSet()) }
    var firstDay by remember { mutableStateOf(if (initial?.weekly == false) start.firstDay else defaultDate) }
    var lastDay by remember { mutableStateOf(if (initial?.weekly == false) start.lastDay else defaultDate) }
    var startMinute by remember { mutableStateOf(start.startMinute) }
    var endMinute by remember { mutableStateOf(start.endMinute) }
    var place by remember { mutableStateOf(start.place) }
    var note by remember { mutableStateOf(start.note) }
    var paused by remember { mutableStateOf(start.pausedOnBreaks) }
    var noClasses by remember { mutableStateOf(start.noClasses) }
    var reminder by remember { mutableStateOf(start.reminderEnabled) }
    var reminderMinutes by remember { mutableStateOf(start.reminderMinutesBefore.takeIf { it in REMINDER_OFFSETS } ?: DEFAULT_ACTIVITY_REMINDER_MINUTES) }
    var pickingTime by remember { mutableStateOf<Boolean?>(null) }
    var pickingDate by remember { mutableStateOf(false) }

    val multiDay = !weekly && lastDay > firstDay
    val timesOk = multiDay || endMinute > startMinute
    val canSave = title.isNotBlank() && (!weekly || days.isNotEmpty()) && timesOk

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column {
                Text("Vanškolska aktivnost", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (initial == null) "Nova aktivnost" else "Izmeni aktivnost", style = MaterialTheme.typography.titleLarge)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ActivityKind.entries.forEach { k ->
                    FilterChip(
                        selected = kind == k,
                        onClick = {
                            // A title still blank or still the previous kind's name follows the kind.
                            if (title.isBlank() || title == kind.label()) title = if (k == ActivityKind.OTHER) "" else k.label()
                            if (initial == null) {
                                weekly = !k.usuallyOnce()
                                noClasses = k.usuallyOnce()
                            }
                            kind = k
                        },
                        label = { Text("${k.emoji()} ${k.label()}") },
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Naziv (npr. Fudbal, Hor, Izlet na Avalu)") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            PillToggle(listOf(true, false), weekly, { if (it) "🔁 Svake nedelje" else "📅 Jednom" }, { weekly = it })
            if (weekly) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        val selected = d.value in days
                        Surface(
                            onClick = { days = if (selected) days - d.value else days + d.value },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.weight(1f).height(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    d.shortLabel(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            } else {
                FieldButton("📅", "Datum", dateRangeLabel(firstDay, lastDay)) { pickingDate = true }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldButton("🕔", if (multiDay) "Polazak" else "Počinje", minuteLabel(startMinute), Modifier.weight(1f)) { pickingTime = true }
                FieldButton("🏁", if (multiDay) "Povratak" else "Završava", minuteLabel(endMinute), Modifier.weight(1f)) { pickingTime = false }
            }
            if (!timesOk) Hint("Kraj mora biti posle početka.")
            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                label = { Text("Mesto (npr. Sportska hala)") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Napomena (npr. patike i flašica vode)") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            )

            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                Column {
                    if (!weekly) {
                        SettingsRow(
                            title = "Nema nastave",
                            subtitle = if (noClasses) "Tih dana nema časova, kao za raspust" else "Časovi se održavaju po rasporedu",
                            emoji = "🏫",
                            trailing = { Switch(checked = noClasses, onCheckedChange = { noClasses = it }) },
                            onClick = { noClasses = !noClasses },
                        )
                        RowDivider()
                    }
                    if (weekly) {
                        SettingsRow(
                            title = "Pauza za raspust i praznike",
                            subtitle = "Ne prikazuje se i ne podseća kad nema nastave zbog raspusta",
                            emoji = "🏖️",
                            trailing = { Switch(checked = paused, onCheckedChange = { paused = it }) },
                            onClick = { paused = !paused },
                        )
                        RowDivider()
                    }
                    SettingsRow(
                        title = "Podsetnik",
                        subtitle = if (reminder) "Obaveštenje ${reminderOffsetLabel(reminderMinutes).lowercase()} početka" else "Bez obaveštenja",
                        emoji = "🔔",
                        trailing = { Switch(checked = reminder, onCheckedChange = { reminder = it }) },
                        onClick = { reminder = !reminder },
                    )
                    if (reminder) {
                        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                            PillToggle(REMINDER_OFFSETS, reminderMinutes, { durationLabel(it.toLong()) }, { reminderMinutes = it }, onCard = true)
                        }
                    }
                }
            }

            BigButton("Sačuvaj", Modifier.fillMaxWidth(), enabled = canSave) {
                onSave(
                    start.copy(
                        title = title.trim(),
                        kind = kind,
                        days = if (weekly) days.sorted() else emptyList(),
                        startEpochDay = if (weekly) 0 else firstDay.toEpochDay(),
                        endEpochDay = if (weekly) 0 else lastDay.toEpochDay(),
                        startMinute = startMinute,
                        endMinute = endMinute,
                        place = place.trim(),
                        note = note.trim(),
                        pausedOnBreaks = weekly && paused,
                        noClasses = !weekly && noClasses,
                        reminderEnabled = reminder,
                        reminderMinutesBefore = reminderMinutes,
                    ),
                )
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Obriši aktivnost", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    pickingTime?.let { isStart ->
        TimeInputDialog(
            title = if (isStart) "Početak" else "Kraj",
            initialMinute = if (isStart) startMinute else endMinute,
            onDismiss = { pickingTime = null },
            onConfirm = { minute ->
                if (isStart) {
                    // Moving the start keeps the length the same, like the bell times.
                    val length = (endMinute - startMinute).coerceAtLeast(15)
                    startMinute = minute
                    endMinute = (minute + length).coerceAtMost(24 * 60 - 1)
                } else {
                    endMinute = minute
                }
                pickingTime = null
            },
        )
    }

    if (pickingDate) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = firstDay.toEpochDay() * MILLIS_PER_DAY,
            initialSelectedEndDateMillis = lastDay.toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null,
                    onClick = {
                        // The picker reports UTC midnight of the chosen days; one tapped day is a one-day activity.
                        state.selectedStartDateMillis?.let { millis ->
                            firstDay = LocalDate.ofEpochDay(millis / MILLIS_PER_DAY)
                            lastDay = state.selectedEndDateMillis?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY) } ?: firstDay
                        }
                        pickingDate = false
                    },
                ) { Text("U redu") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Odustani") } },
        ) {
            DateRangePicker(
                state = state,
                title = { Text("Izaberi dan ili više dana", Modifier.padding(start = 24.dp, top = 16.dp)) },
                showModeToggle = false,
                modifier = Modifier.height(480.dp),
            )
        }
    }
}

private fun minuteLabel(minute: Int): String = LocalTime.of(minute / 60, minute % 60).label()

/** A tappable field showing a value, e.g. the date or a time, with a small label above it. */
@Composable
private fun FieldButton(emoji: String, label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
