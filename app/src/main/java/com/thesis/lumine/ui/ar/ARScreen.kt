package com.thesis.lumine.ui.ar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.utils.CameraManager
import com.thesis.lumine.utils.MediaPipeHelper
import com.thesis.lumine.utils.AROverlayView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(
    jewelry: Jewelry,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Try On: ${jewelry.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (hasCameraPermission) {
                ARCameraView(
                    jewelry = jewelry,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Permission request UI
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera access is needed for AR try-on",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Grant Camera Permission")
                    }
                }
            }

            // Jewelry info card at bottom
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = jewelry.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${jewelry.material.replaceFirstChar { it.uppercase() }} ${jewelry.type.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₱${String.format("%.2f", jewelry.price)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ARCameraView(
    jewelry: Jewelry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraManager: CameraManager? by remember { mutableStateOf(null) }
    var mediaPipeHelper: MediaPipeHelper? by remember { mutableStateOf(null) }
    var overlayView: AROverlayView? by remember { mutableStateOf(null) }
    var trackingActive by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        try {
            mediaPipeHelper = MediaPipeHelper(context)
            trackingActive = true
        } catch (e: Exception) {
            e.printStackTrace()
            trackingActive = false
        }

        onDispose {
            cameraManager?.shutdown()
            mediaPipeHelper?.close()
        }
    }

    Box(modifier = modifier) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    val manager = CameraManager(ctx, this, lifecycleOwner)
                    cameraManager = manager

                    manager.onFrameAvailable = { image, timestamp ->
                        try {
                            when (jewelry.type.lowercase()) {
                                "necklace", "earring" -> {
                                    mediaPipeHelper?.detectFace(image, timestamp)
                                }
                                "ring", "bracelet" -> {
                                    mediaPipeHelper?.detectHand(image, timestamp)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    manager.startCamera()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // AR Overlay
        if (trackingActive) {
            AndroidView(
                factory = { ctx ->
                    AROverlayView(ctx).apply {
                        overlayView = this
                        setJewelryType(jewelry.type)

                        mediaPipeHelper?.onFaceDetected = { result ->
                            updateFaceLandmarks(result)
                        }

                        mediaPipeHelper?.onHandDetected = { result ->
                            updateHandLandmarks(result)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Tracking status indicator
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (trackingActive)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                else
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            )
        ) {
            Text(
                text = if (trackingActive) "AR Tracking Active" else "⚠️ Tracking Unavailable",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}