package com.whatever.caro.core.importparser

import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import okio.Path

/** 테스트용 비압축(STORED) zip. 압축 상한과 xlsx 구조 결함을 작은 파일로 재현하려고 쓴다. */
internal object TestZip {
    private const val LOCAL_HEADER = 0x04034b50
    private const val CENTRAL_HEADER = 0x02014b50
    private const val END_OF_CENTRAL_DIRECTORY = 0x06054b50
    private const val VERSION = 20

    fun write(
        path: Path,
        entries: List<Pair<String, ByteArray>>,
        claimedEntryCount: Int = entries.size,
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
        val centralOffset = body.size
        val centralSize = central.size
        body.writeAll(central)
        body.writeIntLe(END_OF_CENTRAL_DIRECTORY).writeShortLe(0).writeShortLe(0)
        body.writeShortLe(claimedEntryCount).writeShortLe(claimedEntryCount)
        body.writeIntLe(centralSize.toInt()).writeIntLe(centralOffset.toInt()).writeShortLe(0)
        Fixtures.fileSystem.write(path) { writeAll(body) }
    }

    fun xlsx(
        path: Path,
        sheetXml: String,
    ) = write(
        path,
        listOf(
            "xl/workbook.xml" to WORKBOOK.encodeToByteArray(),
            "xl/_rels/workbook.xml.rels" to RELATIONS.encodeToByteArray(),
            "xl/worksheets/sheet1.xml" to sheetXml.encodeToByteArray(),
        ),
    )

    fun sheet(rows: String): String = "<worksheet><sheetData>$rows</sheetData></worksheet>"

    private const val WORKBOOK = "<workbook><sheets><sheet name=\"S\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>"
    private const val RELATIONS =
        "<Relationships><Relationship Id=\"rId1\" Type=\"worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>"

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
