package com.savagestats.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savagestats.app.ai.LlmInferenceManager
import com.savagestats.app.data.HealthConnectManager
import com.savagestats.app.data.LogRepository
import com.savagestats.app.data.SavageDatabase
import com.savagestats.app.data.UserProfileManager
import com.savagestats.app.data.nutrition.NutritionDatabase
import com.savagestats.app.ui.CoachViewModel
import com.savagestats.app.ui.DashboardViewModel
import com.savagestats.app.ui.MainScreen
import com.savagestats.app.ui.MissionsViewModel
import com.savagestats.app.ui.OnboardingScreen
import com.savagestats.app.ui.ProfileViewModel
import com.savagestats.app.ui.theme.LocalWindowSizeClass
import com.savagestats.app.ui.theme.SavageStatsTheme

class MainActivity : ComponentActivity() {
    private val llmManager = LlmInferenceManager()



    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = SavageDatabase.getDatabase(this)
        val nutritionDatabase = NutritionDatabase.getDatabase(this)
        val repository = LogRepository(database.dailyLogDao(), database.missionDao())
        val foodDao = nutritionDatabase.foodDao()
        val customFoodDao = database.customFoodDao()
        val profileManager = UserProfileManager(this)
        val healthConnectManager = HealthConnectManager(this)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this@MainActivity)

            SavageStatsTheme {
                CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                    val modelStatus by llmManager.loadStatus.collectAsStateWithLifecycle()
                    // null = DataStore hasn't emitted yet (cold start).
                    // Only act once we know the real value — no more 500ms onboarding flash.
                    val isProfileComplete by profileManager.isProfileComplete
                        .collectAsStateWithLifecycle(initialValue = null)

                    if (isProfileComplete == null) {
                        // DataStore is loading — show nothing (the dark window background shows).
                    } else if (isProfileComplete == false) {
                        // Confirmed: profile is incomplete — show onboarding.
                        OnboardingScreen(
                            profileManager = profileManager,
                            onProfileComplete = { /* DataStore write triggers isProfileComplete→true */ }
                        )
                    } else {
                        // Initialize LLM if not already ready
                        if (modelStatus is LlmInferenceManager.ModelLoadStatus.Uninitialized) {
                            llmManager.initialize(this@MainActivity)
                        }

                        val dashboardViewModel: DashboardViewModel = viewModel(
                            factory = DashboardViewModel.Factory(repository, healthConnectManager, profileManager, llmManager, foodDao, customFoodDao)
                        )
                        val coachViewModel: CoachViewModel = viewModel(
                            factory = CoachViewModel.Factory(repository, profileManager, llmManager)
                        )
                        val profileViewModel: ProfileViewModel = viewModel(
                            factory = ProfileViewModel.Factory(profileManager)
                        )
                        val missionsViewModel: MissionsViewModel = viewModel(
                            factory = MissionsViewModel.Factory(repository, profileManager)
                        )

                        MainScreen(
                            dashboardViewModel = dashboardViewModel,
                            coachViewModel = coachViewModel,
                            missionsViewModel = missionsViewModel,
                            profileViewModel = profileViewModel,
                        )
                    }
                }
            }
        }
    }

}
