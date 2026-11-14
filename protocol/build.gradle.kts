plugins {
    id("dev.tonholo.ssrcompose.conventions.kotlin-jvm")
    id("dev.tonholo.ssrcompose.conventions.serialization")
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.rpc.core)
    api(libs.kotlinx.rpc.krpc.serialization.json)
}
