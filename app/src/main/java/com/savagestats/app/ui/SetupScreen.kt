package com.savagestats.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savagestats.app.ui.theme.DarkBackground
import com.savagestats.app.ui.theme.DarkSurface
import com.savagestats.app.ui.theme.DarkSurfaceVariant
import com.savagestats.app.ui.theme.LocalWindowSizeClass
import com.savagestats.app.ui.theme.SavageRed
import com.savagestats.app.ui.theme.SuccessGreen
import com.savagestats.app.ui.theme.TextMuted
import com.savagestats.app.ui.theme.TextPrimary
import com.savagestats.app.ui.theme.TextSecondary
import com.savagestats.app.ui.theme.responsiveDp
import com.savagestats.app.ui.theme.maxContentWidth
import kotlinx.coroutines.delay

@Composable
fun SetupScreen(viewModel: SetupViewModel, onSetupComplete: () -> Unit) {
    val downloadStatus by viewModel.downloadStatus.collectAsStateWithLifecycle()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            viewModel.startDownload()
        }
    )

    val widthClass = LocalWindowSizeClass.current.widthSizeClass

    Scaffold(containerColor = DarkBackground) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "SAVAGE AI",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
                color = Color.White.copy(alpha = 0.06f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .widthIn(max = maxContentWidth(widthClass))
                    .fillMaxSize()
                    .padding(horizontal = responsiveDp(widthClass, 24.dp, 32.dp, 40.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "INITIALIZING BRAIN...",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Downloading 528MB of pure fitness sarcasm. Do not close the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = DarkSurface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    when (val status = downloadStatus) {
                        is SetupViewModel.DownloadStatus.Idle -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "DOWNLOAD SAVAGE COACH",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = SavageRed,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.startDownload()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(62.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SavageRed,
                                        contentColor = TextPrimary,
                                        disabledContainerColor = DarkSurfaceVariant,
                                        disabledContentColor = TextMuted
                                    ),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "DOWNLOAD AI MODEL",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 2.5.sp,
                                            fontSize = 17.sp
                                        )
                                    )
                                }

                                Text(
                                    text = "~2 GB required. Progress stays trackable in notifications.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        is SetupViewModel.DownloadStatus.ModelAlreadyExists -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "MODEL READY",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = SuccessGreen,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Your Savage AI brain is ready. Loading...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                LaunchedEffect(Unit) {
                                    delay(1500)
                                    onSetupComplete()
                                }
                            }
                        }

                        is SetupViewModel.DownloadStatus.Downloading -> {
                            // Animated pulsing dots
                            val infiniteTransition = rememberInfiniteTransition(label = "dots")
                            val dotAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "dotPulse"
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "DOWNLOADING BRAIN",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = SavageRed.copy(alpha = dotAlpha),
                                    textAlign = TextAlign.Center
                                )

                                LinearProgressIndicator(
                                    progress = { status.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = SavageRed,
                                    trackColor = DarkSurfaceVariant
                                )

                                Text(
                                    text = "${status.progress}%",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (status.totalMb > 0) {
                                        "${status.downloadedMb} MB / ${status.totalMb} MB"
                                    } else {
                                        "${status.downloadedMb} MB downloaded"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "You can return Home — progress stays in notifications.",
                                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        is SetupViewModel.DownloadStatus.Success -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "SYSTEM READY",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = SuccessGreen,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${status.fileSizeMb} MB verified. Launching dashboard...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            LaunchedEffect(Unit) {
                                delay(1000)
                                onSetupComplete()
                            }
                        }

                        is SetupViewModel.DownloadStatus.Failed -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "DOWNLOAD FAILED",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = SavageRed,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = status.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.startDownload()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(62.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SavageRed,
                                        contentColor = TextPrimary,
                                        disabledContainerColor = DarkSurfaceVariant,
                                        disabledContentColor = TextMuted
                                    ),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "RETRY",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 2.5.sp,
                                            fontSize = 17.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}
