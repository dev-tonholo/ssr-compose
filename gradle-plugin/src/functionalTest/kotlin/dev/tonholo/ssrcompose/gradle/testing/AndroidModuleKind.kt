package dev.tonholo.ssrcompose.gradle.testing

/** The Android module types ADR 0022 supports, each with its build script, its build task and its archive. */
internal enum class AndroidModuleKind(val buildScript: String, val assembleTask: String, val archiveExtension: String) {
    Application(
        // language=kotlin
        buildScript = """
            plugins {
                alias(libs.plugins.android.application)
                alias(libs.plugins.ssrcompose)
            }

            android {
                namespace = "fixture.app"
                compileSdk = libs.versions.compile.sdk.get().toInt()
                defaultConfig { minSdk = libs.versions.min.sdk.get().toInt() }
            }
        """.trimIndent(),
        assembleTask = "assembleDebug",
        archiveExtension = "apk",
    ),
    Library(
        // language=kotlin
        buildScript = """
            plugins {
                alias(libs.plugins.android.library)
                alias(libs.plugins.ssrcompose)
            }

            android {
                namespace = "fixture.library"
                compileSdk = libs.versions.compile.sdk.get().toInt()
                defaultConfig { minSdk = libs.versions.min.sdk.get().toInt() }
            }
        """.trimIndent(),
        assembleTask = "assembleDebug",
        archiveExtension = "aar",
    ),
    KotlinMultiplatformLibrary(
        // language=kotlin
        buildScript = """
            plugins {
                alias(libs.plugins.kotlin.multiplatform)
                alias(libs.plugins.android.kmp.library)
                alias(libs.plugins.ssrcompose)
            }

            kotlin {
                android {
                    namespace = "fixture.kmp"
                    compileSdk = libs.versions.compile.sdk.get().toInt()
                    minSdk = libs.versions.min.sdk.get().toInt()
                }
            }
        """.trimIndent(),
        assembleTask = "assemble",
        archiveExtension = "aar",
    ),
}
