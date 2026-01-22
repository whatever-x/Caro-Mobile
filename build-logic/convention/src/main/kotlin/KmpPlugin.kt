import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
            }

            kotlin {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }

                sourceSets.getByName("commonMain") {
                    dependencies {
                        implementation(libs.library("napier"))
                    }
                }
            }
        }
    }
}

