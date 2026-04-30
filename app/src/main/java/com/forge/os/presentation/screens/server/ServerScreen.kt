package com.forge.os.presentation.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forge.os.presentation.screens.common.ForgeOsPalette
import com.forge.os.presentation.screens.common.ModuleScaffold

@Composable
fun ServerScreen(onBack: () -> Unit, viewModel: ServerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    ModuleScaffold(title = "SERVER", onBack = onBack) {
        Column(
            Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusCard(state)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.start() },
                    enabled = !state.running,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ForgeOsPalette.Success),
                ) { Text("START", fontFamily = FontFamily.Monospace) }
                OutlinedButton(
                    onClick = { viewModel.stop() },
                    enabled = state.running,
                    modifier = Modifier.weight(1f),
                ) { Text("STOP", fontFamily = FontFamily.Monospace) }
            }

            KeyCard(state.apiKey, onCopy = {
                clipboard.setText(AnnotatedString(state.apiKey))
            }, onRotate = { viewModel.rotateKey() })

            EndpointsCard()

            if (state.lanIps.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(ForgeOsPalette.Surface, RoundedCornerShape(6.dp))
                        .border(1.dp, ForgeOsPalette.Border, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text("LAN addresses", color = ForgeOsPalette.Orange,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    state.lanIps.forEach { ip ->
                        Text("http://$ip:${state.port}", color = ForgeOsPalette.TextPrimary,
                            fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: ServerUiState) {
    Column(
        Modifier.fillMaxWidth()
            .background(ForgeOsPalette.Surface, RoundedCornerShape(6.dp))
            .border(1.dp, ForgeOsPalette.Border, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.running) "● RUNNING" else "○ STOPPED",
                color = if (state.running) ForgeOsPalette.Success else ForgeOsPalette.TextMuted,
                fontFamily = FontFamily.Monospace, fontSize = 13.sp,
            )
            Spacer(Modifier.weight(1f))
            Text("port ${state.port}", color = ForgeOsPalette.TextMuted,
                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Local HTTP API for your own tools. Bind to 127.0.0.1 by default — " +
            "reachable from this device and over LAN if your OS routes it.",
            color = ForgeOsPalette.TextMuted, fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun KeyCard(apiKey: String, onCopy: () -> Unit, onRotate: () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(ForgeOsPalette.Surface, RoundedCornerShape(6.dp))
            .border(1.dp, ForgeOsPalette.Border, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text("API KEY", color = ForgeOsPalette.Orange,
            fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(apiKey, color = ForgeOsPalette.TextPrimary,
            fontFamily = FontFamily.Monospace, fontSize = 11.sp,
            modifier = Modifier.clickable { onCopy() })
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                Text("COPY", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            OutlinedButton(onClick = onRotate, modifier = Modifier.weight(1f)) {
                Text("ROTATE", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EndpointsCard() {
    Column(
        Modifier.fillMaxWidth()
            .background(ForgeOsPalette.Surface, RoundedCornerShape(6.dp))
            .border(1.dp, ForgeOsPalette.Border, RoundedCornerShape(6.dp))
            .padding(12.dp)
    ) {
        Text("ENDPOINTS", color = ForgeOsPalette.Orange,
            fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        listOf(
            "GET  /api/status" to "Health check",
            "GET  /api/tools" to "List all tools",
            "POST /api/tool" to "{ \"name\": \"...\", \"args\": {...} }",
        ).forEach { (ep, desc) ->
            Text(ep, color = ForgeOsPalette.TextPrimary,
                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Text("   $desc", color = ForgeOsPalette.TextMuted,
                fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text("All requests require: Authorization: Bearer <api-key>",
            color = ForgeOsPalette.TextMuted, fontFamily = FontFamily.Monospace,
            fontSize = 10.sp)
    }
}
