package dev.tonholo.ssrcompose.contracttests.fixture

public class ConstructorExposure(document: ScreenDocument) {
    init {
        check(document.hashCode() != 0)
    }
}

public open class Holder<T>

public class SupertypeExposure : Holder<ScreenDocument>()

public class BoundExposure<T : ScreenDocument>

internal class InternalClassExposure(public val document: ScreenDocument)

public fun topLevelExposure(document: ScreenDocument): ScreenDocument = document
