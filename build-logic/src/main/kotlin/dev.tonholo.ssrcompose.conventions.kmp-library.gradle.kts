import dev.tonholo.ssrcompose.buildlogic.libs
import dev.tonholo.ssrcompose.buildlogic.versionOf
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("dev.tonholo.ssrcompose.conventions.testing")
    id("dev.tonholo.ssrcompose.conventions.detekt")
}

val jvmTargetVersion = JvmTarget.fromTarget(libs.versionOf("jvm-target"))
val androidNamespace = "dev.tonholo.ssrcompose.${name.replace("-", "")}"

kotlin {
    targets.withType<KotlinJvmTarget>().configureEach {
        compilerOptions.jvmTarget.set(jvmTargetVersion)
    }
    android {
        namespace = androidNamespace
        compileSdk = libs.versionOf("compile-sdk").toInt()
        minSdk = libs.versionOf("min-sdk").toInt()
        compilerOptions.jvmTarget.set(jvmTargetVersion)
    }
}
