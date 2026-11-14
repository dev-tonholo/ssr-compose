plugins {
    id("dev.tonholo.ssrcompose.conventions.android-app")
    id("dev.tonholo.ssrcompose.conventions.serialization")
}

android {
    namespace = "dev.tonholo.ssrcompose.app"

    defaultConfig {
        applicationId = "dev.tonholo.ssrcompose.app"
    }
}

dependencies {
    implementation(projects.protocol)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.kotlinx.rpc.krpc.client)
    implementation(libs.kotlinx.rpc.krpc.ktor.client)
    implementation(libs.remote.core)
    implementation(libs.remote.player.compose)
    implementation(libs.remote.player.core)
    androidTestImplementation(libs.remote.core)
    androidTestImplementation(libs.remote.creation.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
