import dev.tonholo.ssrcompose.buildlogic.javaRelease
import dev.tonholo.ssrcompose.buildlogic.jvmTarget
import dev.tonholo.ssrcompose.buildlogic.libs
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.tonholo.ssrcompose.conventions.testing")
    id("dev.tonholo.ssrcompose.conventions.detekt")
}

extensions.configure<KotlinJvmProjectExtension> {
    compilerOptions {
        jvmTarget.set(libs.jvmTarget)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(libs.javaRelease)
}
