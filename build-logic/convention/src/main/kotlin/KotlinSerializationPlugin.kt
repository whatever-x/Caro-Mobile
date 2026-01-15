import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import com.whatever.caro.sourceSets
import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinSerializationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            kotlin {
                sourceSets {
                    commonMain.dependencies {
                        implementation(libs.library("kotlinx-serialization-json"))
                    }
                }
            }
        }
    }
}
