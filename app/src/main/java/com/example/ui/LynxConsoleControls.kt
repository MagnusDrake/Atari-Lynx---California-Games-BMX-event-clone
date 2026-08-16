package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.BmxGameViewModel

@Composable
fun LynxDPad(
    viewModel: BmxGameViewModel,
    modifier: Modifier = Modifier
) {
    var activeDir by remember { mutableStateOf<String?>(null) }
    var dpadSize by remember { mutableStateOf(IntSize.Zero) }

    fun processPosition(x: Float, y: Float) {
        val cx = if (dpadSize.width > 0) dpadSize.width / 2f else 150f
        val cy = if (dpadSize.height > 0) dpadSize.height / 2f else 150f
        val dx = x - cx
        val dy = y - cy
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        if (dist > 15f) {
            val angle = kotlin.math.atan2(dy, dx) * 180f / Math.PI.toFloat()
            when {
                angle in -45f..45f -> {
                    activeDir = "RIGHT"
                    viewModel.isRightPressed = true
                    viewModel.isLeftPressed = false
                    viewModel.isUpPressed = false
                    viewModel.isDownPressed = false
                }
                angle in 45f..135f -> {
                    activeDir = "DOWN"
                    viewModel.isDownPressed = true
                    viewModel.isUpPressed = false
                    viewModel.isLeftPressed = false
                    viewModel.isRightPressed = false
                }
                angle in -135f..-45f -> {
                    activeDir = "UP"
                    viewModel.isUpPressed = true
                    viewModel.isDownPressed = false
                    viewModel.isLeftPressed = false
                    viewModel.isRightPressed = false
                }
                else -> {
                    activeDir = "LEFT"
                    viewModel.isLeftPressed = true
                    viewModel.isRightPressed = false
                    viewModel.isUpPressed = false
                    viewModel.isDownPressed = false
                }
            }
        }
    }

    fun releaseDpad() {
        activeDir = null
        viewModel.isLeftPressed = false
        viewModel.isRightPressed = false
        viewModel.isUpPressed = false
        viewModel.isDownPressed = false
    }

    Box(
        modifier = modifier
            .size(130.dp)
            .shadow(6.dp, CircleShape)
            .background(ElegantSurface, CircleShape)
            .border(2.dp, ElegantBorder, CircleShape)
            .onSizeChanged { dpadSize = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Press, PointerEventType.Move -> {
                                val pos = event.changes.firstOrNull()?.position
                                if (pos != null) {
                                    processPosition(pos.x, pos.y)
                                }
                            }
                            PointerEventType.Release, PointerEventType.Unknown -> {
                                releaseDpad()
                            }
                        }
                    }
                }
            }
            .testTag("dpad_controller"),
        contentAlignment = Alignment.Center
    ) {
        // D-Pad Cross Drawing
        Canvas(modifier = Modifier.size(110.dp)) {
            val armW = 34.dp.toPx()
            val fullL = 110.dp.toPx()
            val centerOffset = (fullL - armW) / 2f

            // Horizontal arm
            drawRoundRect(
                color = if (activeDir == "LEFT" || activeDir == "RIGHT") ElegantSurfaceActive else ElegantSurfaceVariant,
                topLeft = Offset(0f, centerOffset),
                size = Size(fullL, armW),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
            )
            // Vertical arm
            drawRoundRect(
                color = if (activeDir == "UP" || activeDir == "DOWN") ElegantSurfaceActive else ElegantSurfaceVariant,
                topLeft = Offset(centerOffset, 0f),
                size = Size(armW, fullL),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
            )

            // Center circle
            drawCircle(
                color = ElegantSurface,
                radius = 16.dp.toPx(),
                center = Offset(fullL / 2f, fullL / 2f)
            )

            // Center indicator
            drawCircle(
                color = ElegantPrimaryLavender.copy(alpha = 0.35f),
                radius = 4.dp.toPx(),
                center = Offset(fullL / 2f, fullL / 2f)
            )

            // Direction arrow markings
            val arrowColor = ElegantTextPrimary
            val activeColor = ElegantPrimaryLavender
            // Up
            drawCircle(if (activeDir == "UP") activeColor else arrowColor, radius = 3.dp.toPx(), center = Offset(fullL / 2f, 12.dp.toPx()))
            // Down
            drawCircle(if (activeDir == "DOWN") activeColor else arrowColor, radius = 3.dp.toPx(), center = Offset(fullL / 2f, fullL - 12.dp.toPx()))
            // Left (Backflip)
            drawCircle(if (activeDir == "LEFT") activeColor else arrowColor, radius = 3.dp.toPx(), center = Offset(12.dp.toPx(), fullL / 2f))
            // Right (Frontflip)
            drawCircle(if (activeDir == "RIGHT") activeColor else arrowColor, radius = 3.dp.toPx(), center = Offset(fullL - 12.dp.toPx(), fullL / 2f))
        }

        // Action Hints text overlay
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("TABLE", fontSize = 8.sp, color = ElegantTextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("BACK\nFLIP", fontSize = 7.sp, color = if (activeDir == "LEFT") ElegantPrimaryLavender else ElegantTextSecondary, fontWeight = FontWeight.Bold, lineHeight = 8.sp)
                Text("FRONT\nFLIP", fontSize = 7.sp, color = if (activeDir == "RIGHT") ElegantPrimaryLavender else ElegantTextSecondary, fontWeight = FontWeight.Bold, lineHeight = 8.sp)
            }
            Text("SUPER", fontSize = 8.sp, color = ElegantTextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
fun LynxActionButton(
    label: String,
    subLabel: String,
    buttonColor: Color,
    isPressedState: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    var isDown by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(if (isDown) 2.dp else 8.dp, CircleShape)
                .clip(CircleShape)
                .background(if (isDown) buttonColor.copy(alpha = 0.8f) else buttonColor)
                .border(2.dp, if (isDown) ElegantPrimaryLavender else Color.White.copy(alpha = 0.3f), CircleShape)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Press -> {
                                    isDown = true
                                    isPressedState(true)
                                }
                                PointerEventType.Release, PointerEventType.Unknown -> {
                                    isDown = false
                                    isPressedState(false)
                                }
                            }
                        }
                    }
                }
                .testTag(testTag),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = ElegantOnPrimaryDark
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subLabel,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = ElegantPrimaryLavender.copy(alpha = 0.8f),
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun LynxOptionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = ElegantSurfaceVariant,
            contentColor = ElegantTextPrimary
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElegantBorder),
        modifier = modifier
            .height(26.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = ElegantTextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}

