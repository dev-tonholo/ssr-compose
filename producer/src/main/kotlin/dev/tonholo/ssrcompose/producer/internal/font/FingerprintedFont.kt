package dev.tonholo.ssrcompose.producer.internal.font

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font as bytesFont

/**
 * A font made from [data], the bytes of one font file whose [fingerprint] is their SHA-256 in lowercase hexadecimal,
 * as `fonts.json` gives it or `Fingerprint.of` computes it. Every font the producer creates from bytes comes from here:
 * Compose caches such a font by its identity and never by its bytes, so the identity names the file (ADR 0030).
 */
internal fun fingerprintedFont(data: ByteArray, fingerprint: String, weight: FontWeight, style: FontStyle): Font =
    bytesFont(identity = "sha256:$fingerprint", data = data, weight = weight, style = style)
