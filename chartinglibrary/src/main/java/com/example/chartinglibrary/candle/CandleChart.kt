package com.example.chartinglibrary.candle

import android.content.res.Resources
import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.text.isDigitsOnly
import java.math.RoundingMode

/**
 * Candlestick chart
 *
 *
 */
@Composable
fun CandlestickChart(
    candles: MutableList<List<Float>>,
    timestamps: MutableList<MutableList<String>>,
    timeFormat: List<String>,
    selectedTimeFormat: List<String>,
    priceTagsCount: Int = 4,
    minIndent: Int = 12,
    dateOffset: Int = 1,
    chartWidth: Dp = 300.dp,
    chartHeight: Dp = 300.dp,
    candleWidth: Dp = 8.dp,
    gapWidth: Dp = 2.dp,
    topOffset: Dp = 8.dp,
    rightBarWidth: Dp = 50.dp,
    rightBarTextSize: Int = 10,
    significantDigits: Int = 2,
    bottomBarHeight: Dp = 20.dp,
    priceLineThickness: Dp = 1.dp,
    priceLineStyle: FloatArray = floatArrayOf(30f, 10f, 10f, 10f),
    selectedLineThickness: Dp = 1.dp,
    dojiCandleThickness: Dp = 1.dp,
    selectedLineStyle: FloatArray = floatArrayOf(10f, 10f),
    endButtonSize: Dp = 20.dp,
    backgroundColor: Color = Color(41, 49, 51, 255),
    rightBarColor: Color = Color(37, 44, 46, 255),
    textColor: Color = Color.White,
    separatorColor: Color = Color(71, 74, 81, 255),
    priceColor: Color = Color(0, 128, 255, 255),
    selectedColor: Color = Color(106, 90, 205, 255),
    positiveCandleColor: Color = Color.Green,
    negativeCandleColor: Color = Color.Red,
    dojiCandleColor: Color = Color.White,
    endButtonColor: Color = Color.White,
    reDraw: Boolean = false,
    liveUpdate: Boolean = false,
) {

    // pixel density: Float
    val screenDensity by remember {
        mutableStateOf(Resources.getSystem().displayMetrics.density)
    }
    //Log.d("debug", "screenDensity = $screenDensity")

    // the width of the chart in pixels: Float
    val chartWidthInPx = remember {
        mutableStateOf(chartWidth.toString().removeSuffix(".dp").toFloat().dpToFloat())
    }
    //Log.d("debug", "chartWidthInDp = $chartWidth, chartWidthInPx = ${chartWidthInPx.value}")

    // width of the right panel in pixels: Float
    val rightBarWidthInPx = remember {
        mutableStateOf(rightBarWidth.toString().removeSuffix(".dp").toFloat().dpToFloat())
    }
    //Log.d("debug", "rightBarWidthInDp = $rightBarWidth, rightBarWidthInPx = ${rightBarWidthInPx.value}")

    // the number of visible candles on the chart: Int
    val visibleCandles = remember {
        mutableStateOf((chartWidth / (candleWidth + gapWidth)).toBigDecimal().setScale(1, RoundingMode.DOWN).toInt() + 1)
    }
    //Log.d("debug", "visibleCandles = ${visibleCandles.value}")

    // the width of the (candle + indentation) in pixels: Float
    val candleWithSpaceInPx = remember {
        mutableStateOf((candleWidth + gapWidth).value * screenDensity)
    }
    //Log.d("debug", "candleWithSpace = ${candleWithSpaceInPx.value}")

    // the width of the (candle + indentation) in dp: Dp
    val candleWithSpaceInDp = remember {
        mutableStateOf(candleWidth + gapWidth)
    }
    //Log.d("debug", "candleWithSpaceInDp = ${candleWithSpaceInDp.value}")


    /**
     * Parses time data into a list of strings to display.
     */
    fun parseCandleTime(): List<String> {
        val result = mutableListOf<String>()
        var previousDate = ""
        var counter = 0
        var allowedToDraw = true
        var offsetCounter = 0

        for (i in timestamps.indices) {
            counter++
            var nextDate = ""
            for (j in timeFormat) {
                if (j.isDigitsOnly()) {
                    nextDate += timestamps[i][j.toInt()]
                } else {
                    nextDate += j
                }
            }
            if (nextDate != previousDate && allowedToDraw) {
                if (offsetCounter >= dateOffset) {
                    offsetCounter = 0
                    counter = 0
                    allowedToDraw = false
                    previousDate = nextDate
                    result.add(nextDate)
                } else {
                    offsetCounter++
                    result.add("")
                }
            } else {
                result.add("")
            }
            if (counter == minIndent) {
                counter = 0
                allowedToDraw = true
            }
        }
        //Log.d("info", "candleTime ${result.toString()}")
        return result
    }

    val candleTime = remember {
        mutableStateOf(parseCandleTime())
    }

    /**
     * Returns the minimum price on the visible section of the chart.
     *
     * @param scrollOffset the number of shifted candlesticks when scrolling.
     * @param visibleCandles the number of visible candles on the chart.
     */
    fun getMinPrice(scrollOffset: Int, visibleCandles: Int) : Float {
        var minPrice = candles[scrollOffset][3]
        val end: Int = if (visibleCandles < candles.size) visibleCandles+scrollOffset
        else candles.size - 1
        for (i in (0+scrollOffset) .. end) {
            if (candles[i][3] < minPrice) minPrice = candles[i][3]
        }
        return minPrice
    }

    /**
     * Returns the maximum price on the visible section of the chart.
     *
     * @param scrollOffset the number of shifted candlesticks when scrolling.
     * @param visibleCandles the number of visible candles on the chart.
     */
    fun getMaxPrice(scrollOffset: Int, visibleCandles: Int) : Float {
        var maxPrice = 0f
        val end: Int = if (visibleCandles < candles.size) visibleCandles+scrollOffset
        else candles.size - 1
        for (i in (0+scrollOffset) .. end) {
            if (candles[i][2] > maxPrice) maxPrice = candles[i][2]
        }
        return maxPrice
    }

    /**
     * Splits a string into parts in a list.
     *
     * @param s the string containing space delimiters.
     */
    fun stringToWords(s : String) = s.trim().splitToSequence(' ')
        .filter { it.isNotEmpty() } // or: .filter { it.isNotBlank() }
        .toMutableList()

    if (candles.isNotEmpty()) {
        val candlesCount = remember {
            mutableStateOf(candles.size)
        }
        //Log.d("debug", "candlesCount = ${candlesCount.value}")

        // to update the data
        val isUpdateInitial = remember {
            mutableStateOf(reDraw)
        }
        val isUpdateCurrent = reDraw

        // the number of shifted candlesticks when scrolling: Int
        var scrollOffset = 0

        // the minimum price on the visible section of the chart: Float
        val minPrice = remember {
            mutableStateOf(getMinPrice(scrollOffset, visibleCandles.value))
        }
        //Log.d("debug", "minPrice = ${minPrice.value}")

        // the maximum price on the visible section of the chart: Float
        val maxPrice = remember {
            mutableStateOf(getMaxPrice(scrollOffset, visibleCandles.value))
        }
        //Log.d("debug", "maxPrice = ${maxPrice.value}")

        // scroll position in pixels
        val scrollState = rememberScrollState()
        //Log.d("debug", "scrollState current: ${scrollState.value}, max: ${scrollState.maxValue}")

        // index of the selected candle: Int
        val selectedCandle = remember {
            mutableStateOf(-1)
        }
        if (isUpdateInitial.value != isUpdateCurrent) {
            Log.d("info", "updating the chart, scrollState.value=${scrollState.value}, candles.size=${candles.size}")
            isUpdateInitial.value = isUpdateCurrent
            selectedCandle.value = -1
        }

        // long press status: Boolean
        val isLongTap = remember {
            mutableStateOf(false)
        }

        // the state of the endButton: Boolean
        val isBtnEnd = remember {
            mutableStateOf(false)
        }

        // the transparency of the endButton: Boolean
        val btnEndAlpha = remember {
            mutableStateOf(0f)
        }

        // updating the chart when scrolling
        if (scrollState.isScrollInProgress){
            selectedCandle.value = -1
            if (candles.size >= (visibleCandles.value + 1))
                scrollOffset = minOf((scrollState.value / candleWithSpaceInPx.value).toInt(), (candles.size - (visibleCandles.value + 1)))
            else scrollOffset = (scrollState.value / candleWithSpaceInPx.value).toInt()
            //Log.d("debug", "scrollOffset = $scrollOffset")
            maxPrice.value = getMaxPrice(scrollOffset, visibleCandles.value)
            minPrice.value = getMinPrice(scrollOffset, visibleCandles.value)
            if (scrollState.value < scrollState.maxValue) btnEndAlpha.value = 1f
            else btnEndAlpha.value = 0f
        }

        // updating the chart when the endButton is pressed
        LaunchedEffect(key1 = candles, key2 = isBtnEnd.value) {
            Log.d("info", "LaunchedEffect(key1 = candles, key2 = isBtnEnd.value)")
            //selectedCandle.value = -1
            candleTime.value = parseCandleTime()
            scrollState.scrollTo(scrollState.maxValue)
            if (candles.size >= (visibleCandles.value + 1))
                scrollOffset = minOf((scrollState.value / candleWithSpaceInPx.value).toInt(), (candles.size - (visibleCandles.value + 1)))
            else scrollOffset = (scrollState.value / candleWithSpaceInPx.value).toInt()
            maxPrice.value = getMaxPrice(scrollOffset, visibleCandles.value)
            minPrice.value = getMinPrice(scrollOffset, visibleCandles.value)
            btnEndAlpha.value = 0f
        }

        // updating the chart when changing the display settings
        LaunchedEffect(key1 = reDraw) {
            Log.d("info", "LaunchedEffect(key1 = reDraw)")
            candlesCount.value = candles.size
            candleTime.value = parseCandleTime()
            selectedCandle.value = -1
            chartWidthInPx.value = chartWidth.toString().removeSuffix(".dp").toFloat().dpToFloat()
            rightBarWidthInPx.value = rightBarWidth.toString().removeSuffix(".dp").toFloat().dpToFloat()
            visibleCandles.value = (chartWidth / (candleWidth + gapWidth)).toBigDecimal().setScale(1, RoundingMode.DOWN).toInt() + 1
            candleWithSpaceInPx.value = (candleWidth + gapWidth).value * screenDensity
            candleWithSpaceInDp.value = candleWidth + gapWidth

            scrollState.scrollTo(scrollState.maxValue)
            if (candles.size >= (visibleCandles.value + 1))
                scrollOffset = minOf((scrollState.value / candleWithSpaceInPx.value).toInt(), (candles.size - (visibleCandles.value + 1)))
            else scrollOffset = (scrollState.value / candleWithSpaceInPx.value).toInt()
            maxPrice.value = getMaxPrice(scrollOffset, visibleCandles.value)
            minPrice.value = getMinPrice(scrollOffset, visibleCandles.value)
            btnEndAlpha.value = 0f
        }

        // updating the chart when the number of candles changes
        LaunchedEffect(key1 = liveUpdate) {
            Log.d("info", "LaunchedEffect(key1 = liveUpdate)")
            candlesCount.value = candles.size
            candleTime.value = parseCandleTime()
            if(scrollState.maxValue - scrollState.value < candleWithSpaceInPx.value * 3) {
                scrollState.scrollTo(scrollState.maxValue)
                if (candles.size >= (visibleCandles.value + 1))
                    scrollOffset = minOf((scrollState.value / candleWithSpaceInPx.value).toInt(), (candles.size - (visibleCandles.value + 1)))
                else scrollOffset = (scrollState.value / candleWithSpaceInPx.value).toInt()
                maxPrice.value = getMaxPrice(scrollOffset, visibleCandles.value)
                minPrice.value = getMinPrice(scrollOffset, visibleCandles.value)
                btnEndAlpha.value = 0f
            }
        }

        val currentPrice = candles[candles.size-1][1]


        ConstraintLayout() {
            Column(modifier = Modifier
                .size(width = chartWidth, height = chartHeight)
                .background(backgroundColor)
                .horizontalScroll(state = scrollState, reverseScrolling = false)
                .width((candleWithSpaceInDp.value) * candles.size + rightBarWidth)
                .pointerInput(Unit) {
                    while (true) {
                        detectTapGestures(onLongPress = {
                            isLongTap.value = true
                            val selectedPos = it.x
                            //Log.d("debug", "selectedPos: " + selectedPos)
                            if (scrollState.value != 0) {
                                if (selectedPos < (chartWidthInPx.value - rightBarWidthInPx.value + scrollState.value - 1)) { //-1 из-за округления IndexOutOfBoundsException
                                    selectedCandle.value =
                                        (selectedPos / (candleWithSpaceInDp.value)
                                            .toString()
                                            .removeSuffix(".dp")
                                            .toFloat()
                                            .dpToFloat()).toInt()
                                }
                            } else {
                                if (selectedPos < (candleWithSpaceInPx.value * candlesCount.value - 1)) { //-1 из-за округления IndexOutOfBoundsException
                                    selectedCandle.value =
                                        (selectedPos / (candleWithSpaceInDp.value)
                                            .toString()
                                            .removeSuffix(".dp")
                                            .toFloat()
                                            .dpToFloat()).toInt()
                                }
                            }
                        }) {
                            //Log.d("debug", "detectTapGestures " + it.x)
                        }
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (isLongTap.value) {
                                val selectedPos = stringToWords(event.changes.toString())[2]
                                    .drop(16)
                                    .dropLast(3)
                                    .toInt()
                                //Log.d("debug", "selectedPos: " + selectedPos)
                                if (scrollState.value != 0) {
                                    if (selectedPos < (chartWidthInPx.value - rightBarWidthInPx.value + scrollState.value - 1)) { //-1 из-за округления IndexOutOfBoundsException
                                        selectedCandle.value =
                                            (selectedPos / (candleWithSpaceInDp.value)
                                                .toString()
                                                .removeSuffix(".dp")
                                                .toFloat()
                                                .dpToFloat()).toInt()
                                    }
                                } else {
                                    if (selectedPos < (candleWithSpaceInPx.value * candlesCount.value - 1)) { //-1 из-за округления IndexOutOfBoundsException
                                        selectedCandle.value =
                                            (selectedPos / (candleWithSpaceInDp.value)
                                                .toString()
                                                .removeSuffix(".dp")
                                                .toFloat()
                                                .dpToFloat()).toInt()
                                    }
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            awaitFirstDown()
                            // ACTION_DOWN
                            do {
                                val event: PointerEvent = awaitPointerEvent()
                                // ACTION_MOVE loop
                                //Log.d("debug", "event1 " + event.toString())
                                // Consuming event prevents other gestures or scroll to intercept
                                event.changes.forEach { pointerInputChange: PointerInputChange ->
                                    //pointerInputChange.consumePositionChange()
                                    //Log.d("debug", "event2 " + event.toString())
                                }
                            } while (event.changes.any { it.pressed })
                            // ACTION_UP
                            isLongTap.value = false
                        }
                    }
                }
                .drawBehind {
                    //val chartSize = size
                    //val chartSize = Size(width = size.width, height = size.height)
                    //val chartSize = Size(width = size.width * 0.9f, height = size.height * 0.94f)
                    val chartSize = Size(width = size.width, height = size.height * 0.94f) // !!
                    backgroundCanvas(
                        candles = candles,
                        maxPrice = maxPrice.value,
                        minPrice = minPrice.value,
                        componentSize = chartSize,
                        priceTagsCount = priceTagsCount,
                        priceColor = priceColor,
                        selectedColor = selectedColor,
                        rightBarWidth = rightBarWidth,
                        candleWidth = candleWidth,
                        gapWidth = gapWidth,
                        bottomBarHeight = bottomBarHeight
                            .toString()
                            .removeSuffix(".dp")
                            .toFloat()
                            .dpToFloat(),
                        positiveCandleColor = positiveCandleColor,
                        negativeCandleColor = negativeCandleColor,
                        dojiCandleColor = dojiCandleColor,
                        separatorColor = separatorColor,
                        topOffset = topOffset.toPx(),
                        selectedCandle = selectedCandle.value,
                        priceLineThickness = priceLineThickness,
                        priceLineStyle = priceLineStyle,
                        selectedLineThickness = selectedLineThickness,
                        dojiCandleThickness = dojiCandleThickness,
                        selectedLineStyle = selectedLineStyle
                    )
                }) {

                //Log.d("debug", "test = " + (maxPrice - candles[candles.size-1][1])/(maxPrice-minPrice))
                BoxWithConstraints(
                    Modifier
                        //.background(color = Color.Blue)
                        //.padding(20.dp)
                    ) {
                    //val boxWidth = this.maxWidth

                    for (i in candleTime.value.indices) {
                        if (candleTime.value[i] != "") {
                            Text(
                                text = candleTime.value[i],
                                color = textColor,
                                fontSize = 12.sp,
                                modifier = Modifier.offset(
                                    x = ((candleWidth + gapWidth)*i),
                                    y = chartHeight-16.dp))
                            Canvas(modifier = Modifier
                                .offset(x = 0.dp, y = 0.dp)) {
                                drawLine(
                                    color = separatorColor,
                                    start = Offset(
                                        x = ((candleWidth + gapWidth)*i).toPx() - (gapWidth * 0.5f).toPx(),
                                        y = 0f
                                    ),
                                    end = Offset(
                                        x = ((candleWidth + gapWidth)*i).toPx() - (gapWidth * 0.5f).toPx(),
                                        y = chartHeight.toPx()
                                    ),
                                    strokeWidth = 0.5.dp.toPx(),
                                    //pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 10f, 10f, 10f), phase = 0f)
                                )
                            }
                        }
                    }

                    if (selectedCandle.value >= 0) {
                        var selectedDate = ""
                        for (j in selectedTimeFormat) {
                            if (j.isDigitsOnly()) {
                                selectedDate += timestamps[selectedCandle.value][j.toInt()]
                            } else {
                                selectedDate += j
                            }
                        }

                        var size by remember { mutableStateOf(Size.Zero)}

                        BoxWithConstraints(
                            contentAlignment = Alignment.BottomStart,
                            modifier = Modifier
                                .offset(
                                    x = ((candleWidth + gapWidth) * selectedCandle.value) - size.width.pxToDp() / 2,
                                    y = chartHeight - 16.dp
                                )
                                .background(selectedColor)
                                .padding(2.dp, 0.dp)
                                .onGloballyPositioned { coordinates ->
                                    size = coordinates.size.toSize()
                                }
                        ) {
                            if (selectedCandle.value >= 0) {
                                Text(
                                    text = "$selectedDate",
                                    color = textColor,
                                    //style = TextStyle(background = selectedColor),
                                    fontSize = 12.sp,
                                )
                            }
                        }

                    }
                }
            }
            OutlinedButton(
                onClick = {
                    isBtnEnd.value = !(isBtnEnd.value)
                    Log.d("debug", isBtnEnd.value.toString())
                },
                modifier = Modifier
                    .alpha(btnEndAlpha.value)
                    .size(endButtonSize)
                    .offset(
                        x = chartWidth - rightBarWidth - endButtonSize - 8.dp,
                        y = chartHeight - bottomBarHeight - endButtonSize
                    ),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.Black),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = endButtonColor,
                    contentColor =  Color.Black)
            ) {
                Text(text = ">")
            }

            Canvas(modifier = Modifier
                .offset(x = 0.dp, y = 0.dp)
            ) {
                drawRect(
                    color = rightBarColor,
                    topLeft = Offset(
                        x = chartWidth.toPx() - rightBarWidth.toPx(),
                        y = 0f
                    ),
                    size = Size(
                        width = rightBarWidth.toPx(),
                        height = chartHeight.toPx()
                    )
                )
            }

            // maximum price on visible part of the chart
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(
                        x = chartWidth * 1.0f - rightBarWidth,
                        y = topOffset - rightBarTextSize.dp / 1.5f - 2.dp
                    )
                    //.background(priceColor)
                    .padding(2.dp, 1.dp)
            ) {
                Text(
                    text = "${maxPrice.value.toBigDecimal().setScale(significantDigits, RoundingMode.HALF_UP)}",
                    color = textColor,
                    //style = TextStyle(background = priceLineColor),
                    fontSize = rightBarTextSize.sp,
                )
            }

            // minimum price on visible part of the chart
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(
                        x = chartWidth * 1.0f - rightBarWidth,
                        y = (chartHeight * 0.94f - bottomBarHeight) + topOffset - rightBarTextSize.dp / 1.5f - 2.dp
                    )
                    //.background(priceColor)
                    .padding(2.dp, 1.dp)
            ) {
                Text(
                    text = "${minPrice.value.toBigDecimal().setScale(significantDigits, RoundingMode.HALF_UP)}",
                    color = textColor,
                    //style = TextStyle(background = priceLineColor),
                    fontSize = rightBarTextSize.sp,
                )
            }

            // intermediate prices
            for (i in 1 until priceTagsCount + 1) {
                val price = minPrice.value + (maxPrice.value - minPrice.value) * i / (priceTagsCount + 1)
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(
                            x = chartWidth * 1.0f - rightBarWidth,
                            y = (chartHeight * 0.94f - bottomBarHeight) * (maxPrice.value - price) / (maxPrice.value - minPrice.value) + topOffset - rightBarTextSize.dp / 1.5f - 2.dp
                        )
                        //.background(priceColor)
                        .padding(2.dp, 1.dp)
                ) {
                    Text(
                        text = "${price.toBigDecimal().setScale(significantDigits, RoundingMode.HALF_UP)}",
                        color = textColor,
                        //style = TextStyle(background = priceLineColor),
                        fontSize = rightBarTextSize.sp,
                    )
                }

            }

            // last price
            if (candles[candles.size-1][1] < maxPrice.value && candles[candles.size-1][1] > minPrice.value) {
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(
                            x = chartWidth * 1.0f - rightBarWidth,
                            y = (chartHeight * 0.94f - bottomBarHeight) * (maxPrice.value - candles[candles.size - 1][1]) / (maxPrice.value - minPrice.value) + topOffset - rightBarTextSize.dp / 1.5f - 2.dp
                        )
                        .background(priceColor)
                        .padding(2.dp, 1.dp)
                ) {
                    Text(
                        text = "${currentPrice.toBigDecimal().setScale(significantDigits, RoundingMode.HALF_UP)}",
                        color = textColor,
                        //style = TextStyle(background = priceLineColor),
                        fontSize = rightBarTextSize.sp,
                    )
                }
            }

            if (selectedCandle.value >= 0) {
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(
                            x = chartWidth * 1.0f - rightBarWidth,
                            y = (chartHeight * 0.94f - bottomBarHeight) * (maxPrice.value - candles[selectedCandle.value][1]) / (maxPrice.value - minPrice.value) + topOffset - rightBarTextSize.dp / 1.5f - 2.dp
                        )
                        .background(selectedColor)
                        .padding(2.dp, 1.dp)
                ) {
                    if (selectedCandle.value >= 0) {
                        Text(
                            text = "${
                                candles[selectedCandle.value][1].toBigDecimal()
                                    .setScale(significantDigits, RoundingMode.HALF_UP)
                            }",
                            color = textColor,
                            //style = TextStyle(background = selectedColor),
                            fontSize = rightBarTextSize.sp,
                        )
                    }
                }
            }
        }
    } else {
        ConstraintLayout {
            Box(modifier = Modifier
                .size(width = chartWidth, height = chartHeight)
                .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(modifier = Modifier
                    .fillMaxWidth(1f),
                    textAlign = TextAlign.Center,
                    text = "No data",
                    color = textColor,
                    fontSize = 24.sp)
            }
        }
    }
}

fun DrawScope.backgroundCanvas(
    candles: MutableList<List<Float>>,
    maxPrice: Float,
    minPrice: Float,
    componentSize: Size,
    priceTagsCount: Int,
    priceColor: Color,
    selectedColor: Color,
    rightBarWidth: Dp,
    candleWidth: Dp,
    gapWidth: Dp,
    bottomBarHeight: Float,
    positiveCandleColor: Color,
    negativeCandleColor: Color,
    dojiCandleColor: Color,
    separatorColor: Color,
    topOffset: Float,
    selectedCandle: Int,
    priceLineThickness: Dp,
    priceLineStyle: FloatArray,
    selectedLineThickness: Dp,
    dojiCandleThickness: Dp,
    selectedLineStyle: FloatArray

) {
    val candleAreaHeight = componentSize.height - bottomBarHeight
    val deltaPrice = maxPrice - minPrice
    val candleAndGapWidth = candleWidth + gapWidth

    // intermediate prices lines
    for (i in 1 until priceTagsCount + 1) {
        val price = minPrice + deltaPrice * i / (priceTagsCount + 1)
        drawLine(
            color = separatorColor,
            start = Offset(
                x = 0f,
                y = candleAreaHeight - (price - minPrice) / deltaPrice * candleAreaHeight + topOffset
            ),
            end = Offset(
                x = (componentSize.width - rightBarWidth.toPx()),
                y = candleAreaHeight - (price - minPrice) / deltaPrice * candleAreaHeight + topOffset
            ),
            strokeWidth = 0.5.dp.toPx(),
            //pathEffect = PathEffect.dashPathEffect(selectedLineStyle, phase = 0f)
        )
    }

    // minimum price line
    drawLine(
        color = separatorColor,
        start = Offset(
            x = 0f,
            y = candleAreaHeight + topOffset
        ),
        end = Offset(
            x = (componentSize.width - rightBarWidth.toPx()),
            y = candleAreaHeight + topOffset
        ),
        strokeWidth = 0.5.dp.toPx(),
        //pathEffect = PathEffect.dashPathEffect(selectedLineStyle, phase = 0f)
    )

    for (i in candles.indices) {

        // green candle
        if (candles[i][0] < candles[i][1]) {
            drawRect( // candle body
                color = positiveCandleColor,
                topLeft = Offset(
                    x = (candleAndGapWidth * i).toPx(),
                    y = candleAreaHeight - (candles[i][0] - minPrice) / deltaPrice * candleAreaHeight + topOffset),
                size = Size(
                    width = candleWidth.toPx(),
                    height = (candles[i][0] - candles[i][1]) / deltaPrice * candleAreaHeight),
            )
            drawRect( // candle shadow
                color = positiveCandleColor,
                topLeft = Offset(
                    x = (candleAndGapWidth * i).toPx() + (candleWidth * 0.45f).toPx(),
                    y = candleAreaHeight - (candles[i][2] - minPrice) / deltaPrice * candleAreaHeight + topOffset),
                size = Size(
                    width = candleWidth.toPx() * 0.1f,
                    height = (candles[i][2] - candles[i][3]) / deltaPrice * candleAreaHeight),
            )
        }

        // red candle
        else if (candles[i][0] > candles[i][1]) {
            drawRect( // candle body
                color = negativeCandleColor,
                topLeft = Offset(
                    x = (candleAndGapWidth * i).toPx(),
                    y = candleAreaHeight - (candles[i][0] - minPrice) / deltaPrice * candleAreaHeight + topOffset),
                size = Size(
                    width = candleWidth.toPx(),
                    height = (candles[i][0] - candles[i][1]) / deltaPrice * candleAreaHeight),
            )
            drawRect( // candle shadow
                color = negativeCandleColor,
                topLeft = Offset(
                    x = (candleAndGapWidth * i).toPx() + (candleWidth * 0.45f).toPx(),
                    y = candleAreaHeight - (candles[i][2] - minPrice) / deltaPrice * candleAreaHeight + topOffset),
                size = Size(
                    width = candleWidth.toPx() * 0.1f,
                    height = (candles[i][2] - candles[i][3]) / deltaPrice * candleAreaHeight),
            )
        }

        // doji candle
        else if (candles[i][0] == candles[i][1]) {
            drawRect( // candle body
                color = dojiCandleColor,
                topLeft = Offset(
                    x = (candleAndGapWidth * i).toPx(),
                    y = candleAreaHeight - (candles[i][0] - minPrice) / deltaPrice * candleAreaHeight + topOffset),
                size = Size(
                    width = candleWidth.toPx(),
                    height = dojiCandleThickness.toPx()),
            )
            drawRect( // candle shadow
                color = dojiCandleColor,
                topLeft = Offset(
                    x = (candleAndGapWidth * i).toPx() + (candleWidth * 0.45f).toPx(),
                    y = candleAreaHeight - (candles[i][2] - minPrice) / deltaPrice * candleAreaHeight + topOffset),
                size = Size(
                    width = candleWidth.toPx() * 0.1f,
                    height = (candles[i][2] - candles[i][3]) / deltaPrice * candleAreaHeight),
            )
        }
    }

    // perpendicular to the last price
    if (candles[candles.size-1][1] < maxPrice && candles[candles.size-1][1] > minPrice) {
        drawLine(
            color = priceColor,
            start = Offset(
                x = 0f,
                y = candleAreaHeight - (candles[candles.size - 1][1] - minPrice) / deltaPrice * candleAreaHeight + topOffset
            ),
            end = Offset(
                x = componentSize.width - rightBarWidth.toPx(),
                y = candleAreaHeight - (candles[candles.size - 1][1] - minPrice) / deltaPrice * candleAreaHeight + topOffset
            ),
            strokeWidth = priceLineThickness.toPx(),
            pathEffect = PathEffect.dashPathEffect(priceLineStyle, phase = 0f)
        )
    }

    if (selectedCandle >= 0) {
        //Log.d("debug", (candleAndGapWidth * selectedCandle + (candleWidth / 2)).toString().removeSuffix(".dp").toFloat().dpToFloat().toString())

        // perpendicular to the date/time panel
        drawLine(
            color = selectedColor,
            start = Offset(
                x = (candleAndGapWidth * selectedCandle + (candleWidth / 2)).toPx(),
                y = 0f
            ),
            end = Offset(
                x = (candleAndGapWidth * selectedCandle + (candleWidth / 2)).toPx(),
                y = componentSize.height + topOffset
            ),
            strokeWidth = selectedLineThickness.toPx(),
            pathEffect = PathEffect.dashPathEffect(selectedLineStyle, phase = 0f)
        )

        // perpendicular to the price panel
        drawLine(
            color = selectedColor,
            start = Offset(
                x = 0f,
                y = candleAreaHeight - (candles[selectedCandle][1] - minPrice) / deltaPrice * candleAreaHeight + topOffset
            ),
            end = Offset(
                x = (componentSize.width - rightBarWidth.toPx()),
                y = candleAreaHeight - (candles[selectedCandle][1] - minPrice) / deltaPrice * candleAreaHeight + topOffset
            ),
            strokeWidth = selectedLineThickness.toPx(),
            pathEffect = PathEffect.dashPathEffect(selectedLineStyle, phase = 0f)
        )
    }
}

// dp(Dp) → sp(TextUnit)
@Composable
internal fun Dp.dpToSp(): TextUnit {
    return (this.value * LocalDensity.current.density / LocalDensity.current.fontScale).sp
}

// px(Float) → dp(Dp)
@Composable
internal fun Float.pxToDp(): Dp {
    return (this / LocalDensity.current.density).dp
}

//@Composable
internal fun Float.dpToFloat(): Float {
    val scale = Resources.getSystem().displayMetrics.density
    return (this * scale)
}

//@Composable
internal fun Float.FloatToDp(): Float {
    val scale = Resources.getSystem().displayMetrics.density
    return (this / scale)
}

//@Composable
//@Preview(showBackground = true)
//fun CustomComponentPreview() {
//    CandlestickChart()
//}
