plugins {
    id("caro.android.application")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android.namespace = "com.whatever.caro"

dependencies {
    implementation(projects.composeApp)
    implementation(projects.core.messaging)

    implementation(libs.koin.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.napier)
    implementation(project.dependencies.platform(libs.firebase.bom.android))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.androidx.startup)
}

android {
    productFlavors.all {
        val testAds = name == "dev" || name == "qa"
        manifestPlaceholders["admobAppId"] =
            if (testAds) "ca-app-pub-3940256099942544~3347511713" else providers.gradleProperty("caro.admob.android.appId").orElse("").get()
        manifestPlaceholders["admobInterstitialId"] =
            if (testAds) {
                "ca-app-pub-3940256099942544/1033173712"
            } else {
                providers
                    .gradleProperty(
                        "caro.admob.android.interstitialId",
                    ).orElse("")
                    .get()
            }
    }
}
