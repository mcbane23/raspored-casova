package com.bane23.classschedule.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Every child's timetables as they were when the timetable editor opened. Edits are saved as they are made, so this is
 * what "Odbaci" goes back to. Only the subjects are compared and restored: the bells page can be opened from the
 * editor, and bell times changed there stay.
 */
@Serializable
data class TimetableSnapshot(val timetables: Map<String, Map<Shift, Timetable>>) {
    /** Whether any subject of any child differs from the snapshot. */
    fun hasChanges(children: List<Child>): Boolean = changes(children).isNotEmpty()

    /** The timetables that bring [children] back to the snapshot's subjects, as (child id, shift, timetable). */
    fun changes(children: List<Child>): List<Triple<String, Shift, Timetable>> = children.flatMap { child ->
        val saved = timetables[child.id] ?: return@flatMap emptyList()
        Shift.entries.mapNotNull { shift ->
            val before = saved[shift] ?: return@mapNotNull null
            val current = child.timetable(shift)
            val restored = current.withLessonsOf(before)
            if (current.trimmedLessons() == restored.trimmedLessons()) null else Triple(child.id, shift, restored)
        }
    }

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun of(children: List<Child>): TimetableSnapshot =
            TimetableSnapshot(children.associate { child -> child.id to Shift.entries.associateWith { child.timetable(it) } })

        fun decode(text: String): TimetableSnapshot? = runCatching { json.decodeFromString(serializer(), text) }.getOrNull()
    }
}

/** This timetable's bells with the subjects of [saved], moved like the bells page moves them for a pre-class. */
internal fun Timetable.withLessonsOf(saved: Timetable): Timetable {
    val aligned = saved.withPreClass(hasPreClass)
    return copy(lessons = SCHOOL_DAYS.indices.map { i -> aligned.lessons.getOrNull(i).orEmpty().take(periods.size).dropLastWhile { it.isBlank() } })
}

/** The subjects per day without free periods at the end, which do not show and so are not a change. */
private fun Timetable.trimmedLessons(): List<List<String>> =
    SCHOOL_DAYS.indices.map { i -> lessons.getOrNull(i).orEmpty().dropLastWhile { it.isBlank() } }
