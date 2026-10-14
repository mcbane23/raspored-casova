package com.bane23.classschedule.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bane23.classschedule.data.Child
import com.bane23.classschedule.data.hasTimetable

/** Steps of the first-run setup; the ones between [WELCOME] and [DONE] show as progress dots. */
private enum class SetupStep { WELCOME, CHILD, SHIFT, BELLS, NOTIFY, DONE }

private val DOTTED_STEPS = listOf(SetupStep.CHILD, SetupStep.SHIFT, SetupStep.BELLS, SetupStep.NOTIFY)

/**
 * First-run setup for parents: the child's name and school, the shift, the bells and the notifications, one question
 * per screen, ending with a button straight into entering the timetable. A QR code from another parent's phone fills
 * in everything but the notifications. [onFinish] gets true when the parent wants to enter the timetable now.
 */
@Composable
fun OnboardingScreen(child: Child, viewModel: AppViewModel, onFinish: (enterTimetable: Boolean) -> Unit) {
    var step by rememberSaveable { mutableStateOf(SetupStep.WELCOME) }
    val scanQr = rememberQrImport(viewModel, replaceable = child, onImported = { step = SetupStep.NOTIFY })
    val name = child.name.ifBlank { "dete" }

    BackHandler(enabled = step != SetupStep.WELCOME && step != SetupStep.DONE) {
        step = SetupStep.entries[step.ordinal - 1]
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                if (step in DOTTED_STEPS) {
                    BackButton { step = SetupStep.entries[step.ordinal - 1] }
                }
            }
            if (step in DOTTED_STEPS) ProgressDots(DOTTED_STEPS.indexOf(step), DOTTED_STEPS.size)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (step != SetupStep.DONE) {
                    TextButton(onClick = { onFinish(false) }) { Text("Preskoči") }
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (step) {
                SetupStep.WELCOME -> {
                    StepHeader("🎒", "Raspored časova", "Časovi, zvona, torba i raspust na jednom mestu. Podesi sve za par minuta.")
                    Spacer(Modifier.height(8.dp))
                    BigButton("Počni podešavanje", Modifier.fillMaxWidth()) { step = SetupStep.CHILD }
                    OutlinedButton(onClick = scanQr, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("📷 Uvezi sa QR koda", style = MaterialTheme.typography.titleMedium)
                    }
                    Hint(
                        "Drugi roditelj već ima raspored? Na tom telefonu otvori Podešavanja → dete → Podeli raspored.",
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
                SetupStep.CHILD -> {
                    StepHeader("👧", "Ko ide u školu?", "Ime se vidi na svakom ekranu. Kasnije možeš dodati još dece.")
                    SettingsGroup(null) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NameAndSchoolFields(child, viewModel)
                        }
                    }
                }
                SetupStep.SHIFT -> {
                    StepHeader("☀️", "Kada $name ide u školu?", "Kad se smene menjaju, aplikacija sama zna koja je smena koje nedelje.")
                    SettingsGroup(null) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShiftPicker(child, viewModel)
                        }
                    }
                }
                SetupStep.BELLS -> {
                    StepHeader("⏰", "Kada zvoni?", "Ovo su uobičajena zvona. Ako su u školi drugačija, dodirni vreme i promeni ga.")
                    BellsEditor(child, viewModel)
                }
                SetupStep.NOTIFY -> NotifyStep(child, viewModel)
                SetupStep.DONE -> {
                    val imported = child.hasTimetable()
                    StepHeader(
                        "🎉",
                        "Spremno!",
                        if (imported) "Raspored za $name je uvezen. Sve možeš promeniti u podešavanjima (⚙️ gore desno)."
                        else "Još samo upiši koje časove $name ima svakog dana. Sve ostalo menjaš u podešavanjima (⚙️ gore desno).",
                    )
                    Spacer(Modifier.height(8.dp))
                    if (imported) {
                        BigButton("Otvori raspored", Modifier.fillMaxWidth()) { onFinish(false) }
                    } else {
                        BigButton("✏️ Upiši raspored", Modifier.fillMaxWidth()) { onFinish(true) }
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.addChild("")
                            step = SetupStep.CHILD
                        },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text("➕ Dodaj još jedno dete", style = MaterialTheme.typography.titleMedium)
                    }
                    if (!imported) {
                        TextButton(onClick = { onFinish(false) }, modifier = Modifier.fillMaxWidth()) { Text("Kasnije") }
                    }
                }
            }
        }

        if (step in DOTTED_STEPS) {
            BigButton(
                if (step == SetupStep.NOTIFY) "Završi" else "Dalje",
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) { step = SetupStep.entries[step.ordinal + 1] }
        }
    }
}

/** The parent alarm and the packing reminder as two switches; the details stay in the child's settings. */
@Composable
private fun NotifyStep(child: Child, viewModel: AppViewModel) {
    val setAlarm = rememberAlarmSwitch(viewModel)
    val setPackReminder = rememberPackReminderSwitch(viewModel)
    StepHeader("🔔", "Podsetnici", "Oba su na ovom telefonu. Vreme i zvuk menjaš kasnije u podešavanjima.")
    SettingsGroup(null) {
        SettingsRow(
            title = "Javi mi kad se završe časovi",
            subtitle = "Za roditelja, kad zvoni za kraj poslednjeg časa",
            emoji = "🔔",
            trailing = { Switch(checked = child.alarmEnabled, onCheckedChange = { setAlarm(child.id, it) }) },
            onClick = { setAlarm(child.id, !child.alarmEnabled) },
        )
        RowDivider()
        SettingsRow(
            title = "Podseti da se spakuje torba",
            subtitle = "Za dete, uveče u 19:00 pred školski dan",
            emoji = "🎒",
            trailing = { Switch(checked = child.packReminderEnabled, onCheckedChange = { setPackReminder(child.id, it) }) },
            onClick = { setPackReminder(child.id, !child.packReminderEnabled) },
        )
    }
}

/** Big emoji, title and one explaining sentence at the top of a setup step. */
@Composable
private fun StepHeader(emoji: String, title: String, text: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(emoji, fontSize = 64.sp)
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** One dot per step, the current one stretched into an accent pill. */
@Composable
private fun ProgressDots(current: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (i == current) 24.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}
