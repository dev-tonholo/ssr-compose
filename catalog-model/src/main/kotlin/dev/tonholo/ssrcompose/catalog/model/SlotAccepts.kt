package dev.tonholo.ssrcompose.catalog.model

/**
 * Who owns the composables a slot accepts. It is recorded in the catalog and not checked yet.
 */
public enum class SlotAccepts(internal val fileName: String) {
    Any(fileName = "any"),
    Consumer(fileName = "consumer"),
    Producer(fileName = "producer"),
}
