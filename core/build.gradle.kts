plugins {
    id("dev.tonholo.ssrcompose.conventions.kmp-library")
    id("dev.tonholo.ssrcompose.conventions.serialization")
    id("dev.tonholo.ssrcompose.conventions.publishing")
}

kotlin {
    jvm()

    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.serialization.json)
    }
}
