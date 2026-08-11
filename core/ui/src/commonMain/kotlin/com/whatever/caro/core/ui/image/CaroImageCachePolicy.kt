package com.whatever.caro.core.ui.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import okio.Path

@Immutable
data class CaroImageCacheConfig(
    val diskCacheDirectoryPath: Path?,
    val diskCacheMaxSizePercent: Double?,
    val memoryCacheMaxSizePercent: Double?,
    val memoryCachePolicy: CaroImageCachePolicy,
    val diskCachePolicy: CaroImageCachePolicy,
    val networkCachePolicy: CaroImageCachePolicy,
) {
    init {
        val hasDiskCacheDirectory = diskCacheDirectoryPath != null
        val hasDiskCacheMaxSizePercent = diskCacheMaxSizePercent != null

        require(hasDiskCacheDirectory == hasDiskCacheMaxSizePercent) {
            "디스크 캐시 경로와 최대 크기 비율은 함께 설정해야 합니다."
        }

        diskCacheMaxSizePercent?.let { percent ->
            require(percent > 0.0 && percent <= 1.0) {
                "디스크 캐시 최대 크기 비율은 0보다 크고 1 이하여야 합니다."
            }
        }

        memoryCacheMaxSizePercent?.let { percent ->
            require(percent > 0.0 && percent <= 1.0) {
                "메모리 캐시 최대 크기 비율은 0보다 크고 1 이하여야 합니다."
            }
        }

        require(
            memoryCachePolicy != CaroImageCachePolicy.Disabled ||
                memoryCacheMaxSizePercent == null,
        ) {
            "메모리 캐시를 비활성화한 경우 최대 크기 비율을 설정할 수 없습니다."
        }

        require(
            diskCachePolicy != CaroImageCachePolicy.Disabled ||
                (!hasDiskCacheDirectory && !hasDiskCacheMaxSizePercent),
        ) {
            "디스크 캐시를 비활성화한 경우 디스크 캐시 옵션을 설정할 수 없습니다."
        }
    }

    companion object {
        val Default: CaroImageCacheConfig =
            CaroImageCacheConfig(
                diskCacheDirectoryPath = null,
                diskCacheMaxSizePercent = null,
                memoryCacheMaxSizePercent = 0.125,
                memoryCachePolicy = CaroImageCachePolicy.Enabled,
                diskCachePolicy = CaroImageCachePolicy.Disabled,
                networkCachePolicy = CaroImageCachePolicy.Enabled,
            )
    }
}

enum class CaroImageCachePolicy {
    Enabled,
    ReadOnly,
    WriteOnly,
    Disabled,
}

@Composable
fun ConfigureCaroImageLoader(config: CaroImageCacheConfig = CaroImageCacheConfig.Default) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader
            .Builder(context)
            .memoryCachePolicy(config.memoryCachePolicy.toCoilCachePolicy())
            .diskCachePolicy(config.diskCachePolicy.toCoilCachePolicy())
            .networkCachePolicy(config.networkCachePolicy.toCoilCachePolicy())
            .apply {
                config.diskCacheDirectoryPath?.let { path ->
                    config.diskCacheMaxSizePercent?.let { percent ->
                        diskCache {
                            DiskCache
                                .Builder()
                                .directory(path)
                                .maxSizePercent(percent)
                                .build()
                        }
                    }
                }

                config.memoryCacheMaxSizePercent?.let { percent ->
                    memoryCache {
                        MemoryCache
                            .Builder()
                            .maxSizePercent(context, percent)
                            .build()
                    }
                }
            }.build()
    }
}

private fun CaroImageCachePolicy.toCoilCachePolicy(): CachePolicy =
    when (this) {
        CaroImageCachePolicy.Enabled -> CachePolicy.ENABLED
        CaroImageCachePolicy.ReadOnly -> CachePolicy.READ_ONLY
        CaroImageCachePolicy.WriteOnly -> CachePolicy.WRITE_ONLY
        CaroImageCachePolicy.Disabled -> CachePolicy.DISABLED
    }
