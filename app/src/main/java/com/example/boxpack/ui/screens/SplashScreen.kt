package com.example.boxpack.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val flapAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Subtle box opening animation over ~1.1 seconds
        flapAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
        )
        delay(100)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated 3D Isometric Cardboard Box
            Canvas(modifier = Modifier.size(140.dp)) {
                val progress = flapAnim.value
                val w = size.width
                val h = size.height

                val boxColor = Color(0xFFD97706)
                val topColor = Color(0xFFFEF3C7)
                val sideColor = Color(0xFFB45309)
                val flapColor = Color(0xFFFBBF24)

                val centerX = w / 2f
                val centerY = h / 2f + 10f

                // Base Box front
                val bW = 60f
                val bH = 50f
                val bD = 35f

                // Front Face
                val frontPath = Path().apply {
                    moveTo(centerX - bW, centerY)
                    lineTo(centerX, centerY + 20f)
                    lineTo(centerX, centerY + 20f + bH)
                    lineTo(centerX - bW, centerY + bH)
                    close()
                }
                drawPath(frontPath, boxColor, style = Fill)
                drawPath(frontPath, Color(0xFF78350F), style = Stroke(width = 2.5f))

                // Right Face
                val rightPath = Path().apply {
                    moveTo(centerX, centerY + 20f)
                    lineTo(centerX + bW, centerY)
                    lineTo(centerX + bW, centerY + bH)
                    lineTo(centerX, centerY + 20f + bH)
                    close()
                }
                drawPath(rightPath, sideColor, style = Fill)
                drawPath(rightPath, Color(0xFF78350F), style = Stroke(width = 2.5f))

                // Left Flap (opening upward based on progress)
                val flapAngle = progress * 35f
                val leftFlapPath = Path().apply {
                    moveTo(centerX - bW, centerY)
                    lineTo(centerX, centerY + 20f)
                    lineTo(centerX - 10f, centerY - 15f - flapAngle)
                    lineTo(centerX - bW - 10f, centerY - 25f - flapAngle)
                    close()
                }
                drawPath(leftFlapPath, flapColor, style = Fill)
                drawPath(leftFlapPath, Color(0xFF78350F), style = Stroke(width = 2f))

                // Right Flap (opening upward based on progress)
                val rightFlapPath = Path().apply {
                    moveTo(centerX, centerY + 20f)
                    lineTo(centerX + bW, centerY)
                    lineTo(centerX + bW + 10f, centerY - 25f - flapAngle)
                    lineTo(centerX + 10f, centerY - 15f - flapAngle)
                    close()
                }
                drawPath(rightFlapPath, topColor, style = Fill)
                drawPath(rightFlapPath, Color(0xFF78350F), style = Stroke(width = 2f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BoxPack",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Calculate how many boxes fit inside a container.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
