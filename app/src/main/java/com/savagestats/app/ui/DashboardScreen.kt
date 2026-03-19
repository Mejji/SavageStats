package com.savagestats.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savagestats.app.data.DailyLog
import com.savagestats.app.data.nutrition.FoodItem
import com.savagestats.app.ui.theme.DarkSurface
import com.savagestats.app.ui.theme.DarkSurfaceVariant
import com.savagestats.app.ui.theme.SavageRed
import com.savagestats.app.ui.theme.SavageRedDark
import com.savagestats.app.ui.theme.SavageRedMuted
import com.savagestats.app.ui.theme.SuccessGreen
import com.savagestats.app.ui.theme.TextMuted
import com.savagestats.app.ui.theme.TextPrimary
import com.savagestats.app.ui.theme.TextSecondary
import com.savagestats.app.ui.theme.WarningAmber
import com.savagestats.app.ui.theme.LocalWindowSizeClass
import com.savagestats.app.ui.theme.responsiveDp
import com.savagestats.app.ui.theme.maxContentWidth
import androidx.health.connect.client.PermissionController
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onOpenCamera: () -> Unit = {}
) {
    val proteinInput by viewModel.proteinInput.collectAsStateWithLifecycle()
    val carbsInput by viewModel.carbsInput.collectAsStateWithLifecycle()
    val fatsInput by viewModel.fatsInput.collectAsStateWithLifecycle()
    val fiberInput by viewModel.fiberInput.collectAsStateWithLifecycle()
    val sodiumInput by viewModel.sodiumInput.collectAsStateWithLifecycle()
    val caloriesInput by viewModel.caloriesInput.collectAsStateWithLifecycle()
    val portionSizeInput by viewModel.portionSizeInput.collectAsStateWithLifecycle()
    val durationInput by viewModel.durationInput.collectAsStateWithLifecycle()
    val sleepInput by viewModel.sleepInput.collectAsStateWithLifecycle()
    val foodNameInput by viewModel.foodNameInput.collectAsStateWithLifecycle()
    val customActivityNameInput by viewModel.customActivityNameInput.collectAsStateWithLifecycle()
    val selectedActivity by viewModel.selectedActivity.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedDateLog by viewModel.selectedDateLog.collectAsStateWithLifecycle()
    val weeklyLogs by viewModel.weeklyLogs.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val isEstimatingMacros by viewModel.isEstimatingMacros.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val analyticsUiState by viewModel.analyticsUiState.collectAsStateWithLifecycle()
    val coachReaction by viewModel.coachReaction.collectAsStateWithLifecycle()
    val dailySteps by viewModel.dailySteps.collectAsStateWithLifecycle()
    val dailyActiveCalories by viewModel.dailyActiveCalories.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val widthClass = LocalWindowSizeClass.current.widthSizeClass

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        if (grantedPermissions.containsAll(viewModel.healthPermissions)) {
            viewModel.triggerSyncAfterPermission()
        } else {
            // Silent deny (common on Samsung OneUI) — open HC settings so the
            // user can grant access manually from inside the Health Connect app.
            try {
                val intent = android.content.Intent(
                    "androidx.health.ACTION_MANAGE_HEALTH_PERMISSIONS"
                ).apply {
                    putExtra("android.intent.extra.PACKAGE_NAME", context.packageName)
                }
                context.startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                viewModel.onHealthPermissionsDenied()
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var dashboardTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showCustomFoodDialog by remember { mutableStateOf(false) }

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

            HorizontalCalendar(
                selectedDate = selectedDate,
                onDateSelected = viewModel::onDateSelected
            )

            AnalyticsSection(
                analytics = analyticsUiState,
                selectedDate = selectedDate
            )

            OutlinedButton(
                onClick = {
                    scope.launch {
                        if (!viewModel.isHealthConnectAvailable()) {
                            viewModel.onHealthConnectUnavailable()
                            return@launch
                        }
                        // hasAllPermissions() may resume on a background dispatcher internally.
                        // Capture the result first, then switch back to Main before launching
                        // the permission sheet — ActivityResultLauncher requires the main thread.
                        val hasPerms = viewModel.hasAllPermissions()
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            if (hasPerms) {
                                viewModel.triggerSyncAfterPermission()
                            } else {
                                healthPermissionLauncher.launch(viewModel.healthPermissions)
                            }
                        }
                    }
                },
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

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSurface,
                contentColor = SavageRed
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "NUTRITION",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "TRAINING",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                )
            }

            if (selectedTabIndex == 0) {
                // ── Primary food input with camera fallback ──────────────────
                OutlinedTextField(
                    value = foodNameInput,
                    onValueChange = viewModel::onFoodNameChanged,
                    label = { Text("WHAT DID YOU EAT?") },
                    placeholder = { Text("e.g. 2 scrambled eggs", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { viewModel.searchFoodManually() }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search food database",
                                    tint = TextSecondary
                                )
                            }
                            IconButton(onClick = onOpenCamera) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Scan food with camera",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // ── FTS search results dropdown ──────────────────────
                AnimatedVisibility(
                    visible = searchResults.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ElevatedCard(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(searchResults, key = {it.id }) { food ->
                                FoodSearchResultRow(
                                    food = food,
                                    onClick = { viewModel.onFoodSelected(food) }
                                )
                            }
                        }
                    }
                }

                // ── Add custom food button ──────────────────────
                OutlinedButton(
                    onClick = { showCustomFoodDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, SolidColor(DarkSurfaceVariant))
                ) {
                    Text(
                        text = "+ ADD CUSTOM FOOD",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // ── Custom food dialog ──────────────────────
                if (showCustomFoodDialog) {
                    CreateCustomFoodDialog(
                        onDismiss = { showCustomFoodDialog = false },
                        onSave = { name, calories, protein, carbs, fat, fiber, sodium ->
                            viewModel.saveCustomFood(name, calories, protein, carbs, fat, fiber, sodium)
                            showCustomFoodDialog = false
                        }
                    )
                }

                // ── Coach Reaction Card ──────────────────────
                AnimatedVisibility(
                    visible = coachReaction !is DashboardViewModel.CoachReaction.Idle,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    CoachReactionCard(
                        reaction = coachReaction,
                        onDismiss = { viewModel.dismissCoachReaction() }
                    )
                }

                // GET MACROS: sends food field to Gemma 2B immediately
                Button(
                    onClick = { viewModel.estimateMacrosFromScan(foodNameInput) },
                    enabled = foodNameInput.isNotBlank() && !isEstimatingMacros,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavageRed,
                        contentColor = TextPrimary,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isEstimatingMacros) "ESTIMATING MACROS..." else "GET MACROS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            fontSize = 18.sp
                        )
                    )
                }

                // ── Calories input (auto-fills from DB, user can adjust) ──
                OutlinedTextField(
                    value = caloriesInput,
                    onValueChange = viewModel::onCaloriesChanged,
                    label = { Text("CALORIES (KCAL)") },
                    placeholder = { Text("e.g. 250", color = TextMuted) },
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

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbsInput,
                        onValueChange = viewModel::onCarbsChanged,
                        label = { Text("CARBS (G)") },
                        placeholder = { Text("e.g. 200", color = TextMuted) },
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
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = portionSizeInput,
                    onValueChange = viewModel::onPortionSizeChanged,
                    label = { Text("PORTION SIZE (G)") },
                    placeholder = { Text("e.g. 100", color = TextMuted) },
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

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = fatsInput,
                        onValueChange = viewModel::onFatsChanged,
                        label = { Text("FATS (G)") },
                        placeholder = { Text("e.g. 70", color = TextMuted) },
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
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fiberInput,
                        onValueChange = viewModel::onFiberChanged,
                        label = { Text("FIBER (G)") },
                        placeholder = { Text("e.g. 12", color = TextMuted) },
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
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = sodiumInput,
                    onValueChange = viewModel::onSodiumChanged,
                    label = { Text("SODIUM (MG)") },
                    placeholder = { Text("e.g. 700", color = TextMuted) },
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

                Button(
                    onClick = { viewModel.addMealToDailyLog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavageRed,
                        contentColor = TextPrimary,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "ADD MEAL",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            fontSize = 18.sp
                        )
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.updateSelectedDayLogFromInputs(selectedTabIndex == 0) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SavageRed),
                    border = BorderStroke(1.dp, SolidColor(SavageRed))
                ) {
                    Text(
                        text = if (selectedTabIndex == 0) "UPDATE NUTRITION LOG" else "UPDATE ACTIVITY LOG",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.deleteSelectedDayLog()
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Log deleted.",
                                actionLabel = "UNDO"
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDeleteLog()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SavageRed),
                    border = BorderStroke(1.dp, SolidColor(SavageRed))
                ) {
                    Text(
                        text = "DELETE LOG",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                    )
                }
            } else {
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

                if (selectedActivity == "Other") {
                    OutlinedTextField(
                        value = customActivityNameInput,
                        onValueChange = viewModel::onCustomActivityNameChanged,
                        label = { Text("CUSTOM ACTIVITY NAME") },
                        placeholder = { Text("e.g. stadium hill sprints", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
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
                }

                Button(
                    onClick = { viewModel.logActivityAndSleep() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    enabled = durationInput.isNotEmpty() || sleepInput.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavageRed,
                        contentColor = TextPrimary,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "LOG TRAINING",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            fontSize = 18.sp
                        )
                    )
                }

                Text(
                    text = "Synced today: ${dailySteps} steps • ${formatFloatValue(dailyActiveCalories)} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            TabRow(
                selectedTabIndex = dashboardTabIndex,
                containerColor = DarkSurface,
                contentColor = SavageRed
            ) {
                Tab(
                    selected = dashboardTabIndex == 0,
                    onClick = { dashboardTabIndex = 0 },
                    text = { Text("SUMMARY") }
                )
                Tab(
                    selected = dashboardTabIndex == 1,
                    onClick = { dashboardTabIndex = 1 },
                    text = { Text("REALTIME") }
                )
            }

            // ── Selected day's summary card ─────────────────────
            if (dashboardTabIndex == 0) {
                selectedDateLog?.let { log ->
                    TodaySummaryCard(
                        log = log,
                        selectedDate = selectedDate
                    )
                }
            } else {
                RealtimeDashboardCharts(
                    weeklyLogs = weeklyLogs,
                    selectedDate = selectedDate
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AnalyticsSection(
    analytics: DashboardViewModel.AnalyticsUiState,
    selectedDate: LocalDate
) {
    val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "ANALYTICS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = SavageRed
            )
            Text(
                text = "Numbers don't lie. But you probably did.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            // ── Big Calorie Tracker ──────────────────────────────
            if (analytics.targetCalories > 0f) {
                val calProgress = (analytics.todayCalories / analytics.targetCalories).coerceIn(0f, 1.5f)
                val exceeded = analytics.todayCalories > analytics.targetCalories + 100f
                val calBarColor = when {
                    exceeded -> Color(0xFFFF6B35)          // orange-red — over limit
                    calProgress >= 0.9f -> SuccessGreen    // green — nearly there
                    else -> SavageRed
                }
                val remaining = (analytics.targetCalories - analytics.todayCalories).toInt()

                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, calBarColor),
                    colors = CardDefaults.outlinedCardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CALORIES TODAY",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = calBarColor
                            )
                            Text(
                                text = if (exceeded) "LIMIT EXCEEDED" else "${remaining} LEFT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = calBarColor
                            )
                        }

                        // Giant calorie numbers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${analytics.todayCalories.toInt()}",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = calBarColor
                            )
                            Text(
                                text = "/ ${analytics.targetCalories.toInt()} kcal",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TextSecondary
                            )
                        }

                        // Thick progress bar
                        val displayProgress = (analytics.todayCalories / analytics.targetCalories).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { displayProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp)
                                .clip(RoundedCornerShape(9.dp)),
                            color = calBarColor,
                            trackColor = DarkSurfaceVariant,
                        )

                        if (exceeded) {
                            Text(
                                text = "⚠ ${(analytics.todayCalories - analytics.targetCalories).toInt()} kcal OVER YOUR DAILY LIMIT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color(0xFFFF6B35)
                            )
                        }
                    }
                }
            }

            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TODAY'S FUEL",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = TextPrimary
                        )
                        if (analytics.targetCalories > 0f) {
                            Text(
                                text = "TARGET: ${analytics.targetCalories.toInt()} KCAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = SavageRed
                            )
                        }
                    }

                    MacroProgressRow(
                        label = "Protein",
                        current = analytics.todayProtein,
                        target = analytics.proteinTarget
                    )
                    MacroProgressRow(
                        label = "Carbs",
                        current = analytics.todayCarbs,
                        target = analytics.carbsTarget
                    )
                    MacroProgressRow(
                        label = "Fats",
                        current = analytics.todayFats,
                        target = analytics.fatsTarget
                    )
                }
            }

            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SAVAGE SCORE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Weekly consistency: ${analytics.consistencyScorePercent}%",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = SavageRed
                    )
                    Text(
                        text = "Logged days that hit protein minimum over the last 7 days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurfaceVariant),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "WEEKLY ACTIVITY",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = TextPrimary
                    )

                    WeeklyActivityBars(
                        dayLabels = dayLabels,
                        minutes = analytics.weeklyActivityMinutes
                    )

                    Text(
                        text = "${weekStart.format(DateTimeFormatter.ofPattern("MMM d"))} - ${weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("MMM d"))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroProgressRow(
    label: String,
    current: Float,
    target: Float,
) {
    val safeTarget = target.coerceAtLeast(1f)
    val progress = (current / safeTarget).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$label: ${formatFloatValue(current)}g / ${formatFloatValue(safeTarget)}g",
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = SavageRed,
            trackColor = DarkSurface,
        )
    }
}

@Composable
private fun WeeklyActivityBars(
    dayLabels: List<String>,
    minutes: List<Float>,
) {
    val safeMinutes = if (minutes.size == 7) minutes else List(7) { 0f }
    val maxValue = safeMinutes.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        safeMinutes.forEachIndexed { index, value ->
            val normalizedHeight = ((value / maxValue).coerceIn(0f, 1f) * 88f).dp
            Column(
                modifier = Modifier.defaultMinSize(minWidth = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(92.dp)
                        .background(DarkSurface, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(normalizedHeight.coerceAtLeast(6.dp))
                            .background(SavageRed, RoundedCornerShape(8.dp))
                    )
                }
                Text(
                    text = dayLabels.getOrElse(index) { "?" },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        safeMinutes.forEach { value ->
            Text(
                text = "${value.roundToInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = SavageRed
            )
        }
    }
}

@Composable
private fun RealtimeDashboardCharts(
    weeklyLogs: List<DailyLog>,
    selectedDate: LocalDate,
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "REALTIME TRENDS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                ),
                color = SavageRed
            )

            Text(
                text = "Your week in hard numbers.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val trendLogs = (0..6).map { offset ->
                val day = weekStart.plusDays(offset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                weeklyLogs.firstOrNull { it.date == day } ?: DailyLog(
                    date = day,
                    protein = 0f,
                    carbs = 0f,
                    fats = 0f,
                    fiber = 0f,
                    sodium = 0f,
                    foodName = "",
                    activityDurationMinutes = 0f,
                    activityType = "Rest",
                    sleepHours = 0f,
                    dailySteps = 0L,
                    activeCalories = 0f,
                )
            }

            TrendLineChart(
                title = "Protein (g)",
                values = trendLogs.map { it.protein },
            )

            TrendLineChart(
                title = "Activity (min)",
                values = trendLogs.map { it.activityDurationMinutes },
            )

            TrendLineChart(
                title = "Sleep (hrs)",
                values = trendLogs.map { it.sleepHours },
            )
        }
    }
}

@Composable
private fun TrendLineChart(
    title: String,
    values: List<Float>,
) {
    val safeValues = if (values.isNotEmpty()) values else listOf(0f)
    val maxValue = safeValues.maxOrNull()?.coerceAtLeast(1f) ?: 1f

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            ),
            color = TextPrimary
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            if (safeValues.size < 2) {
                drawCircle(
                    color = SavageRed,
                    radius = 4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                )
                return@Canvas
            }

            val stepX = size.width / (safeValues.size - 1)
            val points = safeValues.mapIndexed { index, value ->
                val x = stepX * index
                val y = size.height - ((value / maxValue).coerceIn(0f, 1f) * size.height)
                androidx.compose.ui.geometry.Offset(x, y)
            }

            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = SavageRed,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )

            points.forEach { point ->
                drawCircle(
                    color = SavageRed,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }
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
                    value = "${formatFloatValue(log.protein)}g",
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

@Composable
private fun FoodSearchResultRow(
    food: FoodItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary,
                maxLines = 2
            )
            Text(
                text = "P: ${formatFloatValue(food.protein)}g • C: ${formatFloatValue(food.carbs)}g • F: ${formatFloatValue(food.fat)}g",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
        Text(
            text = "${food.calories.toInt()} kcal",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = SavageRed
        )
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

@Composable
private fun CoachReactionCard(
    reaction: DashboardViewModel.CoachReaction,
    onDismiss: () -> Unit
) {
    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = when (reaction) {
                is DashboardViewModel.CoachReaction.Loading -> WarningAmber
                is DashboardViewModel.CoachReaction.Success -> SavageRed
                is DashboardViewModel.CoachReaction.Error -> SavageRed.copy(alpha = 0.6f)
                else -> DarkSurfaceVariant
            }
        ),
        colors = CardDefaults.outlinedCardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        when (reaction) {
            is DashboardViewModel.CoachReaction.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = SavageRed,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "COACH IS JUDGING YOU...",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextMuted
                        )
                    }
                }
            }
            
            is DashboardViewModel.CoachReaction.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Verdict section
                    if (reaction.verdict.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "VERDICT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = SavageRed
                            )
                            Text(
                                text = reaction.verdict,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 24.sp
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                    
                    // Strategy section
                    if (reaction.strategy.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                            Text(
                                text = "STRATEGY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color(0xFF2196F3)
                            )
                            Text(
                                text = reaction.strategy,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    
                    // Workout section
                    if (reaction.workout.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
                            Text(
                                text = "PUNISHMENT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = Color(0xFFFF6B35)
                            )
                            Text(
                                text = reaction.workout,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextPrimary
                            )
                        }
                    }
                    
                    // Dismiss button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextMuted
                        )
                    ) {
                        Text(
                            text = "DISMISS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }
            
            is DashboardViewModel.CoachReaction.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = reaction.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextMuted
                        )
                    ) {
                        Text(
                            text = "DISMISS",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            
            else -> { /* Idle - nothing to show */ }
        }
    }
}

// ── Custom Food Dialog ──────────────────────────────────────────

@Composable
private fun CreateCustomFoodDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, calories: Float, protein: Float, carbs: Float, fat: Float, fiber: Float, sodium: Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    var sodium by remember { mutableStateOf("") }

    val macroFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SavageRed,
        unfocusedBorderColor = DarkSurfaceVariant,
        cursorColor = SavageRed,
        focusedLabelColor = SavageRed,
        unfocusedLabelColor = TextSecondary,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = DarkSurface,
        unfocusedContainerColor = DarkSurface
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "ADD CUSTOM FOOD",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                ),
                color = SavageRed
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Create a food that will appear in search results.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                // ── Food name ──
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("FOOD NAME") },
                    placeholder = { Text("e.g. Protein shake", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = RoundedCornerShape(12.dp),
                    colors = macroFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // ── Calories ──
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("CALORIES (KCAL)") },
                    placeholder = { Text("e.g. 250", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = macroFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // ── Row 1: Protein / Carbs / Fat ──
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text("PROTEIN") },
                        placeholder = { Text("g", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = macroFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text("CARBS") },
                        placeholder = { Text("g", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = macroFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text("FAT") },
                        placeholder = { Text("g", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = macroFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // ── Row 2: Fiber / Sodium ──
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fiber,
                        onValueChange = { fiber = it },
                        label = { Text("FIBER") },
                        placeholder = { Text("g", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = macroFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sodium,
                        onValueChange = { sodium = it },
                        label = { Text("SODIUM") },
                        placeholder = { Text("mg", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = macroFieldColors,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        calories.toFloatOrNull() ?: 0f,
                        protein.toFloatOrNull() ?: 0f,
                        carbs.toFloatOrNull() ?: 0f,
                        fat.toFloatOrNull() ?: 0f,
                        fiber.toFloatOrNull() ?: 0f,
                        sodium.toFloatOrNull() ?: 0f
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SavageRed,
                    contentColor = TextPrimary,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "SAVE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = BorderStroke(1.dp, SolidColor(DarkSurfaceVariant))
            ) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    )
}
