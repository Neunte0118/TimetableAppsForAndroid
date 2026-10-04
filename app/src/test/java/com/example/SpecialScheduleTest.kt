package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.csv.CsvParser
import com.example.data.csv.CsvSyncManager
import com.example.data.csv.CsvUrlsConfig
import com.example.model.ClassGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SpecialScheduleTest {

    @Test
    fun testParseSpecialSchedule() {
        // date, period, subject, start_time, end_time
        val csv = """
            date, period, subject, start_time, end_time
            10/20, 1, A火1, 8:40, 9:25
            10/20, 2, A火2, 9:35, 10:20
            10/20, 3, A火3, 10:30, 11:15
            10/20, 4, A火4, 11:25, 12:10
            10/20, 5, A火5, 12:55, 13:40
            10/20, 6, A火6, 13:50, 14:35
        """.trimIndent()

        val parsed = CsvParser.parseSpecialSchedule(csv)
        assertEquals(6, parsed.size)

        val first = parsed[0]
        assertEquals(10, first.month)
        assertEquals(20, first.day)
        assertEquals(1, first.period)
        assertEquals("A火1", first.subject)
        assertEquals("8:40", first.startTime)
        assertEquals("9:25", first.endTime)

        val sixth = parsed[5]
        assertEquals(6, sixth.period)
        assertEquals("A火6", sixth.subject)
        assertEquals("13:50", sixth.startTime)
        assertEquals("14:35", sixth.endTime)
    }

    @Test
    fun testSpecialSchedule_DecodesSubjectCodeAndSetsStartEndTime() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = CsvSyncManager(context)
        val classGroup = ClassGroup(id = "3", name = "3組", grade = 3, section = "3")

        val specialCsv = """
            date, period, subject, start_time, end_time
            11/5, 1, A火1, 8:30, 9:15
            11/5, 6, A金5, 13:45, 14:30
        """.trimIndent()
        manager.setSpecialScheduleForTesting(CsvParser.parseSpecialSchedule(specialCsv))

        val date = LocalDate.of(2026, 11, 5)

        // Period 1
        val p1 = manager.resolvePeriodSchedule(classGroup, date, 1, emptyMap())
        assertEquals("8:30", p1.startTime)
        assertEquals("9:15", p1.endTime)
        assertFalse("Special schedule is not exam", p1.isExam)

        // Period 6
        val p6 = manager.resolvePeriodSchedule(classGroup, date, 6, emptyMap())
        assertEquals("13:45", p6.startTime)
        assertEquals("14:30", p6.endTime)
        assertFalse(p6.isExam)

        // Check dynamic max periods
        val maxPeriods = manager.getMaxPeriodsForDate(date)
        assertEquals(6, maxPeriods)
    }

    @Test
    fun testSecondTerm_AppliedFromOctoberSixth() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = CsvSyncManager(context)
        val classGroup1 = ClassGroup(id = "1", name = "1組", grade = 1, section = "1")

        // 前期時間割: 1組 は "前期数学"
        val firstTermCsv = """
            class,type,day_of_week,first_period,second_period,third_period,fourth_period,fifth_period
            1,A,月,前期数学,英語,国語,理科,社会
            1,A,火,前期数学,英語,国語,理科,社会
        """.trimIndent()
        manager.setBasicClassScheduleForTesting(CsvParser.parseBasicClassSchedule(firstTermCsv))

        // 後期時間割: 1組 は "A2"
        val secondTermCsv = """
            class,type,day_of_week,first_period,second_period,third_period,fourth_period,fifth_period
            1,A,月,A2,E2,古典,英語W,英語R
            1,A,火,A2,E2,古典,英語W,英語R
        """.trimIndent()
        manager.setSecondTermScheduleForTesting(CsvParser.parseBasicClassSchedule(secondTermCsv))

        // 10月5日（前期最終日）: 前期時間割が適用される
        val dateOct5 = LocalDate.of(2026, 10, 5)
        assertFalse(manager.isSecondTerm(dateOct5))
        val schedOct5 = manager.resolvePeriodSchedule(classGroup1, dateOct5, 1, emptyMap())
        assertEquals("前期数学", schedOct5.subject)

        // 10月6日（後期初日）: 後期時間割が適用される
        val dateOct6 = LocalDate.of(2026, 10, 6)
        assertTrue(manager.isSecondTerm(dateOct6))
        val schedOct6 = manager.resolvePeriodSchedule(classGroup1, dateOct6, 1, emptyMap())
        assertEquals("A2", schedOct6.subject)
    }

    @Test
    fun testActualUserScenario_Oct6SpecialScheduleWithSecondTerm() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = CsvSyncManager(context)
        val classGroup1 = ClassGroup(id = "1", name = "1組", grade = 1, section = "1")

        // ユーザーから提供された特別時程 CSV (gid=2041839281)
        val specialScheduleCsv = """
            date,period,subject,start_time,end_time
            2026/10/06,1,始業式等,,
            2026/10/06,2,A火1,,
            2026/10/06,3,B火2,,
            2026/10/06,4,B火3,,
            2026/10/06,5,A火4,,
            2026/10/06,6,A火5,,
        """.trimIndent()
        manager.setSpecialScheduleForTesting(CsvParser.parseSpecialSchedule(specialScheduleCsv))

        // ユーザーから提供された後期時間割 CSV (gid=763994660)
        val secondTermCsv = """
            class,type,day_of_week,first_period,second_period,third_period,fourth_period,fifth_period
            1,A,火,A2,E2,古典,英語W,英語R
            1,B,火,A2,E1,IBA Ⅳ,現代文,英語R
        """.trimIndent()
        manager.setSecondTermScheduleForTesting(CsvParser.parseBasicClassSchedule(secondTermCsv))

        val date = LocalDate.of(2026, 10, 6)

        // 1限: 始業式等
        val p1 = manager.resolvePeriodSchedule(classGroup1, date, 1, emptyMap())
        assertEquals("始業式等", p1.subject)

        // 2限: A火1 -> 後期A火1 -> A2
        val p2 = manager.resolvePeriodSchedule(classGroup1, date, 2, emptyMap())
        assertEquals("A2", p2.subject)

        // 3限: B火2 -> 後期B火2 -> E1
        val p3 = manager.resolvePeriodSchedule(classGroup1, date, 3, emptyMap())
        assertEquals("E1", p3.subject)

        // 4限: B火3 -> 後期B火3 -> IBA Ⅳ
        val p4 = manager.resolvePeriodSchedule(classGroup1, date, 4, emptyMap())
        assertEquals("IBA Ⅳ", p4.subject)

        // 5限: A火4 -> 後期A火4 -> 英語W
        val p5 = manager.resolvePeriodSchedule(classGroup1, date, 5, emptyMap())
        assertEquals("英語W", p5.subject)

        // 6限: A火5 -> 後期A火5 -> 英語R
        val p6 = manager.resolvePeriodSchedule(classGroup1, date, 6, emptyMap())
        assertEquals("英語R", p6.subject)

        // 10月6日の最大時限数は動的に6限まで拡張
        val maxPeriods = manager.getMaxPeriodsForDate(date)
        assertEquals(6, maxPeriods)
    }

    @Test
    fun testDefaultUrlsConfigured() {
        val config = CsvUrlsConfig()
        assertEquals(
            "https://docs.google.com/spreadsheets/d/e/2PACX-1vQSizltFHoOWYdi97m2q_x21-XHwaeeMTzbUk0jlWCZRAD-CmsGn9uKZQMe2rHbIxP7_pEekWK84yf9/pub?gid=2041839281&single=true&output=csv",
            config.specialScheduleUrl
        )
        assertEquals(
            "https://docs.google.com/spreadsheets/d/e/2PACX-1vQSizltFHoOWYdi97m2q_x21-XHwaeeMTzbUk0jlWCZRAD-CmsGn9uKZQMe2rHbIxP7_pEekWK84yf9/pub?gid=763994660&single=true&output=csv",
            config.secondTermClassUrl
        )
    }
}
