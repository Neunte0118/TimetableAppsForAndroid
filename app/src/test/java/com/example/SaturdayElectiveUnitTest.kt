package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.csv.BasicClassScheduleRow
import com.example.data.csv.CsvSyncManager
import com.example.data.csv.ElectiveItemRow
import com.example.data.csv.SpecialScheduleRow
import com.example.data.csv.TimetableChangeNotificationRow
import com.example.model.ClassGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SaturdayElectiveUnitTest {

    private lateinit var context: Context
    private lateinit var syncManager: CsvSyncManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        syncManager = CsvSyncManager(context)
        syncManager.clearAllCache()
    }

    @Test
    fun testCombinedExistingAndSaturdayLectureOptionsForClass() {
        // 基本時間割に「理選」と「日特①」があり、「世特①」は別クラス用で1組にはない構成
        syncManager.setBasicClassScheduleForTesting(
            listOf(
                BasicClassScheduleRow(
                    classId = "1",
                    type = "A",
                    dayOfWeek = "月",
                    periods = listOf("理選", "現代文", "日特①", "英語R", "古典")
                ),
                BasicClassScheduleRow(
                    classId = "2",
                    type = "A",
                    dayOfWeek = "月",
                    periods = listOf("世特①", "現代文", "英語R", "古典", "HR")
                )
            )
        )
        syncManager.setElectivesForTesting(
            listOf(
                ElectiveItemRow(origin = "理選", elective = "物理", name = "物理室"),
                ElectiveItemRow(origin = "理選", elective = "生物", name = "生物室"),
                ElectiveItemRow(origin = "日特①", elective = "日本史演習A", name = "1組"),
                ElectiveItemRow(origin = "日特①", elective = "日本史演習B", name = "大講義室"),
                ElectiveItemRow(origin = "世特①", elective = "世界史演習A", name = "2組")
            )
        )

        val options = syncManager.getElectiveOptionsForClass("1")

        // 元の選択科目（理選・日特①）が残っており、使われない世特①は除外されていること
        assertTrue("Existing elective 理選 should be preserved", options.containsKey("理選"))
        assertTrue("Existing elective 日特① should be preserved", options.containsKey("日特①"))
        assertFalse("Unused elective 世特① should be excluded for class 1", options.containsKey("世特①"))
        assertEquals(listOf("物理", "生物"), options["理選"])

        // 土曜講習①〜④ も合わせて含まれていること
        assertTrue(options.containsKey("土曜講習①"))
        assertTrue(options.containsKey("土曜講習②"))
        assertTrue(options.containsKey("土曜講習③"))
        assertTrue(options.containsKey("土曜講習④"))

        assertEquals(listOf("数学D", "数学F", "なし"), options["土曜講習①"])
        assertEquals(listOf("倫理", "数学E", "なし"), options["土曜講習②"])
        assertEquals(listOf("国語A", "なし"), options["土曜講習③"])
        assertEquals(listOf("国語B", "なし"), options["土曜講習④"])
    }

    @Test
    fun testSaturdayScheduleResolutionWithUserElectives() {
        // 2026/11/14 は土曜日
        val saturday = LocalDate.of(2026, 11, 14)

        val changeRow = TimetableChangeNotificationRow(
            dateStr = "11/14",
            month = 11,
            day = 14,
            classId = "0",
            period = 1,
            subject = "土曜講習①"
        )
        syncManager.setTimetableChangesForTesting(listOf(changeRow))

        val class3 = ClassGroup(id = "3", name = "3組", grade = 3, section = "3")

        // 1. 「数学F」を選択した場合
        val periodMathF = syncManager.resolvePeriodSchedule(
            classGroup = class3,
            date = saturday,
            period = 1,
            userElectives = mapOf("土曜講習①" to "数学F")
        )
        assertEquals("数学F", periodMathF.subject)
        assertFalse(periodMathF.isUnselectedElective)

        // 2. 「なし」を選択した場合 → 何も表示しない
        val periodNone = syncManager.resolvePeriodSchedule(
            classGroup = class3,
            date = saturday,
            period = 1,
            userElectives = mapOf("土曜講習①" to "なし")
        )
        assertEquals("", periodNone.subject)
        assertEquals("", periodNone.classroom)
        assertEquals("", periodNone.startTime)
        assertEquals("", periodNone.endTime)
        assertFalse(periodNone.isUnselectedElective)

        // 3. 未選択の場合 → 何も表示しない
        val periodUnselected = syncManager.resolvePeriodSchedule(
            classGroup = class3,
            date = saturday,
            period = 1,
            userElectives = emptyMap()
        )
        assertEquals("", periodUnselected.subject)
        assertEquals("", periodUnselected.classroom)
        assertEquals("", periodUnselected.startTime)
        assertEquals("", periodUnselected.endTime)
        assertTrue(periodUnselected.isUnselectedElective)
    }

    @Test
    fun testSaturdaySingleSubjectOnlyDayDoesNotShowUnheldAlternative() {
        // 2026/11/21 は土曜日: この日は1限が「数学D」のみで、数学Fは開講されない日
        val saturdayDOnly = LocalDate.of(2026, 11, 21)
        val class3 = ClassGroup(id = "3", name = "3組", grade = 3, section = "3")

        syncManager.setSpecialScheduleForTesting(
            listOf(
                SpecialScheduleRow(
                    dateStr = "11/21",
                    month = 11,
                    day = 21,
                    period = 1,
                    subject = "数学D",
                    startTime = "8:50",
                    endTime = "9:40"
                ),
                SpecialScheduleRow(
                    dateStr = "11/21",
                    month = 11,
                    day = 21,
                    period = 2,
                    subject = "倫理",
                    startTime = "9:50",
                    endTime = "10:40"
                )
            )
        )

        // 数学Dを選択しているユーザー → 「数学D」が表示される
        val p1MathDUser = syncManager.resolvePeriodSchedule(
            classGroup = class3,
            date = saturdayDOnly,
            period = 1,
            userElectives = mapOf("土曜講習①" to "数学D")
        )
        assertEquals("数学D", p1MathDUser.subject)
        assertEquals("8:50", p1MathDUser.startTime)
        assertEquals("9:40", p1MathDUser.endTime)
        assertFalse(p1MathDUser.isUnselectedElective)

        // 数学Fを選択しているユーザー → Dだけの日なのでFは表示されず何も表示しない
        val p1MathFUser = syncManager.resolvePeriodSchedule(
            classGroup = class3,
            date = saturdayDOnly,
            period = 1,
            userElectives = mapOf("土曜講習①" to "数学F")
        )
        assertEquals("", p1MathFUser.subject)
        assertEquals("", p1MathFUser.classroom)
        assertEquals("", p1MathFUser.startTime)
        assertEquals("", p1MathFUser.endTime)
        assertFalse(p1MathFUser.isUnselectedElective)

        // 数学Eを選択しているユーザー → 倫理だけの日なので数学Eは表示されず何も表示しない
        val p2MathEUser = syncManager.resolvePeriodSchedule(
            classGroup = class3,
            date = saturdayDOnly,
            period = 2,
            userElectives = mapOf("土曜講習②" to "数学E")
        )
        assertEquals("", p2MathEUser.subject)
        assertEquals("", p2MathEUser.startTime)
        assertEquals("", p2MathEUser.endTime)
        assertFalse(p2MathEUser.isUnselectedElective)

        // 未選択のユーザー → 何も表示しない
        val p1UnselectedUser = syncManager.resolvePeriodSchedule(
            classGroup = class3,
            date = saturdayDOnly,
            period = 1,
            userElectives = emptyMap()
        )
        assertEquals("", p1UnselectedUser.subject)
        assertEquals("", p1UnselectedUser.startTime)
        assertEquals("", p1UnselectedUser.endTime)

        // 複数行または「数学D/数学F」表記の日は、選択している数学Fが表示される
        val saturdayBoth = LocalDate.of(2026, 11, 28)
        syncManager.setSpecialScheduleForTesting(
            listOf(
                SpecialScheduleRow(
                    dateStr = "11/28",
                    month = 11,
                    day = 28,
                    period = 1,
                    subject = "数学D/数学F",
                    startTime = "8:50",
                    endTime = "9:40"
                )
            )
        )
        val p1BothMathF = syncManager.resolvePeriodSchedule(
            classGroup = class3,
            date = saturdayBoth,
            period = 1,
            userElectives = mapOf("土曜講習①" to "数学F")
        )
        assertEquals("数学F", p1BothMathF.subject)
        assertEquals("8:50", p1BothMathF.startTime)
        assertEquals("9:40", p1BothMathF.endTime)
        assertFalse(p1BothMathF.isUnselectedElective)
    }
}
