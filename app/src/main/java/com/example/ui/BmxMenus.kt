package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.HighScoreEntity
import com.example.model.RETRO_PALETTES
import com.example.ui.theme.*
import com.example.viewmodel.BmxGameViewModel
import com.example.viewmodel.BmxUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TitleMenuOverlay(
    uiState: BmxUiState,
    viewModel: BmxGameViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkCanvas.copy(alpha = 0.88f))
            .testTag("title_menu_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .padding(16.dp)
        ) {
            // Elegant Dark Title Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ElegantBorder)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp)
                ) {
                    Text(
                        text = "CALIFORNIA GAMES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantPrimaryLavender,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "BMX Stunt",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = ElegantTextPrimary
                    )
                    Text(
                        text = "ATARI LYNX 3D ISOMETRIC EDITION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantPrimaryLavender,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Start Main Button
            Button(
                onClick = { viewModel.startNewGame() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElegantPrimaryLavender,
                    contentColor = ElegantOnPrimaryDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_game_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElegantOnPrimaryDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "START BMX EVENT",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = ElegantOnPrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.showTrickGuide() },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("trick_guide_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElegantBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ElegantSurfaceVariant,
                        contentColor = ElegantTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("TRICK GUIDE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElegantTextPrimary)
                }

                OutlinedButton(
                    onClick = { viewModel.showLeaderboard() },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("leaderboard_button"),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElegantBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ElegantSurfaceVariant,
                        contentColor = ElegantTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("HIGH SCORES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElegantPrimaryLavender)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Settings toggles & Palette picker
            Card(
                colors = CardDefaults.cardColors(containerColor = ElegantSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElegantBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Theme cycle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.cycleTheme() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PALETTE:", fontSize = 11.sp, color = ElegantTextSecondary, fontWeight = FontWeight.Medium)
                        Text(
                            RETRO_PALETTES[uiState.selectedThemeIndex].name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantPrimaryLavender
                        )
                    }

                    HorizontalDivider(color = ElegantBorder, modifier = Modifier.padding(vertical = 6.dp))

                    // Audio & Scanline Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        FilterChip(
                            selected = uiState.isMusicEnabled,
                            onClick = { viewModel.toggleMusic() },
                            label = { Text("8-BIT BGM", fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElegantPrimaryLavender,
                                selectedLabelColor = ElegantOnPrimaryDark,
                                containerColor = ElegantSurfaceVariant,
                                labelColor = ElegantTextPrimary
                            )
                        )
                        FilterChip(
                            selected = uiState.isSfxEnabled,
                            onClick = { viewModel.toggleSfx() },
                            label = { Text("SFX", fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElegantPrimaryLavender,
                                selectedLabelColor = ElegantOnPrimaryDark,
                                containerColor = ElegantSurfaceVariant,
                                labelColor = ElegantTextPrimary
                            )
                        )
                        FilterChip(
                            selected = uiState.isScanlinesEnabled,
                            onClick = { viewModel.toggleScanlines() },
                            label = { Text("LCD GRID", fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElegantPrimaryLavender,
                                selectedLabelColor = ElegantOnPrimaryDark,
                                containerColor = ElegantSurfaceVariant,
                                labelColor = ElegantTextPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsSummaryDialog(
    uiState: BmxUiState,
    viewModel: BmxGameViewModel,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("RAD RIDER") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElegantBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("results_summary_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "EVENT COMPLETE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = ElegantPrimaryLavender
                )

                Text(
                    text = uiState.ratingTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElegantTextPrimary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                HorizontalDivider(color = ElegantBorder, modifier = Modifier.padding(vertical = 10.dp))

                // Stats breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL SCORE:", color = ElegantTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${uiState.score} PTS", color = ElegantPrimaryLavender, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TRICKS LANDED:", color = ElegantTextSecondary, fontSize = 12.sp)
                    Text("${uiState.tricksCount}", color = ElegantTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("BEST TRICK:", color = ElegantTextSecondary, fontSize = 12.sp)
                    Text(uiState.bestTrick, color = ElegantSecondaryLavender, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("WIPEOUTS:", color = ElegantTextSecondary, fontSize = 12.sp)
                    Text("${uiState.wipeouts}", color = if (uiState.wipeouts > 0) Color(0xFFFFB4AB) else ElegantTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Enter Name for Leaderboard
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 12) nameInput = it.uppercase() },
                    label = { Text("RIDER NAME", color = ElegantTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ElegantTextPrimary,
                        unfocusedTextColor = ElegantTextPrimary,
                        focusedBorderColor = ElegantPrimaryLavender,
                        unfocusedBorderColor = ElegantBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("name_input_field"),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveCurrentScore(nameInput)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElegantPrimaryLavender,
                            contentColor = ElegantOnPrimaryDark
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_score_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("SAVE SCORE", color = ElegantOnPrimaryDark, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.startNewGame()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ElegantSurfaceVariant,
                            contentColor = ElegantTextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElegantBorder),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("play_again_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("RACE AGAIN", color = ElegantTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardDialog(
    scores: List<HighScoreEntity>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElegantBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .padding(8.dp)
                .testTag("leaderboard_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HALL OF FAME",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = ElegantPrimaryLavender,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "CALIFORNIA BMX RECORDS",
                    fontSize = 10.sp,
                    color = ElegantTextSecondary,
                    letterSpacing = 1.sp
                )

                HorizontalDivider(color = ElegantBorder, modifier = Modifier.padding(vertical = 8.dp))

                if (scores.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO HIGH SCORES YET!\nCOMPLETE A RUN TO MAKE HISTORY.",
                            color = ElegantTextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(scores) { index, item ->
                            val rankColor = when (index) {
                                0 -> ElegantPrimaryLavender // Top
                                1 -> ElegantSecondaryLavender
                                2 -> Color(0xFFCD7F32)
                                else -> ElegantTextPrimary
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .background(if (index % 2 == 0) ElegantSurfaceVariant else Color.Transparent, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = rankColor,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = item.playerName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElegantTextPrimary
                                        )
                                        Text(
                                            text = "Best: ${item.bestTrick} (${dateFormat.format(Date(item.timestamp))})",
                                            fontSize = 9.sp,
                                            color = ElegantTextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = "${item.score}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ElegantPrimaryLavender
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantSurfaceVariant,
                        contentColor = ElegantTextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElegantBorder),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CLOSE", color = ElegantTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TrickGuideDialog(
    onDismiss: () -> Unit
) {
    val tricks = listOf(
        TrickInfo("Backflip", "Hold LEFT / BACK in mid-air", "+600 PTS", "Full 360° backward rotation. Match ground angle on landing!"),
        TrickInfo("Frontflip", "Hold RIGHT / FORWARD in mid-air", "+800 PTS", "High risk extreme stunt. Needs big air off dirt ramp!"),
        TrickInfo("360 Tailwhip", "Tap JUMP (Button B) in mid-air", "+350 PTS", "Whip bike frame in a full 360 circle."),
        TrickInfo("Tabletop", "Hold UP in mid-air", "+300 PTS", "Flat whip horizontal bike tweak."),
        TrickInfo("Superman", "Hold DOWN in mid-air", "+450 PTS", "Extend entire body straight back off the saddle."),
        TrickInfo("Bunny Hop", "Press Button B on ground", "+150 PTS", "Clear mud pits, rocks, and logs cleanly."),
        TrickInfo("Wheelie", "Hold LEFT / BACK while riding ground", "+20/sec", "Balance on rear wheel along flats and downhills."),
        TrickInfo("Clean Combo", "Chain multiple stunts before landing", "2x - 5x MULTIPLIER", "Huge point bonus for multi-trick air combos!")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantSurface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElegantBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 540.dp)
                .padding(8.dp)
                .testTag("trick_guide_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = "BMX TRICK BOOK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = ElegantPrimaryLavender,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "CALIFORNIA GAMES STUNT MANUAL",
                    fontSize = 9.sp,
                    color = ElegantTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = ElegantBorder, modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(tricks) { _, trick ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ElegantSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElegantBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        trick.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElegantPrimaryLavender
                                    )
                                    Text(
                                        trick.score,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ElegantSecondaryLavender
                                    )
                                }
                                Text(
                                    "INPUT: ${trick.input}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElegantTextPrimary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    trick.desc,
                                    fontSize = 10.sp,
                                    color = ElegantTextSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantSurfaceVariant,
                        contentColor = ElegantTextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElegantBorder),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("BACK TO GAME", color = ElegantTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

data class TrickInfo(
    val name: String,
    val input: String,
    val score: String,
    val desc: String
)
