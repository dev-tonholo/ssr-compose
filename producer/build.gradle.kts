plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.compose)
    alias(libs.plugins.ssrcompose.conventions.publishing)
    alias(libs.plugins.ssrcompose.conventions.scale.test)
}

kotlin {
    compilerOptions {
        optIn.add("dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi")
    }
}

tasks.named<Test>("scaleTest") {
    providers.gradleProperty("memoryScreens").orNull?.let { systemProperty("ssrcompose.memory.screens", it) }
}

val robotoFacesLoadedTestClass = "*RobotoFacesLoadedFirstScreenTest"

tasks.test {
    filter { excludeTestsMatching(robotoFacesLoadedTestClass) }
    inputs.files(layout.projectDirectory.dir("src"), buildFile)
        .withPropertyName("sourcesReadByTests")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

val robotoFacesLoadedTest = tasks.register<Test>("robotoFacesLoadedTest") {
    description = "Checks, in a JVM of its own, that every Roboto face is loaded before a screen shows it."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching(robotoFacesLoadedTestClass) }
}

tasks.check {
    dependsOn(robotoFacesLoadedTest)
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
