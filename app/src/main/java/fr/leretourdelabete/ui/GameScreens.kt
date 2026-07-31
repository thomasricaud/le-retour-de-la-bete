package fr.leretourdelabete.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.leretourdelabete.GameUiState
import fr.leretourdelabete.GameViewModel
import fr.leretourdelabete.R
import fr.leretourdelabete.domain.ConfirmedFirstNightTimeline
import fr.leretourdelabete.domain.ConfirmedLaterNightTimeline
import fr.leretourdelabete.domain.GameSequenceFactory
import fr.leretourdelabete.domain.HealingEffectRules
import fr.leretourdelabete.model.DayStage
import fr.leretourdelabete.model.DrawMode
import fr.leretourdelabete.model.EndReason
import fr.leretourdelabete.model.GameScreen
import fr.leretourdelabete.model.HealingOutcome
import fr.leretourdelabete.model.NightColor
import fr.leretourdelabete.ui.theme.BloodRedBright
import fr.leretourdelabete.ui.theme.Bone
import fr.leretourdelabete.ui.theme.GhoulGreen
import fr.leretourdelabete.ui.theme.MoonYellow
import fr.leretourdelabete.ui.theme.Parchment
import kotlinx.coroutines.delay

@Composable
fun SequenceScreen(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    var showStopDialog by remember { mutableStateOf(false) }
    var showPackDialog by remember { mutableStateOf(false) }
    var showPackConfirmation by remember { mutableStateOf(false) }
    val session = state.session
    val isIntro = session.screen == GameScreen.INTRO
    val isConfirmedLaterNight =
        state.currentCue?.id == ConfirmedLaterNightTimeline.CUE_ID
    val canCallPack = isConfirmedLaterNight &&
        ConfirmedLaterNightTimeline.canCall(state.cueRemainingMillis)

    LaunchedEffect(canCallPack) {
        if (!canCallPack) showPackConfirmation = false
    }
    LaunchedEffect(showPackConfirmation) {
        if (showPackConfirmation) {
            delay(10_000L)
            showPackConfirmation = false
        }
    }

    GameBackdrop(nightColor = session.currentNightColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isIntro) {
                        Tag("INTRODUCTION", BloodRedBright)
                    } else {
                        Tag("TOUR ${session.round}", Parchment)
                        NightColorLabel(session.currentNightColor)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AudioRouteBadge(state.audioRoute)
                    if (canCallPack) {
                        LargeActionButton(
                            label = "APPEL",
                            onClick = { showPackConfirmation = true },
                            tone = ActionTone.CALL,
                            iconRes = R.drawable.app_emblem,
                            showLabelWithIcon = true,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (state.isSequenceComplete && isIntro) {
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
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                "Tout est prêt ?",
                                style = MaterialTheme.typography.displayMedium,
                                color = Bone,
                            )
                            Text(
                                "La première nuit est spéciale : seul le loup-garou de sang " +
                                    "se réveille pour partir mordre un villageois qui deviendra " +
                                    "loup-garou.",
                                color = Parchment,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            LargeActionButton(
                                label = "LANCER LA PREMIÈRE NUIT",
                                onClick = viewModel::launchFirstNight,
                                modifier = Modifier.fillMaxWidth(0.72f),
                            )
                        }
                    }
                }
            } else {
                SequenceController(
                    state = state,
                    onReplay = viewModel::replayCurrentCue,
                    onPlayPause = viewModel::toggleSequencePlayback,
                    onSkip = viewModel::skipCurrentCue,
                    onStop = {
                        viewModel.pauseForStopDialog()
                        showStopDialog = true
                    },
                    showPackConfirmation = showPackConfirmation,
                    packCallVillagerLimit = session.packCallVillagerLimit,
                    onCancelPackCall = { showPackConfirmation = false },
                    onConfirmPackCall = {
                        showPackConfirmation = false
                        showPackDialog = true
                    },
                )
            }
        }
    }

    if (showStopDialog) {
        StopDialog(
            onDismiss = { showStopDialog = false },
            onPauseHome = {
                showStopDialog = false
                viewModel.pauseAndReturnHome()
            },
            onAbandon = {
                showStopDialog = false
                viewModel.finishGame(EndReason.ABANDONED)
            },
        )
    }

    if (showPackDialog) {
        PackCallDialog(
            villagerLimit = session.packCallVillagerLimit,
            onDismiss = { showPackDialog = false },
            onSuccess = {
                showPackDialog = false
                viewModel.finishGame(EndReason.PACK_CALL_SUCCESS)
            },
            onFailure = {
                showPackDialog = false
                viewModel.finishGame(EndReason.PACK_CALL_FAILURE)
            },
        )
    }
}

@Composable
private fun ColumnScope.SequenceController(
    state: GameUiState,
    onReplay: () -> Unit,
    onPlayPause: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
    showPackConfirmation: Boolean,
    packCallVillagerLimit: Int,
    onCancelPackCall: () -> Unit,
    onConfirmPackCall: () -> Unit,
) {
    val cue = state.currentCue
    val isConfirmedFirstNight = cue?.id == ConfirmedFirstNightTimeline.CUE_ID
    val isConfirmedLaterNight = cue?.id == ConfirmedLaterNightTimeline.CUE_ID
    val presentation = when {
        isConfirmedFirstNight ->
            ConfirmedFirstNightTimeline.presentation(state.cueRemainingMillis)
        isConfirmedLaterNight && state.session.currentNightColor != null ->
            ConfirmedLaterNightTimeline.presentation(
                remainingMillis = state.cueRemainingMillis,
                color = state.session.currentNightColor,
                packCallVillagerLimit = packCallVillagerLimit,
            )
        else -> null
    }
    val canReplay = cue?.replayable == true && (
        when {
            isConfirmedFirstNight ->
                ConfirmedFirstNightTimeline.canReplay(state.cueRemainingMillis)
            isConfirmedLaterNight ->
                ConfirmedLaterNightTimeline.canReplay(state.cueRemainingMillis)
            else -> true
        }
        )
    val canAdvance = when {
        isConfirmedFirstNight ->
            ConfirmedFirstNightTimeline.advanceTarget(state.cueRemainingMillis) != null
        isConfirmedLaterNight ->
            ConfirmedLaterNightTimeline.advanceTarget(state.cueRemainingMillis) != null
        else -> false
    }
    val skipLabel = if (canAdvance) {
        "AVANCER"
    } else {
        "PASSER"
    }
    val progress = if (state.cueTotalMillis > 0L) {
        1f - state.cueRemainingMillis.toFloat() / state.cueTotalMillis.toFloat()
    } else {
        0f
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassPanel(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(0.9f),
            containerAlpha = 0.64f,
        ) {
            if (showPackConfirmation) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Pensez-vous vraiment qu'il reste $packCallVillagerLimit " +
                            "villageois ou moins ?",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Bone,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.88f),
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(0.88f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        LargeActionButton(
                            label = "OUI",
                            onClick = onConfirmPackCall,
                            modifier = Modifier.weight(1f),
                        )
                        LargeActionButton(
                            label = "NON",
                            onClick = onCancelPackCall,
                            modifier = Modifier.weight(1f),
                            tone = ActionTone.SECONDARY,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                ) {
                    Text(
                        presentation?.title ?: cue?.title.orEmpty(),
                        style = MaterialTheme.typography.displayMedium,
                        color = Bone,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        presentation?.text ?: cue?.text.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Parchment,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.9f),
                    )
                    Text(
                        formatDuration(state.cueRemainingMillis),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(7.dp),
                        color = BloodRedBright,
                        trackColor = Parchment.copy(alpha = 0.18f),
                    )
                    if (!state.currentCueHasAudio) {
                        Tag("AUDIO À FOURNIR", color = MoonYellow)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight(0.9f),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            if (canReplay) {
                LargeActionButton(
                    "RÉPÉTER",
                    onReplay,
                    Modifier.fillMaxWidth(),
                    ActionTone.SECONDARY,
                    enabled = !showPackConfirmation,
                )
            }
            LargeActionButton(
                if (state.isSequencePlaying) "PAUSE" else "LECTURE",
                onPlayPause,
                Modifier.fillMaxWidth(),
                enabled = !showPackConfirmation,
            )
            LargeActionButton(
                skipLabel,
                onSkip,
                Modifier.fillMaxWidth(),
                ActionTone.SECONDARY,
                enabled = cue?.skippable == true && !showPackConfirmation,
            )
            LargeActionButton(
                "ARRÊTER",
                onStop,
                Modifier.fillMaxWidth(),
                ActionTone.DANGER,
                enabled = !showPackConfirmation,
            )
        }
    }
}

@Composable
fun NightReadyScreen(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    val session = state.session
    val colorWord = if (session.currentNightColor == NightColor.YELLOW) {
        "jaunes"
    } else {
        "vertes"
    }
    GameBackdrop(nightColor = session.currentNightColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tag("TOUR ${session.round}", Parchment)
                    NightColorLabel(session.currentNightColor)
                }
                AudioRouteBadge(state.audioRoute)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth(0.76f)
                        .fillMaxHeight(0.84f),
                    containerAlpha = 0.64f,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(
                            18.dp,
                            Alignment.CenterVertically,
                        ),
                    ) {
                        Text(
                            "Tout est prêt ?",
                            style = MaterialTheme.typography.displayMedium,
                            color = Bone,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "Cette nuit, il y aura un conseil des loups. Les éventuels " +
                                "loups-garous et goules $colorWord vont se réveiller et rejoindre " +
                                "le loup-garou de sang en présentant leurs pierres.",
                            color = Parchment,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth(0.88f),
                        )
                        LargeActionButton(
                            label = "LANCER LA ${frenchOrdinal(session.round).uppercase()} NUIT",
                            onClick = viewModel::launchNextNight,
                            modifier = Modifier.fillMaxWidth(0.72f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayScreen(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    var showStopDialog by remember { mutableStateOf(false) }
    val session = state.session
    GameBackdrop(isDay = true) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tag("TOUR ${session.round}", Parchment)
                    Tag("JOUR", MoonYellow)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AudioRouteBadge(state.audioRoute)
                    LargeActionButton(
                        "ARRÊTER",
                        {
                            viewModel.pauseForStopDialog()
                            showStopDialog = true
                        },
                        tone = ActionTone.DANGER,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when (session.dayStage) {
                    DayStage.HEALING -> HealingStage(state, viewModel)
                    else -> {
                        GlassPanel(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .fillMaxHeight(),
                            containerAlpha = 0.6f,
                        ) {
                            when (session.dayStage) {
                                DayStage.DISCUSSION -> DiscussionStage(state, viewModel)
                                DayStage.COUNCIL -> CouncilStage(state, viewModel)
                                DayStage.HEALING_EFFECT -> HealingEffectStage(state, viewModel)
                                DayStage.HEALING -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStopDialog) {
        StopDialog(
            onDismiss = { showStopDialog = false },
            onPauseHome = {
                showStopDialog = false
                viewModel.pauseAndReturnHome()
            },
            onAbandon = {
                showStopDialog = false
                viewModel.finishGame(EndReason.ABANDONED)
            },
        )
    }
}

@Composable
private fun DiscussionStage(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Concertation du village", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "Échangez librement, en groupe ou séparément.",
            color = Parchment,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(0.8f),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            if (state.session.dayDurationMinutes == 0) {
                "TEMPS LIBRE"
            } else {
                formatDuration(state.session.dayRemainingMillis)
            },
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(0.96f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.session.dayDurationMinutes > 0) {
                LargeActionButton(
                    if (state.dayTimerPlaying) "PAUSE" else "DÉMARRER",
                    viewModel::startOrPauseDayTimer,
                    modifier = Modifier.weight(1f),
                    tone = ActionTone.SECONDARY,
                )
                LargeActionButton(
                    "RECOMMENCER",
                    viewModel::resetDayTimer,
                    modifier = Modifier.weight(1f),
                    tone = ActionTone.SECONDARY,
                )
            }
            LargeActionButton(
                "PASSER AU CONSEIL",
                viewModel::advanceDayStage,
                modifier = Modifier.weight(1.25f),
            )
        }
    }
}

@Composable
private fun CouncilStage(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Conseil des villageois", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Qui doit subir le rituel de guérison ?",
            color = Parchment,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(0.78f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tous les joueurs se réunissent pour choisir un joueur à guérir. Votez selon " +
                "les modalités décidées en début de partie, y compris en cas d'égalité.",
            color = Parchment,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(0.78f),
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LargeActionButton(
                "PROCÉDER À LA GUÉRISON",
                viewModel::advanceDayStage,
            )
        }
    }
}

@Composable
private fun HealingStage(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassPanel(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            containerAlpha = 0.6f,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Rituel de guérison", style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Pendant le rituel, le joueur choisi révèle sa pierre. Indiquez alors " +
                        "quel était son rôle.",
                    color = Parchment,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth(0.82f),
                )
            }
        }

        Column(
            modifier = Modifier
                .width(230.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            HealingChoiceButton(
                label = "VILLAGEOIS",
                colors = listOf(Color(0xFF2364A3)),
                onClick = { viewModel.showHealingEffect(HealingOutcome.VILLAGER) },
            )
            if (HealingEffectRules.isGhoulOptionAvailable(state.session.round)) {
                HealingChoiceButton(
                    label = "GOULE",
                    colors = listOf(MoonYellow, GhoulGreen),
                    onClick = { viewModel.showHealingEffect(HealingOutcome.GHOUL) },
                )
            }
            HealingChoiceButton(
                label = "LOUP-GAROU",
                colors = listOf(Color(0xFF6C3FA0)),
                onClick = { viewModel.showHealingEffect(HealingOutcome.WEREWOLF) },
            )
            HealingChoiceButton(
                label = "LOUP DE SANG DÉCOUVERT",
                colors = listOf(Color(0xFF6F1922)),
                onClick = { viewModel.finishGame(EndReason.BLOOD_WOLF_FOUND) },
            )
        }
    }
}

@Composable
private fun HealingEffectStage(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    val text = HealingEffectRules.text(state.session.healingOutcome)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Effet de la guérison", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(14.dp))
        Text(
            text,
            color = Parchment,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(0.86f),
        )
        Spacer(Modifier.height(22.dp))
        LargeActionButton(
            "LA PARTIE CONTINUE",
            viewModel::continueAfterHealing,
        )
    }
}

@Composable
private fun HealingChoiceButton(
    label: String,
    colors: List<Color>,
    onClick: () -> Unit,
) {
    val gradientColors = if (colors.size == 1) {
        listOf(colors.first(), colors.first())
    } else {
        colors
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun DrawScreen(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    var showStopDialog by remember { mutableStateOf(false) }
    val session = state.session
    val nextColor = session.nextNightColor
    GameBackdrop(nightColor = nextColor) {
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
                    title = "Couleur de la prochaine nuit",
                    subtitle = "Tour ${session.round} terminé · ${session.remainingNightDeck.size} carte(s) dans le paquet",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AudioRouteBadge(state.audioRoute)
                    LargeActionButton(
                        "ARRÊTER",
                        {
                            viewModel.pauseForStopDialog()
                            showStopDialog = true
                        },
                        tone = ActionTone.DANGER,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                GlassPanel(
                    modifier = Modifier.fillMaxWidth(0.74f),
                    containerAlpha = 0.64f,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        if (nextColor == null) {
                            if (session.remainingNightDeck.isEmpty()) {
                                Text(
                                    "Le paquet de huit nuits est épuisé",
                                    style = MaterialTheme.typography.headlineLarge,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    "Les règles ne précisent pas la suite. Remélangez explicitement " +
                                        "les quatre nuits jaunes et les quatre nuits vertes.",
                                    color = Parchment,
                                    textAlign = TextAlign.Center,
                                )
                                LargeActionButton(
                                    "REMÉLANGER LES HUIT CARTES",
                                    viewModel::reshuffleNightDeck,
                                )
                            } else if (session.drawMode == DrawMode.APPLICATION) {
                                Text(
                                    "Le paquet contient quatre nuits jaunes et quatre nuits vertes, " +
                                        "tirées sans remise.",
                                    color = Parchment,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                LargeActionButton(
                                    "TIRER UNE CARTE NUIT",
                                    viewModel::drawAutomaticNight,
                                    modifier = Modifier.fillMaxWidth(0.62f),
                                )
                            } else {
                                Text(
                                    "Retournez la carte physique puis indiquez sa couleur.",
                                    color = Parchment,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                    LargeActionButton(
                                        "NUIT JAUNE",
                                        { viewModel.choosePhysicalNight(NightColor.YELLOW) },
                                        tone = ActionTone.YELLOW,
                                    )
                                    LargeActionButton(
                                        "NUIT VERTE",
                                        { viewModel.choosePhysicalNight(NightColor.GREEN) },
                                        tone = ActionTone.GREEN,
                                    )
                                }
                            }
                        } else {
                            NightColorLabel(nextColor)
                            Text(
                                if (nextColor == NightColor.YELLOW) {
                                    "Cette nuit, les goules jaunes vont se réveiller."
                                } else {
                                    "Cette nuit, les goules vertes vont se réveiller."
                                },
                                style = MaterialTheme.typography.displayMedium,
                                color = if (nextColor == NightColor.YELLOW) {
                                    MoonYellow
                                } else {
                                    GhoulGreen
                                },
                                textAlign = TextAlign.Center,
                            )
                            LargeActionButton(
                                "LANCER LE TOUR ${session.round + 1}",
                                viewModel::startNextNight,
                                modifier = Modifier.fillMaxWidth(0.62f),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showStopDialog) {
        StopDialog(
            onDismiss = { showStopDialog = false },
            onPauseHome = {
                showStopDialog = false
                viewModel.pauseAndReturnHome()
            },
            onAbandon = {
                showStopDialog = false
                viewModel.finishGame(EndReason.ABANDONED)
            },
        )
    }
}

@Composable
fun EndScreen(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    val reason = state.session.endReason ?: EndReason.ABANDONED
    val packCallVillagerLimit = state.session.packCallVillagerLimit
    val isDay = reason == EndReason.BLOOD_WOLF_FOUND ||
        reason == EndReason.PACK_CALL_FAILURE
    val title: String
    val text: String
    val cue = when (reason) {
        EndReason.BLOOD_WOLF_FOUND -> {
            title = "La Bête est vaincue"
            text = "Le loup-garou de sang s'est révélé lors de la guérison et a été tué. " +
                "Les villageois gagnent et tentent maintenant de guérir les loups-garous et " +
                "les goules. En cas d'échec de la guérison, c'est l'asile psychiatrique pour " +
                "ces pauvres âmes."
            GameSequenceFactory.endCue("blood_wolf_found", packCallVillagerLimit)
        }
        EndReason.PACK_CALL_SUCCESS -> {
            title = "La meute triomphe"
            text = "L'appel était juste : il restait $packCallVillagerLimit villageois ou " +
                "moins. Le loup-garou de sang et les loups gagnent. Les villageois restant " +
                "sont dévorés. Résolvez maintenant le sort des goules : seront-elles " +
                "remordues pour faire partie de la meute ou dévorées également ?"
            GameSequenceFactory.endCue("pack_success", packCallVillagerLimit)
        }
        EndReason.PACK_CALL_FAILURE -> {
            title = "Le village se soulève"
            text = "Erreur de la Bête ! Plus de $packCallVillagerLimit villageois dormaient " +
                "encore. Le village se réveille et tue le loup-garou de sang et sa meute de " +
                "loups-garous. Les villageois gagnent et tentent maintenant de guérir les " +
                "goules. En cas d'échec de la guérison, c'est l'asile psychiatrique pour ces " +
                "pauvres âmes."
            GameSequenceFactory.endCue("pack_failure", packCallVillagerLimit)
        }
        EndReason.ABANDONED -> {
            title = "Partie arrêtée"
            text = "La partie a été abandonnée sans déclarer de vainqueur."
            null
        }
    }

    GameBackdrop(isDay = isDay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassPanel(
                modifier = Modifier.fillMaxWidth(0.72f),
                containerAlpha = 0.64f,
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Tag("FIN DE PARTIE", BloodRedBright)
                    Text(
                        title,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text,
                        color = Parchment,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (cue != null) {
                            LargeActionButton(
                                "ÉCOUTER LA FIN",
                                { viewModel.playHelp(cue) },
                                tone = ActionTone.SECONDARY,
                            )
                        }
                        LargeActionButton(
                            "NOUVELLE PARTIE",
                            viewModel::openSetup,
                        )
                        LargeActionButton(
                            "ACCUEIL",
                            viewModel::returnToHome,
                            tone = ActionTone.SECONDARY,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StopDialog(
    onDismiss: () -> Unit,
    onPauseHome: () -> Unit,
    onAbandon: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Arrêter la lecture ?") },
        text = {
            Text(
                "La lecture est en pause. Vous pouvez sauvegarder la phase actuelle et revenir à l'accueil, " +
                    "ou abandonner définitivement cette partie.",
            )
        },
        confirmButton = {
            TextButton(onClick = onPauseHome) {
                Text("PAUSE ET ACCUEIL")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("ANNULER")
                }
                TextButton(onClick = onAbandon) {
                    Text("ABANDONNER", color = BloodRedBright)
                }
            }
        },
    )
}

@Composable
private fun PackCallDialog(
    villagerLimit: Int,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Venez à moi, ma meute, mes adorateurs !") },
        text = {
            Text(
                "Toutes les goules jaunes et vertes rejoignent le conseil des loups.\n" +
                    "Comptez alors le nombre de villageois restés endormis.",
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onSuccess) {
                Text("$villagerLimit VILLAGEOIS OU MOINS")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("ANNULER")
                }
                TextButton(onClick = onFailure) {
                    Text("PLUS DE $villagerLimit VILLAGEOIS")
                }
            }
        },
    )
}

private fun frenchOrdinal(round: Int): String = when (round) {
    1 -> "première"
    2 -> "deuxième"
    3 -> "troisième"
    4 -> "quatrième"
    5 -> "cinquième"
    6 -> "sixième"
    7 -> "septième"
    8 -> "huitième"
    9 -> "neuvième"
    10 -> "dixième"
    else -> "${round}e"
}
