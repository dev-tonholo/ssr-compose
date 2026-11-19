package dev.tonholo.ssrcompose.catalog.options

/**
 * The names of the options the compiler plugin reads. The compiler plugin and both build-tool plugins use these
 * names, so they are written only here.
 */
public object CompilerPluginOptions {
    public const val MODE: String = "mode"
    public const val FRAGMENTS_DIR: String = "fragmentsDir"
    public const val CATALOG: String = "catalog"
    public const val SOURCE_FOLDER: String = "sourceFolder"
    public const val EXCLUDED_SOURCES: String = "excludedSources"
}
