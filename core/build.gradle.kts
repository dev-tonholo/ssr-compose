plugins {
    alias(libs.plugins.ssrcompose.conventions.kmp.library)
    alias(libs.plugins.ssrcompose.conventions.serialization)
    alias(libs.plugins.ssrcompose.conventions.publishing)
}

kotlin {
    compilerOptions {
        optIn.add("dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi")
    }

    jvm()

    sourceSets.commonMain.dependencies {
        api(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
    }
}
