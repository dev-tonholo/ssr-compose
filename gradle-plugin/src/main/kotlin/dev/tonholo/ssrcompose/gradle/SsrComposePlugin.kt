package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

internal class SsrComposePlugin : KotlinCompilerPluginSupportPlugin {
    private val coordinates = PluginCoordinates.load()

    override fun apply(target: Project) {
        val extension = target.extensions.create(EXTENSION_NAME, SsrComposeExtension::class.java)
        target.afterEvaluate {
            SyncHook.registerIfProducer(project = target, extension = extension)
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = CompilerPluginBuildConfig.COMPILER_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = coordinates.group,
            artifactId = coordinates.compilerPluginArtifact,
            version = coordinates.version,
        )

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.project
        val extension = project.extensions.getByType(SsrComposeExtension::class.java)
        addCoreToImplementation(kotlinCompilation)
        runBeforeComposeCompiler(kotlinCompilation)
        declareIndexesAsInputs(kotlinCompilation = kotlinCompilation, extension = extension)
        val subpluginOptions = SsrSubpluginOptions(project = project, extension = extension)
        return project.provider { subpluginOptions.forCompilation(kotlinCompilation) }
    }

    private fun addCoreToImplementation(kotlinCompilation: KotlinCompilation<*>) {
        kotlinCompilation.defaultSourceSet.dependencies {
            implementation(coordinates.coreNotation)
        }
    }

    private fun runBeforeComposeCompiler(kotlinCompilation: KotlinCompilation<*>) {
        kotlinCompilation.compileTaskProvider.configure { compileTask ->
            compileTask.compilerOptions.freeCompilerArgs.add(PluginOrder.FLAG)
        }
    }

    private fun declareIndexesAsInputs(
        kotlinCompilation: KotlinCompilation<*>,
        extension: SsrComposeExtension,
    ) {
        val project = kotlinCompilation.project
        val indexes = project.provider {
            extension.producerSettings.heldCatalogs.map { held ->
                CatalogIndexes.fileOf(project = project, version = held.version)
            }
        }
        kotlinCompilation.compileTaskProvider.configure { compileTask ->
            compileTask.inputs.files(indexes)
                .withPropertyName(INDEXES_INPUT_NAME)
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
    }

    private companion object {
        const val EXTENSION_NAME = "ssrCompose"
        const val INDEXES_INPUT_NAME = "ssrCatalogIndexes"
    }
}
