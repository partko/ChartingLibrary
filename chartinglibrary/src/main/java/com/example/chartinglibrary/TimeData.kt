package com.example.chartinglibrary

import java.text.SimpleDateFormat
import java.util.Locale

enum class TimeData(val index: String) {
    YEAR("0"),
    MONTH_NUM("1"),
    MONTH_FULL("2"),
    MONTH_SHORT("3"),
    DAY("4"),
    HOUR("5"),
    MINUTE("6"),
    SECOND("7")
}

fun getTimeIndex(time: TimeData): String {
    return time.index
}
