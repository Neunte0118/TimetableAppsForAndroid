package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TimetableRepository
import com.example.data.csv.BasicClassScheduleRow
import com.example.data.csv.CsvParser
import com.example.data.csv.CsvSyncManager
import com.example.model.ClassGroup
import com.example.notification.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpecialScheduleWidgetAndNotificationTest {

    private lateinit var context: Context
    private lateinit var repo: TimetableRepository
    private lateinit var syncManager: CsvSyncManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repo = TimetableRepository.getInstance(context)
        syncManager = repo.csvSyncManager
        syncManager.clearAllCache()
    }

    @Test
    fun testSpecialScheduleSetsIsSpecialFlagAndStartEndTime() {
        val classGroup = ClassGroup(id = "1", name = "1組", grade = 1, section = "1")
        val specialCsv = """
            date, period, subject, start_time, end_time
            11/10, 1, 国語, 8:50, 9:35
            11/10, 2, 数学, 9:45, 10:30
        """.trimIndent()
        syncManager.setSpecialScheduleForTesting(CsvParser.parseSpecialSchedule(specialCsv))

        val date = LocalDate.of(2026, 11, 10)
        val p1 = syncManager.resolvePeriodSchedule(classGroup, date, 1, emptyMap())

        assertTrue(p1.isSpecialSchedule)
        assertFalse(p1.isExam)
        assertEquals("8:50", p1.startTime)
        assertEquals("9:35", p1.endTime)
        assertTrue(syncManager.isExamOrSpecialDate(date))
    }

    @Test
    fun testExamScheduleSetsIsExamFlagAndStartEndTime() {
        val classGroup = ClassGroup(id = "1", name = "1組", grade = 1, section = "1")
        val examCsv = """
            date, periods, subjects, start_time, end_time, classroom
            12/1, 1, 現代文, 8:50, 9:40, 1組
        """.trimIndent()
        syncManager.setExamScheduleForTesting(CsvParser.parseExamSchedule(examCsv))

        val date = LocalDate.of(2026, 12, 1)
        val p1 = syncManager.resolvePeriodSchedule(classGroup, date, 1, emptyMap())

        assertTrue(p1.isExam)
        assertEquals("8:50", p1.startTime)
        assertEquals("9:40", p1.endTime)
        assertTrue(syncManager.isExamOrSpecialDate(date))
    }

    @Test
    fun testNextClassNotificationExecution() {
        val success = NotificationHelper.showNextClassNotification(
            context,
            1,
            LocalDate.of(2026, 10, 20),
            isTest = true
        )
        assertTrue(success)
    }

    @Test
    fun testNormalTimetableNotificationIncludesTimes() {
        val classGroup = ClassGroup(id = "1", name = "1組", grade = 1, section = "1")
        repo.setSelectedClass(classGroup)

        // 普通時間割を設定
        syncManager.setBasicClassScheduleForTesting(listOf(
            BasicClassScheduleRow(
                classId = "1",
                type = "A",
                dayOfWeek = "火",
                periods = listOf("国語", "数学", "英語", "理科", "社会")
            )
        ))

        // 2026/10/20 は火曜日
        val date = LocalDate.of(2026, 10, 20)
        val p1 = syncManager.resolvePeriodSchedule(classGroup, date, 1, emptyMap())
        assertEquals("8:30", p1.startTime)
        assertEquals("9:35", p1.endTime)

        val success = NotificationHelper.showNextClassNotification(context, 1, date, isTest = false)
        assertTrue(success)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowManager = Shadows.shadowOf(notificationManager)
        val notification = shadowManager.getNotification(NotificationHelper.NOTIFICATION_ID_NEXT_CLASS)

        val extras = notification.extras
        val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""

        // 普通時間割でも8:30〜9:35が含まれていることを確認
        assertTrue("タイトルに時間が含まれる", title.contains("8:30〜9:35"))
        assertTrue("本文に時間が含まれる", text.contains("8:30〜9:35"))
    }
}
