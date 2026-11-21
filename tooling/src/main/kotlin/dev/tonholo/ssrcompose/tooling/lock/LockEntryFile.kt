package dev.tonholo.ssrcompose.tooling.lock

import kotlinx.serialization.Serializable

/** One entry of a lock file as it is written. */
@Serializable
internal data class LockEntryFile(
    val version: String,
    val location: String,
    val fingerprint: String,
) {
    fun toEntry(): LockEntry = LockEntry(version = version, location = location, fingerprint = fingerprint)

    companion object {
        fun of(entry: LockEntry): LockEntryFile =
            LockEntryFile(version = entry.version, location = entry.location, fingerprint = entry.fingerprint)
    }
}
