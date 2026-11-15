package dev.tonholo.ssrcompose.buildlogic.conventions

import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal val LibrariesForLibs.jvmTarget: JvmTarget
    get() = JvmTarget.fromTarget(versions.jvm.target.get())

internal val LibrariesForLibs.javaRelease: Int
    get() = versions.jvm.target.get().toInt()

internal val LibrariesForLibs.minSdk: Int
    get() = versions.min.sdk.get().toInt()

internal val LibrariesForLibs.compileSdk: Int
    get() = versions.compile.sdk.get().toInt()
