package com.bane23.classschedule

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bane23.classschedule.data.AppData
import com.bane23.classschedule.data.CURRENT_DATA_VERSION
import com.bane23.classschedule.data.CalendarEvent
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.ChildRepository
import com.bane23.classschedule.data.Shift
import com.bane23.classschedule.data.ShiftMode
import com.bane23.classschedule.data.Timetable
import com.bane23.classschedule.ui.AppViewModel
import com.bane23.classschedule.ui.theme.RasporedCasovaTheme
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Renders the main screens with sample data into docs/screenshots. Excluded from normal test runs; run it with
 * -Pscreenshots, e.g.
 * `./gradlew testDebugUnitTest --tests '*ScreenshotTest' -Pscreenshots`. The Today tab shows the real current time.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h851dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File("../docs/screenshots")

    @Before
    fun createOutputDir() {
        outDir.mkdirs()
    }

    @Test
    fun firstRunSetup() {
        show(AppData(children = listOf(Child(name = "Dete", referenceEpochDay = today())), dataVersion = CURRENT_DATA_VERSION))
        shot("01-setup-welcome")
        click("Počni podešavanje")
        shot("02-setup-child")
        click("Dalje")
        shot("03-setup-shift")
        click("Dalje")
        shot("04-setup-bells")
        click("Dalje")
        shot("05-setup-notifications")
        click("Završi")
        shot("06-setup-done")
    }

    @Test
    fun mainTabsAndSettings() {
        show(sampleFamily())
        shot("10-family")
        tab("Danas")
        shot("11-today")
        tab("Nedelja")
        shot("12-week")
        compose.onNodeWithContentDescription("Prikaži celu nedelju").performClick()
        shot("13-week-table")
        compose.onNodeWithContentDescription("Prikaži po danu").performClick()
        compose.onNodeWithContentDescription("Uredi raspored").performClick()
        shot("14-week-edit")
        click("Gotovo")
        tab("Raspust")
        shot("15-breaks")
        compose.onNodeWithContentDescription("Podešavanja").performClick()
        shot("20-settings")
        click("Ana")
        shot("21-child-hub")
        click("Alarm i podsetnik za torbu")
        shot("22-notifications")
        compose.onNodeWithContentDescription("Nazad").performClick()
        click("Ime, škola i smena")
        shot("23-profile")
    }

    /** Taps a bottom-bar tab; matching on the selectable item avoids day names such as "Nedelja" elsewhere. */
    private fun tab(label: String) {
        compose.onNode(hasText(label) and isSelectable()).performClick()
        compose.waitForIdle()
    }

    private fun click(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    /** Starts the app on [data], replacing whatever an earlier test left in the repository. */
    private fun show(data: AppData) {
        val app = compose.activity.application
        File(app.filesDir, "data.json").writeText(Json { encodeDefaults = true }.encodeToString(AppData.serializer(), data))
        ChildRepository::class.java.getDeclaredField("instance").apply { isAccessible = true }.set(null, null)
        val viewModel = AppViewModel(app)
        compose.setContent { RasporedCasovaTheme(darkTheme = false) { App(viewModel) } }
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun today(): Long = LocalDate.now().toEpochDay()

    private fun sampleFamily(): AppData {
        val ana = Child(
            name = "Ana",
            school = "OŠ Vuk Karadžić, 3-2",
            shiftMode = ShiftMode.WEEKLY,
            referenceEpochDay = today(),
            referenceShift = Shift.MORNING,
            morning = Timetable.defaultFor(Shift.MORNING).copy(lessons = ANA_WEEK),
            afternoon = Timetable.defaultFor(Shift.AFTERNOON).copy(lessons = ANA_WEEK),
            alarmEnabled = true,
            packReminderEnabled = true,
            packNotes = mapOf(
                "Fizičko i zdravstveno vaspitanje" to "patike i majica",
                "Likovna kultura" to "tempere i blok",
                "Engleski jezik" to "radna sveska",
            ),
            customEvents = listOf(
                CalendarEvent(today() + 9, today() + 9, "Dan škole", noClasses = true),
                CalendarEvent(today() + 23, today() + 23, "Izlet", noClasses = false),
            ),
        )
        val marko = Child(
            name = "Marko",
            school = "OŠ Vuk Karadžić, 7-1",
            shiftMode = ShiftMode.WEEKLY,
            referenceEpochDay = today(),
            referenceShift = Shift.AFTERNOON,
            morning = Timetable.defaultFor(Shift.MORNING).copy(lessons = MARKO_WEEK),
            afternoon = Timetable.defaultFor(Shift.AFTERNOON).copy(lessons = MARKO_WEEK),
        )
        return AppData(children = listOf(ana, marko), selectedChildId = ana.id, dataVersion = CURRENT_DATA_VERSION, onboarded = true)
    }

    private companion object {
        val ANA_WEEK = listOf(
            listOf("Srpski jezik", "Matematika", "Engleski jezik", "Priroda i društvo", "Fizičko i zdravstveno vaspitanje"),
            listOf("Matematika", "Srpski jezik", "Likovna kultura", "Likovna kultura", "Dopunska: Matematika"),
            listOf("Srpski jezik", "Matematika", "Muzička kultura", "Priroda i društvo", "Fizičko i zdravstveno vaspitanje"),
            listOf("Engleski jezik", "Srpski jezik", "Matematika", "Građansko vaspitanje"),
            listOf("Srpski jezik", "Matematika", "Fizičko i zdravstveno vaspitanje", "Čas odeljenskog starešine"),
        )
        val MARKO_WEEK = listOf(
            listOf("Matematika", "Fizika", "Hemija", "Istorija", "Geografija", "Srpski jezik"),
            listOf("Srpski jezik", "Engleski jezik", "Biologija", "Matematika", "Informatika i računarstvo", "Fizičko i zdravstveno vaspitanje"),
            listOf("Hemija", "Matematika", "Srpski jezik", "Drugi strani jezik", "Tehnika i tehnologija", "Tehnika i tehnologija"),
            listOf("Fizika", "Istorija", "Matematika", "Srpski jezik", "Likovna kultura", "Biologija"),
            listOf("Geografija", "Engleski jezik", "Srpski jezik", "Muzička kultura", "Fizičko i zdravstveno vaspitanje"),
        )
    }
}
