plugins {
    alias(libs.plugins.ssrcompose.conventions.kmp.library)
    alias(libs.plugins.ssrcompose.conventions.compose)
    alias(libs.plugins.ssrcompose.conventions.serialization)
}

private val FONT_JAR_METADATA = listOf(
    "META-INF/buildinfo.xml",
    "META-INF/services/com.helger.font.api.IFontResourceProviderSPI",
    "LICENSE",
    "NOTICE",
)

private val textGateSources = layout.projectDirectory.dir("src/textGate/kotlin")

kotlin {
    compilerOptions {
        optIn.add("dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi")
    }
    android {
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            packaging.resources.excludes.addAll(FONT_JAR_METADATA)
        }
    }

    sourceSets.getByName("androidHostTest").apply {
        kotlin.srcDir(textGateSources)
        dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.core)
            implementation(projects.consumer)
            implementation(projects.producer)
            implementation(projects.coreTesting)
            implementation(libs.kotlin.metadata.jvm)
        }
    }

    sourceSets.getByName("androidDeviceTest").apply {
        kotlin.srcDir(textGateSources)
        dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.androidx.test.runner)
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    "androidDeviceTestImplementation"(libs.roboto.fonts) { isTransitive = false }
    "androidDeviceTestImplementation"(libs.open.sans.fonts) { isTransitive = false }
}
