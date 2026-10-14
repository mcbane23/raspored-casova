package com.bane23.classschedule.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.ExtraClass
import com.bane23.classschedule.data.SCHOOL_DAYS
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.other
import com.bane23.classschedule.data.shiftOn
import java.time.DayOfWeek
import java.time.LocalDate

private data class WeekEdit(val day: DayOfWeek, val index: Int, val subject: String)

/**
 * The regular weekly timetable for one shift: one day at a time in big rows (for children) or the whole week as a
 * table. "Uredi" switches to editing, where tapping a period picks its subject for every week.
 */
@Composable
fun WeekScreen(
    children: List<Child>,
    child: Child,
    onSelectChild: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBells: () -> Unit,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onTimetableChange: (Shift, Timetable) -> Unit,
    onSubjectUsed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val currentShift = child.shiftOn(today)
    var shift by rememberSaveable(child.id) { mutableStateOf(currentShift) }
    var dayIndex by rememberSaveable { mutableIntStateOf(SCHOOL_DAYS.indexOf(today.dayOfWeek).coerceAtLeast(0)) }
    var wholeWeek by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WeekEdit?>(null) }
    val timetable = child.timetable(shift)
    val rotates = child.shiftMode == ShiftMode.WEEKLY || child.shiftMode == ShiftMode.MONTHLY
    val day = SCHOOL_DAYS[dayIndex]
    val subjects = timetable.lessonsFor(day)

    LazyColumn(
        modifier.fillMaxSize().then(
            if (wholeWeek && !editMode) Modifier
            else Modifier.swipeBetween(
                onPrevious = { dayIndex = (dayIndex - 1).coerceAtLeast(0) },
                onNext = { dayIndex = (dayIndex + 1).coerceAtMost(SCHOOL_DAYS.size - 1) },
            ),
        ),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ChildTopRow(children, child.id, onSelectChild, title = "Nedelja") {
                if (!editMode) {
                    Surface(
                        onClick = { wholeWeek = !wholeWeek },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (wholeWeek) Icons.AutoMirrored.Filled.List else Icons.Filled.DateRange,
                                contentDescription = if (wholeWeek) "Prikaži po danu" else "Prikaži celu nedelju",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Surface(
                    onClick = { onEditModeChange(!editMode) },
                    shape = CircleShape,
                    color = if (editMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Text(
                        if (editMode) "Gotovo" else "✏️ Uredi",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (editMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                if (!editMode) {
                    Spacer(Modifier.width(8.dp))
                    SettingsButton(onOpenSettings)
                }
            }
        }
        if (editMode) {
            item {
                Text(
                    "Uređuješ raspored · ${child.name.ifBlank { "dete" }} · ${shift.shortLabel().lowercase()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (rotates) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillToggle(Shift.entries, shift, { "${it.icon()} ${it.shortLabel()}" }, { shift = it })
                    Text(
                        (if (child.shiftMode == ShiftMode.WEEKLY) "Ove nedelje" else "Ovog meseca") +
                            " ${currentShift.shortLabel().lowercase()}, " +
                            (if (child.shiftMode == ShiftMode.WEEKLY) "sledeće" else "sledećeg") +
                            " ${currentShift.other().shortLabel().lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        if (wholeWeek && !editMode) {
            item {
                WeekTable(timetable, today.dayOfWeek, onEdit = { onEditModeChange(true) }) {
                    dayIndex = SCHOOL_DAYS.indexOf(it)
                    wholeWeek = false
                }
            }
            item { BellTimes(timetable) }
            return@LazyColumn
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SCHOOL_DAYS.forEachIndexed { i, d ->
                    DayChip(
                        day = d,
                        count = timetable.lessonsFor(d).count { it.isNotBlank() },
                        selected = i == dayIndex,
                        isToday = d == today.dayOfWeek,
                        onClick = { dayIndex = i },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    day.label() + if (day == today.dayOfWeek) " · danas" else "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                val start = timetable.periods.getOrNull(subjects.indexOfFirst { it.isNotBlank() })
                val end = timetable.periods.getOrNull(subjects.indexOfLast { it.isNotBlank() })
                if (start != null && end != null) {
                    Text(
                        "${shift.icon()} ${start.start.label()}–${end.end.label()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val shown = if (editMode) subjects.size else subjects.indexOfLast { it.isNotBlank() } + 1
        if (shown == 0 && !editMode) {
            item {
                AppCard {
                    EmptyState("🗓️", "Nema časova", "Upiši časove za ovaj dan.", action = "✏️ Upiši časove", onAction = { onEditModeChange(true) })
                }
            }
        }
        for (index in 0 until shown) {
            val subject = subjects.getOrNull(index).orEmpty()
            item(key = "${day.name}-$index") {
                LessonRow(
                    label = timetable.periodLabel(index),
                    time = timetable.periods.getOrNull(index)?.label().orEmpty(),
                    number = timetable.periodNumber(index),
                    subject = subject,
                    editMode = editMode,
                    onClick = { if (editMode) editing = WeekEdit(day, index, subject) },
                    onClear = { onTimetableChange(shift, timetable.withLesson(day, index, "")) },
                )
            }
        }
        if (editMode) {
            if (subjects.size < timetable.periods.size) {
                item {
                    val index = subjects.size
                    DashedButton(
                        "+ Dodaj ${timetable.periodLabel(index)} (${timetable.periods[index].start.label()})",
                        onClick = { editing = WeekEdit(day, index, "") },
                    )
                }
            }
            item {
                NoteBanner(
                    "💡",
                    "Dodirni čas da promeniš predmet. Važi za svaki ${day.label().lowercase()} (${shift.shortLabel().lowercase()}). " +
                        "Za promenu samo jednog dana idi na Danas.",
                )
            }
            item {
                NoteBanner("⏰", "Vreme časova se menja u zvonima.") {
                    TextButton(onClick = onOpenBells) { Text("Zvona") }
                }
            }
        }
    }

    editing?.let { edit ->
        SubjectPickerSheet(
            overline = "${edit.day.label()}, ${timetable.periodLabel(edit.index)} · ${shift.shortLabel().lowercase()}",
            initial = edit.subject,
            subjects = child.subjects,
            onDismiss = { editing = null },
            onConfirm = {
                onTimetableChange(shift, timetable.withLesson(edit.day, edit.index, it))
                if (it.isNotBlank()) onSubjectUsed(it)
                editing = null
            },
            clearLabel = if (edit.subject.isEmpty()) null else "Slobodan čas",
        )
    }
}

/** A weekday button: short name and the number of classes, so empty days stand out. */
@Composable
private fun DayChip(day: DayOfWeek, count: Int, selected: Boolean, isToday: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primary else colors.surfaceContainerLowest,
        border = if (isToday && !selected) BorderStroke(2.dp, colors.primary) else null,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(day.shortLabel(), style = MaterialTheme.typography.titleMedium, color = if (selected) colors.onPrimary else colors.onSurface)
            Text(
                if (count == 0) "—" else "$count č.",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) colors.onPrimary.copy(alpha = 0.85f) else colors.onSurfaceVariant,
            )
        }
    }
}

/** One period of the selected day; in edit mode it has an outline and a button that frees the period. */
@Composable
private fun LessonRow(
    label: String,
    time: String,
    number: String,
    subject: String,
    editMode: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    val (kind, name) = ExtraClass.parse(subject)
    val free = subject.isBlank()
    val color = when {
        free -> Color.Transparent
        editMode -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> tintOf(subjectStyle(subject).color)
    }
    Surface(
        onClick = onClick,
        enabled = editMode,
        shape = MaterialTheme.shapes.large,
        color = color,
        border = if (editMode || free) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 8.dp, end = 12.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (free) {
                Box(Modifier.size(44.dp).border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Text("+", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                SubjectBadge(subject, size = 44.dp)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                if (kind != null) {
                    Text(kind.prefix.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    if (free) "Slobodan čas" else name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (free) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text("$label · $time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (editMode && !free) {
                Surface(onClick = onClear, shape = CircleShape, color = MaterialTheme.colorScheme.background, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("×", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Text(number, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** The whole week like the timetable children paste into their notebook; tapping a day opens it. */
@Composable
private fun WeekTable(timetable: Timetable, today: DayOfWeek, onEdit: () -> Unit, onDay: (DayOfWeek) -> Unit) {
    val rows = SCHOOL_DAYS.maxOf { d -> timetable.lessonsFor(d).indexOfLast { it.isNotBlank() } + 1 }
    if (rows == 0) {
        AppCard { EmptyState("🗓️", "Raspored još nije unet", "Upiši časove za svaki dan u nedelji.", action = "✏️ Upiši raspored", onAction = onEdit) }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.width(22.dp))
            SCHOOL_DAYS.forEach { d ->
                Text(
                    d.shortLabel(),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    color = if (d == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (index in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    timetable.periodNumber(index),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(22.dp),
                )
                SCHOOL_DAYS.forEach { d ->
                    val subject = timetable.lessonsFor(d).getOrNull(index).orEmpty()
                    val cell = Modifier.weight(1f).heightIn(min = 54.dp).clip(MaterialTheme.shapes.extraSmall)
                    if (subject.isBlank()) {
                        Box(cell.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraSmall).clickable { onDay(d) })
                    } else {
                        Column(
                            cell
                                .background(tintOf(subjectStyle(subject).color))
                                .then(if (d == today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall) else Modifier)
                                .clickable { onDay(d) }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(subjectStyle(subject).emoji, fontSize = 18.sp)
                            Text(shortName(subject), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** Bell times for the shift, in two columns. */
@Composable
private fun BellTimes(timetable: Timetable) {
    val rows = SCHOOL_DAYS.maxOf { d -> timetable.lessonsFor(d).indexOfLast { it.isNotBlank() } + 1 }
    if (rows == 0) return
    AppCard(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        timetable.periods.take(rows).withIndex().chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                pair.forEach { (i, period) ->
                    Row(Modifier.weight(1f)) {
                        Text(timetable.periodLabel(i), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text(period.label(), style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}
