package com.whatever.caro.core.datastore

expect class PreferencesPathProvider {
    fun resolve(fileName: String): String
}
