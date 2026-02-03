import com.whatever.caro.android
import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

class KmpTestPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("dev.mokkery")
                apply("org.jetbrains.kotlinx.kover")
                apply("io.kotest")
            }

            kotlin {
                android {
                    withHostTest { }
                }

                sourceSets.getByName("androidHostTest") {
                    dependencies {
                        implementation(libs.library("kotest-runner-junit5"))
                    }
                }

                sourceSets.getByName("commonTest") {
                    dependencies {
                        implementation(libs.library("kotest-assertions-core"))
                        implementation(libs.library("kotest-framework-engine"))
                        implementation(libs.library("kotest-extensions-koin"))
                        implementation(libs.library("kotlinx-coroutines-test"))
                        implementation(libs.library("turbine"))
                        implementation(libs.library("koin-test"))
                    }
                }
            }

            tasks.withType<Test>().configureEach {
                if (name == "testAndroidHostTest") {
                    useJUnitPlatform()
                }
            }
        }
    }
}
