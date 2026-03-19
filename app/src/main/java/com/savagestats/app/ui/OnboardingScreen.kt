package com.savagestats.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.savagestats.app.data.UserProfile
import com.savagestats.app.data.UserProfileManager
import com.savagestats.app.ui.theme.DarkSurface
import com.savagestats.app.ui.theme.DarkSurfaceVariant
import com.savagestats.app.ui.theme.LocalWindowSizeClass
import com.savagestats.app.ui.theme.SavageRed
import com.savagestats.app.ui.theme.SavageRedDark
import com.savagestats.app.ui.theme.TextMuted
import com.savagestats.app.ui.theme.TextPrimary
import com.savagestats.app.ui.theme.TextSecondary
import com.savagestats.app.ui.theme.responsiveDp
import com.savagestats.app.ui.theme.maxContentWidth
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onProfileComplete: () -> Unit,
    profileManager: UserProfileManager
) {
    var nameInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var heightInput by remember { mutableStateOf("") }
    var selectedGoal by remember { mutableStateOf("") }
    var customGoalInput by remember { mutableStateOf("") }
    var showCustomGoalField by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val widthClass = LocalWindowSizeClass.current.widthSizeClass

    val goalOptions = listOf(
        "Build Muscle",
        "Athletic Conditioning",
        "Lose Fat",
        "Maintenance",
        "Endurance",
        "Custom"
    )

    val isFormValid = nameInput.isNotBlank() &&
            ageInput.isNotBlank() &&
            weightInput.isNotBlank() &&
            heightInput.isNotBlank() &&
            selectedGoal.isNotBlank() &&
            (selectedGoal != "Custom" || customGoalInput.isNotBlank())

    // Determine the actual goal value (either predefined or custom)
    val actualGoal = if (selectedGoal == "Custom") {
        customGoalInput.trim()
    } else {
        selectedGoal
    }

    Scaffold(
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
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
                    text = "WELCOME TO SAVAGE STATS",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "LET'S BUILD YOUR PROFILE",
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
                        text = "BASIC INFO",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = SavageRed
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
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
                        onValueChange = { ageInput = it },
                        label = { Text("AGE") },
                        placeholder = { Text("e.g. 25", color = TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        onValueChange = { weightInput = it },
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
                        onValueChange = { heightInput = it },
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
                    goalOptions.forEach { option ->
                        val isSelected = option == selectedGoal
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedGoal = option
                                if (option == "Custom") {
                                    showCustomGoalField = true
                                } else {
                                    showCustomGoalField = false
                                    customGoalInput = ""
                                }
                            },
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
                }

                // Custom goal text field
                if (showCustomGoalField) {
                    OutlinedTextField(
                        value = customGoalInput,
                        onValueChange = { customGoalInput = it },
                        label = { Text("DESCRIBE YOUR CUSTOM GOAL") },
                        placeholder = { Text("e.g. Improve flexibility and core strength", color = TextMuted) },
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

            // ── Save button ─────────────────────────────────────
            Button(
                onClick = {
                    coroutineScope.launch {
                        val profile = UserProfile(
                            name = nameInput.trim(),
                            age = ageInput.toIntOrNull() ?: 0,
                            weight = weightInput.toFloatOrNull() ?: 0f,
                            height = heightInput.toFloatOrNull() ?: 0f,
                            goal = actualGoal,
                            targetWeight = 0f,
                            savageXp = 0
                        )
                        profileManager.updateProfile(profile)
                        onProfileComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SavageRed,
                    contentColor = TextPrimary,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "LET'S GO",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp,
                        fontSize = 18.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            } // end Column
        } // end Box
    } // end Scaffold
} // end fun
