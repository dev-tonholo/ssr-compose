package dev.tonholo.ssrcompose.producer.testing

import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction
import org.jetbrains.skia.impl.Stats

private class AllocationLog : ConcurrentHashMap<String, Int>() {
    val allocatedClasses: MutableSet<String> = newKeySet()

    override fun merge(key: String, value: Int, remappingFunction: BiFunction<in Int, in Int, out Int?>): Int? {
        if (value > 0) allocatedClasses.add(key)
        return super.merge(key, value, remappingFunction)
    }
}

/**
 * The simple names of the Skia classes whose native objects Skiko made while [block] ran, from Skiko's own count.
 * The count is one for the process, so no other test may use Skiko at the same time.
 */
internal fun skiaClassesAllocatedBy(block: () -> Unit): Set<String> {
    val log = AllocationLog()
    val countedBefore = Stats.allocated
    val enabledBefore = Stats.enabled
    Stats.allocated = log
    Stats.enabled = true
    try {
        block()
    } finally {
        Stats.enabled = enabledBefore
        Stats.allocated = countedBefore
    }
    return log.allocatedClasses.toSet()
}
