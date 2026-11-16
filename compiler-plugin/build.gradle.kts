import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.publishing)
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
}
