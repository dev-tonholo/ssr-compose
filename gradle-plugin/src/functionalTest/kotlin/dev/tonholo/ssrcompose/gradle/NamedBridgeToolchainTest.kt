package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.catalog.testing.measure.SCALE_TAG
import dev.tonholo.ssrcompose.gradle.testing.FixtureIndexes
import dev.tonholo.ssrcompose.gradle.testing.FixtureProject
import dev.tonholo.ssrcompose.gradle.testing.FixtureSettings
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NamedBridgeToolchainTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    @Tag(SCALE_TAG)
    fun `given a shared metadata compile, when common code mentions a component, then both compiles pass`() {
        // Arrange
        val project = FixtureProject(directory = projectDir)
            .addFile(relativePath = "build.gradle.kts", text = KMP_BUILD_SCRIPT)
            .addFile(relativePath = FixtureSources.indexPath(), bytes = FixtureIndexes.withComponents(COMPONENTS))
            .addFile(relativePath = COMMON_SOURCE, text = COMMON_TEXT)
            .addFile(relativePath = JVM_SOURCE, text = FixtureSources.bridgeScreenSource(ALPHA, title = "jvm"))

        // Act
        val result = project.run(METADATA_TASK, JVM_TASK)

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = result.task(":$METADATA_TASK")?.outcome, result.output)
        assertEquals(expected = TaskOutcome.SUCCESS, actual = result.task(":$JVM_TASK")?.outcome, result.output)
        assertEquals(expected = 1, actual = bridgeFunctionCount(classes = projectDir.resolve(JVM_CLASSES)))
    }

    @Test
    fun `given a bridge called only from KSP output, when another file is edited, then the bridge stays`() {
        // Arrange
        val project = FixtureProject(directory = projectDir)
            .addFile(relativePath = "settings.gradle.kts", text = FixtureSettings.script() + KSP_INCLUDES)
            .addFile(relativePath = "processor/build.gradle.kts", text = PROCESSOR_BUILD_SCRIPT)
            .addFile(relativePath = PROCESSOR_SOURCE, text = PROCESSOR_TEXT)
            .addFile(relativePath = PROCESSOR_SERVICE, text = PROCESSOR_PROVIDER)
            .addFile(relativePath = "app/build.gradle.kts", text = KSP_APP_BUILD_SCRIPT)
            .addFile(relativePath = APP_SOURCE, text = APP_TEXT)
            .addFile(
                relativePath = "app/${FixtureSources.indexPath()}",
                bytes = FixtureIndexes.withComponents(COMPONENTS),
            )

        project.run(KSP_COMPILE_TASK)
        project.addFile(relativePath = APP_SOURCE, text = EDITED_APP_TEXT)

        // Act
        val result = project.run(KSP_COMPILE_TASK)

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = result.task(KSP_COMPILE_TASK)?.outcome, result.output)
        val classes = projectDir.resolve(KSP_APP_CLASSES)
        assertTrue(actual = classes.resolve(KSP_SCREEN_CLASS).isFile, message = result.output)
        assertEquals(expected = 1, actual = bridgeFunctionCount(classes = classes))
    }

    @Test
    @Tag(SCALE_TAG)
    fun `given common and jvm code calling one bridge, when compiled, then the bridge is declared once`() {
        // Arrange
        val project = FixtureProject(directory = projectDir)
            .addFile(relativePath = "build.gradle.kts", text = JVM_ONLY_KMP_BUILD_SCRIPT)
            .addFile(relativePath = FixtureSources.indexPath(), bytes = FixtureIndexes.withComponents(COMPONENTS))
            .addFile(relativePath = COMMON_SCREEN, text = screenCalling(screen = "CommonScreen"))
            .addFile(relativePath = JVM_SCREEN, text = screenCalling(screen = "JvmScreen"))

        // Act
        val result = project.run(JVM_TASK)

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = result.task(":$JVM_TASK")?.outcome, result.output)
        val bridgeClass = projectDir.resolve(JVM_CLASSES).resolve(BRIDGE_FOLDER).listFiles().orEmpty().single()
        assertEquals(expected = 1, actual = declarationsOf(component = ALPHA, classFile = bridgeClass))
    }

    @Test
    @Tag(SCALE_TAG)
    fun `given a bridge called only from common code, when a jvm file is edited, then the bridge stays`() {
        // Arrange
        val project = FixtureProject(directory = projectDir)
            .addFile(relativePath = "build.gradle.kts", text = JVM_ONLY_KMP_BUILD_SCRIPT)
            .addFile(relativePath = FixtureSources.indexPath(), bytes = FixtureIndexes.withComponents(COMPONENTS))
            .addFile(relativePath = COMMON_SCREEN, text = screenCalling(screen = "CommonScreen"))
            .addFile(relativePath = JVM_PLAIN, text = JVM_PLAIN_TEXT)
        project.run(JVM_TASK)
        project.addFile(relativePath = JVM_PLAIN, text = EDITED_JVM_PLAIN_TEXT)

        // Act
        val result = project.run(JVM_TASK)

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = result.task(":$JVM_TASK")?.outcome, result.output)
        val bridgeClass = projectDir.resolve(JVM_CLASSES).resolve(BRIDGE_FOLDER).listFiles().orEmpty().single()
        assertEquals(expected = 1, actual = declarationsOf(component = ALPHA, classFile = bridgeClass))
    }

    private fun declarationsOf(component: String, classFile: File): Int {
        val javap = File(System.getProperty(JAVA_HOME_PROPERTY), JAVAP_PATH).path
        val process = ProcessBuilder(javap, "-p", classFile.path).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { output }
        return output.lines().count { line -> line.contains(" void $component(") }
    }

    private fun screenCalling(screen: String): String =
        // language=kotlin
        """
        package app.shared

        import androidx.compose.runtime.Composable
        import ${FixtureSources.DEFAULT_BRIDGE_PACKAGE}.$ALPHA

        @Composable
        fun $screen() {
            $ALPHA(title = "$screen")
        }
        """.trimIndent()

    private fun bridgeFunctionCount(classes: File): Int =
        classes.resolve(BRIDGE_FOLDER).listFiles().orEmpty().count { it.extension == CLASS_EXTENSION }

    private companion object {
        const val ALPHA = "Alpha"
        const val METADATA_TASK = "compileCommonMainKotlinMetadata"
        const val JVM_TASK = "compileKotlinJvm"
        const val KSP_COMPILE_TASK = ":app:compileKotlin"
        const val COMMON_SOURCE = "src/commonMain/kotlin/app/shared/Common.kt"
        const val COMMON_SCREEN = "src/commonMain/kotlin/app/shared/CommonScreen.kt"
        const val JVM_SCREEN = "src/jvmMain/kotlin/app/shared/JvmScreen.kt"
        const val JVM_PLAIN = "src/jvmMain/kotlin/app/shared/JvmPlain.kt"

        // language=kotlin
        const val JVM_PLAIN_TEXT = "package app.shared\n\nfun jvmPlain(): Int = 1\n"

        // language=kotlin
        const val EDITED_JVM_PLAIN_TEXT = "package app.shared\n\nfun jvmPlain(): Int = 2\n"
        const val JAVA_HOME_PROPERTY = "java.home"
        const val JAVAP_PATH = "bin/javap"
        const val JVM_SOURCE = "src/jvmMain/kotlin/app/screens/AlphaScreen.kt"
        const val JVM_CLASSES = "build/classes/kotlin/jvm/main"
        const val KSP_APP_CLASSES = "app/build/classes/kotlin/main"
        const val KSP_SCREEN_CLASS = "app/generated/KspScreenKt.class"
        const val BRIDGE_FOLDER = "app/bridge/v2"
        const val CLASS_EXTENSION = "class"
        const val PROCESSOR_SOURCE = "processor/src/main/kotlin/fixture/ScreenProcessor.kt"
        const val PROCESSOR_SERVICE =
            "processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider"
        const val PROCESSOR_PROVIDER = "fixture.ScreenProcessorProvider"
        const val APP_SOURCE = "app/src/main/kotlin/app/Plain.kt"
        val COMPONENTS = listOf(ALPHA)

        // language=kotlin
        const val COMMON_TEXT = "package app.shared\n\nconst val MENTION: String = \"$ALPHA\"\n"

        // language=kotlin
        const val APP_TEXT = "package app\n\nfun plain(): Int = 1\n"

        // language=kotlin
        const val EDITED_APP_TEXT = "package app\n\nfun plain(): Int = 2\n"

        // language=kotlin
        const val KSP_INCLUDES = "\n\ninclude(\":processor\", \":app\")\n"

        // language=kotlin
        val KMP_BUILD_SCRIPT = """
            plugins {
                alias(libs.plugins.kotlin.multiplatform)
                alias(libs.plugins.kotlin.compose)
                alias(libs.plugins.ssrcompose)
            }

            kotlin {
                jvm()
                js { nodejs() }
                sourceSets.commonMain.dependencies {
                    implementation(libs.compose.runtime)
                }
                sourceSets.jvmMain.dependencies {
                    implementation("dev.tonholo.ssrcompose:producer")
                }
            }

            ssrCompose {
                ${FixtureSources.producerBlock()}
            }
        """.trimIndent()

        // language=kotlin
        val JVM_ONLY_KMP_BUILD_SCRIPT = """
            plugins {
                alias(libs.plugins.kotlin.multiplatform)
                alias(libs.plugins.kotlin.compose)
                alias(libs.plugins.ssrcompose)
            }

            kotlin {
                jvm()
                sourceSets.commonMain.dependencies {
                    implementation("dev.tonholo.ssrcompose:producer")
                }
            }

            ssrCompose {
                ${FixtureSources.producerBlock()}
            }
        """.trimIndent()

        // language=kotlin
        val PROCESSOR_BUILD_SCRIPT = """
            plugins {
                alias(libs.plugins.kotlin.jvm)
            }

            dependencies {
                implementation(libs.ksp.symbol.processing.api)
            }
        """.trimIndent()

        // language=kotlin
        val KSP_APP_BUILD_SCRIPT = """
            plugins {
                alias(libs.plugins.kotlin.jvm)
                alias(libs.plugins.kotlin.compose)
                alias(libs.plugins.ssrcompose)
                alias(libs.plugins.ksp)
            }

            dependencies {
                implementation("dev.tonholo.ssrcompose:producer")
                ksp(project(":processor"))
            }

            ssrCompose {
                ${FixtureSources.producerBlock()}
            }
        """.trimIndent()

        // language=kotlin
        val PROCESSOR_TEXT = """
            package fixture

            import com.google.devtools.ksp.processing.CodeGenerator
            import com.google.devtools.ksp.processing.Dependencies
            import com.google.devtools.ksp.processing.Resolver
            import com.google.devtools.ksp.processing.SymbolProcessor
            import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
            import com.google.devtools.ksp.processing.SymbolProcessorProvider
            import com.google.devtools.ksp.symbol.KSAnnotated

            class ScreenProcessorProvider : SymbolProcessorProvider {
                override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
                    ScreenProcessor(environment.codeGenerator)
            }

            class ScreenProcessor(private val generator: CodeGenerator) : SymbolProcessor {
                private var written = false

                override fun process(resolver: Resolver): List<KSAnnotated> {
                    if (written) return emptyList()
                    written = true
                    generator.createNewFile(Dependencies(aggregating = true), "app.generated", "KspScreen").use { out ->
                        out.write(SCREEN.encodeToByteArray())
                    }
                    return emptyList()
                }
            }

            private val SCREEN = listOf(
                "package app.generated",
                "import androidx.compose.runtime.Composable",
                "import ${FixtureSources.DEFAULT_BRIDGE_PACKAGE}.$ALPHA",
                "@Composable fun KspScreen() { $ALPHA(title = \"ksp\") }",
            ).joinToString(separator = "\n")
        """.trimIndent()
    }
}
