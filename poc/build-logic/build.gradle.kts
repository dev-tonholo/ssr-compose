plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.compose.compiler.gradle.plugin)
    implementation(libs.kotlin.serialization.gradle.plugin)
    implementation(libs.kotlin.power.assert.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.kotlinx.rpc.gradle.plugin)
}
