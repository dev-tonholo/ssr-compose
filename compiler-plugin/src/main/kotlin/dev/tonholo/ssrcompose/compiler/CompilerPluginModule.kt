package dev.tonholo.ssrcompose.compiler

import org.jetbrains.kotlin.name.FqName

internal class CompilerPluginModule {
    val name: String = "compiler-plugin"

    fun packageNamed(fqName: String): FqName = FqName(fqName)
}
