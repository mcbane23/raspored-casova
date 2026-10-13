package com.bane23.classschedule.ui

import com.bane23.classschedule.data.Period
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
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

private val MONTHS = listOf(
    "januar", "februar", "mart", "april", "maj", "jun",
    "jul", "avgust", "septembar", "oktobar", "novembar", "decembar",
)

fun LocalDate.label(): String = "${dayOfWeek.label()}, $dayOfMonth. ${MONTHS[monthValue - 1]}"

fun Shift.label(): String = if (this == Shift.MORNING) "Prepodnevna smena" else "Popodnevna smena"

fun Shift.shortLabel(): String = if (this == Shift.MORNING) "Prepodne" else "Popodne"

fun ShiftMode.label(): String = when (this) {
    ShiftMode.FIXED_MORNING -> "Uvek prepodne"
    ShiftMode.FIXED_AFTERNOON -> "Uvek popodne"
    ShiftMode.WEEKLY -> "Smena se menja svake nedelje"
    ShiftMode.MONTHLY -> "Smena se menja svakog meseca"
}

fun LocalTime.label(): String = "%02d:%02d".format(hour, minute)

fun Period.label(): String = "${start.label()}–${end.label()}"
