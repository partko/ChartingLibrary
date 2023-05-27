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

fun convertTime(unixTime: String): MutableList<String> {
    val dateFormat = SimpleDateFormat("yyyy MM MMM MMM dd HH mm ss", Locale.ENGLISH)
    return getDateString(unixTime, dateFormat)
}

fun stringToWords(s : String) = s.trim().splitToSequence(' ')
    .filter { it.isNotEmpty() } // or: .filter { it.isNotBlank() }
    .toMutableList()


fun getDateString(time: String, dateFormat: SimpleDateFormat) : MutableList<String> = stringToWords(dateFormat.format(time.toLong()))
