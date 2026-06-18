import com.whatever.caro.kotlin
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpIosPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            kotlin {
                listOf(
                    iosArm64(),
                    iosSimulatorArm64()
                ).forEach { iosTarget ->
                    iosTarget.binaries.framework {
                        baseName = "ComposeApp"
                        isStatic = false
                        export(project(":core:messaging"))
                        export(project(":core:analytics"))
                        export(project(":core:crashlytics"))
                        export(project(":feature:login"))
                        freeCompilerArgs += "-Xbinary=bundleId=com.whatever.caro"
                    }
                }
            }
        }
    }
}

