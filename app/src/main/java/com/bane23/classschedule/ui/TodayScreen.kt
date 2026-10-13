package com.bane23.classschedule.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.DayInfo
import com.bane23.classschedule.data.DayStatus
import com.bane23.classschedule.data.ExtraClass
import com.bane23.classschedule.data.Lesson
import com.bane23.classschedule.data.dayInfo
import com.bane23.classschedule.data.dayStatus
import com.bane23.classschedule.data.isPacked
import com.bane23.classschedule.data.lessonsOn
import com.bane23.classschedule.data.nextSchoolDayAfter
import com.bane23.classschedule.data.overrideOn
import com.bane23.classschedule.data.packNote
import com.bane23.classschedule.data.packSubjects
import com.bane23.classschedule.data.regularSubjectsOn
import com.bane23.classschedule.data.shiftOn
import com.bane23.classschedule.data.subjectsOn
import java.time.LocalDate
import java.time.LocalTime

/** Period being edited on the Today screen; [subject] is empty when adding a new class. */
private data class DayEdit(val index: Int, val subject: String)

/**
 * One child's day: a big card saying what is happening now, the day's classes as stops on a line (tapping one changes
 * it for that day only) and the packing list for the next school day.
 */
@Composable
fun TodayScreen(
    children: List<Child>,
    child: Child,
    onSelectChild: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onChangeDay: (LocalDate, Int, String) -> Unit,
    onResetDay: (LocalDate) -> Unit,
    onPacked: (LocalDate, String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var epochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var editing by remember { mutableStateOf<DayEdit?>(null) }
    val now = rememberNow()
    val date = LocalDate.ofEpochDay(epochDay)
    val isToday = date == now.toLocalDate()
    val clock = now.toLocalTime()
    val lessons = child.lessonsOn(date)
    val shift = child.shiftOn(date)
    val timetable = child.timetable(shift)
    val nextFreeIndex = child.subjectsOn(date).size
    val info = child.dayInfo(date)
    val noClassesReason = (info as? DayInfo.NoClasses)?.reason?.takeIf { child.overrideOn(date) == null }
    val packDay = child.nextSchoolDayAfter(date)

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ChildTopRow(children, child.id, onSelectChild, title = child.name.ifBlank { "Raspored časova" }) {
                SettingsButton(onOpenSettings)
            }
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
                HeroCard(
                    color = HolidayAmber,
                    eyebrow = if (isToday) "Danas" else date.dayOfWeek.label(),
                    emoji = if (date.dayOfWeek.value >= 6) "🛝" else eventEmoji(noClassesReason),
                    title = "Nema nastave",
                    detail = noClassesReason,
                )
            }
        } else {
            item {
                if (lessons.isEmpty()) {
                    AppCard {
                        if (child.regularSubjectsOn(date).any { it.isNotBlank() }) {
                            EmptyState("🎈", "Svi časovi su otkazani", "Za ovaj dan nema časova.")
                        } else {
                            EmptyState("🗓️", "Raspored još nije unet", "Upiši časove na kartici Nedelja.")
                        }
                    }
                } else {
                    DayHero(child, date, isToday, clock, lessons, timetable::periodLabel)
                }
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
            if (lessons.isNotEmpty() || nextFreeIndex < timetable.periods.size) {
                item {
                    StopsCard(
                        lessons = lessons,
                        title = (if (isToday) "Danas " else "") + lessonsCount(lessons.size),
                        clock = if (isToday) clock else null,
                        canAdd = nextFreeIndex < timetable.periods.size,
                        onLesson = { editing = DayEdit(it.index, it.subject) },
                        onAdd = { editing = DayEdit(nextFreeIndex, "") },
                    )
                }
            }
            val next = (dayStatus(lessons, clock) as? DayStatus.InClass)?.next
            if (isToday && next != null) {
                item {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sledeće", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            Text(next.period.start.label(), style = MaterialTheme.typography.titleSmall)
                        }
                        Text("${subjectStyle(next.subject).emoji} ${next.subject}", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }

        if (packDay != null && child.packSubjects(packDay).isNotEmpty()) {
            item { PackingCard(child, date, packDay, onPacked) }
        }
    }

    editing?.let { edit ->
        val isNew = edit.subject.isEmpty()
        SubjectPickerSheet(
            overline = "${timetable.periodLabel(edit.index)} · ${date.label()} · samo ovaj dan",
            initial = edit.subject,
            subjects = child.subjects,
            onDismiss = { editing = null },
            onConfirm = {
                if (it.isNotEmpty() || !isNew) onChangeDay(date, edit.index, it)
                editing = null
            },
            clearLabel = if (isNew) null else "Otkaži ovaj čas",
        )
    }
}

@Composable
private fun DaySwitcher(date: LocalDate, isToday: Boolean, onPrevious: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prethodni dan")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.label(), style = MaterialTheme.typography.titleMedium)
            if (!isToday) {
                Text(
                    "Nazad na danas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onToday).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Sledeći dan")
        }
    }
}

/** The big card: the current class with a countdown ring, a break, the time school starts, or "done for today". */
@Composable
private fun DayHero(child: Child, date: LocalDate, isToday: Boolean, clock: LocalTime, lessons: List<Lesson>, periodLabel: (Int) -> String) {
    val shift = child.shiftOn(date)
    val first = lessons.first()
    val last = lessons.last()
    if (!isToday) {
        HeroCard(
            color = AccentOrange,
            eyebrow = "${shift.icon()} ${shift.shortLabel()}",
            emoji = "🗓️",
            title = lessonsCount(lessons.size),
            detail = "${first.period.start.label()} – ${last.period.end.label()}",
        )
        return
    }
    when (val status = dayStatus(lessons, clock)) {
        is DayStatus.InClass -> HeroCard(
            color = subjectStyle(status.lesson.subject).color,
            eyebrow = "Sada · ${periodLabel(status.lesson.index)}",
            emoji = subjectStyle(status.lesson.subject).emoji,
            title = status.lesson.subject,
            detail = "do ${status.lesson.period.end.label()}",
            ringMinutes = status.minutesLeft,
            ringProgress = status.progress,
        )
        is DayStatus.Break -> HeroCard(
            color = BreakTeal,
            eyebrow = "Odmor ☕",
            emoji = subjectStyle(status.next.subject).emoji,
            title = status.next.subject,
            detail = "počinje u ${status.next.period.start.label()}",
            ringMinutes = status.minutes,
            ringProgress = status.progress,
        )
        is DayStatus.Before -> HeroCard(
            color = AccentOrange,
            eyebrow = "${shift.icon()} ${shift.shortLabel()} · danas ${lessonsCount(lessons.size)}",
            emoji = "🎒",
            title = "Škola u ${first.period.start.label()}",
            detail = "Prvi čas: ${subjectStyle(first.subject).emoji} ${first.subject} · za ${durationLabel(status.minutes)}",
        )
        DayStatus.Done, null -> HeroCard(
            color = DoneGreen,
            eyebrow = "🎉 Bravo!",
            emoji = "🏆",
            title = "Gotovo za danas!",
            detail = "${lessonsCount(lessons.size)} završeno. Sutra spakuj torbu.",
        )
    }
}

/**
 * The day's classes as stops on a line: done ones are faded with a check, the current one is outlined and the line
 * fills up to it. [clock] is null for days other than today.
 */
@Composable
private fun StopsCard(
    lessons: List<Lesson>,
    title: String,
    clock: LocalTime?,
    canAdd: Boolean,
    onLesson: (Lesson) -> Unit,
    onAdd: () -> Unit,
) {
    AppCard(contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 4.dp)) {
        Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            lessons.lastOrNull()?.let { Text("🏁 ${it.period.end.label()}", style = MaterialTheme.typography.titleSmall) }
        }
        if (lessons.isNotEmpty()) {
            val reached = if (clock == null) 0f else when (val s = dayStatus(lessons, clock)) {
                is DayStatus.InClass -> lessons.indexOf(s.lesson) + s.progress
                is DayStatus.Break -> lessons.indexOf(s.next) - 1 + s.progress
                DayStatus.Done -> lessons.size - 1f
                else -> 0f
            }
            val progress = reached / (lessons.size - 1).coerceAtLeast(1)
            val scroll = lessons.size > 6
            Box(Modifier.fillMaxWidth().then(if (scroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier)) {
                Box(Modifier.matchParentSizeWidth(scroll, lessons.size).padding(horizontal = 23.dp).padding(top = 21.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant)) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(MaterialTheme.colorScheme.primary))
                }
                Row(
                    Modifier.matchParentSizeWidth(scroll, lessons.size),
                    horizontalArrangement = if (scroll) Arrangement.spacedBy(10.dp) else Arrangement.SpaceBetween,
                ) {
                    lessons.forEach { lesson ->
                        val done = clock != null && clock >= lesson.period.end
                        val current = clock != null && clock >= lesson.period.start && !done
                        Stop(lesson, done, current) { onLesson(lesson) }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Dodirni čas za izmenu samo ovog dana",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
            if (canAdd) TextButton(onClick = onAdd) { Text("+ Dodaj čas") }
        }
    }
}

/** With few classes the stops spread over the full width; with many they scroll at a fixed spacing. */
private fun Modifier.matchParentSizeWidth(scroll: Boolean, count: Int): Modifier =
    if (scroll) width((46 * count + 10 * (count - 1)).dp) else fillMaxWidth()

@Composable
private fun Stop(lesson: Lesson, done: Boolean, current: Boolean, onClick: () -> Unit) {
    val style = subjectStyle(lesson.subject)
    Column(
        Modifier.width(46.dp).clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(if (current) 46.dp else 42.dp)
                .alpha(if (done) 0.5f else 1f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(BorderStroke(3.dp, if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest), CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(tintOf(style.color, 0.25f, 0.38f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (done) "✓" else style.emoji, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(lesson.period.start.label(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExtraClass.parse(lesson.subject).first?.let { kind ->
            Text(kind.prefix.take(3) + ".", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** "Sutra u torbu": the subjects of the next school day, each with what to bring, ticked off by tapping. */
@Composable
private fun PackingCard(child: Child, from: LocalDate, packDay: LocalDate, onPacked: (LocalDate, String, Boolean) -> Unit) {
    val heading = if (packDay == from.plusDays(1)) "Sutra u torbu" else "Za ${packDay.dayOfWeek.label().lowercase()} u torbu"
    AppCard(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 4.dp)) {
            Text("🎒 $heading", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(6.dp))
            Text(
                "(${packDay.dayOfWeek.label().lowercase()})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        child.packSubjects(packDay).forEach { subject ->
            val packed = child.isPacked(packDay, subject)
            val note = child.packNote(subject)
            Surface(onClick = { onPacked(packDay, subject, !packed) }, color = Color.Transparent, shape = MaterialTheme.shapes.small) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(if (packed) MaterialTheme.colorScheme.tertiary else tintOf(subjectStyle(subject).color, 0.30f, 0.38f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (packed) "✓" else subjectStyle(subject).emoji,
                            fontSize = 18.sp,
                            color = if (packed) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp).alpha(if (packed) 0.55f else 1f)) {
                        Text(
                            subject,
                            style = MaterialTheme.typography.titleSmall,
                            textDecoration = if (packed) TextDecoration.LineThrough else null,
                        )
                        if (note.isNotBlank()) {
                            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
