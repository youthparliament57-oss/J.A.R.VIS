package com.example.ui.hud

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.perception.voice.VoiceEngineStatus
import com.example.ui.theme.NousAmberWarning
import com.example.ui.theme.NousCyanGlow
import com.example.ui.theme.NousCyanNeon
import com.example.ui.theme.NousObsidianDark
import com.example.ui.theme.NousRedAlert
import com.example.ui.theme.NousSurfaceDark
import com.example.ui.theme.NousTextPrimary
import com.example.ui.theme.NousTextSecondary

@Composable
fun HudActionBar(
    onCommandSubmit: (String) -> Unit,
    onEmergencyStop: () -> Unit,
    voiceStatus: VoiceEngineStatus,
    isSpeaking: Boolean,
    onStartVoiceInput: () -> Unit,
    onStopVoiceInput: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_pulse_alpha"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Preset Fast Telemetry & Tool Directives
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickCommandButton(
                label = "BATTERY / SYS",
                onClick = { onCommandSubmit("Check system telemetry and battery status") }
            )
            QuickCommandButton(
                label = "INSPECT ROOM",
                onClick = { onCommandSubmit("Inspect camera visual field") }
            )
            QuickCommandButton(
                label = "MATH: 4096 * 16",
                onClick = { onCommandSubmit("Calculate 4096 * 16") }
            )
            QuickCommandButton(
                label = "DIAGNOSTIC",
                onClick = { onCommandSubmit("Run system diagnostic and full check") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Push-to-Talk / Continuous Mic Button
            IconButton(
                onClick = {
                    if (voiceStatus == VoiceEngineStatus.LISTENING) {
                        onStopVoiceInput()
                    } else {
                        onStartVoiceInput()
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (voiceStatus == VoiceEngineStatus.LISTENING) NousCyanNeon.copy(alpha = micAlpha)
                        else NousSurfaceDark
                    )
                    .border(
                        1.dp,
                        if (voiceStatus == VoiceEngineStatus.LISTENING) NousCyanNeon else NousCyanNeon.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    )
                    .testTag("voice_input_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input Toggle",
                    tint = if (voiceStatus == VoiceEngineStatus.LISTENING) NousObsidianDark else NousCyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Interrupt TTS Button (Barge-in affordance)
            if (isSpeaking) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onStopSpeaking,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NousAmberWarning.copy(alpha = 0.2f))
                        .border(1.dp, NousAmberWarning, RoundedCornerShape(10.dp))
                        .testTag("stop_speech_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Stop Speech",
                        tint = NousAmberWarning,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                placeholder = {
                    Text(
                        text = if (voiceStatus == VoiceEngineStatus.LISTENING) "Listening to voice input..." else "Enter autonomous directive...",
                        color = NousTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        onCommandSubmit(inputText)
                        inputText = ""
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NousCyanNeon,
                    unfocusedBorderColor = NousCyanNeon.copy(alpha = 0.3f),
                    focusedTextColor = NousTextPrimary,
                    unfocusedTextColor = NousTextPrimary,
                    cursorColor = NousCyanNeon
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Submit Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onCommandSubmit(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NousCyanNeon)
                    .testTag("submit_command_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Transmit Command",
                    tint = NousObsidianDark
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Emergency Stop Button
            IconButton(
                onClick = onEmergencyStop,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NousRedAlert.copy(alpha = 0.2f))
                    .border(1.dp, NousRedAlert, RoundedCornerShape(10.dp))
                    .testTag("emergency_stop_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Emergency Stop",
                    tint = NousRedAlert
                )
            }
        }
    }
}

@Composable
private fun QuickCommandButton(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = NousSurfaceDark,
            contentColor = NousCyanGlow
        ),
        modifier = Modifier
            .border(1.dp, NousCyanNeon.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
        shape = RoundedCornerShape(6.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
