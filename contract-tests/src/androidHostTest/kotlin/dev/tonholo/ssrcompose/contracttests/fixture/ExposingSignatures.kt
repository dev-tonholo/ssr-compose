package dev.tonholo.ssrcompose.contracttests.fixture

public class ExposingSignatures {
    public fun multiLineParameter(
        label: String,
        document: ScreenDocument,
        count: Int,
    ) {
        check(label.length + count >= 0 && document.hashCode() != 0)
    }

    public fun listParameter(documents: List<ScreenDocument>) {
        check(documents.size >= 0)
    }

    public val documentsByName: Map<String, ScreenDocument> = emptyMap()

    public fun listReturn(): List<ScreenDocument> = emptyList()

    public fun deeplyNested(documents: Map<String, List<ScreenDocument?>>) {
        check(documents.size >= 0)
    }

    public fun <T : ScreenDocument> boundedTypeParameter(value: T): T = value

    public fun ScreenDocument.receiver(): Int = hashCode()

    public fun varargParameter(vararg documents: ScreenDocument) {
        check(documents.size >= 0)
    }

    public val ScreenDocument.receiverProperty: Int
        get() = hashCode()

    public val <T : ScreenDocument> T.boundedProperty: Int
        get() = hashCode()

    internal fun internalParameter(document: ScreenDocument) {
        check(document.hashCode() != 0)
    }

    private fun privateParameter(document: ScreenDocument) {
        check(document.hashCode() != 0)
    }

    public fun unmarkedParameter(documents: List<String>) {
        check(documents.size >= 0)
    }
}
