plugins {
    id("caro.android.application")
}

android.namespace = "com.whatever.caro.app"

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
}

android {

}
