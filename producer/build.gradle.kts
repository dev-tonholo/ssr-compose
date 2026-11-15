plugins {
    id("dev.tonholo.ssrcompose.conventions.kotlin-jvm")
    id("dev.tonholo.ssrcompose.conventions.compose")
    id("dev.tonholo.ssrcompose.conventions.publishing")
}

dependencies {
    api(projects.core)
    api(libs.compose.runtime)
}
