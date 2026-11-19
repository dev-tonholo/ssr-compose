package dev.tonholo.ssrcompose.consumer.testing

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.consumer.text.SsrComposeStringResource
import dev.tonholo.ssrcompose.consumer.text.SsrComposeStringResourceManager

private const val MARK = "app text for "

/** An app's manager that answers every key with a text no library text can equal. */
object MarkedStringResourceManager : SsrComposeStringResourceManager {
    fun markedTextOf(resource: SsrComposeStringResource): String = MARK + resource.name

    @Composable
    override fun stringResource(resource: SsrComposeStringResource, vararg arguments: Any): String =
        markedTextOf(resource = resource)
}
