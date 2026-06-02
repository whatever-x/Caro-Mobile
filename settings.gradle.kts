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
include(":core:datastore")
include(":core:analytics")
include(":core:crashlytics")
include(":core:messaging")

// :Feature
include(":feature:home")
include(":feature:login")
include(":feature:splash")
include(":core:util")
