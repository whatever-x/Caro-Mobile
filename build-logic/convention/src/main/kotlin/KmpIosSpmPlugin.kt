import com.whatever.caro.kotlin
import io.github.frankois944.spmForKmp.swiftPackageConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.net.URI

class KmpIosSpmPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            kotlin {
                listOf(
                    iosArm64(),
                    iosSimulatorArm64(),
                ).forEach { iosTarget ->

                    iosTarget.swiftPackageConfig {
                        minIos = "15.0"

                        dependency {
                            remotePackageVersion(
                                url = URI(
                                    "https://github.com/firebase/firebase-ios-sdk"
                                ),
                                version = "12.14.0",
                                products = {
                                    add("FirebaseCore")
                                    add("FirebaseAnalytics")
                                    add("FirebaseCrashlytics")
                                    add("FirebaseMessaging")
                                },
                            )

                            remotePackageVersion(
                                url = URI(
                                    "https://github.com/google/GoogleSignIn-iOS"
                                ),
                                version = "9.1.0",
                                products = {
                                    add("GoogleSignIn")
                                },
                            )
                        }

                        exportedPackageSettings {
                            name = "CaroNativeDependencies"

                            includeProduct = listOf(
                                "FirebaseCore",
                                "FirebaseAnalytics",
                                "FirebaseCrashlytics",
                                "FirebaseMessaging",
                                "GoogleSignIn",
                            )
                        }
                    }
                }
            }
        }
    }
}