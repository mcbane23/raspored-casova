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
)

/**
 * Optional classes held for a single subject. Stored inside the lesson text as "Dopunska: Matematika" so the
 * timetable format stays a plain list of strings.
 */
enum class ExtraClass(val prefix: String) {
    REMEDIAL("Dopunska"),
    ADVANCED("Dodatna"),
    ;

    fun lesson(subject: String): String = "$prefix: ${subject.trim()}"

    companion object {
        /** Splits a lesson into its optional-class kind (null for a regular class) and the subject. */
        fun parse(lesson: String): Pair<ExtraClass?, String> {
            entries.forEach { kind ->
                val start = "${kind.prefix}: "
                if (lesson.startsWith(start)) return kind to lesson.removePrefix(start)
            }
            return null to lesson
        }
    }
}

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
    /** The first period is a "pretčas" held before the 1st class. */
    val hasPreClass: Boolean = false,
) {
    /** "Pretčas" for the pre-class, otherwise "1. čas", "2. čas", ... */
    fun periodLabel(index: Int): String = when {
        hasPreClass && index == 0 -> "Pretčas"
        hasPreClass -> "$index. čas"
        else -> "${index + 1}. čas"
    }

    /** Short form for lists: "P" for the pre-class, otherwise "1.", "2.", ... */
    fun periodNumber(index: Int): String = when {
        hasPreClass && index == 0 -> "P"
        hasPreClass -> "$index."
        else -> "${index + 1}."
    }

    /** Adds or removes the pre-class, shifting the subjects so the regular classes keep their places. */
    fun withPreClass(enabled: Boolean): Timetable {
        if (enabled == hasPreClass) return this
        if (enabled) {
            val firstStart = periods.firstOrNull()?.startMinute ?: (14 * 60)
            val start = (firstStart - 50).coerceAtLeast(0)
            return copy(
                periods = listOf(Period(start, start + 45)) + periods,
                lessons = lessons.map { if (it.isEmpty()) it else listOf("") + it },
                hasPreClass = true,
            )
        }
        return copy(
            periods = periods.drop(1),
            lessons = lessons.map { it.drop(1).dropLastWhile { s -> s.isBlank() } },
            hasPreClass = false,
        )
    }

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
    /** Ring like an alarm clock (alarm volume, repeating) instead of the school-bell notification sound. */
    val alarmSound: Boolean = false,
    /** A plain notification without any sound; takes precedence over [alarmSound]. */
    val alarmSilent: Boolean = false,
    /** Remind the child in the evening before a school day to pack the bag. */
    val packReminderEnabled: Boolean = false,
    /** Time of the packing reminder, in minutes since midnight. */
    val packReminderMinute: Int = DEFAULT_PACK_REMINDER_MINUTE,
    /** Ring like an alarm clock for the packing reminder; see [alarmSound]. */
    val packReminderSound: Boolean = false,
    /** A packing reminder without any sound; takes precedence over [packReminderSound]. */
    val packReminderSilent: Boolean = false,
    /** What to bring for a subject (e.g. "patike i majica"), keyed by subject name; shown in the packing list. */
    val packNotes: Map<String, String> = emptyMap(),
    /** School day the [packed] ticks belong to; ticks for any other day are ignored. */
    val packedForEpochDay: Long = 0,
    /** Subjects already ticked off in the packing list for [packedForEpochDay]. */
    val packed: List<String> = emptyList(),
    /** Extracurricular activities (sport, choir, trips, ...) shown next to the classes. */
    val activities: List<Extracurricular> = emptyList(),
    /** Ring like an alarm clock for activity reminders; see [alarmSound]. */
    val activityReminderSound: Boolean = false,
    /** Activity reminders without any sound; takes precedence over [activityReminderSound]. */
    val activityReminderSilent: Boolean = false,
) {
    fun timetable(shift: Shift): Timetable = if (shift == Shift.MORNING) morning else afternoon

    fun withTimetable(shift: Shift, timetable: Timetable): Child =
        if (shift == Shift.MORNING) copy(morning = timetable) else copy(afternoon = timetable)
}

/** Name of a child added without one; the setup shows it as an empty field to type into. */
const val DEFAULT_CHILD_NAME = "Dete"

/** Default time of the packing reminder: 19:00. */
const val DEFAULT_PACK_REMINDER_MINUTE = 19 * 60

/** App color theme; [SYSTEM] follows the phone's light or dark setting. Light is the default. */
@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class AppData(
    val children: List<Child> = emptyList(),
    val selectedChildId: String? = null,
    val theme: ThemeMode = ThemeMode.LIGHT,
    /** Format version of the saved data; files written before versioning count as 1. */
    val dataVersion: Int = 1,
    /** The first-run setup was finished or skipped; until then the app opens on the setup steps. */
    val onboarded: Boolean = false,
    /** The user has tapped a class on Today once, so the "tap a class for details" hint is no longer shown. */
    val lessonHintSeen: Boolean = false,
)

const val CURRENT_DATA_VERSION = 4

/** Subjects that older versions put in every list before remedial and advanced classes became lesson types. */
private val LEGACY_EXTRA_SUBJECTS = setOf("Dopunska nastava", "Dodatna nastava")

/** Brings data saved by an older app version up to date. */
fun AppData.migrated(): AppData {
    var data = this
    if (data.dataVersion < 2) {
        data = data.copy(children = data.children.map { it.copy(subjects = it.subjects - LEGACY_EXTRA_SUBJECTS) })
    }
    if (data.dataVersion < 3) {
        // The light theme became the default; data saved earlier always has the old System default written out.
        if (data.theme == ThemeMode.SYSTEM) data = data.copy(theme = ThemeMode.LIGHT)
    }
    if (data.dataVersion < 4) {
        // The first-run setup came later; anyone with saved data has already set the app up by hand.
        data = data.copy(onboarded = true)
    }
    return data.copy(dataVersion = CURRENT_DATA_VERSION)
}
