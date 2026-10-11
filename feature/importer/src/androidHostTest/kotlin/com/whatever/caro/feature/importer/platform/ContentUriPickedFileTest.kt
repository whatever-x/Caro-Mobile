package com.whatever.caro.feature.importer.platform

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import okio.buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

// provider가 주는 이름과 크기는 믿을 수 없는 값이라, 비어 있거나 음수일 때와 content가 아닌 URI를 받았을 때를 확인한다.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContentUriPickedFileTest {
    private val resolver: ContentResolver get() = RuntimeEnvironment.getApplication().contentResolver
    private lateinit var content: File

    @Before
    fun setUp() {
        content = File.createTempFile("picked", ".bin").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        FakeDocumentsProvider.content = content
        FakeDocumentsProvider.columns = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        FakeDocumentsProvider.row = arrayOf("deck.apkg", 4L)
        Robolectric.setupContentProvider(FakeDocumentsProvider::class.java, AUTHORITY)
    }

    @After
    fun tearDown() {
        content.delete()
    }

    private fun pickedFile() = ContentUriPickedFile(resolver, Uri.parse("content://$AUTHORITY/doc/primary%3ADownload%2Fdeck.apkg"))

    @Test
    fun `provider가 알려 준 이름과 크기를 돌려준다`() {
        val file = pickedFile()

        listOf(file.displayName(), file.sizeBytes()) shouldBe listOf("deck.apkg", 4L)
    }

    @Test
    fun `크기 열이 비어 있으면 크기를 모른다고 돌려준다`() {
        FakeDocumentsProvider.row = arrayOf("deck.apkg", null)

        pickedFile().sizeBytes() shouldBe null
    }

    @Test
    fun `크기 열이 없으면 크기를 모른다고 돌려준다`() {
        FakeDocumentsProvider.columns = arrayOf(OpenableColumns.DISPLAY_NAME)
        FakeDocumentsProvider.row = arrayOf("deck.apkg")

        pickedFile().sizeBytes() shouldBe null
    }

    @Test
    fun `크기가 음수이면 크기를 모른다고 돌려준다`() {
        FakeDocumentsProvider.row = arrayOf("deck.apkg", -1L)

        pickedFile().sizeBytes() shouldBe null
    }

    @Test
    fun `이름 열이 없으면 URI의 마지막 구간을 이름으로 쓴다`() {
        FakeDocumentsProvider.columns = arrayOf(OpenableColumns.SIZE)
        FakeDocumentsProvider.row = arrayOf(4L)

        pickedFile().displayName() shouldBe "primary:Download/deck.apkg"
    }

    @Test
    fun `열 때마다 새로 읽는 원본을 돌려준다`() {
        val file = pickedFile()

        val first = file.openSource().buffer().use { source -> source.readByteArray().toList() }
        val second = file.openSource().buffer().use { source -> source.readByteArray().toList() }

        listOf(first, second) shouldBe listOf(listOf<Byte>(1, 2, 3, 4), listOf<Byte>(1, 2, 3, 4))
    }

    @Test
    fun `선택기가 취소되면 취소 결과를 돌려준다`() {
        pickResultOf(resolver, uri = null) shouldBe PickResult.Cancelled
    }

    @Test
    fun `content URI는 핸들을 담아 돌려준다`() {
        pickResultOf(resolver, Uri.parse("content://$AUTHORITY/doc/1")).shouldBeInstanceOf<PickResult.Picked>()
    }

    @Test
    fun `file URI는 열지 않고 실패로 돌려준다`() {
        val result = pickResultOf(resolver, Uri.parse("file:///data/data/com.whatever.caro/databases/caro.db"))

        result.shouldBeInstanceOf<PickResult.Failed>().cause.shouldBeInstanceOf<SecurityException>()
    }

    private class FakeDocumentsProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor = MatrixCursor(columns).apply { addRow(row) }

        override fun openFile(
            uri: Uri,
            mode: String,
        ): ParcelFileDescriptor = ParcelFileDescriptor.open(content, ParcelFileDescriptor.MODE_READ_ONLY)

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        companion object {
            lateinit var content: File
            lateinit var columns: Array<String>
            lateinit var row: Array<Any?>
        }
    }

    private companion object {
        const val AUTHORITY = "com.whatever.caro.test.documents"
    }
}
