import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class KoinPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("io.insert-koin.compiler.plugin")
            }

            kotlin {
                sourceSets.getByName("commonMain") {
                    dependencies {
                        implementation(libs.library("koin-core"))
                        implementation(libs.library("koin-annotation"))
                    }
                }
            }
        }
    }
}