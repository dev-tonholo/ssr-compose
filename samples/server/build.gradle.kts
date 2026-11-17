plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    application
}

kotlin {
    jvmToolchain(libs.versions.jvm.target.get().toInt())
}

application {
    mainClass = "dev.tonholo.ssrcompose.samples.server.MainKt"
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(file("../../config/detekt/detekt.yml"))
}

apply(from = "../detekt-source.gradle.kts")
