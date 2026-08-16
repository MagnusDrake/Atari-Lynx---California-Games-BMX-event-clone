package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.LynxAudioEngine
import com.example.data.db.AppDatabase
import com.example.data.db.HighScoreEntity
import com.example.data.db.HighScoreRepository
import com.example.engine.BmxPhysicsEngine
import com.example.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class BmxUiState(
    val phase: GamePhase = GamePhase.TITLE,
    val score: Int = 0,
    val timeRemaining: Float = 90f,
    val speedMph: Int = 0,
    val wipeouts: Int = 0,
    val tricksCount: Int = 0,
    val bestTrick: String = "None",
    val distancePct: Int = 0,
    val bannerText: String = "",
    val isLynxBezelEnabled: Boolean = true,
    val isScanlinesEnabled: Boolean = true,
    val isMusicEnabled: Boolean = true,
    val isSfxEnabled: Boolean = true,
    val selectedThemeIndex: Int = 0,
    val ratingTitle: String = "",
    val playerName: String = "RIDER 1"
)

class BmxGameViewModel(application: Application) : AndroidViewModel(application) {

    val audio = LynxAudioEngine()
    val physics = BmxPhysicsEngine(audio)
    private val repository: HighScoreRepository

    private val _uiState = MutableStateFlow(BmxUiState())
    val uiState: StateFlow<BmxUiState> = _uiState.asStateFlow()

    // Top high scores stream from Room
    val topScores: StateFlow<List<HighScoreEntity>>

    // Control inputs
    var isPedalPressed = false
    var isJumpPressed = false
    var isLeftPressed = false
    var isRightPressed = false
    var isUpPressed = false
    var isDownPressed = false

    private var gameLoopJob: Job? = null

    init {
        val db = AppDatabase.getInstance(application)
        repository = HighScoreRepository(db.highScoreDao())
        topScores = repository.topScores.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        audio.start()
        startGameLoop()
    }

    override fun onCleared() {
        super.onCleared()
        audio.stop()
        gameLoopJob?.cancel()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
                lastTime = now

                physics.update(
                    dt = dt,
                    isPedalPressed = isPedalPressed,
                    isJumpPressed = isJumpPressed,
                    isLeanBack = isLeftPressed,
                    isLeanForward = isRightPressed,
                    isTrickUp = isUpPressed,
                    isTrickDown = isDownPressed
                )

                // Update UI state
                val phase = physics.gamePhase
                val speedMph = (physics.velX * 2.237f).toInt().coerceAtLeast(0)
                val distPct = ((physics.posX / physics.trackLength) * 100f).toInt().coerceIn(0, 100)

                _uiState.value = _uiState.value.copy(
                    phase = phase,
                    score = physics.score,
                    timeRemaining = physics.timeRemaining,
                    speedMph = speedMph,
                    wipeouts = physics.wipeoutCount,
                    tricksCount = physics.tricksCount,
                    bestTrick = physics.bestTrick,
                    distancePct = distPct,
                    bannerText = physics.currentTrickBanner,
                    ratingTitle = calculateRating(physics.score)
                )

                delay(16) // ~60 FPS
            }
        }
    }

    private fun calculateRating(score: Int): String {
        return when {
            score >= 6000 -> "RADICAL CHAMPION!"
            score >= 4000 -> "TOTALLY TUBULAR!"
            score >= 2500 -> "GNARLY RIDER!"
            score >= 1200 -> "DUDE, WICKED!"
            else -> "BOGUS RUN!"
        }
    }

    fun startNewGame() {
        physics.resetToStart()
        physics.gamePhase = GamePhase.PLAYING
        _uiState.value = _uiState.value.copy(phase = GamePhase.PLAYING)
        audio.playCountdownBeep(true)
    }

    fun pauseGame() {
        if (physics.gamePhase == GamePhase.PLAYING) {
            physics.gamePhase = GamePhase.TITLE
            _uiState.value = _uiState.value.copy(phase = GamePhase.TITLE)
        }
    }

    fun resumeGame() {
        if (physics.gamePhase == GamePhase.TITLE) {
            physics.gamePhase = GamePhase.PLAYING
            _uiState.value = _uiState.value.copy(phase = GamePhase.PLAYING)
        }
    }

    fun showLeaderboard() {
        _uiState.value = _uiState.value.copy(phase = GamePhase.LEADERBOARD)
    }

    fun showTrickGuide() {
        _uiState.value = _uiState.value.copy(phase = GamePhase.TRICK_GUIDE)
    }

    fun showTitleScreen() {
        physics.resetToStart()
        physics.gamePhase = GamePhase.TITLE
        _uiState.value = _uiState.value.copy(phase = GamePhase.TITLE)
    }

    fun saveCurrentScore(name: String) {
        val finalName = if (name.isBlank()) "LYNX RIDER" else name.take(12).uppercase()
        viewModelScope.launch {
            repository.saveScore(
                HighScoreEntity(
                    playerName = finalName,
                    score = physics.score,
                    bestTrick = physics.bestTrick,
                    timeRemainingSeconds = physics.timeRemaining.toInt(),
                    tricksCount = physics.tricksCount,
                    wipeouts = physics.wipeoutCount
                )
            )
            showLeaderboard()
        }
    }

    fun toggleLynxBezel() {
        val next = !_uiState.value.isLynxBezelEnabled
        _uiState.value = _uiState.value.copy(isLynxBezelEnabled = next)
        audio.playButtonTap()
    }

    fun toggleScanlines() {
        val next = !_uiState.value.isScanlinesEnabled
        _uiState.value = _uiState.value.copy(isScanlinesEnabled = next)
        audio.playButtonTap()
    }

    fun toggleMusic() {
        val next = !_uiState.value.isMusicEnabled
        audio.musicEnabled = next
        _uiState.value = _uiState.value.copy(isMusicEnabled = next)
        audio.playButtonTap()
    }

    fun toggleSfx() {
        val next = !_uiState.value.isSfxEnabled
        audio.sfxEnabled = next
        _uiState.value = _uiState.value.copy(isSfxEnabled = next)
        audio.playButtonTap()
    }

    fun cycleTheme() {
        val next = (_uiState.value.selectedThemeIndex + 1) % RETRO_PALETTES.size
        _uiState.value = _uiState.value.copy(selectedThemeIndex = next)
        audio.playButtonTap()
    }
}
