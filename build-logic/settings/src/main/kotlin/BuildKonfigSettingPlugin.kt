import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class BuildKonfigSettingPlugin : Plugin<Settings> {

    override fun apply(target: Settings) {
        with(target) {
            val cli = gradle.startParameter
                .projectProperties["buildkonfig.flavor"]

            val inferred = inferFromTasks(
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

    private fun inferFromTasks(taskNames: List<String>): String? {
        val tasks = taskNames.map {
            it.substringAfterLast(":")
        }

        val flavors = buildSet {
            if (tasks.any { it.containsVariant("Dev") }) {
                add("dev")
            }

            if (tasks.any { it.containsVariant("Qa") }) {
                add("qa")
            }

            if (tasks.any { it.containsVariant("Prod") }) {
                add("prod")
            }
        }

        return when (flavors.size) {
            0 -> null

            1 -> flavors.single()

            else -> error(
                "Multiple BuildKonfig flavors detected: $flavors. " +
                        "Specify one explicitly with " +
                        "-Pbuildkonfig.flavor=<dev|qa|prod>"
            )
        }
    }

    private fun String.containsVariant(flavor: String): Boolean =
        contains("${flavor}Debug", ignoreCase = true) ||
                contains("${flavor}Release", ignoreCase = true)
}