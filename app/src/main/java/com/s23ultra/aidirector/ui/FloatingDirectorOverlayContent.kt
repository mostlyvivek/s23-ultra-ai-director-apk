package com.s23ultra.aidirector.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.s23ultra.aidirector.model.S23ProRecipe
import com.s23ultra.aidirector.model.SceneTelemetry
import kotlin.math.hypot

@Composable
fun FloatingDirectorOverlayContent(
    telemetry: SceneTelemetry?,
    currentPitch: Float,
    currentRoll: Float,
    onAutoDialRequested: (S23ProRecipe) -> Unit,
    onCloseService: () -> Unit
) {
    val recipe = telemetry?.recipe
    val targetPitch = recipe?.targetPitchDeg ?: 0f
    val pitchDelta = currentPitch - targetPitch
    val rollDelta = currentRoll
    val isLocked = hypot(pitchDelta, rollDelta) < 2.5f

    val accentColor = if (isLocked) Color(0xFF10B981) else Color(0xFFF59E0B)

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. Central 3D Gyro Angle Reticle
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Target Fixed Ring
            drawCircle(
                color = accentColor,
                radius = 70f,
                center = center,
                style = Stroke(width = if (isLocked) 6f else 4f)
            )

            // Level Line
            drawLine(
                color = if (Math.abs(rollDelta) < 1.0f) Color(0xFF10B981) else Color(0xFFF59E0B),
                start = Offset(center.x - 120f, center.y),
                end = Offset(center.x - 75f, center.y),
                strokeWidth = 4f
            )
            drawLine(
                color = if (Math.abs(rollDelta) < 1.0f) Color(0xFF10B981) else Color(0xFFF59E0B),
                start = Offset(center.x + 75f, center.y),
                end = Offset(center.x + 120f, center.y),
                strokeWidth = 4f
            )

            // Moving Gyro Aim Bubble
            val bubbleOffset = Offset(
                x = center.x + (rollDelta * 14f).coerceIn(-180f, 180f),
                y = center.y - (pitchDelta * 14f).coerceIn(-180f, 180f)
            )

            drawCircle(
                color = accentColor.copy(alpha = 0.4f),
                radius = 35f,
                center = bubbleOffset
            )
            drawCircle(
                color = if (isLocked) Color(0xFF10B981) else Color.White,
                radius = 8f,
                center = bubbleOffset
            )
        }

        // 2. Top Status Pill (Scene Badge + Coaching Line)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC0B0F17),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = recipe?.sceneCategory ?: "Analyzing Viewfinder...",
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = recipe?.coachingPrompt ?: "Point camera at subject",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = onCloseService,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Overlay",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 3. Bottom Auto-Dial Recipe Bar
        if (recipe != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xE60B0F17),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProDialReadout("LENS", recipe.recommendedLens)
                        ProDialReadout("ISO", recipe.iso.toString())
                        ProDialReadout("SPEED", recipe.shutterSpeed)
                        ProDialReadout("EV", "${recipe.evComp}")
                        ProDialReadout("LUT", recipe.lutName)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onAutoDialRequested(recipe) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto-Dial into Samsung Pro Camera",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProDialReadout(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
