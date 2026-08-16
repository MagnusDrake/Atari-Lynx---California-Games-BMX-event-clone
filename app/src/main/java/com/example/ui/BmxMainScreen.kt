package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.GamePhase
import com.example.model.RETRO_PALETTES
import com.example.viewmodel.BmxGameViewModel

@Composable
fun BmxMainScreen(
    viewModel: BmxGameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val highScores by viewModel.topScores.collectAsState()
    val currentTheme = RETRO_PALETTES[uiState.selectedThemeIndex]

    LynxConsoleContainer(
        uiState = uiState,
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main Game Pixel Canvas
            BmxGameCanvas(
                physics = viewModel.physics,
                theme = currentTheme,
                showScanlines = uiState.isScanlinesEnabled,
                onCanvasClick = {
                    if (uiState.phase == GamePhase.TITLE) {
                        viewModel.startNewGame()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlays based on game state
            when (uiState.phase) {
                GamePhase.TITLE -> {
                    TitleMenuOverlay(
                        uiState = uiState,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                GamePhase.FINISHED -> {
                    ResultsSummaryDialog(
                        uiState = uiState,
                        viewModel = viewModel,
                        onDismiss = { viewModel.showTitleScreen() }
                    )
                }
                GamePhase.LEADERBOARD -> {
                    LeaderboardDialog(
                        scores = highScores,
                        onDismiss = { viewModel.showTitleScreen() }
                    )
                }
                GamePhase.TRICK_GUIDE -> {
                    TrickGuideDialog(
                        onDismiss = { viewModel.showTitleScreen() }
                    )
                }
                else -> {
                    // In-game active
                }
            }
        }
    }
}
