package com.forge.os.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.os.presentation.screens.common.ModelPickerDialog
import com.forge.os.presentation.screens.common.ModelPickerRow
import com.forge.os.presentation.theme.LocalForgePalette

/**
 * Phase S — Settings → Model Routing editor.
 *
 * Lets the user edit `ModelRoutingConfig.fallbackChain` and the new
 * `backgroundUsesFallback` toggles without touching code.
 */
@Composable
fun ModelRoutingScreen(
    onBack: () -> Unit,
    viewModel: ModelRoutingViewModel = hiltViewModel(),
) {
    val palette = LocalForgePalette.current
    val state by viewModel.state.collectAsState()

    Column(
        Modifier.fillMaxSize().background(palette.bg).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = palette.textMuted, modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "🧭  MODEL ROUTING", color = palette.orange, fontSize = 16.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "Primary provider/model is chosen automatically from your saved keys. " +
                "When the primary fails, the agent walks this fallback chain top-to-bottom " +
                "until one provider succeeds.",
            color = palette.textMuted, fontSize = 11.sp,
            fontFamily = FontFamily.Monospace, lineHeight = 16.sp
        )

        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Text(
                    "FALLBACK CHAIN", color = palette.textMuted, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
                )
            }
            items(state.chain.size) { index ->
                val link = state.chain[index]
                ChainRow(
                    index = index,
                    total = state.chain.size,
                    provider = link.provider,
                    model = link.model,
                    hasKey = link.hasKey,
                    canDelete = state.chain.size > 1,
                    onUp = { viewModel.move(index, -1) },
                    onDown = { viewModel.move(index, +1) },
                    onRemove = { viewModel.remove(index) },
                    surface = palette.surface,
                    text = palette.textPrimary,
                    muted = palette.textMuted,
                    accent = palette.orange,
                )
            }
            item { AddRow(onAdd = { p, m -> viewModel.add(p, m) }, palette = palette) }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(
                    "BACKGROUND CALLERS", color = palette.textMuted, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
                )
            }
            item {
                BackgroundUsageCard(
                    cron = state.cronUsesFallback,
                    alarms = state.alarmsUseFallback,
                    subAgents = state.subAgentsUseFallback,
                    proactive = state.proactiveUsesFallback,
                    onChange = viewModel::setBackgroundUsage,
                    palette = palette,
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(
                    "SYSTEM OVERRIDES", color = palette.textMuted, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
                )
            }
            item {
                SystemOverridesCard(
                    summarizationProvider = state.summarizationProvider,
                    summarizationModel = state.summarizationModel,
                    systemProvider = state.systemProvider,
                    systemModel = state.systemModel,
                    companionProvider = state.companionProvider,
                    companionModel = state.companionModel,
                    plannerProvider = state.plannerProvider,
                    plannerModel = state.plannerModel,
                    visionProvider = state.visionProvider,
                    visionModel = state.visionModel,
                    reasoningProvider = state.reasoningProvider,
                    reasoningModel = state.reasoningModel,
                    reflectionProvider = state.reflectionProvider,
                    reflectionModel = state.reflectionModel,
                    onSave = viewModel::setSystemRouting,
                    availableModels = { viewModel.availableModels() },
                    palette = palette,
                )
            }
            
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(
                    "DYNAMIC TOKEN BUDGETING (ECO-MODE)", color = palette.textMuted, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
                )
            }
            item {
                EcoModeCard(
                    enabled = state.ecoModeEnabled,
                    limitUsd = state.dailyLimitUsd,
                    ecoProvider = state.ecoProvider,
                    ecoModel = state.ecoModel,
                    onSave = viewModel::setEcoMode,
                    availableModels = { viewModel.availableModels() },
                    palette = palette,
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(
                    "EMBEDDING MODEL (RAG)", color = palette.textMuted, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
                )
            }
            item {
                EmbeddingModelCard(
                    provider = state.embeddingProvider,
                    model = state.embeddingModel,
                    suggestions = state.availableEmbeddingSpecs,
                    isRefreshing = state.isRefreshing,
                    onProbe = viewModel::probeModels,
                    onSave = viewModel::setEmbeddingModel,
                    palette = palette,
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text(
                    "Tip — chain links highlighted in red have no saved key for that provider. " +
                        "Add the key in Settings → Built-in providers.",
                    color = palette.textMuted, fontSize = 10.sp, lineHeight = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ChainRow(
    index: Int,
    total: Int,
    provider: String,
    model: String,
    hasKey: Boolean,
    canDelete: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit,
    surface: Color,
    text: Color,
    muted: Color,
    accent: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${index + 1}.",
                color = if (hasKey) accent else Color(0xFFb91c1c),
                fontSize = 12.sp, fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    provider,
                    color = if (hasKey) text else Color(0xFFb91c1c),
                    fontSize = 12.sp, fontFamily = FontFamily.Monospace
                )
                Text(
                    model, color = muted, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!hasKey) {
                    Text(
                        "no key — link will be skipped",
                        color = Color(0xFFb91c1c), fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            IconButton(onClick = onUp, enabled = index > 0, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ArrowUpward, "Up", tint = muted, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onDown, enabled = index < total - 1, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ArrowDownward, "Down", tint = muted, modifier = Modifier.size(14.dp))
            }
            IconButton(onClick = onRemove, enabled = canDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "Remove", tint = muted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun AddRow(
    onAdd: (String, String) -> Unit,
    palette: com.forge.os.presentation.theme.ForgePalette,
) {
    var provider by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                "ADD LINK", color = palette.textMuted, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = provider, onValueChange = { provider = it.uppercase() },
                placeholder = { Text("PROVIDER (e.g. ANTHROPIC, GROQ, OPENAI)", fontSize = 10.sp) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = model, onValueChange = { model = it },
                placeholder = { Text("MODEL (e.g. claude-3-5-sonnet-latest)", fontSize = 10.sp) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    if (provider.isNotBlank() && model.isNotBlank()) {
                        onAdd(provider.trim(), model.trim()); provider = ""; model = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.orange),
                modifier = Modifier.fillMaxWidth()
            ) { Text("ADD", color = Color.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun BackgroundUsageCard(
    cron: Boolean,
    alarms: Boolean,
    subAgents: Boolean,
    proactive: Boolean,
    onChange: (BackgroundCaller, Boolean) -> Unit,
    palette: com.forge.os.presentation.theme.ForgePalette,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Which background callers share the chain?",
                color = palette.textPrimary, fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "ON = if the primary provider fails, the call walks the fallback chain. " +
                    "OFF = the call dies on the first provider error.",
                color = palette.textMuted, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, lineHeight = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF1f1f1f))
            Spacer(Modifier.height(8.dp))
            BgRow("Cron jobs",       cron)      { onChange(BackgroundCaller.CRON, it) }
            BgRow("Alarms",          alarms)    { onChange(BackgroundCaller.ALARMS, it) }
            BgRow("Sub-agents",      subAgents) { onChange(BackgroundCaller.SUB_AGENTS, it) }
            BgRow("Proactive turns", proactive) { onChange(BackgroundCaller.PROACTIVE, it) }
        }
    }
}

@Composable
private fun BgRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalForgePalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = value, onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = palette.orange)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = palette.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun EmbeddingModelCard(
    provider: String,
    model: String,
    suggestions: List<EmbeddingSpecUi>,
    isRefreshing: Boolean,
    onProbe: () -> Unit,
    onSave: (String, String) -> Unit,
    palette: com.forge.os.presentation.theme.ForgePalette,
) {
    var p by remember(provider) { mutableStateOf(provider) }
    var m by remember(model) { mutableStateOf(model) }
    val changed = p.trim().uppercase() != provider || m.trim() != model

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Used for semantic search (RAG) across all channels.",
                    modifier = Modifier.weight(1f),
                    color = palette.textMuted, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, lineHeight = 14.sp
                )
                Button(
                    onClick = onProbe,
                    enabled = !isRefreshing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = palette.orange),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text(if (isRefreshing) "PROBING…" else "PROBE PROVIDERS", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }

            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("DETECTED MODELS:", color = palette.textMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestions.forEach { spec ->
                        Button(
                            onClick = { p = spec.provider; m = spec.model },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (p == spec.provider && m == spec.model) palette.orange else Color(0xFF1f1f1f),
                                contentColor = if (p == spec.provider && m == spec.model) Color.Black else palette.textPrimary
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Text(spec.display, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = p, onValueChange = { p = it },
                label = { Text("Provider (e.g. OPENAI, OLLAMA)", fontSize = 10.sp) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = palette.orange,
                    unfocusedIndicatorColor = palette.textMuted
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = m, onValueChange = { m = it },
                label = { Text("Model ID (e.g. text-embedding-3-small)", fontSize = 10.sp) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = palette.orange,
                    unfocusedIndicatorColor = palette.textMuted
                )
            )
            if (changed) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onSave(p, m) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.orange),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("SAVE EMBEDDING CONFIG", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
            }
        }
    }
}

@Composable
private fun SystemOverridesCard(
    summarizationProvider: String?,
    summarizationModel: String?,
    systemProvider: String?,
    systemModel: String?,
    companionProvider: String?,
    companionModel: String?,
    plannerProvider: String?,
    plannerModel: String?,
    visionProvider: String?,
    visionModel: String?,
    reasoningProvider: String?,
    reasoningModel: String?,
    reflectionProvider: String?,
    reflectionModel: String?,
    onSave: (com.forge.os.domain.companion.Mode, String?, String?) -> Unit,
    availableModels: suspend () -> List<com.forge.os.data.api.AiApiManager.Quad>,
    palette: com.forge.os.presentation.theme.ForgePalette,
) {
    var showSummPicker by remember { mutableStateOf(false) }
    var showSysPicker by remember { mutableStateOf(false) }
    var showCompPicker by remember { mutableStateOf(false) }
    var showPlannerPicker by remember { mutableStateOf(false) }
    var showVisionPicker by remember { mutableStateOf(false) }
    var showReasoningPicker by remember { mutableStateOf(false) }
    var showReflectionPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Configure specific models for conversational and background tasks. If left empty, the system uses the global default.",
                color = palette.textMuted, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, lineHeight = 14.sp
            )
            Spacer(Modifier.height(12.dp))

            ModelPickerRow(
                label = "Companion Chat",
                selectedProvider = companionProvider,
                selectedModel = companionModel,
                onClick = { showCompPicker = true },
                onClear = { onSave(com.forge.os.domain.companion.Mode.COMPANION, null, null) },
                palette = palette
            )

            Spacer(Modifier.height(8.dp))

            ModelPickerRow(
                label = "Summarization",
                selectedProvider = summarizationProvider,
                selectedModel = summarizationModel,
                onClick = { showSummPicker = true },
                onClear = { onSave(com.forge.os.domain.companion.Mode.SUMMARIZATION, null, null) },
                palette = palette
            )
            
            Spacer(Modifier.height(8.dp))

            ModelPickerRow(
                label = "Skill Synthesis",
                selectedProvider = systemProvider,
                selectedModel = systemModel,
                onClick = { showSysPicker = true },
                onClear = { onSave(com.forge.os.domain.companion.Mode.SYSTEM, null, null) },
                palette = palette
            )
            
            Spacer(Modifier.height(8.dp))

            ModelPickerRow(
                label = "DAG Planner",
                selectedProvider = plannerProvider,
                selectedModel = plannerModel,
                onClick = { showPlannerPicker = true },
                onClear = { onSave(com.forge.os.domain.companion.Mode.PLANNER, null, null) },
                palette = palette
            )

            Spacer(Modifier.height(8.dp))

            ModelPickerRow(
                label = "Vision (Phase 4)",
                selectedProvider = visionProvider,
                selectedModel = visionModel,
                onClick = { showVisionPicker = true },
                onClear = { onSave(com.forge.os.domain.companion.Mode.VISION, null, null) },
                palette = palette
            )

            Spacer(Modifier.height(8.dp))

            ModelPickerRow(
                label = "Reasoning (Phase 4)",
                selectedProvider = reasoningProvider,
                selectedModel = reasoningModel,
                onClick = { showReasoningPicker = true },
                onClear = { onSave(com.forge.os.domain.companion.Mode.REASONING, null, null) },
                palette = palette
            )

            Spacer(Modifier.height(8.dp))

            ModelPickerRow(
                label = "Reflection (Phase 4)",
                selectedProvider = reflectionProvider,
                selectedModel = reflectionModel,
                onClick = { showReflectionPicker = true },
                onClear = { onSave(com.forge.os.domain.companion.Mode.REFLECTION, null, null) },
                palette = palette
            )
        }
    }

    if (showCompPicker) {
        ModelPickerDialog(
            onDismiss = { showCompPicker = false },
            onSelect = { p, m ->
                onSave(com.forge.os.domain.companion.Mode.COMPANION, p, m)
                showCompPicker = false
            },
            availableModels = availableModels
        )
    }

    if (showSummPicker) {
        ModelPickerDialog(
            onDismiss = { showSummPicker = false },
            onSelect = { p, m ->
                onSave(com.forge.os.domain.companion.Mode.SUMMARIZATION, p, m)
                showSummPicker = false
            },
            availableModels = availableModels
        )
    }

    if (showSysPicker) {
        ModelPickerDialog(
            onDismiss = { showSysPicker = false },
            onSelect = { p, m ->
                onSave(com.forge.os.domain.companion.Mode.SYSTEM, p, m)
                showSysPicker = false
            },
            availableModels = availableModels
        )
    }

    if (showPlannerPicker) {
        ModelPickerDialog(
            onDismiss = { showPlannerPicker = false },
            onSelect = { p, m ->
                onSave(com.forge.os.domain.companion.Mode.PLANNER, p, m)
                showPlannerPicker = false
            },
            availableModels = availableModels
        )
    }

    if (showVisionPicker) {
        ModelPickerDialog(
            onDismiss = { showVisionPicker = false },
            onSelect = { p, m ->
                onSave(com.forge.os.domain.companion.Mode.VISION, p, m)
                showVisionPicker = false
            },
            availableModels = availableModels
        )
    }

    if (showReasoningPicker) {
        ModelPickerDialog(
            onDismiss = { showReasoningPicker = false },
            onSelect = { p, m ->
                onSave(com.forge.os.domain.companion.Mode.REASONING, p, m)
                showReasoningPicker = false
            },
            availableModels = availableModels
        )
    }

    if (showReflectionPicker) {
        ModelPickerDialog(
            onDismiss = { showReflectionPicker = false },
            onSelect = { p, m ->
                onSave(com.forge.os.domain.companion.Mode.REFLECTION, p, m)
                showReflectionPicker = false
            },
            availableModels = availableModels
        )
    }
}

@Composable
private fun EcoModeCard(
    enabled: Boolean,
    limitUsd: Double,
    ecoProvider: String,
    ecoModel: String,
    onSave: (Boolean?, Double?, String?, String?) -> Unit,
    availableModels: suspend () -> List<com.forge.os.data.api.AiApiManager.Quad>,
    palette: com.forge.os.presentation.theme.ForgePalette,
) {
    var showPicker by remember { mutableStateOf(false) }
    var limitStr by remember(limitUsd) { mutableStateOf("%.2f".format(limitUsd)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Auto-Downshift",
                        color = palette.textPrimary, fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Switch to eco-model when daily spend exceeds limit",
                        color = palette.textMuted, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace, lineHeight = 14.sp
                    )
                }
                androidx.compose.material3.Switch(
                    checked = enabled,
                    onCheckedChange = { onSave(it, null, null, null) },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.orange,
                    )
                )
            }

            if (enabled) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF1f1f1f))
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Daily Limit ($): ",
                        color = palette.textPrimary, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = limitStr,
                        onValueChange = { 
                            limitStr = it
                            it.toDoubleOrNull()?.let { d -> onSave(null, d, null, null) }
                        },
                        modifier = Modifier.width(100.dp).height(48.dp),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = palette.orange,
                            unfocusedIndicatorColor = palette.textMuted
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))

                ModelPickerRow(
                    label = "Eco Model",
                    selectedProvider = ecoProvider,
                    selectedModel = ecoModel,
                    onClick = { showPicker = true },
                    onClear = null, // Eco mode needs a target model
                    palette = palette
                )
            }
        }
    }

    if (showPicker) {
        ModelPickerDialog(
            onDismiss = { showPicker = false },
            onSelect = { p, m ->
                onSave(null, null, p, m)
                showPicker = false
            },
            availableModels = availableModels
        )
    }
}

enum class BackgroundCaller { CRON, ALARMS, SUB_AGENTS, PROACTIVE }
