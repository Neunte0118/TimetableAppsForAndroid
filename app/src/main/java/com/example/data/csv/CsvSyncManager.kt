package com.example.data.csv

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ClassGroup
import com.example.model.DefaultSchoolData
import com.example.model.PeriodSchedule
import com.example.model.SubjectColorDefaults
import com.example.model.formatEventText
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class CsvSyncManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("csv_sync_prefs", Context.MODE_PRIVATE)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Config & Status Flows
    private val _urlsConfig = MutableStateFlow(loadUrlsConfig())
    val urlsConfig: StateFlow<CsvUrlsConfig> = _urlsConfig.asStateFlow()

    private val _syncStatus = MutableStateFlow(loadSyncStatus())
    val syncStatus: StateFlow<CsvSyncStatus> = _syncStatus.asStateFlow()

    // Parsed In-Memory Caches
    var basicClassSchedule: List<BasicClassScheduleRow> = emptyList()
        private set
    var commonSchedules: List<CommonScheduleRow> = emptyList()
        private set
    var electives: List<ElectiveItemRow> = emptyList()
        private set
    var events: List<EventRow> = emptyList()
        private set
    var holidays: List<HolidayRow> = emptyList()
        private set
    var updateHistories: List<UpdateHistoryRow> = emptyList()
        private set
    var latestAppUpdateInfo: AppUpdateInfo? = null
        private set
    var timetableChanges: List<TimetableChangeNotificationRow> = emptyList()
        private set
    var courseChanges: List<CourseChangeNotificationRow> = emptyList()
        private set
    var examSchedules: List<ExamScheduleRow> = emptyList()
        private set
    var subjectMappings: List<SubjectMappingRow> = emptyList()
        private set
    private var sourceToTargetMap: Map<String, String> = emptyMap()
    private var targetToSourcesMap: Map<String, Set<String>> = emptyMap()

    init {
        loadAndParseLocalData()
    }

    private fun loadUrlsConfig(): CsvUrlsConfig {
        val defaultConfig = CsvUrlsConfig()
        // 過去の別URLがSharedPreferencesに残っていた場合に備え、ユーザー指定の正しいURLを確実に反映
        val savedCommon = prefs.getString("url_common_schedule", null)
        val savedBasic = prefs.getString("url_basic_class", null)
        val savedElectives = prefs.getString("url_electives", null)
        val savedUpdates = prefs.getString("url_updates", null)
        val savedEvents = prefs.getString("url_events", null)
        val savedHolidays = prefs.getString("url_holidays", null)
        val savedTimetableChangeSheet = prefs.getString("url_timetable_change_sheet", null)
        val savedCourseChangeSheet = prefs.getString("url_course_change_sheet", null)
        val savedExamSchedule = prefs.getString("url_exam_schedule", null)
        val savedSubjectMapping = prefs.getString("url_subject_mapping", null)

        val isLegacyUrl = savedCommon != null && (savedCommon.contains("pubhtml") || !savedCommon.contains("gid=598100052"))

        if (isLegacyUrl) {
            // 旧URLをクリアして新しいデフォルトURLを保存
            saveUrlsConfig(defaultConfig)
            return defaultConfig
        }

        // 更新情報URLが旧デフォルト（gid=2144261983）または未設定の場合は、新デフォルトにマイグレーションしキャッシュもクリア
        val isLegacyUpdates = savedUpdates != null && savedUpdates.contains("gid=2144261983")
        val effectiveUpdates = if (isLegacyUpdates || savedUpdates.isNullOrBlank()) {
            prefs.edit()
                .putString("url_updates", defaultConfig.updatesUrl)
                .remove("csv_cache_updates")
                .apply()
            defaultConfig.updatesUrl
        } else {
            savedUpdates
        }

        val effectiveExamSchedule = if (savedExamSchedule.isNullOrBlank()) {
            prefs.edit()
                .putString("url_exam_schedule", defaultConfig.examScheduleUrl)
                .apply()
            defaultConfig.examScheduleUrl
        } else {
            savedExamSchedule
        }

        val effectiveSubjectMapping = if (savedSubjectMapping.isNullOrBlank()) {
            prefs.edit()
                .putString("url_subject_mapping", defaultConfig.subjectMappingUrl)
                .apply()
            defaultConfig.subjectMappingUrl
        } else {
            savedSubjectMapping
        }

        return CsvUrlsConfig(
            commonScheduleUrl = savedCommon ?: defaultConfig.commonScheduleUrl,
            basicClassUrl = savedBasic ?: defaultConfig.basicClassUrl,
            electivesUrl = savedElectives ?: defaultConfig.electivesUrl,
            updatesUrl = effectiveUpdates,
            eventsUrl = savedEvents ?: defaultConfig.eventsUrl,
            holidaysUrl = savedHolidays ?: defaultConfig.holidaysUrl,
            timetableChangeSheetUrl = savedTimetableChangeSheet ?: defaultConfig.timetableChangeSheetUrl,
            courseChangeSheetUrl = savedCourseChangeSheet ?: defaultConfig.courseChangeSheetUrl,
            examScheduleUrl = effectiveExamSchedule,
            subjectMappingUrl = effectiveSubjectMapping,
            reportUrl = prefs.getString("url_report", defaultConfig.reportUrl) ?: defaultConfig.reportUrl,
            timetableChangeUrl = prefs.getString("url_timetable_change", defaultConfig.timetableChangeUrl) ?: defaultConfig.timetableChangeUrl
        )
    }

    fun saveUrlsConfig(config: CsvUrlsConfig) {
        val oldUpdatesUrl = prefs.getString("url_updates", null)
        val edit = prefs.edit()
            .putString("url_common_schedule", config.commonScheduleUrl.trim())
            .putString("url_basic_class", config.basicClassUrl.trim())
            .putString("url_electives", config.electivesUrl.trim())
            .putString("url_updates", config.updatesUrl.trim())
            .putString("url_events", config.eventsUrl.trim())
            .putString("url_holidays", config.holidaysUrl.trim())
            .putString("url_timetable_change_sheet", config.timetableChangeSheetUrl.trim())
            .putString("url_course_change_sheet", config.courseChangeSheetUrl.trim())
            .putString("url_exam_schedule", config.examScheduleUrl.trim())
            .putString("url_subject_mapping", config.subjectMappingUrl.trim())
            .putString("url_report", config.reportUrl.trim())
            .putString("url_timetable_change", config.timetableChangeUrl.trim())

        if (oldUpdatesUrl != null && oldUpdatesUrl != config.updatesUrl.trim()) {
            // URLが変更された場合は更新キャッシュを削除して古い情報が残らないようにする
            edit.remove("csv_cache_updates")
        }
        edit.apply()
        _urlsConfig.value = config
    }

    private fun loadSyncStatus(): CsvSyncStatus {
        return CsvSyncStatus(
            isSyncing = false,
            lastSyncTime = prefs.getString("last_sync_time", null),
            lastSyncSuccess = prefs.getBoolean("last_sync_success", true),
            lastSyncMessage = prefs.getString("last_sync_message", "外部CSV同期待機中") ?: "外部CSV同期待機中"
        )
    }

    fun loadAndParseLocalData() {
        val basicCsv = prefs.getString("csv_cache_basic_class", null) ?: DefaultCsvData.BASIC_CLASS_CSV
        val commonCsv = prefs.getString("csv_cache_common_schedule", null) ?: DefaultCsvData.COMMON_SCHEDULE_CSV
        val electivesCsv = prefs.getString("csv_cache_electives", null) ?: DefaultCsvData.ELECTIVES_CSV
        val eventsCsv = prefs.getString("csv_cache_events", null) ?: DefaultCsvData.EVENTS_CSV
        val holidaysCsv = prefs.getString("csv_cache_holidays", null) ?: DefaultCsvData.HOLIDAYS_CSV
        val updatesCsv = prefs.getString("csv_cache_updates", null) ?: DefaultCsvData.UPDATE_HISTORY_CSV
        val timetableChangesCsv = prefs.getString("csv_cache_timetable_changes", null) ?: DefaultCsvData.TIMETABLE_CHANGE_CSV
        val courseChangesCsv = prefs.getString("csv_cache_course_changes", null) ?: DefaultCsvData.COURSE_CHANGE_CSV
        val examScheduleCsv = prefs.getString("csv_cache_exam_schedule", null) ?: DefaultCsvData.EXAM_SCHEDULE_CSV
        val subjectMappingCsv = prefs.getString("csv_cache_subject_mappings", null) ?: DefaultCsvData.SUBJECT_MAPPING_CSV

        basicClassSchedule = CsvParser.parseBasicClassSchedule(basicCsv)
        commonSchedules = CsvParser.parseCommonSchedule(commonCsv)
        electives = CsvParser.parseElectives(electivesCsv)
        events = CsvParser.parseEvents(eventsCsv)
        holidays = CsvParser.parseHolidays(holidaysCsv)
        updateHistories = CsvParser.parseUpdateHistory(updatesCsv)
        latestAppUpdateInfo = CsvParser.parseAppUpdateInfo(updatesCsv)
        timetableChanges = CsvParser.parseTimetableChanges(timetableChangesCsv)
        courseChanges = CsvParser.parseCourseChanges(courseChangesCsv)
        examSchedules = CsvParser.parseExamSchedule(examScheduleCsv)
        updateSubjectMappings(CsvParser.parseSubjectMapping(subjectMappingCsv))

        _syncStatus.value = _syncStatus.value.copy(
            basicCount = basicClassSchedule.size,
            commonCount = commonSchedules.size,
            electiveCount = electives.size,
            eventCount = events.size,
            holidayCount = holidays.size,
            updateCount = updateHistories.size,
            timetableChangeCount = timetableChanges.size,
            courseChangeCount = courseChanges.size,
            examCount = examSchedules.size,
            subjectMappingCount = subjectMappings.size
        )
    }

    fun updateSubjectMappings(list: List<SubjectMappingRow>) {
        subjectMappings = list
        val sToT = mutableMapOf<String, String>()
        val tToS = mutableMapOf<String, MutableSet<String>>()
        // デフォルトで「現世読」->「現代世界を読む」のマッピングを登録
        sToT["現世読"] = "現代世界を読む"
        tToS.getOrPut("現代世界を読む") { mutableSetOf() }.add("現世読")
        for (item in list) {
            val src = item.source.trim()
            val tgt = item.target.trim()
            if (src.isNotEmpty() && tgt.isNotEmpty()) {
                sToT[src] = tgt
                tToS.getOrPut(tgt) { mutableSetOf() }.add(src)
            }
        }
        sourceToTargetMap = sToT
        targetToSourcesMap = tToS
    }

    fun setSubjectMappingsForTesting(list: List<SubjectMappingRow>) {
        updateSubjectMappings(list)
    }

    fun clearAllCache() {
        prefs.edit()
            .remove("csv_cache_basic_class")
            .remove("csv_cache_common_schedule")
            .remove("csv_cache_electives")
            .remove("csv_cache_events")
            .remove("csv_cache_holidays")
            .remove("csv_cache_updates")
            .remove("csv_cache_timetable_changes")
            .remove("csv_cache_course_changes")
            .remove("csv_cache_exam_schedule")
            .remove("csv_cache_subject_mappings")
            .remove("last_sync_time")
            .remove("last_sync_message")
            .apply()
        loadAndParseLocalData()
    }

    /**
     * Downloads CSVs from the configured URLs concurrently.
     */
    suspend fun syncAllCsvs(): Result<String> = withContext(Dispatchers.IO) {
        _syncStatus.value = _syncStatus.value.copy(isSyncing = true, lastSyncMessage = "CSVデータをダウンロード中...")

        val config = _urlsConfig.value
        val errors = mutableListOf<String>()
        var updatedCount = 0

        coroutineScope {
            val commonDeferred = if (config.commonScheduleUrl.isNotBlank()) {
                async { fetchUrlContent(config.commonScheduleUrl) }
            } else null

            val basicDeferred = if (config.basicClassUrl.isNotBlank()) {
                async { fetchUrlContent(config.basicClassUrl) }
            } else null

            val electivesDeferred = if (config.electivesUrl.isNotBlank()) {
                async { fetchUrlContent(config.electivesUrl) }
            } else null

            val updatesDeferred = if (config.updatesUrl.isNotBlank()) {
                async { fetchUrlContent(config.updatesUrl) }
            } else null

            val eventsDeferred = if (config.eventsUrl.isNotBlank()) {
                async { fetchUrlContent(config.eventsUrl) }
            } else null

            val holidaysDeferred = if (config.holidaysUrl.isNotBlank()) {
                async { fetchUrlContent(config.holidaysUrl) }
            } else null

            val timetableChangesDeferred = if (config.timetableChangeSheetUrl.isNotBlank()) {
                async { fetchUrlContent(config.timetableChangeSheetUrl) }
            } else null

            val courseChangesDeferred = if (config.courseChangeSheetUrl.isNotBlank()) {
                async { fetchUrlContent(config.courseChangeSheetUrl) }
            } else null

            val examDeferred = if (config.examScheduleUrl.isNotBlank()) {
                async { fetchUrlContent(config.examScheduleUrl) }
            } else null

            val subjectMappingDeferred = if (config.subjectMappingUrl.isNotBlank()) {
                async { fetchUrlContent(config.subjectMappingUrl) }
            } else null

            // 結果を並列待機して反映
            commonDeferred?.await()?.onSuccess { text ->
                prefs.edit().putString("csv_cache_common_schedule", text).apply()
                commonSchedules = CsvParser.parseCommonSchedule(text)
                updatedCount++
            }?.onFailure { errors.add("時間割(予定): ${it.localizedMessage}") }

            basicDeferred?.await()?.onSuccess { text ->
                prefs.edit().putString("csv_cache_basic_class", text).apply()
                basicClassSchedule = CsvParser.parseBasicClassSchedule(text)
                updatedCount++
            }?.onFailure { errors.add("クラス別時間割: ${it.localizedMessage}") }

            electivesDeferred?.await()?.onSuccess { text ->
                prefs.edit().putString("csv_cache_electives", text).apply()
                electives = CsvParser.parseElectives(text)
                updatedCount++
            }?.onFailure { errors.add("選択科目: ${it.localizedMessage}") }

            updatesDeferred?.await()?.onSuccess { text ->
                prefs.edit().putString("csv_cache_updates", text).apply()
                updateHistories = CsvParser.parseUpdateHistory(text)
                latestAppUpdateInfo = CsvParser.parseAppUpdateInfo(text)
                updatedCount++
            }?.onFailure { errors.add("更新情報: ${it.localizedMessage}") }

            eventsDeferred?.await()?.onSuccess { text ->
                prefs.edit().putString("csv_cache_events", text).apply()
                events = CsvParser.parseEvents(text)
                updatedCount++
            }?.onFailure { errors.add("行事予定: ${it.localizedMessage}") }

            holidaysDeferred?.await()?.onSuccess { text ->
                prefs.edit().putString("csv_cache_holidays", text).apply()
                holidays = CsvParser.parseHolidays(text)
                updatedCount++
            }?.onFailure { errors.add("祝日: ${it.localizedMessage}") }

            timetableChangesDeferred?.await()?.onSuccess { text ->
                prefs.edit().putString("csv_cache_timetable_changes", text).apply()
                timetableChanges = CsvParser.parseTimetableChanges(text)
                updatedCount++
            }?.onFailure { errors.add("時間割変更届: ${it.localizedMessage}") }

            courseChangesDeferred?.await()?.onSuccess { text ->
                prefs.edit().putString("csv_cache_course_changes", text).apply()
                courseChanges = CsvParser.parseCourseChanges(text)
                updatedCount++
            }?.onFailure { errors.add("講座変更: ${it.localizedMessage}") }

            examDeferred?.await()?.onSuccess { text ->
                prefs.edit().putString("csv_cache_exam_schedule", text).apply()
                examSchedules = CsvParser.parseExamSchedule(text)
                updatedCount++
            }?.onFailure { errors.add("考査時間割: ${it.localizedMessage}") }

            subjectMappingDeferred?.await()?.onSuccess { text ->
                prefs.edit().putString("csv_cache_subject_mappings", text).apply()
                val parsed = CsvParser.parseSubjectMapping(text)
                updateSubjectMappings(parsed)
                updatedCount++
            }?.onFailure { errors.add("科目対応表: ${it.localizedMessage}") }
        }

        if (updatedCount == 0 && errors.isEmpty()) {
            loadAndParseLocalData()
        }

        val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("M/d HH:mm:ss", Locale.JAPANESE))
        val isSuccess = updatedCount > 0 || errors.isEmpty()
        val message = if (updatedCount > 0) {
            if (errors.isEmpty()) "全${updatedCount}種類のCSV同期に成功しました ($nowStr)"
            else "${updatedCount}種類のCSV同期に成功しました（一部未更新）($nowStr)"
        } else if (errors.isEmpty()) {
            "CSVデータを読み込みました"
        } else {
            "インターネットに接続されていません。キャッシュデータを表示しています"
        }

        prefs.edit()
            .putString("last_sync_time", nowStr)
            .putBoolean("last_sync_success", isSuccess)
            .putString("last_sync_message", message)
            .apply()

        _syncStatus.value = CsvSyncStatus(
            isSyncing = false,
            lastSyncTime = nowStr,
            lastSyncSuccess = isSuccess,
            lastSyncMessage = message,
            basicCount = basicClassSchedule.size,
            commonCount = commonSchedules.size,
            electiveCount = electives.size,
            eventCount = events.size,
            holidayCount = holidays.size,
            updateCount = updateHistories.size,
            timetableChangeCount = timetableChanges.size,
            courseChangeCount = courseChanges.size,
            examCount = examSchedules.size,
            subjectMappingCount = subjectMappings.size
        )

        if (isSuccess) Result.success(message) else Result.failure(Exception(message))
    }

    private fun fetchUrlContent(url: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                Result.success(body)
            } else {
                Result.failure(Exception("HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Helper to tokenize a string for natural order sorting, supporting numbers and circled numbers (①〜⑳).
     */
    private fun tokenizeForNaturalSort(s: String): List<Any> {
        val list = mutableListOf<Any>()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c in '\u2460'..'\u2473') {
                val num = (c - '\u2460' + 1).toLong()
                list.add(num)
                i++
            } else if (c in '\u3251'..'\u325F') {
                val num = (c - '\u3251' + 21).toLong()
                list.add(num)
                i++
            } else if (c in '0'..'9' || c in '０'..'９') {
                var j = i
                var num = 0L
                while (j < s.length && (s[j] in '0'..'9' || s[j] in '０'..'９')) {
                    val digit = if (s[j] in '0'..'9') s[j] - '0' else s[j] - '０'
                    num = num * 10 + digit
                    j++
                }
                list.add(num)
                i = j
            } else {
                var j = i
                while (j < s.length && !(s[j] in '\u2460'..'\u2473') && !(s[j] in '\u3251'..'\u325F') && !(s[j] in '0'..'9' || s[j] in '０'..'９')) {
                    j++
                }
                list.add(s.substring(i, j))
                i = j
            }
        }
        return list
    }

    /**
     * 科目名の対応先（考査科目名・正式科目名）を取得。
     * マッピングに存在しない場合はそのままトリムした文字列を返す。
     */
    fun getTargetSubject(source: String): String {
        val s = source.trim()
        if (s.isEmpty()) return ""
        return sourceToTargetMap[s]
            ?: sourceToTargetMap.entries.find { it.key.equals(s, ignoreCase = true) }?.value
            ?: s
    }

    /**
     * 2つの科目名が合致するかどうかを判定。
     * ユーザー指示「科目名の正規化はいりません」に基づき、正規化処理（末尾番号除去等）は行わず、
     * 完全一致および科目名対応表（マッピング: source <-> target）を用いて照合します。
     */
    fun isSubjectMatch(name1: String, name2: String): Boolean {
        val s1 = name1.trim()
        val s2 = name2.trim()
        if (s1.isEmpty() || s2.isEmpty()) return false
        if (s1.equals(s2, ignoreCase = true)) return true

        // 「現世読」と「現代世界を読む」/「現読」の同義語対応
        val norm1 = if (s1 == "現世読") "現代世界を読む" else s1
        val norm2 = if (s2 == "現世読") "現代世界を読む" else s2
        if (norm1.equals(norm2, ignoreCase = true)) return true

        // Science L match (物L <-> 物理L, 化L <-> 化学L, etc.)
        val sci1 = ScienceLType.fromSubject(norm1)
        val sci2 = ScienceLType.fromSubject(norm2)
        if (sci1 != null && sci2 != null) {
            if (sci1 != sci2) return false
            val num1 = norm1.filter { it in '\u2460'..'\u2473' || it in '0'..'9' || it in '０'..'９' }
            val num2 = norm2.filter { it in '\u2460'..'\u2473' || it in '0'..'9' || it in '０'..'９' }
            if (num1.isNotEmpty() && num2.isNotEmpty() && num1 != num2) {
                return false
            }
            return true
        }

        // 1) s1 が source で s2 が target
        val target1 = sourceToTargetMap[norm1]
            ?: sourceToTargetMap.entries.find { it.key.equals(norm1, ignoreCase = true) }?.value
        if (target1 != null && target1.equals(norm2, ignoreCase = true)) return true

        // 2) s2 が source で s1 が target
        val target2 = sourceToTargetMap[norm2]
            ?: sourceToTargetMap.entries.find { it.key.equals(norm2, ignoreCase = true) }?.value
        if (target2 != null && target2.equals(norm1, ignoreCase = true)) return true

        // 3) 両方が source であり同一の target を参照している場合 (例: ⅡLa① と ⅡLa②)
        if (target1 != null && target2 != null && target1.equals(target2, ignoreCase = true)) {
            // 講座番号等の識別子が異なる場合（例: 日特① と 日特②）は別科目
            val num1 = s1.filter { it in '\u2460'..'\u2473' || it in '0'..'9' || it in '０'..'９' }
            val num2 = s2.filter { it in '\u2460'..'\u2473' || it in '0'..'9' || it in '０'..'９' }
            if (num1.isNotEmpty() && num2.isNotEmpty() && num1 != num2) {
                return false
            }
            return true
        }

        // 4) target から source 群への逆引き照合
        val sources1 = targetToSourcesMap[norm1]
            ?: targetToSourcesMap.entries.find { it.key.equals(norm1, ignoreCase = true) }?.value
        if (sources1 != null && sources1.any { it.equals(norm2, ignoreCase = true) }) return true

        val sources2 = targetToSourcesMap[norm2]
            ?: targetToSourcesMap.entries.find { it.key.equals(norm2, ignoreCase = true) }?.value
        if (sources2 != null && sources2.any { it.equals(norm1, ignoreCase = true) }) return true

        return false
    }

    private fun compareNaturalOrder(a: String, b: String): Int {
        val tokensA = tokenizeForNaturalSort(a)
        val tokensB = tokenizeForNaturalSort(b)
        val len = minOf(tokensA.size, tokensB.size)
        for (i in 0 until len) {
            val tA = tokensA[i]
            val tB = tokensB[i]
            if (tA is Long && tB is Long) {
                val cmp = tA.compareTo(tB)
                if (cmp != 0) return cmp
            } else {
                val cmp = tA.toString().compareTo(tB.toString(), ignoreCase = true)
                if (cmp != 0) return cmp
            }
        }
        return tokensA.size.compareTo(tokensB.size)
    }

    /**
     * Retrieves elective options for a specific class (1-9組).
     * Returns a map of Origin Subject Name -> List of Elective Names.
     * Excludes elective subjects not taken by this class, and sorts origins and options in natural order.
     */
    fun getElectiveOptionsForClass(classId: String): Map<String, List<String>> {
        val resultMap = LinkedHashMap<String, MutableList<String>>()
        val rawId = classId.replace("組", "").trim()

        // 1. Discover origin subjects used in basicClassSchedule for this class
        val classBasicRows = basicClassSchedule.filter {
            val rowClass = it.classId.trim().replace("組", "")
            rowClass == rawId || rowClass == classId
        }

        val periodsInClass = classBasicRows.flatMap { it.periods }.map { it.trim() }.filter { it.isNotBlank() }.toSet()

        // 2. Collect all distinct origins from electives CSV
        val allCsvOrigins = electives.map { it.origin.trim() }.distinct().filter { it.isNotBlank() }

        // Filter: Only include origins that appear in this class's schedule (exclude others)
        // If periodsInClass is not empty, strictly include only origins taken by this class.
        val activeOrigins = if (periodsInClass.isNotEmpty()) {
            allCsvOrigins.filter { origin ->
                periodsInClass.any { p ->
                    p.equals(origin, ignoreCase = true) ||
                    origin.equals(p, ignoreCase = true) ||
                    p.contains(origin, ignoreCase = true) ||
                    origin.contains(p, ignoreCase = true) ||
                    p.split('/', ' ', '・', '_').any { part -> part.isNotBlank() && origin.contains(part, ignoreCase = true) } ||
                    origin.split('/', ' ', '・', '_').any { part -> part.isNotBlank() && p.contains(part, ignoreCase = true) }
                }
            }
        } else {
            allCsvOrigins
        }.sortedWith(Comparator { a, b -> compareNaturalOrder(a, b) })

        // 3. For each active origin, populate all corresponding electives from CSV and sort them
        for (originKey in activeOrigins) {
            val options = electives
                .filter { it.origin.trim().equals(originKey, ignoreCase = true) }
                .map { it.elective.trim() }
                .distinct()
                .filter { it.isNotBlank() }
                .sortedWith(Comparator { a, b -> compareNaturalOrder(a, b) })

            if (options.isNotEmpty()) {
                resultMap[originKey] = options.toMutableList()
            }
        }

        return resultMap
    }

    /**
     * 考査時間割の教室名を解決。
     * - "$hr" の場合はHR教室（{クラス}+組、例: "3組"）と表示
     * - 教室指定があればそのまま表示（空白、"-" 以外）
     * - 考査CSVで空白等の場合は選択科目CSV等から教室を検索
     */
    fun resolveExamClassroom(rawRoom: String, classGroup: ClassGroup, subject: String = ""): String {
        val trimmed = rawRoom.trim()
        val classNum = classGroup.id.replace("組", "").trim().ifBlank {
            classGroup.name.replace("組", "").trim()
        }
        val hrClassroom = if (classNum.isNotBlank()) "${classNum}組" else classGroup.name.ifBlank { "HR教室" }
        val hrKeyword = "\$hr"

        if (trimmed.equals(hrKeyword, ignoreCase = true)) {
            return hrClassroom
        }
        if (trimmed.contains(hrKeyword, ignoreCase = true)) {
            return trimmed.replace(hrKeyword, hrClassroom, ignoreCase = true)
        }
        if (trimmed.isNotBlank() && trimmed != "-") {
            return trimmed
        }

        // 考査CSVの教室が空の場合、選択科目CSVから教室を探す
        val s = subject.trim()
        val electiveMatch = electives.find { it.elective.trim().equals(s, ignoreCase = true) }
            ?: electives.find { isSubjectMatch(it.elective, s) }
        if (electiveMatch != null && electiveMatch.name.isNotBlank()) {
            return electiveMatch.name.trim()
        }

        return resolveClassroom(subject, "", classGroup)
    }

    /**
     * Formats classroom name according to specification:
     * If name is in the form of "n組" (e.g. "1組", "2組", "A組"), appends "教室" -> ("1組教室").
     * Otherwise, displays as-is (e.g. "大講義室", "PC教室", "生物室").
     */
    fun formatClassroomName(rawName: String): String {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return ""
        val isNGumi = (trimmed.matches(Regex("""^[0-9０-９A-Za-z]+組$""")) || trimmed.endsWith("組")) && !trimmed.endsWith("教室")
        return if (isNGumi) {
            "${trimmed}教室"
        } else {
            trimmed
        }
    }

    /**
     * Resolves schedule for a class, date, and period from the parsed CSVs,
     * decoding A/B/C period codes, applying user elective choices,
     * properly processing 講座変更 and 時間割変更届 (including electives within changes),
     * and resolving moving classrooms.
     */
    fun resolvePeriodSchedule(
        classGroup: ClassGroup,
        date: LocalDate,
        period: Int,
        userElectives: Map<String, String> = emptyMap()
    ): PeriodSchedule {
        val month = date.monthValue
        val day = date.dayOfMonth
        val rawClassId = classGroup.id.replace("組", "").trim()

        // 0. Check 考査時間割 (Exam Schedule) - クラスに関係なく適用（全クラス対象）
        val matchingExams = examSchedules.filter { exam ->
            exam.month == month && exam.day == day && exam.period == period &&
            (exam.classId.isBlank() || exam.classId.trim() == "0" || exam.classId.trim() == "全" ||
             exam.classId.trim() == classGroup.id.trim() ||
             exam.classId.replace("組", "").trim() == rawClassId ||
             exam.classId.trim() == classGroup.section.trim())
        }

        if (matchingExams.isNotEmpty()) {
            val userElectiveValues = userElectives.values.filter { it.isNotBlank() }

            // 0.1 Check Science L special case:
            // 考査時間割で、物理L, 化学L, 生物L, 地学Lの4つ（化学L①なども含む）は、複数とっている人がいるので、
            // 複数とっている場合は、物L/化Lのように表示する。
            val userScienceTypes = (userElectives.values + userElectives.keys)
                .filter { it.isNotBlank() }
                .flatMap { it.split('/') }
                .mapNotNull { ScienceLType.fromSubject(it) }
                .distinct()
                .sortedBy { it.sortOrder }

            val periodExamScienceTypes = matchingExams.flatMap { exam ->
                exam.subject.split('/').mapNotNull { ScienceLType.fromSubject(it) }
            }.distinct()

            val isPeriodAllScienceL = matchingExams.any { exam ->
                val s = exam.subject.trim()
                s.equals("理科演習L", ignoreCase = true) || s.contains("理科演習L") || s.startsWith("理科演習")
            }

            if (userScienceTypes.size >= 2 && (isPeriodAllScienceL || periodExamScienceTypes.isNotEmpty())) {
                val testedScienceTypes = userScienceTypes.filter { type ->
                    isPeriodAllScienceL ||
                    periodExamScienceTypes.contains(type) ||
                    matchingExams.any { exam ->
                        isSubjectMatch(type.label, exam.subject) ||
                        isSubjectMatch(type.shortName, exam.subject) ||
                        exam.subject.contains(type.label) ||
                        exam.subject.contains(type.shortName)
                    }
                }.sortedBy { it.sortOrder }

                if (testedScienceTypes.size >= 2) {
                    val combinedSubject = testedScienceTypes.joinToString("/") { it.shortName }
                    val matchedExams = matchingExams.filter { exam ->
                        val examType = ScienceLType.fromSubject(exam.subject)
                        if (examType != null) {
                            testedScienceTypes.contains(examType)
                        } else {
                            val splitTypes = exam.subject.split('/').mapNotNull { ScienceLType.fromSubject(it) }
                            if (splitTypes.isNotEmpty()) {
                                splitTypes.any { testedScienceTypes.contains(it) }
                            } else {
                                val s = exam.subject.trim()
                                s.equals("理科演習L", ignoreCase = true) || s.contains("理科演習L") ||
                                testedScienceTypes.any { type ->
                                    isSubjectMatch(type.label, exam.subject) ||
                                    isSubjectMatch(type.shortName, exam.subject)
                                }
                            }
                        }
                    }
                    val firstExam = matchedExams.firstOrNull() ?: matchingExams.first()
                    val resolvedRooms = matchedExams.map { exam ->
                        resolveExamClassroom(exam.classroom, classGroup, exam.subject)
                    }.filter { it.isNotBlank() && it != "-" }.distinct()

                    val combinedClassroom = if (resolvedRooms.isNotEmpty()) {
                        resolvedRooms.joinToString("/")
                    } else {
                        resolveExamClassroom(firstExam.classroom, classGroup, firstExam.subject)
                    }

                    return createPeriodSchedule(
                        period = period,
                        subject = combinedSubject,
                        classroom = combinedClassroom,
                        isChanged = false,
                        isExam = true,
                        startTime = firstExam.startTime,
                        endTime = firstExam.endTime,
                        isUnselectedElective = false
                    )
                }
            }

            // 1) ユーザーが選択した科目に合致する考査があるかチェック
            // 優先度A: 完全一致 (例: exam.subject == "日特①" かつ userChoice == "日特①")
            var userSelectedExam = matchingExams.find { exam ->
                userElectiveValues.any { userChoice ->
                    userChoice.equals(exam.subject.trim(), ignoreCase = true)
                }
            }

            // 優先度B: マッピング照合 (例: exam.subject == "日本史特講" かつ userChoice == "日特①")
            if (userSelectedExam == null) {
                userSelectedExam = matchingExams.find { exam ->
                    userElectiveValues.any { userChoice ->
                        isSubjectMatch(userChoice, exam.subject)
                    }
                }
            }

            if (userSelectedExam != null) {
                val classroom = resolveExamClassroom(userSelectedExam.classroom, classGroup, userSelectedExam.subject)
                return createPeriodSchedule(
                    period = period,
                    subject = userSelectedExam.subject,
                    classroom = classroom,
                    isChanged = false,
                    isExam = true,
                    startTime = userSelectedExam.startTime,
                    endTime = userSelectedExam.endTime,
                    isUnselectedElective = false
                )
            }

            // 2) 考査の科目が選択科目グループ名（origin: 例「地歴選」「理選」）と一致するかチェック
            for (exam in matchingExams) {
                val originKey = userElectives.keys.find { isSubjectMatch(it, exam.subject) }
                if (originKey != null) {
                    val userChoice = userElectives[originKey]?.trim() ?: ""
                    if (userChoice.isNotBlank()) {
                        val classroom = resolveExamClassroom(exam.classroom, classGroup, userChoice)
                        return createPeriodSchedule(
                            period = period,
                            subject = userChoice,
                            classroom = classroom,
                            isChanged = false,
                            isExam = true,
                            startTime = exam.startTime,
                            endTime = exam.endTime,
                            isUnselectedElective = false
                        )
                    }
                }
            }

            // 3) ユーザーが受講する科目に該当しない場合（未選択、または自分の選択科目でないところ）
            val distinctSubjects = matchingExams.map { it.subject.trim() }.filter { it.isNotBlank() }.distinct()
            val firstSubject = distinctSubjects.firstOrNull() ?: ""
            val firstExam = matchingExams.first()
            val classroom = resolveExamClassroom(firstExam.classroom, classGroup, firstExam.subject)

            // 必修共通科目かどうかを判定（英語W, 現代文, 英語R, 英語長文, 古典, HR など、または $hr 指定）
            val isCommon = distinctSubjects.size == 1 && (isCommonExamSubject(distinctSubjects.first()) || firstExam.classroom.contains("\$hr", ignoreCase = true))

            return createPeriodSchedule(
                period = period,
                subject = firstSubject,
                classroom = classroom,
                isChanged = false,
                isExam = true,
                startTime = firstExam.startTime,
                endTime = firstExam.endTime,
                isUnselectedElective = !isCommon // 自分の選択科目でないところは灰色で表示
            )
        }

        // 1. Initial resolution from Common Schedule or Default Pattern
        val commonRow = commonSchedules.find { it.month == month && it.day == day }
        val periodIndex = period - 1
        val rawCode = commonRow?.periodCodes?.getOrNull(periodIndex)?.trim() ?: ""

        var currentSubject: String
        var currentOrigin: String

        if (rawCode.isNotBlank()) {
            val (decodedSub, decodedOrig) = decodeSubjectCode(rawCode, classGroup.id, period)
            currentSubject = decodedSub
            currentOrigin = decodedOrig
        } else if (commonRow != null) {
            currentSubject = ""
            currentOrigin = ""
        } else {
            val dowJp = getDayOfWeekJp(date.dayOfWeek)
            if (date.dayOfWeek == DayOfWeek.SUNDAY || isHoliday(date)) {
                currentSubject = ""
                currentOrigin = ""
            } else {
                val base = findSubjectInBasicSchedule(classGroup.id, "A", dowJp, period)
                currentSubject = base
                currentOrigin = base
            }
        }

        // Apply elective selection for initial subject
        var resolvedSubject = resolveWithElectives(currentSubject, currentOrigin, userElectives)
        var isChangedByNotification = false

        // 2. Check 講座変更 (Course Change Notifications)
        val matchingCourseChange = courseChanges.find { change ->
            change.month == month && change.day == day && change.period == period &&
            (change.previousCourse.isNotBlank() && (
                resolvedSubject.trim().equals(change.previousCourse.trim(), ignoreCase = true) ||
                currentSubject.trim().equals(change.previousCourse.trim(), ignoreCase = true) ||
                currentOrigin.trim().equals(change.previousCourse.trim(), ignoreCase = true) ||
                resolvedSubject.contains(change.previousCourse.trim(), ignoreCase = true) ||
                currentSubject.contains(change.previousCourse.trim(), ignoreCase = true)
            ))
        }

        if (matchingCourseChange != null) {
            val (newSub, newOrig) = decodeSubjectCode(matchingCourseChange.newCourse.trim(), classGroup.id, period)
            resolvedSubject = resolveWithElectives(newSub, newOrig, userElectives)
            currentOrigin = if (newOrig.isNotBlank()) newOrig else newSub
            isChangedByNotification = true
        }

        // 3. Check 時間割変更届 (Timetable Change Notifications)
        val matchingTimetableChanges = timetableChanges.filter { change ->
            change.month == month && change.day == day && change.period == period &&
            (change.classId.trim() == "0" || change.classId.trim() == "全" ||
             change.classId.trim() == classGroup.id.trim() ||
             change.classId.replace("組", "").trim() == rawClassId ||
             change.classId.trim() == classGroup.section.trim())
        }

        if (matchingTimetableChanges.isNotEmpty()) {
            val specificChange = matchingTimetableChanges.find { it.classId.trim() != "0" && it.classId.trim() != "全" }
                ?: matchingTimetableChanges.last()
            val rawChangeSubject = specificChange.subject.trim()
            val (decodedSub, decodedOrig) = decodeSubjectCode(rawChangeSubject, classGroup.id, period)
            resolvedSubject = resolveWithElectives(decodedSub, decodedOrig, userElectives)
            currentOrigin = if (decodedOrig.isNotBlank()) decodedOrig else decodedSub
            isChangedByNotification = true
        }

        // 4. Resolve classroom from CSV (選択科目 name column)
        val classroom = resolveClassroom(resolvedSubject, currentOrigin, classGroup)

        // 選択科目のコマだが未選択（自分の選択科目が未指定）かどうかを判定
        val isElectiveSlot = electives.any {
            isSubjectMatch(it.origin, currentSubject) || (currentOrigin.isNotBlank() && isSubjectMatch(it.origin, currentOrigin))
        }
        val isUserChosen = userElectives.values.any { isSubjectMatch(it, resolvedSubject) }
        val isUnselectedElective = isElectiveSlot && !isUserChosen

        return createPeriodSchedule(
            period = period,
            subject = resolvedSubject,
            classroom = classroom,
            isChanged = isChangedByNotification,
            isExam = false,
            isUnselectedElective = isUnselectedElective
        )
    }

    /**
     * 考査において全員共通（必修）の科目かどうかを判定。
     * 英語W, 現代文, 英語R, 英語長文, 古典, HR などの共通科目は true。
     * 選択科目またはマッピング対象の科目は false。
     */
    fun isCommonExamSubject(subjectName: String): Boolean {
        val s = subjectName.trim()
        if (s.isBlank()) return false
        val commonExamSubjects = listOf(
            "英語W", "英語Wt", "現代文", "英語R", "英語長文", "英語長", "古典", "HR", "LHR", "ホームルーム"
        )
        if (commonExamSubjects.any { it.equals(s, ignoreCase = true) }) return true

        val isElectiveOrMapped = electives.any { isSubjectMatch(it.elective, s) } ||
                subjectMappings.any { isSubjectMatch(it.source, s) || isSubjectMatch(it.target, s) } ||
                s == "現世読" ||
                ScienceLType.fromSubject(s) != null ||
                s.contains("理科演習L") ||
                s.contains("物L") || s.contains("化L") || s.contains("生L") || s.contains("地L")
        return !isElectiveOrMapped
    }

    private fun decodeSubjectCode(code: String, classId: String, currentPeriod: Int): Pair<String, String> {
        val trimmed = code.trim()
        if (trimmed.isBlank()) return Pair("", "")

        // Check if code matches pattern e.g. "A月1", "B火3", "C土2"
        val patternRegex = Regex("""([A-Za-z])(月|火|水|木|金|土|日)(\d)""")
        val match = patternRegex.find(trimmed)
        if (match != null) {
            val type = match.groupValues[1].uppercase()
            val dow = match.groupValues[2]
            val targetPeriod = match.groupValues[3].toIntOrNull() ?: currentPeriod
            val found = findSubjectInBasicSchedule(classId, type, dow, targetPeriod)
            return Pair(found, found)
        }
        return Pair(trimmed, trimmed)
    }

    private fun resolveWithElectives(
        subject: String,
        origin: String,
        userElectives: Map<String, String>
    ): String {
        val s = subject.trim()
        val o = origin.trim()
        if (s.isBlank() && o.isBlank()) return ""

        // 1. Direct match in userElectives with subject
        if (userElectives.containsKey(s)) {
            val choice = userElectives[s]
            if (!choice.isNullOrBlank()) return choice
        }

        // 2. Direct match in userElectives with origin
        if (o.isNotBlank() && userElectives.containsKey(o)) {
            val choice = userElectives[o]
            if (!choice.isNullOrBlank()) return choice
        }

        // 3. Subject-match (exact or stripped numbering suffix) in userElectives keys
        for ((key, value) in userElectives) {
            if (value.isBlank()) continue
            if (isSubjectMatch(key, s) || (o.isNotBlank() && isSubjectMatch(key, o))) {
                return value
            }
        }

        // 4. Check if subject or origin matches an origin in electives CSV
        // If user has chosen one of the electives for this origin, return it.
        // If user has NOT chosen any elective, DO NOT arbitrarily fall back to the first elective;
        // return the original subject/code instead.
        val matchingElectives = electives.filter {
            isSubjectMatch(it.origin, s) || (o.isNotBlank() && isSubjectMatch(it.origin, o))
        }
        if (matchingElectives.isNotEmpty()) {
            val userSelected = matchingElectives.find { el ->
                userElectives.values.any { isSubjectMatch(it, el.elective) }
            }
            if (userSelected != null) {
                return userSelected.elective.trim()
            }
            // ユーザーが未選択の場合は勝手にフォールバックせず元のコード (s または o) をそのまま表示
            return if (s.isNotBlank()) s else o
        }

        return if (s.isNotBlank()) s else o
    }

    private fun isCommonSubject(subjectName: String, originName: String): Boolean {
        val targets = listOf(
            "現代文", "現読", "古典", "古講", "英語R", "英語長", "長",
            "英語W", "英語Wt", "W", "Wt", "W/Wt", "英語W/Wt", "HR", "LHR", "ホームルーム",
            "IbA", "iV", "ⅠbA", "ⅠV", "IV", "Ib", "IA", "IIb", "Ⅱb"
        )
        val s = subjectName.trim()
        val o = originName.trim()
        if (s.isBlank() && o.isBlank()) return false

        return targets.any { target ->
            s.equals(target, ignoreCase = true) ||
            o.equals(target, ignoreCase = true) ||
            s.startsWith(target, ignoreCase = true) ||
            o.startsWith(target, ignoreCase = true) ||
            (target.length >= 2 && (s.contains(target, ignoreCase = true) || o.contains(target, ignoreCase = true)))
        }
    }

    private fun resolveClassroom(
        subjectName: String,
        originName: String,
        classGroup: ClassGroup
    ): String {
        val s = subjectName.trim()
        if (s.isBlank()) return ""

        if (s.equals("\$hr", ignoreCase = true) || originName.trim().equals("\$hr", ignoreCase = true)) {
            val classNum = classGroup.id.replace("組", "").trim().ifBlank {
                classGroup.name.replace("組", "").trim()
            }
            return if (classNum.isNotBlank()) "${classNum}組" else classGroup.name.ifBlank { "HR教室" }
        }

        val matching = electives.find {
            it.elective.trim().equals(s, ignoreCase = true)
        } ?: electives.find {
            originName.isNotBlank() && it.origin.trim().equals(originName.trim(), ignoreCase = true) &&
            it.elective.trim().contains(s, ignoreCase = true)
        }

        if (matching != null && matching.name.isNotBlank()) {
            return formatClassroomName(matching.name)
        }

        // 共通科目のフォールバック（O組教室）
        if (isCommonSubject(s, originName)) {
            val ownClassroom = if (classGroup.name.isNotBlank()) {
                "${classGroup.name}教室"
            } else if (classGroup.id.isNotBlank()) {
                "${classGroup.id}組教室"
            } else {
                ""
            }
            if (ownClassroom.isNotBlank()) {
                return ownClassroom
            }
        }

        return "-"
    }

    /**
     * CSVおよび基本時間割から利用可能な全科目名の一覧を取得
     */
    fun getAllKnownSubjects(): List<String> {
        val set = linkedSetOf<String>()
        // 1. 基本時間割に登場する科目
        basicClassSchedule.forEach { row ->
            row.periods.forEach { p ->
                val clean = p.trim()
                if (clean.isNotBlank() && !clean.matches(Regex("""[A-Za-z][月火水木金土日]\d"""))) {
                    set.add(clean)
                }
            }
        }
        // 2. 選択科目CSVに登場する科目
        electives.forEach { el ->
            if (el.origin.isNotBlank()) set.add(el.origin.trim())
            if (el.elective.isNotBlank()) set.add(el.elective.trim())
        }
        // 3. 時間割変更届・講座変更CSVに登場する科目
        timetableChanges.forEach { if (it.subject.isNotBlank()) set.add(it.subject.trim()) }
        courseChanges.forEach {
            if (it.previousCourse.isNotBlank()) set.add(it.previousCourse.trim())
            if (it.newCourse.isNotBlank()) set.add(it.newCourse.trim())
        }
        // 4. 標準教科パレット
        SubjectColorDefaults.defaultSubjectColors.keys.forEach {
            set.add(it)
        }
        return set.toList()
    }

    private fun findSubjectInBasicSchedule(classId: String, type: String, dayOfWeekJp: String, period: Int): String {
        val rawId = classId.replace("組", "").trim()
        val row = basicClassSchedule.find {
            val rId = it.classId.trim().replace("組", "")
            (rId == rawId || rId == classId) && it.type.equals(type, ignoreCase = true) && it.dayOfWeek == dayOfWeekJp
        } ?: basicClassSchedule.find {
            it.classId == "1" && it.type.equals(type, ignoreCase = true) && it.dayOfWeek == dayOfWeekJp
        }

        val periodIdx = (period - 1).coerceIn(0, 4)
        return row?.periods?.getOrNull(periodIdx)?.trim() ?: ""
    }

    private fun getDayOfWeekJp(dow: DayOfWeek): String {
        return when (dow) {
            DayOfWeek.MONDAY -> "月"
            DayOfWeek.TUESDAY -> "火"
            DayOfWeek.WEDNESDAY -> "水"
            DayOfWeek.THURSDAY -> "木"
            DayOfWeek.FRIDAY -> "金"
            DayOfWeek.SATURDAY -> "土"
            DayOfWeek.SUNDAY -> "日"
        }
    }

    fun isHoliday(date: LocalDate): Boolean {
        return holidays.any { it.month == date.monthValue && it.day == date.dayOfMonth && it.holiday.isNotBlank() }
    }

    fun getHolidayName(date: LocalDate): String? {
        return holidays.find { it.month == date.monthValue && it.day == date.dayOfMonth && it.holiday.isNotBlank() }?.holiday
    }

    fun getEventForDate(date: LocalDate): String {
        val eventList = mutableListOf<String>()
        val holidayName = getHolidayName(date)
        if (holidayName != null) {
            eventList.add("$holidayName (祝日)")
        }

        val matchingEvents = events.filter { it.month == date.monthValue && it.day == date.dayOfMonth && it.event.isNotBlank() }
        matchingEvents.forEach { eventList.add(it.event) }

        val combined = eventList.joinToString(" / ")
        return formatEventText(combined)
    }

    /**
     * Creates PeriodSchedule with clean theme colors and actual CSV classroom (no mock data).
     */
    private fun createPeriodSchedule(
        period: Int,
        subject: String,
        classroom: String,
        isChanged: Boolean = false,
        isExam: Boolean = false,
        startTime: String = "",
        endTime: String = "",
        isUnselectedElective: Boolean = false
    ): PeriodSchedule {
        val sub = subject.trim()
        if (sub.isBlank()) {
            return PeriodSchedule(
                period = period,
                subject = "",
                classroom = "",
                teacher = "",
                memo = "",
                colorHex = 0xFF5C6BC0,
                isChanged = isChanged,
                isExam = isExam,
                startTime = startTime,
                endTime = endTime,
                isUnselectedElective = isUnselectedElective
            )
        }

        val colorHex = 0xFF5C6BC0

        return PeriodSchedule(
            period = period,
            subject = sub,
            classroom = classroom,
            teacher = "",
            memo = "",
            colorHex = colorHex,
            isChanged = isChanged,
            isExam = isExam,
            startTime = startTime,
            endTime = endTime,
            isUnselectedElective = isUnselectedElective
        )
    }

    @androidx.annotation.VisibleForTesting
    fun setElectivesForTesting(list: List<ElectiveItemRow>) {
        this.electives = list
    }

    @androidx.annotation.VisibleForTesting
    fun setExamScheduleForTesting(list: List<ExamScheduleRow>) {
        this.examSchedules = list
    }
}
