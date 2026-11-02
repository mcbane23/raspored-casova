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
import com.bane23.classschedule.data.isDefaultChildName
import com.bane23.classschedule.data.hasTimetable
import com.bane23.classschedule.data.tr

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
    // The default name "Dete" reads oddly mid-sentence ("Kada Dete ide u školu?").
    val name = child.name.takeIf { it.isNotBlank() && !isDefaultChildName(it) } ?: tr("dete", "your child")

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
                    TextButton(colors = accentTextButton(), onClick = { onFinish(false) }) { Text(tr("Preskoči", "Skip")) }
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
                    StepHeader(
                        "🎒",
                        tr("Raspored časova", "Class schedule"),
                        tr("Časovi, zvona, torba i raspust na jednom mestu. Podesi sve za par minuta.", "Classes, bells, the school bag and breaks in one place. Set it all up in a couple of minutes."),
                    )
                    Spacer(Modifier.height(8.dp))
                    BigButton(tr("Počni podešavanje", "Start setup"), Modifier.fillMaxWidth()) { step = SetupStep.CHILD }
                    OutlinedButton(onClick = scanQr, shape = CircleShape, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text(tr("📷 Uvezi sa QR koda", "📷 Import from a QR code"), style = MaterialTheme.typography.titleMedium)
                    }
                    Hint(
                        tr("Drugi roditelj već ima raspored? Na tom telefonu otvori Podešavanja → dete → Podeli raspored.", "Does the other parent already have the timetable? On that phone open Settings → child → Share timetable."),
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    )
                }
                SetupStep.CHILD -> {
                    StepHeader("👧", tr("Ko ide u školu?", "Who goes to school?"), tr("Ime se vidi na svakom ekranu. Kasnije možeš dodati još dece.", "The name shows on every screen. You can add more children later."))
                    SettingsGroup(null) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NameAndSchoolFields(child, viewModel)
                        }
                    }
                }
                SetupStep.SHIFT -> {
                    StepHeader("☀️", tr("Kada $name ide u školu?", "When does $name go to school?"), tr("Kad se smene menjaju, aplikacija sama zna koja je smena koje nedelje.", "When shifts alternate, the app knows which shift is on each week."))
                    SettingsGroup(null) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ShiftPicker(child, viewModel)
                        }
                    }
                }
                SetupStep.BELLS -> {
                    StepHeader("⏰", tr("Kada zvoni?", "When does the bell ring?"), tr("Ovo su uobičajena zvona. Ako su u školi drugačija, dodirni vreme i promeni ga.", "These are the usual bell times. If your school differs, tap a time to change it."))
                    BellsEditor(child, viewModel)
                }
                SetupStep.NOTIFY -> NotifyStep(child, viewModel)
                SetupStep.DONE -> {
                    val imported = child.hasTimetable()
                    StepHeader(
                        "🎉",
                        tr("Spremno!", "All set!"),
                        if (imported) tr("Raspored za $name je uvezen. Sve možeš promeniti u podešavanjima (⚙️ gore desno).", "The timetable for $name has been imported. You can change anything in Settings (⚙️ top right).")
                        else tr("Još samo upiši koje časove $name ima svakog dana. Sve ostalo menjaš u podešavanjima (⚙️ gore desno).", "Now just enter the classes $name has each day. Everything else is in Settings (⚙️ top right)."),
                    )
                    Spacer(Modifier.height(8.dp))
                    if (imported) {
                        BigButton(tr("Otvori raspored", "Open timetable"), Modifier.fillMaxWidth()) { onFinish(false) }
                    } else {
                        BigButton(tr("✏️ Upiši raspored", "✏️ Enter timetable"), Modifier.fillMaxWidth()) { onFinish(true) }
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.addChild("")
                            step = SetupStep.CHILD
                        },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(tr("➕ Dodaj još jedno dete", "➕ Add another child"), style = MaterialTheme.typography.titleMedium)
                    }
                    if (!imported) {
                        TextButton(colors = accentTextButton(), onClick = { onFinish(false) }, modifier = Modifier.fillMaxWidth()) { Text(tr("Kasnije", "Later")) }
                    }
                }
            }
        }

        if (step in DOTTED_STEPS) {
            BigButton(
                if (step == SetupStep.NOTIFY) tr("Završi", "Finish") else tr("Dalje", "Next"),
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) { step = SetupStep.entries[step.ordinal + 1] }
        }
    }
}

/** The parent alarm, the morning reminder and the packing reminder as switches; the details stay in the child's settings. */
@Composable
private fun NotifyStep(child: Child, viewModel: AppViewModel) {
    val setAlarm = rememberAlarmSwitch(viewModel)
    val setPackReminder = rememberPackReminderSwitch(viewModel)
    val setMorningReminder = rememberMorningReminderSwitch(viewModel)
    StepHeader("🔔", tr("Podsetnici", "Reminders"), tr("Stižu na ovaj telefon. Vreme i zvuk menjaš kasnije u podešavanjima.", "They come to this phone. Change the time and sound later in Settings."))
    SettingsGroup(null) {
        SettingsRow(
            title = tr("Javi mi kad se završe časovi", "Tell me when classes are over"),
            subtitle = tr("Za roditelja, kad zvoni za kraj poslednjeg časa", "For the parent, when the last class of the day ends"),
            emoji = "🔔",
            checked = child.alarmEnabled,
            onCheckedChange = { setAlarm(child.id, it) },
        )
        RowDivider()
        SettingsRow(
            title = tr("Podseti me pre škole", "Remind me before school"),
            subtitle = tr("Sat vremena pre prvog časa, da proveriš plan za danas", "An hour before the first class, to check today’s plan"),
            emoji = "☀️",
            checked = child.morningReminderEnabled,
            onCheckedChange = { setMorningReminder(child.id, it) },
        )
        RowDivider()
        SettingsRow(
            title = tr("Podseti da se spakuje torba", "Remind to pack the school bag"),
            subtitle = tr("Za dete, uveče u 19:00 pred školski dan", "For the child, at 19:00 the evening before a school day"),
            emoji = "🎒",
            checked = child.packReminderEnabled,
            onCheckedChange = { setPackReminder(child.id, it) },
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
