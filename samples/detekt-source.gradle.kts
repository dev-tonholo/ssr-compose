import org.gradle.api.tasks.SourceTask

tasks.withType<SourceTask>().matching { it.name == "detekt" }.configureEach {
    setSource(fileTree("src") { exclude("*/resources/**") })
}
