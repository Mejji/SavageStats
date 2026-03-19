package com.savagestats.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savagestats.app.ai.LlmInferenceManager
import com.savagestats.app.ai.ModelDownloadService
import com.savagestats.app.data.calculateSavageRank
import com.savagestats.app.data.xpForNextRank
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.savagestats.app.ui.theme.DarkSurface
import com.savagestats.app.ui.theme.DarkSurfaceVariant
import com.savagestats.app.ui.theme.LocalWindowSizeClass
import com.savagestats.app.ui.theme.SavageRed
import com.savagestats.app.ui.theme.SavageRedDark
import com.savagestats.app.ui.theme.SavageRedMuted
import com.savagestats.app.ui.theme.SuccessGreen
import com.savagestats.app.ui.theme.TextMuted
import com.savagestats.app.ui.theme.TextPrimary
import com.savagestats.app.ui.theme.TextSecondary
import com.savagestats.app.ui.theme.WarningAmber
import com.savagestats.app.ui.theme.maxContentWidth
import com.savagestats.app.ui.theme.responsiveDp
import kotlinx.coroutines.launch

@Composable
fun CoachScreen(viewModel: CoachViewModel) {
    val context = LocalContext.current
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isGeneratingWorkout by viewModel.isGeneratingWorkout.collectAsStateWithLifecycle()
    val coachResponse by viewModel.coachResponse.collectAsStateWithLifecycle()
    val verdictText by viewModel.verdictText.collectAsStateWithLifecycle()
    val strategyText by viewModel.strategyText.collectAsStateWithLifecycle()
    val workoutText by viewModel.workoutText.collectAsStateWithLifecycle()
    val workoutSuggestion by viewModel.workoutSuggestion.collectAsStateWithLifecycle()
    val workoutSuggestionItems by viewModel.workoutSuggestionItems.collectAsStateWithLifecycle()
    val modelStatus by viewModel.modelStatus.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val missionAccepted by viewModel.missionAccepted.collectAsStateWithLifecycle()
    val showModelCard by viewModel.showModelCard.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val widthClass = LocalWindowSizeClass.current.widthSizeClass

    // Download progress state
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var isInitializing by remember { mutableStateOf(false) }

    // Show snackbar when mission is accepted
    LaunchedEffect(missionAccepted) {
        if (missionAccepted) {
            snackbarHostState.showSnackbar("Mission accepted. Clock is ticking. 24 hours.")
            viewModel.clearMissionAccepted()
        }
    }

    // Download progress broadcast receiver
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                when (intent?.action) {
                    ModelDownloadService.ACTION_DOWNLOAD_PROGRESS -> {
                        val progress = intent.getIntExtra(ModelDownloadService.EXTRA_PROGRESS, 0)
                        downloadProgress = progress / 100f
                        isDownloading = true
                    }
                    ModelDownloadService.ACTION_DOWNLOAD_COMPLETE -> {
                        isDownloading = false
                        downloadProgress = 1f
                        isInitializing = true
                        // Trigger model initialization using the composable's context
                        viewModel.reinitializeModel(context)
                    }
                    ModelDownloadService.ACTION_DOWNLOAD_ERROR -> {
                        val error = intent.getStringExtra(ModelDownloadService.EXTRA_ERROR_MESSAGE)
                        isDownloading = false
                        downloadError = error
                        scope.launch {
                            snackbarHostState.showSnackbar("Download failed: $error")
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(ModelDownloadService.ACTION_DOWNLOAD_PROGRESS)
            addAction(ModelDownloadService.ACTION_DOWNLOAD_COMPLETE)
            addAction(ModelDownloadService.ACTION_DOWNLOAD_ERROR)
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = responsiveDp(widthClass, 24.dp, 32.dp, 40.dp), vertical = 12.dp)
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
            Spacer(modifier = Modifier.height(12.dp))

            // -- Header ──────────────────────────────────────────
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

            // -- Savage Rank Badge + XP Progress Bar ─────────────
            val rank = calculateSavageRank(
                currentWeight = userProfile.weight,
                targetWeight = userProfile.targetWeight,
                xp = userProfile.savageXp
            )
            val (currentFloor, nextCeiling) = xpForNextRank(userProfile.savageXp)
            val xpProgress = if (nextCeiling > currentFloor) {
                ((userProfile.savageXp - currentFloor).toFloat() / (nextCeiling - currentFloor)).coerceIn(0f, 1f)
            } else {
                1f
            }

            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SAVAGE RANK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.5.sp
                                ),
                                color = TextMuted
                            )
                            Text(
                                text = rank.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = when (rank) {
                                    "Savage God" -> WarningAmber
                                    "Local Threat" -> SuccessGreen
                                    "Iron Novice" -> TextPrimary
                                    else -> SavageRed
                                }
                            )
                        }
                        Text(
                            text = "${userProfile.savageXp} XP",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { xpProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SavageRed,
                            trackColor = SavageRedMuted,
                        )
                        Text(
                            text = "$currentFloor/$nextCeiling",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // -- Recent logs card ────────────────────────────────
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
                            text = "${log.protein}g · ${log.activityType} · ${log.activityDurationMinutes}m · ${log.sleepHours}h",
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

            // -- Loading / Response area ─────────────────────────
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

            if (!isGenerating && verdictText.isNotBlank()) {
                CoachSectionCard(
                    title = "THE VERDICT",
                    content = verdictText,
                    accentColor = SavageRed
                )
            }

            if (!isGenerating && strategyText.isNotBlank()) {
                CoachSectionCard(
                    title = "GAME PLAN",
                    content = strategyText,
                    accentColor = Color(0xFF2196F3)  // Blue
                )
            }

            if (!isGenerating && workoutText.isNotBlank()) {
                CoachSectionCard(
                    title = "PUNISHMENT",
                    content = workoutText,
                    accentColor = Color(0xFFFF6B35)  // Orange
                )
            }

            // -- Workout suggestion area ─────────────────────────
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
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = DarkSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = SavageRed
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Orders Alert",
                                tint = SavageRed
                            )
                            Text(
                                text = "TODAY'S ORDERS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.8.sp
                                ),
                                color = SavageRed
                            )
                        }

                        HorizontalDivider(
                            color = DarkSurfaceVariant,
                            thickness = 1.dp
                        )

                        workoutSuggestionItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Workout Item",
                                    tint = SavageRed,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.acceptMission()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, SavageRed),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SavageRed,
                                containerColor = DarkSurfaceVariant
                            )
                        ) {
                            Text(
                                text = "ACCEPT MISSION",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            )
                        }
                    }
                }
            }

            // -- Model status ────────────────────────────────────
            if (showModelCard && (modelStatus !is LlmInferenceManager.ModelLoadStatus.Ready || isDownloading || isInitializing)) {
                ElevatedCard(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = DarkSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when {
                            // Show initializing state after download completes
                            isInitializing && modelStatus is LlmInferenceManager.ModelLoadStatus.Loading -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = SuccessGreen,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.height(18.dp)
                                    )
                                    Text(
                                        text = "INITIALIZING AI...",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            letterSpacing = 1.sp
                                        ),
                                        color = SuccessGreen
                                    )
                                }
                            }
                            // Show ready state with dismiss button
                            modelStatus is LlmInferenceManager.ModelLoadStatus.Ready && isInitializing -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "✓ AI READY",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.5.sp
                                        ),
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = "Savage Coach is ready to roast",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Button(
                                        onClick = { 
                                            viewModel.hideModelCard()
                                            isInitializing = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DarkSurfaceVariant,
                                            contentColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "DISMISS",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                    }
                                }
                            }
                            // Show downloading state
                            isDownloading -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "DOWNLOADING AI BRAIN",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            letterSpacing = 1.sp
                                        ),
                                        color = SavageRed
                                    )
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = SavageRed,
                                        trackColor = SavageRedMuted
                                    )
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}% complete",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            // Show download button
                            modelStatus is LlmInferenceManager.ModelLoadStatus.NeedsDownload -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "AI BRAIN NOT DOWNLOADED",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            letterSpacing = 1.sp
                                        ),
                                        color = WarningAmber
                                    )
                                    Button(
                                        onClick = {
                                            isDownloading = true
                                            downloadProgress = 0f
                                            downloadError = null
                                            val intent = ModelDownloadService.createStartIntent(context)
                                            ContextCompat.startForegroundService(context, intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SavageRed,
                                            contentColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "DOWNLOAD AI BRAIN (555MB)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                    }
                                    Text(
                                        text = "Connect to WiFi for best results",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                }
                            }
                            // Show loading state
                            modelStatus is LlmInferenceManager.ModelLoadStatus.Loading -> {
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
                            // Show error state
                            modelStatus is LlmInferenceManager.ModelLoadStatus.Error -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "MODEL ERROR",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            letterSpacing = 1.sp
                                        ),
                                        color = SavageRed
                                    )
                                    Text(
                                        text = (modelStatus as LlmInferenceManager.ModelLoadStatus.Error).message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            // Uninitialized state
                            else -> {
                                Text(
                                    text = "MODEL NOT LOADED",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        letterSpacing = 1.sp
                                    ),
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            } // end Column
        } // end Box
    } // end Scaffold
} // end fun CoachScreen

@Composable
private fun CoachSectionCard(
    title: String,
    content: String,
    accentColor: Color
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, accentColor),
        colors = CardDefaults.outlinedCardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = accentColor
            )
            HorizontalDivider(color = accentColor.copy(alpha = 0.3f), thickness = 1.dp)
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 26.sp
                ),
                color = TextPrimary
            )
        }
    }
}
