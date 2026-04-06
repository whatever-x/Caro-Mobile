package com.whatever.caro

import com.skydoves.compose.stability.gradle.StabilityAnalyzerExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.composeStabilityAnalyzer(block: StabilityAnalyzerExtension.() -> Unit) {
    extensions.configure(block)
}