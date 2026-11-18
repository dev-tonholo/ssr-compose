import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.publishing)
    alias(libs.plugins.ssrcompose.conventions.scale.test)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
    sourceSets.test {
        kotlin.srcDir("src/test/resources/fixtures")
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    exclude("**/*.fixture.kt")
}

dependencies {
    implementation(projects.catalogModel)
    compileOnly(libs.kotlin.compiler.embeddable)

    testImplementation(libs.kotlin.compiler.embeddable)
    testImplementation(libs.kotlin.compose.compiler.plugin.embeddable)
    testImplementation(libs.compose.runtime)
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(projects.producer)
    testImplementation(testFixtures(projects.catalogModel))
}

tasks.named<Test>("scaleTest") {
    maxHeapSize = "12g"
}

val namedOnlyProbeJar = tasks.register<Jar>("namedOnlyProbeJar") {
    description = "Packs the named-only bridge prototype so a real Gradle build of the scale experiment can run it."
    archiveClassifier = "named-only-probe"
    from(sourceSets.main.get().output.classesDirs)
    from(sourceSets.test.get().output.classesDirs) {
        include("dev/tonholo/ssrcompose/compiler/testing/scale/**")
    }
    from(layout.projectDirectory.dir("src/namedOnlyProbe/resources"))
}

configurations.consumable("namedOnlyProbe") {
    outgoing.artifact(namedOnlyProbeJar)
}
