package com.whatever.caro.feature.importer.platform

import com.whatever.caro.core.importparser.LegacyKoreanDecoder
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFStringConvertEncodingToNSStringEncoding
import platform.CoreFoundation.kCFStringEncodingDOSKorean
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.create

/** DOSKorean은 CP949(Windows 949)다. 잘못된 바이트가 있으면 Foundation이 nil을 돌려주므로 대체 문자가 들어가지 않는다. */
@OptIn(ExperimentalForeignApi::class)
internal actual fun platformLegacyKoreanDecoder(): LegacyKoreanDecoder? {
    val encoding = CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingDOSKorean.toUInt())
    return LegacyKoreanDecoder { bytes -> decode(bytes, encoding) }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun decode(
    bytes: ByteArray,
    encoding: ULong,
): String? {
    if (bytes.isEmpty()) return ""
    val data = bytes.usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong()) }
    @Suppress("CAST_NEVER_SUCCEEDS")
    return NSString.create(data = data, encoding = encoding) as String?
}
