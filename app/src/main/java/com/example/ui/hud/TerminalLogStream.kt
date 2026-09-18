package com.example.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NousCyanGlow
import com.example.ui.theme.NousCyanNeon
import com.example.ui.theme.NousSurfaceDark
import com.example.ui.theme.NousTextPrimary

@Composable
fun TerminalLogStream(
    logs: List<String>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NousSurfaceDark)
            .border(1.dp, NousCyanNeon.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("terminal_log_stream")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TELEMETRY AUDIT STREAM",
                color = NousCyanNeon,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "LIVE // L0-L4",
                color = NousCyanGlow,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        color = when {
                            log.contains("[SECURITY]") || log.contains("[INTERLOCK]") -> com.example.ui.theme.NousAmberWarning
                            log.contains("[ERROR]") || log.contains("[FAIL]") -> com.example.ui.theme.NousRedAlert
                            log.contains("[VERIFIED]") || log.contains("[COMPLETE]") -> com.example.ui.theme.NousGreenVerified
                            log.contains("[PLANNER]") || log.contains("[EXEC]") -> NousCyanGlow
                            else -> NousTextPrimary.copy(alpha = 0.85f)
                        },
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}
