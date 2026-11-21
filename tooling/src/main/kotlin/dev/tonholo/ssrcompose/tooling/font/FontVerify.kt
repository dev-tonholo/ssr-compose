package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.tooling.StepException
import java.io.File

/** `ssrFontVerify`: checks the saved font bundles against `ssr/fonts.lock` and their own `fonts.json`. */
public object FontVerify {
    /**
     * @param versions the declared font versions.
     * @throws StepException naming every problem found: a declared version with no saved copy or no entry in the
     * lock, a `fonts.json` that does not match the lock or is for another version than its folder, a symbolic link
     * in a saved folder, and a font file that is missing, over its size cap, does not match its fingerprint, or is
     * not listed.
     */
    public fun run(versions: List<String>, savedRoot: File, lockFile: File) {
        val pins = fontLock.read(file = lockFile).associateBy { it.version }
        val problems = versions.map { fontVersionOf(text = it).value }.flatMap { version ->
            SavedBundleCheck(
                version = version,
                saved = savedRoot.resolve(version),
                pin = pins[version],
                reportUnlistedFiles = true,
            ).problems()
        }
        if (problems.isNotEmpty()) {
            throw StepException(
                message = "The saved font bundles do not match ${lockFile.name}:\n" +
                    problems.joinToString(separator = "\n") { "- $it" } +
                    "\nRun ${FontStepNames.SYNC} --update to fetch them again.",
            )
        }
    }
}
