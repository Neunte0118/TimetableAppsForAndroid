package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*
import java.time.LocalDate

enum class TimetableViewMode {
    TIMETABLE,        // 時間割モード (科目名表示)
    MOVING_CLASSROOM  // 移動教室モード (教室名表示)
}

/**
 * 時間割セルの配色モード
 * UNIFORM: すっきりとした統一カラー（過剰な色分けをなくしたモダンデザイン）
 * COLORFUL: 教科別カラー
 * PASTEL: 優しいパステル調
 */
enum class CellColorMode(val label: String, val description: String) {
    UNIFORM("統一（シンプル・推奨）", "落ち着いた単一カラーで統一し、すっきり見やすく表示します"),
    COLORFUL("教科別カラー", "国語・数学・英語・理科・社会などを色分けして表示します"),
    PASTEL("パステルカラー", "柔らかく淡いトーンで色分けします")
}

/**
 * セルフォントサイズ設定
 */
enum class TimetableFontSize(val label: String, val subjectSp: Float, val classroomSp: Float) {
    SMALL("小（コンパクト）", 12f, 11f),
    MEDIUM("標準（おすすめ）", 14f, 12.5f),
    LARGE("大（見やすい）", 16f, 14f)
}

data class ClassGroup(
    val id: String,
    val name: String,
    val grade: Int,
    val section: String
)

data class PeriodSchedule(
    val period: Int,            // 1限, 2限, 3限, 4限, 5限
    val subject: String,        // 科目 (例: 日本史探究, 英語コミュニケーション)
    val classroom: String,      // 教室 (例: 第1社会科室, LL教室)
    val teacher: String = "",   // 担当教員
    val memo: String = "",       // 持ち物・小テストなどのメモ
    val colorHex: Long = 0xFF90A4AEL,
    val isChanged: Boolean = false, // 時間割変更フラグ
    val isExam: Boolean = false,    // 考査フラグ
    val startTime: String = "",     // 開始時間 (例: 8:50)
    val endTime: String = "",       // 終了時間 (例: 9:40)
    val isUnselectedElective: Boolean = false // 選択していない科目フラグ（未選択時は灰色表示）
) {
    fun getComposeColor(colorMode: CellColorMode = CellColorMode.UNIFORM): Color {
        return when (colorMode) {
            CellColorMode.UNIFORM -> Color(0xFF5C6BC0)
            CellColorMode.COLORFUL -> Color(colorHex)
            CellColorMode.PASTEL -> {
                val c = Color(colorHex)
                c.copy(alpha = 0.85f)
            }
        }
    }
}

data class DaySchedule(
    val date: LocalDate,
    val dayLabel: String,      // "今日", "翌日", "2日後", "3日後", etc.
    val periods: List<PeriodSchedule>,
    val event: String = "",    // 行事 (避難訓練, テスト期間, etc.)
    val memo: String = ""      // メモ (宿題, 提出物, etc.)
)

data class ElectiveChoice(
    val id: String,
    val categoryName: String,
    val selectedOption: String,
    val availableOptions: List<String>
)

enum class SearchResultType(val label: String) {
    EVENT("行事・祝日"),
    MEMO("メモ・連絡"),
    SUBJECT("時間割科目")
}

data class SearchResultItem(
    val date: LocalDate,
    val dayLabel: String,
    val type: SearchResultType,
    val title: String,
    val snippet: String
)

/**
 * 教科グループとHSV色設定モデル
 */
data class SubjectColorGroup(
    val id: String,
    val name: String,
    val hue: Float = 0f,          // 0f .. 360f
    val saturation: Float = 0.8f, // 0f .. 1f
    val value: Float = 0.85f,     // 0f .. 1f
    val subjects: List<String> = emptyList()
) {
    fun toColorLong(): Long {
        val hsv = floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f)
        )
        val argb = android.graphics.Color.HSVToColor(hsv)
        return (argb.toLong() and 0xFFFFFFFFL)
    }

    fun toComposeColor(): Color {
        return Color(toColorLong())
    }
}

/**
 * 行事や文字列中の <br>, <br/>, <br /> タグを改行 \n に変換するユーティリティ
 */
fun formatEventText(text: String): String {
    if (text.isBlank()) return ""
    return text.replace(Regex("(?i)<br\\s*/?>"), "\n").trim()
}

/**
 * 各教科・科目ごとの色分け設定とデフォルト色（HSV対応グループ）
 */
object SubjectColorDefaults {
    val defaultGroups: List<SubjectColorGroup> = listOf(
        SubjectColorGroup(
            id = "japanese",
            name = "国語",
            hue = 0.0f,
            saturation = 0.50f,
            value = 0.90f,
            subjects = listOf(
                "国語", "現代文", "現文", "古典", "古講", "古典講読", "古典探求", "古典探究", "古探",
                "現読①", "現読②", "現読③", "現読④", "現世読", "現代世界を読む",
                "国語表現", "言文", "言語文化", "論国", "論理国語", "文国", "文学国語",
                "現文or英長or英W"
            )
        ),
        SubjectColorGroup(
            id = "math",
            name = "数学",
            hue = 207.0f,
            saturation = 0.59f,
            value = 0.96f,
            subjects = listOf(
                "数特", "数演L",
                "数講Sa①", "数講Sa②", "数講Sa③", "数講Sa④", "数講Sa⑤", "数講Sa⑥", "数講Sa⑦", "数講Sa⑧",
                "数講Sb①", "数講Sb②", "数講Sb③", "数講Sb④", "数講Sb⑤", "数講Sb⑥", "数講Sb⑦", "数講Sb⑧",
                "ⅡLa①", "ⅡLa②", "ⅡLa③", "ⅡLa④",
                "ⅡLb①", "ⅡLb②", "ⅡLb③",
                "ⅡS①", "ⅡS②", "ⅡS③", "ⅡS④", "ⅡS⑤", "ⅡS⑥",
            )
        ),
        SubjectColorGroup(
            id = "english",
            name = "英語",
            hue = 291.0f,
            saturation = 0.48f,
            value = 0.88f,
            subjects = listOf(
                "英語", "英語R", "英語W", "英語Wt", "英語長文", "英語長", "英長", "英W", "英Wt",
                "W", "Wt", "W/Wt", "英語W/Wt", "英WorIBA", "英W/IBA",
                "総英 Ⅲ長", "総英 ⅢR", "コミ英", "論表", "コミュニケーション英語", "論理・表現",
                "IBA", "IBA Ⅳ", "IBA IV", "IBA 4", "IBA1", "IBA2", "IBA3", "IBA4",
                "IbA", "ⅠbA", "iV", "ⅠV", "IV", "Ib", "IA", "IIb", "Ⅱb"
            )
        ),
        SubjectColorGroup(
            id = "science",
            name = "理科",
            hue = 123.0f,
            saturation = 0.35f,
            value = 0.78f,
            subjects = listOf(
                "化学", "物理", "生物", "地学", "理演L", "理科演習L",
                "物L", "化L", "生L", "地L",
                "化S①", "化S②", "化S③", "化S④", "化S⑤", "化S⑥", "化学L", "化学L①", "化学L②",
                "物S①", "物S②", "物S③", "物S④", "物S⑤", "物理L",
                "生S①", "生S②", "生物L", "生物L①", "生物L②", "生物L③",
                "地学L", "地学L①", "地学L②", "地学L③"
            )
        ),
        SubjectColorGroup(
            id = "social",
            name = "社会",
            hue = 36.0f,
            saturation = 0.70f,
            value = 1.00f,
            subjects = listOf(
                "地総", "歴総", "公共",
                "地理講①", "地理講②", "地理講③", "地理講④", "地理講⑤", "地理講⑥",
                "地理特①", "地理特②", "地理特③", "地理特④", "地理特⑤", "地理特⑥",
                "日講①", "日講②", "日特①", "日特②",
                "世講①", "世講②", "世特①", "世特②",
                "倫政講①", "倫政講②", "倫政講③", "倫政講④",
                "倫政特①", "倫政特②", "倫政特③", "倫政特④"
            )
        ),
        SubjectColorGroup(
            id = "art",
            name = "芸術",
            hue = 340.0f,
            saturation = 0.59f,
            value = 0.94f,
            subjects = listOf(
                "音楽", "音特", "美術", "美特", "書道", "書特"
            )
        ),
        SubjectColorGroup(
            id = "pe_and_hr",
            name = "体育・HR",
            hue = 18.0f,
            saturation = 0.65f,
            value = 0.88f,
            subjects = listOf(
                "HR", "LHR", "ホームルーム", "進路HR", "HR教室",
                "体育", "体育共修", "体育別修", "保健", "保体",
                "学問発見講座", "総合探究", "総探", "探究", "総合的な探究の時間",
                "始業式等", "始業式", "終業式", "式典", "道徳", "情報"
            )
        ),
        SubjectColorGroup(
            id = "elective_course",
            name = "未選択",
            hue = 220.0f,
            saturation = 0.08f,
            value = 0.82f,
            subjects = listOf(
                "数Ⅱ S/La", "数Ⅱ S/Lb",
                "G1", "G2", "E1", "E2", "A1", "A2", "J1", "J2", "K1", "K2", "C1", "C2", "F1", "F2", "H1", "H2"
            )
        ),
    )

    // 後方互換用マップ
    val defaultSubjectColors: LinkedHashMap<String, Long> by lazy {
        val map = LinkedHashMap<String, Long>()
        defaultGroups.forEach { grp ->
            val colorLong = grp.toColorLong()
            grp.subjects.forEach { s -> map[s] = colorLong }
        }
        map
    }

    fun getColorForSubject(subject: String): Long {
        return defaultSubjectColors[subject] ?: 0xFF90A4AEL
    }

    fun getColorForSubject(subject: String, groups: List<SubjectColorGroup>): Long {
        val trimmed = subject.trim()
        if (trimmed.isBlank()) return 0xFF90A4AEL

        // 1. グループ完全一致
        val directFound = groups.find { grp -> grp.subjects.any { it.equals(trimmed, ignoreCase = true) } }
        if (directFound != null) return directFound.toColorLong()

        // 2. defaultSubjectColors キャッシュ照合
        if (defaultSubjectColors.containsKey(trimmed)) return defaultSubjectColors[trimmed]!!

        // 3. 複合科目・略称の分解照合 (例: 現文or英長or英W, 英WorIBA, A/B, 現文・英語)
        val delimiterRegex = Regex("""/|／|or|OR|・|、""")
        if (trimmed.contains(delimiterRegex)) {
            val parts = trimmed.split(delimiterRegex)
            for (part in parts) {
                val p = part.trim()
                if (p.isBlank()) continue
                val partFound = groups.find { grp ->
                    grp.subjects.any {
                        it.equals(p, ignoreCase = true) ||
                        it.startsWith(p, ignoreCase = true) ||
                        p.startsWith(it, ignoreCase = true)
                    }
                }
                if (partFound != null) return partFound.toColorLong()
            }
        }

        // 4. 科目キーワード・プレフィックス判定
        val keywordFound = groups.find { grp ->
            when (grp.id) {
                "japanese" -> listOf("国語", "現代文", "現文", "古典", "古講", "現読", "言文", "論国", "文国").any { trimmed.contains(it) }
                "english" -> listOf("英語", "英W", "英Wt", "英長", "IBA", "IbA", "ⅠbA", "コミ英", "論表").any { trimmed.contains(it, ignoreCase = true) }
                "math" -> listOf("数学", "数講", "数特", "数演", "ⅡS", "ⅡL").any { trimmed.contains(it) }
                "science" -> listOf("理科", "化学", "物理", "生物", "地学", "理演", "物L", "化L", "生L", "地L", "化S", "物S", "生S").any { trimmed.contains(it) }
                "social" -> listOf("社会", "地理", "日本史", "世界史", "倫政", "地総", "歴総", "公共", "日特", "世特", "日講", "世講").any { trimmed.contains(it) }
                "art" -> listOf("音楽", "美術", "書道", "音特", "美特", "書特").any { trimmed.contains(it) }
                "pe_and_hr" -> listOf("体育", "HR", "LHR", "ホームルーム", "進路HR", "探究", "総探", "始業式", "終業式", "式典", "学問発見").any { trimmed.contains(it, ignoreCase = true) }
                else -> false
            }
        }
        if (keywordFound != null) return keywordFound.toColorLong()

        // 5. フォールバック
        val electiveGroup = groups.find { it.id == "elective_course" }
        return electiveGroup?.toColorLong() ?: 0xFF90A4AEL
    }
}

object DefaultSchoolData {
    val classes = (1..9).map { num ->
        ClassGroup(
            id = num.toString(),
            name = "${num}組",
            grade = 1,
            section = "$num"
        )
    }
}

