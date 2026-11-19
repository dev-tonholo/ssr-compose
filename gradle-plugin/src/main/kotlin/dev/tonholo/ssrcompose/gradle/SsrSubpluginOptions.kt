package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.CompositeSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.InternalSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/** Builds the compiler plugin options of a compilation from the `ssrCompose` block. */
internal class SsrSubpluginOptions(
    private val project: Project,
    private val extension: SsrComposeExtension,
) {
    fun forCompilation(compilation: KotlinCompilation<*>): List<SubpluginOption> {
        val mode = extension.resolveMode()
        val modeOption = SubpluginOption(key = CompilerPluginOptions.MODE, value = mode.optionValue)
        return when (mode) {
            CompilerPluginMode.Consumer -> listOf(modeOption, fragmentsDirOption(compilation))
            CompilerPluginMode.Producer -> listOf(modeOption) + catalogOptions() + sourceFolderOptions(compilation)
            CompilerPluginMode.Check -> listOf(modeOption)
        }
    }

    private fun fragmentsDirOption(compilation: KotlinCompilation<*>): SubpluginOption {
        val fragmentsDir = project.layout.buildDirectory
            .dir("$FRAGMENTS_FOLDER/${compilation.target.targetName}/${compilation.name}")
            .get()
            .asFile
            .absolutePath
        return InternalSubpluginOption(key = CompilerPluginOptions.FRAGMENTS_DIR, value = fragmentsDir)
    }

    private fun catalogOptions(): List<SubpluginOption> =
        extension.producerSettings.heldCatalogs.map { held ->
            val value = CatalogOptionValue(
                version = held.version,
                indexPath = CatalogIndexes.fileOf(project = project, version = held.version).get().asFile.absolutePath,
                bridgePackage = held.bridgePackage.get(),
            )
            CompositeSubpluginOption(
                key = CompilerPluginOptions.CATALOG,
                value = value.encode(),
                originalOptions = listOf(
                    SubpluginOption(key = VERSION_KEY, value = value.version),
                    SubpluginOption(key = BRIDGE_PACKAGE_KEY, value = value.bridgePackage),
                    InternalSubpluginOption(key = INDEX_PATH_KEY, value = value.indexPath),
                ),
            )
        }

    private fun sourceFolderOptions(compilation: KotlinCompilation<*>): List<SubpluginOption> {
        if (extension.producerSettings.heldCatalogs.isEmpty()) return emptyList()
        val folders = compilation.allKotlinSourceSets
            .flatMap { sourceSet -> sourceSet.kotlin.srcDirs }
            .distinct()
            .map { folder ->
                InternalSubpluginOption(key = CompilerPluginOptions.SOURCE_FOLDER, value = folder.absolutePath)
            }
        val excludedList = ExcludedSourcesListFile.fileOf(project = project, compilation = compilation).get().asFile
        return folders + InternalSubpluginOption(
            key = CompilerPluginOptions.EXCLUDED_SOURCES,
            value = excludedList.absolutePath,
        )
    }

    private companion object {
        const val FRAGMENTS_FOLDER = "ssr/fragments"
        const val VERSION_KEY = "version"
        const val BRIDGE_PACKAGE_KEY = "bridgePackage"
        const val INDEX_PATH_KEY = "indexPath"
    }
}
