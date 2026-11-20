plugins {
    alias(libs.plugins.ssrcompose.conventions.kmp.library)
    alias(libs.plugins.ssrcompose.conventions.compose)
    alias(libs.plugins.ssrcompose.conventions.serialization)
}

kotlin {
    compilerOptions {
        optIn.add("dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi")
    }
    android {
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets.commonTest.dependencies {
        implementation(libs.kotlinx.serialization.json)
    }

    sourceSets.getByName("androidHostTest").dependencies {
        implementation(projects.core)
        implementation(projects.consumer)
        implementation(projects.producer)
        implementation(projects.coreTesting)
        implementation(libs.kotlin.metadata.jvm)
    }

    sourceSets.getByName("androidDeviceTest").dependencies {
        implementation(libs.compose.foundation)
        implementation(libs.compose.ui)
        implementation(libs.androidx.compose.ui.test.junit4)
        implementation(libs.androidx.compose.ui.test.manifest)
        implementation(libs.androidx.test.runner)
        implementation(libs.kotlin.test)
    }
}

dependencies {
    "androidDeviceTestImplementation"(libs.roboto.fonts) { isTransitive = false }
    "androidDeviceTestImplementation"(libs.open.sans.fonts) { isTransitive = false }
}
