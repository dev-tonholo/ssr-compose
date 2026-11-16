package dev.tonholo.ssrcompose.gradle

import java.util.Properties

internal data class PluginCoordinates(
    val group: String,
    val version: String,
    val compilerPluginArtifact: String,
    val coreArtifact: String,
) {
    val compilerPluginNotation: String get() = "$group:$compilerPluginArtifact:$version"
    val coreNotation: String get() = "$group:$coreArtifact:$version"

    companion object {
        private const val RESOURCE_NAME = "/ssr-compose-plugin.properties"
        private const val GROUP_KEY = "group"
        private const val VERSION_KEY = "version"
        private const val COMPILER_PLUGIN_ARTIFACT_KEY = "compilerPluginArtifact"
        private const val CORE_ARTIFACT_KEY = "coreArtifact"

        fun load(): PluginCoordinates {
            val stream = requireNotNull(PluginCoordinates::class.java.getResourceAsStream(RESOURCE_NAME)) {
                "$RESOURCE_NAME is missing from the Gradle plugin"
            }
            val properties = Properties().apply { stream.use(::load) }
            return PluginCoordinates(
                group = properties.getProperty(GROUP_KEY),
                version = properties.getProperty(VERSION_KEY),
                compilerPluginArtifact = properties.getProperty(COMPILER_PLUGIN_ARTIFACT_KEY),
                coreArtifact = properties.getProperty(CORE_ARTIFACT_KEY),
            )
        }
    }
}
