package dev.tonholo.ssrcompose.compiler.testing

import java.io.File

internal object ThrowawayIndexFiles {
    private const val FOLDER = "throwaway-index"

    fun path(indexName: String): String {
        val resource = checkNotNull(javaClass.classLoader.getResource("$FOLDER/$indexName")) {
            "Index not found: $indexName"
        }
        return File(resource.toURI()).absolutePath
    }
}
