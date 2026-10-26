package com.bane23.classschedule.ui

import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.isPacked
import com.bane23.classschedule.data.nextSchoolDayAfter
import com.bane23.classschedule.data.packNote
import com.bane23.classschedule.data.packSubjects
import java.time.LocalDate

/** Title and text of a packing reminder; [bigText] is shown when the notification is expanded. */
data class PackReminderContent(val title: String, val text: String, val bigText: String)

/**
 * What the packing reminder says on [date]: the next school day's subjects that are not ticked off in "Sutra u torbu"
 * yet, with what to bring for each. Null when there is no school day ahead or everything is already packed.
 */
fun packReminderContent(child: Child, date: LocalDate): PackReminderContent? {
    val day = child.nextSchoolDayAfter(date) ?: return null
    val subjects = child.packSubjects(day)
    val left = subjects.filterNot { child.isPacked(day, it) }
    if (subjects.isNotEmpty() && left.isEmpty()) return null
    val title = child.name.ifBlank { null }?.let { "🎒 $it, spakuj torbu" } ?: "🎒 Spakuj torbu"
    val forDay = if (day == date.plusDays(1)) "Za sutra" else "Za ${day.dayOfWeek.accusative()}"
    if (left.isEmpty()) {
        val text = "Vreme je da spakuješ knjige i pribor ${forDay.lowercase()}."
        return PackReminderContent(title, text, text)
    }
    val lines = left.map { subject ->
        val note = child.packNote(subject)
        if (note.isEmpty()) "• $subject" else "• $subject: $note"
    }
    return PackReminderContent(title, "$forDay: ${left.joinToString(", ")}", "$forDay:\n${lines.joinToString("\n")}")
}
