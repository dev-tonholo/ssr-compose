package dev.tonholo.ssrcompose.conventions

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByName

private const val CATALOG_NAME = "libs"

internal val Project.libs: LibrariesForLibs
    get() = extensions.getByName<LibrariesForLibs>(CATALOG_NAME)
