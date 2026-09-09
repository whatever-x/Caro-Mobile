import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class BuildKonfigSettingPlugin : Plugin<Settings> {

    override fun apply(target: Settings) {
        with(target) {
            val cli = gradle.startParameter
                .projectProperties["buildkonfig.flavor"]

            val inferred = inferVariantFromTasks(
                gradle.startParameter.taskNames,
            )

            gradle.beforeProject {
                val value = cli ?: inferred ?: return@beforeProject

                if (path == ":") {
                    println("buildkonfig.flavor decided = $value")
                }

                if (findProperty("buildkonfig.flavor") == null) {
                    extensions.extraProperties["buildkonfig.flavor"] = value
                }
            }
        }
    }

    private fun inferVariantFromTasks(taskNames: List<String>): String? {
        val tasks = taskNames.map {
            it.substringAfterLast(":")
        }

        val variants = buildSet {
            listOf("Dev", "Qa", "Prod").forEach { flavor ->
                listOf("Debug", "Release").forEach { buildType ->
                    val variant = flavor + buildType
                    if (tasks.any { it.containsVariant(variant) }) {
                        add(flavor.lowercase() + buildType)
                    }
                }
            }
        }

        return when (variants.size) {
            0 -> null

            1 -> variants.single()

            else -> error(
                "Multiple BuildKonfig variants detected: $variants. " +
                        "Specify one explicitly with " +
                        "-Pbuildkonfig.flavor=<devDebug|devRelease|qaDebug|qaRelease|prodDebug|prodRelease>"
            )
        }
    }

    private fun String.containsVariant(variant: String): Boolean =
        contains(variant, ignoreCase = true)
}
