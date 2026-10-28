import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.whatever.caro.kotlin
import com.whatever.caro.library
import com.whatever.caro.libs
import groovy.json.JsonSlurper
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@OptIn(ExperimentalRoborazziApi::class)
class ScreenshotPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")

            extensions.configure<RoborazziExtension> {
                separateOutputDirs.set(true)
            }

            // JSON provides capture configuration, never executable scenario implementations.
            val configFile = rootProject.layout.projectDirectory.file("scripts/screenshot/capture-config.json")
            val generatedDirectory = layout.buildDirectory.dir("generated/screenshot/commonTest/kotlin")
            val generateConfiguration = tasks.register("generateScreenshotConfiguration") {
                inputs.file(configFile)
                outputs.dir(generatedDirectory)
                doLast {
                    val config = JsonSlurper().parse(configFile.asFile) as Map<*, *>
                    val languages = config["languages"] as List<*>
                    val profiles = config["profiles"] as Map<*, *>
                    val variants = config["scenarioVariants"] as Map<*, *>
                    val contentSized = config["contentSizedScenarios"] as List<*>
                    val output = generatedDirectory.get().file("com/whatever/caro/screenshot/ScreenshotConfiguration.kt").asFile
                    output.parentFile.mkdirs()
                    output.writeText(buildString {
                        appendLine("package com.whatever.caro.screenshot")
                        appendLine("internal object ScreenshotConfiguration {")
                        appendLine("    val languages = listOf(${languages.joinToString { "\"$it\"" }})")
                        appendLine("    val profiles = listOf(")
                        profiles.forEach { (platform, values) ->
                            (values as Map<*, *>).forEach { (variant, value) ->
                                val p = value as Map<*, *>
                                appendLine("        ScreenshotProfile(\"$platform\", \"$variant\", \"${p["id"]}\", ${p["widthPx"]}, ${p["heightPx"]}, ${p["density"]}f, ${p["fontScale"]}f),")
                            }
                        }
                        appendLine("    )")
                        appendLine("    val variants = mapOf(${variants.entries.joinToString { "\"${it.key}\" to \"${it.value}\"" }})")
                        appendLine("    val contentSizedScenarios = setOf(${contentSized.joinToString { "\"$it\"" }})")
                        appendLine("}")
                    })
                }
            }

            kotlin {
                sourceSets.getByName("commonTest") {
                    kotlin.srcDir(rootProject.file("test-support/screenshot/commonTest/kotlin"))
                    kotlin.srcDir(generateConfiguration.map { generatedDirectory.get() })
                    dependencies {
                        implementation(libs.library("compose-ui-test"))
                    }
                }
                sourceSets.getByName("androidHostTest") {
                    kotlin.srcDir(rootProject.file("test-support/screenshot/androidHostTest/kotlin"))
                    dependencies {
                        implementation(libs.library("robolectric"))
                        implementation(libs.library("roborazzi"))
                        implementation(libs.library("roborazzi-compose"))
                        implementation(libs.library("compose-ui-test-junit4"))
                        runtimeOnly(libs.library("compose-ui-test-manifest"))
                        runtimeOnly(libs.library("junit-vintage-engine"))
                    }
                }
                sourceSets.getByName("iosSimulatorArm64Test") {
                    kotlin.srcDir(rootProject.file("test-support/screenshot/iosSimulatorArm64Test/kotlin"))
                    dependencies {
                        implementation(libs.library("roborazzi-compose-ios"))
                    }
                }
            }
        }
    }
}
