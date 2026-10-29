package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.capcutagent.automation.ExecutionState
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SunsetRose

@Composable
fun ExecutionBanner(
    executionState: ExecutionState,
    onStopClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = executionState !is ExecutionState.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val (bgColor, borderColor) = when (executionState) {
            is ExecutionState.Running -> Color(0xFF0C2A3D) to NeonCyan
            is ExecutionState.Completed -> Color(0xFF063321) to EmeraldGlow
            is ExecutionState.Failed -> Color(0xFF3B0B14) to SunsetRose
            is ExecutionState.Paused -> Color(0xFF332008) to Color(0xFFF59E0B)
            else -> Color(0xFF1E293B) to Color(0xFF475569)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("execution_banner"),
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    when (executionState) {
                        is ExecutionState.Running -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = NeonCyan,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Executing Step ${executionState.stepIndex + 1}/${executionState.totalSteps}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = executionState.stepTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }

                        is ExecutionState.Completed -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = EmeraldGlow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Workflow Completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGlow,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${executionState.totalStepsExecuted} steps executed in ${executionState.durationMs / 1000f}s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }

                        is ExecutionState.Failed -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Failed",
                                tint = SunsetRose,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Failed at Step ${executionState.stepIndex + 1}: ${executionState.stepTitle}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SunsetRose,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = executionState.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFD1D8),
                                    maxLines = 1
                                )
                            }
                        }

                        is ExecutionState.Paused -> {
                            Text(
                                text = "Paused",
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        else -> {}
                    }
                }

                if (executionState is ExecutionState.Running) {
                    FilledTonalButton(
                        onClick = onStopClicked,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SunsetRose.copy(alpha = 0.2f),
                            contentColor = SunsetRose
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("stop_workflow_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
