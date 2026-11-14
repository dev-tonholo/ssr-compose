plugins {
    id("dev.tonholo.ssrcompose.conventions.kotlin-jvm")
    id("dev.tonholo.ssrcompose.conventions.serialization")
    id("dev.tonholo.ssrcompose.conventions.compose-runtime")
    application
}

application {
    mainClass.set("dev.tonholo.ssrcompose.server.ApplicationKt")
}

dependencies {
    implementation(projects.protocol)
    implementation(libs.compose.runtime)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.kotlinx.rpc.krpc.server)
    implementation(libs.kotlinx.rpc.krpc.ktor.server)
    implementation(libs.remote.core)
    implementation(libs.remote.creation.core)
    implementation(libs.remote.creation.jvm)
    implementation(libs.slf4j.simple)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.kotlinx.rpc.krpc.client)
    testImplementation(libs.kotlinx.rpc.krpc.ktor.client)
}
