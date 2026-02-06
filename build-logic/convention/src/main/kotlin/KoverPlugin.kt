import com.whatever.caro.kover
import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.api.GradleException

class KoverPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlinx.kover")
            }

            val isDataModule = path == ":core:data"
            val isFeatureModule = path.startsWith(":feature:")

            if (!isDataModule && !isFeatureModule) {
                throw GradleException(
                    "caro.kover 플러그인은 ':core:data' 또는 ':feature:*' 모듈에서만 사용합니다. " +
                            "현재 모듈 경로: $path"
                )
            }

            kover {
                reports {
                    filters {
                        excludes {
                            packages("org.koin.ksp.generated.*")
                            packages("*.ksp.*")
                        }

                        includes {
                            when {
                                path == ":core:data" -> classes("**RepositoryImpl*")
                                path.startsWith(":feature:") -> classes("**ViewModel*")
                            }
                        }
                    }

                    verify {
                        rule {
                            bound {
                                coverageUnits.set(CoverageUnit.LINE)
                                minValue.set(COVERAGE_THRESHOLD)
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val COVERAGE_THRESHOLD = 50 // TODO : 임시 50%로 설정 추후 80으로 변경
    }

}