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
}
