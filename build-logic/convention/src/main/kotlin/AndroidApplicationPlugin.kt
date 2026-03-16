import com.whatever.caro.androidApplication
import com.whatever.caro.libs
import com.whatever.caro.version
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.compose.internal.utils.getLocalProperty
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
                    create(DEV_BUILD_TYPE) {
                        Properties().run {
                            load(FileInputStream(rootProject.file("local.properties")))
                            storeFile = rootProject.file(this["STORE_FILE"] as String)
                            keyAlias = this["KEY_ALIAS"] as String
                            keyPassword = this["KEY_PASSWORD"] as String
                            storePassword = this["STORE_PASSWORD"] as String
                        }
                    }
                    create(PROD_BUILD_TYPE) {
                        Properties().run {
                            load(FileInputStream(rootProject.file("local.properties")))
                            storeFile = rootProject.file(this["STORE_FILE"] as String)
                            keyAlias = this["KEY_ALIAS"] as String
                            keyPassword = this["KEY_PASSWORD"] as String
                            storePassword = this["STORE_PASSWORD"] as String
                        }
                    }
                }

                buildTypes {
                    create(PROD_BUILD_TYPE) {
                        signingConfig = signingConfigs.getByName(PROD_BUILD_TYPE)
                        isDebuggable = false
                        isMinifyEnabled = true
                    }

                    create(DEV_BUILD_TYPE) {
                        initWith(getByName(DEV_BUILD_TYPE))
                        applicationIdSuffix = ".dev"
                        versionNameSuffix = "-dev"
                        isDebuggable = true
                        isMinifyEnabled = false
                        resValue("string", "app_name", "Caro-Dev")
                    }

                    create(QA_BUILD_TYPE) {
                        signingConfig = signingConfigs.getByName(DEV_BUILD_TYPE)
                        applicationIdSuffix = ".qa"
                        versionNameSuffix = "-qa"
                        isDebuggable = true
                        isMinifyEnabled = false
                        resValue("string", "app_name", "Caro-Qa")
                    }
                }
            }
        }
    }

    companion object {
        private const val QA_BUILD_TYPE = "qa"
        private const val DEV_BUILD_TYPE = "dev"
        private const val PROD_BUILD_TYPE = "prod"
    }
}