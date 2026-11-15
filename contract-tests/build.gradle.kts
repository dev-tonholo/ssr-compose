plugins {
    alias(libs.plugins.ssrcompose.conventions.kmp.library)
}

kotlin {
    sourceSets.commonTest.dependencies {
        implementation(projects.core)
        implementation(projects.consumer)
        implementation(projects.producer)
    }
}
