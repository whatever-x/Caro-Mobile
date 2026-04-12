plugins {
    id("caro.android.application")
    alias(libs.plugins.google.services)
}

android.namespace = "com.whatever.caro.app"

dependencies {
    implementation(projects.composeApp)

    implementation(libs.koin.android)
    implementation(libs.androidx.activity.compose)
    implementation(libs.napier)
    implementation(project.dependencies.platform(libs.firebase.bom.android))
    implementation(libs.firebase.auth)
}
