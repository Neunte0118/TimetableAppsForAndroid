package com.example.data.csv

import java.util.regex.Pattern

object CsvParser {

    /**
     * Parses raw CSV or TSV string into list of rows, where each row is a list of column strings.
     * Supports quotation marks, commas, tabs, escaped quotes (""), and multiline cells.
     */
    fun parseRawCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val cleanText = text.removePrefix("\uFEFF")
        if (cleanText.isBlank()) return rows

        val lines = splitCsvLines(cleanText)
        val preferTsv = lines.firstOrNull { it.isNotBlank() }?.let { firstLine ->
            hasUnquotedChar(firstLine, '\t')
        } ?: false
        for (line in lines) {
            val cells = parseCsvLine(line, preferTsv)
            if (cells.isNotEmpty() && cells.any { it.isNotBlank() }) {
                rows.add(cells.map { it.replace("\r\n", "\n").replace("\r", "\n").trim() })
            }
        }
        return rows
    }

    private fun isAtQuotedFieldStart(sb: StringBuilder): Boolean {
        if (sb.isEmpty()) return true
        for (idx in sb.length - 1 downTo 0) {
            val ch = sb[idx]
            if (ch == '\n' || ch == '\r' || ch == ',' || ch == '\t') return true
            if (ch != ' ') return false
        }
        return true
    }

    private fun hasUnquotedChar(line: String, target: Char): Boolean {
        var insideQuote = false
        var atFieldStart = true
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (!insideQuote && atFieldStart) {
                    insideQuote = true
                    atFieldStart = false
                } else if (insideQuote) {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        i++
                    } else {
                        insideQuote = false
                    }
                }
            } else if (c == target && !insideQuote) {
                return true
            } else if ((c == ',' || c == '\t') && !insideQuote) {
                atFieldStart = true
            } else if (c != ' ' && !insideQuote) {
                atFieldStart = false
            }
            i++
        }
        return false
    }

    /**
     * Splits CSV lines while respecting multiline quoted fields.
     */
    private fun splitCsvLines(csvText: String): List<String> {
        val lines = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuote = false
        var i = 0

        while (i < csvText.length) {
            val c = csvText[i]
            if (c == '"') {
                if (!insideQuote && isAtQuotedFieldStart(sb)) {
                    insideQuote = true
                    sb.append(c)
                } else if (insideQuote) {
                    sb.append(c)
                    if (i + 1 < csvText.length && csvText[i + 1] == '"') {
                        i++
                        sb.append(csvText[i])
                    } else {
                        insideQuote = false
                    }
                } else {
                    sb.append(c)
                }
            } else if ((c == '\n' || c == '\r') && !insideQuote) {
                if (c == '\r' && i + 1 < csvText.length && csvText[i + 1] == '\n') {
                    // skip '\r' in CRLF, '\n' will trigger line flush
                } else {
                    if (sb.isNotEmpty()) {
                        lines.add(sb.toString())
                        sb.clear()
                    }
                }
            } else {
                sb.append(c)
            }
            i++
        }
        if (sb.isNotEmpty()) {
            lines.add(sb.toString())
        }
        return lines
    }

    /**
     * Parses a single CSV/TSV line into cell values.
     */
    private fun parseCsvLine(line: String, preferTsv: Boolean = false): List<String> {
        val hasComma = hasUnquotedChar(line, ',')
        val hasTab = hasUnquotedChar(line, '\t')
        if (!hasComma && !hasTab && line.contains(' ')) {
            // Space delimited line
            val tokens = mutableListOf<String>()
            val sb = StringBuilder()
            var insideQuote = false
            var i = 0
            while (i < line.length) {
                val c = line[i]
                if (c == '"') {
                    if (insideQuote && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        insideQuote = !insideQuote
                    }
                } else if ((c == ' ' || c == '\u3000') && !insideQuote) {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                } else {
                    sb.append(c)
                }
                i++
            }
            if (sb.isNotEmpty()) {
                tokens.add(sb.toString())
            }
            if (tokens.isNotEmpty()) {
                return tokens
            }
        }

        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuote = false
        val isTsv = hasTab && (preferTsv || !hasComma)
        val delimiter = if (isTsv) '\t' else ','

        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (!insideQuote && sb.all { it == ' ' }) {
                    sb.clear()
                    insideQuote = true
                } else if (insideQuote) {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++ // Skip escaped quote
                    } else if (i + 1 >= line.length || line[i + 1] == delimiter || line.substring(i + 1).all { it == ' ' }) {
                        insideQuote = false
                    } else {
                        sb.append('"')
                    }
                } else {
                    sb.append('"')
                }
            } else if (c == delimiter && !insideQuote) {
                cells.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        cells.add(sb.toString())
        return cells
    }

    /**
     * Parses Date string such as "4月1日", "04/01", "2026/07/20", "2026-08-24" into Month and Day.
     * Fullwidth digits are automatically normalized to halfwidth.
     */
    fun parseMonthDay(dateStr: String): Pair<Int, Int>? {
        val clean = CsvNormalizer.normalizeDate(dateStr)
        if (clean.isBlank()) return null

        // 1. "4月13日" or "4月1日"
        val jpRegex = Regex("""(\d{1,2})月(\d{1,2})日?""")
        jpRegex.find(clean)?.let { match ->
            val m = match.groupValues[1].toIntOrNull()
            val d = match.groupValues[2].toIntOrNull()
            if (m != null && d != null) return Pair(m, d)
        }

        // 2. "2026/07/20" or "2026-08-24" or "2026.08.24" or "7/20"
        val slashRegex = Regex("""(?:\d{4}[/.-])?(\d{1,2})[/.-](\d{1,2})""")
        slashRegex.find(clean)?.let { match ->
            val m = match.groupValues[1].toIntOrNull()
            val d = match.groupValues[2].toIntOrNull()
            if (m != null && d != null) return Pair(m, d)
        }

        return null
    }

    // 1. Parse 基本クラス別時間割
    // Headers: class, type, day_of_week, first_period, second_period, third_period, fourth_period, fifth_period
    fun parseBasicClassSchedule(csvText: String): List<BasicClassScheduleRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<BasicClassScheduleRow>()
        val startIdx = if (rows.first().getOrNull(0)?.contains("class", ignoreCase = true) == true) 1 else 0

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.size < 3) continue
            val classId = CsvNormalizer.normalizeClassId(row[0].trim())
            val type = CsvNormalizer.normalizeAlphanumeric(row.getOrNull(1)?.trim() ?: "A")
            val dayOfWeek = row.getOrNull(2)?.trim() ?: "月"
            val periods = (3 until maxOf(8, row.size)).map { col ->
                CsvNormalizer.normalizeSubject(row.getOrNull(col)?.trim() ?: "")
            }
            if (classId.isNotBlank()) {
                list.add(
                    BasicClassScheduleRow(
                        classId = classId,
                        type = type,
                        dayOfWeek = dayOfWeek,
                        periods = periods
                    )
                )
            }
        }
        return list
    }

    // 2. Parse 時間割（共通）
    // Headers: dates, first_period, second_period, third_period, fourth_period, fifth_period
    fun parseCommonSchedule(csvText: String): List<CommonScheduleRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<CommonScheduleRow>()
        val startIdx = if (rows.first().getOrNull(0)?.contains("date", ignoreCase = true) == true) 1 else 0

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val dateStr = CsvNormalizer.normalizeDate(row[0].trim())
            val (m, d) = parseMonthDay(dateStr) ?: continue
            val periods = (1..5).map { col ->
                CsvNormalizer.normalizeSubject(row.getOrNull(col)?.trim() ?: "")
            }
            list.add(
                CommonScheduleRow(
                    dateStr = dateStr,
                    month = m,
                    day = d,
                    periodCodes = periods
                )
            )
        }
        return list
    }

    // 3. Parse 行事
    // Headers: dates, events
    fun parseEvents(csvText: String): List<EventRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<EventRow>()
        val startIdx = if (rows.first().getOrNull(0)?.contains("date", ignoreCase = true) == true) 1 else 0

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val dateStr = CsvNormalizer.normalizeDate(row[0].trim())
            val (m, d) = parseMonthDay(dateStr) ?: continue
            val event = row.getOrNull(1)?.trim() ?: ""
            if (event.isNotBlank()) {
                list.add(
                    EventRow(
                        dateStr = dateStr,
                        month = m,
                        day = d,
                        event = event
                    )
                )
            }
        }
        return list
    }

    // 3.5. Parse 追加行事予定
    // Headers: dates, contents, light_color, dark_color
    fun parseAdditionalEvents(csvText: String): List<AdditionalEventRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<AdditionalEventRow>()
        val header = rows.first().map { it.trim().lowercase() }
        val hasHeader = header.any {
            it.contains("date") || it.contains("日付") ||
                it.contains("content") || it.contains("event") || it.contains("内容") ||
                it.contains("light") || it.contains("dark") || it.contains("color")
        }
        val startIdx = if (hasHeader) 1 else 0

        var dateIdx = 0
        var contentsIdx = 1
        var lightIdx = 2
        var darkIdx = 3

        if (hasHeader) {
            val dIdx = header.indexOfFirst { it.contains("date") || it.contains("日付") }
            if (dIdx >= 0) dateIdx = dIdx

            val cIdx = header.indices.firstOrNull { idx ->
                idx != dateIdx && (
                    header[idx].contains("content") || header[idx].contains("event") ||
                        header[idx].contains("内容") || header[idx].contains("行事")
                    )
            }
            if (cIdx != null) contentsIdx = cIdx

            val lIdx = header.indices.firstOrNull { idx ->
                idx != dateIdx && idx != contentsIdx && header[idx].contains("light")
            }
            if (lIdx != null) lightIdx = lIdx

            val dkIdx = header.indices.firstOrNull { idx ->
                idx != dateIdx && idx != contentsIdx && idx != lightIdx && header[idx].contains("dark")
            }
            if (dkIdx != null) darkIdx = dkIdx
        }

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val dateStr = CsvNormalizer.normalizeDate(row.getOrNull(dateIdx)?.trim() ?: "")
            val (m, d) = parseMonthDay(dateStr) ?: continue
            val contents = row.getOrNull(contentsIdx)
                ?.replace(Regex("(?i)<br\\s*/?>"), "\n")
                ?.trim()
                ?: ""
            if (contents.isBlank()) continue

            val lightColor = normalizeHexColorString(row.getOrNull(lightIdx))
            val darkColor = normalizeHexColorString(row.getOrNull(darkIdx))

            list.add(
                AdditionalEventRow(
                    dateStr = dateStr,
                    month = m,
                    day = d,
                    contents = contents,
                    lightColorHex = lightColor,
                    darkColorHex = darkColor
                )
            )
        }
        return list
    }

    private fun normalizeHexColorString(raw: String?): String {
        val trimmed = raw?.trim() ?: return ""
        if (trimmed.isBlank()) return ""
        val cleaned = if (trimmed.startsWith("#")) trimmed.substring(1) else trimmed
        return if (cleaned.matches(Regex("""[0-9a-fA-F]{6}|[0-9a-fA-F]{8}"""))) {
            "#$cleaned"
        } else {
            ""
        }
    }

    // 4. Parse 祝日
    // Headers: dates, holidays
    fun parseHolidays(csvText: String): List<HolidayRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<HolidayRow>()
        val startIdx = if (rows.first().getOrNull(0)?.contains("date", ignoreCase = true) == true) 1 else 0

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val dateStr = CsvNormalizer.normalizeDate(row[0].trim())
            val (m, d) = parseMonthDay(dateStr) ?: continue
            val holiday = row.getOrNull(1)?.trim() ?: ""
            if (holiday.isNotBlank()) {
                list.add(
                    HolidayRow(
                        dateStr = dateStr,
                        month = m,
                        day = d,
                        holiday = holiday
                    )
                )
            }
        }
        return list
    }

    // 5. Parse 選択科目
    // Headers: origin, electives, name
    fun parseElectives(csvText: String): List<ElectiveItemRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<ElectiveItemRow>()
        val header = rows.first()
        val hasHeader = header.any { it.contains("origin", ignoreCase = true) || it.contains("elective", ignoreCase = true) }
        val startIdx = if (hasHeader) 1 else 0

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.size < 2) continue
            val origin = CsvNormalizer.normalizeSubject(row[0].trim())
            val elective = CsvNormalizer.normalizeSubject(row.getOrNull(1)?.trim() ?: "")
            val name = row.getOrNull(2)?.trim() ?: ""
            if (origin.isNotBlank() && elective.isNotBlank()) {
                list.add(
                    ElectiveItemRow(
                        origin = origin,
                        elective = elective,
                        name = name
                    )
                )
            }
        }
        return list
    }

    // 6. Parse 更新情報
    // Headers: ver/versions, info/dates/contents, link (URLは除外、ただし[title](url)とコードブロックは保持)
    fun parseUpdateHistory(csvText: String): List<UpdateHistoryRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<UpdateHistoryRow>()
        val header = rows.first().map { it.trim().lowercase() }
        val hasHeader = header.any {
            it.contains("ver") || it.contains("info") || it.contains("link") ||
                it.contains("date") || it.contains("content") || it.contains("日付") ||
                it.contains("内容") || it.contains("更新")
        }
        val startIdx = if (hasHeader) 1 else 0

        var verCol = 0
        var infoCol = 1
        var dateCol = -1
        var linkCol = -1

        if (hasHeader) {
            val v = header.indexOfFirst { it.contains("ver") }
            if (v >= 0) verCol = v

            val l = header.indices.firstOrNull { idx -> idx != verCol && (header[idx].contains("link") || header[idx].contains("url")) }
            if (l != null) linkCol = l

            val explicitInfo = header.indices.firstOrNull { idx ->
                idx != verCol && idx != linkCol && (
                    header[idx].contains("info") || header[idx].contains("content") ||
                        header[idx].contains("内容") || header[idx].contains("詳細") ||
                        header[idx].contains("更新内容")
                    )
            }
            val fallbackUpdateInfo = if (explicitInfo != null) {
                explicitInfo
            } else {
                header.indices.firstOrNull { idx ->
                    idx != verCol && idx != linkCol &&
                        header[idx].contains("更新") &&
                        !header[idx].contains("date") &&
                        !header[idx].contains("日付") &&
                        !header[idx].contains("日")
                } ?: -1
            }

            val d = header.indices.firstOrNull { idx ->
                idx != verCol && idx != linkCol && idx != fallbackUpdateInfo && (
                    header[idx].contains("date") || header[idx].contains("日付") ||
                        header[idx].contains("更新日") || header[idx] == "日"
                    )
            }
            if (d != null) dateCol = d

            infoCol = if (fallbackUpdateInfo >= 0) {
                fallbackUpdateInfo
            } else {
                header.indices.firstOrNull { idx ->
                    idx != verCol && idx != dateCol && idx != linkCol
                } ?: 1
            }
        }

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val version = row.getOrNull(verCol)?.trim() ?: ""
            if (version.isBlank()) continue

            var dateStr = if (dateCol >= 0 && dateCol != infoCol) row.getOrNull(dateCol)?.trim() ?: "" else ""
            var rawContents = row.getOrNull(infoCol)?.trim() ?: ""

            // ヘッダーがなく、3列以上かつ1列目が日付形式の場合
            if (!hasHeader && row.size >= 3) {
                val c1 = row[1].trim()
                val c2 = row[2].trim()
                if (c1.matches(Regex("""\d{1,4}[-/年]\d{1,2}[-/月]?\d{0,2}日?"""))) {
                    dateStr = c1
                    rawContents = c2
                }
            }

            val cleanedContents = cleanUpdateContents(rawContents, stripBareUrls = true)
            val normalizedVer = normalizeVersionKey(version)
            val isDuplicate = list.any {
                normalizeVersionKey(it.version) == normalizedVer &&
                    it.dateStr == dateStr &&
                    it.contents == cleanedContents
            }
            if (!isDuplicate) {
                list.add(
                    UpdateHistoryRow(
                        version = version,
                        dateStr = dateStr,
                        contents = cleanedContents
                    )
                )
            }
        }
        return list
    }

    private fun normalizeVersionKey(version: String): String {
        return version.trim()
            .replace(Regex("""^(version|ver|v)\s*""", RegexOption.IGNORE_CASE), "")
            .trim()
            .lowercase()
    }

    /**
     * 更新内容のテキストを整形する。
     * コードブロック (```...```) と Markdownリンク ([title](https://...)) の内部は改変せず保持し、
     * プレーンテキスト部分の連結した「・」への改行挿入および任意の生URL除外を行う。
     */
    fun cleanUpdateContents(rawContents: String, stripBareUrls: Boolean = true): String {
        val normalized = rawContents.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (normalized.isEmpty()) return ""

        val protectedPattern = Regex("""```[\s\S]*?```|\[[^\]]+\]\(https?://[^\s)]+\)""")
        val sb = StringBuilder()
        var lastIndex = 0

        for (match in protectedPattern.findAll(normalized)) {
            if (match.range.first > lastIndex) {
                val plainPart = normalized.substring(lastIndex, match.range.first)
                sb.append(cleanPlainUpdateSegment(plainPart, stripBareUrls))
            }
            // コードブロックやMarkdownリンクはそのまま保持
            sb.append(match.value)
            lastIndex = match.range.last + 1
        }
        if (lastIndex < normalized.length) {
            sb.append(cleanPlainUpdateSegment(normalized.substring(lastIndex), stripBareUrls))
        }

        return sb.toString()
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    private fun cleanPlainUpdateSegment(segment: String, stripBareUrls: Boolean): String {
        var result = if (stripBareUrls) {
            segment.replace(Regex("""https?://\S+"""), "")
        } else {
            segment
        }
        result = result.replace(Regex("""(?<=[^\n・])\s*・"""), "\n・")
        return result
    }

    // Parse アプリ更新情報 (ver, info, link)
    // 最新行（最初のデータ行）を取得
    fun parseAppUpdateInfo(csvText: String): AppUpdateInfo? {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return null

        val header = rows.first().map { it.trim().lowercase() }
        val hasHeader = header.any {
            it.contains("ver") || it.contains("info") || it.contains("link") ||
                it.contains("content") || it.contains("内容") || it.contains("更新")
        }
        val startIdx = if (hasHeader) 1 else 0

        var verCol = 0
        var infoCol = 1
        var linkCol = 2

        if (hasHeader) {
            val v = header.indexOfFirst { it.contains("ver") }
            if (v >= 0) verCol = v

            val l = header.indices.firstOrNull { idx -> idx != verCol && (header[idx].contains("link") || header[idx].contains("url")) }
            if (l != null) linkCol = l

            val i = header.indices.firstOrNull { idx ->
                idx != verCol && idx != linkCol && (
                    header[idx].contains("info") || header[idx].contains("content") ||
                        header[idx].contains("内容") || header[idx].contains("詳細") ||
                        (header[idx].contains("更新") && !header[idx].contains("日") && !header[idx].contains("date"))
                    )
            }
            if (i != null) {
                infoCol = i
            } else {
                infoCol = header.indices.firstOrNull { idx ->
                    idx != verCol && idx != linkCol && !header[idx].contains("date") && !header[idx].contains("日付")
                } ?: 1
            }
        }

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val version = row.getOrNull(verCol)?.trim() ?: ""
            val rawInfo = row.getOrNull(infoCol)?.trim() ?: ""
            val rawLink = row.getOrNull(linkCol)?.trim() ?: ""
            if (version.isNotBlank()) {
                val info = cleanUpdateContents(rawInfo, stripBareUrls = false)
                val resolvedLink = AppUpdateInfo.buildDownloadUrl(version, rawLink)
                return AppUpdateInfo(
                    version = version,
                    info = info,
                    link = resolvedLink
                )
            }
        }
        return null
    }

    // 7. Parse 時間割変更届
    // Headers: dates, classes, periods, subjects
    fun parseTimetableChanges(csvText: String): List<TimetableChangeNotificationRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<TimetableChangeNotificationRow>()
        val header = rows.first()
        val hasHeader = header.any { it.contains("date", ignoreCase = true) || it.contains("class", ignoreCase = true) || it.contains("period", ignoreCase = true) }
        val startIdx = if (hasHeader) 1 else 0

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.size < 3) continue
            val dateStr = CsvNormalizer.normalizeDate(row[0].trim())
            if (dateStr.isBlank()) continue
            val (m, d) = parseMonthDay(dateStr) ?: continue
            val classId = CsvNormalizer.normalizeClassId(row[1].trim())
            val period = CsvNormalizer.normalizePeriod(row[2].trim())
            val rawSubject = if (row.size >= 4) row[3].trim() else ""
            val subject = CsvNormalizer.normalizeSubject(rawSubject)

            list.add(
                TimetableChangeNotificationRow(
                    dateStr = dateStr,
                    month = m,
                    day = d,
                    classId = classId,
                    period = period,
                    subject = subject
                )
            )
        }
        return list
    }

    // 8. Parse 講座変更
    // Headers: dates, periods, previous_course, new_course
    fun parseCourseChanges(csvText: String): List<CourseChangeNotificationRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<CourseChangeNotificationRow>()
        val header = rows.first()
        val hasHeader = header.any { it.contains("date", ignoreCase = true) || it.contains("period", ignoreCase = true) || it.contains("course", ignoreCase = true) }
        val startIdx = if (hasHeader) 1 else 0

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.size < 4) continue
            val dateStr = CsvNormalizer.normalizeDate(row[0].trim())
            val (m, d) = parseMonthDay(dateStr) ?: continue
            val period = CsvNormalizer.normalizePeriod(row[1].trim())
            val previousCourse = CsvNormalizer.normalizeSubject(row[2].trim())
            val newCourse = CsvNormalizer.normalizeSubject(row[3].trim())

            if (newCourse.isNotBlank()) {
                list.add(
                    CourseChangeNotificationRow(
                        dateStr = dateStr,
                        month = m,
                        day = d,
                        period = period,
                        previousCourse = previousCourse,
                        newCourse = newCourse
                    )
                )
            }
        }
        return list
    }

    // 9. Parse 考査時間割
    // Headers: dates, periods, subjects, start_time, end_time, classroom
    fun parseExamSchedule(csvText: String): List<ExamScheduleRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<ExamScheduleRow>()
        val header = rows.first()
        val hasHeader = header.any { h ->
            val lower = h.lowercase()
            lower.contains("date") || lower.contains("日付") ||
            lower.contains("period") || lower.contains("時限") || lower.contains("限") ||
            lower.contains("subject") || lower.contains("科目") ||
            lower.contains("start") || lower.contains("開始") ||
            lower.contains("考査") || lower.contains("class") || lower.contains("クラス")
        }

        var dateIdx = 0
        var classIdx = -1
        var periodIdx = 1
        var subjectIdx = 2
        var startIdx = 3
        var endIdx = 4
        var roomIdx = 5

        val startRowIdx = if (hasHeader) {
            // ヘッダー名から動的にカラム位置をマッピング
            var foundDate = false
            var foundClass = false
            var foundPeriod = false
            var foundSubject = false
            var foundStart = false
            var foundEnd = false
            var foundRoom = false

            for (c in header.indices) {
                val col = header[c].lowercase()
                if (!foundDate && (col.contains("date") || col.contains("日付") || col.contains("日"))) {
                    dateIdx = c
                    foundDate = true
                } else if (!foundPeriod && (col.contains("period") || col.contains("時限") || col.contains("限"))) {
                    periodIdx = c
                    foundPeriod = true
                } else if (!foundSubject && (col.contains("subject") || col.contains("科目") || col.contains("教科") || col.contains("コース"))) {
                    subjectIdx = c
                    foundSubject = true
                } else if (!foundStart && (col.contains("start") || col.contains("開始") || col.contains("自"))) {
                    startIdx = c
                    foundStart = true
                } else if (!foundEnd && (col.contains("end") || col.contains("終了") || col.contains("至"))) {
                    endIdx = c
                    foundEnd = true
                } else if (!foundRoom && (col.contains("room") || col.contains("教室") || col.contains("場所"))) {
                    roomIdx = c
                    foundRoom = true
                } else if (!foundClass && !col.contains("room") && !col.contains("教室") && (col.contains("class") || col.contains("クラス") || col.contains("組"))) {
                    classIdx = c
                    foundClass = true
                }
            }
            1
        } else {
            0
        }

        for (i in startRowIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val dateStr = CsvNormalizer.normalizeDate(row.getOrNull(dateIdx)?.trim() ?: "")
            val (m, d) = parseMonthDay(dateStr) ?: continue

            val periodStr = row.getOrNull(periodIdx)?.trim() ?: "1"
            val period = CsvNormalizer.normalizePeriod(periodStr)
            val subject = CsvNormalizer.normalizeSubject(row.getOrNull(subjectIdx)?.trim() ?: "")
            val startTime = CsvNormalizer.normalizeTime(row.getOrNull(startIdx)?.trim() ?: "")
            val endTime = CsvNormalizer.normalizeTime(row.getOrNull(endIdx)?.trim() ?: "")
            val rawRoom = if (roomIdx >= 0) row.getOrNull(roomIdx)?.trim() ?: "" else ""
            val classroom = CsvNormalizer.normalizeAlphanumeric(rawRoom)
            val rawClass = if (classIdx >= 0) row.getOrNull(classIdx)?.trim() ?: "全" else "全"
            val classId = CsvNormalizer.normalizeClassId(if (rawClass.isNotBlank()) rawClass else "全")

            if (subject.isNotBlank()) {
                list.add(
                    ExamScheduleRow(
                        dateStr = dateStr,
                        month = m,
                        day = d,
                        period = period,
                        subject = subject,
                        startTime = startTime,
                        endTime = endTime,
                        classroom = classroom,
                        classId = if (classId.isNotBlank()) classId else "全"
                    )
                )
            }
        }
        return list
    }

    /**
     * 10. 科目名対応表（source -> target）のパース
     */
    fun parseSubjectMapping(csvContent: String): List<SubjectMappingRow> {
        val list = mutableListOf<SubjectMappingRow>()
        val rows = parseRawCsv(csvContent)
        if (rows.isEmpty()) return list

        val firstRow = rows[0].map { it.lowercase().trim() }
        var sourceIdx = 0
        var targetIdx = 1

        val hasHeader = firstRow.any { it.contains("source") || it.contains("target") || it.contains("元") || it.contains("先") }
        val startRowIdx = if (hasHeader) {
            firstRow.forEachIndexed { c, col ->
                if (col.contains("source") || col.contains("元") || col.contains("講座")) {
                    sourceIdx = c
                } else if (col.contains("target") || col.contains("先") || col.contains("科目") || col.contains("正式")) {
                    targetIdx = c
                }
            }
            1
        } else {
            0
        }

        for (i in startRowIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val source = CsvNormalizer.normalizeSubject(row.getOrNull(sourceIdx)?.trim() ?: "")
            val target = CsvNormalizer.normalizeSubject(row.getOrNull(targetIdx)?.trim() ?: "")
            if (source.isNotBlank() && target.isNotBlank()) {
                list.add(SubjectMappingRow(source = source, target = target))
            }
        }
        return list
    }

    /**
     * 11. 特別時程時間割 (Special Schedule) のパース
     * Headers / Columns: date, period, subject, start_time, end_time
     */
    fun parseSpecialSchedule(csvText: String): List<SpecialScheduleRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<SpecialScheduleRow>()
        val header = rows.first()
        val hasHeader = header.any { h ->
            val lower = h.lowercase()
            lower.contains("date") || lower.contains("日付") || lower == "日" ||
            lower.contains("period") || lower.contains("時限") || lower.contains("限") ||
            lower.contains("subject") || lower.contains("科目") ||
            lower.contains("start") || lower.contains("開始") ||
            lower.contains("end") || lower.contains("終了")
        }

        var dateIdx = 0
        var periodIdx = 1
        var subjectIdx = 2
        var startIdx = 3
        var endIdx = 4

        val startRowIdx = if (hasHeader) {
            var foundDate = false
            var foundPeriod = false
            var foundSubject = false
            var foundStart = false
            var foundEnd = false

            for (c in header.indices) {
                val col = header[c].lowercase()
                if (!foundDate && (col.contains("date") || col.contains("日付") || col == "日")) {
                    dateIdx = c
                    foundDate = true
                } else if (!foundPeriod && (col.contains("period") || col.contains("時限") || col.contains("限"))) {
                    periodIdx = c
                    foundPeriod = true
                } else if (!foundSubject && (col.contains("subject") || col.contains("科目") || col.contains("教科") || col.contains("コース"))) {
                    subjectIdx = c
                    foundSubject = true
                } else if (!foundStart && (col.contains("start") || col.contains("開始") || col.contains("自"))) {
                    startIdx = c
                    foundStart = true
                } else if (!foundEnd && (col.contains("end") || col.contains("終了") || col.contains("至"))) {
                    endIdx = c
                    foundEnd = true
                }
            }
            1
        } else {
            0
        }

        for (i in startRowIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val dateStr = CsvNormalizer.normalizeDate(row.getOrNull(dateIdx)?.trim() ?: "")
            val (m, d) = parseMonthDay(dateStr) ?: continue

            val periodStr = row.getOrNull(periodIdx)?.trim() ?: "1"
            val period = CsvNormalizer.normalizePeriod(periodStr)
            val subject = CsvNormalizer.normalizeSubject(row.getOrNull(subjectIdx)?.trim() ?: "")
            val startTime = CsvNormalizer.normalizeTime(row.getOrNull(startIdx)?.trim() ?: "")
            val endTime = CsvNormalizer.normalizeTime(row.getOrNull(endIdx)?.trim() ?: "")

            if (subject.isNotBlank()) {
                list.add(
                    SpecialScheduleRow(
                        dateStr = dateStr,
                        month = m,
                        day = d,
                        period = period,
                        subject = subject,
                        startTime = startTime,
                        endTime = endTime
                    )
                )
            }
        }
        return list
    }
}
