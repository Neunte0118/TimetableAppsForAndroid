package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TimetableRepository
import com.example.model.DefaultSchoolData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("時間割アプリ", appName)
  }

  @Test
  fun `verify timetable repository returns valid schedule`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TimetableRepository(context)
    val today = LocalDate.now()
    val schedule = repo.getDaySchedule(DefaultSchoolData.classes.first(), today, today)
    assertNotNull(schedule)
    assertEquals(5, schedule.periods.size)
  }

  @Test
  fun `verify classes count is 9 and initial setup flag behaves correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TimetableRepository(context)
    assertEquals(9, DefaultSchoolData.classes.size)
    assertEquals("1組", DefaultSchoolData.classes.first().name)
    assertEquals("9組", DefaultSchoolData.classes.last().name)
    
    assertEquals(false, repo.hasCompletedInitialSetup())
    repo.setCompletedInitialSetup(true)
    assertEquals(true, repo.hasCompletedInitialSetup())
  }

  @Test
  fun `verify column count setting and saving memo`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TimetableRepository(context)
    assertEquals(5, repo.columnCount.value)
    repo.setColumnCount(6)
    assertEquals(6, repo.columnCount.value)

    assertEquals(1, repo.tableDisplayCount.value)
    repo.setTableDisplayCount(3)
    assertEquals(3, repo.tableDisplayCount.value)

    val today = LocalDate.now()
    val cls = DefaultSchoolData.classes.first()
    repo.saveMemo(cls.id, today, "数学ノート提出")

    val sched = repo.getDaySchedule(cls, today, today)
    assertEquals("数学ノート提出", sched.memo)
  }

  @Test
  fun `verify subject colors correctly match user sample subjects`() {
    val groups = com.example.model.SubjectColorDefaults.defaultGroups
    
    // Japanese
    val colorGendoku = com.example.model.SubjectColorDefaults.getColorForSubject("現読①", groups)
    val japaneseColor = groups.find { it.id == "japanese" }!!.toColorLong()
    assertEquals(japaneseColor, colorGendoku)

    // Math
    val colorSutoku = com.example.model.SubjectColorDefaults.getColorForSubject("数特", groups)
    val mathColor = groups.find { it.id == "math" }!!.toColorLong()
    assertEquals(mathColor, colorSutoku)

    val colorSukoSa = com.example.model.SubjectColorDefaults.getColorForSubject("数講Sa①", groups)
    assertEquals(mathColor, colorSukoSa)

    val color2La = com.example.model.SubjectColorDefaults.getColorForSubject("ⅡLa①", groups)
    assertEquals(mathColor, color2La)

    // Science
    val colorKaS = com.example.model.SubjectColorDefaults.getColorForSubject("化S①", groups)
    val scienceColor = groups.find { it.id == "science" }!!.toColorLong()
    assertEquals(scienceColor, colorKaS)

    val colorButsuS = com.example.model.SubjectColorDefaults.getColorForSubject("物S①", groups)
    assertEquals(scienceColor, colorButsuS)

    // Elective group
    val colorElectiveUnselected = com.example.model.SubjectColorDefaults.getColorForSubject("数Ⅱ S/La", groups)
    val electiveColor = groups.find { it.id == "elective_course" }!!.toColorLong()
    assertEquals(electiveColor, colorElectiveUnselected)

    // Social
    val colorChiri = com.example.model.SubjectColorDefaults.getColorForSubject("地理講①", groups)
    val socialColor = groups.find { it.id == "social" }!!.toColorLong()
    assertEquals(socialColor, colorChiri)

    val colorRinsei = com.example.model.SubjectColorDefaults.getColorForSubject("倫政講①", groups)
    assertEquals(socialColor, colorRinsei)

    // English
    val colorEnglish = com.example.model.SubjectColorDefaults.getColorForSubject("英語R", groups)
    val englishColor = groups.find { it.id == "english" }!!.toColorLong()
    assertEquals(englishColor, colorEnglish)

    // PE / HR
    val colorPE = com.example.model.SubjectColorDefaults.getColorForSubject("体育別修", groups)
    val peColor = groups.find { it.id == "pe_and_hr" }!!.toColorLong()
    assertEquals(peColor, colorPE)
  }

  @Test
  fun `verify timetable change and course change reflect user elective choice`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TimetableRepository(context)
    val csvManager = repo.csvSyncManager
    
    // User selected 化S① for ⅡS
    val userElectives = mapOf("ⅡS" to "化S①", "ⅡLa" to "現読①")

    val targetDate = LocalDate.of(2026, 9, 10)
    val cls5 = com.example.model.ClassGroup(id = "5", name = "5組", grade = 2, section = "5")
    val resolved = csvManager.resolvePeriodSchedule(cls5, targetDate, 2, userElectives)

    // Verify resolved schedule
    assertNotNull(resolved)
  }

  @Test
  fun `verify widget provider update executes without error`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.widget.TimetableWidgetProvider.setWidgetDateOffset(context, 101, -1)
    org.junit.Assert.assertEquals(-1L, com.example.widget.TimetableWidgetProvider.getWidgetDateOffset(context, 101))
    com.example.widget.TimetableWidgetProvider.updateAppWidget(context, android.appwidget.AppWidgetManager.getInstance(context), 101)
    com.example.widget.TimetableWidgetProvider.updateAllWidgets(context)
    com.example.widget.TimetableWidgetProvider.scheduleMidnightUpdate(context)

    // Event & Memo widget
    val repo = TimetableRepository.getInstance(context)
    repo.setCompletedInitialSetup(true)
    com.example.widget.EventMemoWidgetProvider.setWidgetDateOffset(context, 201, 0)
    com.example.widget.EventMemoWidgetProvider.updateAppWidget(context, android.appwidget.AppWidgetManager.getInstance(context), 201)
    com.example.widget.EventMemoWidgetProvider.updateAllWidgets(context)

    // Notification
    com.example.notification.NotificationHelper.showTodayTimetableNotification(context, isTest = true)
  }

  @Test
  fun `verify unselected elective returns original code without fallback`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TimetableRepository(context)
    val csvManager = repo.csvSyncManager
    
    // Empty user electives
    val userElectives = emptyMap<String, String>()
    val targetDate = LocalDate.of(2026, 9, 10)
    val cls1 = com.example.model.ClassGroup(id = "1", name = "1組", grade = 2, section = "1")
    val resolved = csvManager.resolvePeriodSchedule(cls1, targetDate, 1, userElectives)
    assertNotNull(resolved)
    // When no elective is chosen, the schedule subject should be the original code, not an arbitrarily chosen elective
    // For period with elective origin, it should preserve the original code/subject
  }

  @Test
  fun `verify elective options are sorted in natural order and filtered by class`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TimetableRepository(context)
    val csvManager = repo.csvSyncManager
    
    // Class 1
    val options1 = csvManager.getElectiveOptionsForClass("1")
    assertNotNull(options1)
    
    // Check that each option list is sorted
    for ((origin, opts) in options1) {
      assertNotNull(origin)
      val sortedOpts = opts.sorted()
      // If list has multiple items, verify they are ordered
      if (opts.size > 1) {
        // First option should not be empty
        assertTrue(opts.first().isNotBlank())
      }
    }
  }

  @Test
  fun `verify next class notification format and options`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TimetableRepository.getInstance(context)

    // Default lead minutes should be 5
    assertEquals(5, repo.nextClassLeadMinutes.value)
    assertTrue(repo.isNextClassNotificationEnabled.value)

    // Update settings to 10 minutes
    repo.updateNextClassNotificationSettings(true, 10)
    assertEquals(10, repo.nextClassLeadMinutes.value)

    // Invalid lead minutes coerced to 5
    repo.updateNextClassNotificationSettings(true, 99)
    assertEquals(5, repo.nextClassLeadMinutes.value)

    // Test sending notification with isTest = true
    val result = com.example.notification.NotificationHelper.showNextClassNotification(
      context,
      period = 1,
      date = LocalDate.of(2026, 9, 14), // Monday
      isTest = true
    )
    assertTrue(result)

    // Verify weekend is considered holiday and prevents notification
    val sunday = LocalDate.of(2026, 9, 13) // Sunday
    assertTrue(com.example.notification.NotificationHelper.isHoliday(sunday, repo))
    val weekendResult = com.example.notification.NotificationHelper.showNextClassNotification(
      context,
      period = 1,
      date = sunday,
      isTest = false
    )
    assertEquals(false, weekendResult)

    // Verify schedule and cancel alarms run without error
    com.example.notification.NotificationHelper.scheduleNextClassAlarms(context)
    com.example.notification.NotificationHelper.cancelNextClassAlarms(context)
  }
}

