package com.forge.os.presentation.screens.pairing

/**
 * The payload encoded into the pairing QR code.
 *
 * <p>Format: a `forge://pair?...` URI so a desktop scanner can treat it as a
 * deep link rather than opaque text.
 *
 * <p>Security model: the six-digit [code] is generated and displayed by the
 * *device*. The user reads it off the phone screen and enters it on the desktop,
 * which then calls `POST /api/pairing/confirm`. The code is single-use and
 * expires after 5 minutes (see `PairingService`), and the desktop receives an
 * HS256 JWT scoped to this pairing. The user seeing the code on the phone is
 * what makes this a real human-verified handshake rather than an unattended
 * LAN-side registration.
 *
 * @property host LAN IP of this device, so the desktop knows where to connect.
 * @property port HTTP bridge port (8789 by default).
 * @property code Six-digit pairing code, single-use, 5-minute TTL.
 */
data class PairingPayload(
    val host: String,
    val port: Int,
    val code: String,
) {
    /**
     * Build the URI encoded into the QR. Kept in one place so the desktop
     * parser and any future deep-link handler stay in sync.
     */
    fun toUri(): String = buildString {
        append(FORGE_PAIR_SCHEME)
        append("://pair")
        append("?host=").append(host.urlEncode())
        append("&port=").append(port)
        append("&code=").append(code)
    }

    companion object {
        const val FORGE_PAIR_SCHEME = "forge"
        const val DEFAULT_PORT = 8789

        /**
         * True when [host] is a usable LAN address. Filters out loopback and
         * link-local (169.254.x.x, which usually means DHCP failed) so we can
         * tell the user to fix their Wi-Fi instead of showing a dead QR.
         */
        fun isRoutableHost(host: String): Boolean {
            if (host.isBlank()) return false
            if (host == "127.0.0.1" || host == "::1" || host.startsWith("169.254.")) {
                return false
            }
            // IPv4 private ranges plus anything else non-loopback.
            return !host.startsWith("0.") || host.startsWith("10.")
        }
    }
}

/** Percent-encodes a bare host/IP so it is safe in a query value. */
private fun String.urlEncode(): String = buildString {
    for (ch in this@urlEncode) {
        when {
            ch.isLetterOrDigit() && ch.code < 128 -> append(ch)
            ch == '.' || ch == '-' -> append(ch)
            else -> append("%%%02X".format(ch.code))
        }
    }
}