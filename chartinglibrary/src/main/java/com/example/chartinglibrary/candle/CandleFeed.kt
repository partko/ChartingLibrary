package com.example.chartinglibrary.candle

data class CandleFeed(
    val open: Float,
    val close: Float,
    val high: Float,
    val low: Float,
    val time: String
)