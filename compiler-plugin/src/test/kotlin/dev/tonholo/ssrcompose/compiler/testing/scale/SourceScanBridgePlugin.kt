package dev.tonholo.ssrcompose.compiler.testing.scale

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import java.io.File
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

private const val SOURCE_SCAN_PLUGIN_ID = "dev.tonholo.ssrcompose.scale.source-scan"
private const val CATALOG_OPTION = "catalog"
private const val KOTLIN_EXTENSION = "kt"
private val HELD_CATALOGS = CompilerConfigurationKey.create<List<String>>("held catalogs of the source scan probe")

/** Reads the held catalogs of the named-only probe that a real Gradle build runs. */
internal class SourceScanCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = SOURCE_SCAN_PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = CATALOG_OPTION,
            valueDescription = "<version|index path|bridge package>",
            description = "A held catalog",
            required = true,
            allowMultipleOccurrences = true,
        ),
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        configuration.add(key = HELD_CATALOGS, value = value)
    }
}

/** Declares bridge functions only for the names in the source files this compilation was given. */
internal class SourceScanBridgeRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = SOURCE_SCAN_PLUGIN_ID

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val sources = configuration.kotlinSourceRoots
            .flatMap { root -> File(root.path).walkTopDown().filter { it.extension == KOTLIN_EXTENSION }.toList() }
            .joinToString(separator = "\n") { it.readText() }
        registerExperimentBridges(
            catalogs = configuration.getList(HELD_CATALOGS).map(CatalogOptionValue::decode),
            creation = BridgeCreation(namedIn = sources, files = 1),
        )
    }
}
