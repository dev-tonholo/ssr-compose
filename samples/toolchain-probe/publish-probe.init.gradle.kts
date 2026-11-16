import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

val modulesWithoutPublication = setOf(":compiler-plugin", ":catalog-model")

allprojects {
    if (path in modulesWithoutPublication) {
        afterEvaluate {
            extensions.configure<PublishingExtension> {
                publications.create("probe", MavenPublication::class.java) {
                    from(components["java"])
                }
            }
        }
    }
}
