import com.whatever.caro.composeStabilityAnalyzer
import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class CmpPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("com.github.skydoves.compose.stability.analyzer")
            }

            kotlin {
                jvmToolchain(jdkVersion = 17)

                sourceSets.getByName("commonMain") {
                    dependencies {
                        implementation(libs.library("jetbrains-compose-runtime"))
                        implementation(libs.library("jetbrains-compose-ui"))
                        implementation(libs.library("jetbrains-compose-ui-tooling-preview"))
                        implementation(libs.library("jetbrains-compose-foundation"))
                        implementation(libs.library("jetbrains-compose-material3"))
                        implementation(libs.library("jetbrains-compose-resources"))
                    }
                }

            }

            dependencies {
                "androidRuntimeClasspath"(libs.library("jetbrains-compose-ui-tooling"))
            }

            composeStabilityAnalyzer {
                stabilityValidation {
                    enabled.set(true)
                    // Route 는 ViewModel 을 받아 태생적으로 UNSTABLE 이라 추적 대상에서 제외한다.
                    // 패키지 경로가 모듈마다 달라(feature.profile.edit, feature.card.detail.route ...)
                    // 패키지 프리픽스 대신 `*Route.kt` 파일명(= 함수명)으로 제외한다.
                    ignoredClasses.set(
                        fileTree("src") { include("**/*Route.kt") }
                            .map { it.name.removeSuffix(".kt") }
                            .sorted()
                    )
                    ignoreNonRegressiveChanges.set(true) // 안정성 저하가 발생하지 않으면 통과
                }
            }
        }
    }
}
