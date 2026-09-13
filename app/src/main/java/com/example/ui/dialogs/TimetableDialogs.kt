@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui.dialogs

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.csv.AppUpdateInfo
import com.example.data.csv.CsvSyncStatus
import com.example.data.csv.CsvUrlsConfig
import com.example.data.csv.UpdateHistoryRow
import com.example.model.*
import com.example.ui.theme.SaturdayColor
import com.example.ui.theme.SundayColor
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 共有: 正式な利用規約コンポーネント (全文)
 */
@Composable
fun TermsOfServiceContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("1. はじめに（本アプリの目的）", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "本アプリ「時間割」は、利用者の所属クラス・受講する選択科目や講座に応じた最適な時間割、移動教室、学校行事予定、および時間割変更情報を手元で快適に確認できるようにし、日々の学校生活における時間割確認の負担を軽減することを目的として提供されています。",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("2. 禁止事項", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "利用者は、本アプリの利用にあたり以下の行為を行ってはなりません。\n・本アプリのリバースエンジニアリング、不正な改ざん、またはサーバーや配信元への過度な負荷をかける行為\n・開発者の事前の明示的な承諾のない無断での二次配布や公衆送信\n・法令、公序良俗に反する行為、または開発者が不適切と合理的に判断した行為",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("3. 免責事項", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "・本アプリの利用または利用不能により生じた利用者のいかなる損害（遅刻、情報の不一致、端末の不具合等を含む）について、開発者は一切の責任を負いません。\n・時間割・行事・時間割変更等のデータは公開情報・指定スプレッドシート等から取得して提供されますが、その完全性・正確性・最新性を保証するものではありません。急な時間割変更や公式連絡については、学校からの連絡・掲示等を最優先でご確認ください。\n・予告なく機能の改善、仕様変更、サービスの一時中断やアップデートの配信を行う場合があります。",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("4. データの取り扱いとプライバシー", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "・「メモ」や各種設定内容（選択クラス、選択科目・講座、通知設定、文字サイズ、教科カラー配色など）は、利用者の端末内（ローカルストレージ）にのみ安全に保存されます。利用者の個人情報やメモ内容が外部サーバーに送信・収集されることは一切ありません。\n・アプリの品質向上やクラッシュ解析のため、個人を特定しない匿名の統計ログを収集する場合があります。\n・端末の初期化、アプリのアンインストール、またはデータ消去操作を行った場合、保存された「メモ」等のデータは消去されます。重要な情報は本アプリ単体に依存せず、必要に応じて各自で記録・保管してください。",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("5. 規約の変更", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                "開発者は、必要に応じて本利用規約を改定することができます。改定された規約は、本アプリ内に掲示された時点から効力を生じるものとします。",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text("2026年9月 改訂", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* ==========================================================================
   1. 初回訪問時 オンボーディング ダイアログ群
   ========================================================================== */

@Composable
fun OnboardingTermsDialog(
    onAgree: () -> Unit
) {
    var agreedCheckbox by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* Modal */ },
        modifier = Modifier.testTag("onboarding_terms_dialog"),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "初期設定 (1 / 3)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text("利用規約への同意", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        TermsOfServiceContent()
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { agreedCheckbox = !agreedCheckbox }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = agreedCheckbox,
                        onCheckedChange = { agreedCheckbox = it },
                        modifier = Modifier.testTag("terms_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "利用規約に同意します",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAgree,
                enabled = agreedCheckbox,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("agree_terms_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("次へ", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun OnboardingClassDialog(
    classes: List<ClassGroup>,
    currentSelected: ClassGroup,
    onClassSelected: (ClassGroup) -> Unit
) {
    var selected by remember { mutableStateOf(currentSelected) }

    AlertDialog(
        onDismissRequest = { /* Modal */ },
        modifier = Modifier.testTag("onboarding_class_dialog"),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "初期設定 (2 / 3)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text("所属クラスの選択", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "あなたの所属クラス（1〜9組）を選択してください。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(classes) { classGroup ->
                        val isSelected = classGroup.id == selected.id
                        Surface(
                            onClick = { selected = classGroup },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("onboarding_class_item_${classGroup.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        classGroup.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onClassSelected(selected) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_onboarding_class_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("次へ", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * データロード待機モーダル (初回起動時用)
 */
@Composable
fun OnboardingDataLoadingDialog(
    className: String,
    isSyncing: Boolean,
    syncMessage: String,
    onRetry: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Modal */ },
        modifier = Modifier.testTag("onboarding_data_loading_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "onboarding_sync_rotation")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2400, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "onboarding_sync_angle"
                )
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "読込中",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
                Text("時間割データを読込中", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "$className の時間割・選択科目データを読み込んでいます。\nデータがロードされ次第、自動的に選択科目画面へ進みます。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
                if (syncMessage.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            syncMessage,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.testTag("skip_loading_button")
            ) {
                Text("スキップして進む")
            }
        },
        dismissButton = {
            if (!isSyncing) {
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("retry_sync_button")
                ) {
                    Text("再読み込み")
                }
            }
        }
    )
}

@Composable
fun OnboardingElectivesDialog(
    className: String,
    electiveOptions: Map<String, List<String>>,
    userSelections: Map<String, String>,
    onUpdateElective: (String, String) -> Unit,
    onComplete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Modal */ },
        modifier = Modifier.testTag("onboarding_electives_dialog"),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "初期設定 (3 / 3)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text("選択科目の選択 ($className)", fontWeight = FontWeight.Bold, fontSize = 19.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "選択科目を選んでください。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                if (electiveOptions.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "このクラスで受講する選択科目は登録されていません。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    val sortedOrigins = remember(electiveOptions) {
                        electiveOptions.keys.sortedWith(Comparator { a, b -> compareDialogNaturalOrder(a, b) })
                    }
                    sortedOrigins.forEach { origin ->
                        val options = electiveOptions[origin] ?: emptyList()
                        val currentChoice = userSelections[origin] ?: ""
                        ElectiveDropdownField(
                            origin = origin,
                            options = options,
                            selectedChoice = currentChoice,
                            onOptionSelected = { chosen ->
                                onUpdateElective(origin, chosen)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("complete_onboarding_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("完了", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/* ==========================================================================
   2. 通常ダイアログ群
   ========================================================================== */

/**
 * テーブル表示数設定 ダイアログ (1〜3)
 */
@Composable
fun TableDisplayCountDialog(
    currentCount: Int,
    onSelectCount: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("table_display_count_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("テーブルの表示数", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "画面に同時に表示する時間割テーブルの数（1〜3個）を選択してください。デフォルトは1です。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..3).forEach { count ->
                        val isSelected = count == currentCount
                        Surface(
                            onClick = { onSelectCount(count) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("table_display_count_option_$count")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${count}テーブル",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (count == 1) {
                                        Text("(標準)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

/**
 * 1テーブルの表示列数設定 ダイアログ (3〜7)
 */
@Composable
fun ColumnCountDialog(
    currentCount: Int,
    onSelectCount: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.ViewColumn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("1テーブルの表示列数", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "画面内に表示する日数（3日〜7日）を選択してください。デフォルトは5列です。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (3..7).forEach { count ->
                        val isSelected = count == currentCount
                        Surface(
                            onClick = { onSelectCount(count) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("column_count_option_$count")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${count}列",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

/**
 * 時間割文字サイズ設定 ダイアログ
 */
@Composable
fun TimetableFontSizeDialog(
    currentSize: TimetableFontSize,
    onSelectSize: (TimetableFontSize) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("文字サイズ設定", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "時間割セルに表示される科目名や教室名の文字サイズを選択してください。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TimetableFontSize.values().forEach { sizeOption ->
                    val isSelected = sizeOption == currentSize
                    Surface(
                        onClick = { onSelectSize(sizeOption) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(sizeOption.label, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "科目: ${sizeOption.subjectSp}sp / 移動教室: ${sizeOption.classroomSp}sp",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectSize(sizeOption) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

/**
 * 教科グループごとのHSV色分け設定 ダイアログ
 */
@Composable
fun SubjectColorsDialog(
    isEnabled: Boolean,
    groups: List<SubjectColorGroup>,
    availableSubjects: List<String> = emptyList(),
    onToggleEnabled: (Boolean) -> Unit,
    onUpdateGroupHsv: (String, Float, Float, Float) -> Unit,
    onUpdateGroupName: (String, String) -> Unit,
    onAddGroup: (String, Float, Float, Float) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onAssignSubject: (String, String) -> Unit,
    onRemoveSubject: (String, String) -> Unit,
    onResetGroups: () -> Unit,
    onDismiss: () -> Unit
) {
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showAssignSubjectDialog by remember { mutableStateOf<SubjectColorGroup?>(null) }

    val allKnownSubjects = remember(availableSubjects, groups) {
        val set = linkedSetOf<String>()
        availableSubjects.forEach { if (it.isNotBlank()) set.add(it.trim()) }
        SubjectColorDefaults.defaultSubjectColors.keys.forEach { set.add(it) }
        groups.forEach { g -> g.subjects.forEach { set.add(it) } }
        set.toList()
    }

    val activeEditingGroup = groups.find { it.id == editingGroupId }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("subject_colors_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("教科グループ別色分け設定 (HSV)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. 有効/無効スイッチ
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("教科テキストの色分け", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "グループごとにHSVで決めた色で時間割の文字を表示します（初期値: オフ）",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = onToggleEnabled,
                            modifier = Modifier.testTag("subject_color_enabled_switch")
                        )
                    }
                }

                if (isEnabled) {
                    // グループヘッダーアクション
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "登録グループ (${groups.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { showAddGroupDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("グループ追加", fontSize = 11.sp)
                            }
                            TextButton(
                                onClick = onResetGroups,
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("初期化", fontSize = 11.sp)
                            }
                        }
                    }

                    // グループリスト
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groups, key = { it.id }) { group ->
                            val groupColor = group.toComposeColor()
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // グループ名 & 色見本 & 編集ボタン
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(groupColor)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            )
                                            Column {
                                                Text(
                                                    group.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = groupColor
                                                )
                                                Text(
                                                    "H:${group.hue.toInt()}° S:${(group.saturation * 100).toInt()}% V:${(group.value * 100).toInt()}%",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            IconButton(
                                                onClick = { editingGroupId = group.id },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Colorize,
                                                    contentDescription = "色を編集",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { showAssignSubjectDialog = group },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Outlined.AddCircleOutline,
                                                    contentDescription = "教科を追加",
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            if (groups.size > 1) {
                                                IconButton(
                                                    onClick = { onDeleteGroup(group.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "グループ削除",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 所属科目タグチップ一覧
                                    if (group.subjects.isEmpty()) {
                                        Text(
                                            "※ 所属している教科はありません (右上の＋ボタンから追加)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    } else {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            group.subjects.forEach { subjectName ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = groupColor.copy(alpha = 0.12f),
                                                    border = BorderStroke(0.8.dp, groupColor.copy(alpha = 0.5f))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            subjectName,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = groupColor
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "削除",
                                                            tint = groupColor,
                                                            modifier = Modifier
                                                                .size(12.dp)
                                                                .clickable { onRemoveSubject(subjectName, group.id) }
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
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完了")
            }
        }
    )

    // HSVカラーピッカー & グループ編集サブダイアログ
    activeEditingGroup?.let { group ->
        HsvGroupColorPickerDialog(
            group = group,
            onSave = { name, h, s, v ->
                onUpdateGroupName(group.id, name)
                onUpdateGroupHsv(group.id, h, s, v)
                editingGroupId = null
            },
            onDismiss = { editingGroupId = null }
        )
    }

    // 新規グループ追加ダイアログ
    if (showAddGroupDialog) {
        AddSubjectGroupDialog(
            onAdd = { name, h, s, v ->
                onAddGroup(name, h, s, v)
                showAddGroupDialog = false
            },
            onDismiss = { showAddGroupDialog = false }
        )
    }

    // グループへの科目割り当てダイアログ
    showAssignSubjectDialog?.let { group ->
        AssignSubjectToGroupDialog(
            targetGroup = group,
            allKnownSubjects = allKnownSubjects,
            onAssign = { subject ->
                onAssignSubject(subject, group.id)
            },
            onDismiss = { showAssignSubjectDialog = null }
        )
    }
}

/**
 * HSVカラー調整 & グループ名編集ダイアログ
 */
@Composable
fun HsvGroupColorPickerDialog(
    group: SubjectColorGroup,
    onSave: (name: String, hue: Float, saturation: Float, value: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember(group) { mutableStateOf(group.name) }
    var hue by remember(group) { mutableStateOf(group.hue) }
    var saturation by remember(group) { mutableStateOf(group.saturation) }
    var value by remember(group) { mutableStateOf(group.value) }

    val currentColor = remember(hue, saturation, value) {
        val hsv = floatArrayOf(hue, saturation, value)
        val colorInt = android.graphics.Color.HSVToColor(hsv)
        Color(colorInt)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                )
                Text("HSVカラー調整", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("グループ名") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // プレビューカード
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = currentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, currentColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "文字色の表示プレビュー: ${groupName.ifBlank { "サンプル" }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = currentColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "HEX: #${Integer.toHexString(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))).uppercase()}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. 色相 (Hue: 0 ~ 360°)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("色相 (Hue)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${hue.toInt()}°", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. 彩度 (Saturation: 0 ~ 100%)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("彩度 (Saturation)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${(saturation * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 3. 明度 (Value: 0 ~ 100%)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("明度 (Value)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${(value * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(groupName.trim(), hue, saturation, value) },
                enabled = groupName.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

/**
 * 新規グループ作成ダイアログ
 */
@Composable
fun AddSubjectGroupDialog(
    onAdd: (name: String, hue: Float, saturation: Float, value: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var hue by remember { mutableStateOf(200f) }
    var saturation by remember { mutableStateOf(0.8f) }
    var value by remember { mutableStateOf(0.85f) }

    val currentColor = remember(hue, saturation, value) {
        val hsv = floatArrayOf(hue, saturation, value)
        val colorInt = android.graphics.Color.HSVToColor(hsv)
        Color(colorInt)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新規グループの追加", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("グループ名 (例: 探究, 芸術, 商業)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = currentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, currentColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (groupName.isBlank()) "プレビュー色" else groupName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = currentColor
                        )
                    }
                }

                // Hue
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("色相 (Hue): ${hue.toInt()}°", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                }

                // Saturation
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("彩度 (Saturation): ${(saturation * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)
                }

                // Value
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("明度 (Value): ${(value * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(groupName.trim(), hue, saturation, value) },
                enabled = groupName.isNotBlank()
            ) {
                Text("追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

/**
 * 科目をグループに割り当て・追加ダイアログ
 */
@Composable
fun AssignSubjectToGroupDialog(
    targetGroup: SubjectColorGroup,
    allKnownSubjects: List<String>,
    onAssign: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customSubjectName by remember { mutableStateOf("") }

    val candidates = remember(allKnownSubjects, targetGroup.subjects, searchQuery) {
        val remaining = allKnownSubjects.filter { !targetGroup.subjects.contains(it) }
        if (searchQuery.isBlank()) remaining
        else {
            val q = searchQuery.trim().lowercase()
            remaining.filter { it.lowercase().contains(q) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("「${targetGroup.name}」に教科を追加", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 直接入力
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = customSubjectName,
                        onValueChange = { customSubjectName = it },
                        placeholder = { Text("直接入力 (例: 物理基礎)", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (customSubjectName.isNotBlank()) {
                                onAssign(customSubjectName.trim())
                                customSubjectName = ""
                            }
                        },
                        enabled = customSubjectName.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("追加", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("一覧から教科を検索", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "登録候補一覧から選択して追加:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(candidates) { subject ->
                        Surface(
                            onClick = {
                                onAssign(subject)
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(subject, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "追加",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

/**
 * 行事一覧ダイアログ
 */
@Composable
fun AllEventsDialog(
    eventsList: List<SearchResultItem>,
    onSelectDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(eventsList, searchQuery) {
        if (searchQuery.isBlank()) eventsList
        else {
            val q = searchQuery.trim().lowercase()
            eventsList.filter { it.title.lowercase().contains(q) || it.snippet.lowercase().contains(q) || it.dayLabel.contains(q) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFFE65100))
                        Text("年間行事予定一覧", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("行事名を絞り込み (例: テスト, 終業式)", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "登録行事: ${filteredList.size}件 (タップして該当日付へ移動)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("該当する行事予定はありません", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectDate(item.date)
                                        onDismiss()
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.dayLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFE65100)
                                        )
                                        Text(
                                            item.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "移動",
                                        tint = MaterialTheme.colorScheme.outline
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

/**
 * メモ一覧ダイアログ
 */
@Composable
fun AllMemosDialog(
    memosList: List<SearchResultItem>,
    onSelectDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(memosList, searchQuery) {
        if (searchQuery.isBlank()) memosList
        else {
            val q = searchQuery.trim().lowercase()
            memosList.filter { it.title.lowercase().contains(q) || it.snippet.lowercase().contains(q) || it.dayLabel.contains(q) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF00897B))
                        Text("保存済みメモ一覧", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "閉じる")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("メモ内容を絞り込み (例: 提出物, 持ち物)", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "保存メモ: ${filteredList.size}件 (タップして該当日付へ移動)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("保存されているメモはありません", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectDate(item.date)
                                        onDismiss()
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            color = Color(0xFF00897B).copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = item.dayLabel,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF00897B),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = item.snippet,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 5,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "移動",
                                        tint = MaterialTheme.colorScheme.outline
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

/**
 * クラス選択ダイアログ (1〜9組)
 */
@Composable
fun ClassSelectDialog(
    selectedClass: ClassGroup,
    classes: List<ClassGroup>,
    onDismiss: () -> Unit,
    onClassSelected: (ClassGroup) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Class, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("クラスの変更", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("表示する所属クラスを選択してください。", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(classes) { classGroup ->
                        val isSelected = classGroup.id == selectedClass.id
                        Surface(
                            onClick = { onClassSelected(classGroup) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("class_item_${classGroup.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        classGroup.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

/**
 * 自然順ソート（丸数字 ①〜⑳ や半角/全角数字対応）ヘルパー
 */
private fun tokenizeForDialogNaturalSort(s: String): List<Any> {
    val list = mutableListOf<Any>()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c in '\u2460'..'\u2473') {
            val num = (c - '\u2460' + 1).toLong()
            list.add(num)
            i++
        } else if (c in '\u3251'..'\u325F') {
            val num = (c - '\u3251' + 21).toLong()
            list.add(num)
            i++
        } else if (c in '0'..'9' || c in '０'..'９') {
            var j = i
            var num = 0L
            while (j < s.length && (s[j] in '0'..'9' || s[j] in '０'..'９')) {
                val digit = if (s[j] in '0'..'9') s[j] - '0' else s[j] - '０'
                num = num * 10 + digit
                j++
            }
            list.add(num)
            i = j
        } else {
            var j = i
            while (j < s.length && !(s[j] in '\u2460'..'\u2473') && !(s[j] in '\u3251'..'\u325F') && !(s[j] in '0'..'9' || s[j] in '０'..'９')) {
                j++
            }
            list.add(s.substring(i, j))
            i = j
        }
    }
    return list
}

private fun compareDialogNaturalOrder(a: String, b: String): Int {
    val tokensA = tokenizeForDialogNaturalSort(a)
    val tokensB = tokenizeForDialogNaturalSort(b)
    val len = minOf(tokensA.size, tokensB.size)
    for (i in 0 until len) {
        val tA = tokensA[i]
        val tB = tokensB[i]
        if (tA is Long && tB is Long) {
            val cmp = tA.compareTo(tB)
            if (cmp != 0) return cmp
        } else {
            val cmp = tA.toString().compareTo(tB.toString(), ignoreCase = true)
            if (cmp != 0) return cmp
        }
    }
    return tokensA.size.compareTo(tokensB.size)
}

/**
 * 選択科目用プルダウンセレクター
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectiveDropdownField(
    origin: String,
    options: List<String>,
    selectedChoice: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val sortedOptions = remember(options) {
        options.sortedWith(Comparator { a, b -> compareDialogNaturalOrder(a, b) })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Outlined.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                "選択科目: $origin",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedChoice.ifBlank { "未選択" },
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("elective_dropdown_$origin")
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                sortedOptions.forEach { option ->
                    val isSelected = option == selectedChoice
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = option,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}

/**
 * 選択科目ダイアログ (クラス別・originから選択)
 */
@Composable
fun ElectivesDialog(
    className: String,
    electiveOptions: Map<String, List<String>>,
    userSelections: Map<String, String>,
    onUpdateElective: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("選択科目の変更 ($className)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "選択科目を選んでください。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (electiveOptions.isEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "このクラスで受講する選択科目は登録されていません。",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    val sortedOrigins = remember(electiveOptions) {
                        electiveOptions.keys.sortedWith(Comparator { a, b -> compareDialogNaturalOrder(a, b) })
                    }
                    sortedOrigins.forEach { origin ->
                        val options = electiveOptions[origin] ?: emptyList()
                        val currentChoice = userSelections[origin] ?: ""
                        ElectiveDropdownField(
                            origin = origin,
                            options = options,
                            selectedChoice = currentChoice,
                            onOptionSelected = { chosen ->
                                onUpdateElective(origin, chosen)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完了")
            }
        }
    )
}

/**
 * 開発者メニュー ダイアログ (CSV設定・データ管理)
 */
@Composable
fun DeveloperMenuDialog(
    initialConfig: CsvUrlsConfig,
    syncStatus: CsvSyncStatus,
    cellColorMode: CellColorMode = CellColorMode.UNIFORM,
    onSetCellColorMode: (CellColorMode) -> Unit = {},
    onSaveAndSync: (CsvUrlsConfig) -> Unit,
    onResetToDefaultUrls: () -> Unit,
    onClearAllCache: () -> Unit,
    onClearAllMemos: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    var commonScheduleUrl by remember(initialConfig) { mutableStateOf(initialConfig.commonScheduleUrl) }
    var basicClassUrl by remember(initialConfig) { mutableStateOf(initialConfig.basicClassUrl) }
    var electivesUrl by remember(initialConfig) { mutableStateOf(initialConfig.electivesUrl) }
    var updatesUrl by remember(initialConfig) { mutableStateOf(initialConfig.updatesUrl) }
    var eventsUrl by remember(initialConfig) { mutableStateOf(initialConfig.eventsUrl) }
    var holidaysUrl by remember(initialConfig) { mutableStateOf(initialConfig.holidaysUrl) }
    var timetableChangeSheetUrl by remember(initialConfig) { mutableStateOf(initialConfig.timetableChangeSheetUrl) }
    var courseChangeSheetUrl by remember(initialConfig) { mutableStateOf(initialConfig.courseChangeSheetUrl) }
    var reportUrl by remember(initialConfig) { mutableStateOf(initialConfig.reportUrl) }
    var timetableChangeUrl by remember(initialConfig) { mutableStateOf(initialConfig.timetableChangeUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("developer_menu_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("開発者メニュー", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category Tabs (2 tabs: CSV・URL, データ管理)
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("CSV・URL", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("データ管理", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // 1. CSV・URL設定
                            Surface(
                                color = if (syncStatus.lastSyncSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (syncStatus.lastSyncSuccess) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (syncStatus.isSyncing) {
                                            val infiniteTransition = rememberInfiniteTransition(label = "csv_status_sync_rotation")
                                            val rotation by infiniteTransition.animateFloat(
                                                initialValue = 0f,
                                                targetValue = 360f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(durationMillis = 2400, easing = LinearEasing),
                                                    repeatMode = RepeatMode.Restart
                                                ),
                                                label = "csv_status_sync_angle"
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .graphicsLayer { rotationZ = rotation }
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (syncStatus.lastSyncSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = if (syncStatus.lastSyncSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = syncStatus.lastSyncMessage,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (syncStatus.lastSyncSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    if (syncStatus.lastSyncTime != null) {
                                        Text(
                                            text = "最終同期: ${syncStatus.lastSyncTime}  (予定:${syncStatus.commonCount}件, クラス:${syncStatus.basicCount}件, 選択:${syncStatus.electiveCount}件, 行事:${syncStatus.eventCount}件, 祝日:${syncStatus.holidayCount}件, 変更:${syncStatus.timetableChangeCount}件, 講座:${syncStatus.courseChangeCount}件, 更新:${syncStatus.updateCount}件)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val def = CsvUrlsConfig()
                                        commonScheduleUrl = def.commonScheduleUrl
                                        basicClassUrl = def.basicClassUrl
                                        electivesUrl = def.electivesUrl
                                        updatesUrl = def.updatesUrl
                                        eventsUrl = def.eventsUrl
                                        holidaysUrl = def.holidaysUrl
                                        timetableChangeSheetUrl = def.timetableChangeSheetUrl
                                        courseChangeSheetUrl = def.courseChangeSheetUrl
                                        reportUrl = def.reportUrl
                                        timetableChangeUrl = def.timetableChangeUrl
                                        onResetToDefaultUrls()
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("初期設定に復元", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        commonScheduleUrl = ""
                                        basicClassUrl = ""
                                        electivesUrl = ""
                                        updatesUrl = ""
                                        eventsUrl = ""
                                        holidaysUrl = ""
                                        timetableChangeSheetUrl = ""
                                        courseChangeSheetUrl = ""
                                    },
                                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                                ) {
                                    Text("クリア", fontSize = 11.sp)
                                }
                            }

                            Text("【外部CSVデータ連携URL】", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                            // 1. 時間割（予定） URL
                            OutlinedTextField(
                                value = commonScheduleUrl,
                                onValueChange = { commonScheduleUrl = it },
                                label = { Text("1. 時間割（予定） CSV URL", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("input_common_schedule_url")
                            )

                            // 2. クラス別時間割 URL
                            OutlinedTextField(
                                value = basicClassUrl,
                                onValueChange = { basicClassUrl = it },
                                label = { Text("2. クラス別時間割 CSV URL", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("input_basic_class_url")
                            )

                            // 3. 選択科目 URL
                            OutlinedTextField(
                                value = electivesUrl,
                                onValueChange = { electivesUrl = it },
                                label = { Text("3. 選択科目 CSV URL", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("input_electives_url")
                            )

                            // 4. 更新情報 URL
                            OutlinedTextField(
                                value = updatesUrl,
                                onValueChange = { updatesUrl = it },
                                label = { Text("4. 更新情報 CSV URL", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("input_updates_url")
                            )

                            // 5. 行事予定表 URL
                            OutlinedTextField(
                                value = eventsUrl,
                                onValueChange = { eventsUrl = it },
                                label = { Text("5. 行事予定表 CSV URL", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("input_events_url")
                            )

                            // 6. 祝日 URL
                            OutlinedTextField(
                                value = holidaysUrl,
                                onValueChange = { holidaysUrl = it },
                                label = { Text("6. 祝日 CSV URL", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("input_holidays_url")
                            )

                            // 7. 時間割変更届 URL
                            OutlinedTextField(
                                value = timetableChangeSheetUrl,
                                onValueChange = { timetableChangeSheetUrl = it },
                                label = { Text("7. 時間割変更届 CSV URL", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("input_timetable_change_sheet_url")
                            )

                            // 8. 講座変更 URL
                            OutlinedTextField(
                                value = courseChangeSheetUrl,
                                onValueChange = { courseChangeSheetUrl = it },
                                label = { Text("8. 講座変更 CSV URL", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("input_course_change_sheet_url")
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text("【外部リンク設定】", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                            // 不具合報告リンク
                            OutlinedTextField(
                                value = reportUrl,
                                onValueChange = { reportUrl = it },
                                label = { Text("不具合報告フォーム URL", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("input_report_url")
                            )

                            // 時間割変更届リンク
                            OutlinedTextField(
                                value = timetableChangeUrl,
                                onValueChange = { timetableChangeUrl = it },
                                label = { Text("時間割変更フォーム URL", fontSize = 11.sp) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp),
                                modifier = Modifier.fillMaxWidth().testTag("input_timetable_change_url")
                            )
                        }

                        1 -> {
                            // 2. データ管理 & 診断
                            Text("【キャッシュ・データ管理】", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Text(
                                "端末に保存されたキャッシュやメモの消去、同期ステータスの詳細確認を行えます。",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 同期診断・データ概要カード
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Text("データ概要・同期診断", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text("・予定データ: ${syncStatus.commonCount}件", fontSize = 11.5.sp)
                                    Text("・クラスデータ: ${syncStatus.basicCount}件", fontSize = 11.5.sp)
                                    Text("・選択科目データ: ${syncStatus.electiveCount}件", fontSize = 11.5.sp)
                                    Text("・行事予定データ: ${syncStatus.eventCount}件", fontSize = 11.5.sp)
                                    Text("・祝日データ: ${syncStatus.holidayCount}件", fontSize = 11.5.sp)
                                    Text("・時間割変更データ: ${syncStatus.timetableChangeCount}件", fontSize = 11.5.sp)
                                    Text("・講座変更データ: ${syncStatus.courseChangeCount}件", fontSize = 11.5.sp)
                                    Text("・更新履歴データ: ${syncStatus.updateCount}件", fontSize = 11.5.sp)
                                    if (syncStatus.lastSyncTime != null) {
                                        Text("・最終同期時刻: ${syncStatus.lastSyncTime}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("危険な操作", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)

                                    OutlinedButton(
                                        onClick = onClearAllCache,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("CSVキャッシュをすべて削除", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = onClearAllMemos,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("保存されているメモを全削除", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        onSaveAndSync(
                            CsvUrlsConfig(
                                commonScheduleUrl = commonScheduleUrl,
                                basicClassUrl = basicClassUrl,
                                electivesUrl = electivesUrl,
                                updatesUrl = updatesUrl,
                                eventsUrl = eventsUrl,
                                holidaysUrl = holidaysUrl,
                                timetableChangeSheetUrl = timetableChangeSheetUrl,
                                courseChangeSheetUrl = courseChangeSheetUrl,
                                reportUrl = reportUrl,
                                timetableChangeUrl = timetableChangeUrl
                            )
                        )
                    },
                    modifier = Modifier.testTag("save_and_sync_button"),
                    enabled = !syncStatus.isSyncing
                ) {
                    if (syncStatus.isSyncing) {
                        val infiniteTransition = rememberInfiniteTransition(label = "csv_sync_btn_rotation")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 2400, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "csv_sync_btn_angle"
                        )
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer { rotationZ = rotation }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("同期中...")
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存して今すぐ同期")
                    }
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("閉じる")
                }
            }
        },
        dismissButton = {
            if (selectedTab == 0) {
                TextButton(onClick = onDismiss) {
                    Text("閉じる")
                }
            }
        }
    )
}

/**
 * 毎日の時間割通知 設定ダイアログ (一般ユーザー設定)
 */
@Composable
fun NotificationSettingsDialog(
    isNotificationEnabled: Boolean,
    notificationHour: Int,
    notificationMinute: Int,
    onUpdateNotificationSettings: (Boolean, Int, Int) -> Unit,
    onTestSendNotification: () -> Unit,
    onDismiss: () -> Unit
) {
    var notifEnabled by remember(isNotificationEnabled) { mutableStateOf(isNotificationEnabled) }
    var notifHour by remember(notificationHour) { mutableStateOf(notificationHour) }
    var notifMinute by remember(notificationMinute) { mutableStateOf(notificationMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("notification_settings_dialog"),
        icon = {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text("毎日の時間割通知設定", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "指定した時刻に、当日の時間割（科目・移動教室）と行事予定を自動でお知らせします。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("毎朝の時間割通知", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    if (notifEnabled) "有効（毎朝 ${String.format("%02d:%02d", notifHour, notifMinute)} に通知）" else "無効",
                                    fontSize = 12.sp,
                                    color = if (notifEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = notifEnabled,
                                onCheckedChange = {
                                    notifEnabled = it
                                    onUpdateNotificationSettings(it, notifHour, notifMinute)
                                }
                            )
                        }

                        if (notifEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            Text("通知時刻の設定", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = notifHour.toString(),
                                    onValueChange = {
                                        val v = it.toIntOrNull() ?: 0
                                        notifHour = v.coerceIn(0, 23)
                                        onUpdateNotificationSettings(notifEnabled, notifHour, notifMinute)
                                    },
                                    label = { Text("時", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                Text(":", fontWeight = FontWeight.Bold, fontSize = 18.sp)

                                OutlinedTextField(
                                    value = String.format("%02d", notifMinute),
                                    onValueChange = {
                                        val v = it.toIntOrNull() ?: 0
                                        notifMinute = v.coerceIn(0, 59)
                                        onUpdateNotificationSettings(notifEnabled, notifHour, notifMinute)
                                    },
                                    label = { Text("分", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onTestSendNotification,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("今すぐ今日の時間割テスト通知を送信", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完了")
            }
        }
    )
}

/**
 * ホイール・ドラムロール式 連続日付ピッカー (月と日をセットでダイヤル: 例 8/31の次は9/1になる)
 */
@Composable
fun WheelDatePicker(
    selectedDate: LocalDate,
    dateRange: List<LocalDate>,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 46.dp,
    visibleItemCount: Int = 5
) {
    val initialIndex = remember(selectedDate, dateRange) {
        val idx = dateRange.indexOf(selectedDate)
        if (idx >= 0) idx else (dateRange.size / 2)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val selectedIndex = listState.firstVisibleItemIndex
            if (selectedIndex in dateRange.indices) {
                onDateChange(dateRange[selectedIndex])
            }
        }
    }

    LaunchedEffect(selectedDate) {
        val targetIndex = dateRange.indexOf(selectedDate)
        if (targetIndex >= 0 && !listState.isScrollInProgress && listState.firstVisibleItemIndex != targetIndex) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(itemHeight * visibleItemCount),
        contentAlignment = Alignment.Center
    ) {
        // Center selection highlight card
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(itemHeight),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        ) {}

        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * ((visibleItemCount - 1) / 2)),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(dateRange) { index, dateItem ->
                val isSelected = (listState.firstVisibleItemIndex == index)
                val dayOfWeekJp = when (dateItem.dayOfWeek) {
                    DayOfWeek.MONDAY -> "月"
                    DayOfWeek.TUESDAY -> "火"
                    DayOfWeek.WEDNESDAY -> "水"
                    DayOfWeek.THURSDAY -> "木"
                    DayOfWeek.FRIDAY -> "金"
                    DayOfWeek.SATURDAY -> "土"
                    DayOfWeek.SUNDAY -> "日"
                }
                val isToday = (dateItem == LocalDate.now())
                val isSaturday = (dateItem.dayOfWeek == DayOfWeek.SATURDAY)
                val isSunday = (dateItem.dayOfWeek == DayOfWeek.SUNDAY)

                val textColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isSunday -> SundayColor.copy(alpha = 0.7f)
                    isSaturday -> SaturdayColor.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }

                val dowColor = when {
                    isSunday -> SundayColor
                    isSaturday -> SaturdayColor
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            onDateChange(dateItem)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${dateItem.monthValue}月${dateItem.dayOfMonth}日",
                            fontSize = if (isSelected) 20.sp else 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${dayOfWeekJp})",
                            fontSize = if (isSelected) 16.sp else 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = dowColor
                        )
                        if (isToday) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "今日",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * ダイヤル式（ドラムロール）日付選択ダイアログ (月と日をセットでダイヤル)
 */
@Composable
fun DialDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var currentDateSelection by remember { mutableStateOf(initialDate) }

    // 過去6ヶ月から未来12ヶ月分までの全連続日付リスト（例: 8/31の次は9/1）
    val dateList = remember(initialDate) {
        val start = initialDate.minusMonths(6).withDayOfMonth(1)
        val end = initialDate.plusMonths(12).withDayOfMonth(1).plusMonths(1).minusDays(1)
        val list = mutableListOf<LocalDate>()
        var curr = start
        while (!curr.isAfter(end)) {
            list.add(curr)
            curr = curr.plusDays(1)
        }
        list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dial_date_picker_dialog"),
        icon = {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = {
            Text("日付の選択 (ダイヤル式)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "上下にスクロールして日付を選択してください",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    WheelDatePicker(
                        selectedDate = currentDateSelection,
                        dateRange = dateList,
                        onDateChange = { currentDateSelection = it },
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                // クイック操作ボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            currentDateSelection = currentDateSelection.minusWeeks(1)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("-1週間", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            currentDateSelection = LocalDate.now()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("今日", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            currentDateSelection = currentDateSelection.plusWeeks(1)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text("+1週間", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDateSelected(currentDateSelection)
                }
            ) {
                Text("決定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

/**
 * 更新履歴ダイアログ
 */
@Composable
fun UpdateHistoryDialog(
    historyList: List<UpdateHistoryRow> = emptyList(),
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("更新履歴", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (historyList.isEmpty()) {
                    Text("更新情報はありません", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    historyList.forEach { history ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(history.version, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                                    if (history.dateStr.isNotBlank()) {
                                        Text(history.dateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (history.contents.isNotBlank()) {
                                    Text(history.contents, fontSize = 12.sp, lineHeight = 17.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

/**
 * 新バージョン更新モーダルダイアログ
 * ver, info, link に基づいて更新情報を表示し、APKダウンロードリンクと「二度と表示しない」チェックボックスを提供する
 */
@Composable
fun AppUpdateModalDialog(
    updateInfo: AppUpdateInfo,
    onDownloadClick: (String) -> Unit,
    onDismiss: (dontShowAgain: Boolean) -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss(dontShowAgain) },
        modifier = Modifier.testTag("app_update_dialog"),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "アプリの更新があります",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "v${updateInfo.version}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "最新バージョン",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (updateInfo.info.isNotBlank()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Text(
                                text = "更新内容:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = updateInfo.info,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (updateInfo.link.isNotBlank()) {
                    Button(
                        onClick = { onDownloadClick(updateInfo.link) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("download_apk_button"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "新しいAPKをダウンロード",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { dontShowAgain = !dontShowAgain }
                        .padding(vertical = 4.dp)
                        .testTag("dont_show_again_checkbox_row")
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it },
                        modifier = Modifier.testTag("dont_show_again_checkbox")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "二度と表示しない",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDismiss(dontShowAgain) },
                modifier = Modifier.testTag("close_update_dialog_button")
            ) {
                Text("閉じる", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

/**
 * ヘルプダイアログ
 */
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    val basicGuides = listOf(
        "クラス・選択科目の設定" to "画面上部のクラス名（例: 1組）をタップすると、所属クラスの切り替えや受講する選択科目・講座を設定できます。あなた専用の時間割が自動生成されます。",
        "列タップで日付選択 & 詳細表示" to "テーブルの日付列をタップすると、その日付がハイライトされ、画面下部にその日の学校行事、入力したメモ、時間割変更の詳細が連動して表示されます。",
        "時間割 / 移動教室 切り替え" to "画面上部のトグルボタンで、科目名表示と移動教室の表示を瞬時に切り替えられます。教室移動がある授業を素早く把握できます。",
        "ダイヤル式日付選択" to "画面上部の日付（例: 9月13日）をタップすると、ドラムロール（ダイヤル）式ピッカーが開き、過去や先の日付へスムーズにジャンプできます。",
        "即時CSV更新（同期ボタン）" to "画面右上の更新ボタンをタップすると、最新の時間割・行事・時間割変更データをスプレッドシートから即座に取得して反映します。",
        "メモ入力と自動保存" to "テーブル下のメモ欄に持ち物や課題、連絡事項を自由に記録できます。入力内容は端末に自動保存されます。"
    )

    val widgetGuides = listOf(
        "2種類のホーム画面ウィジェット" to "ホーム画面に「時間割ウィジェット」と「行事・メモウィジェット」を配置できます。",
        "ホームボタン (日付リセット)" to "ウィジェット右上の「ホーム」ボタンをタップすると、今日（15:15以降は明日）の日付に素早く戻ります。",
        "日付切り替え矢印 (◀ / ▶)" to "ウィジェット左右の矢印で、前日や翌日の時間割・行事・メモを手軽に確認できます。",
        "自動日付更新" to "毎日 0:00 および 15:15 のタイミングで、ウィジェットの基準日が自動的に更新されます。",
        "ウィジェットの即時更新" to "ウィジェット右上の更新ボタンをタップすると、アプリ内の最新メモや変更情報が即時に反映されます。"
    )

    val settingGuides = listOf(
        "表示テーブル数・列数の変更" to "「その他」メニューから表示テーブル数（1〜3個）や1テーブルの列数（3〜7列）を自由に変更できます。",
        "教科テキストのHSV色分け" to "「その他」メニューの「教科の色分け」から、教科グループごとに自由なHSVカラーを割り当てて見やすく色分けできます。",
        "時間割変更の強調表示" to "通常と異なるコマ（時間割変更）の背景をハイライト表示する機能のON/OFFを設定できます。",
        "文字サイズの変更" to "小・標準・大・特大から時間割文字のサイズを好みに合わせて調整できます。",
        "毎朝の時間割通知" to "毎朝の設定時刻（例: 7:00）に今日の時間割と移動教室、行事予定が自動で通知されます。",
        "行事予定一覧 & 作成メモ一覧" to "年間の全学校行事や、作成したメモを一覧形式で確認し、対象の日付へワンタップでジャンプできます。",
        "検索機能" to "キーワード入力により、行事予定・メモ・教科名を高速横断検索できます。",
        "アプリ更新通知" to "新しいアプリバージョンが公開された際、通知ダイアログが表示されワンタップで最新版をダウンロード・更新できます。"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("使い方ヘルプ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // セクション 1: 基本操作
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("【基本操作】", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    basicGuides.forEach { (title, desc) ->
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.padding(start = 4.dp)) {
                            Text("・$title", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(desc, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // セクション 2: ウィジェットの使い方
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("【ホーム画面ウィジェット】", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF00897B))
                    widgetGuides.forEach { (title, desc) ->
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.padding(start = 4.dp)) {
                            Text("・$title", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(desc, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // セクション 3: 設定とカスタマイズ
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("【設定とカスタマイズ】", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF7C4DFF))
                    settingGuides.forEach { (title, desc) ->
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp), modifier = Modifier.padding(start = 4.dp)) {
                            Text("・$title", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(desc, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

/**
 * 利用規約ダイアログ
 */
@Composable
fun TermsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("利用規約", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TermsOfServiceContent()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}

/* ==========================================================================
   3. その他モーダルボトムシート
   ========================================================================== */

@Composable
fun OtherMenuModalSheet(
    isDeveloperMode: Boolean,
    isSubjectColorEnabled: Boolean,
    isHighlightChangedPeriods: Boolean,
    currentFontSize: TimetableFontSize,
    currentColumnCount: Int = 5,
    currentTableDisplayCount: Int = 1,
    onToggleDeveloperMode: () -> Unit,
    onToggleHighlightChangedPeriods: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenAllEvents: () -> Unit,
    onOpenAllMemos: () -> Unit,
    onOpenTableDisplayCount: () -> Unit,
    onOpenColumnCount: () -> Unit,
    onOpenSubjectColors: () -> Unit,
    onOpenFontSize: () -> Unit,
    onOpenClassSelect: () -> Unit,
    onOpenElectives: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenDeveloperMenu: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenReportExternal: () -> Unit,
    onOpenTimetableChangeExternal: () -> Unit
) {
    // カテゴリーごとのテーマカラー
    val colorSearch = Color(0xFF0288D1)     // ブルー/シアン: 検索・情報
    val colorDisplay = Color(0xFF7C4DFF)    // パープル: 表示・デザイン
    val colorClass = Color(0xFF00897B)      // ティール: クラス・科目
    val colorInfo = Color(0xFF3949AB)       // インディゴ: ヘルプ・履歴
    val colorExternal = Color(0xFFE65100)   // オレンジ: 外部リンク・規約
    val colorDev = Color(0xFF546E7A)        // スレート: 開発者

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("other_menu_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "設定・メニュー",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            // =========================================================================
            // カテゴリー 1: 検索・予定
            // =========================================================================
            CategoryHeader(title = "検索・予定", color = colorSearch)

            MenuSheetItem(
                icon = Icons.Default.Search,
                iconTint = colorSearch,
                title = "行事・メモ・時間割の検索",
                subtitle = "キーワードで日付や予定を検索",
                onClick = onOpenSearch,
                testTag = "menu_search"
            )

            MenuSheetItem(
                icon = Icons.Default.Event,
                iconTint = colorSearch,
                title = "年間行事予定一覧",
                subtitle = "年間の全行事を一覧表示・日付ジャンプ",
                onClick = onOpenAllEvents,
                testTag = "menu_all_events"
            )

            MenuSheetItem(
                icon = Icons.Default.EditNote,
                iconTint = colorSearch,
                title = "保存済みメモ一覧",
                subtitle = "作成したメモを一覧確認・日付ジャンプ",
                onClick = onOpenAllMemos,
                testTag = "menu_all_memos"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // =========================================================================
            // カテゴリー 2: 表示・レイアウト設定
            // =========================================================================
            CategoryHeader(title = "表示・レイアウト設定", color = colorDisplay)

            MenuSheetItem(
                icon = Icons.Outlined.TableChart,
                iconTint = colorDisplay,
                title = "テーブルの表示数",
                subtitle = "現在: ${currentTableDisplayCount}テーブル (1〜3個表示)",
                onClick = onOpenTableDisplayCount,
                testTag = "menu_change_table_count"
            )

            MenuSheetItem(
                icon = Icons.Outlined.ViewColumn,
                iconTint = colorDisplay,
                title = "1テーブルの表示列数",
                subtitle = "現在: ${currentColumnCount}列 (3〜7列表示)",
                onClick = onOpenColumnCount,
                testTag = "menu_change_columns"
            )

            MenuSheetItem(
                icon = Icons.Outlined.Palette,
                iconTint = colorDisplay,
                title = "教科の色分け",
                subtitle = if (isSubjectColorEnabled) "有効" else "無効 (標準)",
                onClick = onOpenSubjectColors,
                testTag = "menu_subject_colors"
            )

            // 時間割変更の強調表示 トグル
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleHighlightChangedPeriods(!isHighlightChangedPeriods) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorDisplay.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationImportant,
                            contentDescription = null,
                            tint = colorDisplay,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text("時間割変更の強調表示", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            if (isHighlightChangedPeriods) "通常と異なるコマの背景を強調" else "無効（通常背景で表示）",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isHighlightChangedPeriods,
                    onCheckedChange = onToggleHighlightChangedPeriods,
                    modifier = Modifier.testTag("highlight_changed_periods_switch")
                )
            }

            MenuSheetItem(
                icon = Icons.Outlined.FormatSize,
                iconTint = colorDisplay,
                title = "時間割の文字サイズ",
                subtitle = "現在: ${currentFontSize.label} (科目 ${currentFontSize.subjectSp}sp / 教室 ${currentFontSize.classroomSp}sp)",
                onClick = onOpenFontSize,
                testTag = "menu_font_size"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // =========================================================================
            // カテゴリー 3: クラス・時間割設定
            // =========================================================================
            CategoryHeader(title = "クラス・時間割設定", color = colorClass)

            MenuSheetItem(
                icon = Icons.Outlined.Class,
                iconTint = colorClass,
                title = "クラスの変更",
                subtitle = "1〜9組の切り替え",
                onClick = onOpenClassSelect,
                testTag = "menu_change_class"
            )

            MenuSheetItem(
                icon = Icons.Outlined.Tune,
                iconTint = colorClass,
                title = "選択科目の変更",
                subtitle = "取得している選択科目の再設定",
                onClick = onOpenElectives,
                testTag = "menu_change_electives"
            )

            MenuSheetItem(
                icon = Icons.Default.NotificationsActive,
                iconTint = colorClass,
                title = "毎日の時間割通知設定",
                subtitle = "毎朝の時間割通知のON/OFF・時刻変更",
                onClick = onOpenNotificationSettings,
                testTag = "menu_notification_settings"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // =========================================================================
            // カテゴリー 4: ガイド・更新情報
            // =========================================================================
            CategoryHeader(title = "ガイド・更新情報", color = colorInfo)

            MenuSheetItem(
                icon = Icons.Default.History,
                iconTint = colorInfo,
                title = "更新履歴",
                subtitle = "アプリのアップデート情報",
                onClick = onOpenHistory,
                testTag = "menu_history"
            )

            MenuSheetItem(
                icon = Icons.Outlined.HelpOutline,
                iconTint = colorInfo,
                title = "ヘルプ・使い方",
                subtitle = "操作方法・活用ガイド",
                onClick = onOpenHelp,
                testTag = "menu_help"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // =========================================================================
            // カテゴリー 5: 外部リンク・規約
            // =========================================================================
            CategoryHeader(title = "外部リンク・規約", color = colorExternal)

            MenuSheetItem(
                icon = Icons.Outlined.OpenInNew,
                iconTint = colorExternal,
                title = "時間割変更届",
                subtitle = "時間割変更フォーム（外部リンク）",
                isExternal = true,
                onClick = onOpenTimetableChangeExternal,
                testTag = "menu_timetable_change"
            )

            MenuSheetItem(
                icon = Icons.Outlined.BugReport,
                iconTint = colorExternal,
                title = "不具合の報告",
                subtitle = "不具合報告フォーム（外部リンク）",
                isExternal = true,
                onClick = onOpenReportExternal,
                testTag = "menu_report_bug"
            )

            MenuSheetItem(
                icon = Icons.Outlined.Description,
                iconTint = colorExternal,
                title = "利用規約",
                subtitle = "利用規約の確認",
                onClick = onOpenTerms,
                testTag = "menu_terms"
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // =========================================================================
            // カテゴリー 6: 開発者モード
            // =========================================================================
            CategoryHeader(title = "開発者メニュー", color = colorDev)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleDeveloperMode() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colorDev.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeveloperMode,
                            contentDescription = null,
                            tint = if (isDeveloperMode) MaterialTheme.colorScheme.primary else colorDev,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text("開発者モード", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            if (isDeveloperMode) "有効（開発者メニュー利用可能）" else "無効",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isDeveloperMode,
                    onCheckedChange = { onToggleDeveloperMode() },
                    modifier = Modifier.testTag("developer_mode_switch")
                )
            }

            if (isDeveloperMode) {
                MenuSheetItem(
                    icon = Icons.Default.DeveloperMode,
                    iconTint = colorDev,
                    title = "開発者メニューを開く",
                    subtitle = "セル配色モード・CSVリンク・データ管理",
                    onClick = onOpenDeveloperMenu,
                    testTag = "menu_developer_settings"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CategoryHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = color
        )
    }
}

@Composable
private fun MenuSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    title: String,
    subtitle: String,
    isExternal: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = if (isExternal) Icons.Outlined.OpenInNew else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
