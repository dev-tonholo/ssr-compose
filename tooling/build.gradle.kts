plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.serialization)
    alias(libs.plugins.ssrcompose.conventions.publishing)
    alias(libs.plugins.ssrcompose.conventions.scale.test)
}

dependencies {
    api(projects.catalogModel)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(testFixtures(projects.catalogModel))
}

tasks.named<Test>("scaleTest") {
    maxHeapSize = "2g"
}
