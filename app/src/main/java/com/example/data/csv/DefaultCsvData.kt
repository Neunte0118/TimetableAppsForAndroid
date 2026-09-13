package com.example.data.csv

/**
 * 初期CSVデータ定義（空ヘッダーのみ保持、サンプルデータ全削除）
 */
object DefaultCsvData {

    // 1. クラス別時間割
    val BASIC_CLASS_CSV = """
class,type,day_of_week,first_period,second_period,third_period,fourth_period,fifth_period
""".trimIndent()

    // 2. 時間割（予定・共通）
    val COMMON_SCHEDULE_CSV = """
dates,first_period,second_period,third_period,fourth_period,fifth_period
""".trimIndent()

    // 3. 選択科目
    val ELECTIVES_CSV = """
origin,electives,name
""".trimIndent()

    // 4. 行事予定表
    val EVENTS_CSV = """
dates,events
""".trimIndent()

    // 5. 祝日
    val HOLIDAYS_CSV = """
dates,holidays
""".trimIndent()

    // 6. 更新情報
    val UPDATE_HISTORY_CSV = """
versions,dates,contents
""".trimIndent()

    // 7. 時間割変更届
    val TIMETABLE_CHANGE_CSV = """
dates,classes,periods,subjects
""".trimIndent()

    // 8. 講座変更
    val COURSE_CHANGE_CSV = """
dates,periods,previous_course,new_course
""".trimIndent()
}
