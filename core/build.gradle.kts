plugins {
    alias(libs.plugins.ssrcompose.conventions.kmp.library)
    alias(libs.plugins.ssrcompose.conventions.serialization)
    alias(libs.plugins.ssrcompose.conventions.publishing)
}

kotlin {
    jvm()

    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.serialization.json)
    }
}
