package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TimetableRepository
import com.example.data.csv.CsvSyncManager
import com.example.data.csv.CsvUrlsConfig
import com.example.model.DataFetchMode
import com.example.sync.BackgroundSyncScheduler
import com.example.ui.TimetableViewModel
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WifiAndBatteryOptimizationUnitTest {

    private lateinit var app: Application
    private lateinit var server: HttpServer
    private lateinit var baseUrl: String
    private val hitCounts = ConcurrentHashMap<String, AtomicInteger>()

    private val allEndpoints = listOf(
        "/basic_class.csv",
        "/second_term_class.csv",
        "/electives.csv",
        "/holidays.csv",
        "/subject_mapping.csv",
        "/common_schedule.csv",
        "/updates.csv",
        "/events.csv",
        "/additional_events.csv",
        "/timetable_changes.csv",
        "/course_changes.csv",
        "/exam_schedule.csv",
        "/special_schedule.csv"
    )

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("csv_sync_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        app.getSharedPreferences("timetable_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        app.getSharedPreferences("timetable_settings", Context.MODE_PRIVATE).edit().clear().apply()

        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        for (path in allEndpoints) {
            hitCounts[path] = AtomicInteger(0)
            server.createContext(path) { exchange ->
                hitCounts[path]?.incrementAndGet()
                val bytes = "".toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
        }
        server.start()
        baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    private fun testUrlsConfig(): CsvUrlsConfig = CsvUrlsConfig(
        commonScheduleUrl = "$baseUrl/common_schedule.csv",
        basicClassUrl = "$baseUrl/basic_class.csv",
        electivesUrl = "$baseUrl/electives.csv",
        updatesUrl = "$baseUrl/updates.csv",
        eventsUrl = "$baseUrl/events.csv",
        additionalEventsUrl = "$baseUrl/additional_events.csv",
        holidaysUrl = "$baseUrl/holidays.csv",
        timetableChangeSheetUrl = "$baseUrl/timetable_changes.csv",
        courseChangeSheetUrl = "$baseUrl/course_changes.csv",
        examScheduleUrl = "$baseUrl/exam_schedule.csv",
        subjectMappingUrl = "$baseUrl/subject_mapping.csv",
        specialScheduleUrl = "$baseUrl/special_schedule.csv",
        secondTermClassUrl = "$baseUrl/second_term_class.csv"
    )

    private fun resetHitCounts() {
        hitCounts.values.forEach { it.set(0) }
    }

    @Test
    fun syncInterval_is60Minutes() {
        assertEquals(60L, BackgroundSyncScheduler.SYNC_INTERVAL_MINUTES)
        assertEquals(60L * 60 * 1000, CsvSyncManager.INTERVAL_60_MINUTES_MILLIS)
    }

    @Test
    fun defaultDataFetchMode_isIncludeMobile() {
        val manager = CsvSyncManager(app)
        assertEquals(DataFetchMode.INCLUDE_MOBILE, manager.dataFetchMode.value)
    }

    @Test
    fun includeMobileMode_withoutWifi_fetchesOnlyMobileAllowedCsvs() = runBlocking {
        val manager = CsvSyncManager(app)
        manager.saveUrlsConfig(testUrlsConfig())
        manager.setDataFetchMode(DataFetchMode.INCLUDE_MOBILE)
        manager.wifiStateOverrideForTesting = false

        val result = manager.syncAllCsvs(force = true)
        assertTrue(result.isSuccess)

        val wifiOnlyPaths = listOf(
            "/basic_class.csv",
            "/second_term_class.csv",
            "/electives.csv",
            "/holidays.csv",
            "/subject_mapping.csv"
        )
        val mobileAllowedPaths = listOf(
            "/common_schedule.csv",
            "/updates.csv",
            "/events.csv",
            "/additional_events.csv",
            "/timetable_changes.csv",
            "/course_changes.csv",
            "/exam_schedule.csv",
            "/special_schedule.csv"
        )

        for (p in wifiOnlyPaths) {
            assertEquals("Wi-Fi only CSV $p should NOT be fetched on mobile data", 0, hitCounts[p]?.get())
        }
        for (p in mobileAllowedPaths) {
            assertEquals("Mobile-allowed CSV $p SHOULD be fetched on mobile data", 1, hitCounts[p]?.get())
        }
    }

    @Test
    fun includeMobileMode_withWifi_fetchesAll13Csvs() = runBlocking {
        val manager = CsvSyncManager(app)
        manager.saveUrlsConfig(testUrlsConfig())
        manager.setDataFetchMode(DataFetchMode.INCLUDE_MOBILE)
        manager.wifiStateOverrideForTesting = true

        val result = manager.syncAllCsvs(force = true)
        assertTrue(result.isSuccess)

        for (p in allEndpoints) {
            assertEquals("CSV $p should be fetched on Wi-Fi", 1, hitCounts[p]?.get())
        }
    }

    @Test
    fun wifiOnlyMode_withoutWifi_skipsAll13Csvs_andWithWifi_fetchesAll13Csvs() = runBlocking {
        val manager = CsvSyncManager(app)
        manager.saveUrlsConfig(testUrlsConfig())
        manager.setDataFetchMode(DataFetchMode.WIFI_ONLY)

        manager.wifiStateOverrideForTesting = false
        val mobileResult = manager.syncAllCsvs(force = true)
        assertTrue(mobileResult.isSuccess)
        for (p in allEndpoints) {
            assertEquals("CSV $p should NOT be fetched when WIFI_ONLY and not on Wi-Fi", 0, hitCounts[p]?.get())
        }

        resetHitCounts()
        manager.wifiStateOverrideForTesting = true
        val wifiResult = manager.syncAllCsvs(force = true)
        assertTrue(wifiResult.isSuccess)
        for (p in allEndpoints) {
            assertEquals("CSV $p should be fetched when WIFI_ONLY and on Wi-Fi", 1, hitCounts[p]?.get())
        }
    }

    @Test
    fun batteryOptimizationModal_showsOnStartupAfterSetup_andRespectsDontShowAgain() {
        val repo = TimetableRepository.getInstance(app)
        repo.setCompletedInitialSetup(true)
        repo.setBatteryOptimizationPromptDismissed(false)
        repo.setDataFetchMode(DataFetchMode.INCLUDE_MOBILE)

        val vm1 = TimetableViewModel(app)
        assertTrue(vm1.uiState.value.showBatteryOptimizationDialog)
        assertEquals(DataFetchMode.INCLUDE_MOBILE, vm1.uiState.value.dataFetchMode)

        vm1.setDataFetchMode(DataFetchMode.WIFI_ONLY)
        assertEquals(DataFetchMode.WIFI_ONLY, vm1.uiState.value.dataFetchMode)
        assertEquals(DataFetchMode.WIFI_ONLY, repo.dataFetchMode.value)

        // Dismiss with dontShowAgain = true
        vm1.dismissBatteryOptimizationDialog(dontShowAgain = true)
        assertFalse(vm1.uiState.value.showBatteryOptimizationDialog)
        assertTrue(repo.isBatteryOptimizationPromptDismissed())

        // Subsequent launch must not show the dialog automatically
        val vm2 = TimetableViewModel(app)
        assertFalse(vm2.uiState.value.showBatteryOptimizationDialog)
        assertEquals(DataFetchMode.WIFI_ONLY, vm2.uiState.value.dataFetchMode)
    }
}
