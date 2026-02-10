enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.jetbrains.kotlinx.kover.aggregation") version "0.9.4"
    id("caro.build.konfig.setting")
}

rootProject.name = "CaroMobile"

include(":androidApp")
include(":composeApp")

// :Core
include(":core:data")
include(":core:model")
include(":core:designsystem")
include(":core:ui")
include(":core:navigator")
include(":core:viewmodel")
include(":core:remote")

// :Feature
include(":feature:home")
include(":feature:login")