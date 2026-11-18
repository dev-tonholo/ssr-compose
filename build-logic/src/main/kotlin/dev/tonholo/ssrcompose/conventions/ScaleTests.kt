package dev.tonholo.ssrcompose.conventions

import org.gradle.api.tasks.testing.Test
import org.gradle.language.base.plugins.LifecycleBasePlugin

private const val SCALE_TAG = "scale"

/** Leaves tests tagged `scale` out of this task. */
fun Test.skipsScaleTests() {
    useJUnitPlatform { excludeTags(SCALE_TAG) }
}

/** Runs only tests tagged `scale`, on every build, and prints what they measure. */
fun Test.runsOnlyScaleTests() {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    useJUnitPlatform { includeTags(SCALE_TAG) }
    testLogging { showStandardStreams = true }
    outputs.upToDateWhen { false }
}
