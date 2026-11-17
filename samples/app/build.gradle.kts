plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.detekt)
}

android {
    namespace = "dev.tonholo.ssrcompose.samples.app"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        applicationId = "dev.tonholo.ssrcompose.samples.app"
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.compile.sdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    flavorDimensions += "catalog"
    productFlavors {
        create("current") { dimension = "catalog" }
        create("catalogV1") { dimension = "catalog" }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(file("../../config/detekt/detekt.yml"))
}

apply(from = "../detekt-source.gradle.kts")
