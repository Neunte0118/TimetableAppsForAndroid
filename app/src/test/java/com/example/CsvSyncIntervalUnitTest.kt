package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.csv.CsvSyncManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CsvSyncIntervalUnitTest {

    private lateinit var context: Context
    private lateinit var syncManager: CsvSyncManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        syncManager = CsvSyncManager(context)
        syncManager.clearAllCache()
    }

    @Test
    fun testWeeklyIntervalCriteria() {
        val prefs = context.getSharedPreferences("csv_sync_prefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        // キャッシュがない状態では必ず取得
        assertTrue(syncManager.shouldFetchCsv("csv_cache_holidays", CsvSyncManager.KEY_TS_HOLIDAYS, CsvSyncManager.INTERVAL_WEEK_MILLIS, false))

        // キャッシュと現在時刻タイムスタンプを保存
        prefs.edit()
            .putString("csv_cache_holidays", "dates,holidays")
            .putLong(CsvSyncManager.KEY_TS_HOLIDAYS, now)
            .apply()

        // 取得直後なのでスキップされる
        assertFalse(syncManager.shouldFetchCsv("csv_cache_holidays", CsvSyncManager.KEY_TS_HOLIDAYS, CsvSyncManager.INTERVAL_WEEK_MILLIS, false))

        // 強制フラグ指定時は必ず取得
        assertTrue(syncManager.shouldFetchCsv("csv_cache_holidays", CsvSyncManager.KEY_TS_HOLIDAYS, CsvSyncManager.INTERVAL_WEEK_MILLIS, true))

        // 3日経過時点では1週間未満なのでスキップ
        val threeDaysAgo = now - 3L * 24 * 60 * 60 * 1000
        prefs.edit().putLong(CsvSyncManager.KEY_TS_HOLIDAYS, threeDaysAgo).apply()
        assertFalse(syncManager.shouldFetchCsv("csv_cache_holidays", CsvSyncManager.KEY_TS_HOLIDAYS, CsvSyncManager.INTERVAL_WEEK_MILLIS, false))

        // 7日経過時点では取得対象
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
        prefs.edit().putLong(CsvSyncManager.KEY_TS_HOLIDAYS, sevenDaysAgo).apply()
        assertTrue(syncManager.shouldFetchCsv("csv_cache_holidays", CsvSyncManager.KEY_TS_HOLIDAYS, CsvSyncManager.INTERVAL_WEEK_MILLIS, false))
    }

    @Test
    fun testDailyIntervalCriteria() {
        val prefs = context.getSharedPreferences("csv_sync_prefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        // 共通予定のキャッシュと現在時刻タイムスタンプを保存
        prefs.edit()
            .putString("csv_cache_common_schedule", "dates,first_period")
            .putLong(CsvSyncManager.KEY_TS_COMMON_SCHEDULE, now)
            .apply()

        // 直後はスキップ
        assertFalse(syncManager.shouldFetchCsv("csv_cache_common_schedule", CsvSyncManager.KEY_TS_COMMON_SCHEDULE, CsvSyncManager.INTERVAL_DAY_MILLIS, false))

        // 12時間経過時点では1日未満なのでスキップ
        val halfDayAgo = now - 12L * 60 * 60 * 1000
        prefs.edit().putLong(CsvSyncManager.KEY_TS_COMMON_SCHEDULE, halfDayAgo).apply()
        assertFalse(syncManager.shouldFetchCsv("csv_cache_common_schedule", CsvSyncManager.KEY_TS_COMMON_SCHEDULE, CsvSyncManager.INTERVAL_DAY_MILLIS, false))

        // 25時間経過時点では1日経過しているので取得対象
        val overOneDayAgo = now - 25L * 60 * 60 * 1000
        prefs.edit().putLong(CsvSyncManager.KEY_TS_COMMON_SCHEDULE, overOneDayAgo).apply()
        assertTrue(syncManager.shouldFetchCsv("csv_cache_common_schedule", CsvSyncManager.KEY_TS_COMMON_SCHEDULE, CsvSyncManager.INTERVAL_DAY_MILLIS, false))
    }

    @Test
    fun testTimetableChangesDailyInterval() {
        val prefs = context.getSharedPreferences("csv_sync_prefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        // キャッシュがない初期状態では必ず取得
        assertTrue(syncManager.shouldFetchCsv("csv_cache_timetable_changes", CsvSyncManager.KEY_TS_TIMETABLE_CHANGES, CsvSyncManager.INTERVAL_DAY_MILLIS, false))

        // キャッシュと現在時刻タイムスタンプを保存
        prefs.edit()
            .putString("csv_cache_timetable_changes", "dates,classes,periods,subjects")
            .putLong(CsvSyncManager.KEY_TS_TIMETABLE_CHANGES, now)
            .apply()

        // 取得直後（1日未満）はスキップ
        assertFalse(syncManager.shouldFetchCsv("csv_cache_timetable_changes", CsvSyncManager.KEY_TS_TIMETABLE_CHANGES, CsvSyncManager.INTERVAL_DAY_MILLIS, false))

        // 強制フラグ指定時（更新ボタン押下時）は必ず取得
        assertTrue(syncManager.shouldFetchCsv("csv_cache_timetable_changes", CsvSyncManager.KEY_TS_TIMETABLE_CHANGES, CsvSyncManager.INTERVAL_DAY_MILLIS, true))

        // 12時間経過時点では1日未満なので自動取得はスキップ
        val halfDayAgo = now - 12L * 60 * 60 * 1000
        prefs.edit().putLong(CsvSyncManager.KEY_TS_TIMETABLE_CHANGES, halfDayAgo).apply()
        assertFalse(syncManager.shouldFetchCsv("csv_cache_timetable_changes", CsvSyncManager.KEY_TS_TIMETABLE_CHANGES, CsvSyncManager.INTERVAL_DAY_MILLIS, false))

        // 25時間経過時点（翌日）では1日経過しているので自動取得対象
        val overOneDayAgo = now - 25L * 60 * 60 * 1000
        prefs.edit().putLong(CsvSyncManager.KEY_TS_TIMETABLE_CHANGES, overOneDayAgo).apply()
        assertTrue(syncManager.shouldFetchCsv("csv_cache_timetable_changes", CsvSyncManager.KEY_TS_TIMETABLE_CHANGES, CsvSyncManager.INTERVAL_DAY_MILLIS, false))
    }

    @Test
    fun testForceSyncFetchesAllCsvs() {
        val prefs = context.getSharedPreferences("csv_sync_prefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        val allCsvKeys = listOf(
            "csv_cache_holidays" to (CsvSyncManager.KEY_TS_HOLIDAYS to CsvSyncManager.INTERVAL_WEEK_MILLIS),
            "csv_cache_basic_class" to (CsvSyncManager.KEY_TS_BASIC_CLASS to CsvSyncManager.INTERVAL_WEEK_MILLIS),
            "csv_cache_second_term_class" to (CsvSyncManager.KEY_TS_SECOND_TERM_CLASS to CsvSyncManager.INTERVAL_WEEK_MILLIS),
            "csv_cache_subject_mappings" to (CsvSyncManager.KEY_TS_SUBJECT_MAPPINGS to CsvSyncManager.INTERVAL_WEEK_MILLIS),
            "csv_cache_common_schedule" to (CsvSyncManager.KEY_TS_COMMON_SCHEDULE to CsvSyncManager.INTERVAL_DAY_MILLIS),
            "csv_cache_electives" to (CsvSyncManager.KEY_TS_ELECTIVES to CsvSyncManager.INTERVAL_DAY_MILLIS),
            "csv_cache_updates" to (CsvSyncManager.KEY_TS_UPDATES to CsvSyncManager.INTERVAL_DAY_MILLIS),
            "csv_cache_events" to (CsvSyncManager.KEY_TS_EVENTS to CsvSyncManager.INTERVAL_DAY_MILLIS),
            "csv_cache_timetable_changes" to (CsvSyncManager.KEY_TS_TIMETABLE_CHANGES to CsvSyncManager.INTERVAL_DAY_MILLIS),
            "csv_cache_course_changes" to (CsvSyncManager.KEY_TS_COURSE_CHANGES to CsvSyncManager.INTERVAL_DAY_MILLIS),
            "csv_cache_exam_schedule" to (CsvSyncManager.KEY_TS_EXAM_SCHEDULE to CsvSyncManager.INTERVAL_DAY_MILLIS),
            "csv_cache_special_schedule" to (CsvSyncManager.KEY_TS_SPECIAL_SCHEDULE to CsvSyncManager.INTERVAL_DAY_MILLIS)
        )

        val editor = prefs.edit()
        for ((cacheKey, pair) in allCsvKeys) {
            editor.putString(cacheKey, "dummy_data")
            editor.putLong(pair.first, now)
        }
        editor.apply()

        // 通常判定（force = false）では、直後なので全12種類ともスキップされる
        for ((cacheKey, pair) in allCsvKeys) {
            val shouldFetchNormal = syncManager.shouldFetchCsv(cacheKey, pair.first, pair.second, force = false)
            assertFalse("Expected $cacheKey to be skipped on normal sync within interval", shouldFetchNormal)
        }

        // 更新ボタン押下時（force = true）では、全12種類とも必ず再取得対象となる
        for ((cacheKey, pair) in allCsvKeys) {
            val shouldFetchForced = syncManager.shouldFetchCsv(cacheKey, pair.first, pair.second, force = true)
            assertTrue("Expected $cacheKey to be re-fetched when force = true", shouldFetchForced)
        }
    }

    @Test
    fun testTimetableChangeUrlMigration() {
        val prefs = context.getSharedPreferences("csv_sync_prefs", Context.MODE_PRIVATE)
        // 過去の古い時間割変更シートURL（gid=1149682638）が保存されている状態をシミュレート
        prefs.edit()
            .putString("url_timetable_change_sheet", "https://docs.google.com/spreadsheets/d/e/2PACX-1vQiStJCsPKp1ndi958BLOajBqizE_aIcO2Z0f9hPgiyPV19rnWB3qVcrLuVEaeCeE5ddaIudtX7VkzE/pub?gid=1149682638&single=true&output=csv")
            .putString("csv_cache_timetable_changes", "old_cached_data")
            .apply()

        // 新しくインスタンス化するとマイグレーションが走り、新デフォルト（gid=963151426）に自動更新され旧キャッシュが消去される
        val newManager = CsvSyncManager(context)
        val config = newManager.urlsConfig.value
        assertTrue("Expected new timetable change URL with gid=963151426", config.timetableChangeSheetUrl.contains("gid=963151426"))
        assertFalse("Legacy cache should be removed", prefs.contains("csv_cache_timetable_changes"))
    }

    @Test
    fun testTimetableChangeAppliedCorrectly() {
        val testCsv = """
            dates,classes,periods,subjects
            10月29日,5,2,英語W
            10月29日,5,4,E2
            10月30日,2,1,英語R
            11月13日,5,4,現代文
            7月1日,1,6,A1
        """.trimIndent()

        val parsed = com.example.data.csv.CsvParser.parseTimetableChanges(testCsv)
        org.junit.Assert.assertEquals(5, parsed.size)

        val prefs = context.getSharedPreferences("csv_sync_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("csv_cache_timetable_changes", testCsv).apply()
        syncManager.loadAndParseLocalData()

        val classGroup5 = com.example.model.ClassGroup(id = "5", name = "5組", grade = 1, section = "5")
        val dateOct29 = java.time.LocalDate.of(2026, 10, 29)

        val period2 = syncManager.resolvePeriodSchedule(classGroup5, dateOct29, 2)
        assertTrue(period2.isChanged)
        org.junit.Assert.assertEquals("英語W", period2.subject)

        val period4 = syncManager.resolvePeriodSchedule(classGroup5, dateOct29, 4)
        assertTrue(period4.isChanged)
        org.junit.Assert.assertEquals("E2", period4.subject)

        // 7月1日 1組の6限（最大時限数が6限になること）
        val dateJuly1 = java.time.LocalDate.of(2026, 7, 1)
        org.junit.Assert.assertEquals(6, syncManager.getMaxPeriodsForDate(dateJuly1))
        val classGroup1 = com.example.model.ClassGroup(id = "1", name = "1組", grade = 1, section = "1")
        val period6 = syncManager.resolvePeriodSchedule(classGroup1, dateJuly1, 6)
        assertTrue(period6.isChanged)
        org.junit.Assert.assertEquals("A1", period6.subject)
    }
}
