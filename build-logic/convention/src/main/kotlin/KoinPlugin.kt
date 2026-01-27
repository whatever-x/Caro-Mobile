import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class KoinPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
            }

            kotlin {
                sourceSets.commonMain.dependencies {
                    implementation(libs.library("koin-core"))
                    implementation(libs.library("koin-annotation"))
                }

                sourceSets.named("commonMain").configure {
                    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
                }
            }

            tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }
                .configureEach {
                    dependsOn("kspCommonMainKotlinMetadata")
                }

            dependencies {
                add("kspCommonMainMetadata", libs.library("koin-ksp-compiler"))
                add("kspAndroid", libs.library("koin-ksp-compiler"))
                add("kspIosX64", libs.library("koin-ksp-compiler"))
                add("kspIosArm64", libs.library("koin-ksp-compiler"))
                add("kspIosSimulatorArm64", libs.library("koin-ksp-compiler"))
            }
        }
    }
}