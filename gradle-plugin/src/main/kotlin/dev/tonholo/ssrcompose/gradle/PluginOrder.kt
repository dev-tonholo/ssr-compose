package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig

internal object PluginOrder {
    private const val COMPOSE_COMPILER_PLUGIN_ID = "androidx.compose.compiler.plugins.kotlin"

    /** The compiler argument that runs this plugin before the Compose compiler. */
    const val FLAG: String =
        "-Xcompiler-plugin-order=${CompilerPluginBuildConfig.COMPILER_PLUGIN_ID}>$COMPOSE_COMPILER_PLUGIN_ID"
}
