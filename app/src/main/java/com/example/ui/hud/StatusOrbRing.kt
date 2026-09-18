package com.example.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AgentState
import com.example.ui.theme.NousAmberWarning
import com.example.ui.theme.NousCyanGlow
import com.example.ui.theme.NousCyanNeon
import com.example.ui.theme.NousGreenVerified
import com.example.ui.theme.NousRedAlert

@Composable
fun StatusOrbRing(
    agentState: AgentState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val stateColor = when (agentState) {
        AgentState.IDLE -> NousCyanNeon
        AgentState.LISTENING -> NousCyanGlow
        AgentState.REASONING, AgentState.PLANNING -> NousCyanNeon
        AgentState.AWAITING_USER_APPROVAL -> NousAmberWarning
        AgentState.EXECUTING -> Color(0xFF60A5FA)
        AgentState.VERIFYING -> NousGreenVerified
        AgentState.ERROR, AgentState.EMERGENCY_STOPPED -> NousRedAlert
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val center = this.center
                val radius = size.minDimension / 2.2f

                // Outer rotating segmented reticle
                rotate(rotation) {
                    drawArc(
                        color = stateColor.copy(alpha = 0.4f),
                        startAngle = 0f,
                        sweepAngle = 70f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = stateColor.copy(alpha = 0.4f),
                        startAngle = 120f,
                        sweepAngle = 70f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = stateColor.copy(alpha = 0.4f),
                        startAngle = 240f,
                        sweepAngle = 70f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner counter-rotating ring
                rotate(-rotation * 1.5f) {
                    drawArc(
                        color = stateColor.copy(alpha = 0.7f),
                        startAngle = 45f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = stateColor.copy(alpha = 0.7f),
                        startAngle = 225f,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Core pulsating AI Iris
                drawCircle(
                    color = stateColor.copy(alpha = 0.15f * pulse),
                    radius = radius * 0.6f * pulse
                )
                drawCircle(
                    color = stateColor,
                    radius = radius * 0.25f
                )
                drawCircle(
                    color = Color.White,
                    radius = radius * 0.12f
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = agentState.label,
            color = stateColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}
