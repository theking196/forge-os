package com.forge.os.presentation.screens.pairing

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders the desktop-pairing payload as a QR code bitmap.
 *
 * Uses zxing's pure-Java `core` encoder (no camera, no android module) and
 * rasterises the resulting BitMatrix ourselves, so the dependency stays small.
 */
object PairingQr {

    /** Module margin required by the QR spec so scanners find the quiet zone. */
    private const val MARGIN = 1

    /**
     * Encode [payload] into an [ImageBitmap] of [size] x [size] pixels.
     *
     * @throws IllegalStateException if zxing cannot encode the payload. Callers
     *   are expected to have already validated the payload via [PairingPayload].
     */
    fun encode(payload: String, size: Int = 720): ImageBitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to MARGIN,
        )
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val offset = y * size
            for (x in 0 until size) {
                pixels[offset + x] =
                    if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            .apply { setPixels(pixels, 0, size, 0, 0, size, size) }
            .asImageBitmap()
    }

    /**
     * Remembered [Painter] for [payload]. Encoding is pure and deterministic, so
     * it is cached against the payload rather than recomputed each recomposition.
     */
    @Composable
    fun rememberQrPainter(payload: String, sizePx: Int = 720): Painter =
        remember(payload, sizePx) { BitmapPainter(encode(payload, sizePx)) }

    /** Convenience: draw the QR straight into a Compose [Image]. */
    @Composable
    fun QrImage(payload: String, modifier: Modifier = Modifier, sizePx: Int = 720) {
        Image(
            painter = rememberQrPainter(payload, sizePx),
            contentDescription = "Desktop pairing QR code",
            modifier = modifier,
        )
    }
}