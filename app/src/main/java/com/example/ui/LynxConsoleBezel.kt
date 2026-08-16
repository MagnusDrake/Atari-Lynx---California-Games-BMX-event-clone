package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GamePhase
import com.example.ui.theme.*
import com.example.viewmodel.BmxGameViewModel
import com.example.viewmodel.BmxUiState

@Composable
fun LynxConsoleContainer(
    uiState: BmxUiState,
    viewModel: BmxGameViewModel,
    modifier: Modifier = Modifier,
    screenContent: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkCanvas)
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape && uiState.isLynxBezelEnabled) {
            // Landscape Handheld Bezel Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .background(ElegantSurface, RoundedCornerShape(24.dp))
                    .border(2.dp, ElegantBorder, RoundedCornerShape(24.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Wing: D-Pad, Option 1, Pause
                Column(
                    modifier = Modifier
                        .width(170.dp)
                        .fillMaxHeight()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Option buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LynxOptionButton(
                            label = "OPT 1",
                            onClick = { viewModel.showTrickGuide() },
                            testTag = "opt1_button"
                        )
                        LynxOptionButton(
                            label = "PAUSE",
                            onClick = {
                                if (uiState.phase == GamePhase.PLAYING) viewModel.pauseGame()
                                else viewModel.resumeGame()
                            },
                            testTag = "pause_button"
                        )
                    }

                    // D-Pad
                    LynxDPad(
                        viewModel = viewModel,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // Bottom Atari Branding & Speaker Grille
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ATARI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = ElegantPrimaryLavender
                        )
                        LynxSpeakerGrille(modifier = Modifier.size(36.dp, 16.dp))
                    }
                }

                // Center: Handheld Screen with Bezel frame
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 8.dp)
                        .background(ElegantGameScreenBg, RoundedCornerShape(20.dp))
                        .border(2.dp, ElegantBorder, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    screenContent()
                }

                // Right Wing: Action Buttons A & B, Option 2, Power LED
                Column(
                    modifier = Modifier
                        .width(170.dp)
                        .fillMaxHeight()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Controls: Power LED & Option 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF81C784), CircleShape)
                                    .shadow(4.dp, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PWR", fontSize = 8.sp, color = ElegantTextSecondary, fontFamily = FontFamily.Monospace)
                        }

                        LynxOptionButton(
                            label = "OPT 2",
                            onClick = { viewModel.cycleTheme() },
                            testTag = "opt2_button"
                        )
                    }

                    // A & B Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LynxActionButton(
                            label = "B",
                            subLabel = "BUNNYHOP",
                            buttonColor = ElegantPrimaryLavender,
                            isPressedState = { isDown -> viewModel.isJumpPressed = isDown },
                            testTag = "action_button_b"
                        )

                        LynxActionButton(
                            label = "A",
                            subLabel = "STUNT",
                            buttonColor = ElegantSecondaryLavender,
                            isPressedState = { isDown -> viewModel.isPedalPressed = isDown },
                            testTag = "action_button_a",
                            modifier = Modifier.offset(y = (-14).dp)
                        )
                    }

                    // Bottom Branding
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LynxSpeakerGrille(modifier = Modifier.size(36.dp, 16.dp))
                        Text(
                            text = "LYNX",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = ElegantPrimaryLavender
                        )
                    }
                }
            }
        } else {
            // Portrait / Vertical Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ElegantDarkCanvas)
            ) {
                // Top Header matching Elegant Dark specification
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (uiState.phase == GamePhase.PLAYING) viewModel.pauseGame()
                            else viewModel.showTitleScreen()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(ElegantSurfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back / Menu",
                            tint = ElegantTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "BMX Stunt",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElegantTextPrimary
                        )
                        Text(
                            text = "CALIFORNIA GAMES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantPrimaryLavender,
                            letterSpacing = 1.5.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { viewModel.toggleMusic() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(ElegantSurfaceVariant, CircleShape)
                        ) {
                            Icon(
                                if (uiState.isMusicEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                                contentDescription = "Toggle Music",
                                tint = if (uiState.isMusicEnabled) ElegantPrimaryLavender else ElegantTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.cycleTheme() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(ElegantSurfaceVariant, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Cycle Theme",
                                tint = ElegantPrimaryLavender,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Center Game Screen (Screen Container)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.15f)
                        .padding(horizontal = 12.dp)
                        .background(ElegantGameScreenBg, RoundedCornerShape(24.dp))
                        .border(2.dp, ElegantBorder, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    screenContent()
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Handheld Controller Deck
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.85f)
                        .padding(horizontal = 12.dp)
                        .background(ElegantSurface, RoundedCornerShape(28.dp))
                        .border(1.5.dp, ElegantBorder, RoundedCornerShape(28.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Quick sub-header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LynxOptionButton(
                                label = "OPTION 1",
                                onClick = { viewModel.showTrickGuide() },
                                testTag = "opt1_portrait_button"
                            )

                            Text(
                                "TURBO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = ElegantPrimaryLavender.copy(alpha = 0.5f),
                                letterSpacing = 2.sp
                            )

                            LynxOptionButton(
                                label = "OPTION 2",
                                onClick = { viewModel.cycleTheme() },
                                testTag = "opt2_portrait_button"
                            )
                        }

                        // Controllers: D-Pad on Left, Separator, A/B on Right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LynxDPad(
                                viewModel = viewModel,
                                modifier = Modifier.size(118.dp)
                            )

                            // Central subtle divider
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(48.dp)
                                    .background(ElegantBorder.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LynxActionButton(
                                    label = "B",
                                    subLabel = "BUNNYHOP",
                                    buttonColor = ElegantPrimaryLavender,
                                    isPressedState = { isDown -> viewModel.isJumpPressed = isDown },
                                    testTag = "action_button_b_portrait",
                                    modifier = Modifier.offset(y = 8.dp)
                                )

                                LynxActionButton(
                                    label = "A",
                                    subLabel = "STUNT",
                                    buttonColor = ElegantSecondaryLavender,
                                    isPressedState = { isDown -> viewModel.isPedalPressed = isDown },
                                    testTag = "action_button_a_portrait",
                                    modifier = Modifier.offset(y = (-8).dp)
                                )
                            }
                        }

                        // Status Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "HOLD [A] PEDAL • [B] JUMP",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ElegantTextSecondary
                            )
                            Text(
                                "D-PAD FOR STUNTS",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ElegantPrimaryLavender
                            )
                        }
                    }
                }

                // Bottom Tab Navigation Bar matching Elegant Dark styling
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(ElegantDarkCanvas)
                        .border(androidx.compose.foundation.BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f))),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.startNewGame() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.SportsEsports,
                                contentDescription = "Play Event",
                                tint = ElegantPrimaryLavender,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text("PLAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ElegantPrimaryLavender)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.showTrickGuide() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Trick Guide",
                                tint = ElegantTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text("TRICKS", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = ElegantTextSecondary)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.showLeaderboard() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Leaderboard,
                                contentDescription = "Leaderboard",
                                tint = ElegantTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text("RANKS", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = ElegantTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun LynxSpeakerGrille(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barCount = 5
        val step = w / barCount
        for (i in 0 until barCount) {
            drawLine(
                color = ElegantBorder,
                start = Offset(i * step + 2f, 2f),
                end = Offset(i * step + 2f, h - 2f),
                strokeWidth = 2.5f
            )
        }
    }
}
