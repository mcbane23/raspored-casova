package com.bane23.classschedule.ui

import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.extraClassesToAsk
import com.bane23.classschedule.data.lessonLabel
import com.bane23.classschedule.data.lessonsOn
import com.bane23.classschedule.data.overrideOn
import com.bane23.classschedule.data.tr
import java.time.LocalDate

/** Title and text of a morning reminder; [bigText] is shown when the notification is expanded. */
data class MorningReminderContent(val title: String, val text: String, val bigText: String)

/**
 * What the morning reminder says on [date]: when the first class starts and the day's classes, with a nudge to change
 * a class that was moved or cancelled for this day only, or to say whether the child goes to today's remedial or
 * advanced classes. Null when there are no classes that day.
 */
fun morningReminderContent(child: Child, date: LocalDate): MorningReminderContent? {
    val lessons = child.lessonsOn(date)
    val first = lessons.firstOrNull() ?: return null
    val start = first.period.start.label()
    val title = child.name.ifBlank { null }?.let { tr("☀️ $it, prvi čas u $start", "☀️ $it, first class at $start") }
        ?: tr("☀️ Prvi čas u $start", "☀️ First class at $start")
    val text = if (child.extraClassesToAsk(date).isNotEmpty()) {
        tr(
            "Danas ima dopunske ili dodatne nastave. Označi da li ${child.name.ifBlank { "dete" }} ide.",
            "There are remedial or advanced classes today. Mark whether ${child.name.ifBlank { "your child" }} is going.",
        )
    } else if (child.overrideOn(date) != null) {
        tr("Raspored je danas izmenjen. Proveri da li je sve tačno.", "Today’s timetable was changed. Check that everything is right.")
    } else {
        tr("Da li je neki čas pomeren ili otkazan? Proveri plan za danas.", "Was a class moved or cancelled? Check today’s plan.")
    }
    val lines = lessons.map { "• ${it.period.start.label()} ${lessonLabel(it.subject)}" }
    return MorningReminderContent(title, text, "$text\n${lines.joinToString("\n")}")
}
