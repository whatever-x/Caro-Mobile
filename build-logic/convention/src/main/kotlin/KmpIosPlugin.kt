import com.whatever.caro.kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpIosPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            kotlin {
                listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64()
                ).forEach { iosTarget ->
                    iosTarget.binaries.framework {
                        baseName = "ComposeApp"
                        isStatic = true
                        export(project(":core:messaging"))
                        freeCompilerArgs += "-Xbinary=bundleId=com.whatever.caro"
                    }
                }
            }
        }
    }
}

