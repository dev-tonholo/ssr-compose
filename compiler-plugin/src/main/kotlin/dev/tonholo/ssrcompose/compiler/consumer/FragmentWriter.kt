package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.fragment.io.CatalogFragmentWriter
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Writes the fragments of one compilation into the folder of that compilation.
 */
internal class FragmentWriter(private val directory: File) {
    private val writtenNow: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * Writes the fragment of its source file. When the file has no marked composable, removes the fragment written
     * by an earlier compilation, unless another file of this compilation with the same source, such as the
     * `expect` and `actual` files of one name, has written it.
     */
    fun save(fragment: CatalogFragment) {
        val file = File(directory, fragment.fileName)
        if (fragment.components.isEmpty()) {
            if (fragment.fileName !in writtenNow) file.delete()
            return
        }
        writtenNow.add(fragment.fileName)
        directory.mkdirs()
        file.writeBytes(CatalogFragmentWriter.write(fragment = fragment))
    }
}
