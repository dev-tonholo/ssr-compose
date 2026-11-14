import dev.detekt.gradle.extensions.DetektExtension

plugins {
    id("dev.detekt")
}

extensions.configure<DetektExtension> {
    buildUponDefaultConfig = true
}
