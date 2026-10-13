package com.bane23.classschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.ui.AppViewModel
import com.bane23.classschedule.ui.CalendarScreen
import com.bane23.classschedule.ui.ChildAvatar
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

@Composable
fun App(viewModel: AppViewModel = viewModel()) {
    val data by viewModel.data.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val child = data.children.find { it.id == data.selectedChildId } ?: data.children.first()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (data.children.size > 1) {
                ChildSwitcher(
                    children = data.children,
                    selectedId = child.id,
                    onSelect = { viewModel.selectChild(it) },
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
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
                onSubjectUsed = { viewModel.addSubject(child.id, it) },
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

/** Row of children at the top (shown only with two or more), so a child can tap their own name. */
@Composable
private fun ChildSwitcher(children: List<Child>, selectedId: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        children.forEach { c ->
            val selected = c.id == selectedId
            Surface(
                onClick = { onSelect(c.id) },
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 1.dp,
            ) {
                Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ChildAvatar(c.name, size = 32.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        c.name.ifBlank { "Bez imena" },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
