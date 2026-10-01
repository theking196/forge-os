package com.forge.os.presentation.screens.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.os.service.PairingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.NetworkInterface
import javax.inject.Inject

/**
 * Drives the device-side desktop-pairing screen.
 *
 * This is the missing half of the pairing handshake. Previously the six-digit
 * code was minted inside the `POST /api/pairing/initiate` HTTP handler and
 * returned straight to the desktop, so the "enter the code shown on your
 * device" step was a no-op loop any LAN client could bypass. Now the code is
 * generated here, shown on the phone, and only then confirmed by the desktop.
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairingService: PairingService,
    private val addressResolver: LanAddressResolver,
) : ViewModel() {

    /** What the screen is currently doing. */
    sealed interface Phase {
        /** Resolving the LAN address before a code can be issued. */
        data object Resolving : Phase
        /** Waiting for the desktop to POST /api/pairing/confirm. */
        data object AwaitingDesktop : Phase
        /** The desktop confirmed; the handshake is complete. */
        data object Paired : Phase
        /** Could not determine a usable LAN address. */
        data class Error(val message: String) : Phase
    }

    data class PairingUiState(
        val phase: Phase = Phase.Resolving,
        val code: String = "",
        val host: String = "",
        val port: Int = PairingPayload.DEFAULT_PORT,
        /** Seconds until the code expires; counts down while visible. */
        val secondsRemaining: Int = 0,
    ) {
        /** The URI encoded into the QR, or null before a code exists. */
        val qrContent: String?
            get() = if (code.isNotBlank() && host.isNotBlank()) {
                PairingPayload(host, port, code).toUri()
            } else {
                null
            }
    }

    private val _state = MutableStateFlow(PairingUiState())
    val state: StateFlow<PairingUiState> = _state.asStateFlow()

    /** Matches the code TTL enforced in PairingService (5 minutes). */
    private val codeTtlSeconds = 5 * 60

    private var countdownJob: Job? = null

    init {
        startPairing()
    }

    /**
     * Resolve the LAN address, mint a code, then start the expiry countdown.
     * Safe to call again to issue a fresh code (after expiry or on retry).
     */
    fun startPairing() {
        countdownJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(phase = Phase.Resolving) }

            val host = addressResolver.lanAddress()
            if (host == null) {
                _state.update {
                    it.copy(
                        phase = Phase.Error(
                            "Could not find a Wi-Fi address. Join a network and try again."
                        )
                    )
                }
                return@launch
            }

            // The code is displayed on the phone; the desktop name only labels
            // the pending request so logs stay readable.
            val (code, _) = pairingService.generatePairingCode("Pending Desktop")

            _state.update {
                it.copy(
                    phase = Phase.AwaitingDesktop,
                    code = code,
                    host = host,
                    port = PairingPayload.DEFAULT_PORT,
                    secondsRemaining = codeTtlSeconds,
                )
            }
            startCountdown()
        }
    }

    /**
     * Ticks the visible countdown. On expiry the code is stale (PairingService
     * will reject it), so issue a fresh one rather than leaving a QR on screen
     * that silently stops working.
     */
    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_state.value.secondsRemaining > 0) {
                delay(1000)
                val next = _state.value.secondsRemaining - 1
                if (next <= 0) {
                    startPairing()
                    return@launch
                }
                _state.update { it.copy(secondsRemaining = next) }
            }
        }
    }

    /**
     * Called when the desktop reports the handshake finished. An explicit hook so
     * the HTTP layer can notify us without this screen polling the service.
     */
    fun onDesktopConfirmed() {
        countdownJob?.cancel()
        _state.update { it.copy(phase = Phase.Paired, secondsRemaining = 0) }
    }
}

/**
 * Resolves this device's LAN IPv4 address.
 *
 * Extracted so unit tests can substitute a fixed address instead of touching
 * real network interfaces. Mirrors the approach used by the static file server:
 * prefer an up, non-loopback, non-link-local IPv4 interface.
 */
open class LanAddressResolver @Inject constructor() {
    open fun lanAddress(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .flatMap { nic -> nic.inetAddresses.toList() }
            .firstOrNull { addr ->
                addr.address.size == 4 &&
                    !addr.isLoopbackAddress &&
                    !addr.isLinkLocalAddress
            }
            ?.hostAddress
    }.getOrNull()
}
