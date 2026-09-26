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

    // 9. 考査時間割
    val EXAM_SCHEDULE_CSV = """
dates,periods,subjects,start_time,end_time,classroom
""".trimIndent()

    // 10. 科目名対応表 (source -> target)
    val SUBJECT_MAPPING_CSV = """
source,target
ⅡLa①,数学ⅡL
ⅡLa②,数学ⅡL
ⅡLa③,数学ⅡL
ⅡLa④,数学ⅡL
ⅡLb①,数学ⅡL
ⅡLb②,数学ⅡL
ⅡLb③,数学ⅡL
ⅡS①,数学ⅡS
ⅡS②,数学ⅡS
ⅡS③,数学ⅡS
ⅡS④,数学ⅡS
ⅡS⑤,数学ⅡS
ⅡS⑥,数学ⅡS
数講Sa①,数学講究
数講Sa②,数学講究
数講Sa③,数学講究
数講Sa④,数学講究
数講Sa⑤,数学講究
数講Sa⑥,数学講究
数講Sa⑦,数学講究
数講Sa⑧,数学講究
数講Sb①,数学講究
数講Sb②,数学講究
数講Sb③,数学講究
数講Sb④,数学講究
数講Sb⑤,数学講究
数講Sb⑥,数学講究
数講Sb⑦,数学講究
数講Sb⑧,数学講究
現読①,現代世界を読む
現読②,現代世界を読む
現読③,現代世界を読む
現読④,現代世界を読む
現世読,現代世界を読む
古講①,古典講読
古講②,古典講読
古講③,古典講読
古講④,古典講読
化S①,化学講究
化S②,化学講究
化S③,化学講究
化S④,化学講究
化S⑤,化学講究
化S⑥,化学講究
物S①,物理講究
物S②,物理講究
物S③,物理講究
物S④,物理講究
物S⑤,物理講究
生S①,生物講究
生S②,生物講究
化学L,理科演習L
化学L①,理科演習L
化学L②,理科演習L
物理L,理科演習L
生物L,理科演習L
生物L①,理科演習L
生物L②,理科演習L
生物L③,理科演習L
地学L,理科演習L
地学L①,理科演習L
地学L②,理科演習L
地学L③,理科演習L
物L,理科演習L
化L,理科演習L
生L,理科演習L
地L,理科演習L
地理特①,地理特講
地理特②,地理特講
地理特③,地理特講
地理特④,地理特講
地理特⑤,地理特講
地理特⑥,地理特講
地理講①,地理講究
地理講②,地理講究
地理講③,地理講究
地理講④,地理講究
地理講⑤,地理講究
地理講⑥,地理講究
世特①,世界史特講
世特②,世界史特講
世講①,世界史講究
世講②,世界史講究
日特①,日本史特講
日特②,日本史特講
日講①,日本史講究
日講②,日本史講究
倫政特①,倫政特講
倫政特②,倫政特講
倫政特③,倫政特講
倫政特④,倫政特講
倫政講①,倫政講究
倫政講②,倫政講究
倫政講③,倫政講究
倫政講④,倫政講究
数演L,数学演習L
数特,数学特講
生活,生活科学
""".trimIndent()
}
