package com.example.simpleclock.ui

import android.icu.util.ChineseCalendar
import java.util.Calendar
import java.util.Locale

/**
 * 农历日期。换算交给系统自带的 ICU 中国历（API 24+），这里只负责中文名称。
 */
internal data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean
) {
    val monthName: String
        get() = (if (isLeapMonth) "闰" else "") + MONTH_NAMES[month]

    val dayName: String
        get() = DAY_NAMES[day]

    val ganZhi: String
        get() = STEMS[(year - 4).mod(10)] + BRANCHES[(year - 4).mod(12)]

    val zodiac: String
        get() = ZODIAC[(year - 4).mod(12)]

    /** 例：农历九月十九 */
    val dateText: String
        get() = "农历$monthName$dayName"

    /** 例：丙午马年 */
    val yearText: String
        get() = "$ganZhi${zodiac}年"
}

internal fun lunarDateFrom(timeMillis: Long): LunarDate {
    val chineseCalendar = ChineseCalendar(Locale.CHINA).apply { timeInMillis = timeMillis }
    val gregorianYear = Calendar.getInstance().apply { timeInMillis = timeMillis }.get(Calendar.YEAR)
    // YEAR 是 60 年周期里的序号（1984 年为甲子年，序号 1），据此还原农历年份；
    // 不用 EXTENDED_YEAR，因为不同 Android / ICU 版本对它的定义不一致
    var lunarYear = 1984 + chineseCalendar.get(ChineseCalendar.YEAR) - 1
    while (lunarYear < gregorianYear - 1) lunarYear += 60
    while (lunarYear > gregorianYear + 1) lunarYear -= 60
    return LunarDate(
        year = lunarYear,
        month = chineseCalendar.get(ChineseCalendar.MONTH) + 1,
        day = chineseCalendar.get(ChineseCalendar.DAY_OF_MONTH),
        isLeapMonth = chineseCalendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1
    )
}

private val MONTH_NAMES = arrayOf(
    "", "正月", "二月", "三月", "四月", "五月", "六月",
    "七月", "八月", "九月", "十月", "冬月", "腊月"
)

private val DAY_NAMES = arrayOf(
    "",
    "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
    "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
    "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
)

private val STEMS = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
private val BRANCHES = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
private val ZODIAC = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
