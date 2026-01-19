import com.whatever.caro.android
import com.whatever.caro.kotlin
import com.whatever.caro.libs
import com.whatever.caro.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class KmpAndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.kotlin.multiplatform.library")
            }

            kotlin {
                android {
                    namespace?.let { namespace = it }

                    compileSdk = libs.version("android-compileSdk").toInt()
                    minSdk = libs.version("android-minSdk").toInt()

                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }

                    androidResources {
                        enable = true
                    }
                }
            }
        }
    }
}