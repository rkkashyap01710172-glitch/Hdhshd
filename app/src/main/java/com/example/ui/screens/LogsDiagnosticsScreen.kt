package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.capcutagent.automation.AutomationLog
import com.ai.capcutagent.automation.CapCutAutomationService
import com.ai.capcutagent.automation.LogLevel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsDiagnosticsScreen(
    logs: List<AutomationLog>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
    var showCalibrator by remember { mutableStateOf(false) }

    // Calibrator touch state
    var touchPoint by remember { mutableStateOf<Offset?>(null) }
    var touchRatio by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == null) logs
        else logs.filter { it.level == selectedFilter }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Live Automation Console",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${filteredLogs.size} events logged",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showCalibrator = !showCalibrator },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (showCalibrator) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Calibrate Coordinates",
                        tint = if (showCalibrator) NeonCyan else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear Logs",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Touch Coordinate Calibrator Drawer
        if (showCalibrator) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("coordinate_calibrator_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Coordinate Ratio Calibrator",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )

                        if (touchRatio != null) {
                            Text(
                                text = "xRatio: ${"%.2f".format(touchRatio!!.first)}, yRatio: ${"%.2f".format(touchRatio!!.second)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = EmeraldGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF090D14))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                            .onSizeChanged { containerSize = it }
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    touchPoint = offset
                                    if (containerSize.width > 0 && containerSize.height > 0) {
                                        val rx = (offset.x / containerSize.width).coerceIn(0f, 1f)
                                        val ry = (offset.y / containerSize.height).coerceIn(0f, 1f)
                                        touchRatio = rx to ry
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (touchPoint == null) {
                            Text(
                                text = "Tap or drag anywhere to measure touch ratio",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .offset(
                                        x = (touchPoint!!.x - 8).dp,
                                        y = (touchPoint!!.y - 8).dp
                                    )
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == LogLevel.ACTION,
                onClick = { selectedFilter = LogLevel.ACTION },
                label = { Text("Actions", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == LogLevel.SUCCESS,
                onClick = { selectedFilter = LogLevel.SUCCESS },
                label = { Text("Success", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == LogLevel.ERROR,
                onClick = { selectedFilter = LogLevel.ERROR },
                label = { Text("Errors", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Terminal Output Window
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF080C14),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (filteredLogs.isEmpty()) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Console is empty.\nRun an automation macro to view real-time accessibility logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val (textColor, levelBadge) = when (log.level) {
                            LogLevel.SUCCESS -> EmeraldGlow to "[OK]"
                            LogLevel.ERROR -> SunsetRose to "[ERR]"
                            LogLevel.WARNING -> AmberGlow to "[WARN]"
                            LogLevel.ACTION -> NeonCyan to "[ACT]"
                            LogLevel.INFO -> TextSecondary to "[INFO]"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = timeFormat.format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = TextTertiary,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = levelBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${log.tag}: ${log.message}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
