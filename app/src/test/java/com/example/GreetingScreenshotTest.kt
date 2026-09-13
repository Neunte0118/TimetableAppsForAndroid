package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.DefaultSchoolData
import com.example.model.DaySchedule
import com.example.model.PeriodSchedule
import com.example.model.TimetableViewMode
import com.example.ui.components.TimetableTable
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val date = LocalDate.of(2026, 8, 24)
    val daySchedules = (0..3).map { offset ->
      DaySchedule(
        date = date.plusDays(offset.toLong()),
        dayLabel = when(offset) { 0 -> "今日"; 1 -> "翌日"; 2 -> "2日後"; else -> "3日後" },
        periods = (1..5).map { PeriodSchedule(it, "数学", "教室") },
        event = "通常授業",
        memo = "宿題提出"
      )
    }

    composeTestRule.setContent {
      MyApplicationTheme {
        TimetableTable(
          daySchedules = daySchedules,
          selectedDate = date,
          today = date,
          viewMode = TimetableViewMode.TIMETABLE,
          onSelectDate = { }
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

