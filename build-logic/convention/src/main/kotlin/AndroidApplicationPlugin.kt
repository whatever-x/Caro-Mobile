import com.whatever.caro.androidApplication
import com.whatever.caro.libs
import com.whatever.caro.version
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.FileInputStream
import java.util.Properties

class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            androidApplication {
                namespace?.let { this.namespace = it }

                compileSdk = libs.version("android-compileSdk").toInt()

                defaultConfig {
                    minSdk = libs.version("android-minSdk").toInt()
                    targetSdk = libs.version("android-targetSdk").toInt()
                    versionCode = libs.version("version-code").toInt()
                    versionName = libs.version("version-name")
                }

                buildFeatures {
                    buildConfig = true
                    resValues = true
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                signingConfigs {
                    getByName("debug") {
                        Properties().run {
                            load(FileInputStream(rootProject.file("local.properties")))
                            storeFile = rootProject.file(this["STORE_FILE"] as String)
                            keyAlias = this["KEY_ALIAS"] as String
                            keyPassword = this["KEY_PASSWORD"] as String
                            storePassword = this["STORE_PASSWORD"] as String
                        }
                    }
                }

                flavorDimensions += "caro"
                productFlavors {
                    create(FLAVOR_DEV) {
                        dimension = "caro"
                        applicationIdSuffix = ".dev"
                        versionNameSuffix = "-dev"
                        resValue("string", "app_name", "Caro-Dev")
                    }
                    create(FLAVOR_QA) {
                        dimension = "caro"
                        applicationIdSuffix = ".qa"
                        versionNameSuffix = "-qa"
                        resValue("string", "app_name", "Caro-Qa")
                    }
                    create(FLAVOR_PRD) {
                        dimension = "caro"
                        resValue("string", "app_name", "Caro")
                    }
                }

                buildTypes {
                    debug {
                        signingConfig = signingConfigs.getByName(BUILD_DEBUG)
                        isDebuggable = true
                        isMinifyEnabled = false
                    }

                    release {
                        signingConfig = signingConfigs.getByName(BUILD_DEBUG)
                        isDebuggable = false
                        isMinifyEnabled = true
                    }
                }
            }
        }
    }

    companion object {
        private const val BUILD_DEBUG = "debug"
        private const val FLAVOR_QA = "qa"
        private const val FLAVOR_DEV = "dev"
        private const val FLAVOR_PRD = "prod"
    }
}