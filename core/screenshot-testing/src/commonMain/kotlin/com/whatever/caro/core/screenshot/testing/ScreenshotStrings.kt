package com.whatever.caro.core.screenshot.testing

import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

data class ScreenshotString(
    val resource: StringResource,
    val formatArgs: List<Any> = emptyList(),
)

class ScreenshotStrings(
    private val values: Map<StringResource, String>,
) {
    operator fun get(resource: StringResource): String = values.getValue(resource)
}

// Resolve both languages before rendering; callbacks only read these prepared values.
internal fun prepareScreenshotStrings(
    capture: ScreenshotCapture,
    environments: Map<String, ResourceEnvironment>,
): ScreenshotStrings {
    val requests = capture.scenario.expectedStrings
    require(requests.isNotEmpty()) { "${capture.fileName}: no expected string resources" }
    require(requests.distinctBy { it.resource }.size == requests.size) {
        "${capture.fileName}: duplicate expected string resources"
    }
    val values = mutableMapOf<StringResource, String>()
    runTest {
        for (request in requests) {
            val args = request.formatArgs.toTypedArray()
            val localized =
                ScreenshotConfiguration.languages.associateWith { language ->
                    getString(environments.getValue(language), request.resource, *args)
                }
            check(localized.getValue("ko") != localized.getValue("en")) {
                "${capture.fileName}: ${request.resource.key} must distinguish ko/en; check translations and expected environments"
            }
            values[request.resource] = localized.getValue(capture.language)
        }
    }
    return ScreenshotStrings(values)
}
