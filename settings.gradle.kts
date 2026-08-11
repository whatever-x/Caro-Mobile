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
include(":core:util")

// :Feature
include(":feature:home")
include(":feature:login")
include(":feature:splash")
include(":feature:deck")
include(":feature:deck-detail")
include(":feature:setting")
include(":feature:profile")
include(":feature:card")
include(":feature:learning")
