package com.example

import com.example.data.csv.AppUpdateInfo
import com.example.data.csv.CsvParser
import com.example.ui.components.UpdateMarkdownBlock
import com.example.ui.components.UpdateMarkdownParser
import com.example.ui.components.UpdateMarkdownSpan
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
        assertEquals("・軽微な修整", list[0].contents)
        assertEquals("", list[0].dateStr)
        assertFalse(list[0].contents.contains("example.com"))
    }

    @Test
    fun parseUpdateHistory_withActualDataAndNoUrls() {
        val csv = """
            ver,info,link
            1.3.1,"・考査時間割の理科演習Lの表示を改善・バージョン番号が更新されていなかった不具合を修正",https://github.com/Neunte0118/TimetableAppsForAndroid/releases/download/v1.3.1/timetable-app-v1.3.1.apk
        """.trimIndent()

        val list = CsvParser.parseUpdateHistory(csv)
        assertEquals(1, list.size)
        assertEquals("1.3.1", list[0].version)
        assertEquals("・考査時間割の理科演習Lの表示を改善\n・バージョン番号が更新されていなかった不具合を修正", list[0].contents)
        assertFalse(list[0].contents.contains("github.com"))
        assertFalse(list[0].contents.contains(".apk"))
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

    @Test
    fun parseAppUpdateInfo_withBlankLink_generatesLatestUrlAutomatically() {
        val csv = """
            ver,info,link
        """.trimIndent()

        val result = CsvParser.parseAppUpdateInfo(csv)
        assertNotNull(result)
        assertEquals("1.4.0", result?.version)
        assertEquals("・新機能の追加", result?.info)
        assertEquals(
            "https://github.com/Neunte0118/TimetableAppsForAndroid/releases/download/v1.4.0/timetable-app-v1.4.0.apk",
            result?.link
        )
    }

    @Test
    fun parseAppUpdateInfo_withOldVersionUrl_updatesToLatestVersionUrl() {
        val csv = """
            ver,info,link
            1.4.0,・新機能の追加,https://github.com/Neunte0118/TimetableAppsForAndroid/releases/download/v1.3.1/timetable-app-v1.3.1.apk
        """.trimIndent()

        val result = CsvParser.parseAppUpdateInfo(csv)
        assertNotNull(result)
        assertEquals("1.4.0", result?.version)
        assertEquals(
            "https://github.com/Neunte0118/TimetableAppsForAndroid/releases/download/v1.4.0/timetable-app-v1.4.0.apk",
            result?.link
        )
    }

    @Test
    fun appUpdateInfo_buildDownloadUrl_handlesVariousFormats() {
        // Without v prefix
        assertEquals(
            "https://github.com/Neunte0118/TimetableAppsForAndroid/releases/download/v1.5.0/timetable-app-v1.5.0.apk",
            AppUpdateInfo.buildDownloadUrl("1.5.0")
        )
        // With v prefix
        assertEquals(
            "https://github.com/Neunte0118/TimetableAppsForAndroid/releases/download/v1.5.0/timetable-app-v1.5.0.apk",
            AppUpdateInfo.buildDownloadUrl("v1.5.0")
        )
        // With Ver prefix
        assertEquals(
            "https://github.com/Neunte0118/TimetableAppsForAndroid/releases/download/v1.5.0/timetable-app-v1.5.0.apk",
            AppUpdateInfo.buildDownloadUrl("Ver 1.5.0")
        )
    }

    @Test
    fun parseUpdateHistory_preventsDateAndInfoColumnCollisionAndDeduplicates() {
        val csv = """
            ver,更新日,更新内容
            1.4.10,2026/04/18,・不具合修正
            v1.4.10,2026/04/18,・不具合修正
        """.trimIndent()

        val list = CsvParser.parseUpdateHistory(csv)
        assertEquals(1, list.size)
        assertEquals("1.4.10", list[0].version)
        assertEquals("2026/04/18", list[0].dateStr)
        assertEquals("・不具合修正", list[0].contents)
    }

    @Test
    fun parseUpdateHistory_preservesMarkdownLinksAndCodeBlocks() {
        val checksum = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
        val csv = """
            ver,info,link
            1.4.10,"・更新詳細: [リリースノート](https://example.com/release)
            https://example.com/bare-url-to-strip
            ```sha256
            $checksum
            ```",https://example.com/app.apk
        """.trimIndent()

        val history = CsvParser.parseUpdateHistory(csv)
        assertEquals(1, history.size)
        val content = history[0].contents
        assertTrue(content.contains("[リリースノート](https://example.com/release)"))
        assertFalse(content.contains("bare-url-to-strip"))
        assertTrue(content.contains("```sha256\n$checksum\n```"))

        val appUpdate = CsvParser.parseAppUpdateInfo(csv)
        assertNotNull(appUpdate)
        assertTrue(appUpdate!!.info.contains("[リリースノート](https://example.com/release)"))
        assertTrue(appUpdate.info.contains("```sha256\n$checksum\n```"))
    }

    @Test
    fun updateMarkdownParser_parsesLinksAndCodeBlocks() {
        val checksum = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        val input = """
            ・詳細は[こちら](https://example.com/docs)をご確認ください
            ```sha256
            $checksum
            ```
        """.trimIndent()

        val blocks = UpdateMarkdownParser.parseBlocks(input)
        assertEquals(2, blocks.size)

        val textBlock = blocks[0] as UpdateMarkdownBlock.TextBlock
        assertEquals(
            listOf(
                UpdateMarkdownSpan.PlainText("・詳細は"),
                UpdateMarkdownSpan.Link("こちら", "https://example.com/docs"),
                UpdateMarkdownSpan.PlainText("をご確認ください")
            ),
            textBlock.spans
        )

        val codeBlock = blocks[1] as UpdateMarkdownBlock.CodeBlock
        assertEquals("sha256", codeBlock.language)
        assertEquals(checksum, codeBlock.code)
    }

    @Test
    fun updateMarkdownParser_parsesSingleLineCodeBlockAndHashWithoutNewline() {
        val hash = "a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8f90"
        val singleLineBlocks = UpdateMarkdownParser.parseBlocks("```$hash```")
        assertEquals(1, singleLineBlocks.size)
        assertEquals(hash, (singleLineBlocks[0] as UpdateMarkdownBlock.CodeBlock).code)

        val multilineNoLangBlocks = UpdateMarkdownParser.parseBlocks("```\n$hash\n```")
        assertEquals(1, multilineNoLangBlocks.size)
        assertEquals(hash, (multilineNoLangBlocks[0] as UpdateMarkdownBlock.CodeBlock).code)
    }
}
