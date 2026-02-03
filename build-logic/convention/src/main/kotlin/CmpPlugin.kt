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
        }
    }
}