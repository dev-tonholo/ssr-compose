package dev.tonholo.ssrcompose.gradle.tasks

import dev.tonholo.ssrcompose.tooling.StepException
import org.gradle.api.GradleException

internal inline fun runStep(step: () -> Unit) {
    try {
        step()
    } catch (failure: StepException) {
        throw GradleException(failure.message.orEmpty(), failure)
    }
}
