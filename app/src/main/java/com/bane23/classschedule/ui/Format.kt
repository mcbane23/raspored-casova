package com.bane23.classschedule.ui

import com.bane23.classschedule.data.Period
import com.bane23.classschedule.data.Region
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.ThemeMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Serbian labels are kept here rather than relying on the device locale, so the app reads the same on any phone.

fun DayOfWeek.label(): String = when (this) {
    DayOfWeek.MONDAY -> "Ponedeljak"
    DayOfWeek.TUESDAY -> "Utorak"
    DayOfWeek.WEDNESDAY -> "Sreda"
    DayOfWeek.THURSDAY -> "Četvrtak"
    DayOfWeek.FRIDAY -> "Petak"
    DayOfWeek.SATURDAY -> "Subota"
    DayOfWeek.SUNDAY -> "Nedelja"
}

fun DayOfWeek.shortLabel(): String = label().take(3)

private val MONTHS = listOf(
    "januar", "februar", "mart", "april", "maj", "jun",
    "jul", "avgust", "septembar", "oktobar", "novembar", "decembar",
)

fun LocalDate.label(): String = "${dayOfWeek.label()}, $dayOfMonth. ${MONTHS[monthValue - 1]}"

/** "okt" for calendar date badges. */
fun LocalDate.monthShortLabel(): String = MONTHS[monthValue - 1].take(3)

/** "Oktobar 2026" for month headers. */
fun LocalDate.monthYearLabel(): String = "${MONTHS[monthValue - 1].replaceFirstChar { it.uppercase() }} $year"

/** "45 min", "1 h", "1 h 20 min". */
fun durationLabel(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "$m min"
        m == 0L -> "$h h"
        else -> "$h h $m min"
    }
}

/** "11. novembar", or "11–13. novembar" / "31. decembar – 15. januar" for ranges. */
fun dateRangeLabel(start: LocalDate, end: LocalDate): String = when {
    start == end -> "${start.dayOfMonth}. ${MONTHS[start.monthValue - 1]}"
    start.month == end.month && start.year == end.year ->
        "${start.dayOfMonth}–${end.dayOfMonth}. ${MONTHS[start.monthValue - 1]}"
    else -> "${start.dayOfMonth}. ${MONTHS[start.monthValue - 1]} – ${end.dayOfMonth}. ${MONTHS[end.monthValue - 1]}"
}

fun Region.label(): String = if (this == Region.CENTRAL_SERBIA) "Centralna Srbija" else "Vojvodina"

/** "za 1 dan", "za 5 dana", "za 21 dan" (Serbian plural agrees with the last digit). */
fun inDaysLabel(days: Long): String {
    val word = if (days % 10 == 1L && days % 100 != 11L) "dan" else "dana"
    return "za $days $word"
}

fun Shift.label(): String = if (this == Shift.MORNING) "Prepodnevna smena" else "Popodnevna smena"

fun Shift.shortLabel(): String = if (this == Shift.MORNING) "Pre podne" else "Popodne"

fun Shift.icon(): String = if (this == Shift.MORNING) "☀️" else "🌙"

/** "1 čas", "3 časa", "6 časova". */
fun lessonsWord(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "čas"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "časa"
    else -> "časova"
}

/** "5 časova", "1 čas". */
fun lessonsCount(count: Int): String = "$count ${lessonsWord(count)}"

/** "1 dan", "5 dana", "21 dan". */
fun daysCount(days: Long): String = if (days % 10 == 1L && days % 100 != 11L) "$days dan" else "$days dana"

fun ShiftMode.label(): String = when (this) {
    ShiftMode.FIXED_MORNING -> "Uvek pre podne"
    ShiftMode.FIXED_AFTERNOON -> "Uvek popodne"
    ShiftMode.WEEKLY -> "Smena se menja svake nedelje"
    ShiftMode.MONTHLY -> "Smena se menja svakog meseca"
}

fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "📱 Telefon"
    ThemeMode.LIGHT -> "☀️ Svetla"
    ThemeMode.DARK -> "🌙 Tamna"
}

fun LocalTime.label(): String = "%02d:%02d".format(hour, minute)

fun Period.label(): String = "${start.label()}–${end.label()}"
