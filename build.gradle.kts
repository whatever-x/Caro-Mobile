import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.stability.analyzer) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kmp.spm) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.mokkery) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.kotest) apply false
    alias(libs.plugins.build.konfig) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.koin.compiler) apply false
}

val ktlintCliVersion = libs.versions.ktlint.cli.get()

subprojects {
    if (!buildFile.isFile) return@subprojects

    apply(plugin = "com.diffplug.spotless")

    configure<SpotlessExtension> {
        kotlin {
            target(
                fileTree("src") {
                    include("**/*.kt")
                    exclude("**/generated/**")
                }
            )
            ktlint(ktlintCliVersion)
        }

        kotlinGradle {
            target("*.gradle.kts")
            ktlint(ktlintCliVersion)
        }

        // Compose Multiplatform string resources: Compose Resources renders any
        // line break injected into <string> text literally at runtime (unlike
        // Android aapt, which collapses whitespace). Raise lineWidth so the WTP
        // formatter never wraps long string values.
        format("xmlComposeResources") {
            target(
                fileTree("src") {
                    include("**/composeResources/**/values/*.xml")
                    exclude("**/generated/**")
                }
            )

            eclipseWtp(EclipseWtpFormatterStep.XML)
                .configFile(rootProject.file("spotless/eclipse-wtp-xml.prefs"))
        }

        format("xml") {
            target(
                fileTree("src") {
                    include(
                        "**/res/values/*.xml",
                        "**/AndroidManifest.xml"
                    )
                    exclude("**/generated/**")
                }
            )

            eclipseWtp(EclipseWtpFormatterStep.XML)
        }
    }
}

// caro.kover 를 적용한 모듈을 자동 수집한다. 새 모듈이 caro.kover 를 적용하면
// 이 목록을 직접 수정하지 않아도 koverAll* 집계 대상에 자동으로 포함된다.
val koverEnabledProjects = mutableListOf<Project>()
subprojects {
    plugins.withId("caro.kover") {
        koverEnabledProjects.add(this@subprojects)
    }
}

/**
 * Kover 적용된 모듈에 대한 XML 리포트 생성
 * 터미널에서 ./gradlew koverAllXmlReport 를 실행
 */
tasks.register("koverAllXmlReport") {
    group = "verification"
    description = "Generate Kover XML reports for all coverage-enabled modules"

    dependsOn(provider { koverEnabledProjects.map { "${it.path}:koverXmlReport" } })
}

/**
 * Kover 적용된 모듈에 대한 검증 실행
 * 터미널에서 ./gradlew koverAllVerify 를 실행
 */
tasks.register("koverAllVerify") {
    group = "verification"
    description = "Verify Kover coverage for all coverage-enabled modules"

    dependsOn(provider { koverEnabledProjects.map { "${it.path}:koverVerify" } })
}
