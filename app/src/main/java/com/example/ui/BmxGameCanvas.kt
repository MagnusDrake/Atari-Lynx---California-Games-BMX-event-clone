package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.engine.BmxPhysicsEngine
import com.example.graphics.LynxGraphicsRenderer
import com.example.model.ColorTheme
import com.example.model.GamePhase
import kotlinx.coroutines.isActive

@Composable
fun BmxGameCanvas(
    physics: BmxPhysicsEngine,
    theme: ColorTheme,
    showScanlines: Boolean,
    onCanvasClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val renderer = remember { LynxGraphicsRenderer() }
    val textMeasurer = rememberTextMeasurer()

    // 60FPS continuous display-synchronized render clock
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { time ->
                frameNanos = time
            }
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onCanvasClick()
            }
            .testTag("bmx_game_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Reading frameNanos triggers DrawScope invalidation on every frame
            @Suppress("UNUSED_VARIABLE")
            val tick = frameNanos

            renderer.render(
                drawScope = this,
                physics = physics,
                theme = theme,
                showScanlines = showScanlines,
                textMeasurer = textMeasurer
            )
        }
    }
}

