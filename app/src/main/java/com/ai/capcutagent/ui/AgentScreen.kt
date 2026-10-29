package com.ai.capcutagent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.capcutagent.core.AgentState

@Composable
fun AgentScreen(
    uiState: AgentUiState,
    onStartStopClick: () -> Unit,
    onModeChange: (OperatingMode) -> Unit,
    onTrainClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val isRunning = uiState.agentState != AgentState.IDLE && 
                    uiState.agentState != AgentState.STOPPED && 
                    uiState.agentState != AgentState.COMPLETED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
            .padding(24.dp)
            .testTag("agent_screen_root"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // टॉप बार
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CapCut AI Agent",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("agent_settings_button")
            ) {
                Text(text = "⚙️", fontSize = 20.sp)
            }
        }

        // मोड सिलेक्टर (Online / Offline / Auto)
        Row(
            modifier = Modifier
                .background(Color(0xFF1E222D), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OperatingMode.values().forEach { mode ->
                val selected = uiState.mode == mode
                Button(
                    onClick = { onModeChange(mode) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF2563EB) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("mode_button_${mode.name.lowercase()}")
                ) {
                    Text(text = mode.name, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // स्टेटस कार्ड
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("agent_status_card_view"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181C26)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "करंट स्टेटस",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = uiState.statusMessage,
                    color = Color(0xFF38BDF8),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "State: ${uiState.agentState.name}",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }

        // मुख्य एक्शन बटन्स
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onStartStopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("agent_start_stop_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFDC2626) else Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isRunning) "STOP EDITING" else "START EDITING",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onTrainClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("agent_train_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "मुझे और ट्रेन करो", color = Color(0xFFE2E8F0))
            }
        }
    }
}
