package com.bane23.classschedule.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.ExtraClass
import com.bane23.classschedule.data.SCHOOL_DAYS
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.shiftOn
import java.time.DayOfWeek
import java.time.LocalDate

private data class WeekEdit(val day: DayOfWeek, val index: Int, val subject: String)

/** The regular weekly timetable for one shift, one day at a time; tapping a period edits it permanently. */
@Composable
fun WeekScreen(
    child: Child,
    onTimetableChange: (Shift, Timetable) -> Unit,
    onSubjectUsed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val currentShift = child.shiftOn(today)
    var shift by rememberSaveable(child.id) { mutableStateOf(currentShift) }
    var dayIndex by rememberSaveable { mutableIntStateOf(SCHOOL_DAYS.indexOf(today.dayOfWeek).coerceAtLeast(0)) }
    var editing by remember { mutableStateOf<WeekEdit?>(null) }
    val timetable = child.timetable(shift)
    val showBothShifts = child.shiftMode == ShiftMode.WEEKLY || child.shiftMode == ShiftMode.MONTHLY
    val day = SCHOOL_DAYS[dayIndex]
    val subjects = timetable.lessonsFor(day)

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "Nedeljni raspored",
                subtitle = if (showBothShifts) "Ove nedelje: ${currentShift.label().lowercase()}" else shift.label(),
            )
        }
        if (showBothShifts) {
            item {
                PillToggle(
                    options = Shift.entries,
                    selected = shift,
                    label = { (if (it == Shift.MORNING) "☀️ " else "🌙 ") + it.shortLabel() },
                    onSelect = { shift = it },
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                SectionLabel(day.label(), Modifier.weight(1f))
                Text(
                    "Dodirni čas za upis",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AppCard(contentPadding = PaddingValues(vertical = 8.dp)) {
                timetable.periods.forEachIndexed { index, period ->
                    val subject = subjects.getOrNull(index).orEmpty()
                    PeriodRow(
                        number = timetable.periodNumber(index),
                        time = period.label(),
                        subject = subject,
                        onClick = { editing = WeekEdit(day, index, subject) },
                    )
                }
            }
        }
    }

    editing?.let { edit ->
        SubjectPickerDialog(
            title = "${edit.day.label()}, ${timetable.periodLabel(edit.index)} (${shift.shortLabel().lowercase()})",
            initial = edit.subject,
            subjects = child.subjects,
            onDismiss = { editing = null },
            onConfirm = {
                onTimetableChange(shift, timetable.withLesson(edit.day, edit.index, it))
                onSubjectUsed(it)
                editing = null
            },
            extraAction = if (edit.subject.isEmpty()) null else "Slobodan čas (obriši predmet)" to {
                onTimetableChange(shift, timetable.withLesson(edit.day, edit.index, ""))
                editing = null
            },
        )
    }
}

/** A weekday button: short name and the number of classes, so empty days stand out. */
@Composable
private fun DayChip(day: DayOfWeek, count: Int, selected: Boolean, isToday: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primary else colors.surfaceContainerLowest,
        border = if (isToday && !selected) BorderStroke(2.dp, colors.primary) else null,
        shadowElevation = if (selected) 2.dp else 1.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                day.shortLabel(),
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) colors.onPrimary else colors.onSurface,
            )
            Text(
                if (count == 0) "—" else "$count",
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) colors.onPrimary.copy(alpha = 0.8f) else colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PeriodRow(number: String, time: String, subject: String, onClick: () -> Unit) {
    val (kind, name) = ExtraClass.parse(subject)
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (subject.isBlank()) {
                Box(
                    Modifier
                        .size(44.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                SubjectBadge(subject, size = 44.dp)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                if (kind != null) {
                    Text(kind.prefix.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                Text(
                    if (subject.isBlank()) "Slobodan čas" else name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (subject.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(number, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
