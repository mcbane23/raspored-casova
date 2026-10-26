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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bane23.classschedule.data.ThemeMode
import com.bane23.classschedule.ui.ActivitiesScreen
import com.bane23.classschedule.ui.AppViewModel
import com.bane23.classschedule.ui.BellsScreen
import com.bane23.classschedule.ui.CalendarScreen
import com.bane23.classschedule.ui.ChildSettingsScreen
import com.bane23.classschedule.ui.FamilyScreen
import com.bane23.classschedule.ui.LocalUndo
import com.bane23.classschedule.ui.NotificationsScreen
import com.bane23.classschedule.ui.OnboardingScreen
import com.bane23.classschedule.ui.ProfileScreen
import com.bane23.classschedule.ui.SchoolCalendarScreen
import com.bane23.classschedule.ui.SettingsHomeScreen
import com.bane23.classschedule.ui.SettingsPage
import com.bane23.classschedule.ui.SubjectsScreen
import com.bane23.classschedule.ui.TodayScreen
import com.bane23.classschedule.ui.WeekScreen
import com.bane23.classschedule.ui.accentText
import com.bane23.classschedule.ui.rememberActivitySaver
import com.bane23.classschedule.ui.rememberAlarmSwitch
import com.bane23.classschedule.ui.theme.RasporedCasovaTheme
import kotlinx.coroutines.launch

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

private enum class Tab(val label: String, val icon: ImageVector) {
    FAMILY("Porodica", Icons.Rounded.Groups),
    TODAY("Danas", Icons.Rounded.Today),
    WEEK("Nedelja", Icons.Rounded.CalendarViewWeek),
    BREAKS("Raspust", Icons.Rounded.BeachAccess),
}

/** Settings pages are kept as short strings ("home", "child:<id>", ...) so they survive rotation. */
private fun SettingsPage.route(): String = when (this) {
    SettingsPage.Home -> "home"
    is SettingsPage.ChildPage -> "child:$childId"
    is SettingsPage.Profile -> "profile:$childId"
    is SettingsPage.Notifications -> "notifications:$childId"
    is SettingsPage.SchoolCalendar -> "calendar:$childId"
    is SettingsPage.Bells -> "bells:$childId"
    is SettingsPage.Subjects -> "subjects:$childId"
    is SettingsPage.Activities -> "activities:$childId"
}

private fun settingsPage(route: String?): SettingsPage? {
    if (route == null) return null
    val id = route.substringAfter(':', "")
    return when (route.substringBefore(':')) {
        "home" -> SettingsPage.Home
        "child" -> SettingsPage.ChildPage(id)
        "profile" -> SettingsPage.Profile(id)
        "notifications" -> SettingsPage.Notifications(id)
        "calendar" -> SettingsPage.SchoolCalendar(id)
        "bells" -> SettingsPage.Bells(id)
        "subjects" -> SettingsPage.Subjects(id)
        "activities" -> SettingsPage.Activities(id)
        else -> null
    }
}

/**
 * Where the back button leads from a settings page: a child's topic pages go back to that child's hub, or straight to
 * Settings when there is only one child, since its topics are listed there.
 */
private fun SettingsPage.parent(singleChild: Boolean): SettingsPage? = when (this) {
    SettingsPage.Home -> null
    is SettingsPage.ChildPage -> SettingsPage.Home
    is SettingsPage.ForChild -> if (singleChild) SettingsPage.Home else SettingsPage.ChildPage(childId)
}

/** The whole app; [openTodayFor] is a child id from a tapped notification, cleared through [onOpenedToday]. */
@Composable
fun App(viewModel: AppViewModel = viewModel(), openTodayFor: String? = null, onOpenedToday: () -> Unit = {}) {
    val data by viewModel.data.collectAsState()
    val family = data.children.size > 1
    var tab by rememberSaveable { mutableStateOf(if (family) Tab.FAMILY else Tab.TODAY) }
    var settingsRoute by rememberSaveable { mutableStateOf<String?>(null) }
    // Hoisted so empty states, settings and the setup steps can open the Week tab straight in edit mode.
    var editingWeek by rememberSaveable { mutableStateOf(false) }
    // The settings page the timetable editor was opened from, so "Gotovo" leads back there.
    var returnAfterEdit by rememberSaveable { mutableStateOf<String?>(null) }
    val child = data.children.find { it.id == data.selectedChildId } ?: data.children.first()
    val shownTab = if (tab == Tab.FAMILY && !family) Tab.TODAY else tab
    val page = settingsPage(settingsRoute)
    val setAlarm = rememberAlarmSwitch(viewModel)
    val saveActivity = rememberActivitySaver(viewModel)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // One undo message at a time: a new one replaces the one still showing.
    val offerUndo: (String, () -> Unit) -> Unit = remember(snackbar, scope) {
        { message, undo ->
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(message, actionLabel = "Poništi", duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) undo()
            }
        }
    }
    fun openSettings(next: SettingsPage?) { settingsRoute = next?.route() }
    fun editTimetable(childId: String) {
        viewModel.selectChild(childId)
        returnAfterEdit = settingsRoute
        settingsRoute = null
        tab = Tab.WEEK
        editingWeek = true
    }
    fun finishEditing() {
        editingWeek = false
        settingsRoute = returnAfterEdit
        returnAfterEdit = null
    }

    BackHandler(enabled = page != null) { openSettings(page?.parent(!family)) }
    BackHandler(enabled = page == null && shownTab == Tab.WEEK && editingWeek) { finishEditing() }

    LaunchedEffect(openTodayFor) {
        val id = openTodayFor ?: return@LaunchedEffect
        if (data.children.any { it.id == id }) viewModel.selectChild(id)
        tab = Tab.TODAY
        settingsRoute = null
        onOpenedToday()
    }

    if (!data.onboarded) {
        OnboardingScreen(
            child = child,
            viewModel = viewModel,
            onFinish = { enterTimetable ->
                viewModel.finishOnboarding()
                if (enterTimetable) editTimetable(child.id) else tab = if (family) Tab.FAMILY else Tab.TODAY
            },
        )
        return
    }

    CompositionLocalProvider(LocalUndo provides offerUndo) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                if (page == null) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                        Tab.entries.filter { it != Tab.FAMILY || family }.forEach { t ->
                            NavigationBarItem(
                                selected = shownTab == t,
                                onClick = {
                                    tab = t
                                    if (t != Tab.WEEK) editingWeek = false
                                returnAfterEdit = null
                                },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(t.label, style = MaterialTheme.typography.labelLarge) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = accentText(),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            val modifier = Modifier.padding(innerPadding)
            AnimatedContent(
                targetState = settingsRoute ?: "tab:${shownTab.name}",
                transitionSpec = { screenTransition() },
                label = "screen",
            ) { screen ->
                // Draw the screen this content slot stands for, so the outgoing one keeps its look while it animates away.
                val page = settingsPage(screen)
                val screenTab = Tab.entries.find { "tab:${it.name}" == screen } ?: shownTab
                val settingsChild = (page as? SettingsPage.ForChild)?.let { p -> data.children.find { it.id == p.childId } }

                when {
                    page == SettingsPage.Home || (page != null && settingsChild == null) -> SettingsHomeScreen(
                        data = data,
                        selected = child,
                        viewModel = viewModel,
                        onBack = { openSettings(null) },
                        onOpenChild = { openSettings(SettingsPage.ChildPage(it)) },
                        onOpen = { openSettings(it) },
                        onEditTimetable = { editTimetable(it) },
                        modifier = modifier,
                    )
                    page is SettingsPage.ChildPage && settingsChild != null -> ChildSettingsScreen(
                        data = data,
                        child = settingsChild,
                        viewModel = viewModel,
                        onBack = { openSettings(SettingsPage.Home) },
                        onOpen = { openSettings(it) },
                        onEditTimetable = { editTimetable(settingsChild.id) },
                        modifier = modifier,
                    )
                    page is SettingsPage.Profile && settingsChild != null ->
                        ProfileScreen(settingsChild, viewModel, onBack = { openSettings(page.parent(!family)) }, modifier = modifier)
                    page is SettingsPage.Notifications && settingsChild != null ->
                        NotificationsScreen(settingsChild, viewModel, onBack = { openSettings(page.parent(!family)) }, modifier = modifier)
                    page is SettingsPage.SchoolCalendar && settingsChild != null ->
                        SchoolCalendarScreen(settingsChild, viewModel, onBack = { openSettings(page.parent(!family)) }, modifier = modifier)
                    page is SettingsPage.Bells && settingsChild != null ->
                        BellsScreen(settingsChild, viewModel, onBack = { openSettings(page.parent(!family)) }, modifier = modifier)
                    page is SettingsPage.Subjects && settingsChild != null ->
                        SubjectsScreen(settingsChild, viewModel, onBack = { openSettings(page.parent(!family)) }, modifier = modifier)
                    page is SettingsPage.Activities && settingsChild != null ->
                        ActivitiesScreen(settingsChild, viewModel, onBack = { openSettings(page.parent(!family)) }, modifier = modifier)
                    else -> when (screenTab) {
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
                            onEditTimetable = { editTimetable(child.id) },
                            onChangeDay = { date, index, subject -> viewModel.changeDay(child.id, date, index, subject) },
                            onResetDay = { date -> viewModel.resetDay(child.id, date) },
                            onPacked = { date, subject, packed -> viewModel.setPacked(child.id, date, subject, packed) },
                            onSaveActivity = { saveActivity(child.id, it) },
                            onDeleteActivity = { viewModel.deleteActivity(child.id, it) },
                            modifier = modifier,
                        )
                        Tab.WEEK -> WeekScreen(
                            children = data.children,
                            child = child,
                            onSelectChild = { viewModel.selectChild(it) },
                            onOpenSettings = { openSettings(SettingsPage.Home) },
                            onOpenBells = { openSettings(SettingsPage.Bells(child.id)) },
                            editMode = editingWeek,
                            onEditModeChange = { if (it) editingWeek = true else finishEditing() },
                            onTimetableChange = { shift, timetable -> viewModel.setTimetable(child.id, shift, timetable) },
                            onSubjectUsed = { viewModel.addSubject(child.id, it) },
                            onSaveActivity = { saveActivity(child.id, it) },
                            onDeleteActivity = { viewModel.deleteActivity(child.id, it) },
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
    }
}

/** How deep a screen sits: the tabs, then Settings, a child's hub and a child's topic page. */
private fun depth(screen: String): Int = when (settingsPage(screen)) {
    null -> 0
    SettingsPage.Home -> 1
    is SettingsPage.ChildPage -> 2
    else -> 3
}

/** Going deeper slides the new page in from the right, going back from the left; switching tabs crossfades. */
private fun AnimatedContentTransitionScope<String>.screenTransition(): ContentTransform {
    val from = depth(initialState)
    val to = depth(targetState)
    return when {
        to > from -> (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 6 } + fadeOut())
        to < from -> (slideInHorizontally { -it / 6 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
        else -> fadeIn(tween(220)) togetherWith fadeOut(tween(150))
    }
}
