package com.whatever.caro.core.datastore.internal

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual class PreferencesPathProvider {
    @OptIn(ExperimentalForeignApi::class)
    actual fun resolve(fileName: String): String {
        val documentDirectory: NSURL? =
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
        val documentPath = requireNotNull(documentDirectory?.path) { "Document directory를 찾을 수 없습니다." }
        return "$documentPath/$fileName"
    }
}
