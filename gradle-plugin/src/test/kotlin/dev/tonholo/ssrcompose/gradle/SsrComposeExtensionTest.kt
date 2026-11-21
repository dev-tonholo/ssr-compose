package dev.tonholo.ssrcompose.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SsrComposeExtensionTest {
    private val project: Project = ProjectBuilder.builder().build()
    private val extension: SsrComposeExtension =
        project.extensions.create("ssrCompose", SsrComposeExtension::class.java)

    @Test
    fun `given both blocks, when the mode is resolved, then it fails and asks to keep one`() {
        // Arrange
        extension.consumer { it.catalogVersion.set("3") }
        extension.producer { it.catalog(version = "3") { held -> held.bridgePackage.set("app.v3") } }

        // Act
        val failure = assertFailsWith<IllegalStateException> { extension.resolveMode() }

        // Assert
        val message = failure.message.orEmpty()
        assertTrue(actual = message.contains("consumer { }"), message = message)
        assertTrue(actual = message.contains("producer { }"), message = message)
    }

    @Test
    fun `given a family and then a bundle, when declared, then it fails saying where each belongs`() {
        // Arrange
        val location = "https://example.com/ssr/fonts/1/fonts.json"

        // Act
        val failure = assertFailsWith<IllegalStateException> {
            extension.fonts { fonts ->
                fonts.family(name = "brand") { }
                fonts.bundle(version = "1", location = location)
            }
        }

        // Assert
        assertNamesWhereEachBelongs(message = failure.message.orEmpty())
    }

    @Test
    fun `given a bundle and then a family, when declared, then it fails saying where each belongs`() {
        // Arrange
        val location = "https://example.com/ssr/fonts/1/fonts.json"

        // Act
        val failure = assertFailsWith<IllegalStateException> {
            extension.fonts { fonts ->
                fonts.bundle(version = "1", location = location)
                fonts.family(name = "brand") { }
            }
        }

        // Assert
        assertNamesWhereEachBelongs(message = failure.message.orEmpty())
    }

    private fun assertNamesWhereEachBelongs(message: String) {
        assertTrue(actual = message.contains("bundle() belongs to the server"), message = message)
        assertTrue(actual = message.contains("family() to the app"), message = message)
    }

    @Test
    fun `given two font bundles with the same version, when declared, then it fails naming the version`() {
        // Arrange
        val location = "https://example.com/ssr/fonts/1/fonts.json"

        // Act
        val failure = assertFailsWith<IllegalArgumentException> {
            extension.fonts { fonts ->
                fonts.bundle(version = "1", location = location)
                fonts.bundle(version = "1", location = location)
            }
        }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains("\"1\""), message = failure.message)
    }
}
