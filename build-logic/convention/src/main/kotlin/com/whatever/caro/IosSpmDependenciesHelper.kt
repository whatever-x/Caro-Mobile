package com.whatever.caro

import io.github.frankois944.spmForKmp.swiftPackageConfig
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.net.URI

private const val IOS_MIN_VERSION = "15.0"

private const val FIREBASE_IOS_VERSION = "12.14.0"
private const val FIREBASE_IOS_URL = "https://github.com/firebase/firebase-ios-sdk"

private const val GOOGLE_SIGN_IN_IOS_VERSION = "9.1.0"
private const val GOOGLE_SIGN_IN_IOS_URL = "https://github.com/google/GoogleSignIn-iOS"


fun KotlinNativeTarget.configureGoogleSignIn() {
    swiftPackageConfig {
        minIos = IOS_MIN_VERSION

        dependency {
            remotePackageVersion(
                url = URI(GOOGLE_SIGN_IN_IOS_URL),
                version = GOOGLE_SIGN_IN_IOS_VERSION,
                products = {
                    add(
                        "GoogleSignIn", exportToKotlin = true)
                    add("GoogleSignInSwift", exportToKotlin = true)
                },
            )
        }
    }
}

fun KotlinNativeTarget.configureFirebaseAnalytics() {
    swiftPackageConfig {
        minIos = IOS_MIN_VERSION

        dependency {
            remotePackageVersion(
                url = URI(FIREBASE_IOS_URL),
                version = FIREBASE_IOS_VERSION,
                products = {
                    add("FirebaseAnalytics", exportToKotlin = true)
                    add("FirebaseCore", exportToKotlin = true)
                },
            )
        }
    }
}

fun KotlinNativeTarget.configureFirebaseCrashlytics() {
    swiftPackageConfig {
        minIos = IOS_MIN_VERSION

        dependency {
            remotePackageVersion(
                url = URI(FIREBASE_IOS_URL),
                version = FIREBASE_IOS_VERSION,
                products = {
                    add("FirebaseCrashlytics", exportToKotlin = true)
                },
            )
        }
    }
}

fun KotlinNativeTarget.configureFirebaseMessaging() {
    swiftPackageConfig {
        minIos = IOS_MIN_VERSION

        dependency {
            remotePackageVersion(
                url = URI(FIREBASE_IOS_URL),
                version = FIREBASE_IOS_VERSION,
                products = {
                    add("FirebaseMessaging", exportToKotlin = true)
                },
            )
        }
    }
}