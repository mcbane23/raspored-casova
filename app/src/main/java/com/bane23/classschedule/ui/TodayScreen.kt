package com.bane23.classschedule.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.DayInfo
import com.bane23.classschedule.data.ExtraClass
import com.bane23.classschedule.data.Lesson
import com.bane23.classschedule.data.dayInfo
import com.bane23.classschedule.data.lessonsOn
import com.bane23.classschedule.data.overrideOn
import com.bane23.classschedule.data.regularSubjectsOn
import com.bane23.classschedule.data.shiftOn
import com.bane23.classschedule.data.subjectsOn
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

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
    val now = rememberNow()
    val date = LocalDate.ofEpochDay(epochDay)
    val isToday = date == now.toLocalDate()
    val lessons = child.lessonsOn(date)
    val timetable = child.timetable(child.shiftOn(date))
    val periods = timetable.periods
    val nextFreeIndex = child.subjectsOn(date).size
    val info = child.dayInfo(date)
    val noClassesReason = (info as? DayInfo.NoClasses)?.reason?.takeIf { child.overrideOn(date) == null }
    val clock = now.toLocalTime()

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            ScreenHeader(
                title = child.name.ifBlank { "Raspored časova" },
                subtitle = child.school.ifBlank { null },
                compact = true,
            )
        }
        item {
            DaySwitcher(
                date = date,
                isToday = isToday,
                onPrevious = { epochDay-- },
                onNext = { epochDay++ },
                onToday = { epochDay = LocalDate.now().toEpochDay() },
            )
        }

        if (noClassesReason != null) {
            item {
                AppCard {
                    EmptyState(
                        emoji = if (date.dayOfWeek.value >= 6) "🛝" else eventEmoji(noClassesReason),
                        title = "Nema nastave",
                        hint = noClassesReason,
                    )
                }
            }
            return@LazyColumn
        }

        item {
            StatusCard(
                child = child,
                date = date,
                isToday = isToday,
                clock = clock,
                lessons = lessons,
                periodLabel = { timetable.periodLabel(it) },
            )
        }

        if (info is DayInfo.SchoolDay && info.note != null) {
            item { NoteBanner("ℹ️", info.note) }
        }
        if (child.overrideOn(date) != null) {
            item {
                NoteBanner("✏️", "Raspored je izmenjen samo za ovaj dan.") {
                    TextButton(onClick = { onResetDay(date) }) { Text("Vrati redovan") }
                }
            }
        }

        if (lessons.isEmpty()) {
            item {
                AppCard {
                    if (child.regularSubjectsOn(date).any { it.isNotBlank() }) {
                        EmptyState("🎈", "Svi časovi su otkazani", "Za ovaj dan nema časova.")
                    } else {
                        EmptyState("🗓️", "Raspored još nije unet", "Upiši časove na kartici Nedelja.")
                    }
                }
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    SectionLabel("Časovi", Modifier.weight(1f))
                    Text(
                        "Dodirni čas za izmenu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(lessons, key = { it.index }) { lesson ->
                val state = when {
                    !isToday -> LessonState.UPCOMING
                    clock >= lesson.period.end -> LessonState.DONE
                    clock >= lesson.period.start -> LessonState.NOW
                    else -> LessonState.UPCOMING
                }
                LessonRow(
                    lesson = lesson,
                    label = timetable.periodLabel(lesson.index),
                    state = state,
                    onClick = { editing = DayEdit(lesson.index, lesson.subject) },
                )
            }
        }
        if (nextFreeIndex < periods.size) {
            item {
                FilledTonalButton(
                    onClick = { editing = DayEdit(nextFreeIndex, "") },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Dodaj čas samo za ovaj dan")
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
private fun DaySwitcher(date: LocalDate, isToday: Boolean, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prethodni dan")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.label(), style = MaterialTheme.typography.titleMedium)
            if (isToday) {
                Text("Danas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    "Nazad na danas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onToday).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        FilledTonalIconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sledeći dan")
        }
    }
}

/**
 * The colored card at the top of the day: what is happening right now (current class, break, finished) for
 * today, or a summary of the day for other dates.
 */
@Composable
private fun StatusCard(
    child: Child,
    date: LocalDate,
    isToday: Boolean,
    clock: LocalTime,
    lessons: List<Lesson>,
    periodLabel: (Int) -> String,
) {
    val first = lessons.firstOrNull()
    val last = lessons.lastOrNull()
    val current = if (isToday) lessons.firstOrNull { clock >= it.period.start && clock < it.period.end } else null
    val next = if (isToday) lessons.firstOrNull { clock < it.period.start } else null

    val (eyebrow, title, detail) = when {
        first == null || last == null -> Triple(child.shiftOn(date).label(), "Nema časova", null)
        !isToday -> Triple(
            child.shiftOn(date).label(),
            "${lessons.size} ${lessonsWord(lessons.size)}",
            "${first.period.start.label()} – ${last.period.end.label()}",
        )
        current != null -> Triple(
            "Sada · ${periodLabel(current.index)}",
            "${subjectStyle(current.subject).emoji} ${current.subject}",
            "Još ${durationLabel(minutesBetween(clock, current.period.end))}" +
                (next?.let { " · zatim ${it.subject}" } ?: " · poslednji čas"),
        )
        clock < first.period.start -> Triple(
            child.shiftOn(date).label(),
            "Nastava počinje u ${first.period.start.label()}",
            "Prvi čas: ${first.subject} · za ${durationLabel(minutesBetween(clock, first.period.start))}",
        )
        next != null -> Triple(
            "Odmor ☕",
            "Sledeći: ${next.subject}",
            "U ${next.period.start.label()} · za ${durationLabel(minutesBetween(clock, next.period.start))}",
        )
        else -> Triple(
            child.shiftOn(date).label(),
            "Nastava je završena 🎉",
            "Bravo! Danas je bilo ${lessons.size} ${lessonsWord(lessons.size)}.",
        )
    }
    val onColor = MaterialTheme.colorScheme.onPrimary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = onColor.copy(alpha = 0.8f))
            Text(title, style = MaterialTheme.typography.titleLarge, color = onColor)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = onColor.copy(alpha = 0.85f))
            }
            if (current != null) {
                val total = minutesBetween(current.period.start, current.period.end).coerceAtLeast(1)
                val done = minutesBetween(current.period.start, clock)
                LinearProgressIndicator(
                    progress = { (done.toFloat() / total).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = onColor.copy(alpha = 0.25f),
                    drawStopIndicator = {},
                )
            }
            if (last != null) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tag("🏁 Kraj u ${last.period.end.label()}", onColor.copy(alpha = 0.18f), onColor)
                    if (child.alarmEnabled) {
                        val alarm = last.period.end.minusMinutes(child.alarmMinutesBefore.toLong())
                        Tag("🔔 ${alarm.label()}", onColor.copy(alpha = 0.18f), onColor)
                    }
                }
            }
        }
    }
}

/** Whole minutes from [from] to [to], rounded up so a countdown never shows "0 min" early. */
private fun minutesBetween(from: LocalTime, to: LocalTime): Long =
    ((Duration.between(from, to).seconds + 59) / 60).coerceAtLeast(0)

/** "1 čas", "3 časa", "6 časova". */
private fun lessonsWord(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "čas"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "časa"
    else -> "časova"
}

private enum class LessonState { DONE, NOW, UPCOMING }

@Composable
private fun LessonRow(lesson: Lesson, label: String, state: LessonState, onClick: () -> Unit) {
    val (kind, subject) = ExtraClass.parse(lesson.subject)
    val isNow = state == LessonState.NOW
    Row(
        Modifier.fillMaxWidth().alpha(if (state == LessonState.DONE) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(48.dp)) {
            Text(lesson.period.start.label(), style = MaterialTheme.typography.titleSmall)
            Text(
                lesson.period.end.label(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            onClick = onClick,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.large,
            color = if (isNow) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
            border = if (isNow) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            shadowElevation = if (isNow) 0.dp else 1.dp,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SubjectBadge(lesson.subject, size = 40.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    if (kind != null) {
                        Text(
                            kind.prefix.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Text(subject, style = MaterialTheme.typography.titleMedium)
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when (state) {
                    LessonState.NOW -> Tag("SADA", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    LessonState.DONE -> Text("✓", fontSize = 18.sp, color = MaterialTheme.colorScheme.tertiary)
                    LessonState.UPCOMING -> Unit
                }
            }
        }
    }
}

/** Soft yellow banner for notes such as a swapped timetable day or a one-day change. */
@Composable
private fun NoteBanner(emoji: String, text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.size(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
        )
        action?.invoke()
    }
}
