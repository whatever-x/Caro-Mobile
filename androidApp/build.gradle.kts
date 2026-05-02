plugins {
    id("caro.android.application")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android.namespace = "com.whatever.caro.app"

dependencies {
    implementation(projects.composeApp)

    implementation(libs.koin.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.napier)
    implementation(project.dependencies.platform(libs.firebase.bom.android))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    implementation(libs.androidx.startup)
}
