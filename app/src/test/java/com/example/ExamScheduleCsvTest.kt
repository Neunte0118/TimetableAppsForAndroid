package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.csv.CsvParser
import com.example.data.csv.CsvSyncManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExamScheduleCsvTest {

    @Test
    fun parseExamSchedule_withoutClassesColumn() {
        // dates, periods, subjects, start_time, end_time, classroom
        val csv = """
            dates,periods,subjects,start_time,end_time,classroom
            10/14,1,現代の国語,8:50,9:40,
            10/14,2,数学Ⅰ,10:00,10:50,
            10/14,3,英語コミュニケーションⅠ,11:10,12:00,
            10/15,1,化学,8:50,9:50,理科第1講義室
        """.trimIndent()

        val result = CsvParser.parseExamSchedule(csv)
        assertEquals(4, result.size)

        val first = result[0]
        assertEquals(10, first.month)
        assertEquals(14, first.day)
        assertEquals(1, first.period)
        assertEquals("現代の国語", first.subject)
        assertEquals("8:50", first.startTime)
        assertEquals("9:40", first.endTime)
        assertEquals("", first.classroom)
        assertEquals("全", first.classId)

        val chem = result[3]
        assertEquals(10, chem.month)
        assertEquals(15, chem.day)
        assertEquals(1, chem.period)
        assertEquals("化学", chem.subject)
        assertEquals("8:50", chem.startTime)
        assertEquals("9:50", chem.endTime)
        assertEquals("理科第1講義室", chem.classroom)
    }

    @Test
    fun parseExamSchedule_japaneseHeadersWithoutClasses() {
        val csv = """
            日付,時限,科目,開始時間,終了時間,教室
            10/16,1,日本史探究,9:00,10:00,
        """.trimIndent()

        val result = CsvParser.parseExamSchedule(csv)
        assertEquals(1, result.size)
        assertEquals(10, result[0].month)
        assertEquals(16, result[0].day)
        assertEquals(1, result[0].period)
        assertEquals("日本史探究", result[0].subject)
        assertEquals("9:00", result[0].startTime)
        assertEquals("10:00", result[0].endTime)
    }

    @Test
    fun testSubjectMappingMatching() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = CsvSyncManager(context)

        // Test matching with default mapping CSV (source -> target)
        // e.g. 世特① -> 世界史特講, ⅡLa① -> 数学ⅡL, 生S① -> 生物講究, 数講Sa① -> 数学講究
        assertTrue(manager.isSubjectMatch("世特①", "世界史特講"))
        assertTrue(manager.isSubjectMatch("世界史特講", "世特①"))
        assertTrue(manager.isSubjectMatch("ⅡLa①", "数学ⅡL"))
        assertTrue(manager.isSubjectMatch("数学ⅡL", "ⅡLa①"))
        assertTrue(manager.isSubjectMatch("生S①", "生物講究"))
        assertTrue(manager.isSubjectMatch("生物講究", "生S①"))
        assertTrue(manager.isSubjectMatch("数講Sa①", "数学講究"))
        assertTrue(manager.isSubjectMatch("現世読", "現代世界を読む"))

        // Exact match
        assertTrue(manager.isSubjectMatch("数学ⅡL", "数学ⅡL"))
        assertTrue(manager.isSubjectMatch("世特①", "世特①"))

        // Unrelated subjects should NOT match
        assertTrue(!manager.isSubjectMatch("生S①", "物理講究"))
        assertTrue(!manager.isSubjectMatch("物S①", "生物講究"))
        assertTrue(!manager.isSubjectMatch("世特①", "日本史特講"))
        assertTrue(!manager.isSubjectMatch("世特①", "日特①"))
    }

    @Test
    fun testExamElectiveFilteringInSamePeriod() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = CsvSyncManager(context)
        val classGroup = com.example.model.DefaultSchoolData.classes.first()

        // Sample electives with course codes as in electives.csv
        val electivesCsv = """
            origins,electives
            地歴選,世特①
            地歴選,日特①
            地歴選,地理特①
            理選,生S①
            理選,物S①
            理選,化S①
            文理選,現世読
            文理選,数講Sa①
        """.trimIndent()
        manager.setElectivesForTesting(CsvParser.parseElectives(electivesCsv))

        // User selected "世特①" for "地歴選" and "生S①" for "理選"
        val userElectives = mapOf(
            "地歴選" to "世特①",
            "理選" to "生S①"
        )

        // Exam CSV with target subject names:
        // 9月25日 3限: 現世読, 数学講究
        // 9月28日 3限: 生物講究, 物理講究
        // 9月29日 4限: 世界史特講
        val examCsv = """
            dates,periods,subjects,start_time,end_time,classroom
            9月25日,3,現世読,11:10,12:20,-
            9月25日,3,数学講究,11:10,12:40,-
            9月28日,3,生物講究,11:10,12:20,-
            9月28日,3,物理講究,11:10,12:20,-
            9月29日,4,世界史特講,11:55,12:45,-
        """.trimIndent()
        manager.setExamScheduleForTesting(CsvParser.parseExamSchedule(examCsv))

        // On 9月28日 3限, user selected "生S①". Through mapping, "生S①" matches "生物講究". "物理講究" is filtered out.
        val schedule28P3 = manager.resolvePeriodSchedule(
            classGroup,
            java.time.LocalDate.of(2026, 9, 28),
            3,
            userElectives
        )
        assertEquals("生物講究", schedule28P3.subject)
        assertTrue(schedule28P3.isExam)
        assertTrue(!schedule28P3.isUnselectedElective)

        // On 9月29日 4限, user has "世特①" in userElectives. Exam is "世界史特講". It should match via mapping!
        val schedule29P4 = manager.resolvePeriodSchedule(
            classGroup,
            java.time.LocalDate.of(2026, 9, 29),
            4,
            userElectives
        )
        assertEquals("世界史特講", schedule29P4.subject)
        assertTrue(schedule29P4.isExam)

        // What if user selected "物S①" instead? Then on 9月28日 3限, "物理講究" should be shown.
        val userElectives2 = mapOf("理選" to "物S①")
        val schedule28P3Physics = manager.resolvePeriodSchedule(
            classGroup,
            java.time.LocalDate.of(2026, 9, 28),
            3,
            userElectives2
        )
        assertEquals("物理講究", schedule28P3Physics.subject)
        assertTrue(!schedule28P3Physics.isUnselectedElective)

        // 自分の選択科目でないところは灰色で表示 (isUnselectedElective = true)
        // 同じ時間に複数の科目がある場合は、先頭の科目を表示 (例: "現世読")
        val schedule25P3Unselected = manager.resolvePeriodSchedule(
            classGroup,
            java.time.LocalDate.of(2026, 9, 25),
            3,
            userElectives2 // userElectives2 only has "理選" -> "物S①", not 現世読 or 数学講究
        )
        assertEquals("現世読", schedule25P3Unselected.subject)
        assertTrue(schedule25P3Unselected.isExam)
        assertTrue(schedule25P3Unselected.isUnselectedElective)

        // Suppose there's a period with only "生物講究" exam. If user chose "物S①", it displays "生物講究" grayed out.
        val singleExamCsv = """
            dates,periods,subjects,start_time,end_time,classroom
            9月28日,3,生物講究,11:10,12:20,-
        """.trimIndent()
        manager.setExamScheduleForTesting(CsvParser.parseExamSchedule(singleExamCsv))
        val scheduleOtherChosen = manager.resolvePeriodSchedule(
            classGroup,
            java.time.LocalDate.of(2026, 9, 28),
            3,
            userElectives2 // User selected 物S①, but exam is 生物講究
        )
        assertEquals("生物講究", scheduleOtherChosen.subject)
        assertTrue(scheduleOtherChosen.isExam)
        assertTrue(scheduleOtherChosen.isUnselectedElective)
    }

    @Test
    fun testExamScheduleWithClassroomsAndHrSpecialCase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = CsvSyncManager(context)
        val classGroup3 = com.example.model.ClassGroup(id = "3", name = "3組", grade = 3, section = "3")
        val classGroup4 = com.example.model.ClassGroup(id = "4", name = "4組", grade = 3, section = "4")

        // User's exact prompt format:
        // dates periods subjects start_time end_time classroom
        // 9月25日 1 日特① 8:40 9:30 3組
        // 9月25日 1 日特② 8:40 9:30 4組
        // 9月25日 2 英語W 9:45 10:55 $hr
        val examCsv = """
            dates periods subjects start_time end_time classroom
            9月25日 1 日特① 8:40 9:30 3組
            9月25日 1 日特② 8:40 9:30 4組
            9月25日 2 英語W 9:45 10:55 ${'$'}hr
        """.trimIndent()

        val parsed = CsvParser.parseExamSchedule(examCsv)
        assertEquals(3, parsed.size)
        assertEquals("日特①", parsed[0].subject)
        assertEquals("3組", parsed[0].classroom)
        assertEquals("8:40", parsed[0].startTime)
        assertEquals("9:30", parsed[0].endTime)

        assertEquals("日特②", parsed[1].subject)
        assertEquals("4組", parsed[1].classroom)

        assertEquals("英語W", parsed[2].subject)
        assertEquals("${'$'}hr", parsed[2].classroom)
        assertEquals("9:45", parsed[2].startTime)
        assertEquals("10:55", parsed[2].endTime)

        manager.setExamScheduleForTesting(parsed)

        // 1. Student in 3組 chose "日特①"
        val userElectives1 = mapOf("地歴選" to "日特①")
        val schedule1P1 = manager.resolvePeriodSchedule(
            classGroup3,
            java.time.LocalDate.of(2026, 9, 25),
            1,
            userElectives1
        )
        assertEquals("日特①", schedule1P1.subject)
        assertEquals("3組", schedule1P1.classroom)
        assertEquals("8:40", schedule1P1.startTime)
        assertEquals("9:30", schedule1P1.endTime)
        assertTrue(schedule1P1.isExam)
        assertTrue(!schedule1P1.isUnselectedElective)

        // Period 2: 英語W with $hr classroom -> HR教室（3組）
        val schedule1P2 = manager.resolvePeriodSchedule(
            classGroup3,
            java.time.LocalDate.of(2026, 9, 25),
            2,
            userElectives1
        )
        assertEquals("英語W", schedule1P2.subject)
        assertEquals("3組", schedule1P2.classroom)
        assertEquals("9:45", schedule1P2.startTime)
        assertEquals("10:55", schedule1P2.endTime)
        assertTrue(schedule1P2.isExam)
        assertTrue(!schedule1P2.isUnselectedElective)

        // 2. Student in 4組 chose "日特②"
        val userElectives2 = mapOf("地歴選" to "日特②")
        val schedule2P1 = manager.resolvePeriodSchedule(
            classGroup4,
            java.time.LocalDate.of(2026, 9, 25),
            1,
            userElectives2
        )
        assertEquals("日特②", schedule2P1.subject)
        assertEquals("4組", schedule2P1.classroom)
        assertEquals("8:40", schedule2P1.startTime)
        assertEquals("9:30", schedule2P1.endTime)
        assertTrue(schedule2P1.isExam)
        assertTrue(!schedule2P1.isUnselectedElective)

        // Period 2: 英語W with $hr classroom -> HR教室（4組）
        val schedule2P2 = manager.resolvePeriodSchedule(
            classGroup4,
            java.time.LocalDate.of(2026, 9, 25),
            2,
            userElectives2
        )
        assertEquals("英語W", schedule2P2.subject)
        assertEquals("4組", schedule2P2.classroom)
        assertTrue(schedule2P2.isExam)
        assertTrue(!schedule2P2.isUnselectedElective)

        // 3. Student in 3組 chose "日特②" (takes exam in 4組)
        val schedule3P1 = manager.resolvePeriodSchedule(
            classGroup3,
            java.time.LocalDate.of(2026, 9, 25),
            1,
            userElectives2
        )
        assertEquals("日特②", schedule3P1.subject)
        assertEquals("4組", schedule3P1.classroom) // Takes in 4組
        assertTrue(schedule3P1.isExam)
        assertTrue(!schedule3P1.isUnselectedElective)

        val schedule3P2 = manager.resolvePeriodSchedule(
            classGroup3,
            java.time.LocalDate.of(2026, 9, 25),
            2,
            userElectives2
        )
        assertEquals("英語W", schedule3P2.subject)
        assertEquals("3組", schedule3P2.classroom) // Takes in 3組 because $hr is 3組 for classGroup3
    }
}
