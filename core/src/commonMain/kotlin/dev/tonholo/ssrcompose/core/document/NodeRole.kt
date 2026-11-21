package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import kotlinx.serialization.Serializable

/**
 * The accessibility role of a node, read from the producer's semantics.
 */
@InternalSsrComposeApi
@Serializable
public enum class NodeRole {
    Button,
    Checkbox,
    Switch,
    RadioButton,
    Tab,
    Image,
    DropdownList,
    ValuePicker,
    Carousel,
}
