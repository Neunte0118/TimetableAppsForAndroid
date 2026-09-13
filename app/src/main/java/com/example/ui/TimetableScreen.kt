package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimetableViewMode
import com.example.ui.components.TimetableTable
import com.example.ui.dialogs.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Handle External URL intents
    LaunchedEffect(uiState.externalUrlToOpen) {
        uiState.externalUrlToOpen?.let { url ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "リンクを開けませんでした", Toast.LENGTH_SHORT).show()
            } finally {
                viewModel.clearExternalUrl()
            }
        }
    }

    // Handle Info Messages (Toast)
    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearInfoMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text(
                            text = "時間割",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )

                        if (uiState.isDeveloperMode) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "DEV",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Search button
                    IconButton(
                        onClick = { viewModel.openSearchDialog() },
                        modifier = Modifier.testTag("search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "行事・メモ・時間割の検索",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick Sync / CSV button
                    IconButton(
                        onClick = { viewModel.syncCsvData() },
                        modifier = Modifier.testTag("quick_sync_button")
                    ) {
                        if (uiState.csvSyncStatus.isSyncing) {
                            val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 2400, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "sync_angle"
                            )
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "同期中",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "CSVデータを同期",
                                tint = if (uiState.csvSyncStatus.lastSyncSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Class selector chip (1〜9組)
                    SuggestionChip(
                        onClick = { viewModel.openClassDialog() },
                        label = {
                            Text(
                                uiState.selectedClass.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("header_class_chip")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Fixed bottom controls: Navigation row + Bottom action bar
            FixedBottomControls(
                isDarkTheme = uiState.isDarkTheme,
                currentBaseDate = uiState.baseViewDate,
                onPrevious = { viewModel.goToPreviousDay() },
                onToday = { viewModel.resetToToday() },
                onNext = { viewModel.goToNextDay() },
                onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                onOpenAllEvents = { viewModel.openAllEventsDialog() },
                onOpenAllMemos = { viewModel.openAllMemosDialog() },
                onOpenDialDatePicker = { viewModel.openDialDatePickerDialog() },
                onOpenOtherMenu = { viewModel.openOtherMenuSheet() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Mode Switcher & Live Clock
            ModeSwitchAndDateBar(
                viewMode = uiState.viewMode,
                liveTimeString = uiState.liveTimeString,
                columnCount = uiState.columnCount,
                onToggleMode = { viewModel.toggleViewMode() }
            )

            // Scrollable Content: Display Tables (1 to 3 tables as configured) -> Selected Day's Event & Memo
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                val availableHeight = maxHeight
                val blocksToDisplay = if (uiState.displayBlockSchedulesList.isNotEmpty()) {
                    uiState.displayBlockSchedulesList
                } else {
                    listOf(uiState.currentBlockSchedules)
                }
                val tableCount = blocksToDisplay.size

                // Dynamic vertical sizing: when 1 or 2 tables are displayed, expand the cell height
                // and give generous space to the memo area so there is no awkward empty space at the bottom.
                val dynamicCellHeight = when (tableCount) {
                    1 -> {
                        if (availableHeight > 650.dp) 64.dp
                        else if (availableHeight > 550.dp) 58.dp
                        else 52.dp
                    }
                    2 -> {
                        if (availableHeight > 700.dp) 52.dp
                        else 46.dp
                    }
                    else -> 44.dp
                }

                val memoMinLines = when (tableCount) {
                    1 -> 4
                    2 -> 3
                    else -> 2
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp)
                ) {
                    itemsIndexed(
                        items = blocksToDisplay,
                        key = { index, _ -> "display_table_$index" }
                    ) { index, blockSchedules ->
                        TimetableTable(
                            daySchedules = blockSchedules,
                            selectedDate = uiState.selectedDate,
                            today = uiState.currentDate,
                            viewMode = uiState.viewMode,
                            cellColorMode = uiState.cellColorMode,
                            isSubjectColorEnabled = uiState.isSubjectColorEnabled,
                            subjectColorGroups = uiState.subjectColorGroups,
                            subjectColors = uiState.subjectColors,
                            isHighlightChangedPeriods = uiState.isHighlightChangedPeriods,
                            timetableFontSize = uiState.timetableFontSize,
                            onSelectDate = { date -> viewModel.selectDate(date) },
                            tableTitle = null,
                            cellHeight = dynamicCellHeight
                        )
                    }

                    // Selected Day's Event & Memo Section (Under all tables)
                    item(key = "selected_day_details") {
                        SelectedDayEventMemoSection(
                            selectedDate = uiState.selectedDate,
                            isToday = uiState.selectedDate == uiState.currentDate,
                            eventText = uiState.selectedDateEvent,
                            memoText = uiState.selectedDateMemo,
                            minMemoLines = memoMinLines,
                            onSaveMemo = { newMemo -> viewModel.saveSelectedDateMemo(newMemo) }
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // 初回訪問時 オンボーディングモーダルシーケンス
    // ==========================================
    when (uiState.onboardingStep) {
        OnboardingStep.TERMS -> {
            OnboardingTermsDialog(
                onAgree = { viewModel.agreeTermsAndProceedToClassSelect() }
            )
        }
        OnboardingStep.CLASS_SELECT -> {
            OnboardingClassDialog(
                classes = com.example.model.DefaultSchoolData.classes,
                currentSelected = uiState.selectedClass,
                onClassSelected = { selectedClass ->
                    viewModel.selectClassAndProceedToElectives(selectedClass)
                }
            )
        }
        OnboardingStep.DATA_LOADING -> {
            OnboardingDataLoadingDialog(
                className = uiState.selectedClass.name,
                isSyncing = uiState.csvSyncStatus.isSyncing,
                syncMessage = uiState.csvSyncStatus.lastSyncMessage,
                onRetry = { viewModel.syncCsvData() },
                onSkip = { viewModel.proceedToElectivesFromLoading() }
            )
        }
        OnboardingStep.ELECTIVES_SELECT -> {
            OnboardingElectivesDialog(
                className = uiState.selectedClass.name,
                electiveOptions = uiState.electiveOptionsForClass,
                userSelections = uiState.userElectiveSelections,
                onUpdateElective = { origin, elective -> viewModel.updateElectiveChoice(origin, elective) },
                onComplete = { viewModel.completeOnboarding() }
            )
        }
        OnboardingStep.NONE -> {
            // Normal state
        }
    }

    // ==========================================
    // 通常ダイアログ
    // ==========================================
    // 0. 検索ダイアログ
    if (uiState.showSearchDialog) {
        SearchDialog(
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            onQueryChanged = { query -> viewModel.onSearchQueryChanged(query) },
            onResultSelected = { date -> viewModel.jumpToSearchResult(date) },
            onDismiss = { viewModel.closeSearchDialog() }
        )
    }

    // 1. 列数変更ダイアログ
    if (uiState.showColumnCountDialog) {
        ColumnCountDialog(
            currentCount = uiState.columnCount,
            onSelectCount = { count: Int -> viewModel.setColumnCount(count) },
            onDismiss = { viewModel.closeColumnCountDialog() }
        )
    }

    // 1.5. テーブル表示数変更ダイアログ (1〜3)
    if (uiState.showTableDisplayCountDialog) {
        TableDisplayCountDialog(
            currentCount = uiState.tableDisplayCount,
            onSelectCount = { count: Int -> viewModel.setTableDisplayCount(count) },
            onDismiss = { viewModel.closeTableDisplayCountDialog() }
        )
    }

    // 2. クラス選択 (1〜9組)
    if (uiState.showClassDialog) {
        ClassSelectDialog(
            selectedClass = uiState.selectedClass,
            classes = com.example.model.DefaultSchoolData.classes,
            onDismiss = { viewModel.closeClassDialog() },
            onClassSelected = { classGroup -> viewModel.selectClass(classGroup) }
        )
    }

    // 3. 選択科目設定
    if (uiState.showElectivesDialog) {
        ElectivesDialog(
            className = uiState.selectedClass.name,
            electiveOptions = uiState.electiveOptionsForClass,
            userSelections = uiState.userElectiveSelections,
            onUpdateElective = { origin, elective -> viewModel.updateElectiveChoice(origin, elective) },
            onDismiss = { viewModel.closeElectivesDialog() }
        )
    }

    // 4. 更新履歴
    if (uiState.showHistoryDialog) {
        UpdateHistoryDialog(
            historyList = uiState.updateHistories,
            onDismiss = { viewModel.closeHistoryDialog() }
        )
    }

    // 5. 毎日の時間割通知設定 ダイアログ (一般設定)
    if (uiState.showNotificationSettingsDialog) {
        NotificationSettingsDialog(
            isNotificationEnabled = uiState.isDailyNotificationEnabled,
            notificationHour = uiState.notificationHour,
            notificationMinute = uiState.notificationMinute,
            onUpdateNotificationSettings = { enabled, h, m -> viewModel.updateDailyNotificationSettings(enabled, h, m) },
            onTestSendNotification = { viewModel.sendTestNotification() },
            onDismiss = { viewModel.closeNotificationSettingsDialog() }
        )
    }

    // 6. ダイヤル式 日付選択ダイアログ
    if (uiState.showDialDatePickerDialog) {
        DialDatePickerDialog(
            initialDate = uiState.baseViewDate,
            onDateSelected = { selectedDate ->
                viewModel.selectDialDate(selectedDate)
            },
            onDismiss = { viewModel.closeDialDatePickerDialog() }
        )
    }

    // 7. 開発者メニュー ダイアログ (開発者モード)
    if (uiState.showCsvSettingsDialog) {
        DeveloperMenuDialog(
            initialConfig = uiState.csvUrlsConfig,
            syncStatus = uiState.csvSyncStatus,
            onSaveAndSync = { config ->
                viewModel.saveCsvUrls(config)
                viewModel.syncCsvData()
            },
            onResetToDefaultUrls = { viewModel.resetToDefaultCsvUrls() },
            onClearAllCache = { viewModel.clearAllCsvCache() },
            onClearAllMemos = { viewModel.clearAllMemos() },
            onDismiss = { viewModel.closeCsvSettingsDialog() }
        )
    }

    // アプリ更新モーダル
    if (uiState.showAppUpdateDialog && uiState.pendingAppUpdate != null) {
        AppUpdateModalDialog(
            updateInfo = uiState.pendingAppUpdate!!,
            onDownloadClick = { link -> viewModel.openUpdateDownloadLink(link) },
            onDismiss = { dontShowAgain -> viewModel.dismissAppUpdateDialog(dontShowAgain) }
        )
    }

    // 8. ヘルプ
    if (uiState.showHelpDialog) {
        HelpDialog(
            onDismiss = { viewModel.closeHelpDialog() }
        )
    }

    // 9. 利用規約
    if (uiState.showTermsDialog) {
        TermsDialog(
            onDismiss = { viewModel.closeTermsDialog() }
        )
    }

    // 10. 行事一覧ダイアログ
    if (uiState.showAllEventsDialog) {
        AllEventsDialog(
            eventsList = uiState.allEventsList,
            onSelectDate = { date -> viewModel.jumpToSearchResult(date) },
            onDismiss = { viewModel.closeAllEventsDialog() }
        )
    }

    // 11. メモ一覧ダイアログ
    if (uiState.showAllMemosDialog) {
        AllMemosDialog(
            memosList = uiState.allMemosList,
            onSelectDate = { date -> viewModel.jumpToSearchResult(date) },
            onDismiss = { viewModel.closeAllMemosDialog() }
        )
    }

    // 12. 教科グループHSV色分け設定ダイアログ
    if (uiState.showSubjectColorsDialog) {
        SubjectColorsDialog(
            isEnabled = uiState.isSubjectColorEnabled,
            groups = uiState.subjectColorGroups,
            availableSubjects = uiState.allAvailableSubjects,
            onToggleEnabled = { enabled -> viewModel.setSubjectColorEnabled(enabled) },
            onUpdateGroupHsv = { groupId, h, s, v -> viewModel.updateSubjectGroupHsv(groupId, h, s, v) },
            onUpdateGroupName = { groupId, name -> viewModel.updateSubjectGroupName(groupId, name) },
            onAddGroup = { name, h, s, v -> viewModel.addSubjectGroup(name, h, s, v) },
            onDeleteGroup = { groupId -> viewModel.deleteSubjectGroup(groupId) },
            onAssignSubject = { subject, groupId -> viewModel.assignSubjectToGroup(subject, groupId) },
            onRemoveSubject = { subject, groupId -> viewModel.removeSubjectFromGroup(subject, groupId) },
            onResetGroups = { viewModel.resetSubjectColorGroups() },
            onDismiss = { viewModel.closeSubjectColorsDialog() }
        )
    }

    // 13. 文字サイズ設定ダイアログ
    if (uiState.showFontSizeDialog) {
        TimetableFontSizeDialog(
            currentSize = uiState.timetableFontSize,
            onSelectSize = { size -> viewModel.setTimetableFontSize(size) },
            onDismiss = { viewModel.closeFontSizeDialog() }
        )
    }

    // 14. その他メニュー
    if (uiState.showOtherMenuSheet) {
        OtherMenuModalSheet(
            isDeveloperMode = uiState.isDeveloperMode,
            isSubjectColorEnabled = uiState.isSubjectColorEnabled,
            isHighlightChangedPeriods = uiState.isHighlightChangedPeriods,
            currentFontSize = uiState.timetableFontSize,
            currentColumnCount = uiState.columnCount,
            currentTableDisplayCount = uiState.tableDisplayCount,
            onToggleDeveloperMode = { viewModel.toggleDeveloperMode() },
            onToggleHighlightChangedPeriods = { enabled -> viewModel.setHighlightChangedPeriods(enabled) },
            onDismiss = { viewModel.closeOtherMenuSheet() },
            onOpenSearch = {
                viewModel.closeOtherMenuSheet()
                viewModel.openSearchDialog()
            },
            onOpenAllEvents = {
                viewModel.closeOtherMenuSheet()
                viewModel.openAllEventsDialog()
            },
            onOpenAllMemos = {
                viewModel.closeOtherMenuSheet()
                viewModel.openAllMemosDialog()
            },
            onOpenTableDisplayCount = {
                viewModel.openTableDisplayCountDialog()
            },
            onOpenColumnCount = {
                viewModel.openColumnCountDialog()
            },
            onOpenSubjectColors = {
                viewModel.closeOtherMenuSheet()
                viewModel.openSubjectColorsDialog()
            },
            onOpenFontSize = {
                viewModel.closeOtherMenuSheet()
                viewModel.openFontSizeDialog()
            },
            onOpenClassSelect = { viewModel.openClassDialog() },
            onOpenElectives = { viewModel.openElectivesDialog() },
            onOpenNotificationSettings = {
                viewModel.closeOtherMenuSheet()
                viewModel.openNotificationSettingsDialog()
            },
            onOpenDeveloperMenu = { viewModel.openCsvSettingsDialog() },
            onOpenHistory = { viewModel.openHistoryDialog() },
            onOpenHelp = { viewModel.openHelpDialog() },
            onOpenTerms = { viewModel.openTermsDialog() },
            onOpenReportExternal = { viewModel.openExternalReportLink() },
            onOpenTimetableChangeExternal = { viewModel.openExternalTimetableChangeLink() }
        )
    }
}

/**
 * Mode Switcher & Time Header
 */
@Composable
fun ModeSwitchAndDateBar(
    viewMode: TimetableViewMode,
    liveTimeString: String,
    columnCount: Int,
    onToggleMode: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(0.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Segmented Button
            Surface(
                onClick = onToggleMode,
                shape = RoundedCornerShape(8.dp),
                color = if (viewMode == TimetableViewMode.TIMETABLE)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .height(36.dp)
                    .testTag("mode_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (viewMode == TimetableViewMode.TIMETABLE)
                            Icons.Default.MenuBook
                        else
                            Icons.Default.DirectionsWalk,
                        contentDescription = null,
                        tint = if (viewMode == TimetableViewMode.TIMETABLE)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (viewMode == TimetableViewMode.TIMETABLE) "時間割モード" else "移動教室モード",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (viewMode == TimetableViewMode.TIMETABLE)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "切替",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Right Info: Live Time & Column count indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = liveTimeString.ifBlank { "現在時刻" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Section under all tables for the Selected Date's Event and Memo
 * - Events are non-editable (Read-only from CSV)
 * - Memos are fully editable by the user
 */
@Composable
fun SelectedDayEventMemoSection(
    selectedDate: LocalDate,
    isToday: Boolean,
    eventText: String,
    memoText: String,
    minMemoLines: Int = 2,
    onSaveMemo: (String) -> Unit
) {
    val dateFormatted = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("M月d日 (E)", Locale.JAPANESE))
    }

    var localMemo by remember(selectedDate, memoText) { mutableStateOf(memoText) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_day_details_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Selected Date badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$dateFormatted",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isToday) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "今日",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 1. 行事 (Event) Display Field - Non-editable (Read-only)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        "行事",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (eventText.isNotBlank()) eventText else "ここには何もないようです:(",
                        fontSize = 13.sp,
                        fontWeight = if (eventText.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                        color = if (eventText.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            // 2. メモ・持ち物 (Memo) Input Field - User editable
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "メモ",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                OutlinedTextField(
                    value = localMemo,
                    onValueChange = {
                        localMemo = it
                        onSaveMemo(it)
                    },
                    placeholder = { Text("メモを入力...", fontSize = 12.sp) },
                    minLines = minMemoLines,
                    maxLines = (minMemoLines + 3).coerceAtLeast(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selected_day_memo_input")
                )
            }
        }
    }
}

/**
 * Fixed Bottom Navigation Bar combining:
 * - Upper row: [前日] [今日] [翌日]
 * - Lower row: [テーマ] [行事] [メモ] [日付選択] [その他]
 */
@Composable
fun FixedBottomControls(
    isDarkTheme: Boolean,
    currentBaseDate: LocalDate,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onOpenAllEvents: () -> Unit,
    onOpenAllMemos: () -> Unit,
    onOpenDialDatePicker: () -> Unit,
    onOpenOtherMenu: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fixed_bottom_controls"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Date Navigation Buttons [◀ 前日] [今日] [翌日 ▶]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("nav_previous_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("前日", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = onToday,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(38.dp)
                        .testTag("nav_today_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("今日", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onNext,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("nav_next_button"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Text("翌日", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            // Row 2: Controls [Theme] -> [行事一覧] -> [メモ一覧] -> [日付 (○月○日)] -> [その他]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Theme Toggle (Light theme has custom vibrant styling)
                Surface(
                    onClick = onToggleDarkTheme,
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEDE7F6),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkTheme) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0xFFD1C4E9)
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("theme_toggle_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "テーマ切り替え",
                            tint = if (isDarkTheme) Color(0xFFFFD54F) else Color(0xFF5E35B1),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // 行事一覧 Button
                OutlinedButton(
                    onClick = onOpenAllEvents,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("nav_events_button"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "行事",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color(0xFFE65100)
                    )
                }

                // メモ一覧 Button
                OutlinedButton(
                    onClick = onOpenAllMemos,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("nav_memos_button"),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        tint = Color(0xFF00897B),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "メモ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color(0xFF00897B)
                    )
                }

                // Dial Date Selector Button (Opens Dial/Drum-roll Date Picker)
                OutlinedButton(
                    onClick = onOpenDialDatePicker,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("dial_date_picker_button"),
                    contentPadding = PaddingValues(horizontal = 3.dp)
                ) {
                    Text(
                        text = "${currentBaseDate.monthValue}月${currentBaseDate.dayOfMonth}日",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(1.dp))
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = "日付選択",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Other Menu
                Button(
                    onClick = onOpenOtherMenu,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("other_menu_button"),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("その他", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }
            }
        }
    }
}
