plugins {
    id("dev.tonholo.ssrcompose.conventions.kmp-library")
}

kotlin {
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(projects.core)
        implementation(projects.consumer)
        implementation(projects.producer)
    }
}
