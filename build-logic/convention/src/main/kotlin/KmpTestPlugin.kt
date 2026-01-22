import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpTestPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("dev.mokkery")
            }

            kotlin {
                sourceSets.getByName("commonTest") {
                    dependencies {
                        // implementation(project(":core:testing"))

                        implementation(kotlin("test"))
                        implementation(kotlin("test-common"))
                        implementation(kotlin("test-annotations-common"))

                        implementation(libs.library("kotlinx-coroutines-test"))
                        implementation(libs.library("turbine"))
                    }
                }
            }
        }
    }
}