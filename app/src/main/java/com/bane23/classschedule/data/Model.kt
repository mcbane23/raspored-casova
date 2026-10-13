package com.bane23.classschedule.data

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

/** School days shown in the timetable, Monday to Friday. */
val SCHOOL_DAYS: List<DayOfWeek> = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)

/** Subjects of Serbian primary school (grades 1–8), offered until the parent edits the list. */
val DEFAULT_SUBJECTS: List<String> = listOf(
    "Srpski jezik",
    "Matematika",
    "Engleski jezik",
    "Svet oko nas",
    "Priroda i društvo",
    "Likovna kultura",
    "Muzička kultura",
    "Fizičko i zdravstveno vaspitanje",
    "Istorija",
    "Geografija",
    "Biologija",
    "Fizika",
    "Hemija",
    "Tehnika i tehnologija",
    "Informatika i računarstvo",
    "Drugi strani jezik",
    "Verska nastava",
    "Građansko vaspitanje",
    "Čas odeljenskog starešine",
    "Dopunska nastava",
    "Dodatna nastava",
)

@Serializable
enum class Shift { MORNING, AFTERNOON }

@Serializable
enum class ShiftMode {
    /** Always the morning shift. */
    FIXED_MORNING,

    /** Always the afternoon shift. */
    FIXED_AFTERNOON,

    /** Shift alternates every week (Monday to Sunday). */
    WEEKLY,

    /** Shift alternates every calendar month. */
    MONTHLY,
}

/** One class period, stored as minutes since midnight so it serializes without custom adapters. */
@Serializable
data class Period(val startMinute: Int, val endMinute: Int) {
    val start: LocalTime get() = LocalTime.of(startMinute / 60, startMinute % 60)
    val end: LocalTime get() = LocalTime.of(endMinute / 60, endMinute % 60)
}

/**
 * The timetable for one shift: bell times shared by every day, and per school day the subject of each period
 * ("" for a free period). [lessons] has one list per entry of [SCHOOL_DAYS].
 */
@Serializable
data class Timetable(
    val periods: List<Period>,
    val lessons: List<List<String>> = SCHOOL_DAYS.map { emptyList() },
) {
    fun lessonsFor(day: DayOfWeek): List<String> = SCHOOL_DAYS.indexOf(day).let { lessons.getOrNull(it) }.orEmpty()

    fun withLesson(day: DayOfWeek, periodIndex: Int, subject: String): Timetable {
        val dayIndex = SCHOOL_DAYS.indexOf(day)
        val updatedLessons = SCHOOL_DAYS.indices.map { i ->
            val current = lessons.getOrNull(i).orEmpty()
            if (i != dayIndex) return@map current
            val padded = current + List((periodIndex + 1 - current.size).coerceAtLeast(0)) { "" }
            padded.toMutableList().also { it[periodIndex] = subject.trim() }.dropLastWhile { it.isBlank() }
        }
        return copy(lessons = updatedLessons)
    }

    companion object {
        /** Typical Serbian bell schedule: 45-minute classes, 5-minute breaks and a 20-minute break after the 2nd. */
        fun defaultFor(shift: Shift): Timetable {
            val firstStart = if (shift == Shift.MORNING) 8 * 60 else 14 * 60
            val periods = mutableListOf<Period>()
            var start = firstStart
            repeat(7) { index ->
                periods += Period(start, start + 45)
                start += 45 + if (index == 1) 20 else 5
            }
            return Timetable(periods)
        }
    }
}

/** A one-off change to a single day's lessons (a substituted subject or a cancelled class). */
@Serializable
data class DayOverride(val epochDay: Long, val lessons: List<String>)

@Serializable
data class Child(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val school: String = "",
    val shiftMode: ShiftMode = ShiftMode.WEEKLY,
    /** Epoch day of a date whose week (or month) is in [referenceShift]; anchors the alternation. */
    val referenceEpochDay: Long,
    val referenceShift: Shift = Shift.MORNING,
    val morning: Timetable = Timetable.defaultFor(Shift.MORNING),
    val afternoon: Timetable = Timetable.defaultFor(Shift.AFTERNOON),
    val overrides: List<DayOverride> = emptyList(),
    val region: Region = Region.CENTRAL_SERBIA,
    /** 8th grade finishes classes earlier and has the final exam. */
    val finalGrade: Boolean = false,
    /** Events the parent added for this child's school, e.g. the school's slava or a day without classes. */
    val customEvents: List<CalendarEvent> = emptyList(),
    /** Subjects offered when filling in the timetable. */
    val subjects: List<String> = DEFAULT_SUBJECTS,
    /** Notify the parent when the day's last class ends. */
    val alarmEnabled: Boolean = false,
    /** How many minutes before the end of the last class the notification comes (0 = at the end). */
    val alarmMinutesBefore: Int = 0,
) {
    fun timetable(shift: Shift): Timetable = if (shift == Shift.MORNING) morning else afternoon

    fun withTimetable(shift: Shift, timetable: Timetable): Child =
        if (shift == Shift.MORNING) copy(morning = timetable) else copy(afternoon = timetable)
}

@Serializable
data class AppData(
    val children: List<Child> = emptyList(),
    val selectedChildId: String? = null,
)
