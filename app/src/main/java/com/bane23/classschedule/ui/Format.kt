package com.bane23.classschedule.ui

import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.Period
import com.bane23.classschedule.data.Region
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.ThemeMode
import com.bane23.classschedule.data.currentShiftDate
import com.bane23.classschedule.data.shiftOn
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

/** [text] without emoji, for screen readers: "🎉 Bravo!" is read as "Bravo!", not "party popper Bravo". */
fun spoken(text: String): String =
    text.replace(EMOJI, "").replace(Regex("\\s+"), " ").replace(" ,", ",").trim()

private val EMOJI = Regex("[\\p{So}\\p{Sk}\\x{FE0F}\\x{200D}\\x{20E3}]")

/** The day after "za": "za ponedeljak", "za sredu". */
fun DayOfWeek.accusative(): String = label().lowercase().let { if (it.endsWith("a")) it.dropLast(1) + "u" else it }

/** "svaki ponedeljak", "svaku sredu". */
fun DayOfWeek.every(): String = (if (label().endsWith("a")) "svaku " else "svaki ") + accusative()

fun LocalDate.label(): String = "${dayOfWeek.label()}, $dayOfMonth. ${MONTHS[monthValue - 1]}"

/** The date after "za" or "na": "sredu, 28. oktobar". */
fun LocalDate.accusativeLabel(): String = "${dayOfWeek.accusative()}, $dayOfMonth. ${MONTHS[monthValue - 1]}"

private val MONTHS_GENITIVE = listOf(
    "januara", "februara", "marta", "aprila", "maja", "juna",
    "jula", "avgusta", "septembra", "oktobra", "novembra", "decembra",
)

/** The date after "do": "13. novembra". */
fun LocalDate.genitiveLabel(): String = "$dayOfMonth. ${MONTHS_GENITIVE[monthValue - 1]}"

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

/** "1 predmet", "5 predmeta", "21 predmet". */
fun subjectsCount(count: Int): String = if (count % 10 == 1 && count % 100 != 11) "$count predmet" else "$count predmeta"

/** "1 dan", "5 dana", "21 dan". */
fun daysCount(days: Long): String = if (days % 10 == 1L && days % 100 != 11L) "$days dan" else "$days dana"

fun ShiftMode.label(): String = when (this) {
    ShiftMode.FIXED_MORNING -> "Uvek pre podne"
    ShiftMode.FIXED_AFTERNOON -> "Uvek popodne"
    ShiftMode.WEEKLY -> "Smena se menja svake nedelje"
    ShiftMode.MONTHLY -> "Smena se menja svakog meseca"
}

/**
 * "ove nedelje pre podne", "ovog meseca popodne", "uvek popodne"; on a weekend a weekly rotation names the coming
 * week, "od ponedeljka popodne", like the shift picker.
 */
fun shiftSummary(child: Child, today: LocalDate): String {
    val date = child.currentShiftDate(today)
    val shift = child.shiftOn(date).shortLabel().lowercase()
    return when (child.shiftMode) {
        ShiftMode.WEEKLY -> if (date != today) "od ponedeljka $shift" else "ove nedelje $shift"
        ShiftMode.MONTHLY -> "ovog meseca $shift"
        else -> "uvek $shift"
    }
}

fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "📱 Telefon"
    ThemeMode.LIGHT -> "☀️ Svetla"
    ThemeMode.DARK -> "🌙 Tamna"
}

fun LocalTime.label(): String = "%02d:%02d".format(hour, minute)

fun Period.label(): String = "${start.label()}–${end.label()}"

/**
 * What to pack for: " Sutra spakuj torbu." when the next school day is tomorrow, " Za ponedeljak spakuj torbu." over
 * a weekend, nothing before a break or when no school day follows.
 */
fun packHint(date: LocalDate, packDay: LocalDate?): String = when {
    packDay == null || packDay > date.plusDays(3) -> ""
    packDay == date.plusDays(1) -> " Sutra spakuj torbu."
    else -> " Za ${packDay.dayOfWeek.accusative()} spakuj torbu."
}
