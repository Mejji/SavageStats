package com.savagestats.app.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savagestats.app.ui.theme.DarkSurface
import com.savagestats.app.ui.theme.DarkSurfaceVariant
import com.savagestats.app.ui.theme.LocalWindowSizeClass
import com.savagestats.app.ui.theme.SavageRed
import com.savagestats.app.ui.theme.SavageRedDark
import com.savagestats.app.ui.theme.TextMuted
import com.savagestats.app.ui.theme.TextPrimary
import com.savagestats.app.ui.theme.TextSecondary
import com.savagestats.app.ui.theme.maxContentWidth
import com.savagestats.app.ui.theme.responsiveDp
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val nameInput by viewModel.nameInput.collectAsStateWithLifecycle()
    val ageInput by viewModel.ageInput.collectAsStateWithLifecycle()
    val weightInput by viewModel.weightInput.collectAsStateWithLifecycle()
    val heightInput by viewModel.heightInput.collectAsStateWithLifecycle()
    val selectedGoal by viewModel.selectedGoal.collectAsStateWithLifecycle()
    val customGoalInput by viewModel.customGoalInput.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val widthClass = LocalWindowSizeClass.current.widthSizeClass

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("PROFILE SAVED.")
            viewModel.clearSaveSuccess()
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

            // ── Header ──────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "YOUR STATS",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "KNOW YOURSELF. PUSH HARDER.",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp
                    ),
                    color = SavageRed
                )
            }

            // ── Inputs card ─────────────────────────────────────
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
                        text = "BODY METRICS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = SavageRed
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = viewModel::onNameChanged,
                        label = { Text("NAME") },
                        placeholder = { Text("e.g. John", color = TextMuted) },
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

                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = viewModel::onAgeChanged,
                        label = { Text("AGE") },
                        placeholder = { Text("e.g. 25", color = TextMuted) },
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
                        value = weightInput,
                        onValueChange = viewModel::onWeightChanged,
                        label = { Text("WEIGHT (KG)") },
                        placeholder = { Text("e.g. 80", color = TextMuted) },
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
                        value = heightInput,
                        onValueChange = viewModel::onHeightChanged,
                        label = { Text("HEIGHT (CM)") },
                        placeholder = { Text("e.g. 178", color = TextMuted) },
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
                }
            }

            // ── Goal selector ───────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "GOAL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = TextPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    viewModel.goalOptions.forEach { option ->
                        val isSelected = option == selectedGoal
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onGoalSelected(option) },
                            label = {
                                Text(
                                    text = option.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
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
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Reveal custom goal text field when "Custom" is selected
                    if (selectedGoal == "Custom") {
                        OutlinedTextField(
                            value = customGoalInput,
                            onValueChange = viewModel::onCustomGoalChanged,
                            label = { Text("DESCRIBE YOUR GOAL") },
                            placeholder = { Text("e.g. Aggressive cut, 1800 kcal target...", color = TextMuted) },
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
                }
            }

            // ── Save button ─────────────────────────────────────
            Button(
                onClick = { viewModel.saveProfile() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                enabled = nameInput.isNotBlank() && ageInput.isNotEmpty() &&
                        weightInput.isNotEmpty() && heightInput.isNotEmpty() &&
                        selectedGoal.isNotEmpty() &&
                        (selectedGoal != "Custom" || customGoalInput.isNotBlank()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SavageRed,
                    contentColor = TextPrimary,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "SAVE PROFILE",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        fontSize = 18.sp
                    )
                )
            }

            // ── Current profile card ────────────────────────────
            if (userProfile.age > 0) {
                ProfileSummaryCard(
                    name = userProfile.name,
                    age = userProfile.age,
                    weight = userProfile.weight,
                    height = userProfile.height,
                    goal = userProfile.goal
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            } // end Column
        } // end Box
    } // end Scaffold
} // end fun ProfileScreen

// ── Summary card composable ─────────────────────────────────────

@Composable
private fun ProfileSummaryCard(
    name: String,
    age: Int,
    weight: Float,
    height: Float,
    goal: String
) {
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
                text = "CURRENT PROFILE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                ),
                color = SavageRed
            )

            // Name — full width
            if (name.isNotBlank()) {
                ProfileMetricTile(
                    label = "NAME",
                    value = name,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(
                    color = DarkSurfaceVariant,
                    thickness = 1.dp
                )
            }

            // Metric grid — 2 columns
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileMetricTile(
                    label = "AGE",
                    value = "$age",
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricTile(
                    label = "WEIGHT",
                    value = "${formatFloatValue(weight)} kg",
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(
                color = DarkSurfaceVariant,
                thickness = 1.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileMetricTile(
                    label = "HEIGHT",
                    value = "${formatFloatValue(height)} cm",
                    modifier = Modifier.weight(1f)
                )
                ProfileMetricTile(
                    label = "GOAL",
                    value = goal.uppercase(),
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
private fun ProfileMetricTile(
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
