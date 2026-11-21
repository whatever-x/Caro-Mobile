package com.whatever.caro.feature.importer.platform

import okio.Path

/** 시스템 문서 선택기가 고른 파일을 통째로 복사해 두는 앱 tmp 아래 디렉터리. 그런 디렉터리가 없는 플랫폼은 null이다. */
internal expect fun platformInboxDirectory(): Path?
