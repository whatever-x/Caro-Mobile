package com.whatever.caro.core.importparser.text

/**
 * 헤더가 없을 때 구분자를 고른다. 따옴표 안의 구분자를 세지 않도록 후보마다 실제 레코드로 나눠 본다.
 * 가장 많은 행이 같은 개수를 보이는 후보를 고르고, 동률이면 탭, 세미콜론, 쉼표 순이다.
 */
internal object DelimiterDetector {
    private val CANDIDATES = listOf('\t', ';', ',')
    private const val SAMPLE_RECORDS = 50

    fun detect(sample: String): Char? {
        val scores = CANDIDATES.map { candidate -> candidate to consistentRows(sample, candidate) }
        val best = scores.maxOf { (_, score) -> score }
        if (best == 0) return null
        return scores.first { (_, score) -> score == best }.first
    }

    private fun consistentRows(
        sample: String,
        candidate: Char,
    ): Int {
        val reader = DelimitedRecordReader(CharCursor(listOf(sample).iterator()), candidate)
        val counts = mutableListOf<Int>()
        while (counts.size < SAMPLE_RECORDS) {
            val record = reader.nextRecord() ?: break
            counts += record.size - 1
        }
        return counts
            .filter { count -> count > 0 }
            .groupingBy { count -> count }
            .eachCount()
            .values
            .maxOrNull() ?: 0
    }
}
