plugins {
    id("dev.tonholo.ssrcompose.conventions.kotlin-jvm")
    id("dev.tonholo.ssrcompose.conventions.publishing")
    `java-gradle-plugin`
}

dependencies {
    implementation(projects.tooling)
}

gradlePlugin {
    plugins {
        register("ssrCompose") {
            id = "dev.tonholo.ssrcompose"
            implementationClass = "dev.tonholo.ssrcompose.gradle.SsrComposePlugin"
        }
    }
}
