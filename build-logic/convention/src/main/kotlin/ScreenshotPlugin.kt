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
                sourceSets.getByName("androidHostTest") {
                    dependencies {
                        implementation(libs.library("robolectric"))
                        implementation(libs.library("roborazzi"))
                        implementation(libs.library("roborazzi-compose"))
                        implementation(libs.library("compose-ui-test-junit4"))
                        runtimeOnly(libs.library("compose-ui-test-manifest"))
                        runtimeOnly(libs.library("junit-vintage-engine"))
                    }
                }
                sourceSets.getByName("iosSimulatorArm64Test") {
                    dependencies {
                        implementation(libs.library("compose-ui-test"))
                        implementation(libs.library("roborazzi-compose-ios"))
                    }
                }
            }
        }
    }
}
