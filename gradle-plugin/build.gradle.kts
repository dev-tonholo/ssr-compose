plugins {
    `java-gradle-plugin`
    alias(libs.plugins.ssrcompose.conventions.kotlin.jvm)
    alias(libs.plugins.ssrcompose.conventions.publishing)
}

val functionalTest: SourceSet = sourceSets.create("functionalTest")

dependencies {
    implementation(projects.tooling)
    implementation(projects.catalogModel)
    implementation(libs.gradle.idea.ext)
    compileOnly(libs.kotlin.gradle.plugin.api)
    testImplementation(gradleApi())

    "functionalTestImplementation"(gradleTestKit())
    "functionalTestImplementation"(libs.kotlin.test.junit5)

    "functionalTestRuntimeOnly"(projects.compilerPlugin)
    "functionalTestRuntimeOnly"(projects.catalogModel)
    "functionalTestRuntimeOnly"(projects.core)
}

kotlin {
    target.compilations.named("functionalTest") {
        associateWith(target.compilations.getByName("main"))
    }
}

gradlePlugin {
    plugins {
        register("ssrCompose") {
            id = "dev.tonholo.ssrcompose"
            implementationClass = "dev.tonholo.ssrcompose.gradle.SsrComposePlugin"
        }
    }
}

tasks.processResources {
    val coordinates = mapOf(
        "group" to project.group.toString(),
        "version" to project.version.toString(),
        "compilerPluginArtifact" to projects.compilerPlugin.name,
        "coreArtifact" to projects.core.name,
    )
    inputs.properties(coordinates)
    filesMatching("ssr-compose-plugin.properties") {
        expand(coordinates)
    }
}

val functionalTestTask = tasks.register<Test>("functionalTest") {
    description = "Runs the Gradle TestKit tests."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    systemProperty("ssrcompose.repoRoot", rootProject.projectDir.absolutePath)
    inputs.files(rootProject.file("gradle.properties"), rootProject.file("gradle/libs.versions.toml"))
        .withPropertyName("fixtureBuildFiles")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(functionalTestTask)
}
