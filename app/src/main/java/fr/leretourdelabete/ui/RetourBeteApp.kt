package fr.leretourdelabete.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.leretourdelabete.GameUiState
import fr.leretourdelabete.GameViewModel
import fr.leretourdelabete.R
import fr.leretourdelabete.domain.GameSequenceFactory
import fr.leretourdelabete.model.DrawMode
import fr.leretourdelabete.model.GameMode
import fr.leretourdelabete.model.GameScreen
import fr.leretourdelabete.ui.theme.BloodRedBright
import fr.leretourdelabete.ui.theme.Bone
import fr.leretourdelabete.ui.theme.GhoulGreen
import fr.leretourdelabete.ui.theme.MoonYellow
import fr.leretourdelabete.ui.theme.Parchment
import kotlinx.coroutines.delay

@Composable
fun RetourBeteApp(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val screen = state.session.screen

    BackHandler(enabled = screen != GameScreen.HOME) {
        when (screen) {
            GameScreen.SETUP,
            GameScreen.HELP,
            GameScreen.END,
            -> viewModel.returnToHome()
            else -> viewModel.pauseAndReturnHome()
        }
    }

    LaunchedEffect(state.statusMessage) {
        val message = state.statusMessage ?: return@LaunchedEffect
        delay(6_500L)
        if (viewModel.uiState.value.statusMessage == message) {
            viewModel.clearStatusMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (screen) {
            GameScreen.HOME -> HomeScreen(
                state = state,
                onNewGame = viewModel::openSetup,
                onResume = viewModel::resumeSavedGame,
                onHelp = viewModel::openHelp,
            )
            GameScreen.SETUP -> SetupScreen(
                state = state,
                onBack = viewModel::returnToHome,
                onUpdate = viewModel::updateSetup,
                onTestSpeaker = viewModel::testSpeaker,
                onBluetooth = viewModel::openBluetoothSettings,
                onStart = viewModel::startConfiguredGame,
            )
            GameScreen.INTRO,
            GameScreen.NIGHT,
            -> SequenceScreen(state = state, viewModel = viewModel)
            GameScreen.DAY -> DayScreen(state = state, viewModel = viewModel)
            GameScreen.DRAW -> DrawScreen(state = state, viewModel = viewModel)
            GameScreen.HELP -> HelpScreen(
                state = state,
                viewModel = viewModel,
                onBack = viewModel::returnToHome,
            )
            GameScreen.END -> EndScreen(state = state, viewModel = viewModel)
        }

        state.statusMessage?.let { message ->
            StatusBanner(
                message = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
                    .fillMaxWidth(0.76f),
            )
        }
    }
}

@Composable
private fun HomeScreen(
    state: GameUiState,
    onNewGame: () -> Unit,
    onResume: () -> Unit,
    onHelp: () -> Unit,
) {
    GameBackdrop {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(46.dp),
        ) {
            Column(
                modifier = Modifier.weight(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.app_emblem),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(260.dp),
                )
                Spacer(Modifier.height(10.dp))
                Tag("HORS LIGNE · AUCUN RÔLE ENREGISTRÉ", color = GhoulGreen)
            }

            Column(
                modifier = Modifier.weight(1.25f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Le Retour de la Bête",
                    style = MaterialTheme.typography.displayLarge,
                    color = Bone,
                )
                Text(
                    text = "Conducteur sonore de partie",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Parchment,
                )
                Spacer(Modifier.height(24.dp))
                LargeActionButton(
                    label = "NOUVELLE PARTIE",
                    onClick = onNewGame,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                LargeActionButton(
                    label = "REPRENDRE LA PARTIE",
                    onClick = onResume,
                    enabled = state.hasSavedGame,
                    tone = ActionTone.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                LargeActionButton(
                    label = "AIDES ET EXPLICATIONS",
                    onClick = onHelp,
                    tone = ActionTone.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Les pierres et les joueurs gardent tous leurs secrets. " +
                        "L'application ne pilote que le rythme, les tirages et les annonces.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Parchment.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onUpdate: ((fr.leretourdelabete.model.SetupOptions) -> fr.leretourdelabete.model.SetupOptions) -> Unit,
    onTestSpeaker: () -> Unit,
    onBluetooth: () -> Unit,
    onStart: () -> Unit,
) {
    val setup = state.setup
    GameBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ScreenTitle(
                    title = "Préparer la partie",
                    subtitle = "Une configuration simple, sans saisir aucun rôle.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AudioRouteBadge(state.audioRoute)
                    LargeActionButton(
                        label = "RETOUR",
                        onClick = onBack,
                        tone = ActionTone.SECONDARY,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GlassPanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("Format de la partie", style = MaterialTheme.typography.titleLarge)
                        StepperRow(
                            label = "Nombre de joueurs",
                            value = setup.playerCount.toString(),
                            onMinus = {
                                onUpdate { it.copy(playerCount = (it.playerCount - 1).coerceAtLeast(4)) }
                            },
                            onPlus = {
                                onUpdate { it.copy(playerCount = (it.playerCount + 1).coerceAtMost(20)) }
                            },
                        )
                        OptionLabel("Niveau de guidage")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleChoice(
                                "DÉBUTANT",
                                setup.mode == GameMode.BEGINNER,
                                { onUpdate { it.copy(mode = GameMode.BEGINNER) } },
                                Modifier.weight(1f),
                            )
                            ToggleChoice(
                                "CONFIRMÉ",
                                setup.mode == GameMode.CONFIRMED,
                                { onUpdate { it.copy(mode = GameMode.CONFIRMED) } },
                                Modifier.weight(1f),
                            )
                        }
                        OptionLabel("Tirage des nuits")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleChoice(
                                "DANS L'APPLI",
                                setup.drawMode == DrawMode.APPLICATION,
                                { onUpdate { it.copy(drawMode = DrawMode.APPLICATION) } },
                                Modifier.weight(1f),
                            )
                            ToggleChoice(
                                "CARTES PHYSIQUES",
                                setup.drawMode == DrawMode.PHYSICAL_CARDS,
                                { onUpdate { it.copy(drawMode = DrawMode.PHYSICAL_CARDS) } },
                                Modifier.weight(1f),
                            )
                        }
                        OptionLabel("Temps pour rejoindre les habitations")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30, 45).forEach { seconds ->
                                ToggleChoice(
                                    "$seconds SECONDES",
                                    setup.departureSeconds == seconds,
                                    { onUpdate { it.copy(departureSeconds = seconds) } },
                                    Modifier.weight(1f),
                                    accent = MoonYellow,
                                )
                            }
                        }
                        OptionLabel("Durée de concertation du jour")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0, 3, 5, 8).forEach { minutes ->
                                ToggleChoice(
                                    if (minutes == 0) "LIBRE" else "$minutes MIN",
                                    setup.dayDurationMinutes == minutes,
                                    { onUpdate { it.copy(dayDurationMinutes = minutes) } },
                                    Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            MaterialSummary(setup.playerCount)
                            HorizontalDivider(color = Parchment.copy(alpha = 0.25f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    if (state.audioRoute.external) {
                                        "ENCEINTE DÉTECTÉE"
                                    } else {
                                        "SON DU TÉLÉPHONE"
                                    },
                                    color = if (state.audioRoute.external) GhoulGreen else Parchment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(0.8f),
                                )
                                LargeActionButton(
                                    "TESTER",
                                    onTestSpeaker,
                                    Modifier.weight(1f),
                                    ActionTone.SECONDARY,
                                )
                                LargeActionButton(
                                    "RÉGLAGES BT",
                                    onBluetooth,
                                    Modifier.weight(1f),
                                    ActionTone.SECONDARY,
                                )
                            }
                        }
                    }
                    LargeActionButton(
                        label = "LANCER LA PARTIE",
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpScreen(
    state: GameUiState,
    viewModel: GameViewModel,
    onBack: () -> Unit,
) {
    val cues = GameSequenceFactory.helpCues()
    GameBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScreenTitle(
                    title = "Aides et explications",
                    subtitle = "Ces aides sont destinées à être écoutées avant la partie.",
                )
                LargeActionButton("RETOUR", onBack, tone = ActionTone.SECONDARY)
            }
            Spacer(Modifier.height(14.dp))
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(cues.size, key = { cues[it].id }) { index ->
                    val cue = cues[index]
                    val available = viewModel.isAudioAvailable(cue)
                    GlassPanel(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cue.title, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    cue.text,
                                    color = Parchment,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            Tag(
                                if (available) "AUDIO PRÊT" else "AUDIO À FOURNIR",
                                color = if (available) GhoulGreen else MoonYellow,
                            )
                            LargeActionButton(
                                label = if (state.standaloneCueId == cue.id) "ARRÊTER" else "ÉCOUTER",
                                onClick = {
                                    if (state.standaloneCueId == cue.id) {
                                        viewModel.stopStandalone()
                                    } else {
                                        viewModel.playHelp(cue)
                                    }
                                },
                                tone = ActionTone.SECONDARY,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Parchment, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToggleChoice("−", false, onMinus)
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(64.dp),
            )
            ToggleChoice("+", false, onPlus)
        }
    }
}

@Composable
private fun OptionLabel(text: String) {
    Text(
        text = text,
        color = Parchment,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun MaterialSummary(playerCount: Int) {
    val secondary = playerCount - 1
    val yellow = (secondary + 1) / 2
    val green = secondary / 2
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Matériel pour $playerCount joueurs", style = MaterialTheme.typography.titleLarge)
        InlineLabelValue("Tirage des rôles", "1 rouge + $secondary bleues")
        InlineLabelValue("Sac + goules", "$secondary bleues · $yellow jaunes · $green vertes")
        InlineLabelValue("Loups + Nuits", "$playerCount violettes · 4 jaunes · 4 vertes")
        Text(
            "Aucun nom ni rôle n'est saisi dans l'application.",
            color = Parchment.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
