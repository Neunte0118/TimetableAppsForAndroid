package com.example

import com.example.data.csv.CsvParser
import com.example.data.csv.CsvUrlsConfig
import com.example.model.AdditionalEventDisplayItem
import com.example.model.DaySchedule
import com.example.model.stripLinksToPlainText
import com.example.ui.components.UpdateMarkdownParser
import com.example.ui.components.UpdateMarkdownSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdditionalEventUnitTest {

    @Test
    fun normalizeCsvUrl_convertsGoogleSheetsPubHtmlToCsv() {
        val raw = "https://docs.google.com/spreadsheets/d/e/2PACX-1vR1dN6poNIpBsmis-JO2N2Hjiu96bkMuqs2fDtf1V3FH6iBs3BuQZVskaDq8n-xhoKEMdGYD-P5LscW/pubhtml?gid=0&single=true"
        val normalized = CsvUrlsConfig.normalizeCsvUrl(raw)
        assertEquals(
            "https://docs.google.com/spreadsheets/d/e/2PACX-1vR1dN6poNIpBsmis-JO2N2Hjiu96bkMuqs2fDtf1V3FH6iBs3BuQZVskaDq8n-xhoKEMdGYD-P5LscW/pub?gid=0&single=true&output=csv",
            normalized
        )
        // Idempotent when already normalized
        assertEquals(normalized, CsvUrlsConfig.normalizeCsvUrl(normalized))
    }

    @Test
    fun parseAdditionalEvents_supportsTsvCsvAndMultipleEntriesPerDate() {
        val tsv = """
            dates	contents	light_color	dark_color
            6月12日	化学　第二中間対策講座（於 B31）	#3ec9b0	#3ec9b0
            6月12日	<a href="https://drive.google.com/drive/folders/1HCY-7nYmwWcAtWUieBylBuWZA2br2SCb">早朝勉強会 第1回 </a>	#ff9800	#ffb74d
        """.trimIndent()

        val rows = CsvParser.parseAdditionalEvents(tsv)
        assertEquals(2, rows.size)

        assertEquals(6, rows[0].month)
        assertEquals(12, rows[0].day)
        assertEquals("化学　第二中間対策講座（於 B31）", rows[0].contents)
        assertEquals("#3ec9b0", rows[0].lightColorHex)
        assertEquals("#3ec9b0", rows[0].darkColorHex)

        assertEquals(6, rows[1].month)
        assertEquals(12, rows[1].day)
        assertTrue(rows[1].contents.contains("<a href=\"https://drive.google.com/drive/folders/1HCY-7nYmwWcAtWUieBylBuWZA2br2SCb\">"))
        assertEquals("#ff9800", rows[1].lightColorHex)
        assertEquals("#ffb74d", rows[1].darkColorHex)
    }

    @Test
    fun parseAdditionalEvents_supportsStandardCsvWithCommaInsideContent() {
        val csv = """
            dates,contents,light_color,dark_color
            6月12日,"数学, 英語 補習",#1976d2,#64b5f6
        """.trimIndent()

        val rows = CsvParser.parseAdditionalEvents(csv)
        assertEquals(1, rows.size)
        assertEquals("数学, 英語 補習", rows[0].contents)
        assertEquals("#1976d2", rows[0].lightColorHex)
        assertEquals("#64b5f6", rows[0].darkColorHex)
    }

    @Test
    fun updateMarkdownParser_parsesHtmlAnchorTagsAndMarkdownLinks() {
        val input = """<a href="https://drive.google.com/drive/folders/1HCY-7nYmwWcAtWUieBylBuWZA2br2SCb">早朝勉強会 第1回 </a>"""
        val spans = UpdateMarkdownParser.parseInlineSpans(input)
        assertEquals(
            listOf(
                UpdateMarkdownSpan.Link(
                    title = "早朝勉強会 第1回",
                    url = "https://drive.google.com/drive/folders/1HCY-7nYmwWcAtWUieBylBuWZA2br2SCb"
                )
            ),
            spans
        )
    }

    @Test
    fun combinedPlainEventText_joinsMultipleAdditionalEventsWithNewlineAndStripsHtmlLinks() {
        val schedule = DaySchedule(
            date = LocalDate.of(2026, 6, 12),
            dayLabel = "今日",
            periods = emptyList(),
            event = "避難訓練",
            additionalEvents = listOf(
                AdditionalEventDisplayItem(
                    contents = "化学　第二中間対策講座（於 B31）",
                    lightColorHex = "#3ec9b0",
                    darkColorHex = "#3ec9b0"
                ),
                AdditionalEventDisplayItem(
                    contents = """<a href="https://drive.google.com/drive/folders/1HCY-7nYmwWcAtWUieBylBuWZA2br2SCb">早朝勉強会 第1回 </a>""",
                    lightColorHex = "#ff9800",
                    darkColorHex = "#ffb74d"
                )
            )
        )

        assertEquals(
            "避難訓練\n化学　第二中間対策講座（於 B31）\n早朝勉強会 第1回",
            schedule.combinedPlainEventText()
        )
        assertEquals(
            "早朝勉強会 第1回",
            stripLinksToPlainText("""<a href="https://drive.google.com/drive/folders/1HCY-7nYmwWcAtWUieBylBuWZA2br2SCb">早朝勉強会 第1回 </a>""")
        )
    }
}
