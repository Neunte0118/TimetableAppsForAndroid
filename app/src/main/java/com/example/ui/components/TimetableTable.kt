package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance

/**
 * Clean & modern timetable grid component.
 * - Today's header has a solid red background without "今日" label text.
 * - Tap column header or cell to select the date and highlight the entire column cleanly (from 1st to 5th period).
 * - Configurable subject text color coding (Group HSV support).
 * - Highlight cells with timetable changes (isChanged) with high-contrast in both light & dark themes.
 * - Read-only cells.
 * - Shows only Subject in TIMETABLE mode, and only Classroom in MOVING_CLASSROOM mode.
 */
@Composable
fun TimetableTable(
    daySchedules: List<DaySchedule>,
    selectedDate: LocalDate,
    today: LocalDate,
    viewMode: TimetableViewMode,
    cellColorMode: CellColorMode = CellColorMode.UNIFORM,
    isSubjectColorEnabled: Boolean = false,
    subjectColorGroups: List<SubjectColorGroup> = SubjectColorDefaults.defaultGroups,
    subjectColors: Map<String, Long> = emptyMap(),
    isHighlightChangedPeriods: Boolean = true,
    timetableFontSize: TimetableFontSize = TimetableFontSize.MEDIUM,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    tableTitle: String? = null,
    cellHeight: androidx.compose.ui.unit.Dp = 48.dp
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timetable_table_card"),
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
                .padding(6.dp)
        ) {
            // Optional Table Title
            if (tableTitle != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp, top = 2.dp)
                ) {
                    Text(
                        text = tableTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 1. Column Headers (Date & Day of Week)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Period header blank label
                val periodColWidth = if (daySchedules.size >= 6) 30.dp else if (daySchedules.size >= 5) 32.dp else 36.dp
                Box(
                    modifier = Modifier
                        .width(periodColWidth)
                        .height(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "時限",
                        fontSize = if (daySchedules.size >= 6) 10.5.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Date Columns
                daySchedules.forEach { schedule ->
                    val isToday = schedule.date == today
                    val isSelected = schedule.date == selectedDate
                    val dayOfWeek = schedule.date.dayOfWeek

                    val defaultDayColor = when (dayOfWeek) {
                        DayOfWeek.SUNDAY -> Color(0xFFE53935)
                        DayOfWeek.SATURDAY -> Color(0xFF1E88E5)
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    val dateString = remember(schedule.date, daySchedules.size) {
                        if (daySchedules.size >= 6) {
                            schedule.date.format(DateTimeFormatter.ofPattern("M/d(E)", Locale.JAPANESE))
                        } else {
                            schedule.date.format(DateTimeFormatter.ofPattern("M/d (E)", Locale.JAPANESE))
                        }
                    }

                    val headerBgColor = when {
                        isToday -> Color(0xFFE53935) // 赤い背景 (今日)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    }

                    val headerTextColor = when {
                        isToday -> Color.White // 赤背景用の白文字
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> defaultDayColor
                    }

                    val headerFontSize = when {
                        daySchedules.size >= 7 -> 9.5.sp
                        daySchedules.size >= 6 -> 10.5.sp
                        daySchedules.size >= 5 -> 11.5.sp
                        else -> 12.5.sp
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .padding(horizontal = 1.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(headerBgColor)
                            .then(
                                if (isToday && isSelected) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = Color.White,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                } else if (isSelected) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelectDate(schedule.date) }
                            .padding(vertical = 2.dp, horizontal = 1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = dateString,
                            fontSize = headerFontSize,
                            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                            color = headerTextColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Timetable Grid Body (Periods 1-5 with clean column-enclosing highlight)
            val periodColWidth = if (daySchedules.size >= 6) 30.dp else if (daySchedules.size >= 5) 32.dp else 36.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Period numbers column (1〜5)
                Column(
                    modifier = Modifier.width(periodColWidth),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (period in 1..5) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(cellHeight)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "$period",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (daySchedules.size >= 6) 12.sp else 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Day columns (each column contains periods 1 to 5 enclosed together)
                daySchedules.forEach { schedule ->
                    val isSelectedCol = schedule.date == selectedDate
                    val isTodayCol = schedule.date == today

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelectedCol) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .then(
                                if (isSelectedCol) {
                                    Modifier.border(
                                        width = 1.5.dp,
                                        color = if (isTodayCol) Color(0xFFE53935) else MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelectDate(schedule.date) }
                            .padding(if (isSelectedCol) 1.5.dp else 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            for (period in 1..5) {
                                val periodSchedule = schedule.periods.find { it.period == period }
                                    ?: PeriodSchedule(
                                        period = period,
                                        subject = "",
                                        classroom = "",
                                        colorHex = 0xFF9E9E9E
                                    )

                                val cellContent = if (viewMode == TimetableViewMode.TIMETABLE) {
                                    periodSchedule.subject
                                } else {
                                    periodSchedule.classroom
                                }

                                val hasContent = cellContent.isNotBlank()

                                // 背景色の計算（ダークモード・ライトモードで明瞭なコントラストを確保）
                                val baseCellColor = periodSchedule.getComposeColor(cellColorMode)
                                val isChangedActive = periodSchedule.isChanged && isHighlightChangedPeriods && hasContent
                                val isExamActive = periodSchedule.isExam && hasContent
                                val isUnselectedActive = periodSchedule.isUnselectedElective && hasContent

                                val surfaceBgColor = when {
                                    // 1. 未選択科目・自分の選択科目でない科目（灰色、ダークテーマでは明度を変える）
                                    isUnselectedActive -> {
                                        if (isDark) {
                                            if (isSelectedCol) Color(0xFF424242) else Color(0xFF303030)
                                        } else {
                                            if (isSelectedCol) Color(0xFFE0E0E0) else Color(0xFFEEEEEE)
                                        }
                                    }
                                    // 2. 考査：通常科目（紫色）
                                    isExamActive -> {
                                        if (isDark) {
                                            if (isSelectedCol) Color(0xFF4A148C).copy(alpha = 0.7f) else Color(0xFF311B92).copy(alpha = 0.55f)
                                        } else {
                                            if (isSelectedCol) Color(0xFFD1C4E9).copy(alpha = 0.85f) else Color(0xFFEDE7F6)
                                        }
                                    }
                                    // 3. 授業変更がありハイライト有効な場合
                                    isChangedActive -> {
                                        if (isDark) {
                                            if (isSelectedCol) Color(0xFF5D4037) else Color(0xFF3E2723)
                                        } else {
                                            if (isSelectedCol) Color(0xFFFFE082).copy(alpha = 0.7f) else Color(0xFFFFF3E0)
                                        }
                                    }
                                    !hasContent -> {
                                        // 空きコマ/授業なし
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    }
                                    cellColorMode == CellColorMode.UNIFORM -> {
                                        if (isSelectedCol) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.surface
                                    }
                                    cellColorMode == CellColorMode.COLORFUL -> {
                                        if (isSelectedCol) baseCellColor.copy(alpha = 0.28f)
                                        else baseCellColor.copy(alpha = 0.12f)
                                    }
                                    else -> {
                                        if (isSelectedCol) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                        else MaterialTheme.colorScheme.surface
                                    }
                                }

                                // テキスト色の計算（教科色分け・ダークテーマ・時間割変更時の視認性を完全保証）
                                val calculatedTextColor = when {
                                    // 1. 未選択科目・自分の選択科目でない科目（灰色）
                                    isUnselectedActive -> {
                                        if (isDark) Color(0xFF9E9E9E) else Color(0xFF757575)
                                    }
                                    // 2. 考査：通常科目（紫色）
                                    isExamActive -> {
                                        if (isDark) Color(0xFFEDE7F6) else Color(0xFF4A148C)
                                    }
                                    // 3. 時間割変更コマ（色分けOFF時はダーク/ライトに合わせた高コントラスト文字色）
                                    isChangedActive -> {
                                        if (isSubjectColorEnabled && viewMode == TimetableViewMode.TIMETABLE && periodSchedule.subject.isNotBlank()) {
                                            val colorLong = SubjectColorDefaults.getColorForSubject(periodSchedule.subject, subjectColorGroups)
                                            Color(colorLong)
                                        } else {
                                            if (isDark) Color(0xFFFFD54F) else Color(0xFFBF360C)
                                        }
                                    }
                                    // 4. 教科色分け有効時
                                    viewMode == TimetableViewMode.TIMETABLE && isSubjectColorEnabled && periodSchedule.subject.isNotBlank() -> {
                                        val colorLong = SubjectColorDefaults.getColorForSubject(periodSchedule.subject, subjectColorGroups)
                                        Color(colorLong)
                                    }
                                    // 5. 通常文字色
                                    else -> {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                }

                                val examTimeTextColor = when {
                                    isUnselectedActive -> if (isDark) Color(0xFF757575) else Color(0xFF9E9E9E)
                                    isDark -> Color(0xFFD1C4E9)
                                    else -> Color(0xFF5E35B1)
                                }

                                val targetSp = if (viewMode == TimetableViewMode.TIMETABLE) {
                                    timetableFontSize.subjectSp
                                } else {
                                    timetableFontSize.classroomSp
                                }
                                val colScale = when {
                                    daySchedules.size >= 7 -> 2.5f
                                    daySchedules.size >= 6 -> 1.8f
                                    daySchedules.size >= 5 -> 1.0f
                                    daySchedules.size >= 4 -> 0.3f
                                    else -> 0f
                                }
                                val charLengthPenalty = when {
                                    cellContent.length >= 7 && daySchedules.size >= 6 -> 1.2f
                                    cellContent.length >= 5 && daySchedules.size >= 6 -> 0.6f
                                    else -> 0f
                                }

                                val finalFontSize = (targetSp - colScale - charLengthPenalty).coerceAtLeast(8.5f).sp

                                val cellBorder = when {
                                    isUnselectedActive -> BorderStroke(1.dp, if (isDark) Color(0xFF616161) else Color(0xFFBDBDBD))
                                    isExamActive -> BorderStroke(1.5.dp, if (isDark) Color(0xFFCE93D8) else Color(0xFF7E57C2))
                                    isChangedActive -> BorderStroke(1.5.dp, if (isDark) Color(0xFFFFB74D) else Color(0xFFFF8F00))
                                    isSelectedCol -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                    hasContent -> BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                    else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }

                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = surfaceBgColor,
                                    border = cellBorder,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(cellHeight)
                                        .testTag("cell_${schedule.date}_$period")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        // 考査開始時間（左上）
                                        if (periodSchedule.isExam && periodSchedule.startTime.isNotBlank()) {
                                            Text(
                                                text = periodSchedule.startTime,
                                                fontSize = 7.5.sp,
                                                lineHeight = 8.5.sp,
                                                color = examTimeTextColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(start = 1.dp, top = 1.dp)
                                            )
                                        }

                                        // 考査終了時間（左下）
                                        if (periodSchedule.isExam && periodSchedule.endTime.isNotBlank()) {
                                            Text(
                                                text = periodSchedule.endTime,
                                                fontSize = 7.5.sp,
                                                lineHeight = 8.5.sp,
                                                color = examTimeTextColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(start = 1.dp, bottom = 1.dp)
                                            )
                                        }

                                        // 考査教室・サブ情報（右上）
                                        if (periodSchedule.isExam) {
                                            val cornerText = if (viewMode == TimetableViewMode.TIMETABLE) {
                                                periodSchedule.classroom
                                            } else {
                                                periodSchedule.subject
                                            }
                                            if (cornerText.isNotBlank()) {
                                                Text(
                                                    text = cornerText,
                                                    fontSize = 7.5.sp,
                                                    lineHeight = 8.5.sp,
                                                    color = examTimeTextColor,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(end = 1.dp, top = 1.dp)
                                                )
                                            }
                                        }

                                        // 中央の教科・科目名
                                        Text(
                                            text = cellContent,
                                            fontWeight = if (isSelectedCol || isChangedActive || (periodSchedule.isExam && !periodSchedule.isUnselectedElective)) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = finalFontSize,
                                            lineHeight = (finalFontSize.value * 1.15f).sp,
                                            color = calculatedTextColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

