import com.whatever.caro.composeStabilityAnalyzer
import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class CmpPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("com.github.skydoves.compose.stability.analyzer")
            }

            kotlin {
                jvmToolchain(jdkVersion = 17)

                sourceSets.getByName("commonMain") {
                    dependencies {
                        implementation(libs.library("jetbrains-compose-runtime"))
                        implementation(libs.library("jetbrains-compose-ui"))
                        implementation(libs.library("jetbrains-compose-ui-tooling-preview"))
                        implementation(libs.library("jetbrains-compose-foundation"))
                        implementation(libs.library("jetbrains-compose-material3"))
                        implementation(libs.library("jetbrains-compose-resources"))
                    }
                }

            }

            dependencies {
                "androidRuntimeClasspath"(libs.library("jetbrains-compose-ui-tooling"))
            }

            composeStabilityAnalyzer {
                stabilityValidation {
                    enabled.set(true)
                    ignoredPackages.set(
                        listOf(
                            "com.whatever.caro.feature.${name}.route",
                        )
                    )
                    ignoreNonRegressiveChanges.set(true) // 안정성 저하가 발생하지 않으면 통과
                }
            }

            tasks
                .matching { task ->
                    task.name == "androidMainStabilityCheck" ||
                        task.name == "androidMainStabilityDump"
                }.configureEach {
                    dependsOn("compileAndroidMain")
                    mustRunAfter(
                        tasks.matching { task ->
                            task.name.startsWith("compile") &&
                                task.name != "compileAndroidMain"
                        },
                    )
                }
        }
    }
}
