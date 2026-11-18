package dev.tonholo.ssrcompose.compiler.testing

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File

/** The size of one compiled method, as its class file states it. */
internal data class MethodSize(
    val name: String,
    val descriptor: String,
    val codeLength: Int,
    val maxStack: Int,
    val maxLocals: Int,
) {
    /** The JVM's parameter units of a static method: `long` and `double` take two, every other type one. */
    val parameterUnits: Int
        get() = parameterTypes().sumOf { type -> if (type == "J" || type == "D") WIDE_UNITS else NARROW_UNITS }

    /** The most dimensions of any array type in the descriptor. */
    val arrayDimensions: Int
        get() = ARRAY_PREFIX.findAll(descriptor).maxOfOrNull { it.value.length } ?: 0

    private fun parameterTypes(): List<String> = buildList {
        var position = descriptor.indexOf('(') + 1
        while (descriptor[position] != ')') {
            val type = checkNotNull(PARAMETER_TYPE.matchAt(descriptor, position)) { descriptor }.value
            add(type)
            position += type.length
        }
    }

    private companion object {
        const val WIDE_UNITS = 2
        const val NARROW_UNITS = 1
        val PARAMETER_TYPE = Regex("\\[*(?:[BCDFIJSZ]|L[^;]+;)")
        val ARRAY_PREFIX = Regex("\\[+")
    }
}

/** What one class file states about the limits of chapter 4 of the JVM specification. */
internal data class ClassFileSize(
    val path: String,
    val majorVersion: Int,
    val constantPoolCount: Int,
    val longestUtf8Bytes: Int,
    val interfaceCount: Int,
    val fieldCount: Int,
    val methodCount: Int,
    val methods: List<MethodSize>,
)

/** Reads sizes straight from class files, so the numbers do not depend on the compiler that wrote them. */
internal object ClassFileSizes {
    private const val CLASS_EXTENSION = "class"
    private const val CODE_ATTRIBUTE = "Code"
    private const val HEADER_BYTES_BEFORE_MAJOR_VERSION = 6
    private const val ACCESS_THIS_SUPER_BYTES = 6
    private const val MEMBER_HEADER_BYTES = 6
    private const val TAG_UTF8 = 1
    private const val TAG_INTEGER = 3
    private const val TAG_FLOAT = 4
    private const val TAG_LONG = 5
    private const val TAG_DOUBLE = 6
    private const val TAG_CLASS = 7
    private const val TAG_STRING = 8
    private const val TAG_FIELD_REF = 9
    private const val TAG_METHOD_REF = 10
    private const val TAG_INTERFACE_METHOD_REF = 11
    private const val TAG_NAME_AND_TYPE = 12
    private const val TAG_METHOD_HANDLE = 15
    private const val TAG_METHOD_TYPE = 16
    private const val TAG_DYNAMIC = 17
    private const val TAG_INVOKE_DYNAMIC = 18
    private const val TAG_MODULE = 19
    private const val TAG_PACKAGE = 20
    private const val TWO_BYTES = 2
    private const val THREE_BYTES = 3
    private const val FOUR_BYTES = 4
    private const val EIGHT_BYTES = 8

    /** Reads every class file under [directory]. */
    fun of(directory: File): List<ClassFileSize> =
        directory.walkTopDown()
            .filter { it.isFile && it.extension == CLASS_EXTENSION }
            .map { file -> read(path = file.relativeTo(directory).invariantSeparatorsPath, bytes = file.readBytes()) }
            .toList()

    private fun read(path: String, bytes: ByteArray): ClassFileSize =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            input.skipBytes(HEADER_BYTES_BEFORE_MAJOR_VERSION)
            val majorVersion = input.readUnsignedShort()
            val poolCount = input.readUnsignedShort()
            val pool = readPool(input = input, count = poolCount)
            input.skipBytes(ACCESS_THIS_SUPER_BYTES)
            val interfaceCount = input.readUnsignedShort()
            input.skipBytes(interfaceCount * TWO_BYTES)
            val fieldCount = input.readUnsignedShort()
            repeat(times = fieldCount) { skipField(input = input) }
            val methodCount = input.readUnsignedShort()
            val methods = List(size = methodCount) { readMethod(input = input, utf8 = pool.utf8) }
            ClassFileSize(
                path = path,
                majorVersion = majorVersion,
                constantPoolCount = poolCount,
                longestUtf8Bytes = pool.longestUtf8Bytes,
                interfaceCount = interfaceCount,
                fieldCount = fieldCount,
                methodCount = methodCount,
                methods = methods.filterNotNull(),
            )
        }

    private class Pool(val utf8: Map<Int, String>, val longestUtf8Bytes: Int)

    private fun readPool(input: DataInputStream, count: Int): Pool {
        val utf8 = HashMap<Int, String>()
        var longest = 0
        var index = 1
        while (index < count) {
            val tag = input.readUnsignedByte()
            if (tag == TAG_UTF8) {
                input.mark(TWO_BYTES)
                longest = maxOf(longest, input.readUnsignedShort())
                input.reset()
                utf8[index] = input.readUTF()
            } else {
                input.skipBytes(entryBytes(tag = tag))
            }
            index += if (tag == TAG_LONG || tag == TAG_DOUBLE) 2 else 1
        }
        return Pool(utf8 = utf8, longestUtf8Bytes = longest)
    }

    private fun entryBytes(tag: Int): Int = when (tag) {
        TAG_CLASS, TAG_STRING, TAG_METHOD_TYPE, TAG_MODULE, TAG_PACKAGE -> TWO_BYTES
        TAG_METHOD_HANDLE -> THREE_BYTES
        TAG_INTEGER, TAG_FLOAT, TAG_FIELD_REF, TAG_METHOD_REF, TAG_INTERFACE_METHOD_REF,
        TAG_NAME_AND_TYPE, TAG_DYNAMIC, TAG_INVOKE_DYNAMIC,
        -> FOUR_BYTES
        TAG_LONG, TAG_DOUBLE -> EIGHT_BYTES
        else -> error("Unknown constant pool tag $tag")
    }

    private fun skipField(input: DataInputStream) {
        input.skipBytes(MEMBER_HEADER_BYTES)
        repeat(times = input.readUnsignedShort()) {
            input.skipBytes(TWO_BYTES)
            input.skipBytes(input.readInt())
        }
    }

    private fun readMethod(input: DataInputStream, utf8: Map<Int, String>): MethodSize? {
        input.skipBytes(TWO_BYTES)
        val name = utf8.getValue(input.readUnsignedShort())
        val descriptor = utf8.getValue(input.readUnsignedShort())
        var size: MethodSize? = null
        repeat(times = input.readUnsignedShort()) {
            val attribute = utf8.getValue(input.readUnsignedShort())
            val length = input.readInt()
            if (attribute == CODE_ATTRIBUTE) {
                val maxStack = input.readUnsignedShort()
                val maxLocals = input.readUnsignedShort()
                val codeLength = input.readInt()
                input.skipBytes(length - TWO_BYTES - TWO_BYTES - FOUR_BYTES)
                size = MethodSize(
                    name = name,
                    descriptor = descriptor,
                    codeLength = codeLength,
                    maxStack = maxStack,
                    maxLocals = maxLocals,
                )
            } else {
                input.skipBytes(length)
            }
        }
        return size
    }
}
