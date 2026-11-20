package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.SpreadsheetDateStyle

/** 앱 문구가 ko와 en뿐이라 ko가 아니면 en 표기를 쓴다. 엑셀 기본 날짜 서식은 기기 지역 설정을 따르므로 파서에 앱 언어 기준 표기를 넘긴다. */
internal fun spreadsheetDateStyleFor(language: String): SpreadsheetDateStyle =
    if (language == "ko") SpreadsheetDateStyle.YEAR_MONTH_DAY else SpreadsheetDateStyle.MONTH_DAY_YEAR
