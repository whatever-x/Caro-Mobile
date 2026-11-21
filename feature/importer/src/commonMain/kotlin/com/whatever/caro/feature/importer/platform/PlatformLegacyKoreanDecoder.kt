package com.whatever.caro.feature.importer.platform

import com.whatever.caro.core.importparser.LegacyKoreanDecoder

/** 이 기기에서 CP949를 엄격하게 읽을 수 있으면 디코더를, 없으면 null을 돌려준다. null이면 UTF-8이 아닌 파일은 인코딩 오류로 멈춘다. */
internal expect fun platformLegacyKoreanDecoder(): LegacyKoreanDecoder?
