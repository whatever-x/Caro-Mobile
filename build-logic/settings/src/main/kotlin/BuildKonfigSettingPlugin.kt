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
            has(
                """:androidApp:.*\bDev\b""",
                """\bassembleDevDebug\b""",
                """\bassembleDevRelease\b""",
                """\binstallDevDebug\b""",
                """\binstallDevRelease\b""",
                """\btestDevDebug\b""",
                """\btestDevRelease\b""",
            ) -> "dev"

            has(
                """:androidApp:.*\bDev\b""",
                """\bassembleQaDebug\b""",
                """\bassembleQaRelease\b""",
                """\binstallQaDebug\b""",
                """\binstallQaRelease\b""",
                """\btestQaDebug\b""",
                """\btestQaRelease\b""",
            ) -> "qa"

            has(
                """:androidApp:.*\bProd\b""",
                """\bassembleProdDebug\b""",
                """\bassembleProdRelease\b""",
                """\binstallProdDebug\b""",
                """\binstallProdRelease\b""",
                """\btestProdDebug\b""",
                """\btestProdRelease\b""",
            ) -> "prod"

            else -> null
        }
    }

}