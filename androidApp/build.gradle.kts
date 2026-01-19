plugins {
    id("caro.android.application")
}

android.namespace = "com.whatever.caro.app"

dependencies {
    implementation(projects.composeApp)

    implementation(libs.androidx.activity.compose)
}

android {

}
