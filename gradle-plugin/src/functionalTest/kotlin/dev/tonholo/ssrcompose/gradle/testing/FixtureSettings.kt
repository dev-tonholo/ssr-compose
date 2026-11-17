package dev.tonholo.ssrcompose.gradle.testing

import java.io.File

/**
 * The `settings.gradle.kts` every fixture project has: the repository's version catalog, the repository as an
 * included build so the fixture finds the plugin, and an optional local build cache.
 */
internal object FixtureSettings {
    private const val REPOSITORY_ROOT_PROPERTY = "ssrcompose.repoRoot"
    private const val REPOSITORY_ROOT_PLACEHOLDER = "REPOSITORY_ROOT"
    private const val BUILD_CACHE_PLACEHOLDER = "BUILD_CACHE_DIRECTORY"

    fun script(buildCacheDirectory: File? = null): String {
        val repositoryRoot = requireNotNull(System.getProperty(REPOSITORY_ROOT_PROPERTY)) {
            "The $REPOSITORY_ROOT_PROPERTY system property is not set"
        }
        val settings = SETTINGS_TEMPLATE.replace(oldValue = REPOSITORY_ROOT_PLACEHOLDER, newValue = repositoryRoot)
        val cacheBlock = buildCacheDirectory?.let { cacheDirectory ->
            BUILD_CACHE_TEMPLATE.replace(
                oldValue = BUILD_CACHE_PLACEHOLDER,
                newValue = cacheDirectory.invariantSeparatorsPath,
            )
        }
        return listOfNotNull(settings, cacheBlock).joinToString(separator = "\n")
    }

    private val SETTINGS_TEMPLATE = """
        pluginManagement {
            repositories {
                google()
                mavenCentral()
                gradlePluginPortal()
            }
        }

        dependencyResolutionManagement {
            repositories {
                google()
                mavenCentral()
            }
            versionCatalogs {
                create("libs") {
                    from(files("$REPOSITORY_ROOT_PLACEHOLDER/gradle/libs.versions.toml"))
                }
            }
        }

        includeBuild("$REPOSITORY_ROOT_PLACEHOLDER")

        rootProject.name = "fixture"
    """.trimIndent()

    private val BUILD_CACHE_TEMPLATE = """
        buildCache {
            local {
                directory = file("$BUILD_CACHE_PLACEHOLDER")
            }
        }
    """.trimIndent()
}
