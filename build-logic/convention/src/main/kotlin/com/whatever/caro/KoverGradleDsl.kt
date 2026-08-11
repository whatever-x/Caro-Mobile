package com.whatever.caro

import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.kover(block: KoverProjectExtension.() -> Unit) {
    extensions.configure(block)
}