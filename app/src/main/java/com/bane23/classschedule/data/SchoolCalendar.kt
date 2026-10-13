package com.bane23.classschedule.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@Serializable
enum class Region { CENTRAL_SERBIA, VOJVODINA }

/**
 * A calendar entry covering [startEpochDay]..[endEpochDay] (inclusive). [noClasses] marks breaks and days off;
 * [timetableDay] (ISO day number, 1 = Monday) marks a day taught by another weekday's timetable.
 */
@Serializable
data class CalendarEvent(
    val startEpochDay: Long,
    val endEpochDay: Long,
    val title: String,
    val noClasses: Boolean = true,
    val timetableDay: Int? = null,
    val id: String = UUID.randomUUID().toString(),
) {
    val start: LocalDate get() = LocalDate.ofEpochDay(startEpochDay)
    val end: LocalDate get() = LocalDate.ofEpochDay(endEpochDay)

    fun covers(date: LocalDate): Boolean = date.toEpochDay() in startEpochDay..endEpochDay
}

/** An entry of the official calendar; [region] or [finalGrade] restrict whom it applies to (null = everyone). */
data class OfficialEvent(
    val event: CalendarEvent,
    val region: Region? = null,
    val finalGrade: Boolean? = null,
)

/** One school year of the official calendar, from September 1 to August 31. */
data class SchoolYear(val label: String, val firstDay: LocalDate, val events: List<OfficialEvent>) {
    val lastDayOfYear: LocalDate get() = firstDay.withMonth(8).withDayOfMonth(31).plusYears(1)

    operator fun contains(date: LocalDate): Boolean = date >= firstDay && date <= lastDayOfYear
}

private fun official(
    id: String,
    start: LocalDate,
    end: LocalDate,
    title: String,
    noClasses: Boolean = true,
    timetableDay: DayOfWeek? = null,
    region: Region? = null,
    finalGrade: Boolean? = null,
) = OfficialEvent(
    CalendarEvent(start.toEpochDay(), end.toEpochDay(), title, noClasses, timetableDay?.value, id = id),
    region,
    finalGrade,
)

private fun d(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)

/**
 * Official calendars for primary schools in Serbia ("Pravilnik o kalendaru obrazovno-vaspitnog rada osnovne
 * škole"). Add a new [SchoolYear] each year once the ministry publishes it.
 */
val SCHOOL_YEARS: List<SchoolYear> = listOf(
    SchoolYear(
        label = "2026/27",
        firstDay = d(2026, 9, 1),
        events = listOf(
            official("2627-swap-1110", d(2026, 11, 10), d(2026, 11, 10), "Nastava po rasporedu za petak",
                noClasses = false, timetableDay = DayOfWeek.FRIDAY),
            official("2627-autumn", d(2026, 11, 11), d(2026, 11, 13), "Jesenji raspust"),
            official("2627-winter", d(2026, 12, 31), d(2027, 1, 15), "Zimski raspust", region = Region.CENTRAL_SERBIA),
            official("2627-winter-v", d(2026, 12, 24), d(2027, 1, 8), "Zimski raspust", region = Region.VOJVODINA),
            official("2627-sava", d(2027, 1, 27), d(2027, 1, 27), "Savindan, nema nastave"),
            official("2627-sretenje", d(2027, 2, 15), d(2027, 2, 19), "Sretenjski raspust"),
            official("2627-mock-exam", d(2027, 3, 19), d(2027, 3, 20), "Probni završni ispit",
                noClasses = false, finalGrade = true),
            official("2627-easter-v", d(2027, 3, 26), d(2027, 3, 26), "Nenastavni dan (Vojvodina)", region = Region.VOJVODINA),
            official("2627-easter-v2", d(2027, 3, 29), d(2027, 3, 29), "Nenastavni dan (Vojvodina)", region = Region.VOJVODINA),
            official("2627-spring", d(2027, 4, 30), d(2027, 5, 4), "Prolećni raspust"),
            official("2627-summer-8", d(2027, 5, 29), d(2027, 8, 31), "Kraj nastave za osmi razred, letnji raspust",
                finalGrade = true),
            official("2627-final-exam", d(2027, 6, 14), d(2027, 6, 16), "Završni ispit",
                noClasses = false, finalGrade = true),
            official("2627-summer", d(2027, 6, 12), d(2027, 8, 31), "Letnji raspust", finalGrade = false),
        ),
    ),
)

fun schoolYearOf(date: LocalDate): SchoolYear? = SCHOOL_YEARS.find { date in it }

/** What kind of day [date] is for this child. */
sealed interface DayInfo {
    data class NoClasses(val reason: String) : DayInfo

    /** A school day taught by the timetable of [timetableDay] (usually the date's own weekday). */
    data class SchoolDay(val timetableDay: DayOfWeek, val note: String? = null) : DayInfo
}

/** Official events that apply to this child (by region and grade) plus the child's own events, sorted by date. */
fun Child.calendarEvents(): List<CalendarEvent> {
    val official = SCHOOL_YEARS.flatMap { it.events }
        .filter { (it.region == null || it.region == region) && (it.finalGrade == null || it.finalGrade == finalGrade) }
        .map { it.event }
    return (official + customEvents).sortedWith(compareBy({ it.startEpochDay }, { it.endEpochDay }))
}

fun Child.dayInfo(date: LocalDate): DayInfo {
    val events = calendarEvents().filter { it.covers(date) }
    events.firstOrNull { it.noClasses }?.let { return DayInfo.NoClasses(it.title) }
    if (date.dayOfWeek !in SCHOOL_DAYS) return DayInfo.NoClasses("Vikend")
    val swap = events.firstOrNull { it.timetableDay != null }
    if (swap != null) return DayInfo.SchoolDay(DayOfWeek.of(swap.timetableDay!!), swap.title)
    return DayInfo.SchoolDay(date.dayOfWeek, events.firstOrNull()?.title)
}

/** The next break or day off that has not ended yet, as seen from [today]. */
fun Child.nextDayOff(today: LocalDate): CalendarEvent? =
    calendarEvents().firstOrNull { it.noClasses && it.endEpochDay >= today.toEpochDay() }
