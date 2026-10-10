package com.whatever.caro.core.model.importing

/** 외부 학습 데이터 가져오기 정책 값. 성능 측정 결과에 따라 장수 상한을 5,000으로 낮출 수 있다. */
object ImportLimits {
    const val MAX_CARDS = 10_000

    /** 화면에는 "100MB"로 쓰지만 서버 업로드 한도와 같은 100MiB다. 이 값까지는 통과하고 1바이트라도 넘으면 막는다. */
    const val MAX_FILE_BYTES = 100L * 1024 * 1024

    /** 가져오기 카드 저장 1회 요청의 서버 상한. 일반 카드 생성의 요청당 200장과 다른 가져오기 전용 예외 값이다. */
    const val SERVER_CHUNK_MAX = 1_000
}
