plugins {
    id("dev.tonholo.ssrcompose.conventions.kotlin-jvm")
    id("dev.tonholo.ssrcompose.conventions.publishing")
}

dependencies {
    implementation(projects.core)
    compileOnly(libs.kotlin.compiler.embeddable)
}
