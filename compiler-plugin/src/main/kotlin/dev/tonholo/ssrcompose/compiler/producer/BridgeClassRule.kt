package dev.tonholo.ssrcompose.compiler.producer

import java.nio.ByteBuffer
import java.security.MessageDigest

/** Spreads the bridge functions of one catalog over a fixed set of classes, so a component never changes class. */
internal object BridgeClassRule {
    private const val CLASS_COUNT = 256
    private const val FILE_NAME_PREFIX = "Bridge"
    private const val HASH_ALGORITHM = "SHA-256"

    /** The file that holds the function of [componentName]. Its class is this name followed by `Kt`. */
    fun fileNameOf(componentName: String): String = "$FILE_NAME_PREFIX${bucketOf(componentName = componentName)}"

    /** bucket = floorMod(first 4 bytes of SHA-256(UTF-8 name), read as a big endian signed integer, 256) */
    private fun bucketOf(componentName: String): Int {
        val hash = MessageDigest.getInstance(HASH_ALGORITHM).digest(componentName.encodeToByteArray())
        return Math.floorMod(ByteBuffer.wrap(hash).getInt(), CLASS_COUNT)
    }
}
