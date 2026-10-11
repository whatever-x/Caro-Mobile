package com.whatever.caro.core.importparser

import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.Path

/** 테스트용 비압축(STORED) zip. 압축 상한과 xlsx 구조 결함을 작은 파일로 재현하려고 쓴다. */
internal object TestZip {
    private const val LOCAL_HEADER = 0x04034b50
    private const val CENTRAL_HEADER = 0x02014b50
    private const val END_OF_CENTRAL_DIRECTORY = 0x06054b50
    private const val ZIP64_END_RECORD = 0x06064b50
    private const val ZIP64_LOCATOR = 0x07064b50
    private const val ZIP64_END_RECORD_REMAINING_BYTES = 44L
    private const val ZIP64_END_RECORD_BYTES = 56L
    private const val ZIP64_LOCATOR_BYTES = 20L
    private const val END_RECORD_BYTES = 22L
    private const val VERSION = 20

    /** ZIP64 끝 레코드를 둘 위치. locator는 어느 쪽이든 일반 끝 레코드 바로 앞에 있다. */
    enum class Zip64Placement {
        /** 일반 끝 레코드 바로 앞(표준 배치). */
        BEFORE_END_RECORD,

        /** 중앙 디렉터리 앞. 레코드에서 중앙 디렉터리까지의 거리가 레코드 위치로는 잡히지 않는다. */
        BEFORE_CENTRAL_DIRECTORY,

        /** 일반 끝 레코드 뒤 코멘트 영역. */
        IN_COMMENT,
    }

    /**
     * [zip64EntryCount]를 주면 그 항목 수를 적은 ZIP64 끝 레코드와 locator를 [zip64Placement]에 붙인다.
     * [claimedCentralDirectoryBytes]를 주면 일반 끝 레코드에, [zip64ClaimedCentralDirectoryBytes]를 주면 ZIP64 끝 레코드에 실제와 다른 중앙 디렉터리 크기를 적는다.
     * [trailingBytes]는 일반 끝 레코드 뒤에 0을 덧붙여 끝 레코드를 파일 끝에서 멀리 민다.
     */
    fun write(
        path: Path,
        entries: List<Pair<String, ByteArray>>,
        claimedEntryCount: Int = entries.size,
        zip64EntryCount: Long? = null,
        claimedCentralDirectoryBytes: Int? = null,
        zip64Placement: Zip64Placement = Zip64Placement.BEFORE_END_RECORD,
        zip64ClaimedCentralDirectoryBytes: Long? = null,
        trailingBytes: Int = 0,
    ) {
        val body = Buffer()
        val central = Buffer()
        entries.forEach { (name, data) ->
            val offset = body.size
            val nameBytes = name.encodeUtf8()
            val crc = crc32(data)
            body
                .writeIntLe(LOCAL_HEADER)
                .writeShortLe(VERSION)
                .writeShortLe(0)
                .writeShortLe(0)
                .writeIntLe(0)
            body
                .writeIntLe(crc)
                .writeIntLe(data.size)
                .writeIntLe(data.size)
                .writeShortLe(nameBytes.size)
                .writeShortLe(0)
            body.write(nameBytes).write(data)
            central
                .writeIntLe(CENTRAL_HEADER)
                .writeShortLe(VERSION)
                .writeShortLe(VERSION)
                .writeShortLe(0)
                .writeShortLe(0)
                .writeIntLe(0)
            central
                .writeIntLe(crc)
                .writeIntLe(data.size)
                .writeIntLe(data.size)
                .writeShortLe(nameBytes.size)
            central
                .writeShortLe(0)
                .writeShortLe(0)
                .writeShortLe(0)
                .writeShortLe(0)
                .writeIntLe(0)
                .writeIntLe(offset.toInt())
            central.write(nameBytes)
        }
        val centralSize = central.size
        val zip64Size = zip64ClaimedCentralDirectoryBytes ?: centralSize
        var recordOffset = 0L
        var comment = Buffer()
        if (zip64EntryCount != null && zip64Placement == Zip64Placement.BEFORE_CENTRAL_DIRECTORY) {
            recordOffset = body.size
            body.writeAll(zip64End(zip64EntryCount, zip64Size, body.size + ZIP64_END_RECORD_BYTES))
        }
        val centralOffset = body.size
        body.writeAll(central)
        if (zip64EntryCount != null && zip64Placement == Zip64Placement.BEFORE_END_RECORD) {
            recordOffset = body.size
            body.writeAll(zip64End(zip64EntryCount, zip64Size, centralOffset))
        }
        if (zip64EntryCount != null && zip64Placement == Zip64Placement.IN_COMMENT) {
            recordOffset = body.size + ZIP64_LOCATOR_BYTES + END_RECORD_BYTES
            comment = zip64End(zip64EntryCount, zip64Size, centralOffset)
        }
        if (zip64EntryCount != null) writeZip64Locator(body, recordOffset)
        body.writeIntLe(END_OF_CENTRAL_DIRECTORY).writeShortLe(0).writeShortLe(0)
        body.writeShortLe(claimedEntryCount).writeShortLe(claimedEntryCount)
        val declaredCentralSize = claimedCentralDirectoryBytes ?: centralSize.toInt()
        body.writeIntLe(declaredCentralSize).writeIntLe(centralOffset.toInt()).writeShortLe(comment.size.toInt())
        body.writeAll(comment)
        body.write(ByteArray(trailingBytes))
        Fixtures.fileSystem.write(path) { writeAll(body) }
    }

    private fun writeZip64Locator(
        body: Buffer,
        recordOffset: Long,
    ) {
        body
            .writeIntLe(ZIP64_LOCATOR)
            .writeIntLe(0)
            .writeLongLe(recordOffset)
            .writeIntLe(1)
    }

    private fun zip64End(
        entryCount: Long,
        centralSize: Long,
        centralOffset: Long,
    ): Buffer =
        Buffer()
            .writeIntLe(ZIP64_END_RECORD)
            .writeLongLe(ZIP64_END_RECORD_REMAINING_BYTES)
            .writeShortLe(VERSION)
            .writeShortLe(VERSION)
            .writeIntLe(0)
            .writeIntLe(0)
            .writeLongLe(entryCount)
            .writeLongLe(entryCount)
            .writeLongLe(centralSize)
            .writeLongLe(centralOffset)

    fun xlsx(
        path: Path,
        sheetXml: String,
        sharedStrings: String? = null,
        zip64EntryCount: Long? = null,
        claimedCentralDirectoryBytes: Int? = null,
        zip64Placement: Zip64Placement = Zip64Placement.BEFORE_END_RECORD,
        zip64ClaimedCentralDirectoryBytes: Long? = null,
    ) = write(
        path,
        entries =
            listOfNotNull(
                "xl/workbook.xml" to WORKBOOK.encodeToByteArray(),
                "xl/_rels/workbook.xml.rels" to (if (sharedStrings == null) RELATIONS else RELATIONS_WITH_STRINGS).encodeToByteArray(),
                "xl/worksheets/sheet1.xml" to sheetXml.encodeToByteArray(),
                sharedStrings?.let { "xl/sharedStrings.xml" to "<sst>$it</sst>".encodeToByteArray() },
            ),
        zip64EntryCount = zip64EntryCount,
        claimedCentralDirectoryBytes = claimedCentralDirectoryBytes,
        zip64Placement = zip64Placement,
        zip64ClaimedCentralDirectoryBytes = zip64ClaimedCentralDirectoryBytes,
    )

    fun sheet(rows: String): String = "<worksheet><sheetData>$rows</sheetData></worksheet>"

    private const val WORKBOOK = "<workbook><sheets><sheet name=\"S\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"
    private const val SHEET_RELATION = "<Relationship Id=\"rId1\" Type=\"worksheet\" Target=\"worksheets/sheet1.xml\"/>"
    private const val STRINGS_RELATION = "<Relationship Id=\"rId2\" Type=\"x/sharedStrings\" Target=\"sharedStrings.xml\"/>"
    private const val RELATIONS = "<Relationships>$SHEET_RELATION</Relationships>"
    private const val RELATIONS_WITH_STRINGS = "<Relationships>$SHEET_RELATION$STRINGS_RELATION</Relationships>"

    private const val CRC_POLYNOMIAL = 0xEDB88320.toInt()
    private const val BYTE_MASK = 0xFF
    private const val BITS_PER_BYTE = 8

    private fun crc32(data: ByteArray): Int {
        var crc = -1
        data.forEach { byte ->
            crc = crc xor (byte.toInt() and BYTE_MASK)
            repeat(BITS_PER_BYTE) { crc = if (crc and 1 != 0) (crc ushr 1) xor CRC_POLYNOMIAL else crc ushr 1 }
        }
        return crc.inv()
    }
}
