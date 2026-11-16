plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(libs.versions.jvm.target.get().toInt())
}

application {
    mainClass = "dev.tonholo.ssrcompose.samples.server.MainKt"
}
