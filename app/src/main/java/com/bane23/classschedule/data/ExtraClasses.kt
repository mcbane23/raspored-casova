package com.bane23.classschedule.data

import java.time.LocalDate
import java.time.LocalTime

/**
 * Remedial and advanced classes on [date] the parent has not yet said the child goes to, so Today can ask. With
 * [now], classes that have already ended are left out, since the answer no longer changes anything.
 */
fun Child.extraClassesToAsk(date: LocalDate, now: LocalTime? = null): List<Lesson> =
    lessonsOn(date).filter { lesson ->
        ExtraClass.parse(lesson.subject).first != null &&
            ExtraClassAnswer(date.toEpochDay(), lesson.index) !in attendingExtra &&
            (now == null || now < lesson.period.end)
    }

/**
 * Records whether the child goes to the remedial or advanced class in period [index] on [date]. Going keeps the class
 * and stops the question; not going cancels it for that day only, so the day's first and last class, the parent alarm
 * and the morning reminder follow the classes the child really has.
 */
fun Child.withExtraClassAnswer(date: LocalDate, index: Int, attends: Boolean): Child {
    val answer = ExtraClassAnswer(date.toEpochDay(), index)
    val others = attendingExtra - answer
    return if (attends) copy(attendingExtra = others + answer) else copy(attendingExtra = others).withDayChange(date, index, "")
}
