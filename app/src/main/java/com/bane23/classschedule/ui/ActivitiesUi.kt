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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.ActivityKind
import com.bane23.classschedule.data.ActivitySession
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.DEFAULT_ACTIVITY_REMINDER_MINUTES
import com.bane23.classschedule.data.Extracurricular
import com.bane23.classschedule.data.SessionChange
import com.bane23.classschedule.data.activitiesOn
import com.bane23.classschedule.data.remindsOf
import com.bane23.classschedule.data.sessionsOn
import com.bane23.classschedule.data.upcomingRegularDays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Color of extracurricular activities, so they never look like a school class. */
val ActivityViolet = Color(0xFF8B5CF6)

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * Days from [first] on (the picker reports UTC midnight of each day). A day already past would make an activity or a
 * moved session vanish at once, since past ones are not shown and are cleared away.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun selectableFrom(first: LocalDate) = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis / MILLIS_PER_DAY >= first.toEpochDay()

    override fun isSelectableYear(year: Int): Boolean = year >= first.year
}

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

/**
 * One activity in a list: tile, title, [time], [detail] and place, and the reminder bell when [reminder] is on. [tag]
 * marks a session changed this once ("Otkazano", "Pomereno"); [crossed] strikes out one that does not take place here.
 */
@Composable
fun ActivityRow(
    activity: Extracurricular,
    time: String,
    done: Boolean = false,
    now: Boolean = false,
    tag: String? = null,
    detail: String? = null,
    crossed: Boolean = false,
    reminder: Boolean = activity.reminderEnabled,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent, shape = MaterialTheme.shapes.small) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .semantics(mergeDescendants = true) {
                    // Read without emoji, but with whether it was cancelled or moved and where it takes place.
                    contentDescription = listOfNotNull(
                        activity.title,
                        time,
                        tag ?: if (now) "Sada" else null,
                        detail,
                        activity.place.ifBlank { null },
                    ).joinToString(", ")
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.alpha(if (crossed) 0.5f else 1f)) { ActivityTile(activity, done = done) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp).alpha(if (done || crossed) 0.55f else 1f)) {
                Text(
                    activity.title,
                    style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (crossed) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(time, detail, activity.place.ifBlank { null }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                tag != null -> Tag(tag, tintOf(HolidayAmber, 0.22f, 0.4f), MaterialTheme.colorScheme.onSurface)
                now -> Tag("Sada", tintOf(ActivityViolet, 0.25f, 0.45f), MaterialTheme.colorScheme.onSurface)
                reminder && !done -> Text("🔔", fontSize = 14.sp)
            }
        }
    }
}

/**
 * The child's activity sessions on [date] for the Today tab; with [clock] (today only) finished ones are faded and
 * the one in progress is marked. Sessions cancelled or moved this once stay in the list, marked, so they can be
 * restored. Tapping one opens it.
 */
@Composable
fun ActivitiesCard(child: Child, date: LocalDate, clock: LocalTime?, onOpen: (ActivitySession) -> Unit, onAdd: () -> Unit) {
    val sessions = child.sessionsOn(date)
    if (sessions.isEmpty()) return
    AppCard(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Text("🏅 Aktivnosti", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Tag("Vanškolske", tintOf(ActivityViolet, 0.18f, 0.35f), MaterialTheme.colorScheme.onSurface)
        }
        sessions.forEach { session ->
            val activity = session.activity
            val lastDay = activity.weekly || date == activity.lastDay
            val firstDay = activity.weekly || date == activity.firstDay
            val done = clock != null && session.takesPlace && lastDay && clock >= activity.end
            val now = clock != null && session.takesPlace && !done && (!firstDay || clock >= activity.start)
            ActivityRow(
                activity,
                activity.timeLabelOn(date),
                reminder = child.remindsOf(activity),
                done = done,
                now = now,
                tag = session.tag(),
                detail = session.detail(),
                crossed = !session.takesPlace,
            ) { onOpen(session) }
        }
        TextButton(colors = accentTextButton(), onClick = onAdd, modifier = Modifier.align(Alignment.End)) { Text("+ Dodaj aktivnost") }
    }
}

/** The first activity on [date] that has not started by [clock], for "next up" lines. */
fun Child.nextActivityOn(date: LocalDate, clock: LocalTime): Extracurricular? =
    activitiesOn(date).firstOrNull { (it.weekly || date == it.firstDay) && it.start > clock }

/**
 * Adds or edits an extracurricular activity of [child]: kind, title, every week on chosen days or once on a date (or
 * several days, e.g. a school camp), times, place, a note and the reminder. A weekly activity being edited also lists
 * its next sessions, each of which can be cancelled or moved once. [initial] null adds a new activity whose day is
 * [defaultDate]; [onDelete] is offered only when editing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityEditorSheet(
    child: Child,
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
    var changes by remember { mutableStateOf(start.changes) }
    var pickingTime by remember { mutableStateOf<Boolean?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    // Regular day of the session open in the one-session sheet.
    var sessionDay by remember { mutableStateOf<LocalDate?>(null) }

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
                            checked = selected,
                            onCheckedChange = { days = if (it) days + d.value else days - d.value },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
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
                            checked = noClasses,
                            onCheckedChange = { noClasses = it },
                        )
                        RowDivider()
                    }
                    if (weekly) {
                        SettingsRow(
                            title = "Pauza za raspust",
                            subtitle = "Ne prikazuje se i ne podseća tokom raspusta; na Dan škole i praznike ostaje",
                            emoji = "🏖️",
                            checked = paused,
                            onCheckedChange = { paused = it },
                        )
                        RowDivider()
                    }
                    SettingsRow(
                        title = "Podsetnik",
                        subtitle = when {
                            !reminder -> "Bez obaveštenja"
                            !child.activityRemindersEnabled -> "Svi podsetnici za aktivnosti su isključeni u „Obaveštenja”"
                            else -> "Obaveštenje ${reminderOffsetLabel(reminderMinutes).lowercase()} početka"
                        },
                        emoji = "🔔",
                        checked = reminder,
                        onCheckedChange = { reminder = it },
                    )
                    if (reminder) {
                        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                            PillToggle(REMINDER_OFFSETS, reminderMinutes, { durationLabel(it.toLong()) }, { reminderMinutes = it }, onCard = true)
                        }
                    }
                }
            }

            if (initial != null && initial.weekly && weekly) {
                // Sessions are listed by the saved days and times; a session changed earlier stays listed until it is past.
                val today = LocalDate.now()
                val draft = initial.copy(days = days.sorted(), pausedOnBreaks = paused, changes = changes)
                val sessionDays = (child.upcomingRegularDays(draft, today, 4) + changes.map { it.regularDay }.filter { it >= today })
                    .distinct().sorted()
                if (sessionDays.isNotEmpty()) {
                    Text("Otkaži ili pomeri jedan termin", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        Column {
                            sessionDays.forEachIndexed { i, day ->
                                if (i > 0) RowDivider()
                                val change = changes.find { it.regularDay == day }
                                SettingsRow(
                                    title = day.label(),
                                    subtitle = change?.label()?.replaceFirstChar { it.uppercase() }
                                        ?: "${minuteLabel(startMinute)}–${minuteLabel(endMinute)}",
                                    emoji = when {
                                        change == null -> "📅"
                                        change.cancelled -> "❌"
                                        else -> "➡️"
                                    },
                                    chevron = true,
                                    onClick = { sessionDay = day },
                                )
                            }
                        }
                    }
                    Hint("Promena važi samo za taj termin; redovan raspored ostaje isti za naredne nedelje.")
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
                        changes = if (weekly) changes else emptyList(),
                    ),
                )
            }
            if (onDelete != null) {
                TextButton(colors = accentTextButton(), onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
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

    sessionDay?.let { day ->
        ActivitySessionSheet(
            activity = start.copy(title = title.ifBlank { start.title }, kind = kind, startMinute = startMinute, endMinute = endMinute, changes = changes),
            regularDay = day,
            onDismiss = { sessionDay = null },
            onChange = {
                changes = it.changes
                sessionDay = null
            },
        )
    }

    if (pickingDate) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = firstDay.toEpochDay() * MILLIS_PER_DAY,
            initialSelectedEndDateMillis = lastDay.toEpochDay() * MILLIS_PER_DAY,
            // An activity already under way (a camp that started yesterday) can keep its first day.
            selectableDates = selectableFrom(minOf(firstDay, LocalDate.now())),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    colors = accentTextButton(),
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
            dismissButton = { TextButton(colors = accentTextButton(), onClick = { pickingDate = false }) { Text("Odustani") } },
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

/**
 * One session of a weekly [activity], the one regularly on [regularDay]: cancel it, move it to another day or time, or
 * restore it, all for this session only, so the following weeks keep the regular schedule. [onChange] gets the
 * activity with its changes updated; [onEditAll] (when given) opens the whole activity instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySessionSheet(
    activity: Extracurricular,
    regularDay: LocalDate,
    onDismiss: () -> Unit,
    onChange: (Extracurricular) -> Unit,
    onEditAll: (() -> Unit)? = null,
) {
    val change = activity.changeOn(regularDay)
    var moving by remember { mutableStateOf(change?.movedTo != null) }
    var moveDay by remember { mutableStateOf(change?.movedTo ?: regularDay) }
    var startMinute by remember { mutableStateOf(change?.takeIf { !it.cancelled }?.startMinute ?: activity.startMinute) }
    var endMinute by remember { mutableStateOf(change?.takeIf { !it.cancelled }?.endMinute ?: activity.endMinute) }
    var pickingTime by remember { mutableStateOf<Boolean?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    val regularTime = "${minuteLabel(activity.startMinute)}–${minuteLabel(activity.endMinute)}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActivityTile(activity, size = 44.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("Jedan termin", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(activity.title, style = MaterialTheme.typography.titleLarge)
                    Text("${regularDay.label()} · $regularTime", style = MaterialTheme.typography.bodyMedium)
                }
            }
            when {
                change == null -> Unit
                change.cancelled -> NoteBanner("❌", "Ovaj termin je otkazan.")
                else -> NoteBanner("➡️", change.label().replaceFirstChar { it.uppercase() } + ".")
            }

            if (!moving) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    Column {
                        if (change?.cancelled != true) {
                            SettingsRow(
                                title = "Otkaži ovaj termin",
                                subtitle = "Bez podsetnika; naredne nedelje ostaju iste",
                                emoji = "❌",
                                onClick = { onChange(activity.withChange(regularDay, SessionChange(regularDay.toEpochDay()))) },
                            )
                            RowDivider()
                        }
                        SettingsRow(
                            title = "Pomeri ovaj termin",
                            subtitle = "Drugi dan ili drugo vreme, samo ovog puta",
                            emoji = "📆",
                            chevron = true,
                            onClick = { moving = true },
                        )
                        if (change != null) {
                            RowDivider()
                            SettingsRow(
                                title = "Vrati redovan termin",
                                subtitle = "${regularDay.label()} · $regularTime",
                                emoji = "↩️",
                                onClick = { onChange(activity.withChange(regularDay, null)) },
                            )
                        }
                        if (onEditAll != null) {
                            RowDivider()
                            SettingsRow(
                                title = "Izmeni aktivnost",
                                subtitle = "Dani, vreme i podsetnik za sve nedelje",
                                emoji = "✏️",
                                chevron = true,
                                onClick = onEditAll,
                            )
                        }
                    }
                }
                Hint("Redovan raspored aktivnosti ostaje isti za naredne nedelje.")
            } else {
                FieldButton("📅", "Novi dan", moveDay.label()) { pickingDate = true }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldButton("🕔", "Počinje", minuteLabel(startMinute), Modifier.weight(1f)) { pickingTime = true }
                    FieldButton("🏁", "Završava", minuteLabel(endMinute), Modifier.weight(1f)) { pickingTime = false }
                }
                val timesOk = endMinute > startMinute
                if (!timesOk) Hint("Kraj mora biti posle početka.")
                Hint("Podsetnik stiže ${reminderOffsetLabel(activity.reminderMinutesBefore).lowercase()} novog početka, ako je uključen.")
                BigButton("Sačuvaj pomeranje", Modifier.fillMaxWidth(), enabled = timesOk) {
                    val unchanged = moveDay == regularDay && startMinute == activity.startMinute && endMinute == activity.endMinute
                    val moved = SessionChange(regularDay.toEpochDay(), moveDay.toEpochDay(), startMinute, endMinute)
                    onChange(activity.withChange(regularDay, if (unchanged) null else moved))
                }
                TextButton(colors = accentTextButton(), onClick = { moving = false }, modifier = Modifier.fillMaxWidth()) { Text("Nazad") }
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
        val state = rememberDatePickerState(
            initialSelectedDateMillis = moveDay.toEpochDay() * MILLIS_PER_DAY,
            selectableDates = selectableFrom(LocalDate.now()),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    colors = accentTextButton(),
                    enabled = state.selectedDateMillis != null,
                    onClick = {
                        state.selectedDateMillis?.let { moveDay = LocalDate.ofEpochDay(it / MILLIS_PER_DAY) }
                        pickingDate = false
                    },
                ) { Text("U redu") }
            },
            dismissButton = { TextButton(colors = accentTextButton(), onClick = { pickingDate = false }) { Text("Odustani") } },
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

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
