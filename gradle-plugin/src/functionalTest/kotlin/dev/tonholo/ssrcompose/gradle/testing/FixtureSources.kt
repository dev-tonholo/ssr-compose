package dev.tonholo.ssrcompose.gradle.testing

internal object FixtureSources {
    const val MARKED_SOURCE_PATH = "src/main/kotlin/Marked.kt"
    // language=kotlin
    const val CONSUMER_BLOCK = """consumer { catalogVersion = "3" }"""

    const val DEFAULT_VERSION = "2"
    const val DEFAULT_BRIDGE_PACKAGE = "app.bridge.v2"

    /** What Gradle prints with `--info` when a change makes a Kotlin compile task rebuild everything. */
    const val FULL_REBUILD_MESSAGE = "The input changes require a full rebuild for incremental task"

    fun indexPath(version: String = DEFAULT_VERSION): String = "build/ssr/index/$version.index"

    // language=kotlin
    fun producerBlock(version: String = DEFAULT_VERSION, bridgePackage: String = DEFAULT_BRIDGE_PACKAGE): String = """
        producer {
            catalog(version = "$version") {
                location = "https://example.com/app/catalogs/$version/manifest.json"
                bridgePackage = "$bridgePackage"
            }
        }
    """.trimIndent()

    // language=kotlin
    val MARKED_SOURCE = """
        import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

        @ConsumerComposable
        fun Marked() {
        }
    """.trimIndent()

    // language=kotlin
    val PRODUCER_BLOCK = """
        producer {
            catalog(version = "2") {
                location = "https://example.com/app/catalogs/2/manifest.json"
                bridgePackage = "app.bridge.v2"
            }
            catalog(version = "3") {
                location = "https://example.com/app/catalogs/3/manifest.json"
                bridgePackage = "app.bridge.v3"
            }
        }
    """.trimIndent()
}
