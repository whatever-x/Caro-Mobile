import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class BuildKonfigSettingPlugin : Plugin<Settings> {

    override fun apply(target: Settings) {
        with(target) {
            val cli = gradle.startParameter.projectProperties["buildkonfig.flavor"]
            val inferred = inferFromTasks(gradle.startParameter.taskNames)

            gradle.beforeProject {
                val value = cli ?: inferred ?: return@beforeProject

                if (path == ":") println("buildkonfig.flavor decided = $value")

                if (findProperty("buildkonfig.flavor") == null) {
                    extensions.extraProperties["buildkonfig.flavor"] = value
                }
            }
        }
    }

    private fun inferFromTasks(taskNames: List<String>): String? {
        val tasks = taskNames.joinToString(" ")

        fun has(vararg patterns: String): Boolean =
            patterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(tasks) }

        return when {
            has(""":androidApp:.*\bDev\b""", """\bassembleDev\b""", """\binstallDev\b""", """\btestDev\b""") -> "dev"
            has(""":androidApp:.*\bQa\b""",  """\bassembleQa\b""",  """\binstallQa\b""",  """\btestQa\b""") -> "qa"
            has(""":androidApp:.*\bProd\b""", """\bassembleProd\b""", """\binstallProd\b""", """\btestProd\b""") -> "prod"
            else -> null
        }
    }

}