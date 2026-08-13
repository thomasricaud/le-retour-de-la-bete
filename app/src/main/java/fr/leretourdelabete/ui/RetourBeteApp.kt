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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.leretourdelabete.GameUiState
import fr.leretourdelabete.GameViewModel
import fr.leretourdelabete.BuildConfig
import fr.leretourdelabete.R
import fr.leretourdelabete.domain.GameSequenceFactory
import fr.leretourdelabete.domain.BeginnerSetupGuidance
import fr.leretourdelabete.domain.PackCallRule
import fr.leretourdelabete.domain.SetupPreviewPermission
import fr.leretourdelabete.model.AiVoice
import fr.leretourdelabete.model.DrawMode
import fr.leretourdelabete.model.GameMode
import fr.leretourdelabete.model.GameScreen
import fr.leretourdelabete.ui.theme.BloodRedBright
import fr.leretourdelabete.ui.theme.Bone
import fr.leretourdelabete.ui.theme.MoonYellow
import fr.leretourdelabete.ui.theme.Parchment
import kotlinx.coroutines.delay

@Composable
fun RetourBeteApp(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val screen = state.session.screen
    val beginnerGuidance = state.beginnerGuidance

    BackHandler(enabled = beginnerGuidance != null) {
        val step = beginnerGuidance
            ?.stepIndex
            ?.let(BeginnerSetupGuidance.steps::getOrNull)
        if (step?.showCancel == true) viewModel.cancelBeginnerGuidance()
    }

    BackHandler(enabled = screen != GameScreen.HOME && beginnerGuidance == null) {
        when (screen) {
            GameScreen.BLUETOOTH_SETUP,
            GameScreen.HELP,
            GameScreen.END,
            -> viewModel.returnToHome()
            GameScreen.SETUP -> viewModel.returnToBluetoothSetup()
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
            GameScreen.BLUETOOTH_SETUP -> BluetoothSetupScreen(
                state = state,
                onBack = viewModel::returnToHome,
                onTestSpeaker = viewModel::testSpeaker,
                onBluetooth = viewModel::openBluetoothSettings,
                onContinue = viewModel::continueToSetup,
            )
            GameScreen.SETUP -> SetupScreen(
                state = state,
                onBack = viewModel::returnToBluetoothSetup,
                onUpdate = viewModel::updateSetup,
                onSelectGuidanceMode = viewModel::selectGuidanceMode,
                onStart = viewModel::startConfiguredGame,
            )
            GameScreen.INTRO,
            GameScreen.NIGHT,
            -> SequenceScreen(state = state, viewModel = viewModel)
            GameScreen.NIGHT_READY -> NightReadyScreen(state = state, viewModel = viewModel)
            GameScreen.DAY -> DayScreen(state = state, viewModel = viewModel)
            GameScreen.DRAW -> DrawScreen(state = state, viewModel = viewModel)
            GameScreen.HELP -> HelpScreen(
                state = state,
                viewModel = viewModel,
                onBack = viewModel::returnToHome,
            )
            GameScreen.END -> EndScreen(state = state, viewModel = viewModel)
        }

        val bottomMessage = state.updateDownload
            ?.takeUnless { it.readyToInstall }
            ?.let(::formatUpdateDownloadProgress)
            ?: state.statusMessage
        bottomMessage?.let { message ->
            StatusBanner(
                message = message,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 14.dp)
                    .fillMaxWidth(0.76f),
            )
        }

        if (screen == GameScreen.HOME) {
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = Parchment.copy(alpha = 0.75f),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }

    beginnerGuidance
        ?.takeUnless { it.isPreviewing }
        ?.let { guidance ->
            val step = BeginnerSetupGuidance.steps[guidance.stepIndex]
            Dialog(
                onDismissRequest = {
                    if (step.showCancel) viewModel.cancelBeginnerGuidance()
                },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(0.86f),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "Guidage · ${step.number}/${BeginnerSetupGuidance.steps.size}",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            if (guidance.isComplete) {
                                "Étape terminée. Appuyez sur SUIVANT."
                            } else if (guidance.isPlaying) {
                                "Écoutez les instructions. Le déroulement de l'application est suspendu."
                            } else {
                                "Lecture en pause."
                            },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                space = 10.dp,
                                alignment = Alignment.End,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (step.showCancel) {
                                TextButton(onClick = viewModel::cancelBeginnerGuidance) {
                                    Text("ANNULER", softWrap = false)
                                }
                            }
                            if (step.showPlaybackControls) {
                                TextButton(onClick = viewModel::repeatBeginnerGuidance) {
                                    Text("RÉPÉTER", softWrap = false)
                                }
                                TextButton(onClick = viewModel::toggleBeginnerGuidancePlayback) {
                                    Text(
                                        if (guidance.isPlaying) "PAUSE" else "LECTURE",
                                        softWrap = false,
                                    )
                                }
                            }
                            if (step.canPreview) {
                                TextButton(onClick = viewModel::previewBeginnerGuidanceScreen) {
                                    Text("VOIR", softWrap = false)
                                }
                            }
                            TextButton(onClick = viewModel::advanceBeginnerGuidance) {
                                Text("SUIVANT", softWrap = false)
                            }
                        }
                    }
                }
            }
        }

    state.availableUpdate?.takeIf { screen == GameScreen.HOME }?.let { update ->
        AlertDialog(
            onDismissRequest = viewModel::dismissAvailableUpdate,
            title = {
                Text("Mise à jour disponible")
            },
            text = {
                Text(
                    "La version ${update.latestVersion} est disponible " +
                        "(version installée : ${update.currentVersion}). " +
                        "Voulez-vous ouvrir le téléchargement ?",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::startAvailableUpdateDownload) {
                    Text("TÉLÉCHARGER")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAvailableUpdate) {
                    Text("PLUS TARD")
                }
            },
        )
    }

    state.updateDownload
        ?.takeIf { it.readyToInstall && state.showUpdateInstallPrompt && screen == GameScreen.HOME }
        ?.let { download ->
            AlertDialog(
                onDismissRequest = viewModel::dismissUpdateInstallPrompt,
                title = {
                    Text("Mise à jour téléchargée")
                },
                text = {
                    Text(
                        "La version ${download.version} est prête. " +
                            "Android peut demander d’autoriser temporairement " +
                            "l’installation depuis cette application.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::installDownloadedUpdate) {
                        Text("INSTALLER")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissUpdateInstallPrompt) {
                        Text("PLUS TARD")
                    }
                },
            )
        }
}

private fun formatUpdateDownloadProgress(download: fr.leretourdelabete.UpdateDownloadUiState): String {
    val total = download.totalBytes
    return if (total > 0L) {
        val percent = (download.downloadedBytes * 100L / total).coerceIn(0L, 100L)
        "Téléchargement de la mise à jour ${download.version} : $percent %"
    } else {
        "Téléchargement de la mise à jour ${download.version}…"
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
private fun BluetoothSetupScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onTestSpeaker: () -> Unit,
    onBluetooth: () -> Unit,
    onContinue: () -> Unit,
) {
    GameBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ScreenTitle(
                    title = "Connexion Bluetooth",
                    subtitle = "Vérifiez la connexion de votre téléphone au système audio.",
                )
                LargeActionButton(
                    label = "RETOUR",
                    onClick = onBack,
                    tone = ActionTone.SECONDARY,
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(0.72f),
                    containerAlpha = 0.64f,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        AudioRouteBadge(state.audioRoute)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LargeActionButton(
                                label = "Réglages Bluetooth",
                                onClick = onBluetooth,
                                modifier = Modifier.weight(1f),
                                tone = ActionTone.SECONDARY,
                                iconRes = R.drawable.ic_bluetooth,
                            )
                            LargeActionButton(
                                label = "TESTER",
                                onClick = onTestSpeaker,
                                modifier = Modifier.weight(1f),
                                tone = ActionTone.SECONDARY,
                            )
                            LargeActionButton(
                                label = "PASSER",
                                onClick = onContinue,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(
    state: GameUiState,
    onBack: () -> Unit,
    onUpdate: ((fr.leretourdelabete.model.SetupOptions) -> fr.leretourdelabete.model.SetupOptions) -> Unit,
    onSelectGuidanceMode: (GameMode) -> Unit,
    onStart: () -> Unit,
) {
    val setup = state.setup
    val guidance = state.beginnerGuidance
    val previewPermission = guidance
        ?.takeIf { it.isPreviewing }
        ?.let { BeginnerSetupGuidance.steps[it.stepIndex].previewPermission }
    val playerCountEnabled = guidance == null || previewPermission in setOf(
        SetupPreviewPermission.PLAYER_COUNT,
        SetupPreviewPermission.PLAYER_COUNT_AND_DRAW_MODE,
    )
    val drawModeEnabled = guidance == null ||
        previewPermission == SetupPreviewPermission.PLAYER_COUNT_AND_DRAW_MODE
    val generalOptionsEnabled = guidance == null
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
                    subtitle = "Choisir le format et préparer la table.",
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
                            enabled = playerCountEnabled,
                        )
                        OptionLabel("Niveau de guidage")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleChoice(
                                "DÉBUTANT",
                                setup.mode == GameMode.BEGINNER,
                                { onSelectGuidanceMode(GameMode.BEGINNER) },
                                Modifier.weight(1f),
                                enabled = generalOptionsEnabled,
                            )
                            ToggleChoice(
                                "CONFIRMÉ",
                                setup.mode == GameMode.CONFIRMED,
                                { onSelectGuidanceMode(GameMode.CONFIRMED) },
                                Modifier.weight(1f),
                                enabled = generalOptionsEnabled,
                            )
                        }
                        OptionLabel("Tirage des nuits")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleChoice(
                                "DANS L'APPLI",
                                setup.drawMode == DrawMode.APPLICATION,
                                { onUpdate { it.copy(drawMode = DrawMode.APPLICATION) } },
                                Modifier.weight(1f),
                                enabled = drawModeEnabled,
                            )
                            ToggleChoice(
                                "CARTES PHYSIQUES",
                                setup.drawMode == DrawMode.PHYSICAL_CARDS,
                                { onUpdate { it.copy(drawMode = DrawMode.PHYSICAL_CARDS) } },
                                Modifier.weight(1f),
                                enabled = drawModeEnabled,
                            )
                        }
                        OptionLabel("Durée de concertation du jour")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0, 3, 5, 8).forEach { minutes ->
                                ToggleChoice(
                                    if (minutes == 0) "LIBRE" else "$minutes MIN",
                                    setup.dayDurationMinutes == minutes,
                                    { onUpdate { it.copy(dayDurationMinutes = minutes) } },
                                    Modifier.weight(1f),
                                    enabled = generalOptionsEnabled,
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Checkbox(
                                checked = setup.dayAmbienceEnabled,
                                enabled = generalOptionsEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdate { it.copy(dayAmbienceEnabled = enabled) }
                                },
                            )
                            Text(
                                "Ambiance sonore le jour",
                                color = Parchment,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        OptionLabel("Voix IA")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleChoice(
                                "HOMME",
                                setup.aiVoice == AiVoice.MALE,
                                { onUpdate { it.copy(aiVoice = AiVoice.MALE) } },
                                Modifier.weight(1f),
                                enabled = generalOptionsEnabled,
                            )
                            ToggleChoice(
                                "FEMME",
                                setup.aiVoice == AiVoice.FEMALE,
                                { onUpdate { it.copy(aiVoice = AiVoice.FEMALE) } },
                                Modifier.weight(1f),
                                enabled = generalOptionsEnabled,
                            )
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
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                        ) {
                            MaterialSummary(
                                playerCount = setup.playerCount,
                                drawMode = setup.drawMode,
                            )
                        }
                    }
                    LargeActionButton(
                        label = "LANCER LA PARTIE",
                        onClick = onStart,
                        enabled = generalOptionsEnabled,
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
    val cues = GameSequenceFactory.helpCues(state.session.packCallVillagerLimit)
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
                            if (!available) {
                                Tag("AUDIO À FOURNIR", color = MoonYellow)
                            }
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
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Parchment, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToggleChoice("−", false, onMinus, enabled = enabled)
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(64.dp),
            )
            ToggleChoice("+", false, onPlus, enabled = enabled)
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
private fun MaterialSummary(
    playerCount: Int,
    drawMode: DrawMode,
) {
    val secondary = playerCount - 1
    val yellow = (secondary + 1) / 2
    val green = secondary / 2
    val packCallVillagerLimit = PackCallRule.maxRemainingVillagers(playerCount)
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            "Préparation de la table pour $playerCount joueurs",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Pierres dans le sac de guérison",
            color = Bone,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
        )
        InlineLabelValue("Tirez les rôles", "1 rouge + $secondary bleues")
        InlineLabelValue(
            "puis placez",
            "$secondary bleues + $yellow jaunes + $green vertes",
        )
        InlineLabelValue(
            "Pierres dans la boîte des loups",
            "$secondary violettes",
        )
        if (drawMode == DrawMode.PHYSICAL_CARDS) {
            InlineLabelValue(
                "Cartes nuits mélangées",
                "4 jaunes + 4 vertes",
            )
        }

        Spacer(Modifier.height(5.dp))
        Text(
            "Objectifs et conditions de victoire",
            style = MaterialTheme.typography.titleLarge,
        )
        ObjectiveLine(
            role = "Villageois (pierre bleue) : ",
            objective = "découvrir le loup-garou de sang en votant pour sa guérison " +
                "lors du conseil des villageois (le jour).",
        )
        ObjectiveLine(
            role = "Loup-garou de sang (pierre rouge) : ",
            objective = "appeler « sa meute, ses adorateurs » lors du conseil des loups " +
                "(la nuit) quand il ne reste plus que $packCallVillagerLimit " +
                "villageois ou moins.",
        )
        ObjectiveLine(
            role = "Loup-garou (pierre violette) : ",
            objective = "aider le loup-garou de sang pour gagner avec lui (si c'est le " +
                "village qui gagne, votre victoire est incertaine).",
        )
        ObjectiveLine(
            role = "Goule (pierre jaune ou verte) : ",
            objective = "aider le loup-garou de sang pour gagner avec lui (dans tous les " +
                "cas, votre victoire est incertaine).",
        )
    }
}

@Composable
private fun ObjectiveLine(
    role: String,
    objective: String,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(role)
            }
            append(objective)
        },
        color = Parchment,
        style = MaterialTheme.typography.bodySmall,
    )
}
