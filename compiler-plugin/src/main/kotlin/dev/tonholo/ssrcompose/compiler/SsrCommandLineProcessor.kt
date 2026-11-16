package dev.tonholo.ssrcompose.compiler

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

internal class SsrCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = CompilerPluginBuildConfig.COMPILER_PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = CompilerPluginOptions.MODE,
            valueDescription = CompilerPluginMode.entries.joinToString(separator = "|") { it.optionValue },
            description = "The side of the connection this compilation belongs to",
            required = true,
        ),
        CliOption(
            optionName = CompilerPluginOptions.FRAGMENTS_DIR,
            valueDescription = "<path>",
            description = "Folder where the consumer side writes its fragments",
            required = false,
        ),
        CliOption(
            optionName = CompilerPluginOptions.CATALOG,
            valueDescription = catalogValueDescription(),
            description = "A held catalog of the producer side, given once for each catalog",
            required = false,
            allowMultipleOccurrences = true,
        ),
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            CompilerPluginOptions.MODE ->
                configuration.put(key = SsrConfigurationKeys.MODE, value = parseMode(value))

            CompilerPluginOptions.FRAGMENTS_DIR ->
                configuration.put(key = SsrConfigurationKeys.FRAGMENTS_DIR, value = value)

            CompilerPluginOptions.CATALOG ->
                configuration.add(key = SsrConfigurationKeys.CATALOGS, value = parseCatalog(value))

            else -> throw CliOptionProcessingException(message = "Unknown option: ${option.optionName}")
        }
    }

    private fun catalogValueDescription(): String =
        CatalogOptionValue.FIELD_NAMES.joinToString(
            separator = CatalogOptionValue.FIELD_SEPARATOR.toString(),
            prefix = "<",
            postfix = ">",
        )

    private fun parseMode(value: String): CompilerPluginMode =
        CompilerPluginMode.fromOptionValue(value)
            ?: throw CliOptionProcessingException(
                message = "Unknown mode '$value'. " +
                    "Use one of: ${CompilerPluginMode.entries.joinToString { it.optionValue }}",
            )

    private fun parseCatalog(value: String): CatalogOptionValue =
        try {
            CatalogOptionValue.decode(value)
        } catch (failure: IllegalArgumentException) {
            throw CliOptionProcessingException(message = failure.message.orEmpty(), cause = failure)
        }
}
