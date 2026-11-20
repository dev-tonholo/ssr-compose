plugins {
    alias(libs.plugins.ssrcompose.conventions.kmp.library)
}

kotlin {
    compilerOptions {
        optIn.add("dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi")
    }

    sourceSets.commonTest.dependencies {
        implementation(projects.core)
        implementation(projects.consumer)
        implementation(projects.producer)
    }
}
