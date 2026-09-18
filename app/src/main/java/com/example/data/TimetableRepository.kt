package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.example.data.csv.CsvSyncManager
import com.example.data.csv.AppUpdateInfo
import com.example.data.csv.CsvSyncStatus
import com.example.data.csv.CsvUrlsConfig
import com.example.data.csv.UpdateHistoryRow
import com.example.model.*
import com.example.notification.NotificationHelper
import com.example.widget.EventMemoWidgetProvider
import com.example.widget.TimetableWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TimetableRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE)
    val csvSyncManager = CsvSyncManager(context)

    private val _selectedClass = MutableStateFlow(loadSelectedClass())
    val selectedClass: StateFlow<ClassGroup> = _selectedClass.asStateFlow()

    private val _columnCount = MutableStateFlow(loadColumnCount())
    val columnCount: StateFlow<Int> = _columnCount.asStateFlow()

    // テーブルの表示数 (1〜3, デフォルト 1)
    private val _tableDisplayCount = MutableStateFlow(loadTableDisplayCount())
    val tableDisplayCount: StateFlow<Int> = _tableDisplayCount.asStateFlow()

    private val _cellColorMode = MutableStateFlow(loadCellColorMode())
    val cellColorMode: StateFlow<CellColorMode> = _cellColorMode.asStateFlow()

    // 教科テキスト色分け設定（デフォルトOFF）
    private val _isSubjectColorEnabled = MutableStateFlow(loadSubjectColorEnabled())
    val isSubjectColorEnabled: StateFlow<Boolean> = _isSubjectColorEnabled.asStateFlow()

    // 教科グループ（HSV設定対応）
    private val _subjectColorGroups = MutableStateFlow(loadSubjectColorGroups())
    val subjectColorGroups: StateFlow<List<SubjectColorGroup>> = _subjectColorGroups.asStateFlow()

    // 時間割変更ハイライト設定
    private val _isHighlightChangedPeriods = MutableStateFlow(loadHighlightChangedPeriods())
    val isHighlightChangedPeriods: StateFlow<Boolean> = _isHighlightChangedPeriods.asStateFlow()

    // フォントサイズ設定
    private val _timetableFontSize = MutableStateFlow(loadTimetableFontSize())
    val timetableFontSize: StateFlow<TimetableFontSize> = _timetableFontSize.asStateFlow()

    private val _isDeveloperMode = MutableStateFlow(loadDeveloperMode())
    val isDeveloperMode: StateFlow<Boolean> = _isDeveloperMode.asStateFlow()

    // Notification settings
    private val _isDailyNotificationEnabled = MutableStateFlow(loadDailyNotificationEnabled())
    val isDailyNotificationEnabled: StateFlow<Boolean> = _isDailyNotificationEnabled.asStateFlow()

    private val _notificationHour = MutableStateFlow(loadNotificationHour())
    val notificationHour: StateFlow<Int> = _notificationHour.asStateFlow()

    private val _notificationMinute = MutableStateFlow(loadNotificationMinute())
    val notificationMinute: StateFlow<Int> = _notificationMinute.asStateFlow()

    private val _isNextClassNotificationEnabled = MutableStateFlow(loadNextClassNotificationEnabled())
    val isNextClassNotificationEnabled: StateFlow<Boolean> = _isNextClassNotificationEnabled.asStateFlow()

    private val _nextClassLeadMinutes = MutableStateFlow(loadNextClassLeadMinutes())
    val nextClassLeadMinutes: StateFlow<Int> = _nextClassLeadMinutes.asStateFlow()

    val csvUrlsConfig: StateFlow<CsvUrlsConfig> = csvSyncManager.urlsConfig
    val csvSyncStatus: StateFlow<CsvSyncStatus> = csvSyncManager.syncStatus

    private fun loadDeveloperMode(): Boolean {
        return prefs.getBoolean("is_developer_mode", false)
    }

    fun setDeveloperMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_developer_mode", enabled).apply()
        _isDeveloperMode.value = enabled
    }

    private fun loadSubjectColorEnabled(): Boolean {
        return prefs.getBoolean("subject_color_text_enabled", false)
    }

    fun setSubjectColorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("subject_color_text_enabled", enabled).apply()
        _isSubjectColorEnabled.value = enabled
        TimetableWidgetProvider.updateAllWidgets(context)
    }

    private fun loadSubjectColorGroups(): List<SubjectColorGroup> {
        // v2キー、またはenglishが含まれている完全な設定かを判定
        val version = prefs.getInt("subject_color_groups_version", 0)
        val jsonStr = prefs.getString("subject_color_groups_json", null)
        if (version < 2 || jsonStr.isNullOrBlank()) {
            // 新しいデフォルトグループを保存して反映
            val defaults = SubjectColorDefaults.defaultGroups
            persistSubjectColorGroups(defaults)
            prefs.edit().putInt("subject_color_groups_version", 2).apply()
            return defaults
        }
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<SubjectColorGroup>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val name = obj.optString("name", "グループ")
                val hue = obj.optDouble("hue", 0.0).toFloat()
                val sat = obj.optDouble("saturation", 0.8).toFloat()
                val value = obj.optDouble("value", 0.85).toFloat()
                val subjectsArr = obj.optJSONArray("subjects")
                val subjects = mutableListOf<String>()
                if (subjectsArr != null) {
                    for (j in 0 until subjectsArr.length()) {
                        subjects.add(subjectsArr.getString(j))
                    }
                }
                list.add(SubjectColorGroup(id, name, hue, sat, value, subjects))
            }
            // 古い7グループなどの不完全な設定の場合は最新デフォルトに更新
            if (list.isEmpty() || list.size < 10 || list.none { it.id == "english" }) {
                val defaults = SubjectColorDefaults.defaultGroups
                persistSubjectColorGroups(defaults)
                prefs.edit().putInt("subject_color_groups_version", 2).apply()
                defaults
            } else {
                list
            }
        } catch (_: Exception) {
            val defaults = SubjectColorDefaults.defaultGroups
            persistSubjectColorGroups(defaults)
            prefs.edit().putInt("subject_color_groups_version", 2).apply()
            defaults
        }
    }

    private fun persistSubjectColorGroups(groups: List<SubjectColorGroup>) {
        val array = org.json.JSONArray()
        for (g in groups) {
            val obj = org.json.JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("hue", g.hue)
            obj.put("saturation", g.saturation)
            obj.put("value", g.value)
            val subs = org.json.JSONArray()
            g.subjects.forEach { subs.put(it) }
            obj.put("subjects", subs)
            array.put(obj)
        }
        prefs.edit()
            .putString("subject_color_groups_json", array.toString())
            .putInt("subject_color_groups_version", 2)
            .apply()
    }

    private fun saveSubjectColorGroups(groups: List<SubjectColorGroup>) {
        persistSubjectColorGroups(groups)
        _subjectColorGroups.value = groups
        TimetableWidgetProvider.updateAllWidgets(context)
    }

    fun updateSubjectGroupHsv(groupId: String, hue: Float, saturation: Float, value: Float) {
        val current = _subjectColorGroups.value.toMutableList()
        val idx = current.indexOfFirst { it.id == groupId }
        if (idx != -1) {
            current[idx] = current[idx].copy(
                hue = hue.coerceIn(0f, 360f),
                saturation = saturation.coerceIn(0f, 1f),
                value = value.coerceIn(0f, 1f)
            )
            saveSubjectColorGroups(current)
        }
    }

    fun updateSubjectGroupName(groupId: String, newName: String) {
        val current = _subjectColorGroups.value.toMutableList()
        val idx = current.indexOfFirst { it.id == groupId }
        if (idx != -1 && newName.isNotBlank()) {
            current[idx] = current[idx].copy(name = newName.trim())
            saveSubjectColorGroups(current)
        }
    }

    fun addSubjectGroup(name: String, hue: Float = 180f, saturation: Float = 0.8f, value: Float = 0.85f): SubjectColorGroup {
        val newGroup = SubjectColorGroup(
            id = java.util.UUID.randomUUID().toString(),
            name = name.ifBlank { "新規グループ" },
            hue = hue,
            saturation = saturation,
            value = value,
            subjects = emptyList()
        )
        val current = _subjectColorGroups.value.toMutableList()
        current.add(newGroup)
        saveSubjectColorGroups(current)
        return newGroup
    }

    fun deleteSubjectGroup(groupId: String) {
        val current = _subjectColorGroups.value.filterNot { it.id == groupId }
        if (current.isNotEmpty()) {
            saveSubjectColorGroups(current)
        }
    }

    fun assignSubjectToGroup(subjectName: String, targetGroupId: String) {
        val s = subjectName.trim()
        if (s.isBlank()) return
        val current = _subjectColorGroups.value.map { grp ->
            val updatedSubs = grp.subjects.filterNot { it.equals(s, ignoreCase = true) }
            if (grp.id == targetGroupId) {
                grp.copy(subjects = updatedSubs + s)
            } else {
                grp.copy(subjects = updatedSubs)
            }
        }
        saveSubjectColorGroups(current)
    }

    fun removeSubjectFromGroup(subjectName: String, groupId: String) {
        val s = subjectName.trim()
        val current = _subjectColorGroups.value.map { grp ->
            if (grp.id == groupId) {
                grp.copy(subjects = grp.subjects.filterNot { it.equals(s, ignoreCase = true) })
            } else {
                grp
            }
        }
        saveSubjectColorGroups(current)
    }

    fun resetSubjectColorGroups() {
        prefs.edit().remove("subject_color_groups_json").apply()
        _subjectColorGroups.value = SubjectColorDefaults.defaultGroups
        TimetableWidgetProvider.updateAllWidgets(context)
    }

    private fun loadTableDisplayCount(): Int {
        return prefs.getInt("table_display_count", 1).coerceIn(1, 3)
    }

    fun setTableDisplayCount(count: Int) {
        val valid = count.coerceIn(1, 3)
        prefs.edit().putInt("table_display_count", valid).apply()
        _tableDisplayCount.value = valid
    }

    private fun loadHighlightChangedPeriods(): Boolean {
        return prefs.getBoolean("highlight_changed_periods", true)
    }

    fun setHighlightChangedPeriods(enabled: Boolean) {
        prefs.edit().putBoolean("highlight_changed_periods", enabled).apply()
        _isHighlightChangedPeriods.value = enabled
    }

    private fun loadTimetableFontSize(): TimetableFontSize {
        val name = prefs.getString("timetable_font_size", TimetableFontSize.MEDIUM.name) ?: TimetableFontSize.MEDIUM.name
        return try {
            TimetableFontSize.valueOf(name)
        } catch (_: Exception) {
            TimetableFontSize.MEDIUM
        }
    }

    fun setTimetableFontSize(size: TimetableFontSize) {
        prefs.edit().putString("timetable_font_size", size.name).apply()
        _timetableFontSize.value = size
    }

    private fun loadCellColorMode(): CellColorMode {
        return CellColorMode.UNIFORM
    }

    fun setCellColorMode(mode: CellColorMode = CellColorMode.UNIFORM) {
        prefs.edit().putString("cell_color_mode", CellColorMode.UNIFORM.name).apply()
        _cellColorMode.value = CellColorMode.UNIFORM
    }

    private fun loadColumnCount(): Int {
        return prefs.getInt("table_column_count", 5).coerceIn(3, 7)
    }

    fun setColumnCount(count: Int) {
        val validCount = count.coerceIn(3, 7)
        prefs.edit().putInt("table_column_count", validCount).apply()
        _columnCount.value = validCount
    }

    // Notification settings methods
    private fun loadDailyNotificationEnabled(): Boolean {
        return prefs.getBoolean("daily_notification_enabled", true)
    }

    private fun loadNotificationHour(): Int {
        return prefs.getInt("daily_notification_hour", 7).coerceIn(0, 23)
    }

    private fun loadNotificationMinute(): Int {
        return prefs.getInt("daily_notification_minute", 0).coerceIn(0, 59)
    }

    fun updateDailyNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        prefs.edit()
            .putBoolean("daily_notification_enabled", enabled)
            .putInt("daily_notification_hour", h)
            .putInt("daily_notification_minute", m)
            .apply()

        _isDailyNotificationEnabled.value = enabled
        _notificationHour.value = h
        _notificationMinute.value = m

        if (enabled) {
            NotificationHelper.scheduleDailyNotification(context, h, m)
        } else {
            NotificationHelper.cancelDailyNotification(context)
        }
    }

    fun testSendNotificationNow() {
        NotificationHelper.showTodayTimetableNotification(context)
    }

    private fun loadNextClassNotificationEnabled(): Boolean {
        return prefs.getBoolean("next_class_notification_enabled", true)
    }

    private fun loadNextClassLeadMinutes(): Int {
        val saved = prefs.getInt("next_class_lead_minutes", 5)
        return if (saved in listOf(3, 5, 10, 20)) saved else 5
    }

    fun updateNextClassNotificationSettings(enabled: Boolean, leadMinutes: Int) {
        val validLead = if (leadMinutes in listOf(3, 5, 10, 20)) leadMinutes else 5
        prefs.edit()
            .putBoolean("next_class_notification_enabled", enabled)
            .putInt("next_class_lead_minutes", validLead)
            .apply()

        _isNextClassNotificationEnabled.value = enabled
        _nextClassLeadMinutes.value = validLead

        if (enabled) {
            NotificationHelper.scheduleNextClassAlarms(context)
        } else {
            NotificationHelper.cancelNextClassAlarms(context)
        }
    }

    fun saveCsvUrls(config: CsvUrlsConfig) {
        csvSyncManager.saveUrlsConfig(config)
    }

    suspend fun syncCsvData(): Result<String> {
        val res = csvSyncManager.syncAllCsvs()
        TimetableWidgetProvider.updateAllWidgets(context)
        EventMemoWidgetProvider.updateAllWidgets(context)
        return res
    }

    fun clearAllCsvCache() {
        csvSyncManager.clearAllCache()
    }

    fun getUpdateHistories(): List<UpdateHistoryRow> {
        return csvSyncManager.updateHistories
    }

    fun getLatestAppUpdateInfo(): AppUpdateInfo? {
        return csvSyncManager.latestAppUpdateInfo
    }

    fun getIgnoredUpdateVersion(): String? {
        return prefs.getString("ignored_update_version", null)
    }

    fun setIgnoredUpdateVersion(version: String) {
        prefs.edit().putString("ignored_update_version", version).apply()
    }

    private fun getMemoKey(classId: String, date: LocalDate): String {
        return "memo_${classId}_$date"
    }

    fun hasCompletedInitialSetup(): Boolean {
        return prefs.getBoolean("has_completed_initial_setup", false)
    }

    fun hasAgreedTerms(): Boolean {
        return prefs.getBoolean("has_agreed_terms", false) || hasCompletedInitialSetup()
    }

    fun setAgreedTerms(agreed: Boolean) {
        prefs.edit().putBoolean("has_agreed_terms", agreed).apply()
        TimetableWidgetProvider.updateAllWidgets(context)
        EventMemoWidgetProvider.updateAllWidgets(context)
    }

    fun hasSelectedClass(): Boolean {
        return prefs.getBoolean("has_selected_class", false) || hasCompletedInitialSetup()
    }

    fun setClassSelected(selected: Boolean) {
        prefs.edit().putBoolean("has_selected_class", selected).apply()
        TimetableWidgetProvider.updateAllWidgets(context)
        EventMemoWidgetProvider.updateAllWidgets(context)
    }

    fun hasCompletedTermsAndClass(): Boolean {
        return (hasAgreedTerms() && hasSelectedClass()) || hasCompletedInitialSetup()
    }

    fun setCompletedInitialSetup(completed: Boolean) {
        prefs.edit()
            .putBoolean("has_completed_initial_setup", completed)
            .putBoolean("has_agreed_terms", completed)
            .putBoolean("has_selected_class", completed)
            .apply()
        // When initial setup completes, enable notification if not already
        if (completed && _isDailyNotificationEnabled.value) {
            NotificationHelper.scheduleDailyNotification(context, _notificationHour.value, _notificationMinute.value)
        }
        if (completed && _isNextClassNotificationEnabled.value) {
            NotificationHelper.scheduleNextClassAlarms(context)
        }
        TimetableWidgetProvider.updateAllWidgets(context)
        EventMemoWidgetProvider.updateAllWidgets(context)
    }

    private fun loadSelectedClass(): ClassGroup {
        val savedId = prefs.getString("selected_class_id", "1") ?: "1"
        return DefaultSchoolData.classes.find { it.id == savedId } ?: DefaultSchoolData.classes.first()
    }

    fun setSelectedClass(classGroup: ClassGroup) {
        prefs.edit()
            .putString("selected_class_id", classGroup.id)
            .putBoolean("has_selected_class", true)
            .apply()
        _selectedClass.value = classGroup
        TimetableWidgetProvider.updateAllWidgets(context)
        EventMemoWidgetProvider.updateAllWidgets(context)
    }

    /**
     * Retrieves elective selections for the specified class.
     * Maps origin name -> chosen elective name.
     */
    fun getElectiveSelectionsForClass(classId: String): Map<String, String> {
        val optionsMap = csvSyncManager.getElectiveOptionsForClass(classId)
        val resultMap = LinkedHashMap<String, String>()
        for ((origin, options) in optionsMap) {
            val saved = prefs.getString("elective_${classId}_$origin", null)
            val choice = if (saved != null && (options.contains(saved) || saved.isNotBlank())) {
                saved
            } else {
                ""
            }
            resultMap[origin] = choice
        }
        return resultMap
    }

    fun saveElectiveChoice(classId: String, origin: String, electiveChoice: String) {
        prefs.edit().putString("elective_${classId}_$origin", electiveChoice.trim()).apply()
        TimetableWidgetProvider.updateAllWidgets(context)
        EventMemoWidgetProvider.updateAllWidgets(context)
    }

    fun getDaySchedule(classGroup: ClassGroup, date: LocalDate, baseDate: LocalDate): DaySchedule {
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(baseDate, date)
        val relativeLabel = when (daysDiff) {
            0L -> "今日"
            1L -> "翌日"
            2L -> "2日後"
            3L -> "3日後"
            4L -> "4日後"
            5L -> "5日後"
            6L -> "6日後"
            7L -> "7日後"
            -1L -> "昨日"
            -2L -> "2日前"
            -3L -> "3日前"
            else -> if (daysDiff > 0) "${daysDiff}日後" else "${-daysDiff}日前"
        }

        // Event from CSV (Read only)
        val event = csvSyncManager.getEventForDate(date)

        // Memo (User notes)
        val savedMemo = prefs.getString(getMemoKey(classGroup.id, date), null)
        val memo = savedMemo ?: ""

        // User electives for this class
        val userElectives = getElectiveSelectionsForClass(classGroup.id)

        // Periods 1 to 5 generated from CSV data
        val periods = (1..5).map { period ->
            csvSyncManager.resolvePeriodSchedule(classGroup, date, period, userElectives)
        }

        return DaySchedule(
            date = date,
            dayLabel = relativeLabel,
            periods = periods,
            event = event,
            memo = memo
        )
    }

    fun saveMemo(classId: String, date: LocalDate, memoText: String) {
        prefs.edit().putString(getMemoKey(classId, date), memoText).apply()
        TimetableWidgetProvider.updateAllWidgets(context)
        EventMemoWidgetProvider.updateAllWidgets(context)
    }

    fun getAllEvents(currentClass: ClassGroup): List<SearchResultItem> {
        val results = mutableListOf<SearchResultItem>()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("M月d日 (E)", Locale.JAPANESE)

        // 過去30日から未来180日の行事・祝日
        for (i in -30..180) {
            val date = today.plusDays(i.toLong())
            val eventText = csvSyncManager.getEventForDate(date)
            if (eventText.isNotBlank()) {
                val formatted = formatEventText(eventText)
                results.add(
                    SearchResultItem(
                        date = date,
                        dayLabel = date.format(formatter),
                        type = SearchResultType.EVENT,
                        title = formatted.replace("\n", " / "),
                        snippet = "日付: ${date.monthValue}月${date.dayOfMonth}日"
                    )
                )
            }
        }
        return results.distinctBy { Pair(it.date, it.title) }.sortedBy { it.date }
    }

    fun getAllMemos(classId: String): List<SearchResultItem> {
        val results = mutableListOf<SearchResultItem>()
        val formatter = DateTimeFormatter.ofPattern("M月d日 (E)", Locale.JAPANESE)
        val allEntries = prefs.all
        val prefix = "memo_${classId}_"
        for ((key, value) in allEntries) {
            if (key.startsWith(prefix) && value is String && value.isNotBlank()) {
                val dateStr = key.removePrefix(prefix)
                try {
                    val date = LocalDate.parse(dateStr)
                    val trimmed = value.trim()
                    val firstLine = trimmed.lines().firstOrNull { it.isNotBlank() } ?: "メモ"
                    results.add(
                        SearchResultItem(
                            date = date,
                            dayLabel = date.format(formatter),
                            type = SearchResultType.MEMO,
                            title = firstLine,
                            snippet = trimmed
                        )
                    )
                } catch (_: Exception) {}
            }
        }
        return results.sortedByDescending { it.date }
    }

    fun getAllKnownSubjects(): List<String> {
        return csvSyncManager.getAllKnownSubjects()
    }

    /**
     * Search events, holidays, memos, and subjects across dates.
     * Supports keywords, subject names, classrooms, teacher names, memo contents, and date formats (e.g. 8/26, 8月26日, 水).
     */
    fun searchScheduleAndNotes(classGroup: ClassGroup, query: String): List<SearchResultItem> {
        val rawTrimmed = query.trim()
        if (rawTrimmed.isBlank()) return emptyList()

        val normalized = normalizeQuery(rawTrimmed)
        val results = mutableListOf<SearchResultItem>()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("M月d日 (E)", Locale.JAPANESE)

        // Check if query is a month/day search (e.g. "8/26", "8月26", "8-26")
        val dateMatch = Regex("""^(\d{1,2})[/月\-](\d{1,2})日?""").find(rawTrimmed)
        val searchMonth = dateMatch?.groupValues?.get(1)?.toIntOrNull()
        val searchDay = dateMatch?.groupValues?.get(2)?.toIntOrNull()

        // 1. Search in Events & Holidays from CSV (across school year)
        val currentSchoolYear = if (today.monthValue >= 4) today.year else today.year - 1
        for (eventRow in csvSyncManager.events) {
            val eventNormalized = normalizeQuery(formatEventText(eventRow.event))
            val isDateHit = searchMonth != null && searchDay != null && eventRow.month == searchMonth && eventRow.day == searchDay
            if (isDateHit || eventNormalized.contains(normalized)) {
                val year = if (eventRow.month < 4) currentSchoolYear + 1 else currentSchoolYear
                try {
                    val date = LocalDate.of(year, eventRow.month, eventRow.day)
                    results.add(
                        SearchResultItem(
                            date = date,
                            dayLabel = date.format(formatter),
                            type = SearchResultType.EVENT,
                            title = formatEventText(eventRow.event).replace("\n", " / "),
                            snippet = "日付: ${eventRow.month}月${eventRow.day}日 (行事予定)"
                        )
                    )
                } catch (_: Exception) {}
            }
        }

        for (holidayRow in csvSyncManager.holidays) {
            val holidayNormalized = normalizeQuery(holidayRow.holiday)
            val isDateHit = searchMonth != null && searchDay != null && holidayRow.month == searchMonth && holidayRow.day == searchDay
            if (isDateHit || holidayNormalized.contains(normalized)) {
                val year = if (holidayRow.month < 4) currentSchoolYear + 1 else currentSchoolYear
                try {
                    val date = LocalDate.of(year, holidayRow.month, holidayRow.day)
                    results.add(
                        SearchResultItem(
                            date = date,
                            dayLabel = date.format(formatter),
                            type = SearchResultType.EVENT,
                            title = "祝日: ${holidayRow.holiday}",
                            snippet = "日付: ${holidayRow.month}月${holidayRow.day}日 (祝日)"
                        )
                    )
                } catch (_: Exception) {}
            }
        }

        // 2. Search in saved Memos
        val allEntries = prefs.all
        val prefix = "memo_${classGroup.id}_"
        for ((key, value) in allEntries) {
            if (key.startsWith(prefix) && value is String && value.isNotBlank()) {
                val memoNormalized = normalizeQuery(value)
                val dateStr = key.removePrefix(prefix)
                val parsedDate = try { LocalDate.parse(dateStr) } catch (_: Exception) { null }
                val isDateHit = parsedDate != null && searchMonth != null && searchDay != null && parsedDate.monthValue == searchMonth && parsedDate.dayOfMonth == searchDay

                if (isDateHit || memoNormalized.contains(normalized)) {
                    if (parsedDate != null) {
                        val trimmed = value.trim()
                        val firstLine = trimmed.lines().firstOrNull { it.isNotBlank() } ?: "メモ"
                        results.add(
                            SearchResultItem(
                                date = parsedDate,
                                dayLabel = parsedDate.format(formatter),
                                type = SearchResultType.MEMO,
                                title = firstLine,
                                snippet = trimmed
                            )
                        )
                    }
                }
            }
        }

        // 3. Search timetable subjects & classrooms & direct dates across -30..120 days
        for (i in -30..120) {
            val date = today.plusDays(i.toLong())
            val isDateHit = (searchMonth != null && searchDay != null && date.monthValue == searchMonth && date.dayOfMonth == searchDay) ||
                    (rawTrimmed.length in 1..3 && date.format(DateTimeFormatter.ofPattern("E", Locale.JAPANESE)) == rawTrimmed.removeSuffix("曜").removeSuffix("曜日"))

            val schedule = getDaySchedule(classGroup, date, today)
            val matchedPeriods = schedule.periods.filter {
                val s = normalizeQuery(it.subject)
                val c = normalizeQuery(it.classroom)
                s.contains(normalized) || c.contains(normalized)
            }

            if (isDateHit || matchedPeriods.isNotEmpty()) {
                val periodText = if (matchedPeriods.isNotEmpty()) {
                    matchedPeriods.joinToString(", ") { "${it.period}限: ${it.subject}${if (it.classroom.isNotBlank()) " (${it.classroom})" else ""}" }
                } else {
                    schedule.periods.filter { it.subject.isNotBlank() }.joinToString(", ") { "${it.period}限: ${it.subject}" }.ifBlank { "授業なし" }
                }

                if (results.none { it.date == date && it.type == SearchResultType.SUBJECT }) {
                    results.add(
                        SearchResultItem(
                            date = date,
                            dayLabel = date.format(formatter),
                            type = SearchResultType.SUBJECT,
                            title = "時間割: $periodText",
                            snippet = schedule.event.ifBlank { "通常授業" }.replace("\n", " / ")
                        )
                    )
                }
            }
        }

        return results.distinctBy { Pair(it.date, it.title) }.sortedBy { it.date }
    }

    private fun normalizeQuery(input: String): String {
        // Convert fullwidth alphanumeric/katakana to standard normalized lowercase
        var s = input.trim().lowercase(Locale.JAPANESE)
        // Fullwidth numbers to halfwidth
        val fullwidthNumbers = "０１２３４５６７８９"
        val halfwidthNumbers = "0123456789"
        for (i in fullwidthNumbers.indices) {
            s = s.replace(fullwidthNumbers[i], halfwidthNumbers[i])
        }
        // Fullwidth alphabet to halfwidth
        val fullwidthAlpha = "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ"
        val halfwidthAlpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".lowercase()
        for (i in fullwidthAlpha.indices) {
            s = s.replace(fullwidthAlpha[i], halfwidthAlpha[i % halfwidthAlpha.length])
        }
        return s
    }

    // Memo management utilities
    fun clearAllMemosForClass(classId: String) {
        val editor = prefs.edit()
        val prefix = "memo_${classId}_"
        for (key in prefs.all.keys) {
            if (key.startsWith(prefix)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    fun exportMemosJson(classId: String): String {
        val json = JSONObject()
        val prefix = "memo_${classId}_"
        for ((key, value) in prefs.all) {
            if (key.startsWith(prefix) && value is String) {
                val dateStr = key.removePrefix(prefix)
                json.put(dateStr, value)
            }
        }
        return json.toString(2)
    }

    fun importMemosJson(classId: String, jsonStr: String): Boolean {
        return try {
            val json = JSONObject(jsonStr)
            val editor = prefs.edit()
            val keys = json.keys()
            while (keys.hasNext()) {
                val dateKey = keys.next()
                val text = json.getString(dateKey)
                editor.putString("memo_${classId}_$dateKey", text)
            }
            editor.apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: TimetableRepository? = null

        fun getInstance(context: Context): TimetableRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimetableRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
