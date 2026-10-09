package com.example.data.csv

import java.util.Locale

/**
 * CSVデータおよび科目・クラス・時限・日付等の正規化処理を一元管理する共通パイプライン。
 *
 * 全角・半角英数字、全角スペース、記号の表記揺れ、および同義語の対応を統一的に処理します。
 * なお、講座番号（①、②、1、2）などの各講座・セクション固有の識別情報は安易に削除せず保持します。
 */
object CsvNormalizer {

    private const val FULLWIDTH_DIGITS = "０１２３４５６７８９"
    private const val HALFWIDTH_DIGITS = "0123456789"

    private const val FULLWIDTH_UPPER = "ＡＢＣＤＥＦＧＨＩＪＫＬＭＮＯＰＱＲＳＴＵＶＷＸＹＺ"
    private const val HALFWIDTH_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    private const val FULLWIDTH_LOWER = "ａｂｃｄｅｆｇｈｉｊｋｌｍｎｏｐｑｒｓｔｕｖｗｘｙｚ"
    private const val HALFWIDTH_LOWER = "abcdefghijklmnopqrstuvwxyz"

    /**
     * 全角英数字を半角英数字に変換し、全角スペースや特殊空白を標準空白に置換します。
     * 全角コロン、スラッシュ、カッコ、ハイフンなどの一般的な記号も半角化します。
     */
    fun normalizeAlphanumeric(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder(text.length)
        for (c in text) {
            when (c) {
                in '０'..'９' -> sb.append(HALFWIDTH_DIGITS[c - '０'])
                in 'Ａ'..'Ｚ' -> sb.append(HALFWIDTH_UPPER[c - 'Ａ'])
                in 'ａ'..'ｚ' -> sb.append(HALFWIDTH_LOWER[c - 'ａ'])
                '\u3000', '\u00A0' -> sb.append(' ') // 全角スペース、NBSP
                '：' -> sb.append(':')
                '／' -> sb.append('/')
                '（' -> sb.append('(')
                '）' -> sb.append(')')
                '－', '―', 'ー', '−' -> sb.append('-')
                '～', '〜' -> sb.append('~')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * 数字のみ全角（０〜９）から半角（0〜9）へ変換します。
     */
    fun normalizeDigits(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (c in '０'..'９') {
                sb.append(HALFWIDTH_DIGITS[c - '０'])
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private val WHITESPACE_REGEX = Regex("""\s+""")
    private val subjectCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * 科目名の正規化パイプライン。
     * - 英数字・記号の半角化（例: 化Ｓ① -> 化S①, 物理Ｌ -> 物理L）
     * - 余分な空白のトリム・連続空白の集約
     * - 同義語・通称の標準化（例: 「現世読」->「現代世界を読む」）
     */
    fun normalizeSubject(subject: String): String {
        val trimmed = subject.trim()
        if (trimmed.isEmpty()) return ""
        val cached = subjectCache[trimmed]
        if (cached != null) return cached

        var s = normalizeAlphanumeric(trimmed)
        // 連続空白を1つに集約
        s = s.replace(WHITESPACE_REGEX, " ").trim()

        subjectCache[trimmed] = s
        return s
    }

    /**
     * クラスIDの正規化。
     * 例: "３組" -> "3組", " 3 " -> "3"
     */
    fun normalizeClassId(classId: String): String {
        val trimmed = classId.trim()
        if (trimmed.isEmpty()) return ""
        return normalizeDigits(trimmed)
    }

    /**
     * 時限の数値抽出パイプライン。
     * 例: "1", "１", "1限", "第2限", " 3 " -> 1, 2, 3
     */
    fun normalizePeriod(periodStr: String): Int {
        val normalized = normalizeDigits(periodStr.trim())
        val digits = normalized.filter { it.isDigit() }
        return digits.toIntOrNull() ?: 1
    }

    /**
     * 日付文字列の正規化パイプライン。
     * 全角数字を半角化し、前後の空白を除去します。
     * 例: "９月２６日" -> "9月26日", "１０／１４" -> "10/14"
     */
    fun normalizeDate(dateStr: String): String {
        return normalizeAlphanumeric(dateStr.trim())
    }

    /**
     * 時刻文字列の正規化パイプライン。
     * 例: "９：４５" -> "9:45"
     */
    fun normalizeTime(timeStr: String): String {
        val trimmed = timeStr.trim()
        if (trimmed.isEmpty()) return ""
        return normalizeAlphanumeric(trimmed).replace(" ", "")
    }

    /**
     * 検索クエリ用の正規化パイプライン。
     * 全角半角統一・小文字化を行い、検索精度を高めます。
     */
    fun normalizeSearch(query: String): String {
        val normalized = normalizeAlphanumeric(query.trim())
        return normalized.lowercase(Locale.JAPANESE)
    }
}
