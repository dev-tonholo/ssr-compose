package dev.tonholo.ssrcompose.contracttests.textgate

import androidx.compose.ui.text.font.FontFamily

/**
 * A text gate font read from its file. [sha256] is the hash of the bytes the [family] was made from.
 */
class LoadedFont(
    val family: FontFamily,
    val sha256: String,
)
