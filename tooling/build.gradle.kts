plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.publishing)
}

dependencies {
    api(projects.catalogModel)
}
