package com.whatever.caro.feature.importer.platform

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import okio.IOException
import okio.Source
import okio.source

private const val CONTENT_SCHEME = "content"

@Composable
internal actual fun rememberImportFilePicker(onResult: (PickResult) -> Unit): () -> Unit {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val latestOnResult = rememberUpdatedState(onResult)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            latestOnResult.value(pickResultOf(resolver, uri))
        }
    return remember(launcher) {
        {
            try {
                // .apkg의 MIME이 기기마다 달라 필터를 걸지 않고, 확장자는 ViewModel이 거른다.
                launcher.launch(arrayOf("*/*"))
            } catch (error: ActivityNotFoundException) {
                latestOnResult.value(PickResult.Failed(error))
            }
        }
    }
}

internal fun pickResultOf(
    resolver: ContentResolver,
    uri: Uri?,
): PickResult =
    when {
        uri == null -> PickResult.Cancelled

        // 선택기가 주는 값은 content URI뿐이다. file URI를 열면 앱 내부 파일을 읽을 수 있어 받지 않는다.
        uri.scheme != CONTENT_SCHEME -> PickResult.Failed(SecurityException("Unsupported uri scheme: ${uri.scheme}"))

        else -> PickResult.Picked(ContentUriPickedFile(resolver, uri))
    }

/** 이름과 크기는 provider가 알려 주는 값이라 믿을 수 없다. 크기를 모르면 null이고 ViewModel이 복사하면서 센다. */
internal class ContentUriPickedFile(
    private val resolver: ContentResolver,
    private val uri: Uri,
) : PickedImportFile {
    private val metadata: Metadata by lazy { queryMetadata() }

    override fun displayName(): String = metadata.displayName

    override fun sizeBytes(): Long? = metadata.sizeBytes

    override fun openSource(): Source {
        val stream = resolver.openInputStream(uri) ?: throw IOException("Cannot open the picked file")
        return stream.source()
    }

    // 선택기가 사본을 만들지 않으므로 지울 것이 없다.
    override fun discard() = Unit

    private fun queryMetadata(): Metadata {
        var displayName: String? = null
        var sizeBytes: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) displayName = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex).takeIf { size -> size >= 0 }
            }
        }
        return Metadata(displayName ?: uri.lastPathSegment.orEmpty(), sizeBytes)
    }

    private class Metadata(
        val displayName: String,
        val sizeBytes: Long?,
    )
}
