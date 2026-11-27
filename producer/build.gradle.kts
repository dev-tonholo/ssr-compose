plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.compose)
    alias(libs.plugins.ssrcompose.conventions.publishing)
}

kotlin {
    compilerOptions {
        optIn.add("dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi")
    }
}

tasks.test {
    inputs.files(layout.projectDirectory.dir("src"), buildFile)
        .withPropertyName("sourcesReadByTests")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

dependencies {
    api(projects.core)
    api(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.roboto.fonts) { isTransitive = false }

    testImplementation(libs.kotlin.metadata.jvm)
    testRuntimeOnly(libs.skiko.runtime.macos.arm64)
    testRuntimeOnly(libs.skiko.runtime.linux.x64)
}
