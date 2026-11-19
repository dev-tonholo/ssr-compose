package dev.tonholo.ssrcompose.compiler.producer

import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Spreads the bridge functions of one catalog over a fixed set of classes named after [moduleName], so a component
 * never changes class and two modules with different names never write the same class.
 */
internal class BridgeClassRule(moduleName: String) {
    private val moduleSuffix: String = moduleSuffixOf(moduleName = moduleName)

    /** The file that holds the function of [componentName]. Its class is this name followed by `Kt`. */
    fun fileNameOf(componentName: String): String =
        "$FILE_NAME_PREFIX${bucketOf(componentName = componentName)}$SEPARATOR$moduleSuffix"

    /** bucket = floorMod(first 4 bytes of SHA-256(UTF-8 name), read as a big endian signed integer, 256) */
    private fun bucketOf(componentName: String): Int =
        Math.floorMod(ByteBuffer.wrap(sha256(text = componentName)).getInt(), CLASS_COUNT)

    private companion object {
        const val CLASS_COUNT = 256
        const val FILE_NAME_PREFIX = "Bridge"
        const val SEPARATOR = '_'
        const val HASH_ALGORITHM = "SHA-256"
        const val MAX_READABLE_MODULE_NAME = 64
        const val MODULE_HASH_BYTES = 8
        const val BYTE_MASK = 0xFF
        const val HEX_RADIX = 16
        const val HEX_DIGITS_PER_BYTE = 2
        const val HEX_PADDING = '0'
        val NOT_FILE_SAFE = Regex("[^A-Za-z0-9_]")

        /** The readable part keeps stack traces clear. The hash keeps names apart that read the same once cleaned. */
        fun moduleSuffixOf(moduleName: String): String {
            val readable = moduleName.replace(regex = NOT_FILE_SAFE, replacement = SEPARATOR.toString())
                .take(n = MAX_READABLE_MODULE_NAME)
            val hash = sha256(text = moduleName).take(n = MODULE_HASH_BYTES).joinToString(separator = "") { byte ->
                (byte.toInt() and BYTE_MASK).toString(radix = HEX_RADIX)
                    .padStart(length = HEX_DIGITS_PER_BYTE, padChar = HEX_PADDING)
            }
            return "$readable$SEPARATOR$hash"
        }

        fun sha256(text: String): ByteArray =
            MessageDigest.getInstance(HASH_ALGORITHM).digest(text.encodeToByteArray())
    }
}
