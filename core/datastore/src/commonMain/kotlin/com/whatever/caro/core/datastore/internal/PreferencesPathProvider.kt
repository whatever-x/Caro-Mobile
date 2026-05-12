package com.whatever.caro.core.datastore.internal

expect class PreferencesPathProvider {
    fun resolve(fileName: String): String
}
