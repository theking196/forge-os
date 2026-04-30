package com.forge.os.domain.agent.providers

import com.forge.os.data.sandbox.SandboxManager
import com.forge.os.domain.agent.ToolDefinition
import com.forge.os.domain.agent.ToolProvider
import com.forge.os.domain.agent.params
import com.forge.os.domain.agent.tool
import com.forge.os.domain.workspace.WorkspaceLayout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileToolProvider @Inject constructor(
    private val sandboxManager: SandboxManager
) : ToolProvider {

    override fun getTools(): List<ToolDefinition> = listOf(
        tool("file_read",
            "Read the text content of a file. Supports pagination for large files to avoid context limits.",
            params("path" to "string:Workspace-relative path",
                   "start_line" to "int:Optional 1-based start line",
                   "end_line" to "int:Optional 1-based end line")),
        tool("file_write",
            "Create or overwrite a file in the workspace.",
            params("path" to "string:Workspace-relative path",
                   "content" to "string:Content to write")),
        tool("file_list",
            "List files in a workspace directory.",
            params("path" to "string:Workspace-relative directory (default '.')")),
        tool("file_delete",
            "Delete a file from the workspace.",
            params("path" to "string:Workspace-relative path")),
        tool("shell_exec",
            "Execute a shell command in the workspace root.",
            params("command" to "string:Shell command")),
        tool("python_run",
            "Execute Python 3.11 code in the workspace sandbox. Returns stdout/stderr.",
            params("code" to "string:Python source code")),
        tool("workspace_info",
            "Get summary of files, directories, and disk usage in the sandbox.",
            emptyMap(), required = emptyList()),
        tool("workspace_describe",
            "Describe the canonical Forge OS workspace layout and folder purposes.",
            emptyMap(), required = emptyList()),
        tool("python_packages",
            "List installed Python packages in the local Chaquopy environment. " +
            "Use this to check if a library is available before trying to import it.",
            emptyMap(), required = emptyList())
    )

    override suspend fun dispatch(toolName: String, args: Map<String, Any>): String? {
        return when (toolName) {
            "file_read"          -> fileRead(args)
            "file_write"         -> fileWrite(args)
            "file_list"          -> fileList(args)
            "file_delete"        -> fileDelete(args)
            "shell_exec"         -> shellExec(args)
            "python_run"         -> pythonRun(args)
            "workspace_info"     -> workspaceInfo()
            "workspace_describe" -> WorkspaceLayout.describe()
            "python_packages"   -> pythonPackages()
            else -> null
        }
    }

    private suspend fun fileRead(args: Map<String, Any>): String {
        val path = args["path"]?.toString() ?: return "Error: path required"
        val startLine = args["start_line"]?.toString()?.toDoubleOrNull()?.toInt()?.coerceAtLeast(1)
        val endLine = args["end_line"]?.toString()?.toDoubleOrNull()?.toInt()?.coerceAtLeast(1)

        return sandboxManager.readFile(path).fold(
            onSuccess = { content ->
                val lines = content.lines()
                val limit = 500

                // If no bounds specified and file is huge, truncate to save context limit
                if (startLine == null && endLine == null && lines.size > limit) {
                    val sliced = lines.take(limit)
                    return@fold "--- Lines 1 to $limit of ${lines.size} ---\n" +
                                "(File too large to read at once. Use start_line and end_line to read the rest!)\n\n" +
                                sliced.joinToString("\n")
                }

                val startIdx = (startLine ?: 1) - 1
                val endIdx = (endLine ?: lines.size).coerceAtMost(lines.size) - 1
                
                if (startIdx >= lines.size) return@fold "File only has ${lines.size} lines."
                if (startIdx > endIdx) return@fold "Invalid range: $startLine to $endLine"
                
                val sliced = lines.slice(startIdx..endIdx)
                val header = if (startLine != null || endLine != null) 
                    "--- Lines ${startIdx + 1} to ${endIdx + 1} of ${lines.size} ---\n"
                else ""
                    
                header + sliced.joinToString("\n")
            },
            onFailure = { "❌ readFile failed: ${it.message}" }
        )
    }

    private suspend fun fileWrite(args: Map<String, Any>): String {
        val path = args["path"]?.toString() ?: return "Error: path required"
        val content = args["content"]?.toString() ?: return "Error: content required"
        return sandboxManager.writeFile(path, content).fold(
            onSuccess = { "✅ Written ${content.length} chars to $path" },
            onFailure = { "❌ writeFile failed: ${it.message}" }
        )
    }

    private suspend fun fileList(args: Map<String, Any>): String {
        val path = args["path"]?.toString() ?: ""
        return sandboxManager.listFiles(path).fold(
            onSuccess = { files ->
                if (files.isEmpty()) "(empty)"
                else files.joinToString("\n") { "${if (it.isDirectory) "📁" else "📄"} ${it.name}  (${it.size}b)" }
            },
            onFailure = { "❌ listFiles failed: ${it.message}" }
        )
    }

    private suspend fun fileDelete(args: Map<String, Any>): String {
        val path = args["path"]?.toString() ?: return "Error: path required"
        return sandboxManager.deleteFile(path).fold(
            onSuccess = { "✅ Deleted $path" },
            onFailure = { "❌ deleteFile failed: ${it.message}" }
        )
    }

    private suspend fun shellExec(args: Map<String, Any>): String {
        val command = args["command"]?.toString() ?: return "Error: command required"
        return sandboxManager.executeShell(command).getOrElse { "❌ shell failed: ${it.message}" }
    }

    private suspend fun pythonRun(args: Map<String, Any>): String {
        val code = args["code"]?.toString() ?: return "Error: code required"
        return sandboxManager.executePython(code).getOrElse { "❌ python failed: ${it.message}" }
    }

    private suspend fun workspaceInfo(): String {
        val info = sandboxManager.getWorkspaceInfo()
        val pct = "%.1f".format(info.usagePercent)
        return buildString {
            appendLine("📊 Workspace")
            appendLine("  files: ${info.totalFiles}")
            appendLine("  dirs: ${info.totalDirs}")
            appendLine("  size: ${info.totalSize}b / ${info.maxSize}b ($pct%)")
        }
    }

    private suspend fun pythonPackages(): String {
        // Simple way to list packages in most environments
        val code = """
import sys
import subprocess

try:
    # Try using pip if available
    import pip
    result = subprocess.check_output([sys.executable, "-m", "pip", "list"], stderr=subprocess.STDOUT)
    print(result.decode())
except Exception:
    try:
        # Fallback to pkg_resources
        import pkg_resources
        for d in pkg_resources.working_set:
            print(f"{d.project_name}=={d.version}")
    except Exception as e:
        print(f"Error listing packages: {e}")
        # Last resort: just show what's in sys.path
        print("\nsys.path:")
        print("\n".join(sys.path))
""".trimIndent()
        return sandboxManager.executePython(code).getOrElse { "❌ python failed: ${it.message}" }
    }
}
