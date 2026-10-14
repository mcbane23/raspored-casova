package com.bane23.classschedule.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.bane23.classschedule.MainActivity
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.ChildRepository
import com.bane23.classschedule.data.DayInfo
import com.bane23.classschedule.data.Lesson
import com.bane23.classschedule.data.dayInfo
import com.bane23.classschedule.data.hasTimetable
import com.bane23.classschedule.data.shiftOn
import com.bane23.classschedule.data.widgetDay
import com.bane23.classschedule.ui.childColor
import com.bane23.classschedule.ui.eventEmoji
import com.bane23.classschedule.ui.icon
import com.bane23.classschedule.ui.label
import com.bane23.classschedule.ui.shortLabel
import com.bane23.classschedule.ui.subjectStyle
import com.bane23.classschedule.ui.theme.DarkColors
import com.bane23.classschedule.ui.theme.LightColors
import java.time.LocalDate
import java.time.LocalDateTime

private val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

/**
 * Home-screen widget for the selected child: the day's classes (today until the last one ends, then the next school
 * day) and how much of the bag is packed. Tapping it opens that child's Today tab. Kept current by [WidgetUpdater].
 */
class ScheduleWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = ChildRepository.get(context)
        provideContent {
            val data by repository.data.collectAsState()
            GlanceTheme(colors = WidgetColors) { WidgetContent(context, data) }
        }
    }
}

class ScheduleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == WidgetUpdater.ACTION_REFRESH) WidgetUpdater.refresh(context, goAsync())
    }

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdater.scheduleNext(context)
    }
}

@Composable
private fun WidgetContent(context: Context, data: AppData) {
    val child = data.children.find { it.id == data.selectedChildId } ?: data.children.firstOrNull() ?: return
    val now = LocalDateTime.now()
    val day = child.widgetDay(now)
    val open = actionStartActivity(MainActivity.openTodayIntent(context, child.id))
    val colors = GlanceTheme.colors

    Column(
        GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(22.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .clickable(open),
    ) {
        Header(child, day?.date, now.toLocalDate(), day?.lessons.orEmpty())
        Spacer(GlanceModifier.height(8.dp))
        when {
            !child.hasTimetable() -> Message("🗓️", "Raspored još nije unet", "Dodirni da ga upišeš.")
            day == null -> {
                val reason = (child.dayInfo(now.toLocalDate()) as? DayInfo.NoClasses)?.reason ?: "Nema nastave"
                Message(eventEmoji(reason), reason, "Nema časova u naredne dve nedelje.")
            }
            else -> {
                LazyColumn(GlanceModifier.defaultWeight().fillMaxWidth()) {
                    items(day.lessons, itemId = { it.index.toLong() }) { lesson -> LessonRow(lesson, open) }
                }
                if (day.toPack > 0) PackLine(day.packed, day.toPack, day.packDay == now.toLocalDate().plusDays(1))
            }
        }
    }
}

/** Avatar, name, and which day is shown: "Danas · ☀️ Pre podne · 08:00–12:20". */
@Composable
private fun Header(child: Child, date: LocalDate?, today: LocalDate, lessons: List<Lesson>) {
    val colors = GlanceTheme.colors
    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            GlanceModifier.size(30.dp).cornerRadius(15.dp).background(childColor(child.name)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                child.name.trim().take(1).uppercase().ifEmpty { "?" },
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
        }
        Spacer(GlanceModifier.width(10.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                child.name.ifBlank { "Raspored časova" },
                style = TextStyle(color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            if (date != null) {
                val dayName = when (date) {
                    today -> "Danas"
                    today.plusDays(1) -> "Sutra"
                    else -> date.dayOfWeek.label()
                }
                val shift = child.shiftOn(date)
                val span = if (lessons.isEmpty()) "" else " · ${lessons.first().period.start.label()}–${lessons.last().period.end.label()}"
                Text(
                    "$dayName · ${shift.icon()} ${shift.shortLabel()}$span",
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

/** One class: emoji on a tint of the subject's color, the subject and the start time. */
@Composable
private fun LessonRow(lesson: Lesson, open: Action) {
    val colors = GlanceTheme.colors
    val style = subjectStyle(lesson.subject)
    Row(
        GlanceModifier.fillMaxWidth().padding(vertical = 2.dp).clickable(open),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            GlanceModifier.size(28.dp).cornerRadius(9.dp).background(style.color.copy(alpha = 0.28f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(style.emoji, style = TextStyle(fontSize = 14.sp))
        }
        Spacer(GlanceModifier.width(10.dp))
        Text(
            lesson.subject,
            style = TextStyle(color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(lesson.period.start.label(), style = TextStyle(color = colors.onSurfaceVariant, fontSize = 13.sp))
    }
}

/** "🎒 Torba za sutra: 2/4" until everything is ticked off, then "✅ Torba je spremna!". */
@Composable
private fun PackLine(packed: Int, total: Int, forTomorrow: Boolean) {
    val colors = GlanceTheme.colors
    val done = packed >= total
    Box(
        GlanceModifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .cornerRadius(12.dp)
            .background(if (done) colors.tertiaryContainer else colors.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            if (done) "✅ Torba je spremna!" else "🎒 Torba${if (forTomorrow) " za sutra" else ""}: $packed/$total spakovano",
            style = TextStyle(
                color = if (done) colors.onTertiaryContainer else colors.onPrimaryContainer,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun Message(emoji: String, title: String, hint: String) {
    val colors = GlanceTheme.colors
    Column(GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
        Text("$emoji $title", style = TextStyle(color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold))
        Text(hint, style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp))
    }
}
