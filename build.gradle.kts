import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep

plugins {
    alias(libs.plugins.android.application) apply false
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

        format("xml") {
            target(
                fileTree("src") {
                    include(
                        "**/values/*.xml",
                        "**/AndroidManifest.xml"
                    )
                    exclude("**/generated/**")
                }
            )

            eclipseWtp(EclipseWtpFormatterStep.XML)
        }
    }
}

/**
 * Kover 적용된 모듈에 대한 XML 리포트 생성
 * 터미널에서 ./gradlew koverAllXmlReport 를 실행
 */
tasks.register("koverAllXmlReport") {
    group = "verification"
    description = "Generate Kover XML reports for all coverage-enabled modules"

    dependsOn(
        ":feature:login:koverXmlReport",
        ":feature:home:koverXmlReport",
        ":core:data:koverXmlReport"
    )
}

/**
 * Kover 적용된 모듈에 대한 검증 실행
 * 터미널에서 ./gradlew koverAllVerify 를 실행
 */
tasks.register("koverAllVerify") {
    group = "verification"
    description = "Verify Kover coverage for all coverage-enabled modules"

    dependsOn(
        ":feature:login:koverVerify",
        ":feature:home:koverVerify",
        ":core:data:koverVerify"
    )
}
