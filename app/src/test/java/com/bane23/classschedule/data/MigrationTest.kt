package com.bane23.classschedule.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MigrationTest {
    private val oldList = listOf("Srpski jezik", "Matematika", "Dopunska nastava", "Dodatna nastava")

    @Test
    fun oldDataLosesTheFormerExtraClassSubjects() {
        val old = AppData(children = listOf(Child(name = "Ana", referenceEpochDay = 0, subjects = oldList)))

        val migrated = old.migrated()

        assertEquals(listOf("Srpski jezik", "Matematika"), migrated.children.single().subjects)
        assertEquals(CURRENT_DATA_VERSION, migrated.dataVersion)
    }

    @Test
    fun subjectsAddedByHandAfterMigrationStay() {
        val current = AppData(children = listOf(Child(name = "Ana", referenceEpochDay = 0, subjects = oldList)), dataVersion = CURRENT_DATA_VERSION)

        assertEquals(oldList, current.migrated().children.single().subjects)
    }

    @Test
    fun oldSystemThemeBecomesLight() {
        val old = AppData(theme = ThemeMode.SYSTEM, dataVersion = 2)
        assertEquals(ThemeMode.LIGHT, old.migrated().theme)

        val chosenLater = AppData(theme = ThemeMode.SYSTEM, dataVersion = CURRENT_DATA_VERSION)
        assertEquals(ThemeMode.SYSTEM, chosenLater.migrated().theme)
    }

    @Test
    fun defaultListHasNoExtraClassSubjects() {
        assertEquals(emptyList<String>(), DEFAULT_SUBJECTS.filter { it.startsWith("Dopunska") || it.startsWith("Dodatna") })
    }
}
