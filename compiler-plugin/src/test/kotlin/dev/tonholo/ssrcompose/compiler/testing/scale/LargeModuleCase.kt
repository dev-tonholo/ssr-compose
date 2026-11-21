package dev.tonholo.ssrcompose.compiler.testing.scale

/**
 * One way to compile a server module of thousands of files.
 *
 * @property heldCatalogs how many full catalogs the module holds.
 * @property isClean true when the compiler gets every file, false when it gets only the one edited file.
 * @property usesPlugin false means hand-written stand-ins for the bridges and no plugin. It is false when the module
 * holds no catalog, except for the case that runs the plugin with no catalog.
 */
internal enum class LargeModuleCase(val heldCatalogs: Int, val isClean: Boolean, val usesPlugin: Boolean) {
    CleanStandIns(heldCatalogs = 0, isClean = true, usesPlugin = false),
    CleanNoCatalog(heldCatalogs = 0, isClean = true, usesPlugin = true),
    CleanOneCatalog(heldCatalogs = 1, isClean = true, usesPlugin = true),
    CleanTenCatalogs(heldCatalogs = 10, isClean = true, usesPlugin = true),
    EditedFileStandIns(heldCatalogs = 0, isClean = false, usesPlugin = false),
    EditedFileOneCatalog(heldCatalogs = 1, isClean = false, usesPlugin = true),
    EditedFileTenCatalogs(heldCatalogs = 10, isClean = false, usesPlugin = true),
}
