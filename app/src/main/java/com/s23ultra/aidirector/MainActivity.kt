package com.s23ultra.aidirector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s23ultra.aidirector.service.DirectorOverlayService

class MainActivity : ComponentActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val mp = mediaProjectionManager.getMediaProjection(result.resultCode, result.data!!)
            DirectorOverlayService.mediaProjection = mp

            val serviceIntent = Intent(this, DirectorOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "AI Director Overlay Active! Open Samsung Camera.", Toast.LENGTH_LONG).show()
            finish() // Minimize to background so user can launch Camera
        } else {
            Toast.makeText(this, "Screen capture permission required for viewfinder AI.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setContent {
            MainLauncherScreen(
                onStartOverlay = { checkAndStartOverlay() },
                onOpenOverlaySettings = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    }
                },
                onOpenAccessibilitySettings = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        }
    }

    private fun checkAndStartOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant 'Appear on top' permission first.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }

        // Request MediaProjection
        mediaProjectionLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }
}

@Composable
fun MainLauncherScreen(
    onStartOverlay: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val context = LocalContext.current
    val hasOverlayPerm = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true
    }

    Scaffold(
        containerColor = Color(0xFF06090F)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "S23 Ultra AI Director",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Professional AI Coaching Overlay for Samsung Pro Mode & Expert RAW",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Required S23 Permissions", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        val permStatus = if (hasOverlayPerm) "Granted" else "Needed"
                        Text(
                            text = "1. Appear on Top: $permStatus",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "2. Accessibility Auto-Dialer: Optional (for 1-tap Pro dial adjustments)",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "3. Screen Capture: Prompts on Launch (analyzes viewfinder in real-time)",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenOverlaySettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant 'Appear On Top' Permission", color = Color.White)
                }

                OutlinedButton(
                    onClick = onOpenAccessibilitySettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF06B6D4))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Auto-Dialer in Accessibility", color = Color(0xFF06B6D4))
                }
            }

            Button(
                onClick = onStartOverlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Launch AI Overlay & Open Camera", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
