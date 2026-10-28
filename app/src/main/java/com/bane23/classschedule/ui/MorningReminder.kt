package com.bane23.classschedule.ui

import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.lessonsOn
import com.bane23.classschedule.data.overrideOn
import java.time.LocalDate

/** Title and text of a morning reminder; [bigText] is shown when the notification is expanded. */
data class MorningReminderContent(val title: String, val text: String, val bigText: String)

/**
 * What the morning reminder says on [date]: when the first class starts and the day's classes, with a nudge to change
 * a class that was moved or cancelled for this day only. Null when there are no classes that day.
 */
fun morningReminderContent(child: Child, date: LocalDate): MorningReminderContent? {
    val lessons = child.lessonsOn(date)
    val first = lessons.firstOrNull() ?: return null
    val start = first.period.start.label()
    val title = child.name.ifBlank { null }?.let { "☀️ $it, prvi čas u $start" } ?: "☀️ Prvi čas u $start"
    val text = if (child.overrideOn(date) != null) {
        "Raspored je danas izmenjen. Proveri da li je sve tačno."
    } else {
        "Da li je neki čas pomeren ili otkazan? Proveri plan za danas."
    }
    val lines = lessons.map { "• ${it.period.start.label()} ${it.subject}" }
    return MorningReminderContent(title, text, "$text\n${lines.joinToString("\n")}")
}
