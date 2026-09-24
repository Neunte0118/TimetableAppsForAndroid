package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.TimetableRepository
import com.example.data.csv.AppUpdateInfo
import com.example.data.csv.CsvSyncStatus
import com.example.data.csv.CsvUrlsConfig
import com.example.data.csv.UpdateHistoryRow
import com.example.model.*
import com.example.notification.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class OnboardingStep {
    NONE,
    TERMS,           // 1. 利用規約モーダル
    CLASS_SELECT,    // 2. クラス (1-9) 選択モーダル
    DATA_LOADING,    // 3. データロード中モーダル
    ELECTIVES_SELECT // 4. 選択科目モーダル
}

data class TimetableUiState(
    val currentDate: LocalDate = LocalDate.now(),
    val baseViewDate: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val columnCount: Int = 5,
    val tableDisplayCount: Int = 1, // テーブルの表示数 (1〜3, デフォルト1)
    val liveTimeString: String = "",
    val viewMode: TimetableViewMode = TimetableViewMode.TIMETABLE,
    val cellColorMode: CellColorMode = CellColorMode.UNIFORM,
    // 教科テキスト色分け設定 (グループHSV対応, デフォルトOFF)
    val isSubjectColorEnabled: Boolean = false,
    val subjectColorGroups: List<SubjectColorGroup> = SubjectColorDefaults.defaultGroups,
    // 後方互換
    val subjectColors: Map<String, Long> = SubjectColorDefaults.defaultSubjectColors,
    // 時間割変更ハイライト設定
    val isHighlightChangedPeriods: Boolean = true,
    // フォントサイズ設定
    val timetableFontSize: TimetableFontSize = TimetableFontSize.MEDIUM,
    val selectedClass: ClassGroup = DefaultSchoolData.classes.first(),
    // 選択科目 (origin -> 選択肢群 & ユーザーの選択)
    val electiveOptionsForClass: Map<String, List<String>> = emptyMap(),
    val userElectiveSelections: Map<String, String> = emptyMap(),
    // 過去ブロック (基準日の前 columnCount 日間)
    val pastBlockSchedules: List<DaySchedule> = emptyList(),
    // 現在ブロック (基準日からの columnCount 日間)
    val currentBlockSchedules: List<DaySchedule> = emptyList(),
    // 複数テーブル表示用のブロックリスト (tableDisplayCount 個のテーブルブロック)
    val displayBlockSchedulesList: List<List<DaySchedule>> = emptyList(),
    // 選択された日付の行事(読み取り専用)とメモ
    val selectedDateEvent: String = "",
    val selectedDateMemo: String = "",
    val isDarkTheme: Boolean = false,
    // 毎日の時間割通知
    val isDailyNotificationEnabled: Boolean = true,
    val notificationHour: Int = 7,
    val notificationMinute: Int = 0,
    // 次の授業の事前通知
    val isNextClassNotificationEnabled: Boolean = true,
    val nextClassLeadMinutes: Int = 5,
    // 検索機能
    val showSearchDialog: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResultItem> = emptyList(),
    // 行事一覧・メモ一覧
    val showAllEventsDialog: Boolean = false,
    val showAllMemosDialog: Boolean = false,
    val allEventsList: List<SearchResultItem> = emptyList(),
    val allMemosList: List<SearchResultItem> = emptyList(),
    // 教科カラー設定ダイアログ
    val showSubjectColorsDialog: Boolean = false,
    val allAvailableSubjects: List<String> = emptyList(),
    // 文字サイズ設定ダイアログ
    val showFontSizeDialog: Boolean = false,
    // 開発者モード
    val isDeveloperMode: Boolean = false,
    // CSV Data sync
    val csvUrlsConfig: CsvUrlsConfig = CsvUrlsConfig(),
    val csvSyncStatus: CsvSyncStatus = CsvSyncStatus(),
    val updateHistories: List<UpdateHistoryRow> = emptyList(),
    // Onboarding modal flow
    val onboardingStep: OnboardingStep = OnboardingStep.NONE,
    // Active Dialogs
    val showClassDialog: Boolean = false,
    val showElectivesDialog: Boolean = false,
    val showColumnCountDialog: Boolean = false,
    val showTableDisplayCountDialog: Boolean = false,
    val showDialDatePickerDialog: Boolean = false,
    val showNotificationSettingsDialog: Boolean = false,
    val showCsvSettingsDialog: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val showHelpDialog: Boolean = false,
    val showTermsDialog: Boolean = false,
    val showOtherMenuSheet: Boolean = false,
    val showAppUpdateDialog: Boolean = false,
    val pendingAppUpdate: AppUpdateInfo? = null,
    val externalUrlToOpen: String? = null,
    val infoMessage: String? = null
)

class TimetableViewModel(application: Application) : AndroidViewModel(application) {
    val repository = TimetableRepository.getInstance(application)
    private val sharedPrefs = application.getSharedPreferences("timetable_settings", Context.MODE_PRIVATE)

    private val isSystemDarkTheme: Boolean =
        (application.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

    private val initialDarkTheme: Boolean =
        if (sharedPrefs.contains("dark_theme")) {
            sharedPrefs.getBoolean("dark_theme", false)
        } else {
            isSystemDarkTheme
        }

    private val _uiState = MutableStateFlow(
        TimetableUiState(
            isDarkTheme = initialDarkTheme,
            columnCount = repository.columnCount.value,
            tableDisplayCount = repository.tableDisplayCount.value,
            cellColorMode = repository.cellColorMode.value,
            isSubjectColorEnabled = repository.isSubjectColorEnabled.value,
            subjectColorGroups = repository.subjectColorGroups.value,
            isHighlightChangedPeriods = repository.isHighlightChangedPeriods.value,
            timetableFontSize = repository.timetableFontSize.value,
            isDailyNotificationEnabled = repository.isDailyNotificationEnabled.value,
            notificationHour = repository.notificationHour.value,
            notificationMinute = repository.notificationMinute.value,
            isNextClassNotificationEnabled = repository.isNextClassNotificationEnabled.value,
            nextClassLeadMinutes = repository.nextClassLeadMinutes.value,
            csvUrlsConfig = repository.csvUrlsConfig.value,
            csvSyncStatus = repository.csvSyncStatus.value,
            onboardingStep = if (!repository.hasCompletedInitialSetup()) OnboardingStep.TERMS else OnboardingStep.NONE,
            updateHistories = repository.getUpdateHistories()
        )
    )
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    init {
        // Collect repository states
        viewModelScope.launch {
            combine(
                repository.selectedClass,
                repository.columnCount,
                repository.tableDisplayCount,
                repository.isDeveloperMode,
                repository.cellColorMode,
                repository.isDailyNotificationEnabled,
                repository.notificationHour,
                repository.notificationMinute
            ) { values ->
                values
            }.collect { values ->
                val selectedClass = values[0] as ClassGroup
                val columnCount = values[1] as Int
                val tableDisplayCount = values[2] as Int
                val isDev = values[3] as Boolean
                val colorMode = values[4] as CellColorMode
                val notifEnabled = values[5] as Boolean
                val notifHour = values[6] as Int
                val notifMin = values[7] as Int

                val options = repository.csvSyncManager.getElectiveOptionsForClass(selectedClass.id)
                val userChoices = repository.getElectiveSelectionsForClass(selectedClass.id)
                _uiState.update { current ->
                    current.copy(
                        selectedClass = selectedClass,
                        columnCount = columnCount,
                        tableDisplayCount = tableDisplayCount,
                        isDeveloperMode = isDev,
                        cellColorMode = colorMode,
                        isDailyNotificationEnabled = notifEnabled,
                        notificationHour = notifHour,
                        notificationMinute = notifMin,
                        electiveOptionsForClass = options,
                        userElectiveSelections = userChoices
                    )
                }
                refreshSchedules()
            }
        }

        // Collect subject color groups and other preferences
        viewModelScope.launch {
            combine(
                repository.isSubjectColorEnabled,
                repository.subjectColorGroups,
                repository.isHighlightChangedPeriods,
                repository.timetableFontSize
            ) { enabled, groups, highlight, fontSize ->
                _uiState.update {
                    it.copy(
                        isSubjectColorEnabled = enabled,
                        subjectColorGroups = groups,
                        isHighlightChangedPeriods = highlight,
                        timetableFontSize = fontSize
                    )
                }
            }.collect()
        }

        // Collect next class notification settings
        viewModelScope.launch {
            combine(
                repository.isNextClassNotificationEnabled,
                repository.nextClassLeadMinutes
            ) { enabled, lead ->
                enabled to lead
            }.collect { (enabled, lead) ->
                _uiState.update {
                    it.copy(
                        isNextClassNotificationEnabled = enabled,
                        nextClassLeadMinutes = lead
                    )
                }
            }
        }

        // Collect CSV configs and status
        viewModelScope.launch {
            repository.csvUrlsConfig.collect { config ->
                _uiState.update { it.copy(csvUrlsConfig = config) }
            }
        }
        viewModelScope.launch {
            repository.csvSyncStatus.collect { status ->
                val selectedClass = _uiState.value.selectedClass
                val options = repository.csvSyncManager.getElectiveOptionsForClass(selectedClass.id)
                val userChoices = repository.getElectiveSelectionsForClass(selectedClass.id)
                val currentStep = _uiState.value.onboardingStep
                val nextStep = if (currentStep == OnboardingStep.DATA_LOADING && !status.isSyncing) {
                    OnboardingStep.ELECTIVES_SELECT
                } else {
                    currentStep
                }
                _uiState.update {
                    it.copy(
                        csvSyncStatus = status,
                        updateHistories = repository.getUpdateHistories(),
                        electiveOptionsForClass = options,
                        userElectiveSelections = userChoices,
                        onboardingStep = nextStep
                    )
                }
                if (!status.isSyncing) {
                    checkForAppUpdate()
                }
                refreshSchedules()
            }
        }

        // Live Clock updates every second
        viewModelScope.launch {
            val formatter = DateTimeFormatter.ofPattern("M月d日(E) HH:mm", Locale.JAPANESE)
            while (true) {
                val now = LocalDateTime.now()
                _uiState.update {
                    it.copy(
                        currentDate = now.toLocalDate(),
                        liveTimeString = now.format(formatter)
                    )
                }
                delay(1000)
            }
        }

        // Sync CSV at start
        viewModelScope.launch {
            repository.syncCsvData()
        }

        checkForAppUpdate()

        refreshSchedules()
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadSelectedDateEventAndMemo(date)
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(
                viewMode = if (it.viewMode == TimetableViewMode.TIMETABLE)
                    TimetableViewMode.MOVING_CLASSROOM
                else
                    TimetableViewMode.TIMETABLE
            )
        }
    }

    fun setCellColorMode(mode: CellColorMode) {
        repository.setCellColorMode(mode)
        _uiState.update { it.copy(cellColorMode = mode) }
    }

    // テーブル表示数設定 (1〜3)
    fun setTableDisplayCount(count: Int) {
        repository.setTableDisplayCount(count)
        _uiState.update { it.copy(tableDisplayCount = count) }
        refreshSchedules()
    }

    // 教科グループ（HSV）色分け設定
    fun setSubjectColorEnabled(enabled: Boolean) {
        repository.setSubjectColorEnabled(enabled)
        _uiState.update { it.copy(isSubjectColorEnabled = enabled) }
    }

    fun updateSubjectGroupHsv(groupId: String, hue: Float, saturation: Float, value: Float) {
        repository.updateSubjectGroupHsv(groupId, hue, saturation, value)
        _uiState.update { it.copy(subjectColorGroups = repository.subjectColorGroups.value) }
    }

    fun updateSubjectGroupName(groupId: String, newName: String) {
        repository.updateSubjectGroupName(groupId, newName)
        _uiState.update { it.copy(subjectColorGroups = repository.subjectColorGroups.value) }
    }

    fun addSubjectGroup(name: String, hue: Float = 180f, saturation: Float = 0.8f, value: Float = 0.85f): SubjectColorGroup {
        val newGrp = repository.addSubjectGroup(name, hue, saturation, value)
        _uiState.update { it.copy(subjectColorGroups = repository.subjectColorGroups.value) }
        return newGrp
    }

    fun deleteSubjectGroup(groupId: String) {
        repository.deleteSubjectGroup(groupId)
        _uiState.update { it.copy(subjectColorGroups = repository.subjectColorGroups.value) }
    }

    fun assignSubjectToGroup(subjectName: String, targetGroupId: String) {
        repository.assignSubjectToGroup(subjectName, targetGroupId)
        _uiState.update { it.copy(subjectColorGroups = repository.subjectColorGroups.value) }
    }

    fun removeSubjectFromGroup(subjectName: String, groupId: String) {
        repository.removeSubjectFromGroup(subjectName, groupId)
        _uiState.update { it.copy(subjectColorGroups = repository.subjectColorGroups.value) }
    }

    fun resetSubjectColorGroups() {
        repository.resetSubjectColorGroups()
        _uiState.update { it.copy(subjectColorGroups = repository.subjectColorGroups.value) }
    }

    fun openSubjectColorsDialog() {
        val subjects = repository.getAllKnownSubjects()
        _uiState.update { it.copy(showSubjectColorsDialog = true, allAvailableSubjects = subjects, showOtherMenuSheet = false) }
    }

    fun closeSubjectColorsDialog() {
        _uiState.update { it.copy(showSubjectColorsDialog = false) }
    }

    // 時間割変更ハイライト設定
    fun setHighlightChangedPeriods(enabled: Boolean) {
        repository.setHighlightChangedPeriods(enabled)
        _uiState.update { it.copy(isHighlightChangedPeriods = enabled) }
    }

    // フォントサイズ設定
    fun setTimetableFontSize(size: TimetableFontSize) {
        repository.setTimetableFontSize(size)
        _uiState.update { it.copy(timetableFontSize = size, showFontSizeDialog = false) }
    }

    fun openFontSizeDialog() {
        _uiState.update { it.copy(showFontSizeDialog = true, showOtherMenuSheet = false) }
    }

    fun closeFontSizeDialog() {
        _uiState.update { it.copy(showFontSizeDialog = false) }
    }

    // 行事一覧・メモ一覧ダイアログ
    fun openAllEventsDialog() {
        val events = repository.getAllEvents(_uiState.value.selectedClass)
        _uiState.update { it.copy(showAllEventsDialog = true, allEventsList = events, showOtherMenuSheet = false) }
    }

    fun closeAllEventsDialog() {
        _uiState.update { it.copy(showAllEventsDialog = false) }
    }

    fun openAllMemosDialog() {
        val memos = repository.getAllMemos(_uiState.value.selectedClass.id)
        _uiState.update { it.copy(showAllMemosDialog = true, allMemosList = memos, showOtherMenuSheet = false) }
    }

    fun closeAllMemosDialog() {
        _uiState.update { it.copy(showAllMemosDialog = false) }
    }

    fun toggleDarkTheme() {
        val newDark = !_uiState.value.isDarkTheme
        sharedPrefs.edit().putBoolean("dark_theme", newDark).apply()
        _uiState.update { it.copy(isDarkTheme = newDark) }
        try {
            com.example.widget.TimetableWidgetProvider.updateAllWidgets(getApplication())
            com.example.widget.EventMemoWidgetProvider.updateAllWidgets(getApplication())
        } catch (_: Exception) {}
    }

    fun toggleDeveloperMode() {
        val newDev = !_uiState.value.isDeveloperMode
        repository.setDeveloperMode(newDev)
        _uiState.update { it.copy(isDeveloperMode = newDev) }
    }

    fun setColumnCount(count: Int) {
        repository.setColumnCount(count)
        _uiState.update { it.copy(columnCount = count, showColumnCountDialog = false) }
        refreshSchedules()
    }

    fun updateDailyNotificationSettings(enabled: Boolean, hour: Int, minute: Int) {
        repository.updateDailyNotificationSettings(enabled, hour, minute)
        _uiState.update {
            it.copy(
                isDailyNotificationEnabled = enabled,
                notificationHour = hour,
                notificationMinute = minute,
                infoMessage = if (enabled) "毎朝 ${String.format("%02d:%02d", hour, minute)} に今日の時間割を通知します" else "通知を無効にしました"
            )
        }
    }

    fun sendTestNotification() {
        repository.testSendNotificationNow()
        _uiState.update { it.copy(infoMessage = "テスト通知を送信しました") }
    }

    fun updateNextClassNotificationSettings(enabled: Boolean, leadMinutes: Int) {
        repository.updateNextClassNotificationSettings(enabled, leadMinutes)
        _uiState.update {
            it.copy(
                isNextClassNotificationEnabled = enabled,
                nextClassLeadMinutes = leadMinutes,
                infoMessage = if (enabled) "次の授業開始の ${leadMinutes}分前に通知します" else "次の授業通知を無効にしました"
            )
        }
    }

    fun sendTestNextClassNotification() {
        val success = NotificationHelper.showNextClassNotification(
            getApplication(),
            1,
            LocalDate.now(),
            isTest = true
        )
        if (!success) {
            val schedule = repository.getDaySchedule(uiState.value.selectedClass, LocalDate.now(), LocalDate.now())
            val firstActive = schedule.periods.firstOrNull { it.subject.isNotBlank() }?.period ?: 1
            NotificationHelper.showNextClassNotification(
                getApplication(),
                firstActive,
                LocalDate.now(),
                isTest = true
            )
        }
        _uiState.update { it.copy(infoMessage = "次の授業のテスト通知を送信しました") }
    }

    // 検索機能
    fun openSearchDialog() {
        val query = _uiState.value.searchQuery
        val results = repository.searchScheduleAndNotes(_uiState.value.selectedClass, query)
        _uiState.update { it.copy(showSearchDialog = true, searchResults = results) }
    }

    fun closeSearchDialog() {
        _uiState.update { it.copy(showSearchDialog = false) }
    }

    fun onSearchQueryChanged(query: String) {
        val results = repository.searchScheduleAndNotes(_uiState.value.selectedClass, query)
        _uiState.update { it.copy(searchQuery = query, searchResults = results) }
    }

    fun jumpToSearchResult(date: LocalDate) {
        _uiState.update {
            it.copy(
                baseViewDate = date,
                selectedDate = date,
                showSearchDialog = false
            )
        }
        refreshSchedules()
    }

    fun goToPreviousDay() {
        val newDate = _uiState.value.baseViewDate.minusDays(1)
        _uiState.update {
            it.copy(baseViewDate = newDate, selectedDate = newDate)
        }
        refreshSchedules()
    }

    fun resetToToday() {
        val today = _uiState.value.currentDate
        _uiState.update {
            it.copy(baseViewDate = today, selectedDate = today)
        }
        refreshSchedules()
    }

    fun goToNextDay() {
        val newDate = _uiState.value.baseViewDate.plusDays(1)
        _uiState.update {
            it.copy(baseViewDate = newDate, selectedDate = newDate)
        }
        refreshSchedules()
    }

    fun jumpToDate(month: Int, day: Int) {
        val currentYear = _uiState.value.currentDate.year
        try {
            val maxDayInMonth = java.time.YearMonth.of(currentYear, month).lengthOfMonth()
            val validDay = day.coerceIn(1, maxDayInMonth)
            val newDate = LocalDate.of(currentYear, month, validDay)
            _uiState.update { it.copy(baseViewDate = newDate, selectedDate = newDate) }
            refreshSchedules()
        } catch (e: Exception) {
            // ignore invalid date
        }
    }

    fun selectClass(classGroup: ClassGroup) {
        repository.setSelectedClass(classGroup)
        val options = repository.csvSyncManager.getElectiveOptionsForClass(classGroup.id)
        val userChoices = repository.getElectiveSelectionsForClass(classGroup.id)
        _uiState.update {
            it.copy(
                showClassDialog = false,
                electiveOptionsForClass = options,
                userElectiveSelections = userChoices
            )
        }
        refreshSchedules()
    }

    // 開発者メニュー
    fun openCsvSettingsDialog() {
        _uiState.update { it.copy(showCsvSettingsDialog = true, showOtherMenuSheet = false) }
    }

    fun closeCsvSettingsDialog() {
        _uiState.update { it.copy(showCsvSettingsDialog = false) }
        checkForAppUpdate()
    }

    fun saveCsvUrls(config: CsvUrlsConfig) {
        repository.saveCsvUrls(config)
        _uiState.update { it.copy(infoMessage = "URL設定を保存しました") }
    }

    fun resetToDefaultCsvUrls() {
        val defaultConfig = CsvUrlsConfig()
        repository.saveCsvUrls(defaultConfig)
        _uiState.update { it.copy(infoMessage = "初期URLに復元しました") }
    }

    fun clearAllCsvCache() {
        repository.clearAllCsvCache()
        val selectedClass = _uiState.value.selectedClass
        val options = repository.csvSyncManager.getElectiveOptionsForClass(selectedClass.id)
        val userChoices = repository.getElectiveSelectionsForClass(selectedClass.id)
        _uiState.update {
            it.copy(
                updateHistories = emptyList(),
                electiveOptionsForClass = options,
                userElectiveSelections = userChoices,
                infoMessage = "CSVキャッシュを全消去しました"
            )
        }
        com.example.widget.TimetableWidgetProvider.updateAllWidgets(getApplication())
        com.example.widget.EventMemoWidgetProvider.updateAllWidgets(getApplication())
        refreshSchedules()
    }

    fun clearAllMemos() {
        repository.clearAllMemosForClass(_uiState.value.selectedClass.id)
        _uiState.update {
            it.copy(
                selectedDateMemo = "",
                infoMessage = "保存されていたメモをすべて消去しました"
            )
        }
        com.example.widget.TimetableWidgetProvider.updateAllWidgets(getApplication())
        com.example.widget.EventMemoWidgetProvider.updateAllWidgets(getApplication())
        refreshSchedules()
    }

    fun syncCsvData() {
        viewModelScope.launch {
            val result = repository.syncCsvData()
            val selectedClass = _uiState.value.selectedClass
            val options = repository.csvSyncManager.getElectiveOptionsForClass(selectedClass.id)
            val userChoices = repository.getElectiveSelectionsForClass(selectedClass.id)
            val infoMsg = if (result.isSuccess) {
                "時間割データを最新に更新しました"
            } else {
                "インターネットに接続されていません。事前に取得したデータを表示しています"
            }
            _uiState.update {
                it.copy(
                    updateHistories = repository.getUpdateHistories(),
                    electiveOptionsForClass = options,
                    userElectiveSelections = userChoices,
                    infoMessage = infoMsg
                )
            }
            com.example.widget.TimetableWidgetProvider.updateAllWidgets(getApplication())
            com.example.widget.EventMemoWidgetProvider.updateAllWidgets(getApplication())
            refreshSchedules()
            checkForAppUpdate()
        }
    }

    // Onboarding Flow Transitions
    fun agreeTermsAndProceedToClassSelect() {
        repository.setAgreedTerms(true)
        _uiState.update { it.copy(onboardingStep = OnboardingStep.CLASS_SELECT) }
    }

    fun selectClassAndProceedToElectives(classGroup: ClassGroup) {
        repository.setSelectedClass(classGroup)
        repository.setClassSelected(true)
        val isDataLoaded = repository.csvSyncManager.basicClassSchedule.isNotEmpty() ||
                repository.csvSyncManager.electives.isNotEmpty()
        val isSyncing = repository.csvSyncStatus.value.isSyncing

        if (!isDataLoaded || isSyncing) {
            // データロード完了を待ってから選択科目画面を表示
            _uiState.update {
                it.copy(
                    selectedClass = classGroup,
                    onboardingStep = OnboardingStep.DATA_LOADING
                )
            }
            if (!isSyncing) {
                viewModelScope.launch {
                    repository.syncCsvData()
                }
            }
        } else {
            val options = repository.csvSyncManager.getElectiveOptionsForClass(classGroup.id)
            val userChoices = repository.getElectiveSelectionsForClass(classGroup.id)
            _uiState.update {
                it.copy(
                    selectedClass = classGroup,
                    electiveOptionsForClass = options,
                    userElectiveSelections = userChoices,
                    onboardingStep = OnboardingStep.ELECTIVES_SELECT
                )
            }
        }
    }

    fun proceedToElectivesFromLoading() {
        val classGroup = _uiState.value.selectedClass
        val options = repository.csvSyncManager.getElectiveOptionsForClass(classGroup.id)
        val userChoices = repository.getElectiveSelectionsForClass(classGroup.id)
        _uiState.update {
            it.copy(
                electiveOptionsForClass = options,
                userElectiveSelections = userChoices,
                onboardingStep = OnboardingStep.ELECTIVES_SELECT
            )
        }
    }

    fun completeOnboarding() {
        repository.setCompletedInitialSetup(true)
        _uiState.update { it.copy(onboardingStep = OnboardingStep.NONE) }
        com.example.widget.TimetableWidgetProvider.updateAllWidgets(getApplication())
        com.example.widget.EventMemoWidgetProvider.updateAllWidgets(getApplication())
    }

    fun updateElectiveChoice(origin: String, elective: String) {
        val classId = _uiState.value.selectedClass.id
        repository.saveElectiveChoice(classId, origin, elective)
        val updatedChoices = repository.getElectiveSelectionsForClass(classId)
        _uiState.update { it.copy(userElectiveSelections = updatedChoices) }
        refreshSchedules()
    }

    fun saveSelectedDateMemo(memo: String) {
        val date = _uiState.value.selectedDate
        val classId = _uiState.value.selectedClass.id
        repository.saveMemo(classId, date, memo)
        _uiState.update { it.copy(selectedDateMemo = memo) }
        refreshSchedules()
    }

    fun openColumnCountDialog() {
        _uiState.update { it.copy(showColumnCountDialog = true, showOtherMenuSheet = false) }
    }

    fun closeColumnCountDialog() {
        _uiState.update { it.copy(showColumnCountDialog = false) }
    }

    fun openTableDisplayCountDialog() {
        _uiState.update { it.copy(showTableDisplayCountDialog = true, showOtherMenuSheet = false) }
    }

    fun closeTableDisplayCountDialog() {
        _uiState.update { it.copy(showTableDisplayCountDialog = false) }
    }

    fun openDialDatePickerDialog() {
        _uiState.update { it.copy(showDialDatePickerDialog = true) }
    }

    fun closeDialDatePickerDialog() {
        _uiState.update { it.copy(showDialDatePickerDialog = false) }
    }

    fun selectDialDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                baseViewDate = date,
                selectedDate = date,
                showDialDatePickerDialog = false
            )
        }
        loadSelectedDateEventAndMemo(date)
        refreshSchedules()
    }

    fun openNotificationSettingsDialog() {
        _uiState.update { it.copy(showNotificationSettingsDialog = true, showOtherMenuSheet = false) }
    }

    fun closeNotificationSettingsDialog() {
        _uiState.update { it.copy(showNotificationSettingsDialog = false) }
    }

    fun openClassDialog() {
        _uiState.update { it.copy(showClassDialog = true, showOtherMenuSheet = false) }
    }

    fun closeClassDialog() {
        _uiState.update { it.copy(showClassDialog = false) }
    }

    fun openElectivesDialog() {
        val classId = _uiState.value.selectedClass.id
        val options = repository.csvSyncManager.getElectiveOptionsForClass(classId)
        val choices = repository.getElectiveSelectionsForClass(classId)
        _uiState.update {
            it.copy(
                showElectivesDialog = true,
                showOtherMenuSheet = false,
                electiveOptionsForClass = options,
                userElectiveSelections = choices
            )
        }
    }

    fun closeElectivesDialog() {
        _uiState.update { it.copy(showElectivesDialog = false) }
    }

    fun openHistoryDialog() {
        _uiState.update { it.copy(showHistoryDialog = true, showOtherMenuSheet = false) }
    }

    fun closeHistoryDialog() {
        _uiState.update { it.copy(showHistoryDialog = false) }
    }

    fun openHelpDialog() {
        _uiState.update { it.copy(showHelpDialog = true, showOtherMenuSheet = false) }
    }

    fun closeHelpDialog() {
        _uiState.update { it.copy(showHelpDialog = false) }
    }

    fun openTermsDialog() {
        _uiState.update { it.copy(showTermsDialog = true, showOtherMenuSheet = false) }
    }

    fun closeTermsDialog() {
        _uiState.update { it.copy(showTermsDialog = false) }
    }

    fun openOtherMenuSheet() {
        _uiState.update { it.copy(showOtherMenuSheet = true) }
    }

    fun closeOtherMenuSheet() {
        _uiState.update { it.copy(showOtherMenuSheet = false) }
    }

    fun openExternalReportLink() {
        val reportUrl = _uiState.value.csvUrlsConfig.reportUrl.ifBlank { "https://forms.gle/KiiEAds2vtjAmsZ97" }
        _uiState.update {
            it.copy(
                showOtherMenuSheet = false,
                externalUrlToOpen = reportUrl
            )
        }
    }

    fun openExternalTimetableChangeLink() {
        val changeUrl = _uiState.value.csvUrlsConfig.timetableChangeUrl.ifBlank { "https://docs.google.com/forms/d/e/1FAIpQLSfTOKMLJz896qfq7OKSv7TRwxxJxX4VIqXT4npLcGmqWNyBkg/viewform?usp=preview" }
        _uiState.update {
            it.copy(
                showOtherMenuSheet = false,
                externalUrlToOpen = changeUrl
            )
        }
    }

    fun clearExternalUrl() {
        _uiState.update { it.copy(externalUrlToOpen = null) }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun checkForAppUpdate(manual: Boolean = false) {
        val updateInfo = repository.getLatestAppUpdateInfo()
        val currentVersion = BuildConfig.VERSION_NAME.trim()

        if (updateInfo == null) {
            if (manual) {
                _uiState.update {
                    it.copy(infoMessage = "更新情報を取得できませんでした。時間割の更新（再同期）をお試しください。")
                }
            }
            return
        }

        if (updateInfo.isNewerThan(currentVersion)) {
            val ignoredVersion = repository.getIgnoredUpdateVersion()
            if (manual || ignoredVersion != updateInfo.version.trim()) {
                _uiState.update {
                    it.copy(
                        showAppUpdateDialog = true,
                        pendingAppUpdate = updateInfo
                    )
                }
            }
        } else if (manual) {
            _uiState.update {
                it.copy(infoMessage = "お使いのアプリ (v$currentVersion) は最新バージョンです")
            }
        }
    }

    fun dismissAppUpdateDialog(dontShowAgain: Boolean) {
        val currentPending = _uiState.value.pendingAppUpdate
        if (dontShowAgain && currentPending != null && currentPending.version.isNotBlank()) {
            repository.setIgnoredUpdateVersion(currentPending.version.trim())
        }
        _uiState.update {
            it.copy(
                showAppUpdateDialog = false,
                pendingAppUpdate = null
            )
        }
    }

    fun openUpdateDownloadLink(link: String) {
        val trimmed = link.trim()
        if (trimmed.isNotBlank()) {
            val fullUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
            _uiState.update {
                it.copy(
                    externalUrlToOpen = fullUrl
                )
            }
        }
    }

    private fun loadSelectedDateEventAndMemo(date: LocalDate) {
        val state = _uiState.value
        val schedule = repository.getDaySchedule(state.selectedClass, date, state.currentDate)
        _uiState.update {
            it.copy(
                selectedDateEvent = schedule.event,
                selectedDateMemo = schedule.memo
            )
        }
    }

    private fun refreshSchedules() {
        val state = _uiState.value
        val base = state.baseViewDate
        val today = state.currentDate
        val selectedClass = state.selectedClass
        val columns = state.columnCount
        val tableCount = state.tableDisplayCount.coerceIn(1, 3)

        // 過去ブロック: baseDate の直前 columns 日間 (-columns .. -1)
        val pastBlock = (columns downTo 1).map { offset ->
            val targetDate = base.minusDays(offset.toLong())
            repository.getDaySchedule(selectedClass, targetDate, today)
        }

        // 現在ブロック: baseDate から columns 日間 (0 .. columns - 1)
        val currentBlock = (0 until columns).map { offset ->
            val targetDate = base.plusDays(offset.toLong())
            repository.getDaySchedule(selectedClass, targetDate, today)
        }

        // 複数テーブル表示用のブロックリスト (tableCount 個)
        val displayBlocks = (0 until tableCount).map { tableIndex ->
            val startOffset = tableIndex * columns
            (0 until columns).map { colOffset ->
                val targetDate = base.plusDays((startOffset + colOffset).toLong())
                repository.getDaySchedule(selectedClass, targetDate, today)
            }
        }

        val selectedSched = repository.getDaySchedule(selectedClass, state.selectedDate, today)

        _uiState.update {
            it.copy(
                pastBlockSchedules = pastBlock,
                currentBlockSchedules = currentBlock,
                displayBlockSchedulesList = displayBlocks,
                selectedDateEvent = selectedSched.event,
                selectedDateMemo = selectedSched.memo
            )
        }
    }
}
