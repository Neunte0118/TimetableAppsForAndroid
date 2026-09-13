package com.example

import com.example.data.csv.AppUpdateInfo
import com.example.data.csv.CsvParser
import org.junit.Assert.*
import org.junit.Test

class AppUpdateCheckUnitTest {

    @Test
    fun parseAppUpdateInfo_withStandardFormat_parsesCorrectly() {
        val csv = """
            ver, info, link
            1.0.0, ・軽微な修整, example.com
        """.trimIndent()

        val result = CsvParser.parseAppUpdateInfo(csv)
        assertNotNull(result)
        assertEquals("1.0.0", result?.version)
        assertEquals("・軽微な修整", result?.info)
        assertEquals("example.com", result?.link)
    }

    @Test
    fun parseAppUpdateInfo_withMultilineAndQuotes_parsesCorrectly() {
        val csv = """
            "ver","info","link"
            "1.2.0","・時間割変更機能の向上
            ・バグ修正","https://github.com/example/app/releases"
        """.trimIndent()

        val result = CsvParser.parseAppUpdateInfo(csv)
        assertNotNull(result)
        assertEquals("1.2.0", result?.version)
        assertTrue(result?.info?.contains("時間割変更機能の向上") == true)
        assertEquals("https://github.com/example/app/releases", result?.link)
    }

    @Test
    fun parseUpdateHistory_supportsNewFormat() {
        val csv = """
            ver, info, link
            1.0.0, ・軽微な修整, example.com
        """.trimIndent()

        val list = CsvParser.parseUpdateHistory(csv)
        assertEquals(1, list.size)
        assertEquals("1.0.0", list[0].version)
        assertEquals("・軽微な修整", list[0].dateStr)
        assertEquals("example.com", list[0].contents)
    }

    @Test
    fun isNewerVersion_sameVersionDifferentFormats_returnsFalse() {
        // "1.0.0" vs "1.0"
        assertFalse(AppUpdateInfo.isNewerVersion("1.0.0", "1.0"))
        assertFalse(AppUpdateInfo.isNewerVersion("1.0", "1.0.0"))
        // "1.0.0" vs "1.0.0"
        assertFalse(AppUpdateInfo.isNewerVersion("1.0.0", "1.0.0"))
        // "v1.0.0" vs "1.0.0"
        assertFalse(AppUpdateInfo.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(AppUpdateInfo.isNewerVersion("1.0.0", "v1.0.0"))
        // "Ver 1.0.0" vs "1.0"
        assertFalse(AppUpdateInfo.isNewerVersion("Ver 1.0.0", "1.0"))
    }

    @Test
    fun isNewerVersion_newerVersion_returnsTrue() {
        // 1.0.1 > 1.0.0
        assertTrue(AppUpdateInfo.isNewerVersion("1.0.1", "1.0.0"))
        // 1.1.0 > 1.0.0
        assertTrue(AppUpdateInfo.isNewerVersion("1.1.0", "1.0.0"))
        // 2.0.0 > 1.9.9
        assertTrue(AppUpdateInfo.isNewerVersion("2.0.0", "1.9.9"))
        // v1.0.1 > 1.0
        assertTrue(AppUpdateInfo.isNewerVersion("v1.0.1", "1.0"))
    }

    @Test
    fun isNewerVersion_olderVersion_returnsFalse() {
        // 0.9.9 < 1.0.0
        assertFalse(AppUpdateInfo.isNewerVersion("0.9.9", "1.0.0"))
        // 1.0.0 < 1.0.1
        assertFalse(AppUpdateInfo.isNewerVersion("1.0.0", "1.0.1"))
        // 1.0 < 1.1
        assertFalse(AppUpdateInfo.isNewerVersion("1.0", "1.1"))
    }

    @Test
    fun appUpdateInfo_isNewerThan_worksCorrectly() {
        val info = AppUpdateInfo(version = "1.0.0", info = "test", link = "http://example.com")
        // Same as current version
        assertFalse(info.isNewerThan("1.0.0"))
        assertFalse(info.isNewerThan("1.0"))

        val newInfo = AppUpdateInfo(version = "1.0.1", info = "test", link = "http://example.com")
        assertTrue(newInfo.isNewerThan("1.0.0"))
        assertTrue(newInfo.isNewerThan("1.0"))
    }
}
