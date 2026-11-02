package com.bane23.classschedule.ui

import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.ExtraClass
import com.bane23.classschedule.data.Period
import com.bane23.classschedule.data.Region
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Lang
import com.bane23.classschedule.data.ThemeMode
import com.bane23.classschedule.data.currentShiftDate
import com.bane23.classschedule.data.shiftOn
import com.bane23.classschedule.data.subjectLabel
import com.bane23.classschedule.data.tr
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// Labels in the app's language (see Lang) are kept here rather than relying on the device locale, so the app reads
// the same on any phone. Serbian needs grammar forms (cases, plurals) that English does not; in English those
// functions return the plain form.

fun DayOfWeek.label(): String = when (this) {
    DayOfWeek.MONDAY -> tr("Ponedeljak", "Monday")
    DayOfWeek.TUESDAY -> tr("Utorak", "Tuesday")
    DayOfWeek.WEDNESDAY -> tr("Sreda", "Wednesday")
    DayOfWeek.THURSDAY -> tr("Četvrtak", "Thursday")
    DayOfWeek.FRIDAY -> tr("Petak", "Friday")
    DayOfWeek.SATURDAY -> tr("Subota", "Saturday")
    DayOfWeek.SUNDAY -> tr("Nedelja", "Sunday")
}

fun DayOfWeek.shortLabel(): String = label().take(3)

/**
 * A day or date label inside a sentence: lowercase in Serbian ("u sredu"), unchanged in English, where day and month
 * names keep their capital letter ("on Wednesday").
 */
fun String.inSentence(): String = if (Lang.english) this else lowercase()

private val MONTHS_SR = listOf(
    "januar", "februar", "mart", "april", "maj", "jun",
    "jul", "avgust", "septembar", "oktobar", "novembar", "decembar",
)

private val MONTHS_EN = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)

private val MONTHS: List<String> get() = if (Lang.english) MONTHS_EN else MONTHS_SR

/** "11. novembar" in Serbian, "November 11" in English. */
private fun dayMonth(day: Int, month: Int): String =
    if (Lang.english) "${MONTHS_EN[month - 1]} $day" else "$day. ${MONTHS_SR[month - 1]}"

/** [text] without emoji, for screen readers: "🎉 Bravo!" is read as "Bravo!", not "party popper Bravo". */
fun spoken(text: String): String =
    text.replace(EMOJI, "").replace(Regex("\\s+"), " ").replace(" ,", ",").trim()

private val EMOJI = Regex("[\\p{So}\\p{Sk}\\x{FE0F}\\x{200D}\\x{20E3}]")

/** The day after "za": "za ponedeljak", "za sredu"; "Monday" in English. */
fun DayOfWeek.accusative(): String =
    if (Lang.english) label() else label().lowercase().let { if (it.endsWith("a")) it.dropLast(1) + "u" else it }

/** "svaki ponedeljak", "svaku sredu"; "every Monday" in English. */
fun DayOfWeek.every(): String =
    if (Lang.english) "every ${label()}" else (if (label().endsWith("a")) "svaku " else "svaki ") + accusative()

/** "Sreda, 28. oktobar"; "Wednesday, October 28" in English. */
fun LocalDate.label(): String = "${dayOfWeek.label()}, ${dayMonth(dayOfMonth, monthValue)}"

/** The date after "za" or "na": "sredu, 28. oktobar"; "Wednesday, October 28" in English. */
fun LocalDate.accusativeLabel(): String = "${dayOfWeek.accusative()}, ${dayMonth(dayOfMonth, monthValue)}"

private val MONTHS_GENITIVE = listOf(
    "januara", "februara", "marta", "aprila", "maja", "juna",
    "jula", "avgusta", "septembra", "oktobra", "novembra", "decembra",
)

/** The date after "do": "13. novembra"; "November 13" in English. */
fun LocalDate.genitiveLabel(): String =
    if (Lang.english) dayMonth(dayOfMonth, monthValue) else "$dayOfMonth. ${MONTHS_GENITIVE[monthValue - 1]}"

/** "okt" ("Oct" in English) for calendar date badges. */
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

/**
 * "11. novembar", or "11–13. novembar" / "31. decembar – 15. januar" for ranges; in English "November 11",
 * "November 11–13" and "December 31 – January 15".
 */
fun dateRangeLabel(start: LocalDate, end: LocalDate): String = when {
    start == end -> dayMonth(start.dayOfMonth, start.monthValue)
    start.month == end.month && start.year == end.year ->
        if (Lang.english) "${MONTHS_EN[start.monthValue - 1]} ${start.dayOfMonth}–${end.dayOfMonth}"
        else "${start.dayOfMonth}–${end.dayOfMonth}. ${MONTHS_SR[start.monthValue - 1]}"
    else -> "${dayMonth(start.dayOfMonth, start.monthValue)} – ${dayMonth(end.dayOfMonth, end.monthValue)}"
}

fun Region.label(): String = if (this == Region.CENTRAL_SERBIA) tr("Centralna Srbija", "Central Serbia") else "Vojvodina"

/** "za 1 dan", "za 5 dana", "za 21 dan" (Serbian plural agrees with the last digit); "in 1 day", "in 5 days". */
fun inDaysLabel(days: Long): String {
    if (Lang.english) return if (days == 1L) "in 1 day" else "in $days days"
    val word = if (days % 10 == 1L && days % 100 != 11L) "dan" else "dana"
    return "za $days $word"
}

fun Shift.label(): String =
    if (this == Shift.MORNING) tr("Prepodnevna smena", "Morning shift") else tr("Popodnevna smena", "Afternoon shift")

fun Shift.shortLabel(): String = if (this == Shift.MORNING) tr("Pre podne", "Morning") else tr("Popodne", "Afternoon")

fun Shift.icon(): String = if (this == Shift.MORNING) "☀️" else "🌙"

/** "1 čas", "3 časa", "6 časova"; "class" or "classes" in English. */
fun lessonsWord(count: Int): String = when {
    Lang.english -> if (count == 1) "class" else "classes"
    count % 10 == 1 && count % 100 != 11 -> "čas"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "časa"
    else -> "časova"
}

/** "5 časova", "1 čas". */
fun lessonsCount(count: Int): String = "$count ${lessonsWord(count)}"

/** "1 predmet", "5 predmeta", "21 predmet"; "1 subject", "5 subjects". */
fun subjectsCount(count: Int): String = if (Lang.english) (if (count == 1) "1 subject" else "$count subjects")
else if (count % 10 == 1 && count % 100 != 11) "$count predmet" else "$count predmeta"

/** "1 dan", "5 dana", "21 dan"; "1 day", "5 days". */
fun daysCount(days: Long): String = if (Lang.english) (if (days == 1L) "1 day" else "$days days")
else if (days % 10 == 1L && days % 100 != 11L) "$days dan" else "$days dana"

fun ShiftMode.label(): String = when (this) {
    ShiftMode.FIXED_MORNING -> tr("Uvek pre podne", "Always in the morning")
    ShiftMode.FIXED_AFTERNOON -> tr("Uvek popodne", "Always in the afternoon")
    ShiftMode.WEEKLY -> tr("Smena se menja svake nedelje", "The shift changes every week")
    ShiftMode.MONTHLY -> tr("Smena se menja svakog meseca", "The shift changes every month")
}

/**
 * "ove nedelje pre podne", "ovog meseca popodne", "uvek popodne"; on a weekend a weekly rotation names the coming
 * week, "od ponedeljka popodne", like the shift picker. In English: "mornings this week", "afternoons from Monday".
 */
fun shiftSummary(child: Child, today: LocalDate): String {
    val date = child.currentShiftDate(today)
    val shift = child.shiftOn(date).shortLabel().lowercase()
    if (Lang.english) {
        val plural = "${shift}s"
        return when (child.shiftMode) {
            ShiftMode.WEEKLY -> if (date != today) "$plural from Monday" else "$plural this week"
            ShiftMode.MONTHLY -> "$plural this month"
            else -> "always $plural"
        }
    }
    return when (child.shiftMode) {
        ShiftMode.WEEKLY -> if (date != today) "od ponedeljka $shift" else "ove nedelje $shift"
        ShiftMode.MONTHLY -> "ovog meseca $shift"
        else -> "uvek $shift"
    }
}

fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> tr("📱 Telefon", "📱 Phone")
    ThemeMode.LIGHT -> tr("☀️ Svetla", "☀️ Light")
    ThemeMode.DARK -> tr("🌙 Tamna", "🌙 Dark")
}

fun LocalTime.label(): String = "%02d:%02d".format(hour, minute)

fun Period.label(): String = "${start.label()}–${end.label()}"

/**
 * What to pack for: " Sutra spakuj torbu." when the next school day is tomorrow, " Za ponedeljak spakuj torbu." over
 * a weekend, nothing before a break or when no school day follows.
 */
fun packHint(date: LocalDate, packDay: LocalDate?): String = when {
    packDay == null || packDay > date.plusDays(3) -> ""
    packDay == date.plusDays(1) -> tr(" Sutra spakuj torbu.", " Pack your bag for tomorrow.")
    else -> tr(" Za ${packDay.dayOfWeek.accusative()} spakuj torbu.", " Pack your bag for ${packDay.dayOfWeek.label()}.")
}

/** Short subject names for places where only a few letters fit (the week table, the stops on Today). */
private val SHORT_NAMES = listOf(
    "srpski" to "Srp", "matemat" to "Mat", "engleski" to "Eng", "strani" to "Str", "svet oko nas" to "SON",
    "priroda" to "PiD", "likovn" to "Lik", "muzič" to "Muz", "muzic" to "Muz", "fizičk" to "Fiz", "fizick" to "Fiz",
    "istorij" to "Ist", "geograf" to "Geo", "biolog" to "Bio", "fizika" to "Fizika", "hemij" to "Hem", "tehni" to "Teh",
    "informat" to "Inf", "verska" to "Ver", "građansk" to "Građ", "gradjansk" to "Građ", "odeljensk" to "ČOS",
    "serbian" to "Ser", "math" to "Math", "english" to "Eng", "foreign" to "Lang", "world around" to "World",
    "nature" to "Nat", "music" to "Mus", "physical" to "PE", "history" to "Hist", "geograph" to "Geo", "physics" to "Phys",
    "chemi" to "Chem", "techn" to "Tech", "computer" to "CS", "religio" to "Rel", "civic" to "Civ",
    "homeroom" to "HR", "art" to "Art",
)

/**
 * "Mat" for "Matematika" or "Dopunska: Matematika" ("Math" in English); unknown subjects keep their first four
 * letters. Works on the name as shown, so a built-in subject gets the short name of its translation.
 */
fun shortName(lesson: String): String {
    val name = subjectLabel(ExtraClass.parse(lesson).second.trim())
    return SHORT_NAMES.firstOrNull { name.lowercase().contains(it.first) }?.second ?: name.take(4)
}
