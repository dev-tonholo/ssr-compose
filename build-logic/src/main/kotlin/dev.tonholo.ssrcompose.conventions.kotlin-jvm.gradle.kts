import dev.tonholo.ssrcompose.buildlogic.libs
import dev.tonholo.ssrcompose.buildlogic.versionOf
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.tonholo.ssrcompose.conventions.testing")
    id("dev.tonholo.ssrcompose.conventions.detekt")
}

val jvmTargetVersion = libs.versionOf("jvm-target")

extensions.configure<KotlinJvmProjectExtension> {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(jvmTargetVersion.toInt())
}
