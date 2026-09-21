package com.example.data.csv

import java.util.regex.Pattern

object CsvParser {

    /**
     * Parses raw CSV or TSV string into list of rows, where each row is a list of column strings.
     * Supports quotation marks, commas, tabs, escaped quotes (""), and multiline cells.
     */
    fun parseRawCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        if (text.isBlank()) return rows

        val lines = splitCsvLines(text)
        for (line in lines) {
            val cells = parseCsvLine(line)
            if (cells.isNotEmpty() && cells.any { it.isNotBlank() }) {
                rows.add(cells.map { it.trim() })
            }
        }
        return rows
    }

    /**
     * Splits CSV lines while respecting multiline quoted fields.
     */
    private fun splitCsvLines(csvText: String): List<String> {
        val lines = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuote = false

        for (i in csvText.indices) {
            val c = csvText[i]
            if (c == '"') {
                insideQuote = !insideQuote
                sb.append(c)
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
        }
        if (sb.isNotEmpty()) {
            lines.add(sb.toString())
        }
        return lines
    }

    /**
     * Parses a single CSV/TSV line into cell values.
     */
    private fun parseCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var insideQuote = false
        val isTsv = line.contains('\t') && !line.contains(',')
        val delimiter = if (isTsv) '\t' else ','

        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (insideQuote && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++ // Skip escaped quote
                } else {
                    insideQuote = !insideQuote
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
     */
    fun parseMonthDay(dateStr: String): Pair<Int, Int>? {
        val clean = dateStr.trim()
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
            val classId = row[0].trim()
            val type = row.getOrNull(1)?.trim() ?: "A"
            val dayOfWeek = row.getOrNull(2)?.trim() ?: "月"
            val periods = (3..7).map { col ->
                row.getOrNull(col)?.trim() ?: ""
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
            val dateStr = row[0].trim()
            val (m, d) = parseMonthDay(dateStr) ?: continue
            val periods = (1..5).map { col ->
                row.getOrNull(col)?.trim() ?: ""
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
            val dateStr = row[0].trim()
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
            val dateStr = row[0].trim()
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
            val origin = row[0].trim()
            val elective = row.getOrNull(1)?.trim() ?: ""
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
    // Headers: versions/ver, dates/info, contents/link
    fun parseUpdateHistory(csvText: String): List<UpdateHistoryRow> {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return emptyList()

        val list = mutableListOf<UpdateHistoryRow>()
        val startIdx = if (rows.first().getOrNull(0)?.contains("ver", ignoreCase = true) == true) 1 else 0

        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val version = row[0].trim()
            val dateStr = row.getOrNull(1)?.trim() ?: ""
            val contents = row.getOrNull(2)?.trim() ?: ""
            if (version.isNotBlank()) {
                list.add(
                    UpdateHistoryRow(
                        version = version,
                        dateStr = dateStr,
                        contents = contents
                    )
                )
            }
        }
        return list
    }

    // Parse アプリ更新情報 (ver, info, link)
    // 最新行（最初のデータ行）を取得
    fun parseAppUpdateInfo(csvText: String): AppUpdateInfo? {
        val rows = parseRawCsv(csvText)
        if (rows.isEmpty()) return null

        val startIdx = if (rows.first().getOrNull(0)?.contains("ver", ignoreCase = true) == true) 1 else 0
        for (i in startIdx until rows.size) {
            val row = rows[i]
            if (row.isEmpty()) continue
            val version = row[0].trim()
            val info = row.getOrNull(1)?.trim() ?: ""
            val link = row.getOrNull(2)?.trim() ?: ""
            if (version.isNotBlank()) {
                return AppUpdateInfo(
                    version = version,
                    info = info,
                    link = link
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
            if (row.size < 4) continue
            val dateStr = row[0].trim()
            val (m, d) = parseMonthDay(dateStr) ?: continue
            val classId = row[1].trim()
            val period = row[2].trim().toIntOrNull() ?: 1
            val subject = row[3].trim()

            if (subject.isNotBlank()) {
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
            val dateStr = row[0].trim()
            val (m, d) = parseMonthDay(dateStr) ?: continue
            val period = row[1].trim().toIntOrNull() ?: 1
            val previousCourse = row[2].trim()
            val newCourse = row[3].trim()

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
                } else if (!foundClass && (col.contains("class") || col.contains("クラス") || col.contains("組"))) {
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
            val dateStr = row.getOrNull(dateIdx)?.trim() ?: ""
            val (m, d) = parseMonthDay(dateStr) ?: continue

            val periodStr = row.getOrNull(periodIdx)?.trim() ?: "1"
            val period = periodStr.filter { it.isDigit() }.toIntOrNull() ?: 1
            val subject = row.getOrNull(subjectIdx)?.trim() ?: ""
            val startTime = row.getOrNull(startIdx)?.trim() ?: ""
            val endTime = row.getOrNull(endIdx)?.trim() ?: ""
            val classroom = if (roomIdx >= 0) row.getOrNull(roomIdx)?.trim() ?: "" else ""
            val classId = if (classIdx >= 0) row.getOrNull(classIdx)?.trim() ?: "全" else "全"

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
            val source = row.getOrNull(sourceIdx)?.trim() ?: ""
            val target = row.getOrNull(targetIdx)?.trim() ?: ""
            if (source.isNotBlank() && target.isNotBlank()) {
                list.add(SubjectMappingRow(source = source, target = target))
            }
        }
        return list
    }
}
