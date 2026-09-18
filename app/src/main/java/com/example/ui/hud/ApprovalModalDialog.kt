package com.example.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.model.ApprovalRequest
import com.example.ui.theme.NousAmberWarning
import com.example.ui.theme.NousCyanGlow
import com.example.ui.theme.NousCyanNeon
import com.example.ui.theme.NousObsidianDark
import com.example.ui.theme.NousRedAlert
import com.example.ui.theme.NousSurfaceDark
import com.example.ui.theme.NousSurfaceVariant
import com.example.ui.theme.NousTextPrimary
import com.example.ui.theme.NousTextSecondary

@Composable
fun ApprovalModalDialog(
    request: ApprovalRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, NousAmberWarning, RoundedCornerShape(16.dp))
            .background(NousSurfaceDark)
            .testTag("security_approval_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Security Alert",
                    tint = NousAmberWarning
                )
                Text(
                    text = "SECURITY INTERLOCK: ${request.permissionLevel.name}",
                    color = NousAmberWarning,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "An autonomous agent has proposed a privileged action requiring explicit human consent.",
                    color = NousTextSecondary,
                    fontSize = 12.sp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NousSurfaceVariant)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "TARGET TOOL: ${request.toolName}",
                        color = NousCyanNeon,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = request.summary,
                        color = NousTextPrimary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "RISK ANALYSIS: ${request.riskWarning}",
                        color = NousAmberWarning,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApprove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NousCyanNeon,
                    contentColor = NousObsidianDark
                ),
                modifier = Modifier.testTag("approve_button")
            ) {
                Text(
                    text = "AUTHORIZE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NousRedAlert
                ),
                modifier = Modifier.testTag("reject_button")
            ) {
                Text(
                    text = "ABORT ACTION",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = NousSurfaceDark
    )
}
