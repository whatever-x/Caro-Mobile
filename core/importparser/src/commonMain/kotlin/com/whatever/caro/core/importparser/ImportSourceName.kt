package com.whatever.caro.core.importparser

/** 파서와 파일 선택기 사전 확인이 같은 규칙을 쓰도록 공개한다. 임시 사본 경로에는 확장자가 없을 수 있어 사용자에게 보인 이름만 본다. */
object ImportSourceName {
    private val SUPPORTED_EXTENSIONS = setOf("apkg", "txt", "csv", "tsv", "xlsx")

    /** 마지막 점 뒤를 소문자로 바꿔 돌려준다. 확장자가 없거나 지원하지 않으면 null이다. */
    fun supportedExtension(sourceName: String): String? =
        sourceName
            .substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .takeIf { extension -> extension in SUPPORTED_EXTENSIONS }
}
