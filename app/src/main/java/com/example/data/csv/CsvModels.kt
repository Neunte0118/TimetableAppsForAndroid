package com.example.data.csv

/**
 * 6種類のCSVデータモデル
 */

// 1. クラス別時間割
// class, type, day_of_week, first_period, second_period, third_period, fourth_period, fifth_period
data class BasicClassScheduleRow(
    val classId: String,
    val type: String,          // A, B, C, etc.
    val dayOfWeek: String,     // 月, 火, 水, 木, 金, 土, 日
    val periods: List<String>  // [1限, 2限, 3限, 4限, 5限]
)

// 2. 時間割（予定・共通）
// dates, first_period, second_period, third_period, fourth_period, fifth_period
data class CommonScheduleRow(
    val dateStr: String,
    val month: Int,
    val day: Int,
    val periodCodes: List<String> // ["A月1", "A月2", "A月3", "A月4", "A月5"] or ["C土1", "学問発見講座", ...]
)

// 3. 選択科目
// origin, electives, name
data class ElectiveItemRow(
    val origin: String,     // 例: "数Ⅱ S/La", "A1", "E1"
    val elective: String,   // 例: "ⅡLa①", "現読①"
    val name: String        // 例: "1組", "2組", "B31", "大講義室", "PC教室"
) {
    val className: String get() = name
}

// 4. 行事予定表
// dates, events
data class EventRow(
    val dateStr: String,
    val month: Int,
    val day: Int,
    val event: String
)

// 5. 祝日
// dates, holidays
data class HolidayRow(
    val dateStr: String,
    val month: Int,
    val day: Int,
    val holiday: String
)

// 6. 更新情報
// versions, dates, contents
data class UpdateHistoryRow(
    val version: String,
    val dateStr: String,
    val contents: String
)

// アプリ更新案内情報 (ver, info, link)
data class AppUpdateInfo(
    val version: String,
    val info: String,
    val link: String
) {
    /**
     * この更新情報のバージョンが現在のアプリバージョン (currentVersion) よりも新しいかを判定する
     */
    fun isNewerThan(currentVersion: String): Boolean {
        return isNewerVersion(version, currentVersion)
    }

    companion object {
        /**
         * latest が current よりも新しいバージョンであるかをセマンティック比較する
         * - "v1.0.0", "ver 1.0" などのプレフィックスに対応
         * - "1.0" と "1.0.0" などの欠損桁は 0 埋めして同一バージョンとして比較
         * - 単なる不一致 (!=) ではなく、最新バージョンが現在バージョンを上回る場合のみ true を返す
         */
        fun isNewerVersion(latest: String, current: String): Boolean {
            val cleanLatest = latest.trim()
                .removePrefix("v").removePrefix("V")
                .removePrefix("ver").removePrefix("Ver")
                .trim()
            val cleanCurrent = current.trim()
                .removePrefix("v").removePrefix("V")
                .removePrefix("ver").removePrefix("Ver")
                .trim()

            if (cleanLatest.isEmpty()) return false
            if (cleanLatest.equals(cleanCurrent, ignoreCase = true)) return false

            val latestParts = cleanLatest.split('.').map { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }
            val currentParts = cleanCurrent.split('.').map { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val vLatest = latestParts.getOrElse(i) { 0 }
                val vCurrent = currentParts.getOrElse(i) { 0 }
                if (vLatest > vCurrent) return true
                if (vLatest < vCurrent) return false
            }
            return false
        }
    }
}

// 7. 時間割変更届 (class 0 は全クラス)
// dates, classes, periods, subjects
data class TimetableChangeNotificationRow(
    val dateStr: String,
    val month: Int,
    val day: Int,
    val classId: String,   // "0" = 全クラス, "1" ~ "9", etc.
    val period: Int,       // 1 ~ 5 (or more)
    val subject: String    // 変更後の教科・科目
)

// 8. 講座変更
// dates, periods, previous_course, new_course
data class CourseChangeNotificationRow(
    val dateStr: String,
    val month: Int,
    val day: Int,
    val period: Int,           // 1 ~ 5 (or more)
    val previousCourse: String,// 変更前の講座名 (例: ⅡLb②)
    val newCourse: String      // 変更後の講座名 (例: 化S③)
)

// 9. 考査時間割
// dates, periods, subjects, start_time, end_time, classroom
data class ExamScheduleRow(
    val dateStr: String,
    val month: Int,
    val day: Int,
    val period: Int,           // 1 ~ 7
    val subject: String,       // 考査科目名
    val startTime: String,     // 開始時間 (例: 8:50)
    val endTime: String,       // 終了時間 (例: 9:40)
    val classroom: String = "",// 教室（省略時は通常教室または空）
    val classId: String = "全" // クラスに関係なくテストがあるため原則不要（互換用デフォルト"全"）
)

// 10. 科目名対応表 (source -> target)
// source, target
data class SubjectMappingRow(
    val source: String, // 選択科目の講座記号 (例: ⅡLa①, 世特①, 生S①)
    val target: String  // 考査科目名・正式名称 (例: 数学ⅡL, 世界史特講, 生物講究)
)

/**
 * 外部CSV URL設定及び連携リンク設定
 */
data class CsvUrlsConfig(
    val commonScheduleUrl: String = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQSizltFHoOWYdi97m2q_x21-XHwaeeMTzbUk0jlWCZRAD-CmsGn9uKZQMe2rHbIxP7_pEekWK84yf9/pub?gid=598100052&single=true&output=csv",
    val basicClassUrl: String = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQSizltFHoOWYdi97m2q_x21-XHwaeeMTzbUk0jlWCZRAD-CmsGn9uKZQMe2rHbIxP7_pEekWK84yf9/pub?gid=886789577&single=true&output=csv",
    val electivesUrl: String = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQSizltFHoOWYdi97m2q_x21-XHwaeeMTzbUk0jlWCZRAD-CmsGn9uKZQMe2rHbIxP7_pEekWK84yf9/pub?gid=1007938727&single=true&output=csv",
    val updatesUrl: String = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQSizltFHoOWYdi97m2q_x21-XHwaeeMTzbUk0jlWCZRAD-CmsGn9uKZQMe2rHbIxP7_pEekWK84yf9/pub?gid=2022174384&single=true&output=csv",
    val eventsUrl: String = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQSizltFHoOWYdi97m2q_x21-XHwaeeMTzbUk0jlWCZRAD-CmsGn9uKZQMe2rHbIxP7_pEekWK84yf9/pub?gid=1047451506&single=true&output=csv",
    val holidaysUrl: String = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQSizltFHoOWYdi97m2q_x21-XHwaeeMTzbUk0jlWCZRAD-CmsGn9uKZQMe2rHbIxP7_pEekWK84yf9/pub?gid=652046519&single=true&output=csv",
    val timetableChangeSheetUrl: String = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQiStJCsPKp1ndi958BLOajBqizE_aIcO2Z0f9hPgiyPV19rnWB3qVcrLuVEaeCeE5ddaIudtX7VkzE/pub?gid=1149682638&single=true&output=csv",
    val courseChangeSheetUrl: String = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQiStJCsPKp1ndi958BLOajBqizE_aIcO2Z0f9hPgiyPV19rnWB3qVcrLuVEaeCeE5ddaIudtX7VkzE/pub?gid=1592703701&single=true&output=csv",
    val examScheduleUrl: String = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQSizltFHoOWYdi97m2q_x21-XHwaeeMTzbUk0jlWCZRAD-CmsGn9uKZQMe2rHbIxP7_pEekWK84yf9/pub?gid=436772895&single=true&output=csv",
    val subjectMappingUrl: String = "https://docs.google.com/spreadsheets/d/e/2PACX-1vQSizltFHoOWYdi97m2q_x21-XHwaeeMTzbUk0jlWCZRAD-CmsGn9uKZQMe2rHbIxP7_pEekWK84yf9/pub?gid=1612164498&single=true&output=csv",
    val reportUrl: String = "https://forms.gle/KiiEAds2vtjAmsZ97",
    val timetableChangeUrl: String = "https://docs.google.com/forms/d/e/1FAIpQLSfTOKMLJz896qfq7OKSv7TRwxxJxX4VIqXT4npLcGmqWNyBkg/viewform?usp=preview"
)

/**
 * CSV同期結果
 */
data class CsvSyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val lastSyncSuccess: Boolean = true,
    val lastSyncMessage: String = "未同期（設定済みURLから同期可能）",
    val basicCount: Int = 0,
    val commonCount: Int = 0,
    val electiveCount: Int = 0,
    val eventCount: Int = 0,
    val holidayCount: Int = 0,
    val updateCount: Int = 0,
    val timetableChangeCount: Int = 0,
    val courseChangeCount: Int = 0,
    val examCount: Int = 0,
    val subjectMappingCount: Int = 0
)
