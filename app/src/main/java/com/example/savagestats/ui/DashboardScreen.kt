package com.example.savagestats.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.savagestats.data.DailyLog
import com.example.savagestats.ui.theme.DarkSurface
import com.example.savagestats.ui.theme.DarkSurfaceVariant
import com.example.savagestats.ui.theme.SavageRed
import com.example.savagestats.ui.theme.SavageRedDark
import com.example.savagestats.ui.theme.SavageRedMuted
import com.example.savagestats.ui.theme.TextMuted
import com.example.savagestats.ui.theme.TextPrimary
import com.example.savagestats.ui.theme.TextSecondary
import androidx.health.connect.client.PermissionController
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val proteinInput by viewModel.proteinInput.collectAsStateWithLifecycle()
    val durationInput by viewModel.durationInput.collectAsStateWithLifecycle()
    val sleepInput by viewModel.sleepInput.collectAsStateWithLifecycle()
    val selectedActivity by viewModel.selectedActivity.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedDateLog by viewModel.selectedDateLog.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        if (grantedPermissions.containsAll(viewModel.healthPermissions)) {
            viewModel.syncWithHealthConnect()
        } else {
            viewModel.onHealthPermissionsDenied()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("LOGGED.")
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(syncMessage) {
        syncMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSyncMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
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

            HorizontalCalendar(
                selectedDate = selectedDate,
                onDateSelected = viewModel::onDateSelected
            )

            OutlinedButton(
                onClick = { viewModel.checkPermissionsAndRunSync(healthPermissionLauncher) },
                enabled = !isSyncing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SavageRed
                ),
                border = BorderStroke(width = 1.dp, brush = SolidColor(SavageRed))
            ) {
                Text(
                    text = if (isSyncing) "SYNCING DEVICES..." else "SYNC DEVICES",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
            }

            // ── Header ──────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TODAY'S GRIND",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "TRACK. GRIND. REPEAT.",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp
                    ),
                    color = SavageRed
                )
            }

            // ── Protein input ───────────────────────────────────
            OutlinedTextField(
                value = proteinInput,
                onValueChange = viewModel::onProteinChanged,
                label = { Text("PROTEIN (G)") },
                placeholder = { Text("e.g. 180", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavageRed,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    cursorColor = SavageRed,
                    focusedLabelColor = SavageRed,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Duration input ──────────────────────────────────
            OutlinedTextField(
                value = durationInput,
                onValueChange = viewModel::onDurationChanged,
                label = { Text("DURATION (MIN)") },
                placeholder = { Text("e.g. 60", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavageRed,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    cursorColor = SavageRed,
                    focusedLabelColor = SavageRed,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Sleep input ─────────────────────────────────────
            OutlinedTextField(
                value = sleepInput,
                onValueChange = viewModel::onSleepChanged,
                label = { Text("SLEEP (HRS)") },
                placeholder = { Text("e.g. 8", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavageRed,
                    unfocusedBorderColor = DarkSurfaceVariant,
                    cursorColor = SavageRed,
                    focusedLabelColor = SavageRed,
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Activity chips ──────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ACTIVITY",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = TextPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.activityOptions) { option ->
                        val isSelected = option == selectedActivity
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onActivitySelected(option) },
                            label = {
                                Text(
                                    text = option.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = DarkSurface,
                                labelColor = TextSecondary,
                                selectedContainerColor = SavageRed,
                                selectedLabelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = DarkSurfaceVariant,
                                selectedBorderColor = SavageRedDark,
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.dp
                            )
                        )
                    }
                }
            }

            // ── Log button ──────────────────────────────────────
            Button(
                onClick = { viewModel.saveLog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                enabled = proteinInput.isNotEmpty() && durationInput.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SavageRed,
                    contentColor = TextPrimary,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "LOG IT",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        fontSize = 18.sp
                    )
                )
            }

            // ── Selected day's summary card ─────────────────────
            selectedDateLog?.let { log ->
                TodaySummaryCard(
                    log = log,
                    selectedDate = selectedDate
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HorizontalCalendar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val weekDates = (0L..6L).map(weekStart::plusDays)
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val dateFormatter = DateTimeFormatter.ofPattern("d")

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "THIS WEEK",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = TextPrimary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(weekDates) { day ->
                val isSelected = day == selectedDate
                ElevatedCard(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isSelected) SavageRed else DarkSurface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = if (isSelected) 6.dp else 2.dp
                    ),
                    modifier = Modifier
                        .size(width = 64.dp, height = 82.dp)
                        .clickable { onDateSelected(day) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = day.format(dayFormatter),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = if (isSelected) TextPrimary else TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = day.format(dateFormatter),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Summary card composable ─────────────────────────────────────

@Composable
private fun TodaySummaryCard(
    log: DailyLog,
    selectedDate: LocalDate
) {
    val dateLabel = selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))

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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "LOG FOR ${dateLabel.uppercase()}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = SavageRed
            )

            // Metric grid — 2 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    label = "PROTEIN",
                    value = "${formatFloatValue(log.proteinGrams)}g",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "SLEEP",
                    value = "${formatFloatValue(log.sleepHours)} hrs",
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                color = DarkSurfaceVariant,
                thickness = 1.dp
            )

            // Activity + Duration row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTile(
                    label = "ACTIVITY",
                    value = log.activityType.uppercase(),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "DURATION",
                    value = "${formatFloatValue(log.activityDurationMinutes)} min",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatFloatValue(value: Float): String {
    val rounded = (value * 10f).roundToInt() / 10f
    val isWhole = rounded % 1f == 0f
    return if (isWhole) rounded.toInt().toString() else rounded.toString()
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .then(
                Modifier
                    .height(72.dp)
                    .padding(vertical = 4.dp)
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.sp
            ),
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = TextPrimary
        )
    }
}
