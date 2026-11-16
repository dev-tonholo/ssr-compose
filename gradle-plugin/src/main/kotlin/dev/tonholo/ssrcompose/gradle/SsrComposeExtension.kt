package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The `ssrCompose { }` block. Configure `consumer { }` in an app project or `producer { }` in a server project.
 */
abstract class SsrComposeExtension @Inject constructor(objects: ObjectFactory) {
    internal val consumerSettings: ConsumerSettings = objects.newInstance(ConsumerSettings::class.java)
    internal val producerSettings: ProducerSettings = objects.newInstance(ProducerSettings::class.java)

    /** The largest manifest or part, in bytes, that a catalog step reads. */
    abstract val catalogFileSizeLimit: Property<Long>

    init {
        catalogFileSizeLimit.convention(DEFAULT_CATALOG_FILE_SIZE_LIMIT)
    }

    private var consumerConfigured = false
    private var producerConfigured = false

    fun consumer(action: Action<ConsumerSettings>) {
        consumerConfigured = true
        action.execute(consumerSettings)
    }

    fun producer(action: Action<ProducerSettings>) {
        producerConfigured = true
        action.execute(producerSettings)
    }

    internal fun resolveMode(): CompilerPluginMode =
        when {
            consumerConfigured && !producerConfigured -> CompilerPluginMode.Consumer
            producerConfigured && !consumerConfigured -> CompilerPluginMode.Producer
            consumerConfigured -> error(message = BOTH_BLOCKS_MESSAGE)
            else -> error(message = NO_BLOCK_MESSAGE)
        }

    private companion object {
        const val BYTES_PER_MIB = 1024L * 1024L
        const val DEFAULT_CATALOG_FILE_SIZE_LIMIT = 16L * BYTES_PER_MIB
        val BLOCK_EXAMPLES = listOf(
            "An app module sets the catalog version it builds:",
            "    ssrCompose { consumer { catalogVersion = \"3\" } }",
            "A server module lists the catalogs it holds:",
            "    ssrCompose {",
            "        producer {",
            "            catalog(version = \"3\") {",
            "                location = \"<address>\"",
            "                bridgePackage = \"<package>\"",
            "            }",
            "        }",
            "    }",
        )
        val NO_BLOCK_MESSAGE = message(
            "This module applies the ssrCompose plugin but sets no block.",
            "Add exactly one of consumer { } or producer { } inside ssrCompose { }.",
        )
        val BOTH_BLOCKS_MESSAGE = message(
            "This module sets both consumer { } and producer { } inside ssrCompose { }.",
            "Keep exactly one of consumer { } or producer { }.",
        )

        private fun message(vararg lines: String): String =
            (lines.toList() + BLOCK_EXAMPLES).joinToString(separator = "\n")
    }
}
