import dev.detekt.gradle.extensions.DetektExtension

plugins {
    id("dev.detekt")
}

extensions.configure<DetektExtension> {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}
