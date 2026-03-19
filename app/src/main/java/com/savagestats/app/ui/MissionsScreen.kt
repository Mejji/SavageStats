package com.savagestats.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savagestats.app.data.Mission
import com.savagestats.app.ui.theme.DarkSurface
import com.savagestats.app.ui.theme.DarkSurfaceVariant
import com.savagestats.app.ui.theme.LocalWindowSizeClass
import com.savagestats.app.ui.theme.SavageRed
import com.savagestats.app.ui.theme.SavageRedMuted
import com.savagestats.app.ui.theme.SuccessGreen
import com.savagestats.app.ui.theme.TextMuted
import com.savagestats.app.ui.theme.TextPrimary
import com.savagestats.app.ui.theme.TextSecondary
import com.savagestats.app.ui.theme.maxContentWidth
import com.savagestats.app.ui.theme.responsiveDp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// --- Missions screen ---

@Composable
fun MissionsScreen(viewModel: MissionsViewModel) {
    val activeMissions by viewModel.activeMissions.collectAsStateWithLifecycle()
    val completedMissions by viewModel.completedMissions.collectAsStateWithLifecycle()
    val missionCompleteEvent by viewModel.missionCompleteEvent.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val widthClass = LocalWindowSizeClass.current.widthSizeClass

    // Show celebration snackbar on mission completion
    LaunchedEffect(missionCompleteEvent) {
        missionCompleteEvent?.let {
            snackbarHostState.showSnackbar("Mission Accomplished. +50 XP.")
            viewModel.clearMissionCompleteEvent()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxContentWidth(widthClass))
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = responsiveDp(widthClass, 24.dp, 32.dp, 40.dp))
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
            Spacer(modifier = Modifier.height(12.dp))

            // -- Header ──────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "MISSIONS",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = TextPrimary,
                )
                Text(
                    text = "COMPLETE THEM OR FACE SHAME",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp,
                    ),
                    color = SavageRed,
                )
            }

            // -- Active missions ─────────────────────────────────
            if (activeMissions.isEmpty()) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = DarkSurface,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 6.dp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "NO ACTIVE MISSIONS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                            ),
                            color = SavageRed,
                        )
                        Text(
                            text = "Hit the Coach tab and accept a workout to start a mission.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                        )
                    }
                }
            } else {
                activeMissions.forEach { mission ->
                    MissionCard(
                        mission = mission,
                        onTaskChecked = { taskIndex, isChecked ->
                            viewModel.onTaskChecked(mission, taskIndex, isChecked)
                        },
                    )
                }
            }

            // -- Completed missions ──────────────────────────────
            if (completedMissions.isNotEmpty()) {
                Text(
                    text = "COMPLETED",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = TextMuted,
                )

                completedMissions.forEach { mission ->
                    CompletedMissionCard(mission = mission)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            } // end Column
        } // end Box
    } // end Scaffold
} // end fun

// --- Active mission card with checkbox rows and countdown ---

@Composable
private fun MissionCard(
    mission: Mission,
    onTaskChecked: (taskIndex: Int, isChecked: Boolean) -> Unit,
) {
    val completedCount = mission.tasks.count { it.isDone }
    val totalCount = mission.tasks.size

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = DarkSurface,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Title row with countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mission.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = SavageRed,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                CountdownTimer(expiresAt = mission.expiresAt)
            }

            // Progress text
            Text(
                text = "$completedCount / $totalCount TASKS",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                ),
                color = TextMuted,
            )

            HorizontalDivider(
                color = DarkSurfaceVariant,
                thickness = 0.5.dp,
            )

            // Task checkbox rows
            mission.tasks.forEachIndexed { index, task ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = task.isDone,
                        onCheckedChange = { isChecked ->
                            onTaskChecked(index, isChecked)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SuccessGreen,
                            uncheckedColor = TextMuted,
                            checkmarkColor = TextPrimary,
                        ),
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (task.isDone) TextMuted else TextPrimary,
                        label = "taskTextColor",
                    )

                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (task.isDone) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            },
                        ),
                        color = textColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// --- Countdown timer composable ---

@Composable
private fun CountdownTimer(expiresAt: Long) {
    val remainingText = remember { androidx.compose.runtime.mutableStateOf(formatTimeRemaining(expiresAt)) }

    LaunchedEffect(expiresAt) {
        while (isActive) {
            remainingText.value = formatTimeRemaining(expiresAt)
            delay(60_000L) // Update every minute
        }
    }

    Text(
        text = remainingText.value,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        ),
        color = SavageRed,
    )
}

private fun formatTimeRemaining(expiresAt: Long): String {
    val remaining = expiresAt - System.currentTimeMillis()
    if (remaining <= 0) return "EXPIRED"

    val hours = remaining / (1000 * 60 * 60)
    val minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60)

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

// --- Completed mission card (dimmed) ---

@Composable
private fun CompletedMissionCard(mission: Mission) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = DarkSurfaceVariant,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = mission.title.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${mission.tasks.size} TASKS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
            Text(
                text = "DONE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                ),
                color = SuccessGreen,
            )
        }
    }
}
