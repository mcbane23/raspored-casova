package com.bane23.classschedule

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bane23.classschedule.data.ThemeMode
import com.bane23.classschedule.ui.AppViewModel
import com.bane23.classschedule.ui.BellsScreen
import com.bane23.classschedule.ui.CalendarScreen
import com.bane23.classschedule.ui.ChildSettingsScreen
import com.bane23.classschedule.ui.FamilyScreen
import com.bane23.classschedule.ui.SettingsHomeScreen
import com.bane23.classschedule.ui.SettingsPage
import com.bane23.classschedule.ui.SubjectsScreen
import com.bane23.classschedule.ui.TodayScreen
import com.bane23.classschedule.ui.WeekScreen
import com.bane23.classschedule.ui.rememberAlarmSwitch
import com.bane23.classschedule.ui.theme.RasporedCasovaTheme

class MainActivity : ComponentActivity() {
    /** Child whose Today tab a tapped notification asked for, until [App] has opened it. */
    private var openTodayFor by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only a fresh launch; after rotation the same intent must not jump back to Today.
        if (savedInstanceState == null) openTodayFor = intent?.getStringExtra(EXTRA_OPEN_TODAY_FOR)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppViewModel = viewModel()
            val data by viewModel.data.collectAsState()
            val dark = when (data.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Status and navigation bar icons follow the in-app theme, not only the phone setting.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            RasporedCasovaTheme(darkTheme = dark) {
                App(viewModel, openTodayFor = openTodayFor, onOpenedToday = { openTodayFor = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_TODAY_FOR)?.let { openTodayFor = it }
    }

    companion object {
        private const val EXTRA_OPEN_TODAY_FOR = "openTodayFor"

        /** Opens the app (or brings it to the front) on the Today tab of the child [childId]. */
        fun openTodayIntent(context: Context, childId: String): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_TODAY_FOR, childId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

// Same scrims androidx.activity uses by default for three-button navigation bars.
private val LIGHT_SCRIM = AndroidColor.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b)

private enum class Tab(val label: String, val emoji: String) {
    FAMILY("Porodica", "👨‍👩‍👧"),
    TODAY("Danas", "🏠"),
    WEEK("Nedelja", "📅"),
    BREAKS("Raspust", "🏖️"),
}

/** Settings pages are kept as short strings ("home", "child:<id>", ...) so they survive rotation. */
private fun SettingsPage.route(): String = when (this) {
    SettingsPage.Home -> "home"
    is SettingsPage.ChildPage -> "child:$childId"
    is SettingsPage.Bells -> "bells:$childId"
    is SettingsPage.Subjects -> "subjects:$childId"
}

private fun settingsPage(route: String?): SettingsPage? {
    if (route == null) return null
    val id = route.substringAfter(':', "")
    return when (route.substringBefore(':')) {
        "home" -> SettingsPage.Home
        "child" -> SettingsPage.ChildPage(id)
        "bells" -> SettingsPage.Bells(id)
        "subjects" -> SettingsPage.Subjects(id)
        else -> null
    }
}

/** Where the back button leads from a settings page. */
private fun SettingsPage.parent(): SettingsPage? = when (this) {
    SettingsPage.Home -> null
    is SettingsPage.ChildPage -> SettingsPage.Home
    is SettingsPage.Bells -> SettingsPage.ChildPage(childId)
    is SettingsPage.Subjects -> SettingsPage.ChildPage(childId)
}

/** The whole app; [openTodayFor] is a child id from a tapped notification, cleared through [onOpenedToday]. */
@Composable
fun App(viewModel: AppViewModel = viewModel(), openTodayFor: String? = null, onOpenedToday: () -> Unit = {}) {
    val data by viewModel.data.collectAsState()
    val family = data.children.size > 1
    var tab by rememberSaveable { mutableStateOf(if (family) Tab.FAMILY else Tab.TODAY) }
    var settingsRoute by rememberSaveable { mutableStateOf<String?>(null) }
    val child = data.children.find { it.id == data.selectedChildId } ?: data.children.first()
    val shownTab = if (tab == Tab.FAMILY && !family) Tab.TODAY else tab
    val page = settingsPage(settingsRoute)
    val setAlarm = rememberAlarmSwitch(viewModel)
    fun openSettings(next: SettingsPage?) { settingsRoute = next?.route() }

    BackHandler(enabled = page != null) { openSettings(page?.parent()) }

    LaunchedEffect(openTodayFor) {
        val id = openTodayFor ?: return@LaunchedEffect
        if (data.children.any { it.id == id }) viewModel.selectChild(id)
        tab = Tab.TODAY
        settingsRoute = null
        onOpenedToday()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (page == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                    Tab.entries.filter { it != Tab.FAMILY || family }.forEach { t ->
                        NavigationBarItem(
                            selected = shownTab == t,
                            onClick = { tab = t },
                            icon = { Text(t.emoji, fontSize = 22.sp) },
                            label = { Text(t.label, style = MaterialTheme.typography.labelLarge) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        val settingsChild = when (page) {
            is SettingsPage.ChildPage -> page.childId
            is SettingsPage.Bells -> page.childId
            is SettingsPage.Subjects -> page.childId
            else -> null
        }?.let { id -> data.children.find { it.id == id } }

        when {
            page == SettingsPage.Home || (page != null && settingsChild == null) -> SettingsHomeScreen(
                data = data,
                selected = child,
                viewModel = viewModel,
                onBack = { openSettings(null) },
                onOpenChild = { openSettings(SettingsPage.ChildPage(it)) },
                modifier = modifier,
            )
            page is SettingsPage.ChildPage && settingsChild != null -> ChildSettingsScreen(
                data = data,
                child = settingsChild,
                viewModel = viewModel,
                onBack = { openSettings(SettingsPage.Home) },
                onOpenBells = { openSettings(SettingsPage.Bells(settingsChild.id)) },
                onOpenSubjects = { openSettings(SettingsPage.Subjects(settingsChild.id)) },
                modifier = modifier,
            )
            page is SettingsPage.Bells && settingsChild != null ->
                BellsScreen(settingsChild, viewModel, onBack = { openSettings(page.parent()) }, modifier = modifier)
            page is SettingsPage.Subjects && settingsChild != null ->
                SubjectsScreen(settingsChild, viewModel, onBack = { openSettings(page.parent()) }, modifier = modifier)
            else -> when (shownTab) {
                Tab.FAMILY -> FamilyScreen(
                    children = data.children,
                    onOpenChild = {
                        viewModel.selectChild(it)
                        tab = Tab.TODAY
                    },
                    onOpenSettings = { openSettings(SettingsPage.Home) },
                    onAlarmChange = setAlarm,
                    modifier = modifier,
                )
                Tab.TODAY -> TodayScreen(
                    children = data.children,
                    child = child,
                    onSelectChild = { viewModel.selectChild(it) },
                    onOpenSettings = { openSettings(SettingsPage.Home) },
                    onChangeDay = { date, index, subject -> viewModel.changeDay(child.id, date, index, subject) },
                    onResetDay = { date -> viewModel.resetDay(child.id, date) },
                    onPacked = { date, subject, packed -> viewModel.setPacked(child.id, date, subject, packed) },
                    modifier = modifier,
                )
                Tab.WEEK -> WeekScreen(
                    children = data.children,
                    child = child,
                    onSelectChild = { viewModel.selectChild(it) },
                    onTimetableChange = { shift, timetable -> viewModel.setTimetable(child.id, shift, timetable) },
                    onSubjectUsed = { viewModel.addSubject(child.id, it) },
                    modifier = modifier,
                )
                Tab.BREAKS -> CalendarScreen(
                    children = data.children,
                    child = child,
                    onSelectChild = { viewModel.selectChild(it) },
                    onOpenSettings = { openSettings(SettingsPage.Home) },
                    onAddEvent = { viewModel.addEvent(child.id, it) },
                    onDeleteEvent = { viewModel.deleteEvent(child.id, it) },
                    modifier = modifier,
                )
            }
        }
    }
}
