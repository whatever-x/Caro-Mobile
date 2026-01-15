import com.whatever.caro.androidApplication
import com.whatever.caro.libs
import com.whatever.caro.version
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target){
            with(pluginManager){
                apply("com.android.application")
                apply("org.jetbrains.compose")
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<KotlinAndroidProjectExtension>("kotlin") {
                jvmToolchain(17)
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
            }
        }
    }
}