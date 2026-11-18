package dev.tonholo.ssrcompose.catalog.testing.indexformat

import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind
import java.io.DataOutputStream

internal class BinaryBodyWriter(private val idOf: (String) -> Int) {
    fun write(output: DataOutputStream, component: ComponentDescriptor) {
        output.writeInt(component.parameters.size)
        component.parameters.forEach { parameter -> writeParameter(output = output, parameter = parameter) }
    }

    private fun writeParameter(output: DataOutputStream, parameter: Parameter) {
        when (parameter) {
            is Parameter.Value -> {
                val nullable = parameter.nullable
                writeHeader(output = output, tag = PARAMETER_VALUE, parameter = parameter, nullable = nullable)
                writeKind(output = output, kind = parameter.kind)
            }

            is Parameter.Callback -> {
                writeHeader(output = output, tag = PARAMETER_CALLBACK, parameter = parameter, nullable = false)
                writeKinds(output = output, kinds = parameter.arguments)
            }

            is Parameter.Slot -> {
                writeHeader(output = output, tag = PARAMETER_SLOT, parameter = parameter, nullable = false)
                output.writeByte(parameter.accepts.ordinal)
                writeKinds(output = output, kinds = parameter.arguments)
            }
        }
    }

    private fun writeHeader(output: DataOutputStream, tag: Int, parameter: Parameter, nullable: Boolean) {
        output.writeByte(tag)
        output.writeInt(idOf(parameter.name))
        val requiredFlag = if (parameter.required) FLAG_REQUIRED else 0
        val nullableFlag = if (nullable) FLAG_NULLABLE else 0
        output.writeByte(requiredFlag or nullableFlag)
    }

    private fun writeKinds(output: DataOutputStream, kinds: List<ValueKind>) {
        output.writeInt(kinds.size)
        kinds.forEach { kind -> writeKind(output = output, kind = kind) }
    }

    private fun writeKind(output: DataOutputStream, kind: ValueKind) {
        when (kind) {
            ValueKind.Text -> output.writeByte(KIND_TEXT)
            ValueKind.Whole -> output.writeByte(KIND_WHOLE)
            ValueKind.Decimal -> output.writeByte(KIND_DECIMAL)
            ValueKind.Flag -> output.writeByte(KIND_FLAG)
            ValueKind.Bytes -> output.writeByte(KIND_BYTES)
            is ValueKind.Choice -> {
                output.writeByte(KIND_CHOICE)
                output.writeInt(idOf(kind.listName))
            }

            is ValueKind.ListOf -> {
                output.writeByte(KIND_LIST)
                writeKind(output = output, kind = kind.element)
            }
        }
    }
}
