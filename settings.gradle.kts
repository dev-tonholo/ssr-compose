pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ssr-compose"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":core")
include(":consumer")
include(":producer")
include(":compiler-plugin")
include(":tooling")
include(":gradle-plugin")
include(":contract-tests")
