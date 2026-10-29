package com.bane23.classschedule.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.ActivitySession
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.DayInfo
import com.bane23.classschedule.data.DayStatus
import com.bane23.classschedule.data.ExtraClass
import com.bane23.classschedule.data.Extracurricular
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
import com.bane23.classschedule.data.sessionsOn
import com.bane23.classschedule.data.shiftOn
import com.bane23.classschedule.data.subjectsOn
import java.time.LocalDate
import java.time.LocalTime

/** Period being edited on the Today screen; [subject] is empty when adding a new class. */
private data class DayEdit(val index: Int, val subject: String)

/**
 * One child's day: a big card saying what is happening now, the day's classes as stops on a line (tapping one changes
 * it for that day only), the day's extracurricular activities and the packing list for the next school day.
 */
@Composable
fun TodayScreen(
    children: List<Child>,
    child: Child,
    onSelectChild: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onEditTimetable: () -> Unit,
    onChangeDay: (LocalDate, Int, String) -> Unit,
    onResetDay: (LocalDate) -> Unit,
    onPacked: (LocalDate, String, Boolean) -> Unit,
    onSaveActivity: (Extracurricular) -> Unit,
    onDeleteActivity: (String) -> Unit,
    showLessonHint: Boolean,
    onLessonHintSeen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The day the user moved to, or null to follow today, so the screen turns to the new day after midnight.
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<DayEdit?>(null) }
    var details by remember { mutableStateOf<Lesson?>(null) }
    // The activity open in the editor; a new one has an empty title.
    var activityEdit by remember { mutableStateOf<Extracurricular?>(null) }
    var addingActivity by remember { mutableStateOf(false) }
    // The weekly activity session open for a one-time cancel or move.
    var session by remember { mutableStateOf<ActivitySession?>(null) }
    val now = rememberNow()
    val date = pickedDay?.let(LocalDate::ofEpochDay) ?: now.toLocalDate()
    val isToday = date == now.toLocalDate()
    fun moveBy(days: Long) {
        pickedDay = date.plusDays(days).toEpochDay().takeIf { it != now.toLocalDate().toEpochDay() }
    }
    val offerUndo = LocalUndo.current
    val clock = now.toLocalTime()
    val lessons = child.lessonsOn(date)
    val shift = child.shiftOn(date)
    val timetable = child.timetable(shift)
    val nextFreeIndex = child.subjectsOn(date).size
    val info = child.dayInfo(date)
    // A day off wins over a one-day change made for that day before it was added.
    val noClassesReason = (info as? DayInfo.NoClasses)?.reason
    // Packing is about the real tomorrow; other days would tick the bag for the wrong date and reset today's ticks.
    val packDay = if (isToday) child.nextSchoolDayAfter(date) else null

    val warning = rememberReminderWarning(listOf(child))

    LazyColumn(
        modifier.fillMaxSize().swipeBetween(onPrevious = { moveBy(-1) }, onNext = { moveBy(1) }),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ChildTopRow(children, child.id, onSelectChild, title = child.name.ifBlank { "Raspored časova" }) {
                SettingsButton(onOpenSettings)
            }
        }
        if (warning != null) {
            item { ReminderProblemBanner(warning) }
        }
        item {
            DaySwitcher(
                date = date,
                isToday = isToday,
                onPrevious = { moveBy(-1) },
                onNext = { moveBy(1) },
                onToday = { pickedDay = null },
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
                    footer = child.nextActivityOn(date, if (isToday) clock else LocalTime.MIN)
                        ?.let { "Zatim ${it.kind.emoji()} ${it.title} u ${it.start.label()}" },
                )
            }
        } else {
            item {
                if (lessons.isEmpty()) {
                    AppCard {
                        if (child.regularSubjectsOn(date).any { it.isNotBlank() }) {
                            EmptyState("🎈", "Svi časovi su otkazani", "Za ovaj dan nema časova.")
                        } else {
                            EmptyState(
                                "🗓️",
                                "Raspored još nije unet",
                                "Upiši časove za svaki dan u nedelji.",
                                action = "✏️ Upiši raspored",
                                onAction = onEditTimetable,
                            )
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
                        TextButton(colors = accentTextButton(), onClick = { onResetDay(date) }) { Text("Vrati redovan") }
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
                        showHint = showLessonHint,
                        onLesson = {
                            details = it
                            if (showLessonHint) onLessonHintSeen()
                        },
                        onAdd = { editing = DayEdit(nextFreeIndex, "") },
                    )
                }
            }
        }

        if (child.sessionsOn(date).isNotEmpty()) item {
            ActivitiesCard(
                child,
                date,
                clock = if (isToday) clock else null,
                onOpen = { opened ->
                    // A moved session carries the moved times; the sheets work on the activity as saved.
                    val saved = child.activities.find { it.id == opened.activity.id } ?: opened.activity
                    if (saved.weekly) session = opened.copy(activity = saved) else activityEdit = saved
                },
                onAdd = { addingActivity = true },
            )
        }

        if (packDay != null && child.packSubjects(packDay).isNotEmpty()) {
            item { PackingCard(child, date, packDay, onPacked) }
        }
    }

    details?.let { lesson ->
        LessonDetailsSheet(
            lesson = lesson,
            periodLabel = timetable.periodLabel(lesson.index),
            note = child.packNote(lesson.subject),
            dateLabel = if (isToday) "danas" else "za ${date.accusativeLabel()}",
            onDismiss = { details = null },
            onChange = {
                details = null
                editing = DayEdit(lesson.index, lesson.subject)
            },
        )
    }

    session?.let { open ->
        ActivitySessionSheet(
            activity = open.activity,
            regularDay = open.regularDay,
            onDismiss = { session = null },
            onChange = {
                onSaveActivity(it)
                session = null
            },
            onEditAll = {
                activityEdit = open.activity
                session = null
            },
        )
    }

    if (addingActivity || activityEdit != null) {
        val initial = activityEdit
        ActivityEditorSheet(
            child = child,
            initial = initial,
            defaultDate = date,
            onDismiss = {
                addingActivity = false
                activityEdit = null
            },
            onSave = {
                onSaveActivity(it)
                addingActivity = false
                activityEdit = null
            },
            onDelete = initial?.let { activity ->
                {
                    onDeleteActivity(activity.id)
                    activityEdit = null
                    offerUndo("Aktivnost „${activity.title}” je obrisana") { onSaveActivity(activity) }
                }
            },
        )
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
                if (it.isEmpty() && !isNew) {
                    offerUndo("${timetable.periodLabel(edit.index)} je otkazan za ${date.accusativeLabel()}") {
                        onChangeDay(date, edit.index, edit.subject)
                    }
                }
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
            // The date slides in from the side the user moved to, so a swipe visibly changes the day.
            AnimatedContent(
                targetState = date,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it / 2 else -it / 2 } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (forward) -it / 2 else it / 2 } + fadeOut())
                },
                label = "day",
            ) { shown ->
                Text(shown.label(), style = MaterialTheme.typography.titleMedium)
            }
            if (!isToday) {
                Surface(onClick = onToday, shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        "Danas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
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
        // Another day: when it starts and with what, the first thing a child checks the evening before.
        HeroCard(
            color = AccentOrange,
            eyebrow = "${shift.icon()} ${shift.shortLabel()} · ${lessonsCount(lessons.size)}",
            emoji = subjectStyle(first.subject).emoji,
            title = "Škola u ${first.period.start.label()}",
            detail = "Prvi čas: ${first.subject} · kraj u ${last.period.end.label()}",
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
            footer = status.next?.let { "Zatim ${subjectStyle(it.subject).emoji} ${it.subject} u ${it.period.start.label()}" }
                ?: "🏁 Poslednji čas danas",
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
            eyebrow = "🏁 Kraj nastave",
            emoji = "🏆",
            title = "Gotovo za danas!",
            detail = "Završeno: ${lessonsCount(lessons.size)}." + packHint(date, child.nextSchoolDayAfter(date)),
            footer = child.nextActivityOn(date, clock)?.let { "Zatim ${it.kind.emoji()} ${it.title} u ${it.start.label()}" },
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
    showHint: Boolean,
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
            // Stops widen with the phone's font size so short names and times are not cut off.
            val stopWidth = 48.dp * LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val rowWidth = stopWidth * lessons.size + 10.dp * (lessons.size - 1)
                val scroll = rowWidth > maxWidth
                val lineWidth = if (scroll) Modifier.width(rowWidth) else Modifier.fillMaxWidth()
                Box(Modifier.fillMaxWidth().then(if (scroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier)) {
                    Box(lineWidth.padding(horizontal = stopWidth / 2 - 1.dp).padding(top = 21.dp).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant)) {
                        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(MaterialTheme.colorScheme.primary))
                    }
                    Row(
                        lineWidth,
                        horizontalArrangement = if (scroll) Arrangement.spacedBy(10.dp) else Arrangement.SpaceBetween,
                    ) {
                        lessons.forEach { lesson ->
                            val done = clock != null && clock >= lesson.period.end
                            val current = clock != null && clock >= lesson.period.start && !done
                            Stop(lesson, stopWidth, done, current) { onLesson(lesson) }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).padding(start = 6.dp)) {
                if (showHint) {
                    Text("Dodirni čas za detalje", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (canAdd) TextButton(colors = accentTextButton(), onClick = onAdd) { Text("+ Dodaj čas") }
        }
    }
}

/** One class on the line; with few classes the stops spread over the full width, with many they scroll. */
@Composable
private fun Stop(lesson: Lesson, width: Dp, done: Boolean, current: Boolean, onClick: () -> Unit) {
    val style = subjectStyle(lesson.subject)
    val state = when {
        done -> ", završen"
        current -> ", sada"
        else -> ""
    }
    Column(
        Modifier
            .width(width)
            .clip(MaterialTheme.shapes.small)
            .clearAndSetSemantics {
                contentDescription = "${lesson.subject}, ${lesson.period.start.label()}$state"
                role = Role.Button
                this.onClick {
                    onClick()
                    true
                }
            }
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp),
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
        Text(shortName(lesson.subject), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        Text(lesson.period.start.label(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ExtraClass.parse(lesson.subject).first?.let { kind ->
            Text(kind.prefix.take(3) + ".", style = MaterialTheme.typography.labelSmall, color = accentText())
        }
    }
}

/** "Sutra u torbu": the subjects of the next school day, each with what to bring, ticked off by tapping. */
@Composable
private fun PackingCard(child: Child, from: LocalDate, packDay: LocalDate, onPacked: (LocalDate, String, Boolean) -> Unit) {
    val heading = if (packDay == from.plusDays(1)) "Sutra u torbu" else "Za ${packDay.dayOfWeek.accusative()} u torbu"
    val subjects = child.packSubjects(packDay)
    val packedCount = subjects.count { child.isPacked(packDay, it) }
    val allPacked = packedCount == subjects.size
    val haptic = LocalHapticFeedback.current
    AppCard(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Text("🎒 $heading", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(6.dp))
            Text(
                "(${packDay.dayOfWeek.label().lowercase()})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Tag(
                "$packedCount / ${subjects.size}",
                if (allPacked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.background,
                if (allPacked) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val progress by animateFloatAsState(packedCount.toFloat() / subjects.size.coerceAtLeast(1), label = "packed")
        Box(Modifier.fillMaxWidth().padding(bottom = 6.dp).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant)) {
            Box(Modifier.fillMaxWidth(progress).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
        }
        AnimatedVisibility(allPacked) {
            Text(
                "🎉 Torba je spremna!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        subjects.forEach { subject ->
            val packed = child.isPacked(packDay, subject)
            val note = child.packNote(subject)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(if (packed) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                    onPacked(packDay, subject, !packed)
                },
                color = Color.Transparent,
                shape = MaterialTheme.shapes.small,
                // Announced as a checkbox: "Matematika, označeno".
                modifier = Modifier.semantics(mergeDescendants = true) {
                    role = Role.Checkbox
                    toggleableState = ToggleableState(packed)
                },
            ) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clearAndSetSemantics {}
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

/**
 * What a tapped class is: subject, time and what to bring. Changing it for the day is one more deliberate tap, so a
 * child looking at their day can't cancel a class by accident.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonDetailsSheet(
    lesson: Lesson,
    periodLabel: String,
    note: String,
    dateLabel: String,
    onDismiss: () -> Unit,
    onChange: () -> Unit,
) {
    val (kind, name) = ExtraClass.parse(lesson.subject)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SubjectBadge(lesson.subject, size = 72.dp)
            if (kind != null) {
                Text(kind.prefix.uppercase(), style = MaterialTheme.typography.labelLarge, color = accentText())
            }
            Text(name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                "$periodLabel · ${lesson.period.label()}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                NoteBanner("🎒", "Nosi: $note")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onChange, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("✏️ Izmeni ili otkaži samo $dateLabel", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
