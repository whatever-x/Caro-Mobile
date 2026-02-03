import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kmp.spm) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.mokkery) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.kotest) apply false
}

val ktlintCliVersion = libs.versions.ktlint.cli.get()

subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude(
                "**/build/**",
                "**/generated/**"
            )
            ktlint(ktlintCliVersion)
        }

        kotlinGradle {
            target("**/*.gradle.kts")
            ktlint(ktlintCliVersion)
        }

        format("xml") {
            target(
                "src/**/values/*.xml",
                "src/**/AndroidManifest.xml"
            )
            targetExclude(
                "**/build/**",
                "**/generated/**"
            )

            eclipseWtp(EclipseWtpFormatterStep.XML)
        }
    }
}