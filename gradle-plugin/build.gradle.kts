plugins {
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.publishing)
    `java-gradle-plugin`
}

dependencies {
    implementation(projects.tooling)
    implementation(projects.catalogModel)
}

gradlePlugin {
    plugins {
        register("ssrCompose") {
            id = "dev.tonholo.ssrcompose"
            implementationClass = "dev.tonholo.ssrcompose.gradle.SsrComposePlugin"
        }
    }
}
