package com.whatever.caro.feature.importer.platform

import androidx.compose.runtime.Composable

/**
 * 선택기를 여는 함수를 돌려준다. 결과는 핸들만 담고 이름과 크기 조회, 복사는 ViewModel이 io에서 한다.
 * 대화상자나 시트 안이 아니라 화면 수준에서 호출해야 한다. iOS는 그 안에서 읽으면 layer 전용 컨트롤러가 나와 선택기를 띄울 수 없다.
 */
@Composable
internal expect fun rememberImportFilePicker(onResult: (PickResult) -> Unit): () -> Unit
