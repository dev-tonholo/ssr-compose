package dev.tonholo.ssrcompose.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private const val CATALOG_NAME = "libs"
private const val JVM_TARGET_ALIAS = "jvm-target"
private const val MIN_SDK_ALIAS = "min-sdk"
private const val COMPILE_SDK_ALIAS = "compile-sdk"

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named(CATALOG_NAME)

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

internal fun VersionCatalog.versionOf(alias: String): String =
    findVersion(alias).get().requiredVersion

internal val VersionCatalog.jvmTarget: JvmTarget
    get() = JvmTarget.fromTarget(versionOf(alias = JVM_TARGET_ALIAS))

internal val VersionCatalog.javaRelease: Int
    get() = versionOf(alias = JVM_TARGET_ALIAS).toInt()

internal val VersionCatalog.minSdk: Int
    get() = versionOf(alias = MIN_SDK_ALIAS).toInt()

internal val VersionCatalog.compileSdk: Int
    get() = versionOf(alias = COMPILE_SDK_ALIAS).toInt()
