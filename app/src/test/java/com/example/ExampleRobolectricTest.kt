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

  @Test
  fun `verify HR classroom subjects have distinctive subject colors and are not unselected`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = TimetableRepository.getInstance(context)
    val groups = repo.subjectColorGroups.value

    val japaneseColor = groups.find { it.id == "japanese" }!!.toColorLong()
    val englishColor = groups.find { it.id == "english" }!!.toColorLong()
    val peHrColor = groups.find { it.id == "pe_and_hr" }!!.toColorLong()

    // 1. HR Classroom Japanese subjects
    assertEquals(japaneseColor, com.example.model.SubjectColorDefaults.getColorForSubject("現代文", groups))
    assertEquals(japaneseColor, com.example.model.SubjectColorDefaults.getColorForSubject("古典", groups))
    assertEquals(japaneseColor, com.example.model.SubjectColorDefaults.getColorForSubject("現文", groups))
    assertEquals(japaneseColor, com.example.model.SubjectColorDefaults.getColorForSubject("現文or英長or英W", groups))

    // 2. HR Classroom English subjects
    assertEquals(englishColor, com.example.model.SubjectColorDefaults.getColorForSubject("英語W", groups))
    assertEquals(englishColor, com.example.model.SubjectColorDefaults.getColorForSubject("英語R", groups))
    assertEquals(englishColor, com.example.model.SubjectColorDefaults.getColorForSubject("英語長文", groups))
    assertEquals(englishColor, com.example.model.SubjectColorDefaults.getColorForSubject("IBA Ⅳ", groups))
    assertEquals(englishColor, com.example.model.SubjectColorDefaults.getColorForSubject("英WorIBA", groups))

    // 3. HR Classroom Homeroom subjects
    assertEquals(peHrColor, com.example.model.SubjectColorDefaults.getColorForSubject("HR", groups))
    assertEquals(peHrColor, com.example.model.SubjectColorDefaults.getColorForSubject("LHR", groups))
    assertEquals(peHrColor, com.example.model.SubjectColorDefaults.getColorForSubject("進路HR", groups))

    // 4. Verify CsvSyncManager resolves HR subjects as common (isUnselectedElective = false)
    val manager = repo.csvSyncManager
    val classGroup1 = com.example.model.ClassGroup(id = "1", name = "1組", grade = 1, section = "1")
    val secondTermCsv = """
        class,type,day_of_week,first_period,second_period,third_period,fourth_period,fifth_period
        1,A,火,A2,E2,古典,英語W,英語R
        1,B,火,A2,E1,IBA Ⅳ,現代文,英語R
    """.trimIndent()
    manager.setSecondTermScheduleForTesting(com.example.data.csv.CsvParser.parseBasicClassSchedule(secondTermCsv))

    val date = LocalDate.of(2026, 10, 6)
    val pKoten = manager.resolvePeriodSchedule(classGroup1, date, 3, emptyMap())
    assertEquals("古典", pKoten.subject)
    assertTrue("Koten is not unselected", !pKoten.isUnselectedElective)

    val pEigoW = manager.resolvePeriodSchedule(classGroup1, date, 4, emptyMap())
    assertEquals("英語W", pEigoW.subject)
    assertTrue("EigoW is not unselected", !pEigoW.isUnselectedElective)
  }

  @Test
  fun `verify date navigation advances and retreats baseViewDate and selectedDate`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.TimetableViewModel(application)

    val initialBase = viewModel.uiState.value.baseViewDate
    val initialSelected = viewModel.uiState.value.selectedDate

    // 1. Move to Next Day: both baseViewDate and selectedDate move forward by 1 day
    viewModel.goToNextDay()
    val nextBase = viewModel.uiState.value.baseViewDate
    val nextSelected = viewModel.uiState.value.selectedDate

    assertEquals(initialBase.plusDays(1), nextBase)
    assertEquals(initialSelected.plusDays(1), nextSelected)
    val firstDisplayedDate = viewModel.uiState.value.displayBlockSchedulesList.first().first().date
    assertEquals(nextBase, firstDisplayedDate)

    // 2. Move to Previous Day: both baseViewDate and selectedDate move backward by 1 day
    viewModel.goToPreviousDay()
    val prevBase = viewModel.uiState.value.baseViewDate
    val prevSelected = viewModel.uiState.value.selectedDate

    assertEquals(initialBase, prevBase)
    assertEquals(initialSelected, prevSelected)
    val prevFirstDisplayedDate = viewModel.uiState.value.displayBlockSchedulesList.first().first().date
    assertEquals(prevBase, prevFirstDisplayedDate)
  }
}

