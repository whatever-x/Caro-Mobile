package com.whatever.caro.feature.importer.platform

import okio.FileSystem
import okio.Path
import platform.Foundation.NSBundle

internal actual fun platformInboxDirectory(): Path? = inboxDirectoryFor(NSBundle.mainBundle.bundleIdentifier)

/** asCopy 선택기가 사본을 두는 이름 규칙이다. 앱이 지우지 못하고 종료되면 다음 실행에도 남는다. */
internal fun inboxDirectoryFor(bundleIdentifier: String?): Path? =
    bundleIdentifier?.let { identifier -> FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "$identifier-Inbox" }
