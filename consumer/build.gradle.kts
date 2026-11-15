plugins {
    id("dev.tonholo.ssrcompose.conventions.kmp-library")
    id("dev.tonholo.ssrcompose.conventions.compose")
    id("dev.tonholo.ssrcompose.conventions.publishing")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(projects.core)
        implementation(libs.compose.runtime)
        implementation(libs.compose.foundation)
        implementation(libs.compose.ui)
    }
}
