package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TimetableRepository
import com.example.data.csv.CsvSyncManager
import com.example.data.csv.CsvUrlsConfig
import com.example.model.DefaultSchoolData
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImmediateLaunchSyncUnitTest {

    private lateinit var context: Context
    private lateinit var server: HttpServer
    private lateinit var baseUrl: String

    private val timetableChangeHits = AtomicInteger(0)
    private val updatesHits = AtomicInteger(0)

    @Volatile
    private var servedTimetableChangesCsv = """
        dates,classes,periods,subjects
        10月29日,1,1,英語R
    """.trimIndent()

    @Volatile
    private var servedUpdatesCsv = """
        ver,info,link
        1.5.0,・初期リリース,
    """.trimIndent()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/timetable_changes.csv") { exchange ->
            timetableChangeHits.incrementAndGet()
            val bytes = servedTimetableChangesCsv.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.createContext("/updates.csv") { exchange ->
            updatesHits.incrementAndGet()
            val bytes = servedUpdatesCsv.toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.createContext("/empty.csv") { exchange ->
            val bytes = "".toByteArray(Charsets.UTF_8)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        baseUrl = "http://127.0.0.1:${server.address.port}"
    }

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun syncCsvData_withDefaultArgs_alwaysFetchesLatestTimetableAndUpdatesDespiteRecentTimestamps() = runBlocking {
        val repo = TimetableRepository(context)
        repo.clearAllCsvCache()

        val emptyUrl = "$baseUrl/empty.csv"
        repo.saveCsvUrls(
            CsvUrlsConfig(
                commonScheduleUrl = emptyUrl,
                basicClassUrl = emptyUrl,
                electivesUrl = emptyUrl,
                updatesUrl = "$baseUrl/updates.csv",
                eventsUrl = emptyUrl,
                additionalEventsUrl = emptyUrl,
                holidaysUrl = emptyUrl,
                timetableChangeSheetUrl = "$baseUrl/timetable_changes.csv",
                courseChangeSheetUrl = emptyUrl,
                examScheduleUrl = emptyUrl,
                subjectMappingUrl = emptyUrl,
                specialScheduleUrl = emptyUrl,
                secondTermClassUrl = emptyUrl
            )
        )

        val now = System.currentTimeMillis()
        val syncPrefs = context.getSharedPreferences("csv_sync_prefs", Context.MODE_PRIVATE)
        syncPrefs.edit()
            .putString("csv_cache_timetable_changes", servedTimetableChangesCsv)
            .putLong(CsvSyncManager.KEY_TS_TIMETABLE_CHANGES, now)
            .putString("csv_cache_updates", servedUpdatesCsv)
            .putLong(CsvSyncManager.KEY_TS_UPDATES, now)
            .apply()
        repo.csvSyncManager.loadAndParseLocalData()

        val cls1 = DefaultSchoolData.classes.first()
        val targetDate = LocalDate.of(2026, 10, 29)

        // Warm dayScheduleCache with the old data
        val beforeSchedule = repo.getDaySchedule(cls1, targetDate, targetDate)
        assertEquals("英語R", beforeSchedule.periods.first { it.period == 1 }.subject)
        assertEquals("1.5.0", repo.getLatestAppUpdateInfo()?.version)

        // Mutate server payloads to simulate fresh timetable change & app update published right before launch
        servedTimetableChangesCsv = """
            dates,classes,periods,subjects
            10月29日,1,1,物理演習
        """.trimIndent()
        servedUpdatesCsv = """
            ver,info,link
            9.9.9,・緊急の時間割変更反映対応,
        """.trimIndent()

        // Default syncCsvData() (used at launch) must bypass 1-day/1-week intervals and clear dayScheduleCache
        val result = repo.syncCsvData()
        assertTrue(result.isSuccess)
        assertEquals(1, timetableChangeHits.get())
        assertEquals(1, updatesHits.get())

        val afterSchedule = repo.getDaySchedule(cls1, targetDate, targetDate)
        assertEquals("物理演習", afterSchedule.periods.first { it.period == 1 }.subject)

        val latestUpdate = repo.getLatestAppUpdateInfo()
        assertNotNull(latestUpdate)
        assertEquals("9.9.9", latestUpdate?.version)
        assertEquals("9.9.9", repo.getUpdateHistories().firstOrNull()?.version)

        // Subsequent refreshUpdateInfo() (manual check / history open) must fetch again immediately
        servedUpdatesCsv = """
            ver,info,link
            9.9.10,・さらなる更新,
        """.trimIndent()
        val updateRefresh = repo.refreshUpdateInfo()
        assertTrue(updateRefresh.isSuccess)
        assertEquals(2, updatesHits.get())
        assertEquals("9.9.10", repo.getLatestAppUpdateInfo()?.version)
    }
}
