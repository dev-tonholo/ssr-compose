plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.publishing)
}

dependencies {
    implementation(projects.core)
    compileOnly(libs.kotlin.compiler.embeddable)
}
