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

                signingConfigs { // TODO : keystore 관리 논의 필요
                    create("release") { }
                    create(DEV_BUILD_TYPE) { }
                    create(QA_BUILD_TYPE) { }
                    create(PROD_BUILD_TYPE) { } // TODO : keyStore 생성 및 등록
                }

                buildTypes {
                    create(PROD_BUILD_TYPE) {
                        signingConfig = signingConfigs.getByName("release") // TODO : keystore 생성 후 변경
                        isDebuggable = false
                        isMinifyEnabled = true
                    }

                    create(DEV_BUILD_TYPE) {
                        initWith(getByName("debug")) // TODO : keystore 생성 후 변경
                        applicationIdSuffix = ".dev"
                        versionNameSuffix = "-dev"
                        isDebuggable = true
                        isMinifyEnabled = false
                        resValue("string", "app_name", "Caro-Dev")
                    }

                    create(QA_BUILD_TYPE) {
                        initWith(getByName("debug")) // TODO : keystore 생성 후 변경
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