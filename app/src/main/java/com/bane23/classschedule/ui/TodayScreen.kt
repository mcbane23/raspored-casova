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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.DayInfo
import com.bane23.classschedule.data.dayInfo
import com.bane23.classschedule.data.lessonsOn
import com.bane23.classschedule.data.overrideOn
import com.bane23.classschedule.data.regularSubjectsOn
import com.bane23.classschedule.data.shiftOn
import com.bane23.classschedule.data.subjectsOn
import java.time.LocalDate

/** Period being edited on the Today screen; [subject] is empty when adding a new class. */
private data class DayEdit(val index: Int, val subject: String)

@Composable
fun TodayScreen(
    child: Child,
    onChangeDay: (LocalDate, Int, String) -> Unit,
    onResetDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var epochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var editing by remember { mutableStateOf<DayEdit?>(null) }
    val date = LocalDate.ofEpochDay(epochDay)
    val isToday = date == LocalDate.now()
    val lessons = child.lessonsOn(date)
    val timetable = child.timetable(child.shiftOn(date))
    val periods = timetable.periods
    val nextFreeIndex = child.subjectsOn(date).size

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { epochDay-- }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prethodni dan")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.label(), style = MaterialTheme.typography.titleMedium)
                if (isToday) {
                    Text("Danas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                } else {
                    TextButton(onClick = { epochDay = LocalDate.now().toEpochDay() }) { Text("Nazad na danas") }
                }
            }
            IconButton(onClick = { epochDay++ }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sledeći dan")
            }
        }

        val info = child.dayInfo(date)
        if (info is DayInfo.NoClasses && child.overrideOn(date) == null) {
            EmptyMessage("Nema nastave: ${info.reason}.")
            return@Column
        }

        Text(child.shiftOn(date).label(), style = MaterialTheme.typography.titleSmall)
        if (info is DayInfo.SchoolDay && info.note != null) {
            Text(info.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }

        if (child.overrideOn(date) != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Raspored je izmenjen samo za ovaj dan.", Modifier.weight(1f))
                    TextButton(onClick = { onResetDay(date) }) { Text("Vrati redovan") }
                }
            }
        }

        if (lessons.isEmpty()) {
            EmptyMessage(
                if (child.regularSubjectsOn(date).any { it.isNotBlank() }) "Svi časovi za ovaj dan su otkazani."
                else "Raspored za ovaj dan još nije unet. Dodaj časove na kartici Nedelja.",
            )
        } else {
            Text(
                "Prvi čas počinje u ${lessons.first().period.start.label()}, " +
                    "nastava se završava u ${lessons.last().period.end.label()}.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Text(
                "Dodirni čas da ga zameniš ili otkažeš samo za ovaj dan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lessons, key = { it.index }) { lesson ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { editing = DayEdit(lesson.index, lesson.subject) },
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(timetable.periodNumber(lesson.index), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(lesson.subject, style = MaterialTheme.typography.titleMedium)
                            Text(lesson.period.label(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (nextFreeIndex < periods.size) {
                item {
                    OutlinedButton(onClick = { editing = DayEdit(nextFreeIndex, "") }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Dodaj čas samo za ovaj dan")
                    }
                }
            }
        }
    }

    editing?.let { edit ->
        val isNew = edit.subject.isEmpty()
        SubjectPickerDialog(
            title = "${timetable.periodLabel(edit.index)}, ${date.label()}",
            initial = edit.subject,
            subjects = child.subjects,
            onDismiss = { editing = null },
            onConfirm = {
                onChangeDay(date, edit.index, it)
                editing = null
            },
            extraAction = if (isNew) null else "Otkaži ovaj čas" to {
                onChangeDay(date, edit.index, "")
                editing = null
            },
        )
    }
}

@Composable
fun EmptyMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}
