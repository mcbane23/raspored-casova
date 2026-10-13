package com.bane23.classschedule.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.SCHOOL_DAYS
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.data.shiftOn
import java.time.DayOfWeek
import java.time.LocalDate

private data class WeekEdit(val day: DayOfWeek, val index: Int, val subject: String)

/** The regular weekly timetable for one shift; tapping a period edits it permanently. */
@Composable
fun WeekScreen(
    child: Child,
    onTimetableChange: (Shift, Timetable) -> Unit,
    onSubjectUsed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentShift = child.shiftOn(LocalDate.now())
    var shift by rememberSaveable(child.id) { mutableStateOf(currentShift) }
    var editing by remember { mutableStateOf<WeekEdit?>(null) }
    val timetable = child.timetable(shift)
    val showBothShifts = child.shiftMode == ShiftMode.WEEKLY || child.shiftMode == ShiftMode.MONTHLY

    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(top = 16.dp)) {
                if (showBothShifts) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Shift.entries.forEach { s ->
                            FilterChip(selected = shift == s, onClick = { shift = s }, label = { Text(s.shortLabel()) })
                        }
                    }
                    Text(
                        "Trenutno: ${currentShift.label().lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(shift.label(), style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "Dodirni čas da upišeš ili promeniš predmet u redovnom rasporedu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        SCHOOL_DAYS.forEach { day ->
            item(key = day.name) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Text(
                            day.label(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        val subjects = timetable.lessonsFor(day)
                        timetable.periods.forEachIndexed { index, period ->
                            if (index > 0) HorizontalDivider()
                            val subject = subjects.getOrNull(index).orEmpty()
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { editing = WeekEdit(day, index, subject) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text(timetable.periodNumber(index), Modifier.width(32.dp))
                                Text(
                                    period.label(),
                                    Modifier.width(112.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    subject.ifBlank { "—" },
                                    color = if (subject.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Text("", Modifier.padding(bottom = 8.dp)) }
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
