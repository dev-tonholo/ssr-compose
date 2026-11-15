import dev.tonholo.ssrcompose.buildlogic.ANDROID_HOST_TEST_SOURCE_SET

plugins {
    alias(libs.plugins.ssrcompose.conventions.kmp.library)
}

kotlin {
    sourceSets.getByName(ANDROID_HOST_TEST_SOURCE_SET).dependencies {
        implementation(projects.core)
        implementation(projects.consumer)
        implementation(projects.producer)
    }
}
