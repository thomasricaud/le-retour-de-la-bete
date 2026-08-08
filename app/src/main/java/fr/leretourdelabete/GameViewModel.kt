package fr.leretourdelabete

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.leretourdelabete.audio.AudioEngine
import fr.leretourdelabete.audio.PlaybackInfo
import fr.leretourdelabete.audio.AudioRouteMonitor
import fr.leretourdelabete.audio.AudioRouteState
import fr.leretourdelabete.data.GameSessionRepository
import fr.leretourdelabete.data.AppUpdate
import fr.leretourdelabete.data.AppUpdateDownloadManager
import fr.leretourdelabete.data.AppUpdateDownloadStatus
import fr.leretourdelabete.data.AppUpdateInstallResult
import fr.leretourdelabete.data.GitHubReleaseUpdateChecker
import fr.leretourdelabete.data.PendingAppUpdateDownload
import fr.leretourdelabete.domain.CueKind
import fr.leretourdelabete.domain.ConfirmedFirstNightTimeline
import fr.leretourdelabete.domain.ConfirmedLaterNightTimeline
import fr.leretourdelabete.domain.GameCue
import fr.leretourdelabete.domain.GameSequenceFactory
import fr.leretourdelabete.domain.NightDeck
import fr.leretourdelabete.domain.PackCallRule
import fr.leretourdelabete.domain.VioletStoneRules
import fr.leretourdelabete.model.DayStage
import fr.leretourdelabete.model.DrawMode
import fr.leretourdelabete.model.EndReason
import fr.leretourdelabete.model.GameScreen
import fr.leretourdelabete.model.GameSession
import fr.leretourdelabete.model.HealingOutcome
import fr.leretourdelabete.model.NightColor
import fr.leretourdelabete.model.SetupOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UpdateDownloadUiState(
    val version: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val readyToInstall: Boolean = false,
)

data class GameUiState(
    val session: GameSession = GameSession(),
    val setup: SetupOptions = SetupOptions(),
    val hasSavedGame: Boolean = false,
    val currentCue: GameCue? = null,
    val cueTotalMillis: Long = 0L,
    val cueRemainingMillis: Long = 0L,
    val isSequencePlaying: Boolean = false,
    val isSequenceComplete: Boolean = false,
    val currentCueHasAudio: Boolean = false,
    val dayTimerPlaying: Boolean = false,
    val dayTimerDisplayResetKey: Int = 0,
    val audioRoute: AudioRouteState = AudioRouteState(),
    val statusMessage: String? = null,
    val standaloneCueId: String? = null,
    val availableUpdate: AppUpdate? = null,
    val updateDownload: UpdateDownloadUiState? = null,
    val showUpdateInstallPrompt: Boolean = false,
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameSessionRepository(application)
    private val updateChecker = GitHubReleaseUpdateChecker()
    private val updateDownloadManager = AppUpdateDownloadManager(application)
    private val audioEngine = AudioEngine(application) {
        handleAudioInterruption(
            "Lecture mise en pause : une autre application utilise le son.",
        )
    }
    private val routeMonitor = AudioRouteMonitor(application) { route ->
        val previous = _uiState.value.audioRoute
        _uiState.value = _uiState.value.copy(audioRoute = route)
        if (previous.external && !route.external) {
            handleAudioInterruption(
                "Enceinte déconnectée. La partie et les sons d'ambiance sont en pause.",
            )
        }
    }

    private val _uiState = MutableStateFlow(
        GameUiState(hasSavedGame = repository.hasResumableSession()),
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var activeSequence: List<GameCue> = emptyList()
    private var sequenceJob: Job? = null
    private var dayTimerJob: Job? = null
    private var dayStageAudioJob: Job? = null
    private var standaloneJob: Job? = null
    private var updateDownloadJob: Job? = null
    private var lastPersistedSecond: Long = -1L
    private var waitingForInstallPermission = false

    init {
        routeMonitor.start()
        if (!BuildConfig.DEBUG) {
            updateDownloadManager.clearIfAlreadyInstalled(BuildConfig.VERSION_NAME)
            val pendingDownload = updateDownloadManager.pendingDownload()
            if (pendingDownload != null) {
                monitorUpdateDownload(pendingDownload)
            } else {
                viewModelScope.launch {
                    updateChecker.findAvailableUpdate()?.let { update ->
                        _uiState.value = _uiState.value.copy(availableUpdate = update)
                    }
                }
            }
        }
    }

    fun updateSetup(transform: (SetupOptions) -> SetupOptions) {
        _uiState.value = _uiState.value.copy(setup = transform(_uiState.value.setup))
    }

    fun openSetup() {
        stopAllPlayback()
        _uiState.value = _uiState.value.copy(
            session = GameSession(screen = GameScreen.BLUETOOTH_SETUP),
            statusMessage = null,
        )
    }

    fun continueToSetup() {
        _uiState.value = _uiState.value.copy(
            session = GameSession(screen = GameScreen.SETUP),
            statusMessage = null,
        )
    }

    fun returnToBluetoothSetup() {
        _uiState.value = _uiState.value.copy(
            session = GameSession(screen = GameScreen.BLUETOOTH_SETUP),
            statusMessage = null,
        )
    }

    fun startConfiguredGame() {
        stopAllPlayback()
        val setup = _uiState.value.setup
        val session = GameSession(
            screen = GameScreen.INTRO,
            mode = setup.mode,
            drawMode = setup.drawMode,
            playerCount = setup.playerCount,
            violetStonesInWolfBox = VioletStoneRules.initialCount(setup.playerCount),
            packCallVillagerLimit = PackCallRule.maxRemainingVillagers(setup.playerCount),
            dayDurationMinutes = setup.dayDurationMinutes,
            dayAmbienceEnabled = setup.dayAmbienceEnabled,
            aiVoice = setup.aiVoice,
            round = 1,
            currentNightColor = null,
            remainingNightDeck = NightDeck.shuffled(),
            dayRemainingMillis = setup.dayDurationMinutes * 60_000L,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            hasSavedGame = true,
            statusMessage = null,
        )
        loadSequenceFor(session, autoplay = true)
    }

    fun resumeSavedGame() {
        val saved = repository.load()?.takeIf { it.isResumable } ?: return
        stopAllPlayback()
        _uiState.value = _uiState.value.copy(
            session = saved,
            setup = SetupOptions(
                playerCount = saved.playerCount,
                mode = saved.mode,
                drawMode = saved.drawMode,
                dayDurationMinutes = saved.dayDurationMinutes,
                dayAmbienceEnabled = saved.dayAmbienceEnabled,
                aiVoice = saved.aiVoice,
            ),
            hasSavedGame = true,
            statusMessage = if (saved.screen == GameScreen.NIGHT_READY) {
                "Partie restaurée. Lancez la nuit lorsque tout est prêt."
            } else {
                "Partie restaurée. Appuyez sur Lecture pour reprendre."
            },
        )
        if (saved.screen == GameScreen.INTRO || saved.screen == GameScreen.NIGHT) {
            loadSequenceFor(saved, autoplay = false)
        } else if (saved.screen == GameScreen.DAY) {
            startDayStageAudio(saved.dayStage)
        } else if (saved.screen == GameScreen.DRAW) {
            startPhaseAmbience()
        }
    }

    fun launchFirstNight() {
        val session = _uiState.value.session.copy(
            screen = GameScreen.NIGHT,
            cueIndex = 0,
            cueRemainingMillis = 0L,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            isSequenceComplete = false,
            statusMessage = null,
        )
        loadSequenceFor(session, autoplay = true)
    }

    fun toggleSequencePlayback() {
        if (_uiState.value.isSequenceComplete) return
        if (_uiState.value.isSequencePlaying) {
            pauseSequence()
        } else {
            playCurrentCue()
        }
    }

    fun replayCurrentCue() {
        val cue = _uiState.value.currentCue ?: return
        if (!cue.replayable) return
        if (
            cue.id == ConfirmedFirstNightTimeline.CUE_ID &&
            !ConfirmedFirstNightTimeline.canReplay(_uiState.value.cueRemainingMillis)
        ) {
            return
        }
        if (
            cue.id == ConfirmedLaterNightTimeline.CUE_ID &&
            !ConfirmedLaterNightTimeline.canReplay(_uiState.value.cueRemainingMillis)
        ) {
            return
        }
        stopSequenceClock()
        audioEngine.stopForeground()
        updateSessionPlayback(
            cueIndex = _uiState.value.session.cueIndex,
            remainingMillis = cue.fallbackDurationMillis,
        )
        playCurrentCue(reset = true)
    }

    fun skipCurrentCue() {
        val cue = _uiState.value.currentCue ?: return
        if (!cue.skippable) return
        if (cue.id == ConfirmedFirstNightTimeline.CUE_ID) {
            val target = ConfirmedFirstNightTimeline.advanceTarget(
                _uiState.value.cueRemainingMillis,
            )
            if (target != null) {
                jumpTimedNight(target)
            } else {
                advanceCue()
            }
            return
        }
        if (cue.id == ConfirmedLaterNightTimeline.CUE_ID) {
            val target = ConfirmedLaterNightTimeline.advanceTarget(
                _uiState.value.cueRemainingMillis,
            )
            if (target != null) {
                jumpTimedNight(target)
            } else {
                advanceCue()
            }
            return
        }
        advanceCue()
    }

    fun pauseForStopDialog() {
        pauseSequence()
        dayStageAudioJob?.cancel()
        dayStageAudioJob = null
        if (_uiState.value.dayTimerPlaying) {
            dayTimerJob?.cancel()
            dayTimerJob = null
            _uiState.value = _uiState.value.copy(dayTimerPlaying = false)
            repository.save(_uiState.value.session)
        }
        stopStandalone()
        audioEngine.stopAmbience()
    }

    fun pauseForPackCallDialog() {
        pauseSequence()
    }

    fun pauseAndReturnHome() {
        pauseSequence()
        dayTimerJob?.cancel()
        dayTimerJob = null
        dayStageAudioJob?.cancel()
        dayStageAudioJob = null
        audioEngine.stopAll()
        repository.save(_uiState.value.session)
        _uiState.value = _uiState.value.copy(
            session = _uiState.value.session.copy(screen = GameScreen.HOME),
            hasSavedGame = true,
            currentCue = null,
            isSequencePlaying = false,
            statusMessage = null,
        )
    }

    fun discardAndReturnHome() {
        stopAllPlayback()
        repository.clear()
        _uiState.value = GameUiState(
            hasSavedGame = false,
            audioRoute = _uiState.value.audioRoute,
            setup = _uiState.value.setup,
            availableUpdate = _uiState.value.availableUpdate,
            updateDownload = _uiState.value.updateDownload,
            showUpdateInstallPrompt = _uiState.value.showUpdateInstallPrompt,
        )
    }

    fun startOrPauseDayTimer() {
        val state = _uiState.value
        if (state.session.dayDurationMinutes == 0) return
        if (state.dayTimerPlaying) {
            dayTimerJob?.cancel()
            dayTimerJob = null
            audioEngine.pauseAmbience()
            _uiState.value = state.copy(dayTimerPlaying = false)
            repository.save(state.session)
        } else {
            startDiscussionAudio(restart = false)
            startDayTimer()
        }
    }

    fun resetDayTimer() {
        dayTimerJob?.cancel()
        dayTimerJob = null
        stopDayStageAudio()
        val duration = _uiState.value.session.dayDurationMinutes * 60_000L
        val session = _uiState.value.session.copy(dayRemainingMillis = duration)
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            dayTimerPlaying = false,
            dayTimerDisplayResetKey = _uiState.value.dayTimerDisplayResetKey + 1,
        )
    }

    fun advanceDayStage() {
        val current = _uiState.value.session
        val next = when (current.dayStage) {
            DayStage.DISCUSSION -> DayStage.COUNCIL
            DayStage.COUNCIL -> DayStage.HEALING
            DayStage.HEALING,
            DayStage.HEALING_EFFECT,
            -> return
        }
        moveToDayStage(next)
    }

    fun showHealingEffect(outcome: HealingOutcome) {
        stopDayStageAudio()
        val current = _uiState.value.session
        val violetStones = if (outcome == HealingOutcome.WEREWOLF) {
            VioletStoneRules.afterWerewolfHealing(
                current.violetStonesInWolfBox,
                current.playerCount,
            )
        } else {
            current.violetStonesInWolfBox
        }
        val session = current.copy(
            dayStage = DayStage.HEALING_EFFECT,
            healingOutcome = outcome,
            violetStonesInWolfBox = violetStones,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            statusMessage = null,
        )
    }

    fun continueAfterHealing() {
        stopAllPlayback()
        val session = _uiState.value.session.copy(
            screen = GameScreen.DRAW,
            nextNightColor = null,
            cueIndex = 0,
            cueRemainingMillis = 0L,
            healingOutcome = null,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            statusMessage = null,
            standaloneCueId = null,
        )
        startPhaseAmbience()
    }

    fun drawAutomaticNight() {
        val current = _uiState.value.session
        if (current.remainingNightDeck.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Les huit cartes Nuit ont été utilisées. Remélangez le paquet.",
            )
            return
        }
        val (color, remaining) = NightDeck.draw(current.remainingNightDeck)
        val resolved = color ?: return
        val session = current.copy(
            nextNightColor = resolved,
            remainingNightDeck = remaining,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(session = session, statusMessage = null)
        playStandalone(colorAnnouncement(resolved))
    }

    fun choosePhysicalNight(color: NightColor) {
        val current = _uiState.value.session
        val session = current.copy(
            nextNightColor = color,
            remainingNightDeck = NightDeck.removePhysicalDraw(
                current.remainingNightDeck,
                color,
            ),
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(session = session, statusMessage = null)
        playStandalone(colorAnnouncement(color))
    }

    fun reshuffleNightDeck() {
        val session = _uiState.value.session.copy(
            remainingNightDeck = NightDeck.shuffled(),
            nextNightColor = null,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            statusMessage = "Le paquet Nuit a été remélangé.",
        )
    }

    fun startNextNight() {
        val current = _uiState.value.session
        val color = current.nextNightColor ?: return
        stopAllPlayback()
        val session = current.copy(
            screen = GameScreen.NIGHT_READY,
            round = current.round + 1,
            currentNightColor = color,
            nextNightColor = null,
            cueIndex = 0,
            cueRemainingMillis = 0L,
            dayStage = DayStage.DISCUSSION,
            healingOutcome = null,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            isSequenceComplete = false,
            statusMessage = null,
            standaloneCueId = null,
        )
    }

    fun launchNextNight() {
        val current = _uiState.value.session
        if (current.screen != GameScreen.NIGHT_READY || current.currentNightColor == null) return
        val session = current.copy(
            screen = GameScreen.NIGHT,
            cueIndex = 0,
            cueRemainingMillis = 0L,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            isSequenceComplete = false,
            statusMessage = null,
        )
        loadSequenceFor(session, autoplay = true)
    }

    fun finishGame(reason: EndReason) {
        val playBloodWolfVictory = reason == EndReason.BLOOD_WOLF_FOUND &&
            _uiState.value.session.dayAmbienceEnabled
        stopAllPlayback()
        val session = _uiState.value.session.copy(
            screen = GameScreen.END,
            endReason = reason,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            hasSavedGame = false,
            statusMessage = null,
        )
        if (playBloodWolfVictory) {
            audioEngine.playForeground(BLOOD_WOLF_VICTORY_RESOURCE)
        }
    }

    fun openHelp() {
        stopAllPlayback()
        _uiState.value = _uiState.value.copy(
            session = _uiState.value.session.copy(screen = GameScreen.HELP),
            statusMessage = null,
        )
    }

    fun returnToHome() {
        stopAllPlayback()
        _uiState.value = _uiState.value.copy(
            session = _uiState.value.session.copy(screen = GameScreen.HOME),
            standaloneCueId = null,
            statusMessage = null,
            hasSavedGame = repository.hasResumableSession(),
        )
    }

    fun closeHelp() = returnToHome()

    fun playHelp(cue: GameCue) {
        playStandalone(cue)
    }

    fun stopStandalone() {
        standaloneJob?.cancel()
        standaloneJob = null
        audioEngine.stopForeground()
        _uiState.value = _uiState.value.copy(standaloneCueId = null)
    }

    fun testSpeaker() {
        audioEngine.testSpeaker()
        _uiState.value = _uiState.value.copy(
            statusMessage = "Son test envoyé vers la sortie audio active.",
        )
    }

    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun dismissAvailableUpdate() {
        _uiState.value = _uiState.value.copy(availableUpdate = null)
    }

    fun startAvailableUpdateDownload() {
        val update = _uiState.value.availableUpdate ?: return
        runCatching {
            updateDownloadManager.enqueue(update)
        }.onSuccess { pending ->
            _uiState.value = _uiState.value.copy(
                availableUpdate = null,
                statusMessage = null,
            )
            monitorUpdateDownload(pending)
        }.onFailure {
            _uiState.value = _uiState.value.copy(
                statusMessage = "Impossible de démarrer le téléchargement de la mise à jour.",
            )
        }
    }

    fun installDownloadedUpdate() {
        val pending = updateDownloadManager.pendingDownload()
        if (pending == null) {
            _uiState.value = _uiState.value.copy(
                updateDownload = null,
                showUpdateInstallPrompt = false,
                statusMessage = "Le fichier de mise à jour est introuvable.",
            )
            return
        }
        when (updateDownloadManager.install(pending)) {
            AppUpdateInstallResult.Started -> {
                waitingForInstallPermission = false
                _uiState.value = _uiState.value.copy(showUpdateInstallPrompt = false)
            }
            AppUpdateInstallResult.PermissionRequired -> {
                waitingForInstallPermission = true
                if (!updateDownloadManager.openInstallPermissionSettings()) {
                    waitingForInstallPermission = false
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Impossible d’ouvrir l’autorisation d’installation.",
                    )
                }
            }
            AppUpdateInstallResult.FileMissing -> {
                updateDownloadManager.remove(pending)
                _uiState.value = _uiState.value.copy(
                    updateDownload = null,
                    showUpdateInstallPrompt = false,
                    statusMessage = "Le fichier de mise à jour est introuvable.",
                )
            }
            AppUpdateInstallResult.NoInstaller -> {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Aucun installateur Android n’est disponible.",
                )
            }
        }
    }

    fun dismissUpdateInstallPrompt() {
        _uiState.value = _uiState.value.copy(showUpdateInstallPrompt = false)
    }

    fun resumePendingUpdateInstallation() {
        if (!waitingForInstallPermission) return
        waitingForInstallPermission = false
        installDownloadedUpdate()
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    fun isAudioAvailable(cue: GameCue): Boolean =
        audioEngine.hasResource(cue.audioResource)

    private fun jumpTimedNight(remainingMillis: Long) {
        stopSequenceClock()
        audioEngine.stopForeground()
        val session = _uiState.value.session.copy(
            cueRemainingMillis = remainingMillis,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            cueRemainingMillis = remainingMillis,
            isSequencePlaying = false,
        )
        playCurrentCue()
    }

    private fun playbackResourceFor(cue: GameCue, remainingMillis: Long): String =
        when (cue.id) {
            ConfirmedFirstNightTimeline.CUE_ID ->
                ConfirmedFirstNightTimeline.playback(remainingMillis).audioResource
            ConfirmedLaterNightTimeline.CUE_ID ->
                ConfirmedLaterNightTimeline.playback(remainingMillis).audioResource
            else -> cue.audioResource
        }

    private fun loadSequenceFor(session: GameSession, autoplay: Boolean) {
        activeSequence = when (session.screen) {
            GameScreen.INTRO -> GameSequenceFactory.intro(session.mode)
            GameScreen.NIGHT -> GameSequenceFactory.night(
                round = session.round,
                color = session.currentNightColor,
                mode = session.mode,
                packCallVillagerLimit = session.packCallVillagerLimit,
            )
            else -> emptyList()
        }
        if (activeSequence.isEmpty()) return
        if (session.cueIndex >= activeSequence.size) {
            _uiState.value = _uiState.value.copy(
                session = session.copy(cueRemainingMillis = 0L),
                currentCue = activeSequence.lastOrNull(),
                cueTotalMillis = 0L,
                cueRemainingMillis = 0L,
                isSequencePlaying = false,
                isSequenceComplete = true,
                currentCueHasAudio = false,
            )
            return
        }
        val safeIndex = session.cueIndex.coerceIn(0, activeSequence.lastIndex)
        val cue = activeSequence[safeIndex]
        val remaining = session.cueRemainingMillis
            .takeIf { it > 0L }
            ?.coerceAtMost(cue.fallbackDurationMillis)
            ?: cue.fallbackDurationMillis
        val normalized = session.copy(
            cueIndex = safeIndex,
            cueRemainingMillis = remaining,
        )
        _uiState.value = _uiState.value.copy(
            session = normalized,
            currentCue = cue,
            cueTotalMillis = cue.fallbackDurationMillis,
            cueRemainingMillis = remaining,
            isSequencePlaying = false,
            isSequenceComplete = false,
            currentCueHasAudio = audioEngine.hasResource(
                playbackResourceFor(cue, remaining),
            ),
        )
        if (autoplay) playCurrentCue()
    }

    private fun playCurrentCue(reset: Boolean = false) {
        val state = _uiState.value
        val cue = state.currentCue ?: return
        sequenceJob?.cancel()
        val requestedRemaining = if (reset) {
            cue.fallbackDurationMillis
        } else {
            state.cueRemainingMillis.takeIf { it > 0L } ?: cue.fallbackDurationMillis
        }
        val fallbackTotal = cue.fallbackDurationMillis
        val fallbackElapsed = (fallbackTotal - requestedRemaining).coerceAtLeast(0L)
        val timedNightPlayback = when (cue.id) {
            ConfirmedFirstNightTimeline.CUE_ID ->
                ConfirmedFirstNightTimeline.playback(requestedRemaining)
            ConfirmedLaterNightTimeline.CUE_ID ->
                ConfirmedLaterNightTimeline.playback(requestedRemaining)
            else -> null
        }
        val ambiencePlayback = if (timedNightPlayback != null) {
            audioEngine.stopAmbience()
            null
        } else {
            startPhaseAmbience()
        }
        val playback = if (
            cue.kind == CueKind.TIMER &&
            cue.audioResource == NIGHT_AMBIENCE_RESOURCE
        ) {
            ambiencePlayback ?: PlaybackInfo(available = false, durationMillis = 0L)
        } else {
            audioEngine.playForeground(
                resourceName = timedNightPlayback?.audioResource ?: cue.audioResource,
                looping = cue.loopAudio,
                seekMillis = timedNightPlayback?.seekMillis ?: fallbackElapsed,
            )
        }
        val total = when {
            cue.kind == CueKind.TIMER -> fallbackTotal
            playback.available && playback.durationMillis > 0L -> playback.durationMillis
            else -> fallbackTotal.coerceAtMost(5_000L)
        }
        val remaining = if (reset || state.cueRemainingMillis <= 0L) {
            total
        } else {
            requestedRemaining.coerceAtMost(total)
        }
        updateSessionPlayback(state.session.cueIndex, remaining)
        _uiState.value = _uiState.value.copy(
            cueTotalMillis = total,
            cueRemainingMillis = remaining,
            isSequencePlaying = true,
            currentCueHasAudio = playback.available,
            statusMessage = if (playback.available) {
                null
            } else {
                "Audio « ${cue.audioResource}.mp3 » à fournir : minuterie silencieuse active."
            },
        )
        startSequenceClock()
    }

    private fun startSequenceClock() {
        sequenceJob?.cancel()
        lastPersistedSecond = -1L
        sequenceJob = viewModelScope.launch {
            var previousTick = SystemClock.elapsedRealtime()
            while (_uiState.value.isSequencePlaying) {
                delay(100L)
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - previousTick
                previousTick = now
                val nextRemaining = (_uiState.value.cueRemainingMillis - elapsed)
                    .coerceAtLeast(0L)
                val session = _uiState.value.session.copy(
                    cueRemainingMillis = nextRemaining,
                )
                _uiState.value = _uiState.value.copy(
                    session = session,
                    cueRemainingMillis = nextRemaining,
                )
                val second = nextRemaining / 1_000L
                if (second != lastPersistedSecond) {
                    lastPersistedSecond = second
                    repository.save(session)
                }
                if (nextRemaining == 0L) {
                    advanceCue()
                    break
                }
            }
        }
    }

    private fun advanceCue() {
        stopSequenceClock()
        audioEngine.stopForeground()
        val nextIndex = _uiState.value.session.cueIndex + 1
        if (nextIndex > activeSequence.lastIndex) {
            completeSequence()
            return
        }
        val cue = activeSequence[nextIndex]
        val session = _uiState.value.session.copy(
            cueIndex = nextIndex,
            cueRemainingMillis = cue.fallbackDurationMillis,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            currentCue = cue,
            cueTotalMillis = cue.fallbackDurationMillis,
            cueRemainingMillis = cue.fallbackDurationMillis,
            isSequencePlaying = false,
            currentCueHasAudio = audioEngine.hasResource(cue.audioResource),
        )
        playCurrentCue(reset = true)
    }

    private fun completeSequence() {
        stopSequenceClock()
        audioEngine.stopForeground()
        when (_uiState.value.session.screen) {
            GameScreen.INTRO -> {
                val session = _uiState.value.session.copy(
                    cueIndex = activeSequence.size,
                    cueRemainingMillis = 0L,
                )
                repository.save(session)
                _uiState.value = _uiState.value.copy(
                    session = session,
                    isSequencePlaying = false,
                    isSequenceComplete = true,
                    cueRemainingMillis = 0L,
                    statusMessage = null,
                )
            }
            GameScreen.NIGHT -> enterDay()
            else -> Unit
        }
    }

    private fun enterDay() {
        val current = _uiState.value.session
        val duration = current.dayDurationMinutes * 60_000L
        val session = current.copy(
            screen = GameScreen.DAY,
            violetStonesInWolfBox = VioletStoneRules.afterNight(
                current.violetStonesInWolfBox,
            ),
            dayStage = DayStage.DISCUSSION,
            healingOutcome = null,
            dayRemainingMillis = duration,
            cueIndex = 0,
            cueRemainingMillis = 0L,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            currentCue = null,
            isSequencePlaying = false,
            isSequenceComplete = false,
            dayTimerPlaying = false,
            statusMessage = null,
        )
        startDayStageAudio(DayStage.DISCUSSION)
    }

    private fun startDayTimer() {
        dayTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            dayTimerPlaying = true,
            dayTimerDisplayResetKey = _uiState.value.dayTimerDisplayResetKey + 1,
        )
        dayTimerJob = viewModelScope.launch {
            var previousTick = SystemClock.elapsedRealtime()
            while (_uiState.value.dayTimerPlaying) {
                delay(250L)
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - previousTick
                previousTick = now
                val remaining = (_uiState.value.session.dayRemainingMillis - elapsed)
                    .coerceAtLeast(0L)
                val session = _uiState.value.session.copy(dayRemainingMillis = remaining)
                _uiState.value = _uiState.value.copy(session = session)
                if (remaining % 1_000L < 250L) repository.save(session)
                if (remaining == 0L) {
                    dayTimerJob = null
                    moveToDayStage(DayStage.COUNCIL)
                    break
                }
            }
        }
    }

    private fun pauseSequence(message: String? = null) {
        if (!_uiState.value.isSequencePlaying) {
            if (message != null) {
                _uiState.value = _uiState.value.copy(statusMessage = message)
            }
            return
        }
        stopSequenceClock()
        audioEngine.stopForeground()
        audioEngine.stopAmbience()
        val session = _uiState.value.session.copy(
            cueRemainingMillis = _uiState.value.cueRemainingMillis,
        )
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            isSequencePlaying = false,
            statusMessage = message,
        )
    }

    private fun playStandalone(cue: GameCue) {
        stopSequenceClock()
        standaloneJob?.cancel()
        standaloneJob = null
        audioEngine.stopForeground()
        val playback = audioEngine.playForeground(cue.audioResource)
        _uiState.value = _uiState.value.copy(
            standaloneCueId = if (playback.available) cue.id else null,
            statusMessage = if (playback.available) {
                null
            } else {
                "Fichier « ${cue.audioResource}.mp3 » à fournir. Le texte reste affiché."
            },
        )
        if (playback.available) {
            standaloneJob = viewModelScope.launch {
                delay(playback.durationMillis.coerceAtLeast(250L))
                if (_uiState.value.standaloneCueId == cue.id) {
                    audioEngine.stopForeground()
                    _uiState.value = _uiState.value.copy(standaloneCueId = null)
                }
                standaloneJob = null
            }
        }
    }

    private fun moveToDayStage(next: DayStage) {
        dayTimerJob?.cancel()
        dayTimerJob = null
        stopDayStageAudio()
        val session = _uiState.value.session.copy(dayStage = next)
        repository.save(session)
        _uiState.value = _uiState.value.copy(
            session = session,
            dayTimerPlaying = false,
            statusMessage = null,
        )
        startDayStageAudio(next)
    }

    private fun startDayStageAudio(stage: DayStage) {
        when (stage) {
            DayStage.DISCUSSION -> {
                if (_uiState.value.session.dayDurationMinutes == 0) {
                    startDiscussionAudio(restart = true)
                } else {
                    stopDayStageAudio()
                }
            }
            DayStage.COUNCIL -> startCouncilAudio()
            DayStage.HEALING -> startLoopingDayAudio(HEALING_LOOP_RESOURCE)
            DayStage.HEALING_EFFECT -> stopDayStageAudio()
        }
    }

    private fun startDiscussionAudio(restart: Boolean) {
        dayStageAudioJob?.cancel()
        dayStageAudioJob = null
        audioEngine.stopForeground()
        if (!_uiState.value.session.dayAmbienceEnabled) {
            audioEngine.stopAmbience()
            return
        }
        if (restart) audioEngine.stopAmbience()
        audioEngine.playAmbience(DAY_DISCUSSION_RESOURCE)
    }

    private fun startCouncilAudio() {
        stopDayStageAudio()
        if (!_uiState.value.session.dayAmbienceEnabled) return
        val gong = audioEngine.playForeground(COUNCIL_GONG_RESOURCE)
        if (!gong.available || gong.durationMillis <= 0L) {
            audioEngine.playAmbience(COUNCIL_LOOP_RESOURCE)
            return
        }
        dayStageAudioJob = viewModelScope.launch {
            delay(gong.durationMillis)
            val session = _uiState.value.session
            if (
                session.screen == GameScreen.DAY &&
                session.dayStage == DayStage.COUNCIL &&
                session.dayAmbienceEnabled
            ) {
                audioEngine.stopForeground()
                audioEngine.playAmbience(COUNCIL_LOOP_RESOURCE)
            }
            dayStageAudioJob = null
        }
    }

    private fun startLoopingDayAudio(resourceName: String) {
        stopDayStageAudio()
        if (_uiState.value.session.dayAmbienceEnabled) {
            audioEngine.playAmbience(resourceName)
        }
    }

    private fun stopDayStageAudio() {
        dayStageAudioJob?.cancel()
        dayStageAudioJob = null
        audioEngine.stopForeground()
        audioEngine.stopAmbience()
    }

    private fun colorAnnouncement(color: NightColor): GameCue =
        if (color == NightColor.YELLOW) {
            GameCue(
                id = "draw_yellow",
                title = "Nuit jaune",
                text = "Cette nuit, les goules jaunes vont se réveiller.",
                audioResource = "jaune_302_annonce_prochaine_nuit_jaune",
                fallbackDurationMillis = 7_000L,
            )
        } else {
            GameCue(
                id = "draw_green",
                title = "Nuit verte",
                text = "Cette nuit, les goules vertes vont se réveiller.",
                audioResource = "vert_302_annonce_prochaine_nuit_verte",
                fallbackDurationMillis = 7_000L,
            )
        }

    private fun startPhaseAmbience(): PlaybackInfo? {
        val resourceName = when (_uiState.value.session.screen) {
            GameScreen.NIGHT -> NIGHT_AMBIENCE_RESOURCE
            GameScreen.DRAW -> DAY_AMBIENCE_RESOURCE.takeIf {
                _uiState.value.session.dayAmbienceEnabled
            }
            else -> null
        }
        if (resourceName == null) {
            audioEngine.stopAmbience()
            return null
        }
        return audioEngine.playAmbience(resourceName)
    }

    private fun handleAudioInterruption(message: String) {
        if (_uiState.value.isSequencePlaying) {
            pauseSequence(message)
            return
        }

        val timerWasPlaying = _uiState.value.dayTimerPlaying
        dayTimerJob?.cancel()
        dayTimerJob = null
        dayStageAudioJob?.cancel()
        dayStageAudioJob = null
        standaloneJob?.cancel()
        standaloneJob = null
        audioEngine.stopAll()
        if (timerWasPlaying) {
            repository.save(_uiState.value.session)
        }
        _uiState.value = _uiState.value.copy(
            dayTimerPlaying = false,
            standaloneCueId = null,
            statusMessage = message,
        )
    }

    private fun updateSessionPlayback(cueIndex: Int, remainingMillis: Long) {
        val session = _uiState.value.session.copy(
            cueIndex = cueIndex,
            cueRemainingMillis = remainingMillis,
        )
        _uiState.value = _uiState.value.copy(session = session)
        repository.save(session)
    }

    private fun monitorUpdateDownload(pending: PendingAppUpdateDownload) {
        updateDownloadJob?.cancel()
        _uiState.value = _uiState.value.copy(
            updateDownload = UpdateDownloadUiState(version = pending.version),
            showUpdateInstallPrompt = false,
        )
        updateDownloadJob = viewModelScope.launch {
            while (true) {
                val snapshot = withContext(Dispatchers.IO) {
                    updateDownloadManager.snapshot(pending)
                }
                when (snapshot.status) {
                    AppUpdateDownloadStatus.RUNNING -> {
                        _uiState.value = _uiState.value.copy(
                            updateDownload = UpdateDownloadUiState(
                                version = pending.version,
                                downloadedBytes = snapshot.downloadedBytes,
                                totalBytes = snapshot.totalBytes,
                            ),
                        )
                        delay(500L)
                    }
                    AppUpdateDownloadStatus.SUCCESSFUL -> {
                        _uiState.value = _uiState.value.copy(
                            updateDownload = UpdateDownloadUiState(
                                version = pending.version,
                                downloadedBytes = snapshot.downloadedBytes,
                                totalBytes = snapshot.totalBytes,
                                readyToInstall = true,
                            ),
                            showUpdateInstallPrompt = true,
                            statusMessage = null,
                        )
                        break
                    }
                    AppUpdateDownloadStatus.FAILED,
                    AppUpdateDownloadStatus.MISSING,
                    -> {
                        updateDownloadManager.remove(pending)
                        _uiState.value = _uiState.value.copy(
                            updateDownload = null,
                            showUpdateInstallPrompt = false,
                            statusMessage = "Le téléchargement de la mise à jour a échoué.",
                        )
                        break
                    }
                }
            }
            updateDownloadJob = null
        }
    }

    private fun stopSequenceClock() {
        sequenceJob?.cancel()
        sequenceJob = null
    }

    private fun stopAllPlayback() {
        stopSequenceClock()
        dayTimerJob?.cancel()
        dayTimerJob = null
        dayStageAudioJob?.cancel()
        dayStageAudioJob = null
        standaloneJob?.cancel()
        standaloneJob = null
        audioEngine.stopAll()
        _uiState.value = _uiState.value.copy(
            isSequencePlaying = false,
            dayTimerPlaying = false,
            standaloneCueId = null,
        )
    }

    override fun onCleared() {
        _uiState.value.session.takeIf { it.isResumable }?.let(repository::save)
        routeMonitor.stop()
        audioEngine.release()
        super.onCleared()
    }

    private companion object {
        const val NIGHT_AMBIENCE_RESOURCE = "commun_012_ambiance_nuit_boucle"
        const val DAY_AMBIENCE_RESOURCE = "commun_013_ambiance_jour_boucle"
        const val DAY_DISCUSSION_RESOURCE = "jour_8min"
        const val COUNCIL_GONG_RESOURCE = "gong"
        const val COUNCIL_LOOP_RESOURCE = "conseil_villageois_votes_boucle"
        const val HEALING_LOOP_RESOURCE = "guerison_boucle"
        const val BLOOD_WOLF_VICTORY_RESOURCE = "fin_partie_a"
    }
}
