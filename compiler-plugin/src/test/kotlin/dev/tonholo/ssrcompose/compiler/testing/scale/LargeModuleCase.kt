package dev.tonholo.ssrcompose.compiler.testing.scale

/**
 * One way to compile a server module of thousands of files.
 *
 * @property heldCatalogs how many full catalogs the module holds. Zero means hand-written stand-ins and no plugin.
 * @property isClean true when the compiler gets every file, false when it gets only the one edited file.
 */
internal enum class LargeModuleCase(val heldCatalogs: Int, val isClean: Boolean) {
    CleanStandIns(heldCatalogs = 0, isClean = true),
    CleanOneCatalog(heldCatalogs = 1, isClean = true),
    CleanTenCatalogs(heldCatalogs = 10, isClean = true),
    EditedFileStandIns(heldCatalogs = 0, isClean = false),
    EditedFileOneCatalog(heldCatalogs = 1, isClean = false),
    EditedFileTenCatalogs(heldCatalogs = 10, isClean = false),
}
