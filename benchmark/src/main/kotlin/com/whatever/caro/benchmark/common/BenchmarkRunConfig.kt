package com.whatever.caro.benchmark.common

import android.os.Bundle

internal data class BenchmarkRunConfig(
    val iterations: Int = DEFAULT_BENCHMARK_ITERATIONS,
) {
    init {
        require(iterations > 0) { "$BENCHMARK_ITERATIONS_ARGUMENT must be greater than zero." }
    }
}

internal fun Bundle.toBenchmarkRunConfig(): BenchmarkRunConfig =
    BenchmarkRunConfig(
        iterations = getString(BENCHMARK_ITERATIONS_ARGUMENT)?.toIntOrNull() ?: DEFAULT_BENCHMARK_ITERATIONS,
    )

private const val BENCHMARK_ITERATIONS_ARGUMENT = "benchmarkIterations"
private const val DEFAULT_BENCHMARK_ITERATIONS = 5
