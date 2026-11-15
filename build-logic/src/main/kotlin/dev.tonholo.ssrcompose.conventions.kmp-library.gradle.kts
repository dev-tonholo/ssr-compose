import dev.tonholo.ssrcompose.buildlogic.PROJECT_GROUP
import dev.tonholo.ssrcompose.buildlogic.compileSdk
import dev.tonholo.ssrcompose.buildlogic.jvmTarget
import dev.tonholo.ssrcompose.buildlogic.libs
import dev.tonholo.ssrcompose.buildlogic.minSdk
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("dev.tonholo.ssrcompose.conventions.testing")
    id("dev.tonholo.ssrcompose.conventions.detekt")
}

val androidNamespace = "$PROJECT_GROUP.${name.replace(oldValue = "-", newValue = "")}"

kotlin {
    targets.withType<KotlinJvmTarget>().configureEach {
        compilerOptions.jvmTarget.set(libs.jvmTarget)
    }
    android {
        namespace = androidNamespace
        compileSdk = libs.compileSdk
        minSdk = libs.minSdk
        compilerOptions.jvmTarget.set(libs.jvmTarget)
        withHostTest {}
    }
}
