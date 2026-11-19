package dev.tonholo.ssrcompose.compiler.consumer

import com.tschuchort.compiletesting.JvmCompilationResult
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.fragment.io.CatalogFragmentReader
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import dev.tonholo.ssrcompose.core.data.ValueKind
import java.io.File

private const val ERROR_PREFIX = "e: "

internal class FragmentsFolder(private val directory: File) {
    fun options(): List<Pair<String, String>> = listOf(
        CompilerPluginOptions.MODE to CompilerPluginMode.Consumer.optionValue,
        CompilerPluginOptions.FRAGMENTS_DIR to directory.absolutePath,
    )

    fun read(): List<CatalogFragment> =
        directory.listFiles().orEmpty().sortedBy { it.name }.map { file ->
            CatalogFragmentReader.read(bytes = file.readBytes(), subject = "fragment ${file.name}")
        }
}

internal fun JvmCompilationResult.errorLines(): List<String> =
    messages.lines().filter { line -> line.startsWith(prefix = ERROR_PREFIX) }

internal fun requiredValue(name: String, kind: ValueKind, nullable: Boolean = false): Parameter.Value =
    Parameter.Value(name = name, kind = kind, required = true, nullable = nullable)

internal fun optionalValue(name: String, kind: ValueKind, nullable: Boolean = false): Parameter.Value =
    Parameter.Value(name = name, kind = kind, required = false, nullable = nullable)
