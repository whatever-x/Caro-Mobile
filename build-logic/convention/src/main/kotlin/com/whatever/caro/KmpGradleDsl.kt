package com.whatever.caro

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun Project.kotlin(action: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(action)
}

fun KotlinMultiplatformExtension.android(action: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) {
    extensions.configure(action)
}
