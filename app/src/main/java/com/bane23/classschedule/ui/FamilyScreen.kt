package com.bane23.classschedule.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.CalendarEvent
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.DayInfo
import com.bane23.classschedule.data.DayStatus
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.activitiesOn
import com.bane23.classschedule.data.calendarEvents
import com.bane23.classschedule.data.currentShiftDate
import com.bane23.classschedule.data.dayInfo
import com.bane23.classschedule.data.dayStatus
import com.bane23.classschedule.data.lessonsOn
import com.bane23.classschedule.data.nextAlarm
import com.bane23.classschedule.data.shiftOn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** First tab when there are two or more children: every child's day on one screen, for parents. */
@Composable
fun FamilyScreen(
    children: List<Child>,
    onOpenChild: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onAlarmChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = rememberNow()
    val today = now.toLocalDate()
    val upcoming = children.flatMap { it.calendarEvents() }
        .filter { it.endEpochDay >= today.toEpochDay() }
        .distinctBy { it.title to it.startEpochDay }
        // Children imported from one QR code share activity ids, so also drop repeats of the list key below.
        .distinctBy { it.id to it.startEpochDay }
        .sortedBy { it.startEpochDay }
        .take(3)

    val warning = rememberReminderWarning(children)

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScreenHeader("Porodica", overline = today.label(), modifier = Modifier.weight(1f))
                SettingsButton(onOpenSettings)
            }
        }
        if (warning != null) {
            item { ReminderProblemBanner(warning) }
        }
        items(children, key = { it.id }) { child ->
            KidCard(child, now, onClick = { onOpenChild(child.id) }, onAlarmChange = { onAlarmChange(child.id, it) })
        }
        if (upcoming.isNotEmpty()) {
            item { SectionLabel("Uskoro") }
            items(upcoming, key = { "${it.id}-${it.startEpochDay}" }) { EventTicket(it, today) }
        }
    }
}

@Composable
private fun KidCard(child: Child, now: LocalDateTime, onClick: () -> Unit, onAlarmChange: (Boolean) -> Unit) {
    val today = now.toLocalDate()
    val clock = now.toLocalTime()
    val lessons = child.lessonsOn(today)
    val status = dayStatus(lessons, clock)
    val info = child.dayInfo(today)
    // On a weekend a weekly rotation shows the coming week's shift, as the Week tab does.
    val shift = child.shiftOn(child.currentShiftDate(today))
    val colors = MaterialTheme.colorScheme
    val bold = SpanStyle(fontWeight = FontWeight.ExtraBold)

    val line = buildAnnotatedString {
        when {
            info is DayInfo.NoClasses -> {
                append("${eventEmoji(info.reason)} Nema nastave: ")
                withStyle(bold) { append(info.reason) }
            }
            status == null -> append("Danas nema časova")
            status is DayStatus.InClass -> {
                append("Sada ")
                withStyle(bold) { append("${subjectStyle(status.lesson.subject).emoji} ${status.lesson.subject}") }
                append(", još ${durationLabel(status.minutesLeft)}")
            }
            status is DayStatus.Break -> {
                append("Odmor, zatim ")
                withStyle(bold) { append("${subjectStyle(status.next.subject).emoji} ${status.next.subject}") }
                append(" u ${status.next.period.start.label()}")
            }
            status is DayStatus.Before -> {
                append("Škola u ")
                withStyle(bold) { append(status.first.period.start.label()) }
                append(" · za ${durationLabel(status.minutes)}")
            }
            else -> withStyle(bold) { append("Gotovo za danas ✓") }
        }
    }
    val end = lessons.lastOrNull()?.period?.end
    // The next alarm that will really ring: once today's has passed, the one on the next school day.
    val alarm = child.nextAlarm(now)

    Surface(onClick = onClick, shape = MaterialTheme.shapes.extraLarge, color = colors.surfaceContainerLowest, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChildAvatar(child.name, size = 40.dp)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(child.name.ifBlank { "Bez imena" }, style = MaterialTheme.typography.titleLarge)
                    Text(
                        listOfNotNull(child.school.ifBlank { null }, if (lessons.isEmpty()) null else lessonsCount(lessons.size)).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Tag(
                    "${shift.icon()} ${shift.shortLabel()}",
                    tintOf(if (shift == Shift.MORNING) Color(0xFFF2B01E) else Color(0xFF5B6CF0), 0.22f, 0.35f),
                    colors.onSurface,
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Text(line, style = MaterialTheme.typography.bodyLarge)
            if (lessons.isNotEmpty()) MiniStops(lessons.map { it.subject to it.period }, clock)
            // Today's activities that have not ended yet, on a violet line so they read apart from the classes.
            val activities = child.activitiesOn(today).filter { !(it.weekly || today == it.lastDay) || clock < it.end }
            if (activities.isNotEmpty()) {
                Text(
                    "🏅 " + activities.joinToString("  ·  ") { "${it.kind.emoji()} ${it.title} ${it.timeLabelOn(today)}" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(tintOf(ActivityViolet, 0.14f, 0.28f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The texts take the room left by the switch, so with large fonts they wrap instead of pushing it out.
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (end != null) {
                        Text("Kraj ", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        Text(end.label(), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.width(14.dp))
                    }
                    Text("Alarm ", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(
                        when {
                            !child.alarmEnabled -> "isključen"
                            alarm == null -> "🔔 uključen"
                            alarm.toLocalDate() == today -> "🔔 ${alarm.toLocalTime().label()}"
                            alarm.toLocalDate() == today.plusDays(1) -> "🔔 sutra ${alarm.toLocalTime().label()}"
                            else -> "🔔 ${alarm.dayOfWeek.shortLabel().lowercase()} ${alarm.toLocalTime().label()}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Switch(
                    checked = child.alarmEnabled,
                    onCheckedChange = onAlarmChange,
                    modifier = Modifier.semantics { contentDescription = "Alarm za kraj nastave, ${child.name}" },
                )
            }
        }
    }
}

/** Small version of the Today screen's stops: one dot per class, faded when done, outlined when current. */
@Composable
private fun MiniStops(lessons: List<Pair<String, com.bane23.classschedule.data.Period>>, clock: java.time.LocalTime) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        lessons.forEachIndexed { i, (subject, period) ->
            if (i > 0) {
                Box(Modifier.widthIn(max = 10.dp).weight(1f).height(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
            }
            val done = clock >= period.end
            val current = clock >= period.start && !done
            Box(
                Modifier
                    .size(28.dp)
                    .alpha(if (done) 0.45f else 1f)
                    .clip(CircleShape)
                    .background(tintOf(subjectStyle(subject).color))
                    .then(if (current) Modifier.border(BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary), CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (done) "✓" else subjectStyle(subject).emoji, fontSize = 13.sp)
            }
        }
    }
}

/** A coming break or school event with the number of days until it. */
@Composable
fun EventTicket(event: CalendarEvent, today: LocalDate, trailing: (@Composable () -> Unit)? = null) {
    val days = ChronoUnit.DAYS.between(today, event.start)
    AppCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(eventEmoji(event.title), fontSize = 28.sp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    dateRangeLabel(event.start, event.end) + if (event.noClasses) " · nema nastave" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (days > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("$days", style = MaterialTheme.typography.titleLarge)
                    Text(daysCount(days).removePrefix("$days "), style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Tag("Sada", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }
            trailing?.invoke()
        }
    }
}
