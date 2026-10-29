package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.capcutagent.core.*
import com.example.ui.theme.*

@Composable
fun AgentStatusCard(
    telemetry: AgentTelemetry,
    onStopClicked: () -> Unit,
    onResetClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = telemetry.state
    val stateColor = getStateColor(state)
    val stateIcon = getStateIcon(state)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("agent_status_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (state.isActive) stateColor.copy(alpha = 0.8f) else DarkCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with State Pill & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing Status Orb
                    Box(contentAlignment = Alignment.Center) {
                        if (state.isActive) {
                            Box(
                                modifier = Modifier
                                    .size((34 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(stateColor.copy(alpha = 0.2f))
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = stateColor.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, stateColor),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = stateIcon,
                                    contentDescription = null,
                                    tint = stateColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AGENT: ${state.name}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = stateColor,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = state.getDisplayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Control action
                if (state.isActive) {
                    FilledTonalButton(
                        onClick = onStopClicked,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SunsetRose.copy(alpha = 0.2f),
                            contentColor = SunsetRose
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("agent_stop_button")
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (state == AgentState.COMPLETED || state == AgentState.FAILED || state == AgentState.STOPPED) {
                    IconButton(
                        onClick = onResetClicked,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // State Description & Active Step
            Text(
                text = telemetry.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (state == AgentState.FAILED) SunsetRose else TextPrimary,
                lineHeight = 18.sp
            )

            if (telemetry.activeStepName.isNotBlank() && state.isActive) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTION:",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = telemetry.activeStepName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lifecycle Pipeline Stages
            AgentLifecycleBreadcrumbs(currentState = state)

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            if (state.isActive || telemetry.progress > 0f) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Phase Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${(telemetry.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = stateColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { telemetry.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = stateColor,
                        trackColor = DarkSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Bottom Telemetry Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryChip(
                    label = "CONFIDENCE",
                    value = "${telemetry.confidenceScore}%",
                    tint = EmeraldGlow
                )
                TelemetryChip(
                    label = "ACTIONS",
                    value = "${telemetry.totalActionsCompleted}",
                    tint = NeonCyan
                )
                if (telemetry.recoveryAttempts > 0) {
                    TelemetryChip(
                        label = "RECOVERIES",
                        value = "${telemetry.recoveryAttempts}",
                        tint = AmberGlow
                    )
                }
            }
        }
    }
}

@Composable
fun AgentLifecycleBreadcrumbs(currentState: AgentState) {
    val stages = listOf(
        "Init" to (currentState in listOf(AgentState.INITIALIZING)),
        "Scan" to (currentState in listOf(AgentState.ANALYZING_MEDIA)),
        "Plan" to (currentState in listOf(AgentState.BUILDING_EDIT_PLAN)),
        "Edit" to (currentState in listOf(AgentState.APPLYING_EDITS, AgentState.RECOVERING)),
        "Verify" to (currentState in listOf(AgentState.VERIFYING_EDITS)),
        "Render" to (currentState in listOf(AgentState.READY_FOR_EXPORT, AgentState.EXPORTING, AgentState.COMPLETED))
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        stages.forEachIndexed { index, (label, isCurrent) ->
            val isPassed = isStagePassed(label, currentState)
            val chipColor = when {
                isCurrent -> NeonCyan
                isPassed -> EmeraldGlow
                else -> TextTertiary
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isCurrent) NeonCyan.copy(alpha = 0.2f) else if (isPassed) EmeraldGlow.copy(alpha = 0.1f) else DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isCurrent) 1.dp else 0.5.dp,
                    color = chipColor.copy(alpha = if (isCurrent || isPassed) 0.6f else 0.2f)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = chipColor,
                        fontSize = 9.sp,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun isStagePassed(stage: String, currentState: AgentState): Boolean = when (stage) {
    "Init" -> currentState in listOf(AgentState.ANALYZING_MEDIA, AgentState.BUILDING_EDIT_PLAN, AgentState.APPLYING_EDITS, AgentState.VERIFYING_EDITS, AgentState.READY_FOR_EXPORT, AgentState.EXPORTING, AgentState.COMPLETED)
    "Scan" -> currentState in listOf(AgentState.BUILDING_EDIT_PLAN, AgentState.APPLYING_EDITS, AgentState.VERIFYING_EDITS, AgentState.READY_FOR_EXPORT, AgentState.EXPORTING, AgentState.COMPLETED)
    "Plan" -> currentState in listOf(AgentState.APPLYING_EDITS, AgentState.VERIFYING_EDITS, AgentState.READY_FOR_EXPORT, AgentState.EXPORTING, AgentState.COMPLETED)
    "Edit" -> currentState in listOf(AgentState.VERIFYING_EDITS, AgentState.READY_FOR_EXPORT, AgentState.EXPORTING, AgentState.COMPLETED)
    "Verify" -> currentState in listOf(AgentState.READY_FOR_EXPORT, AgentState.EXPORTING, AgentState.COMPLETED)
    "Render" -> currentState == AgentState.COMPLETED
    else -> false
}

@Composable
fun RowScope.TelemetryChip(label: String, value: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
        modifier = Modifier.weight(1f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = tint,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

fun getStateColor(state: AgentState): Color = when (state) {
    AgentState.IDLE -> TextSecondary
    AgentState.INITIALIZING -> NeonCyan
    AgentState.WAITING_FOR_USER -> AmberGlow
    AgentState.ANALYZING_MEDIA -> ElectricViolet
    AgentState.BUILDING_EDIT_PLAN -> Color(0xFFFBBF24)
    AgentState.APPLYING_EDITS -> NeonCyan
    AgentState.VERIFYING_EDITS -> EmeraldGlow
    AgentState.RECOVERING -> SunsetRose
    AgentState.READY_FOR_EXPORT -> Color(0xFFC084FC)
    AgentState.EXPORTING -> Color(0xFF38BDF8)
    AgentState.COMPLETED -> EmeraldGlow
    AgentState.STOPPED -> TextSecondary
    AgentState.FAILED -> SunsetRose
}

fun getStateIcon(state: AgentState): ImageVector = when (state) {
    AgentState.IDLE -> Icons.Default.Pause
    AgentState.INITIALIZING -> Icons.Default.Sync
    AgentState.WAITING_FOR_USER -> Icons.Default.Person
    AgentState.ANALYZING_MEDIA -> Icons.Default.Radar
    AgentState.BUILDING_EDIT_PLAN -> Icons.Default.AutoAwesome
    AgentState.APPLYING_EDITS -> Icons.Default.ContentCut
    AgentState.VERIFYING_EDITS -> Icons.Default.CheckCircleOutline
    AgentState.RECOVERING -> Icons.Default.Healing
    AgentState.READY_FOR_EXPORT -> Icons.Default.VideoCameraBack
    AgentState.EXPORTING -> Icons.Default.CloudUpload
    AgentState.COMPLETED -> Icons.Default.CheckCircle
    AgentState.STOPPED -> Icons.Default.Stop
    AgentState.FAILED -> Icons.Default.ErrorOutline
}
