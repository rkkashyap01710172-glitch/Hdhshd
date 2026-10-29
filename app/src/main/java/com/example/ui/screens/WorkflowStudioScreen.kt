package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.capcutagent.automation.*
import com.example.ui.theme.*

@Composable
fun WorkflowStudioScreen(
    presets: List<Workflow>,
    customWorkflows: List<Workflow>,
    onRunWorkflow: (Workflow) -> Unit,
    onTestInSimulator: (Workflow) -> Unit,
    onSaveCustomWorkflow: (Workflow) -> Unit,
    onDeleteCustomWorkflow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var expandedWorkflowId by remember { mutableStateOf<String?>(null) }

    // AI Generator prompt state
    var aiPrompt by remember { mutableStateOf("") }
    var aiSuccessMessage by remember { mutableStateOf<String?>(null) }

    val allWorkflows = remember(presets, customWorkflows) {
        presets + customWorkflows
    }

    val categories = listOf("All", "Cutting", "Captions", "Cleanup", "Formatting", "Export", "Custom")

    val filteredWorkflows = remember(allWorkflows, selectedCategory) {
        if (selectedCategory == "All") allWorkflows
        else allWorkflows.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = NeonCyan,
                contentColor = Color(0xFF003548),
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 72.dp)
                    .testTag("create_macro_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Create Macro")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
        ) {
            // AI Assistant Generation Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_assistant_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = ElectricViolet.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = ElectricViolet,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI Macro Generator",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Describe your editing workflow in natural language",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            placeholder = { Text("e.g. Cut clip every 3s, add captions, and export in 4K") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_prompt_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricViolet,
                                unfocusedBorderColor = DarkCardBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick prompt suggestions
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                listOf(
                                    "Jump cut beat splitter",
                                    "Auto subtitle + style",
                                    "Delete CapCut ending logo",
                                    "Batch 4K export"
                                )
                            ) { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkSurfaceVariant,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                                    onClick = { aiPrompt = suggestion }
                                ) {
                                    Text(
                                        text = "+ $suggestion",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (aiPrompt.isNotBlank()) {
                                    val generated = generateWorkflowFromPrompt(aiPrompt)
                                    onSaveCustomWorkflow(generated)
                                    aiSuccessMessage = "Generated macro '${generated.title}' with ${generated.steps.size} steps!"
                                    aiPrompt = ""
                                }
                            },
                            enabled = aiPrompt.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_macro_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Macro Steps", fontWeight = FontWeight.Bold)
                        }

                        if (aiSuccessMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiSuccessMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldGlow,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }
            }

            // Workflows Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Workflows (${filteredWorkflows.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "${filteredWorkflows.count { it.isPreset }} Presets • ${filteredWorkflows.count { !it.isPreset }} Custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Workflows List
            items(filteredWorkflows, key = { it.id }) { workflow ->
                val isExpanded = expandedWorkflowId == workflow.id
                val icon = getIconForWorkflow(workflow.iconKey)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workflow_card_${workflow.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isExpanded) NeonCyan.copy(alpha = 0.6f) else DarkCardBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (workflow.isPreset) NeonCyanDark.copy(alpha = 0.2f) else ElectricViolet.copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (workflow.isPreset) NeonCyan else ElectricViolet,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = workflow.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (!workflow.isPreset) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ElectricViolet.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "CUSTOM",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricViolet,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = workflow.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = if (isExpanded) Int.MAX_VALUE else 2
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onRunWorkflow(workflow) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyanDark),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("run_workflow_button_${workflow.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            FilledTonalButton(
                                onClick = { onTestInSimulator(workflow) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = DarkSurfaceVariant,
                                    contentColor = NeonCyan
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_sim_button_${workflow.id}")
                            ) {
                                Text("Simulate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            IconButton(
                                onClick = {
                                    expandedWorkflowId = if (isExpanded) null else workflow.id
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle Details",
                                    tint = TextSecondary
                                )
                            }

                            if (!workflow.isPreset) {
                                IconButton(
                                    onClick = { onDeleteCustomWorkflow(workflow.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = SunsetRose
                                    )
                                }
                            }
                        }

                        // Expanded Steps View
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HorizontalDivider(color = DarkCardBorder)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Step Sequence (${workflow.steps.size} actions):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )

                                workflow.steps.forEachIndexed { index, step ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyan
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = step.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "${step.action.type} • +${step.delayAfterMs}ms",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateMacroDialog(
            onDismiss = { showCreateDialog = false },
            onSaveWorkflow = { workflow ->
                onSaveCustomWorkflow(workflow)
                showCreateDialog = false
            }
        )
    }
}

private fun getIconForWorkflow(key: String): ImageVector = when (key) {
    "scissors" -> Icons.Default.ContentCut
    "subtitles" -> Icons.Default.Subtitles
    "delete" -> Icons.Default.DeleteSweep
    "aspect_ratio" -> Icons.Default.AspectRatio
    "cloud_upload" -> Icons.Default.CloudUpload
    "zoom_in" -> Icons.Default.ZoomIn
    else -> Icons.Default.SmartToy
}

private fun generateWorkflowFromPrompt(prompt: String): Workflow {
    val lower = prompt.lowercase()
    val steps = mutableListOf<AutomationStep>()

    if (lower.contains("cut") || lower.contains("split") || lower.contains("beat")) {
        steps.add(
            AutomationStep(
                title = "Tap Video Playhead",
                description = "Position playhead on clip",
                action = AutomationAction(type = ActionType.CLICK_COORDINATES, xRatio = 0.5f, yRatio = 0.65f),
                delayAfterMs = 500
            )
        )
        steps.add(
            AutomationStep(
                title = "Click 'Split'",
                description = "Perform split at playhead",
                action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Split"),
                delayAfterMs = 600
            )
        )
        steps.add(
            AutomationStep(
                title = "Scrub Timeline",
                description = "Advance playhead 2.5s",
                action = AutomationAction(type = ActionType.SWIPE_TIMELINE, startXRatio = 0.65f, startYRatio = 0.65f, endXRatio = 0.4f, endYRatio = 0.65f),
                delayAfterMs = 500
            )
        )
        steps.add(
            AutomationStep(
                title = "Click 'Split' Segment 2",
                description = "Perform second split",
                action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Split"),
                delayAfterMs = 500
            )
        )
    }

    if (lower.contains("caption") || lower.contains("subtitle") || lower.contains("text")) {
        steps.add(
            AutomationStep(
                title = "Select 'Text'",
                description = "Open text panel",
                action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Text"),
                delayAfterMs = 800
            )
        )
        steps.add(
            AutomationStep(
                title = "Tap 'Auto captions'",
                description = "Open speech-to-text",
                action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Auto captions"),
                delayAfterMs = 1200
            )
        )
        steps.add(
            AutomationStep(
                title = "Confirm 'Generate'",
                description = "Start captions processing",
                action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Generate"),
                delayAfterMs = 2000
            )
        )
    }

    if (lower.contains("watermark") || lower.contains("outro") || lower.contains("ending") || lower.contains("delete")) {
        steps.add(
            AutomationStep(
                title = "Scroll to Video Ending",
                description = "Fast forward to last clip",
                action = AutomationAction(type = ActionType.SWIPE_TIMELINE, startXRatio = 0.2f, startYRatio = 0.65f, endXRatio = 0.9f, endYRatio = 0.65f),
                delayAfterMs = 700
            )
        )
        steps.add(
            AutomationStep(
                title = "Select Ending Logo",
                description = "Tap CapCut watermark clip",
                action = AutomationAction(type = ActionType.CLICK_COORDINATES, xRatio = 0.78f, yRatio = 0.65f),
                delayAfterMs = 500
            )
        )
        steps.add(
            AutomationStep(
                title = "Click 'Delete'",
                description = "Remove watermark outro",
                action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Delete"),
                delayAfterMs = 500
            )
        )
    }

    if (lower.contains("export") || lower.contains("4k") || lower.contains("render")) {
        steps.add(
            AutomationStep(
                title = "Tap Quality Dropdown",
                description = "Open 4K/60fps selector",
                action = AutomationAction(type = ActionType.CLICK_COORDINATES, xRatio = 0.8f, yRatio = 0.06f),
                delayAfterMs = 600
            )
        )
        steps.add(
            AutomationStep(
                title = "Click 'Export'",
                description = "Render video file",
                action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Export"),
                delayAfterMs = 1000
            )
        )
    }

    if (steps.isEmpty()) {
        // Fallback generic split & text
        steps.add(
            AutomationStep(
                title = "Tap Playhead",
                description = "Select active timeline layer",
                action = AutomationAction(type = ActionType.CLICK_COORDINATES, xRatio = 0.5f, yRatio = 0.65f),
                delayAfterMs = 500
            )
        )
        steps.add(
            AutomationStep(
                title = "Click 'Edit'",
                description = "Open editing sub-bar",
                action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Edit"),
                delayAfterMs = 600
            )
        )
    }

    val title = prompt.take(30).trim().replaceFirstChar { it.uppercase() }
    return Workflow(
        title = "AI: $title",
        description = "Synthesized from: '$prompt'",
        category = "Custom",
        steps = steps,
        isPreset = false
    )
}
