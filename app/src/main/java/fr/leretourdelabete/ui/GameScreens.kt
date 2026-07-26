package fr.leretourdelabete.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.leretourdelabete.GameUiState
import fr.leretourdelabete.GameViewModel
import fr.leretourdelabete.domain.GameSequenceFactory
import fr.leretourdelabete.model.DayStage
import fr.leretourdelabete.model.DrawMode
import fr.leretourdelabete.model.EndReason
import fr.leretourdelabete.model.GameScreen
import fr.leretourdelabete.model.NightColor
import fr.leretourdelabete.ui.theme.BloodRedBright
import fr.leretourdelabete.ui.theme.Bone
import fr.leretourdelabete.ui.theme.GhoulGreen
import fr.leretourdelabete.ui.theme.MoonYellow
import fr.leretourdelabete.ui.theme.Parchment

@Composable
fun SequenceScreen(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    var showStopDialog by remember { mutableStateOf(false) }
    var showPackDialog by remember { mutableStateOf(false) }
    val session = state.session
    val isIntro = session.screen == GameScreen.INTRO

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
                    if (!isIntro && session.round > 1) {
                        LargeActionButton(
                            label = "APPEL DE LA MEUTE · ≤ " +
                                "${session.packCallVillagerLimit} VILLAGEOIS",
                            onClick = { showPackDialog = true },
                            tone = ActionTone.DANGER,
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
                                "Tout est prêt",
                                style = MaterialTheme.typography.displayMedium,
                                color = Bone,
                            )
                            Text(
                                "La première nuit est spéciale : aucune couleur n'est tirée et " +
                                    "seul le loup-garou de sang se réveille.",
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
            playerCount = session.playerCount,
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
) {
    val cue = state.currentCue
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
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    cue?.title.orEmpty(),
                    style = MaterialTheme.typography.displayMedium,
                    color = Bone,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    cue?.text.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Parchment,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.9f),
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    formatDuration(state.cueRemainingMillis),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(7.dp),
                    color = BloodRedBright,
                    trackColor = Parchment.copy(alpha = 0.18f),
                )
                if (!state.currentCueHasAudio) {
                    Spacer(Modifier.height(14.dp))
                    Tag("AUDIO À FOURNIR", color = MoonYellow)
                }
            }
        }

        Column(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight(0.9f),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            LargeActionButton(
                "RÉPÉTER",
                onReplay,
                Modifier.fillMaxWidth(),
                ActionTone.SECONDARY,
            )
            LargeActionButton(
                if (state.isSequencePlaying) "PAUSE" else "LECTURE",
                onPlayPause,
                Modifier.fillMaxWidth(),
            )
            LargeActionButton(
                "PASSER",
                onSkip,
                Modifier.fillMaxWidth(),
                ActionTone.SECONDARY,
                enabled = cue?.skippable == true,
            )
            LargeActionButton(
                "ARRÊTER",
                onStop,
                Modifier.fillMaxWidth(),
                ActionTone.DANGER,
            )
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
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .fillMaxHeight(),
                    containerAlpha = 0.6f,
                ) {
                    when (session.dayStage) {
                        DayStage.DISCUSSION -> DiscussionStage(state, viewModel)
                        DayStage.COUNCIL -> CouncilStage(viewModel)
                        DayStage.HEALING -> HealingStage(viewModel)
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
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Concertation du village", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            "Échangez librement, en groupe ou séparément. Observez les bruits, les absences " +
                "et les contradictions de la nuit.",
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.session.dayDurationMinutes > 0) {
                LargeActionButton(
                    if (state.dayTimerPlaying) "PAUSE" else "DÉMARRER",
                    viewModel::startOrPauseDayTimer,
                    tone = ActionTone.SECONDARY,
                )
                LargeActionButton(
                    "RECOMMENCER",
                    viewModel::resetDayTimer,
                    tone = ActionTone.SECONDARY,
                )
            }
            LargeActionButton(
                "PASSER AU CONSEIL",
                viewModel::advanceDayStage,
            )
        }
    }
}

@Composable
private fun CouncilStage(viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Conseil des villageois", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Réunissez tous les joueurs. Organisez le vote selon les modalités décidées au " +
                "début de la partie, y compris en cas d'égalité.",
            color = Parchment,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(0.78f),
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LargeActionButton(
                "ÉCOUTER L'ANNONCE",
                { viewModel.playHelp(GameSequenceFactory.dayCue("council")) },
                tone = ActionTone.SECONDARY,
            )
            LargeActionButton(
                "PROCÉDER À LA GUÉRISON",
                viewModel::advanceDayStage,
            )
        }
    }
}

@Composable
private fun HealingStage(viewModel: GameViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Rituel de guérison", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Le joueur choisi révèle seulement sa pierre au moment prévu et applique la règle " +
                "correspondante. L'application ne demande ni son nom ni son rôle.",
            color = Parchment,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth(0.78f),
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LargeActionButton(
                "ÉCOUTER L'ANNONCE",
                { viewModel.playHelp(GameSequenceFactory.dayCue("healing")) },
                tone = ActionTone.SECONDARY,
            )
            LargeActionButton(
                "LA PARTIE CONTINUE",
                viewModel::continueAfterHealing,
            )
            LargeActionButton(
                "LOUP DE SANG DÉCOUVERT",
                { viewModel.finishGame(EndReason.BLOOD_WOLF_FOUND) },
                tone = ActionTone.DANGER,
            )
        }
    }
}

@Composable
fun DrawScreen(
    state: GameUiState,
    viewModel: GameViewModel,
) {
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
                        "PAUSE ET ACCUEIL",
                        viewModel::pauseAndReturnHome,
                        tone = ActionTone.SECONDARY,
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
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                    LargeActionButton(
                                        "NUIT JAUNE",
                                        { viewModel.choosePhysicalNight(NightColor.YELLOW) },
                                        tone = ActionTone.SECONDARY,
                                    )
                                    LargeActionButton(
                                        "NUIT VERTE",
                                        { viewModel.choosePhysicalNight(NightColor.GREEN) },
                                        tone = ActionTone.SECONDARY,
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
}

@Composable
fun EndScreen(
    state: GameUiState,
    viewModel: GameViewModel,
) {
    val reason = state.session.endReason ?: EndReason.ABANDONED
    val playerCount = state.session.playerCount
    val packCallVillagerLimit = state.session.packCallVillagerLimit
    val isDay = reason == EndReason.BLOOD_WOLF_FOUND ||
        reason == EndReason.PACK_CALL_FAILURE
    val title: String
    val text: String
    val cue = when (reason) {
        EndReason.BLOOD_WOLF_FOUND -> {
            title = "La Bête est vaincue"
            text = "Le loup-garou de sang a été découvert pendant la guérison. Les villageois " +
                "gagnent et tentent maintenant de guérir les loups et les goules."
            GameSequenceFactory.endCue("blood_wolf_found", packCallVillagerLimit)
        }
        EndReason.PACK_CALL_SUCCESS -> {
            title = "La meute triomphe"
            text = "L'appel était juste : il restait $packCallVillagerLimit villageois ou moins " +
                "(seuil pour $playerCount joueurs). Le loup-garou de sang et les loups gagnent. " +
                "Résolvez maintenant le sort des goules."
            GameSequenceFactory.endCue("pack_success", packCallVillagerLimit)
        }
        EndReason.PACK_CALL_FAILURE -> {
            title = "Le village se soulève"
            text = "Plus de $packCallVillagerLimit villageois dormaient encore " +
                "(seuil pour $playerCount joueurs). L'appel était une erreur : le village tue " +
                "le loup-garou de sang et sa meute."
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
                                "RÉÉCOUTER LA FIN",
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
    playerCount: Int,
    villagerLimit: Int,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("« Venez à moi, ma meute… »") },
        text = {
            Text(
                "Pour $playerCount joueurs, l'appel est juste s'il reste $villagerLimit " +
                    "villageois ou moins. Toutes les goules rejoignent le conseil. Sans saisir " +
                    "aucun rôle, indiquez simplement si l'appel était juste.",
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
