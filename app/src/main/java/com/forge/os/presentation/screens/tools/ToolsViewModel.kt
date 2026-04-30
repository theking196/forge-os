package com.forge.os.presentation.screens.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.os.domain.agent.ToolRegistry
import com.forge.os.domain.config.ConfigRepository
import com.forge.os.domain.plugins.PluginManager
import com.forge.os.domain.security.PermissionManager
import com.forge.os.domain.security.ToolAuditEntry
import com.forge.os.domain.security.ToolAuditLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolRow(
    val name: String,
    val description: String,
    val isPlugin: Boolean,
    val enabled: Boolean,
    val requiresConfirmation: Boolean,
    val parametersJson: String = "{}",
)

data class ToolsUiState(
    val tools: List<ToolRow> = emptyList(),
    val audit: List<ToolAuditEntry> = emptyList(),
    val testRunning: String? = null,
    val testResult: String? = null,
    val showAudit: Boolean = false,
)

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val permissionManager: PermissionManager,
    private val auditLog: ToolAuditLog,
    private val toolRegistry: ToolRegistry,
    private val pluginManager: PluginManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ToolsUiState())
    val state: StateFlow<ToolsUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        val config = configRepository.get()
        val perms = permissionManager.getPermissions()
        val builtinNames = (config.toolRegistry.enabledTools + config.toolRegistry.disabledTools).distinct().sorted()
        val builtins = builtinNames.map { name ->
            val disabledGlobally = name in config.toolRegistry.disabledTools
            val perm = perms.toolPermissions[name]
            ToolRow(
                name = name,
                description = TOOL_DESCRIPTIONS[name] ?: "(built-in tool)",
                isPlugin = false,
                enabled = !disabledGlobally && (perm?.allowed != false),
                requiresConfirmation = perm?.requiresConfirmation == true,
                parametersJson = toolRegistry.getSchema(name) ?: "{}",
            )
        }
        // Build the plugin row list from every installed plugin's tools (not just
        // the currently-resolved index), so the list stays stable when a plugin
        // is toggled off — and the row's `enabled` flag honestly reflects the
        // plugin's current enabled state instead of being hard-coded to true.
        val pluginRows = pluginManager.listPlugins().flatMap { manifest ->
            manifest.tools.map { tool ->
                val isUserDisabled = tool.name in config.toolRegistry.disabledTools
                ToolRow(
                    name = tool.name,
                    description = tool.description.ifBlank { "(plugin tool from ${manifest.id})" },
                    isPlugin = true,
                    enabled = manifest.enabled && !isUserDisabled,
                    requiresConfirmation = tool.name in config.behaviorRules.confirmDestructive,
                    parametersJson = toolRegistry.getSchema(tool.name) ?: "{}",
                )
            }
        }.filter { it.name !in builtinNames }
        _state.value = _state.value.copy(
            tools = builtins + pluginRows,
            audit = auditLog.entries.value,
        )
    }

    fun toggle(name: String, enabled: Boolean) {
        viewModelScope.launch {
            configRepository.update { c ->
                val disabled = c.toolRegistry.disabledTools.toMutableList()
                if (enabled) disabled.remove(name) else if (name !in disabled) disabled += name
                c.copy(toolRegistry = c.toolRegistry.copy(disabledTools = disabled))
            }
            permissionManager.updateToolPermission(toolName = name, enabled = enabled)
            refresh()
        }
    }

    fun setRequiresConfirmation(name: String, requires: Boolean) {
        // We mutate ConfigRepository.behaviorRules.confirmDestructive as the source of truth.
        viewModelScope.launch {
            configRepository.update { c ->
                val list = c.behaviorRules.confirmDestructive.toMutableList()
                if (requires) { if (name !in list) list += name } else list.remove(name)
                c.copy(behaviorRules = c.behaviorRules.copy(confirmDestructive = list))
            }
            refresh()
        }
    }

    fun runTest(name: String, args: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(testRunning = name, testResult = null)
            val safeArgs = args.ifBlank { "{}" }
            val result = toolRegistry.dispatch(name, safeArgs, toolCallId = "ui_test_${System.currentTimeMillis()}")
            _state.value = _state.value.copy(
                testRunning = null,
                testResult = if (result.isError) "❌ ${result.output}" else "✅ ${result.output}",
                audit = auditLog.entries.value,
            )
        }
    }

    fun toggleAudit() { _state.value = _state.value.copy(showAudit = !_state.value.showAudit) }

    fun clearAudit() {
        auditLog.clear()
        refresh()
    }

    fun dismissTestResult() { _state.value = _state.value.copy(testResult = null) }

    companion object {
        private val TOOL_DESCRIPTIONS = mapOf(
            "file_read" to "Read a file from the workspace",
            "file_write" to "Write to a workspace file",
            "file_list" to "List a workspace directory",
            "file_delete" to "Delete a workspace file",
            "shell_exec" to "Run a shell command",
            "python_run" to "Execute Python in Chaquopy",
            "workspace_info" to "Workspace stats",
            "config_read" to "Read agent config",
            "config_write" to "Mutate config in natural language",
            "config_rollback" to "Roll back config version",
            "heartbeat_check" to "Run health check",
            "memory_store" to "Store a long-term fact",
            "memory_recall" to "Semantic memory recall",
            "memory_store_skill" to "Store a Python skill",
            "memory_summary" to "Summary of memory tiers",
            "cron_add" to "Schedule a job",
            "cron_list" to "List scheduled jobs",
            "cron_remove" to "Remove a scheduled job",
            "cron_run_now" to "Run a job immediately",
            "cron_history" to "Recent cron executions",
            "plugin_list" to "List installed plugins",
            "plugin_install" to "Install a plugin (manifest+code)",
            "plugin_uninstall" to "Uninstall a plugin",
            "plugin_execute" to "Invoke a plugin tool",
            "delegate_task" to "Spawn a sub-agent",
            "delegate_batch" to "Spawn N sub-agents",
            "agents_list" to "List sub-agents",
            "agent_status" to "Sub-agent transcript",
            "agent_cancel" to "Cancel a sub-agent",
            "git_init" to "Init a git repo",
            "git_status" to "git status",
            "git_add" to "Stage files",
            "git_commit" to "Commit current state",
            "git_log" to "Show last N commits",
            "git_diff" to "Diff against HEAD",
            "git_branch" to "List branches",
            "git_checkout" to "Switch / create branch",
            "git_remote_set" to "Set remote URL",
            "git_clone" to "Clone a repo",
            "git_push" to "Push to remote",
            "git_pull" to "Pull from remote",
            "file_download" to "Stream a URL into workspace",
            "browser_download" to "Download with browser cookies",
            "android_device_info" to "Phone model/OS/ID",
            "android_battery" to "Battery level + state",
            "android_volume" to "Read volume",
            "android_network" to "Wi-Fi / cellular state",
            "android_storage" to "Free / total storage",
            "android_screen" to "Screen brightness / size",
            "android_snapshot" to "Bundle: device+battery+net+storage+screen",
            "android_list_apps" to "Installed apps",
            "android_set_volume" to "Change volume (confirm)",
            "android_launch_app" to "Launch app by package (confirm)",
        )
    }
}
