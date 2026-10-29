package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ai.capcutagent.automation.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMacroDialog(
    onDismiss: () -> Unit,
    onSaveWorkflow: (Workflow) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Custom") }

    val steps = remember { mutableStateListOf<AutomationStep>() }

    var showAddStepSheet by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Create Automation Macro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Macro Name") },
                    placeholder = { Text("e.g., Fast Clip Cutter") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("macro_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("What this workflow automates in CapCut") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Steps (${steps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    FilledTonalButton(
                        onClick = { showAddStepSheet = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = NeonCyan.copy(alpha = 0.2f),
                            contentColor = NeonCyan
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("add_step_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Step", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (steps.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "No steps added yet.\nTap '+ Add Step' to define actions like clicking text or swiping timeline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(steps) { index, step ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = step.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${step.action.type} • ${step.delayAfterMs}ms delay",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = { steps.removeAt(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = SunsetRose,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val workflow = Workflow(
                                    title = title.trim(),
                                    description = description.ifBlank { "Custom macro with ${steps.size} steps" },
                                    category = category,
                                    steps = steps.toList(),
                                    isPreset = false
                                )
                                onSaveWorkflow(workflow)
                                onDismiss()
                            }
                        },
                        enabled = title.isNotBlank() && steps.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_macro_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyanDark)
                    ) {
                        Text("Save Macro")
                    }
                }
            }
        }
    }

    if (showAddStepSheet) {
        AddStepDialog(
            onDismiss = { showAddStepSheet = false },
            onAddStep = { newStep ->
                steps.add(newStep)
                showAddStepSheet = false
            }
        )
    }
}

@Composable
fun AddStepDialog(
    onDismiss: () -> Unit,
    onAddStep: (AutomationStep) -> Unit
) {
    var stepTitle by remember { mutableStateOf("") }
    var selectedActionType by remember { mutableStateOf(ActionType.CLICK_TEXT) }
    var targetText by remember { mutableStateOf("Split") }
    var delayAfterMs by remember { mutableStateOf(600L) }
    var xRatio by remember { mutableStateOf(0.5f) }
    var yRatio by remember { mutableStateOf(0.65f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add Action Step",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = stepTitle,
                    onValueChange = { stepTitle = it },
                    label = { Text("Step Title") },
                    placeholder = { Text("e.g. Tap Split Button") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Action Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedActionType == ActionType.CLICK_TEXT,
                        onClick = { selectedActionType = ActionType.CLICK_TEXT },
                        label = { Text("Click Text", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedActionType == ActionType.CLICK_COORDINATES,
                        onClick = { selectedActionType = ActionType.CLICK_COORDINATES },
                        label = { Text("Coordinates", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedActionType == ActionType.SWIPE_TIMELINE,
                        onClick = { selectedActionType = ActionType.SWIPE_TIMELINE },
                        label = { Text("Swipe", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedActionType) {
                    ActionType.CLICK_TEXT -> {
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { targetText = it },
                            label = { Text("Button / Element Text") },
                            placeholder = { Text("e.g. Split, Text, Export") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ActionType.CLICK_COORDINATES -> {
                        Column {
                            Text(
                                text = "Screen X/Y Ratio: (${(xRatio * 100).toInt()}%, ${(yRatio * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeonCyan
                            )
                            Slider(
                                value = xRatio,
                                onValueChange = { xRatio = it },
                                valueRange = 0f..1f
                            )
                            Slider(
                                value = yRatio,
                                onValueChange = { yRatio = it },
                                valueRange = 0f..1f
                            )
                        }
                    }

                    ActionType.SWIPE_TIMELINE -> {
                        Text(
                            text = "Swipes horizontally along timeline track to scroll playhead forward",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    else -> {}
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Delay after action: ${delayAfterMs}ms",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Slider(
                    value = delayAfterMs.toFloat(),
                    onValueChange = { delayAfterMs = it.toLong() },
                    valueRange = 200f..3000f,
                    steps = 14
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val action = when (selectedActionType) {
                                ActionType.CLICK_TEXT -> AutomationAction(
                                    type = ActionType.CLICK_TEXT,
                                    targetText = targetText.trim()
                                )
                                ActionType.CLICK_COORDINATES -> AutomationAction(
                                    type = ActionType.CLICK_COORDINATES,
                                    xRatio = xRatio,
                                    yRatio = yRatio
                                )
                                ActionType.SWIPE_TIMELINE -> AutomationAction(
                                    type = ActionType.SWIPE_TIMELINE,
                                    startXRatio = 0.65f,
                                    startYRatio = 0.65f,
                                    endXRatio = 0.35f,
                                    endYRatio = 0.65f,
                                    durationMs = 300
                                )
                                else -> AutomationAction(type = selectedActionType)
                            }

                            val step = AutomationStep(
                                title = stepTitle.ifBlank { "Action ${selectedActionType.name}" },
                                description = "Trigger $selectedActionType",
                                action = action,
                                delayAfterMs = delayAfterMs
                            )
                            onAddStep(step)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyanDark)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}
