package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ai.capcutagent.voice.HindiVoiceAgent
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantSheet(
    voiceAgent: HindiVoiceAgent?,
    lastRecognizedText: String?,
    lastAgentFeedback: String?,
    onExecuteVoiceCommand: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isListening = voiceAgent?.isListening?.collectAsState()?.value ?: false
    val rmsLevel = voiceAgent?.rmsLevel?.collectAsState()?.value ?: 0f

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            voiceAgent?.startListening()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseSize by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseSize"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = DarkCardBorder) },
        modifier = modifier.testTag("voice_assistant_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = SunsetRose.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = SunsetRose,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Hindi Voice Agent (हिंदी वॉइस)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "बोलकर CapCut वीडियो एडिट करें",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Mic Button
            Box(contentAlignment = Alignment.Center) {
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size((90 * pulseSize).dp)
                            .clip(CircleShape)
                            .background(SunsetRose.copy(alpha = 0.2f))
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isListening) SunsetRose else DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (isListening) SunsetRose else NeonCyan
                    ),
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    onClick = {
                        if (hasAudioPermission) {
                            if (isListening) {
                                voiceAgent?.stopListening()
                            } else {
                                voiceAgent?.speak("बोलिए, क्या एडिट करना है?")
                                voiceAgent?.startListening()
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "Microphone",
                            tint = if (isListening) Color.White else NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isListening) "सुन रहे हैं... (Listening...)" else "माइक दबाकर बोलें (Tap mic to speak)",
                style = MaterialTheme.typography.labelMedium,
                color = if (isListening) SunsetRose else TextSecondary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Recognized Speech Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "YOU SAID (सुनाई दिया):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = lastRecognizedText ?: "उदाहरण: 'कट लगाओ' या 'ऑटो कैप्शन बनाओ'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (lastRecognizedText != null) Color.White else TextTertiary,
                        fontWeight = if (lastRecognizedText != null) FontWeight.SemiBold else FontWeight.Normal
                    )

                    if (lastAgentFeedback != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = DarkCardBorder)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "AGENT (जवाब):",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lastAgentFeedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldGlow,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Voice Commands List
            Text(
                text = "त्वरित वॉइस कमांड (Quick Commands):",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val quickCommands = listOf(
                "कट लगाओ (Split)" to "कट लगाओ",
                "ऑटो कैप्शन (Captions)" to "ऑटो कैप्शन बनाओ",
                "वाटरमार्क हटाओ (Delete Logo)" to "वाटरमार्क हटाओ",
                "4K एक्सपोर्ट (Render 4K)" to "4K में एक्सपोर्ट करो",
                "9:16 रील्स (Shorts)" to "शॉर्ट्स 9:16 फॉर्मेट करो",
                "स्टॉप (Halt)" to "स्टॉप करो"
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickCommands) { (label, command) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                        onClick = {
                            onExecuteVoiceCommand(command)
                        }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
