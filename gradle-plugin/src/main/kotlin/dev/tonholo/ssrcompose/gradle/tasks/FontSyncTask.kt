package dev.tonholo.ssrcompose.gradle.tasks

import dev.tonholo.ssrcompose.tooling.fetch.LocationFetcher
import dev.tonholo.ssrcompose.tooling.font.FontSync
import dev.tonholo.ssrcompose.tooling.font.HeldFontBundle
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.options.Option

/** `ssrFontSync`: fetches, checks and pins the font bundles the server holds. */
@UntrackedTask(because = "It fetches from the declared locations and writes the committed ssr/fonts folder.")
internal abstract class FontSyncTask : DefaultTask() {
    /** Each held font version and the location of its `fonts.json`. */
    @get:Input
    abstract val bundles: MapProperty<String, String>

    @get:Input
    @get:Option(option = "update", description = UPDATE_DESCRIPTION)
    abstract val update: Property<Boolean>

    @get:Internal
    abstract val baseFolder: DirectoryProperty

    @get:Internal
    abstract val savedRoot: DirectoryProperty

    @get:Internal
    abstract val lockFile: RegularFileProperty

    init {
        update.convention(false)
    }

    private companion object {
        const val UPDATE_DESCRIPTION = "Replaces a saved font bundle whose fonts.json no longer matches the lock."
    }

    @TaskAction
    fun sync() = runStep {
        FontSync(
            fetcher = LocationFetcher(baseFolder = baseFolder.get().asFile),
            savedRoot = savedRoot.get().asFile,
            lockFile = lockFile.get().asFile,
            report = { message -> logger.warn(message) },
        ).run(
            held = bundles.get().map { (version, location) -> HeldFontBundle(version = version, location = location) },
            update = update.get(),
        )
    }
}
