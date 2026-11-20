package dev.tonholo.ssrcompose.core.document.internal

import dev.tonholo.ssrcompose.core.document.DocumentNode

private const val MAX_POSITION = 100_000f

internal fun requirePlacement(node: DocumentNode) {
    requirePosition(value = node.x, what = "x")
    requirePosition(value = node.y, what = "y")
    requireSize(value = node.width, what = "width")
    requireSize(value = node.height, what = "height")
}

internal fun requireSize(value: Float, what: String) {
    require(value.isFinite() && value >= 0f) { "A $what must be finite and not negative, was $value" }
}

private fun requirePosition(value: Float, what: String) {
    require(value in -MAX_POSITION..MAX_POSITION) { "An $what must be within $MAX_POSITION dp either way, was $value" }
}
