plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.serialization)
    alias(libs.plugins.ssrcompose.conventions.publishing)
    alias(libs.plugins.ssrcompose.conventions.scale.test)
}

kotlin {
    compilerOptions {
        optIn.add("dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi")
    }
}

dependencies {
    api(projects.catalogModel)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(testFixtures(projects.catalogModel))
}

tasks.named<Test>("scaleTest") {
    maxHeapSize = "2g"
}
