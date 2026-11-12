package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.csv.CsvSyncManager
import com.example.data.csv.TimetableChangeNotificationRow
import com.example.domain.model.ClassGroup
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
    fun testSaturdayLectureOptionsGeneratedForClass() {
        val options = syncManager.getElectiveOptionsForClass("3")
        // 土曜講習①〜④ が必ず含まれる
        assertTrue("土曜講習① should be present", options.containsKey("土曜講習①"))
        assertTrue("土曜講習② should be present", options.containsKey("土曜講習②"))
        assertTrue("土曜講習③ should be present", options.containsKey("土曜講習③"))
        assertTrue("土曜講習④ should be present", options.containsKey("土曜講習④"))

        // 各選択肢の確認（「なし」が含まれていること）
        val sat1 = options["土曜講習①"] ?: emptyList()
        assertEquals(listOf("数学D", "数学F", "なし"), sat1)

        val sat2 = options["土曜講習②"] ?: emptyList()
        assertEquals(listOf("倫理", "数学E", "なし"), sat2)

        val sat3 = options["土曜講習③"] ?: emptyList()
        assertEquals(listOf("国語A", "なし"), sat3)

        val sat4 = options["土曜講習④"] ?: emptyList()
        assertEquals(listOf("国語B", "なし"), sat4)
    }

    @Test
    fun testSaturdayScheduleResolutionWithUserElectives() {
        // 土曜日 (2026/11/14 は土曜日)
        val saturday = LocalDate.of(2026, 11, 14)

        // 土曜日の時間割変更として「土曜講習①」または「土講1」が1限に登録されているケース
        val changeRow = TimetableChangeNotificationRow(
            sheetName = "11月14日",
            month = 11,
            day = 14,
            period = 1,
            classNames = listOf("3組", "全クラス"),
            subject = "土曜講習①",
            classroom = "講義室A",
            memo = "土曜講習第1回"
        )
        syncManager.setTimetableChangesForTesting(listOf(changeRow))

        // 1. ユーザーが「数学F」を選択した場合
        val userElectivesMathF = mapOf("土曜講習①" to "数学F")
        val periodScheduleMathF = syncManager.resolvePeriodSchedule(
            date = saturday,
            period = 1,
            classGroup = ClassGroup.CLASS_3,
            userElectives = userElectivesMathF,
            classroomList = emptyList()
        )
        assertEquals("数学F", periodScheduleMathF.subject)
        assertFalse(periodScheduleMathF.isUnselectedElective)

        // 2. ユーザーが「なし」を選択した場合
        val userElectivesNone = mapOf("土曜講習①" to "なし")
        val periodScheduleNone = syncManager.resolvePeriodSchedule(
            date = saturday,
            period = 1,
            classGroup = ClassGroup.CLASS_3,
            userElectives = userElectivesNone,
            classroomList = emptyList()
        )
        assertEquals("なし", periodScheduleNone.subject)
        assertEquals("", periodScheduleNone.classroom)
        assertFalse("Explicitly selected 'なし' should NOT be marked as unselected", periodScheduleNone.isUnselectedElective)

        // 3. ユーザーが未選択（空文字）の場合
        val periodScheduleUnselected = syncManager.resolvePeriodSchedule(
            date = saturday,
            period = 1,
            classGroup = ClassGroup.CLASS_3,
            userElectives = emptyMap(),
            classroomList = emptyList()
        )
        assertTrue("When no choice made, should be marked as unselected", periodScheduleUnselected.isUnselectedElective)
    }

    @Test
    fun testUnselectedCheckLogic() {
        val classId = "1"
        val options = syncManager.getElectiveOptionsForClass(classId)
        assertTrue(options.isNotEmpty())

        // 全て未選択の場合
        val emptySelections = emptyMap<String, String>()
        val hasUnselected1 = options.keys.any { origin ->
            val choice = emptySelections[origin]?.trim() ?: ""
            choice.isBlank()
        }
        assertTrue("Empty selections must count as unselected", hasUnselected1)

        // 土曜講習含めて全て選択された場合（受講しない講習は「なし」を選択）
        val fullSelections = options.keys.associateWith { origin ->
            when (origin) {
                "土曜講習①" -> "数学F"
                "土曜講習②" -> "なし"
                "土曜講習③" -> "国語A"
                "土曜講習④" -> "なし"
                else -> options[origin]?.firstOrNull() ?: "選択A"
            }
        }
        val hasUnselected2 = options.keys.any { origin ->
            val choice = fullSelections[origin]?.trim() ?: ""
            choice.isBlank()
        }
        assertFalse("All electives chosen (including 'なし') must not be unselected", hasUnselected2)
    }
}
