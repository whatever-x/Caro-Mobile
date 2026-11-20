package com.whatever.caro.feature.importer.platform

import okio.Path

// Android 선택기는 사본을 만들지 않고 content URI만 준다.
internal actual fun platformInboxDirectory(): Path? = null
