package com.whatever.caro.feature.importer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject

@Composable
internal actual fun rememberImportFilePicker(onResult: (PickResult) -> Unit): () -> Unit {
    val viewController = LocalUIViewController.current
    val latestOnResult = rememberUpdatedState(onResult)
    // UIKit은 delegate를 약한 참조로만 잡으므로 remember로 붙잡지 않으면 해제되어 콜백이 오지 않는다.
    val delegate = remember { DocumentPickerDelegate { result -> latestOnResult.value(result) } }
    return remember(viewController, delegate) {
        {
            // asCopy가 거짓이면 보안 범위 접근과 iCloud 내려받기 조정을 직접 해야 해서 앱 tmp에 사본을 받는다.
            val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeData), asCopy = true)
            picker.delegate = delegate
            picker.allowsMultipleSelection = false
            (viewController.presentedViewController ?: viewController).presentViewController(picker, animated = true, completion = null)
        }
    }
}

private class DocumentPickerDelegate(
    private val onResult: (PickResult) -> Unit,
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val path = (didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.path
        onResult(
            if (path == null) {
                PickResult.Failed(IOException("The picker returned no file path"))
            } else {
                PickResult.Picked(InboxCopyPickedFile(path.toPath()))
            },
        )
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(PickResult.Cancelled)
    }
}

/** 선택기가 앱 tmp에 만든 사본이다. 크기를 넘으면 ViewModel이 복사하기 전에 거르고, 끝나면 [discard]로 사본을 지운다. */
internal class InboxCopyPickedFile(
    private val path: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : PickedImportFile {
    override fun displayName(): String = path.name

    override fun sizeBytes(): Long? = fileSystem.metadataOrNull(path)?.size

    override fun openSource(): Source = fileSystem.source(path)

    override fun discard() {
        try {
            fileSystem.delete(path, mustExist = false)
        } catch (_: IOException) {
            return
        }
    }
}
