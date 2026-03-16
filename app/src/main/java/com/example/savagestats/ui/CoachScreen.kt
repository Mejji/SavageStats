package com.example.savagestats.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.savagestats.ai.LlmInferenceManager
import com.example.savagestats.ui.theme.DarkSurface
import com.example.savagestats.ui.theme.DarkSurfaceVariant
import com.example.savagestats.ui.theme.SavageRed
import com.example.savagestats.ui.theme.SavageRedDark
import com.example.savagestats.ui.theme.TextMuted
import com.example.savagestats.ui.theme.TextPrimary
import com.example.savagestats.ui.theme.TextSecondary
import com.example.savagestats.ui.theme.WarningAmber

@Composable
fun CoachScreen(viewModel: CoachViewModel) {
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isGeneratingWorkout by viewModel.isGeneratingWorkout.collectAsStateWithLifecycle()
    val coachResponse by viewModel.coachResponse.collectAsStateWithLifecycle()
    val workoutSuggestion by viewModel.workoutSuggestion.collectAsStateWithLifecycle()
    val modelStatus by viewModel.modelStatus.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.roastMyWeek() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavageRedDark,
                        contentColor = TextPrimary,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    ),
                    enabled = !isGenerating &&
                            !isGeneratingWorkout &&
                            modelStatus is LlmInferenceManager.ModelLoadStatus.Ready &&
                            recentLogs.isNotEmpty()
                ) {
                    Text(
                        text = "ROAST MY WEEK",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            fontSize = 18.sp
                        )
                    )
                }

                Button(
                    onClick = { viewModel.generateWorkoutSuggestion() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavageRed,
                        contentColor = TextPrimary,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    ),
                    enabled = !isGeneratingWorkout &&
                            !isGenerating &&
                            modelStatus is LlmInferenceManager.ModelLoadStatus.Ready
                ) {
                    Text(
                        text = "SUGGEST WORKOUT",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.5.sp,
                            fontSize = 17.sp
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── Header ──────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "THE SAVAGE COACH",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "YOUR OFFLINE AI ROASTMASTER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp
                    ),
                    color = SavageRed
                )
            }

            // ── Recent logs card ────────────────────────────────
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = DarkSurface
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 6.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "THIS WEEK'S LOG",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = SavageRed
                    )

                    if (recentLogs.isEmpty()) {
                        Text(
                            text = "NO DATA. LOG SOMETHING FIRST.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            itemsIndexed(recentLogs) { index, log ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.date,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                letterSpacing = 0.5.sp
                                            ),
                                            color = TextMuted
                                        )
                                        Text(
                                            text = "${log.proteinGrams}g · ${log.activityType} · ${log.activityDurationMinutes}m · ${log.sleepHours}h",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (index < recentLogs.lastIndex) {
                                        HorizontalDivider(
                                            color = DarkSurfaceVariant,
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Loading / Response area ─────────────────────────
            if (isGenerating) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = DarkSurfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 4.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = SavageRed,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "ANALYZING YOUR WEAKNESS...",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            if (!isGenerating && coachResponse != null) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = DarkSurfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 4.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "THE VERDICT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = SavageRed
                        )

                        HorizontalDivider(
                            color = DarkSurface,
                            thickness = 1.dp
                        )

                        coachResponse?.let { response ->
                            Text(
                                text = response,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 26.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // ── Workout suggestion area ─────────────────────────
            if (isGeneratingWorkout) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = DarkSurfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 4.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = SavageRed,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "FORGING TODAY'S PAIN...",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            if (!isGeneratingWorkout && workoutSuggestion != null) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = DarkSurfaceVariant
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 4.dp
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "TODAY'S ORDERS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = SavageRed
                        )

                        HorizontalDivider(
                            color = DarkSurface,
                            thickness = 1.dp
                        )

                        workoutSuggestion?.let { response ->
                            Text(
                                text = response,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 26.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // ── Model status ────────────────────────────────────
            if (modelStatus !is LlmInferenceManager.ModelLoadStatus.Ready) {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val status = modelStatus) {
                            is LlmInferenceManager.ModelLoadStatus.Uninitialized -> {
                                Text(
                                    text = "MODEL NOT LOADED",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        letterSpacing = 1.sp
                                    ),
                                    color = TextMuted
                                )
                            }
                            is LlmInferenceManager.ModelLoadStatus.Loading -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = WarningAmber,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.height(18.dp)
                                    )
                                    Text(
                                        text = "LOADING MODEL...",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            letterSpacing = 1.sp
                                        ),
                                        color = WarningAmber
                                    )
                                }
                            }
                            is LlmInferenceManager.ModelLoadStatus.Error -> {
                                Text(
                                    text = "MODEL ERROR: ${status.message}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = SavageRed
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
