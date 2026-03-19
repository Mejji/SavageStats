package com.savagestats.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.savagestats.app.ai.FoodScanner
import com.savagestats.app.ui.theme.DarkSurfaceVariant
import com.savagestats.app.ui.theme.LocalWindowSizeClass
import com.savagestats.app.ui.theme.SavageRed
import com.savagestats.app.ui.theme.TextMuted
import com.savagestats.app.ui.theme.TextPrimary
import com.savagestats.app.ui.theme.responsiveDp
import com.savagestats.app.ui.theme.maxContentWidth
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraScreen(
    onMealScanned: (String) -> Unit,
    onBackToDashboard: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val foodScanner = remember { FoodScanner() }
    val isDisposed = remember { AtomicBoolean(false) }
    var isCameraBound by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }

    val widthClass = LocalWindowSizeClass.current.widthSizeClass

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri == null || isScanning) return@rememberLauncherForActivityResult

            isScanning = true
            cameraExecutor.execute {
                try {
                    if (isDisposed.get()) return@execute
                    val bitmap = decodeBitmapFromUri(uri, context)
                    val label = try {
                        foodScanner.scanImage(bitmap)
                    } finally {
                        bitmap.recycle()
                    }
                    if (isDisposed.get()) return@execute
                    ContextCompat.getMainExecutor(context).execute {
                        if (isDisposed.get()) return@execute
                        if (label.isBlank()) {
                            Toast.makeText(context, "No objects detected.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "SavageScanner saw: $label", Toast.LENGTH_SHORT).show()
                            onMealScanned(label)
                            onBackToDashboard()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Gallery scan failed", e)
                } finally {
                    ContextCompat.getMainExecutor(context).execute {
                        isScanning = false
                    }
                }
            }
        },
    )

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return@LaunchedEffect
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener(
            {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                    isCameraBound = true
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Failed to bind camera", e)
                    isCameraBound = false
                }
            },
            mainExecutor,
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            isDisposed.set(true)
            try {
                ProcessCameraProvider.getInstance(context).get().unbindAll()
            } catch (e: Exception) {
                Log.w("CameraScreen", "Failed to unbind camera on dispose", e)
            }
            cameraExecutor.shutdown()
            Thread {
                try {
                    cameraExecutor.awaitTermination(1500, TimeUnit.MILLISECONDS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                } finally {
                    foodScanner.close()
                }
            }.start()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(responsiveDp(widthClass, 24.dp, 32.dp, 40.dp)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Camera permission required to scan meals.",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavageRed,
                        contentColor = TextPrimary,
                    ),
                ) {
                    Text("ALLOW CAMERA")
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(responsiveDp(widthClass, 24.dp, 32.dp, 40.dp))
                .widthIn(max = maxContentWidth(widthClass))
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        if (!isCameraBound || isScanning || !hasCameraPermission) return@Button
                        isScanning = true
                        val outputFile = File.createTempFile("meal_scan_", ".jpg", context.cacheDir)
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                        imageCapture.takePicture(
                            outputOptions,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    try {
                                        val imageUri: Uri = outputFileResults.savedUri ?: Uri.fromFile(outputFile)
                                        val bitmap = if (outputFileResults.savedUri != null) {
                                            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                                                BitmapFactory.decodeStream(stream)
                                            }
                                        } else {
                                            BitmapFactory.decodeFile(outputFile.absolutePath)
                                        } ?: throw IllegalStateException("Unable to decode captured bitmap")
                                        val label = try {
                                            foodScanner.scanImage(bitmap)
                                        } finally {
                                            bitmap.recycle()
                                        }
                                        ContextCompat.getMainExecutor(context).execute {
                                            if (isDisposed.get()) return@execute
                                            if (label.isBlank()) {
                                                Toast.makeText(context, "No objects detected.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "SavageScanner saw: $label", Toast.LENGTH_SHORT).show()
                                                onMealScanned(label)
                                                onBackToDashboard()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CameraScreen", "Scan failed", e)
                                    } finally {
                                        ContextCompat.getMainExecutor(context).execute {
                                            isScanning = false
                                        }
                                        if (outputFile.exists()) {
                                            outputFile.delete()
                                        }
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    val message = "Image capture failed (code=${exception.imageCaptureError})"
                                    Log.e("CameraScreen", message, exception)
                                    ContextCompat.getMainExecutor(context).execute {
                                        isScanning = false
                                    }
                                    if (outputFile.exists()) {
                                        outputFile.delete()
                                    }
                                }
                            },
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    enabled = isCameraBound && !isScanning && hasCameraPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SavageRed,
                        contentColor = TextPrimary,
                        disabledContainerColor = DarkSurfaceVariant,
                        disabledContentColor = TextMuted,
                    ),
                ) {
                    Text(
                        text = if (isScanning) "SCANNING..." else "SCAN MEAL",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.5.sp,
                        ),
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (isScanning) return@OutlinedButton
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    enabled = !isScanning,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SavageRed,
                        disabledContentColor = TextMuted,
                    ),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "GALLERY",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp,
                            ),
                        )
                    }
                }
            }
        }

        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = SavageRed)
                    Text(
                        text = "PROCESSING PHOTO...",
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.8.sp,
                        ),
                    )
                }
            }
        }
    }
}

private fun decodeBitmapFromUri(uri: Uri, context: android.content.Context): Bitmap {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val decodedBitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        if (decodedBitmap.config == Bitmap.Config.ARGB_8888 && decodedBitmap.isMutable) {
            decodedBitmap
        } else {
            decodedBitmap.copy(Bitmap.Config.ARGB_8888, true).also {
                decodedBitmap.recycle()
            }
        }
    } else {
        val legacyBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalStateException("Unable to decode selected image")
        if (legacyBitmap.config == Bitmap.Config.ARGB_8888 && legacyBitmap.isMutable) {
            legacyBitmap
        } else {
            legacyBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }
}
