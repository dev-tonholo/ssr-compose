package dev.tonholo.ssrcompose.catalog.bundle

internal object TestResources {
    fun bytes(name: String): ByteArray {
        val stream = requireNotNull(TestResources::class.java.getResourceAsStream(name)) {
            "The test resource $name is missing"
        }
        return stream.use { it.readBytes() }
    }
}
