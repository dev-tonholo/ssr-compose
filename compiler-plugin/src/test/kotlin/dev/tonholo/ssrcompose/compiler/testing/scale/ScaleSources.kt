package dev.tonholo.ssrcompose.compiler.testing.scale

import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter

/** The Kotlin sources the scale experiment compiles. */
internal object ScaleSources {
    private const val LINE_BREAK = "\n"

    /** A screen that calls each of [components] from the bridge in [bridgePackage]. */
    fun bridgeCaller(components: List<ComponentDescriptor>, bridgePackage: String): String {
        val imports = components.joinToString(separator = LINE_BREAK) { component ->
            // language=kotlin
            val import = "import $bridgePackage.${component.name}"
            import
        }
        // language=kotlin
        return """
package scale.caller

import androidx.compose.runtime.Composable
$imports

${screenOf(components = components)}
"""
    }

    /** The same screen, calling hand-written functions with the signatures of [components]. */
    fun standInCaller(components: List<ComponentDescriptor>): String {
        val standIns = components.joinToString(separator = LINE_BREAK, transform = ::standInOf)
        // language=kotlin
        return """
package scale.caller

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.producer.RemoteAction1

$standIns

${screenOf(components = components)}
"""
    }

    /** A module with [functions] composables of [calls] calls each, none of them to a bridge. */
    fun manyCalls(functions: Int, calls: Int): String {
        val body = List(size = calls) { index ->
            // language=kotlin
            val call = "    Leaf(value = \"$index\")"
            call
        }.joinToString(separator = LINE_BREAK)
        val callers = List(size = functions) { index ->
            // language=kotlin
            val caller = "@Composable\nfun Caller$index() {\n$body\n}"
            caller
        }.joinToString(separator = LINE_BREAK)
        // language=kotlin
        return """
package scale.many

import androidx.compose.runtime.Composable

@Composable
fun Leaf(value: String) = Unit

$callers
"""
    }

    private fun screenOf(components: List<ComponentDescriptor>): String {
        val calls = components.joinToString(separator = LINE_BREAK, transform = ::callOf)
        // language=kotlin
        val screen = "@Composable\nfun Screen() {\n$calls\n}"
        return screen
    }

    private fun callOf(component: ComponentDescriptor): String {
        val arguments = component.parameters.filter { it.required }.joinToString(transform = ::argumentOf)
        // language=kotlin
        val call = "    ${component.name}($arguments)"
        return call
    }

    private fun argumentOf(parameter: Parameter): String {
        // language=kotlin
        val textArgument = "${parameter.name} = \"value\""
        // language=kotlin
        val callbackArgument = "${parameter.name} = { _ -> }"
        // language=kotlin
        val slotArgument = "${parameter.name} = { }"
        return when (parameter) {
            is Parameter.Value -> textArgument
            is Parameter.Callback -> callbackArgument
            is Parameter.Slot -> slotArgument
        }
    }

    private fun standInOf(component: ComponentDescriptor): String {
        val parameters = component.parameters.joinToString(transform = ::standInParameterOf)
        // language=kotlin
        val standIn = "@Composable\nfun ${component.name}($parameters) = Unit"
        return standIn
    }

    private fun standInParameterOf(parameter: Parameter): String {
        // language=kotlin
        val requiredText = "${parameter.name}: String"
        // language=kotlin
        val optionalText = "${parameter.name}: String? = null"
        // language=kotlin
        val callback = "${parameter.name}: RemoteAction1<String>"
        // language=kotlin
        val requiredSlot = "${parameter.name}: @Composable () -> Unit"
        // language=kotlin
        val optionalSlot = "${parameter.name}: (@Composable () -> Unit)? = null"
        return when (parameter) {
            is Parameter.Value -> if (parameter.required) requiredText else optionalText
            is Parameter.Callback -> callback
            is Parameter.Slot -> if (parameter.required) requiredSlot else optionalSlot
        }
    }
}
