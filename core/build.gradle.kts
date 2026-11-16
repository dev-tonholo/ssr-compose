plugins {
    alias(libs.plugins.ssrcompose.conventions.kmp.library)
    alias(libs.plugins.ssrcompose.conventions.serialization)
    alias(libs.plugins.ssrcompose.conventions.publishing)
}

kotlin {
    jvm()

    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
    }
}
