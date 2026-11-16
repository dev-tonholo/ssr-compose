package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.DEFAULT_CAP_BYTES

private const val SPACE: Byte = 32

/**
 * A file of exactly [DEFAULT_CAP_BYTES] bytes: [head], as many [element] as fit, one [last] element, spaces and
 * [tail].
 */
internal fun fileAtCap(head: String, element: String, last: String, tail: String): ByteArray {
    val headBytes = head.encodeToByteArray()
    val elementBytes = element.encodeToByteArray()
    val lastBytes = last.encodeToByteArray()
    val tailBytes = tail.encodeToByteArray()
    val file = ByteArray(size = DEFAULT_CAP_BYTES)
    val room = file.size - headBytes.size - lastBytes.size - tailBytes.size
    val count = room / elementBytes.size
    headBytes.copyInto(destination = file)
    var position = headBytes.size
    repeat(times = count) {
        elementBytes.copyInto(destination = file, destinationOffset = position)
        position += elementBytes.size
    }
    lastBytes.copyInto(destination = file, destinationOffset = position)
    position += lastBytes.size
    val tailStart = file.size - tailBytes.size
    file.fill(element = SPACE, fromIndex = position, toIndex = tailStart)
    tailBytes.copyInto(destination = file, destinationOffset = tailStart)
    return file
}
