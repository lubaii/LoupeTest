package com.example.loupetest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.loupetest.ui.components.CameraCaptureView
import com.example.loupetest.ui.components.PhotoViewerWithLoupe
import com.example.loupetest.ui.theme.LoupeTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoupeTestTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var photoPath by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(true) }
    var hasPermission by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }
    
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                !hasPermission -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Требуется разрешение на использование камеры")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Запросить разрешение")
                        }
                    }
                }
                showCamera -> {
                    CameraCaptureView(
                        onPhotoTaken = { path ->
                            photoPath = path
                            showCamera = false
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PhotoViewerWithLoupe(
                            photoUri = photoPath,
                            onTakePhoto = {
                                photoPath = null
                                showCamera = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { showCamera = true }) {
                                Text("Сделать новое фото")
                            }
                        }
                    }
                }
            }
        }
    }
}