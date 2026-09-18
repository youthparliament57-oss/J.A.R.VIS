package com.example.ui.hud

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.perception.vision.VisionAnalysisResult
import com.example.ui.theme.NousAmberWarning
import com.example.ui.theme.NousCyanGlow
import com.example.ui.theme.NousCyanNeon
import com.example.ui.theme.NousObsidianDark
import com.example.ui.theme.NousSurfaceDark
import com.example.ui.theme.NousSurfaceVariant
import com.example.ui.theme.NousTextPrimary
import com.example.ui.theme.NousTextSecondary
import java.util.concurrent.Executors

@Composable
fun OpticalViewportCard(
    isAnalyzing: Boolean,
    lastAnalysis: VisionAnalysisResult?,
    onFrameCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    isCameraEnabled: Boolean = false,
    onToggleCamera: ((Boolean) -> Unit)? = null
) {
    var internalCameraActive by remember { mutableStateOf(false) }
    val cameraActive = onToggleCamera?.let { isCameraEnabled } ?: internalCameraActive

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
        }
    }

    DisposableEffect(cameraActive, lifecycleOwner) {
        if (cameraActive) {
            try {
                cameraController.bindToLifecycle(lifecycleOwner)
            } catch (e: Exception) {
                // Handled safely
            }
        } else {
            try {
                cameraController.unbind()
            } catch (e: Exception) {
                // Handled safely
            }
        }
        onDispose {
            try {
                cameraController.unbind()
            } catch (e: Exception) {
                // Handled safely
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NousSurfaceDark)
            .border(1.dp, NousCyanNeon.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("optical_viewport_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable {
                    val nextState = !cameraActive
                    if (onToggleCamera != null) {
                        onToggleCamera(nextState)
                    } else {
                        internalCameraActive = nextState
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Optical Sensor",
                    tint = if (cameraActive) NousCyanNeon else NousTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "OPTICAL SENSOR VIEWPORT",
                    color = if (cameraActive) NousCyanNeon else NousTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = when {
                        isAnalyzing -> "ANALYZING..."
                        cameraActive -> "STANDBY / ACTIVE"
                        else -> "STANDBY (TAP TO OPEN)"
                    },
                    color = when {
                        isAnalyzing -> NousAmberWarning
                        cameraActive -> NousCyanGlow
                        else -> NousTextSecondary
                    },
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        val nextState = !cameraActive
                        if (onToggleCamera != null) {
                            onToggleCamera(nextState)
                        } else {
                            internalCameraActive = nextState
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (cameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = if (cameraActive) "Turn Camera Off" else "Turn Camera On",
                        tint = if (cameraActive) NousCyanNeon else NousTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = cameraActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                // Camera Preview Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, NousCyanNeon.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .background(NousObsidianDark)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                controller = cameraController
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )

                    // Reticle Target overlay in center
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, NousCyanNeon.copy(alpha = 0.6f), CircleShape)
                            .align(Alignment.Center)
                    )

                    // Tap to capture & analyze immediately button
                    Button(
                        onClick = {
                            cameraController.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val bitmap = image.toBitmap()
                                        image.close()
                                        ContextCompat.getMainExecutor(context).execute {
                                            onFrameCaptured(bitmap)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        // Handled safely
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NousCyanNeon,
                            contentColor = NousObsidianDark
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .testTag("capture_frame_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Frame",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "INSPECT",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        lastAnalysis?.let { analysis ->
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(NousSurfaceVariant)
                    .padding(8.dp)
            ) {
                Text(
                    text = analysis.summary,
                    color = NousTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (analysis.detectedObjects.isNotEmpty()) {
                    Text(
                        text = "FEATURES: ${analysis.detectedObjects.joinToString(" • ")}",
                        color = NousCyanGlow,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
