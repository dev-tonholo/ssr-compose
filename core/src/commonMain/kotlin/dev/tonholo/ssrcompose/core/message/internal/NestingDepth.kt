package dev.tonholo.ssrcompose.core.message.internal

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** The deepest nesting of JSON objects and arrays a frame may have. A list inside a list takes 2 levels, a slot 3. */
internal const val MAX_NESTING_DEPTH = 128

internal fun JsonElement.exceedsNestingDepth(limit: Int): Boolean {
    val pending = ArrayDeque<Pair<JsonElement, Int>>()
    pending.addLast(element = this to 1)
    while (pending.isNotEmpty()) {
        val (element, depth) = pending.removeLast()
        val children = when (element) {
            is JsonObject -> element.values
            is JsonArray -> element
            else -> continue
        }
        if (depth > limit) return true
        children.forEach { child -> pending.addLast(element = child to depth + 1) }
    }
    return false
}
