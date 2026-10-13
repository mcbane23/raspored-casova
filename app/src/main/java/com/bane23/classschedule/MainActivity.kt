package com.bane23.classschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bane23.classschedule.ui.AppViewModel
import com.bane23.classschedule.ui.CalendarScreen
import com.bane23.classschedule.ui.SettingsScreen
import com.bane23.classschedule.ui.TodayScreen
import com.bane23.classschedule.ui.WeekScreen
import com.bane23.classschedule.ui.theme.RasporedCasovaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RasporedCasovaTheme {
                App()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Danas", Icons.Filled.Home),
    WEEK("Nedelja", Icons.AutoMirrored.Filled.List),
    CALENDAR("Kalendar", Icons.Filled.DateRange),
    SETTINGS("Podešavanja", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(viewModel: AppViewModel = viewModel()) {
    val data by viewModel.data.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    var childMenuOpen by remember { mutableStateOf(false) }
    val child = data.children.find { it.id == data.selectedChildId } ?: data.children.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (data.children.size > 1) {
                        Box {
                            TextButton(onClick = { childMenuOpen = true }) {
                                Text(child.name)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Izaberi dete")
                            }
                            DropdownMenu(expanded = childMenuOpen, onDismissRequest = { childMenuOpen = false }) {
                                data.children.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name) },
                                        onClick = {
                                            viewModel.selectChild(c.id)
                                            childMenuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (tab) {
            Tab.TODAY -> TodayScreen(
                child = child,
                onChangeDay = { date, index, subject -> viewModel.changeDay(child.id, date, index, subject) },
                onResetDay = { date -> viewModel.resetDay(child.id, date) },
                modifier = modifier,
            )
            Tab.WEEK -> WeekScreen(
                child = child,
                onTimetableChange = { shift, timetable -> viewModel.setTimetable(child.id, shift, timetable) },
                modifier = modifier,
            )
            Tab.CALENDAR -> CalendarScreen(
                child = child,
                onAddEvent = { viewModel.addEvent(child.id, it) },
                onDeleteEvent = { viewModel.deleteEvent(child.id, it) },
                modifier = modifier,
            )
            Tab.SETTINGS -> SettingsScreen(data = data, child = child, viewModel = viewModel, modifier = modifier)
        }
    }
}
