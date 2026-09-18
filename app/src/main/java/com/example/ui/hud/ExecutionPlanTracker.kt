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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.core.model.ExecutionPlan
import com.example.core.model.PlanStep
import com.example.core.model.StepStatus
import com.example.ui.theme.NousAmberWarning
import com.example.ui.theme.NousCyanGlow
import com.example.ui.theme.NousCyanNeon
import com.example.ui.theme.NousGreenVerified
import com.example.ui.theme.NousRedAlert
import com.example.ui.theme.NousSurfaceDark
import com.example.ui.theme.NousSurfaceVariant
import com.example.ui.theme.NousTextPrimary
import com.example.ui.theme.NousTextSecondary

@Composable
fun ExecutionPlanTracker(
    plan: ExecutionPlan?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NousSurfaceDark)
            .border(1.dp, NousCyanNeon.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("execution_plan_tracker")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ACTIVE EXECUTION PLAN",
                color = NousCyanNeon,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (plan != null) {
                Text(
                    text = "${plan.steps.size} STEPS",
                    color = NousCyanGlow,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (plan == null || plan.steps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Awaiting user command or goal input...",
                    color = NousTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                plan.steps.forEachIndexed { index, step ->
                    StepItemRow(index = index + 1, step = step)
                }
            }
        }
    }
}

@Composable
private fun StepItemRow(index: Int, step: PlanStep) {
    val (statusColor, badgeLabel) = when (step.status) {
        is StepStatus.Pending -> Pair(NousTextSecondary, "PENDING")
        is StepStatus.WaitingApproval -> Pair(NousAmberWarning, "APPROVAL REQUIRED")
        is StepStatus.InProgress -> Pair(NousCyanNeon, "RUNNING")
        is StepStatus.Success -> Pair(NousGreenVerified, "VERIFIED")
        is StepStatus.Failure -> Pair(NousRedAlert, "FAILED")
        is StepStatus.Skipped -> Pair(NousTextSecondary, "SKIPPED")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NousSurfaceVariant)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon Indicator
            when (step.status) {
                is StepStatus.InProgress -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = NousCyanNeon,
                        strokeWidth = 2.dp
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f))
                            .border(1.dp, statusColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString(),
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = step.description,
                color = NousTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = badgeLabel,
                color = statusColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Verification or Output Evidence Display
        if (step.outputResult != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "➔ ${step.outputResult.take(160)}",
                color = if (step.status is StepStatus.Success) NousGreenVerified else NousTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}
