package com.example.savagestats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.savagestats.ai.LlmInferenceManager
import com.example.savagestats.data.HealthConnectManager
import com.example.savagestats.data.LogRepository
import com.example.savagestats.data.SavageDatabase
import com.example.savagestats.data.UserProfileManager
import com.example.savagestats.ui.CoachViewModel
import com.example.savagestats.ui.DashboardViewModel
import com.example.savagestats.ui.MainScreen
import com.example.savagestats.ui.ProfileViewModel
import com.example.savagestats.ui.SetupScreen
import com.example.savagestats.ui.SetupViewModel
import com.example.savagestats.ui.theme.SavageStatsTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private val llmManager = LlmInferenceManager()

    private fun isModelFileValid(modelFile: File): Boolean {
        return modelFile.exists() && modelFile.length() >= 1_300_000_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = SavageDatabase.getDatabase(this)
        val repository = LogRepository(database.dailyLogDao())
        val profileManager = UserProfileManager(this)
        val healthConnectManager = HealthConnectManager(this)
        val modelFile = File(filesDir, "gemma-2b.bin")

        setContent {
            SavageStatsTheme {
                var isSetupComplete by rememberSaveable {
                    mutableStateOf(isModelFileValid(modelFile))
                }

                if (!isSetupComplete) {
                    val setupViewModel: SetupViewModel = viewModel(
                        factory = SetupViewModel.Factory(applicationContext)
                    )
                    SetupScreen(
                        viewModel = setupViewModel,
                        onSetupComplete = {
                            llmManager.initialize(this@MainActivity)
                            isSetupComplete = true
                        }
                    )
                } else {
                    // Initialize LLM if not already ready
                    if (llmManager.loadStatus.value is LlmInferenceManager.ModelLoadStatus.Uninitialized) {
                        llmManager.initialize(this@MainActivity)
                    }

                    val dashboardViewModel: DashboardViewModel = viewModel(
                        factory = DashboardViewModel.Factory(repository, healthConnectManager)
                    )
                    val coachViewModel: CoachViewModel = viewModel(
                        factory = CoachViewModel.Factory(repository, profileManager, llmManager)
                    )
                    val profileViewModel: ProfileViewModel = viewModel(
                        factory = ProfileViewModel.Factory(profileManager)
                    )

                    MainScreen(
                        dashboardViewModel = dashboardViewModel,
                        coachViewModel = coachViewModel,
                        profileViewModel = profileViewModel,
                    )
                }
            }
        }
    }
}
