import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import com.whatever.caro.sourceSets
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
            }

            kotlin {
                iosX64()
                iosArm64()
                iosSimulatorArm64()

                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }

                sourceSets {
                    commonMain.dependencies {
                        implementation(libs.library("napier"))
                    }
                }
            }
        }
    }
}

