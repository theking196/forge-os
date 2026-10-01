package com.forge.os.presentation.screens.pairing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DesktopMac
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.forge.os.presentation.theme.forgePalette

/**
 * Device-side desktop pairing screen.
 *
 * Shows the six-digit code and a QR encoding [PairingPayload.toUri]. The user
 * reads this on the phone and enters the code on their desktop, which then calls
 * `POST /api/pairing/confirm` to exchange it for a scoped JWT.
 *
 * Intended to be embedded in the onboarding pager (see ModernOnboardingScreen),
 * which owns paging and the bottom bar; this renders one page and deliberately
 * exposes no back/next so onboarding and a standalone route can both host it.
 */
@Composable
fun PairingPage(
    modifier: Modifier = Modifier,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Pair your desktop",
            color = forgePalette.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Scan this from Forge Desktop, or type the code by hand",
            color = forgePalette.textMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        when (val phase = state.phase) {
            is PairingViewModel.Phase.Resolving -> ResolvingBody()
            is PairingViewModel.Phase.Error -> ErrorBody(phase.message) { viewModel.startPairing() }
            is PairingViewModel.Phase.AwaitingDesktop -> ActiveBody(state)
            is PairingViewModel.Phase.Paired -> PairedBody()
        }
    }
}

@Composable
private fun ResolvingBody() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = forgePalette.orange, strokeWidth = 2.dp)
        Spacer(Modifier.height(16.dp))
        Text("Finding your Wi-Fi address…", color = forgePalette.textMuted, fontSize = 13.sp)
    }
}

@Composable
private fun ErrorBody(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = forgePalette.danger,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            color = forgePalette.textMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Try again")
        }
    }
}
@Composable

private fun ActiveBody(state: PairingViewModel.PairingUiState) {
    // ── QR ──────────────────────────────────────────────────────────────
    val qr = state.qrContent
    if (qr != null) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.72f).aspectRatio(1f),
        ) {
            PairingQr.QrImage(
                payload = qr,
                modifier = Modifier.fillMaxSize().padding(10.dp),
            )
        }
    } else {
        Box(
            Modifier.fillMaxWidth(0.72f).aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = forgePalette.orange)
        }
    }

    Spacer(Modifier.height(18.dp))

    // ── Code ────────────────────────────────────────────────────────────
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = forgePalette.surface,
        border = BorderStroke(1.dp, forgePalette.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("PAIRING CODE", color = forgePalette.textDim, fontSize = 10.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.code.forEach { ch ->
                    Box(
                        Modifier
                            .width(38.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(forgePalette.surfaceSunken),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            ch.toString(),
                            color = forgePalette.textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    // ── Where to point the desktop ──────────────────────────────────────
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Wifi,
            contentDescription = null,
            tint = forgePalette.textDim,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "${state.host}:${state.port}",
            color = forgePalette.textMuted,
            fontSize = 13.sp,
        )
    }

    Spacer(Modifier.height(10.dp))

    // ── Countdown ───────────────────────────────────────────────────────
    val seconds = state.secondsRemaining
    val urgent = seconds <= 30
    Text(
        text = if (urgent) {
            "Expiring in ${seconds}s — a new code is coming"
        } else {
            "Expires in %d:%02d".format(seconds / 60, seconds % 60)
        },
        color = if (urgent) forgePalette.warning else forgePalette.textDim,
        fontSize = 11.sp,
    )

    Spacer(Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.DesktopMac,
            contentDescription = null,
            tint = forgePalette.textDim,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Both devices must be on the same Wi-Fi network",
            color = forgePalette.textDim,
            fontSize = 11.sp,
        )
    }
}


@Composable
private fun PairedBody() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(forgePalette.successBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.DesktopMac,
                contentDescription = null,
                tint = forgePalette.success,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Desktop paired",
            color = forgePalette.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "You can close this and continue.",
            color = forgePalette.textMuted,
            fontSize = 13.sp,
        )
    }
}
