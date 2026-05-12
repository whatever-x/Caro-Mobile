package com.whatever.caro.core.datastore.internal

import android.content.Context

actual class PreferencesPathProvider(
    private val context: Context,
) {
    actual fun resolve(fileName: String): String = context.filesDir.resolve("datastore/$fileName").absolutePath
}
