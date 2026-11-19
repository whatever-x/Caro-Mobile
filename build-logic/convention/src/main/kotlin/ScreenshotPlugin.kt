import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@OptIn(ExperimentalRoborazziApi::class)
class ScreenshotPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")

            extensions.configure<RoborazziExtension> {
                separateOutputDirs.set(true)
            }

            kotlin {
                sourceSets.getByName("commonTest") {
                    dependencies {
                        implementation(project(":core:screenshot-testing"))
                    }
                }
                sourceSets.getByName("androidHostTest") {
                    dependencies {
                        runtimeOnly(libs.library("compose-ui-test-manifest"))
                        runtimeOnly(libs.library("junit-vintage-engine"))
                    }
                }
            }
        }
    }
}
