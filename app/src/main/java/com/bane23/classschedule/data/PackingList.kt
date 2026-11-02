package com.bane23.classschedule.data

import java.time.LocalDate

/** Suggestions used until the parent writes their own note for a subject (matched on the start of a word in its name). */
private val DEFAULT_PACK_NOTES: List<Pair<String, () -> String>> = listOf(
    "fizičk" to { tr("patike i sportska oprema", "sneakers and sports gear") },
    "fizick" to { tr("patike i sportska oprema", "sneakers and sports gear") },
    "physical" to { tr("patike i sportska oprema", "sneakers and sports gear") },
    "likovn" to { tr("pribor za likovno", "art supplies") },
    "art" to { tr("pribor za likovno", "art supplies") },
)

/** The first day after [date] (within two weeks) that has classes, or null. */
fun Child.nextSchoolDayAfter(date: LocalDate): LocalDate? =
    (1L..14L).map { date.plusDays(it) }.firstOrNull { lessonsOn(it).isNotEmpty() }

/** Subjects to pack for [date], in class order, once each; optional classes count as their subject. */
fun Child.packSubjects(date: LocalDate): List<String> =
    lessonsOn(date).map { ExtraClass.parse(it.subject).second.trim() }.filter { it.isNotEmpty() }.distinct()

/** What to bring for [subject]: the parent's note, a built-in suggestion, or "". */
fun Child.packNote(subject: String): String {
    packNotes[subject]?.let { return it }
    val name = subject.lowercase()
    return DEFAULT_PACK_NOTES.firstOrNull { name.startsWith(it.first) || name.contains(" ${it.first}") }?.second?.invoke().orEmpty()
}

fun Child.isPacked(date: LocalDate, subject: String): Boolean =
    packedForEpochDay == date.toEpochDay() && subject in packed

/** Ticks [subject] on or off for [date]; ticks left over from another day are dropped. */
fun Child.withPacked(date: LocalDate, subject: String, isPacked: Boolean): Child {
    val current = if (packedForEpochDay == date.toEpochDay()) packed else emptyList()
    val updated = if (isPacked) (current + subject).distinct() else current - subject
    return copy(packedForEpochDay = date.toEpochDay(), packed = updated)
}

/** Sets or clears (blank [note]) the packing note for [subject]. */
fun Child.withPackNote(subject: String, note: String): Child =
    copy(packNotes = if (note.isBlank()) packNotes - subject else packNotes + (subject to note.trim()))
