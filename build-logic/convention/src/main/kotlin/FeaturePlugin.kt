import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with

class FeaturePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            kotlin {
                sourceSets.getByName("commonMain") {
                    dependencies {
                        implementation(project(":core:designsystem"))
                        implementation(project(":core:data"))
                        implementation(project(":core:ui"))
                        implementation(project(":core:viewmodel"))
                        implementation(project(":core:navigator"))

                        implementation(libs.library("kotlinx-collections-immutable"))
                        implementation(libs.library("koin-compose-viewmodel"))

                        implementation(libs.library("jetbrains-androidx-lifecycle-runtime-compose"))
                    }
                }
            }
        }
    }
}