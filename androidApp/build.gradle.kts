plugins {
    id("caro.android.application")
}

android.namespace = "com.whatever.caro.app"

dependencies {
    implementation(projects.composeApp)

    implementation(libs.koin.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.napier)
}
