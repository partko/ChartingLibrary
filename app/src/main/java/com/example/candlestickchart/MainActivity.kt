package com.example.candlestickchart

import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.candlestickchart.ui.theme.CandlestickChartTheme
import com.example.chartinglibrary.TimeData
import com.example.chartinglibrary.area.AreaChart
import com.example.chartinglibrary.area.AreaFeed
import com.example.chartinglibrary.candle.CandleFeed
import com.example.chartinglibrary.candle.CandlestickChart
import org.json.JSONException
import org.json.JSONObject
import kotlin.random.Random

const val API_KEY = "fVYFzwwxhgYGobQCWje8h9oYE5pufXvm"

class MainActivity : ComponentActivity() {
    lateinit var mainHandler: Handler

    private fun requestData(
        ticker: String,
        multiplier: String,
        timespan: String,
        from: String,
        to: String,
        limit: String,
        candlesFeed: MutableState<MutableList<CandleFeed>>,
        areaFeed: MutableState<MutableList<AreaFeed>>,
        timeFormat: MutableState<List<String>>,
        reDraw: MutableState<Boolean>) {
        //https://api.polygon.io/v2/aggs/ticker/AAPL/range/1/day/2023-01-09/2023-01-09?adjusted=true&sort=asc&limit=120&apiKey=fVYFzwwxhgYGobQCWje8h9oYE5pufXvm
        val url = "https://api.polygon.io/v2/aggs/ticker/$ticker/range/$multiplier/$timespan/$from/$to?adjusted=true&sort=asc&limit=$limit&apiKey=$API_KEY"
        val queue = Volley.newRequestQueue(this)
        val request = StringRequest(
            Request.Method.GET,
            url,
            {
                //result -> Log.d("debug", "Result: $result")
                //result -> parseData(result)
                result ->
                val (list, list1) = parseData(result)
                candlesFeed.value = list
                areaFeed.value = list1

                when (timespan) {
                    "minute" -> timeFormat.value = listOf(TimeData.HOUR.index, ":00")
                    "hour" -> timeFormat.value = listOf(TimeData.DAY.index, " ", TimeData.MONTH_SHORT.index)
                    "day" -> timeFormat.value = listOf(TimeData.MONTH_FULL.index)
                    "week" -> timeFormat.value = listOf(TimeData.MONTH_SHORT.index)
                    "month" -> timeFormat.value = listOf(TimeData.YEAR.index)
                    "quarter" -> timeFormat.value = listOf(TimeData.YEAR.index)
                    "year" -> timeFormat.value = listOf(TimeData.YEAR.index)
                    else -> {
                        timeFormat.value = listOf<String>()
                    }
                }
                reDraw.value = !reDraw.value
            },
            {
                    error -> Log.d("debug", "Request Error: $error")
            }
        )
        queue.add(request)
    }

    private fun parseData(result: String): Pair<MutableList<CandleFeed>, MutableList<AreaFeed>> {
        val root = JSONObject(result)
        try {
            val results = root.getJSONArray("results")
            val candles = mutableListOf<CandleFeed>()
            val area = mutableListOf<AreaFeed>()
            Log.d("debug", results.length().toString())
            for (i in 0 until results.length()) {
                val currentCandle = results.getJSONObject(i)
                candles.add(
                    CandleFeed(
                        currentCandle.getString("o").toFloat(),
                        currentCandle.getString("c").toFloat(),
                        currentCandle.getString("h").toFloat(),
                        currentCandle.getString("l").toFloat(),
                        currentCandle.getString("t").toString()
                    )
                )
                area.add(
                    AreaFeed(
                        currentCandle.getString("o").toFloat(),
                        currentCandle.getString("t").toString()
                    )
                )
            }
            //Log.d("debug", "Result: ${candles[1][0]}")
            //showList(candles)
            if (candles.size > 3000) candles.removeRange(3000..candles.size)
            if (area.size > 3000) area.removeRange(3000..area.size)
            return Pair(candles, area)
        } catch (e: JSONException) {
            return Pair(mutableListOf<CandleFeed>(), mutableListOf<AreaFeed>())
        }
    }

    fun showList(list: MutableList<MutableList<Float>>) {
        for (row in list) {
            Log.d("debug", row.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CandlestickChartTheme {
                val showSettings = remember{mutableStateOf(false)}

                val reDraw = remember{mutableStateOf(false)}
                val liveUpdate = remember{mutableStateOf(false)}

                val ticker = remember{mutableStateOf("NVDA")}
                val from = remember{mutableStateOf("2023-03-01")}
                val to = remember{mutableStateOf("2023-05-20")}
                val timespan = remember{mutableStateOf("hour")}

                val startPrice = remember{mutableStateOf("4300")}
                val endPrice = remember{mutableStateOf("5700")}
                val candleCount = remember{mutableStateOf("500")}
                val generateNew = remember{mutableStateOf("true")}

                val candleFeed = remember { mutableStateOf(mutableListOf<CandleFeed>()) }
                val areaFeed = remember { mutableStateOf(mutableListOf<AreaFeed>()) }
                val timeFormat = remember { mutableStateOf(listOf<String>()) }
                val selectedTimeFormat = remember { mutableStateOf(listOf<String>()) }

                mainHandler = Handler(Looper.getMainLooper())

                val chartWidth = remember{mutableStateOf(1f)}
                val chartHeight = remember{mutableStateOf(0.5f)}
                val rightBarWidth = remember{mutableStateOf(50.dp)}
                val bottomBarHeight = remember{mutableStateOf(20.dp)}
                val candleWidth = remember{mutableStateOf(8.dp)}
                val gapWidth = remember{mutableStateOf(2.dp)}
                val priceLineThickness = remember{mutableStateOf(1.dp)}
                val selectedLineThickness = remember{mutableStateOf(1.dp)}
                val dojiCandleThickness = remember{mutableStateOf(1.dp)}
                val endButtonSize = remember{mutableStateOf(20.dp)}
                val topOffset = remember{mutableStateOf(8.dp)}
                val rightBarTextSize = remember{mutableStateOf(10)}
                val significantDigits = remember{mutableStateOf(2)}
                val priceTagsCount = remember{mutableStateOf(4)}
                val minIndent = remember{mutableStateOf(12)}
                val dateOffset = remember{mutableStateOf(1)}

                val backgroundColor = remember{mutableStateOf(listOf("41", "49", "51", "255"))}
                val rightBarColor = remember{mutableStateOf(listOf("37", "44", "46", "255"))}
                val positiveCandleColor = remember{mutableStateOf(listOf("0", "255", "0", "255"))}
                val negativeCandleColor = remember{mutableStateOf(listOf("255", "0", "0", "255"))}
                val dojiCandleColor = remember{mutableStateOf(listOf("255", "255", "255", "255"))}
                val endButtonColor = remember{mutableStateOf(listOf("255", "255", "255", "255"))}
                val priceColor = remember{mutableStateOf(listOf("0", "128", "255", "255"))}
                val selectedColor = remember{mutableStateOf(listOf("106", "90", "205", "255"))}
                val textColor = remember{mutableStateOf(listOf("255", "255", "255", "255"))}
                val separatorColor = remember{mutableStateOf(listOf("71", "74", "81", "255"))}

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(modifier = Modifier
                        .verticalScroll(rememberScrollState())
                    ) {
                        CandlestickChart(
                            candleFeed = candleFeed.value,
                            timeFormat = timeFormat.value,
                            selectedTimeFormat = selectedTimeFormat.value,
                            priceTagsCount = priceTagsCount.value,
                            minIndent = minIndent.value,
                            dateOffset = dateOffset.value,
                            chartWidth = GetWidth() * chartWidth.value,
                            chartHeight = GetHeight() * chartHeight.value,
                            rightBarWidth = rightBarWidth.value,
                            bottomBarHeight = bottomBarHeight.value,
                            candleWidth = candleWidth.value,
                            gapWidth = gapWidth.value,
                            topOffset = topOffset.value,
                            rightBarTextSize = rightBarTextSize.value,
                            significantDigits = significantDigits.value,
                            priceLineThickness = priceLineThickness.value,
                            //priceLineStyle = floatArrayOf(30f, 10f, 10f, 10f),
                            selectedLineThickness = selectedLineThickness.value,
                            dojiCandleThickness = dojiCandleThickness.value,
                            //selectedLineStyle = floatArrayOf(10f, 10f),
                            endButtonSize = endButtonSize.value,
                            backgroundColor = Color(backgroundColor.value[0].toInt(), backgroundColor.value[1].toInt(), backgroundColor.value[2].toInt(), backgroundColor.value[3].toInt()),
                            rightBarColor = Color(rightBarColor.value[0].toInt(), rightBarColor.value[1].toInt(), rightBarColor.value[2].toInt(), rightBarColor.value[3].toInt()),
                            textColor = Color(textColor.value[0].toInt(), textColor.value[1].toInt(), textColor.value[2].toInt(), textColor.value[3].toInt()),
                            separatorColor = Color(separatorColor.value[0].toInt(), separatorColor.value[1].toInt(), separatorColor.value[2].toInt(), separatorColor.value[3].toInt()),
                            priceColor = Color(priceColor.value[0].toInt(), priceColor.value[1].toInt(), priceColor.value[2].toInt(), priceColor.value[3].toInt()),
                            selectedColor = Color(selectedColor.value[0].toInt(), selectedColor.value[1].toInt(), selectedColor.value[2].toInt(), selectedColor.value[3].toInt()),
                            positiveCandleColor = Color(positiveCandleColor.value[0].toInt(), positiveCandleColor.value[1].toInt(), positiveCandleColor.value[2].toInt(), positiveCandleColor.value[3].toInt()),
                            negativeCandleColor = Color(negativeCandleColor.value[0].toInt(), negativeCandleColor.value[1].toInt(), negativeCandleColor.value[2].toInt(), negativeCandleColor.value[3].toInt()),
                            dojiCandleColor = Color(dojiCandleColor.value[0].toInt(), dojiCandleColor.value[1].toInt(), dojiCandleColor.value[2].toInt(), dojiCandleColor.value[3].toInt()),
                            endButtonColor = Color(endButtonColor.value[0].toInt(), endButtonColor.value[1].toInt(), endButtonColor.value[2].toInt(), endButtonColor.value[3].toInt()),
                            reDraw = reDraw.value,
                            liveUpdate = liveUpdate.value
                        )
                        AreaChart(
                            areaFeed = areaFeed.value,
                            timeFormat = timeFormat.value,
                            selectedTimeFormat = selectedTimeFormat.value,
                            priceTagsCount = priceTagsCount.value,
                            minIndent = minIndent.value,
                            dateOffset = dateOffset.value,
                            chartWidth = GetWidth() * chartWidth.value,
                            chartHeight = GetHeight() * chartHeight.value,
                            rightBarWidth = rightBarWidth.value,
                            bottomBarHeight = bottomBarHeight.value,
                            candleWidth = candleWidth.value,
                            gapWidth = gapWidth.value,
                            topOffset = topOffset.value,
                            rightBarTextSize = rightBarTextSize.value,
                            significantDigits = significantDigits.value,
                            priceLineThickness = priceLineThickness.value,
                            //priceLineStyle = floatArrayOf(30f, 10f, 10f, 10f),
                            selectedLineThickness = selectedLineThickness.value,
                            //selectedLineStyle = floatArrayOf(10f, 10f),
                            endButtonSize = endButtonSize.value,
                            backgroundColor = Color(backgroundColor.value[0].toInt(), backgroundColor.value[1].toInt(), backgroundColor.value[2].toInt(), backgroundColor.value[3].toInt()),
                            rightBarColor = Color(rightBarColor.value[0].toInt(), rightBarColor.value[1].toInt(), rightBarColor.value[2].toInt(), rightBarColor.value[3].toInt()),
                            textColor = Color(textColor.value[0].toInt(), textColor.value[1].toInt(), textColor.value[2].toInt(), textColor.value[3].toInt()),
                            separatorColor = Color(separatorColor.value[0].toInt(), separatorColor.value[1].toInt(), separatorColor.value[2].toInt(), separatorColor.value[3].toInt()),
                            priceColor = Color(priceColor.value[0].toInt(), priceColor.value[1].toInt(), priceColor.value[2].toInt(), priceColor.value[3].toInt()),
                            selectedColor = Color(selectedColor.value[0].toInt(), selectedColor.value[1].toInt(), selectedColor.value[2].toInt(), selectedColor.value[3].toInt()),
                            endButtonColor = Color(endButtonColor.value[0].toInt(), endButtonColor.value[1].toInt(), endButtonColor.value[2].toInt(), endButtonColor.value[3].toInt()),
                            reDraw = reDraw.value,
                            liveUpdate = liveUpdate.value
                        )
                        Text(text = "", modifier = Modifier.height(2.dp))
                        Row {
                            TextField(
                                modifier = Modifier
                                    .width(GetWidth() / 2),
                                label = {Text("Ticker")},
                                value = ticker.value,
                                onValueChange = { newText -> ticker.value = newText },
                                placeholder = { Text("AAPL") }
                            )
                            var expandedTimespan by remember { mutableStateOf(false) }
                            val timespanList = listOf("minute", "hour", "day", "week", "month", "quarter", "year")
                            Column {
                                TextField(
                                    enabled = false,
                                    value = timespan.value,
                                    onValueChange = { timespan.value = it },
                                    modifier = Modifier
                                        //.textColor
                                        .clickable { expandedTimespan = !expandedTimespan }
                                        .fillMaxWidth(),
                                    label = {Text(text = "Timespan")},
                                )
                                DropdownMenu(
                                    expanded = expandedTimespan,
                                    onDismissRequest = { expandedTimespan = false }
                                ) {
                                    timespanList.forEach { label ->
                                        DropdownMenuItem(onClick = {
                                            timespan.value = label
                                            expandedTimespan = false
                                        }) {
                                            Text(text = label)
                                        }
                                    }
                                }
                            }
                        }
                        Row {
                            TextField(
                                modifier = Modifier
                                    .width(GetWidth() / 2),
                                label = {Text("From")},
                                value = from.value,
                                onValueChange = { newText -> from.value = newText },
                                placeholder = { Text("2023-03-01") }
                            )
                            TextField(
                                modifier = Modifier
                                    .width(GetWidth() / 2),
                                label = {Text("To")},
                                value = to.value,
                                onValueChange = {newText -> to.value = newText},
                                placeholder = { Text("2023-05-20") }
                            )
                        }
                        Button(modifier = Modifier
                            .width(GetWidth()),
                            onClick = {
                                mainHandler.removeCallbacksAndMessages(null)
                                selectedTimeFormat.value = listOf(TimeData.DAY.index, " ", TimeData.MONTH_SHORT.index, " ", TimeData.HOUR.index, ":", TimeData.MINUTE.index)
                                requestData(ticker.value, "1", timespan.value, from.value, to.value, "50000", candleFeed, areaFeed, timeFormat, reDraw)
                            }){
                            Text(text = "Upload Stock Data")
                        }

                        val newStartPrice = remember{ mutableStateOf(startPrice.value) }
                        val newEndPrice = remember{ mutableStateOf(endPrice.value) }
                        val newCandleCount = remember{ mutableStateOf(candleCount.value) }
                        val newGenerateNew = remember{ mutableStateOf(generateNew.value) }
                        Row {
                            TextField(
                                modifier = Modifier
                                    .width(GetWidth() / 2),
                                label = {Text("Start Price")},
                                value = newStartPrice.value,
                                onValueChange = { newText -> newStartPrice.value = newText },
                                placeholder = { Text("4300") }
                            )
                            TextField(
                                modifier = Modifier
                                    .width(GetWidth() / 2),
                                label = {Text("End Price")},
                                value = newEndPrice.value,
                                onValueChange = {newText -> newEndPrice.value = newText},
                                placeholder = { Text("5700") }
                            )
                        }
                        Row {
                            TextField(
                                modifier = Modifier
                                    .width(GetWidth() / 2),
                                label = {Text("Candle Count")},
                                value = newCandleCount.value,
                                onValueChange = { newText -> newCandleCount.value = newText },
                                placeholder = { Text("500") }
                            )

                            var mExpanded by remember { mutableStateOf(false) }
                            val booleanList = listOf("true", "false")
                            Column {
                                TextField(
                                    enabled = false,
                                    value = newGenerateNew.value,
                                    onValueChange = { newGenerateNew.value = it },
                                    modifier = Modifier
                                        //.textColor
                                        .clickable { mExpanded = !mExpanded }
                                        .fillMaxWidth(),
                                    label = {Text(text = "Generate new every second")},
                                )
                                DropdownMenu(
                                    expanded = mExpanded,
                                    onDismissRequest = { mExpanded = false }
                                ) {
                                    booleanList.forEach { label ->
                                        DropdownMenuItem(onClick = {
                                            newGenerateNew.value = label
                                            mExpanded = false
                                        }) {
                                            Text(text = label)
                                        }
                                    }
                                }
                            }

                        }
                        Button(modifier = Modifier
                            .width(GetWidth()),
                            onClick = {
                                // ConstraintLayout limit is no more than 260000 pixels
                                if (newCandleCount.value.toInt() * (candleWidth.value.value.dpToFloat() + gapWidth.value.value.dpToFloat()) + rightBarWidth.value.value.dpToFloat() < 260000) {
                                    mainHandler.removeCallbacksAndMessages(null)
                                    selectedTimeFormat.value = listOf(TimeData.DAY.index, " ", TimeData.MONTH_SHORT.index, " ", TimeData.HOUR.index, ":", TimeData.MINUTE.index, ":", TimeData.SECOND.index)

                                    timeFormat.value = listOf(TimeData.HOUR.index, " : ", TimeData.MINUTE.index, " : ", TimeData.SECOND.index)

                                    startPrice.value = newStartPrice.value
                                    endPrice.value = newEndPrice.value

                                    candleCount.value = newCandleCount.value
                                    generateNew.value = newGenerateNew.value

                                    val randomCandles = generateRandomData(startPrice.value.toFloat().toInt(), endPrice.value.toFloat().toInt(), candleCount.value.toInt())
                                    if(generateNew.value.toBoolean()) {
                                        reDraw.value = !reDraw.value
                                        candleFeed.value = mutableListOf<CandleFeed>()
                                        areaFeed.value = mutableListOf<AreaFeed>()

                                        val updateTask = object : Runnable {
                                            override fun run() {
                                                // ConstraintLayout limit is no more than 260000 pixels
                                                if (candleFeed.value.size * (candleWidth.value.value.dpToFloat() + gapWidth.value.value.dpToFloat()) + rightBarWidth.value.value.dpToFloat() < 260000) {
                                                    mainHandler.postDelayed(this, 1000)
                                                    addRandomCandle(startPrice.value.toFloat().toInt(), endPrice.value.toFloat().toInt(), randomCandles.first, randomCandles.second)
                                                    candleFeed.value = randomCandles.first
                                                    areaFeed.value = randomCandles.second
                                                    liveUpdate.value = !liveUpdate.value
                                                } else {
                                                    mainHandler.removeCallbacksAndMessages(null)
                                                    val toast = Toast.makeText(applicationContext, "the maximum number of candles has been reached", Toast.LENGTH_LONG)
                                                    toast.show()
                                                }
                                            }
                                        }
                                        //mainHandler = Handler(Looper.getMainLooper())
                                        mainHandler.post(updateTask)
                                    } else {
                                        //liveUpdate.value = !liveUpdate.value
                                        reDraw.value = !reDraw.value
                                        candleFeed.value = randomCandles.first
                                        areaFeed.value = randomCandles.second
                                        //liveUpdate.value = !liveUpdate.value
                                        //reDraw.value = !reDraw.value
                                    }
                                } else {
                                    val toast = Toast.makeText(applicationContext, "At the current settings, the maximum number of candles is ${((260000 - rightBarWidth.value.value.dpToFloat()) / (candleWidth.value.value.dpToFloat() + gapWidth.value.value.dpToFloat())).toInt() - 1}", Toast.LENGTH_LONG)
                                    toast.show()
                                }

                            }){
                            Text(text = "Generate Random Data")
                        }
                        ConstraintLayout {
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.End) {
                                FloatingActionButton(modifier = Modifier.padding(8.dp),
                                    onClick = { showSettings.value = true }) {
                                    Icon(Icons.Filled.Settings, "")
                                }
                            }
                        }
                    }
                    if (showSettings.value) {
                        ConstraintLayout {
                            val newChartWidth = remember{mutableStateOf(chartWidth.value.toString())}
                            val newChartHeight = remember{mutableStateOf(chartHeight.value.toString())}
                            val newRightBarWidth = remember{mutableStateOf(rightBarWidth.value.toString().removeSuffix(".dp"))}
                            val newBottomBarHeight = remember{mutableStateOf(bottomBarHeight.value.toString().removeSuffix(".dp"))}
                            val newCandleWidth = remember{mutableStateOf(candleWidth.value.toString().removeSuffix(".dp"))}
                            val newGapWidth = remember{mutableStateOf(gapWidth.value.toString().removeSuffix(".dp"))}
                            val newPriceLineThickness = remember{mutableStateOf(priceLineThickness.value.toString().removeSuffix(".dp"))}
                            val newSelectedLineThickness = remember{mutableStateOf(selectedLineThickness.value.toString().removeSuffix(".dp"))}
                            val newDojiCandleThickness = remember{mutableStateOf(dojiCandleThickness.value.toString().removeSuffix(".dp"))}
                            val newEndButtonSize = remember{mutableStateOf(endButtonSize.value.toString().removeSuffix(".dp"))}
                            val newTopOffset = remember{mutableStateOf(topOffset.value.toString().removeSuffix(".dp"))}
                            val newRightBarTextSize = remember{mutableStateOf(rightBarTextSize.value.toString())}
                            val newSignificantDigits = remember{mutableStateOf(significantDigits.value.toString())}
                            val newPriceTagsCount = remember{mutableStateOf(priceTagsCount.value.toString())}
                            val newMinIndent = remember{mutableStateOf(minIndent.value.toString())}
                            val newDateOffset = remember{mutableStateOf(dateOffset.value.toString())}

                            val newBackgroundColor = remember{mutableStateOf(backgroundColor.value.joinToString(", "))}
                            val newRightBarColor = remember{mutableStateOf(rightBarColor.value.joinToString(", "))}
                            val newPositiveCandleColor = remember{mutableStateOf(positiveCandleColor.value.joinToString(", "))}
                            val newNegativeCandleColor = remember{mutableStateOf(negativeCandleColor.value.joinToString(", "))}
                            val newDojiCandleColor = remember{mutableStateOf(dojiCandleColor.value.joinToString(", "))}
                            val newEndButtonColor = remember{mutableStateOf(endButtonColor.value.joinToString(", "))}
                            val newPriceColor = remember{mutableStateOf(priceColor.value.joinToString(", "))}
                            val newSelectedColor = remember{mutableStateOf(selectedColor.value.joinToString(", "))}
                            val newTextColor = remember{mutableStateOf(textColor.value.joinToString(", "))}
                            val newSeparatorColor = remember{mutableStateOf(separatorColor.value.joinToString(", "))}

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .background(Color.Gray)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Chart Width")},
                                        value = newChartWidth.value,
                                        onValueChange = {newText -> newChartWidth.value = newText},
                                        placeholder = { Text("1.0") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Chart Height")},
                                        value = newChartHeight.value,
                                        onValueChange = {newText -> newChartHeight.value = newText},
                                        placeholder = { Text("0.5") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Right Bar Width: Dp")},
                                        value = newRightBarWidth.value,
                                        onValueChange = {newText -> newRightBarWidth.value = newText},
                                        placeholder = { Text("50") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Bottom Bar Height: Dp")},
                                        value = newBottomBarHeight.value,
                                        onValueChange = {newText -> newBottomBarHeight.value = newText},
                                        placeholder = { Text("20") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Candle Width: Dp")},
                                        value = newCandleWidth.value,
                                        onValueChange = {newText -> newCandleWidth.value = newText},
                                        placeholder = { Text("8") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Gap Width: Dp")},
                                        value = newGapWidth.value,
                                        onValueChange = {newText -> newGapWidth.value = newText},
                                        placeholder = { Text("2") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Price Line Thickness: Dp")},
                                        value = newPriceLineThickness.value,
                                        onValueChange = {newText -> newPriceLineThickness.value = newText},
                                        placeholder = { Text("1") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Selected Line Thickness: Dp")},
                                        value = newSelectedLineThickness.value,
                                        onValueChange = {newText -> newSelectedLineThickness.value = newText},
                                        placeholder = { Text("1") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Doji Candle Thickness: Dp")},
                                        value = newDojiCandleThickness.value,
                                        onValueChange = {newText -> newDojiCandleThickness.value = newText},
                                        placeholder = { Text("1") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("End Button Size: Dp")},
                                        value = newEndButtonSize.value,
                                        onValueChange = {newText -> newEndButtonSize.value = newText},
                                        placeholder = { Text("20") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Top Offset: Dp")},
                                        value = newTopOffset.value,
                                        onValueChange = {newText -> newTopOffset.value = newText},
                                        placeholder = { Text("8") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Right Bar Text Size: Sp")},
                                        value = newRightBarTextSize.value,
                                        onValueChange = {newText -> newRightBarTextSize.value = newText},
                                        placeholder = { Text("10") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Significant Digits: Int >= 0")},
                                        value = newSignificantDigits.value,
                                        onValueChange = {newText -> newSignificantDigits.value = newText},
                                        placeholder = { Text("2") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Price Tags Count: Int >= 0")},
                                        value = newPriceTagsCount.value,
                                        onValueChange = {newText -> newPriceTagsCount.value = newText},
                                        placeholder = { Text("4") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Min Indent: Int >= 0")},
                                        value = newMinIndent.value,
                                        onValueChange = {newText -> newMinIndent.value = newText},
                                        placeholder = { Text("12") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Date Offset: Int >= 0")},
                                        value = newDateOffset.value,
                                        onValueChange = {newText -> newDateOffset.value = newText},
                                        placeholder = { Text("1") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Background Color: r,g,b,a")},
                                        value = newBackgroundColor.value,
                                        onValueChange = {newText -> newBackgroundColor.value = newText},
                                        placeholder = { Text("41, 49, 51, 255") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Right Bar Color: r,g,b,a")},
                                        value = newRightBarColor.value,
                                        onValueChange = {newText -> newRightBarColor.value = newText},
                                        placeholder = { Text("37, 44, 46, 255") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Positive Candle Color: r,g,b,a")},
                                        value = newPositiveCandleColor.value,
                                        onValueChange = {newText -> newPositiveCandleColor.value = newText},
                                        placeholder = { Text("0, 255, 0, 255") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Negative Candle Color: r,g,b,a")},
                                        value = newNegativeCandleColor.value,
                                        onValueChange = {newText -> newNegativeCandleColor.value = newText},
                                        placeholder = { Text("255, 0, 0, 255") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Doji Candle Color: r,g,b,a")},
                                        value = newDojiCandleColor.value,
                                        onValueChange = {newText -> newDojiCandleColor.value = newText},
                                        placeholder = { Text("255, 255, 255, 255") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("End Button Color: r,g,b,a")},
                                        value = newEndButtonColor.value,
                                        onValueChange = {newText -> newEndButtonColor.value = newText},
                                        placeholder = { Text("255, 255, 255, 255") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Price Color: r,g,b,a")},
                                        value = newPriceColor.value,
                                        onValueChange = {newText -> newPriceColor.value = newText},
                                        placeholder = { Text("0, 128, 255, 255") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Selected Color: r,g,b,a")},
                                        value = newSelectedColor.value,
                                        onValueChange = {newText -> newSelectedColor.value = newText},
                                        placeholder = { Text("106, 90, 205, 255") }
                                    )
                                }
                                Row {
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Text Color: r,g,b,a")},
                                        value = newTextColor.value,
                                        onValueChange = {newText -> newTextColor.value = newText},
                                        placeholder = { Text("255, 255, 255, 255") }
                                    )
                                    TextField(
                                        modifier = Modifier
                                            .width(GetWidth() / 2),
                                        label = {Text("Separator Color: r,g,b,a")},
                                        value = newSeparatorColor.value,
                                        onValueChange = {newText -> newSeparatorColor.value = newText},
                                        placeholder = { Text("71, 74, 81, 255") }
                                    )
                                }
                                Button(
                                    modifier = Modifier
                                    .width(GetWidth()),
                                    onClick = {
                                        chartWidth.value = newChartWidth.value.toFloat()
                                        chartHeight.value = newChartHeight.value.toFloat()
                                        rightBarWidth.value = newRightBarWidth.value.toFloat().dp
                                        bottomBarHeight.value = newBottomBarHeight.value.toFloat().dp
                                        candleWidth.value = newCandleWidth.value.toFloat().dp
                                        gapWidth.value = newGapWidth.value.toFloat().dp
                                        priceLineThickness.value = newPriceLineThickness.value.toFloat().dp
                                        selectedLineThickness.value = newSelectedLineThickness.value.toFloat().dp
                                        dojiCandleThickness.value = newDojiCandleThickness.value.toFloat().dp
                                        endButtonSize.value = newEndButtonSize.value.toFloat().dp
                                        topOffset.value = newTopOffset.value.toFloat().dp
                                        rightBarTextSize.value = newRightBarTextSize.value.toInt()
                                        significantDigits.value = newSignificantDigits.value.toInt()
                                        priceTagsCount.value = newPriceTagsCount.value.toInt()
                                        minIndent.value = newMinIndent.value.toInt()
                                        dateOffset.value = newDateOffset.value.toInt()

                                        backgroundColor.value = newBackgroundColor.value.filter { !it.isWhitespace() }.split(",")
                                        rightBarColor.value = newRightBarColor.value.filter { !it.isWhitespace() }.split(",")
                                        positiveCandleColor.value = newPositiveCandleColor.value.filter { !it.isWhitespace() }.split(",")
                                        negativeCandleColor.value = newNegativeCandleColor.value.filter { !it.isWhitespace() }.split(",")
                                        dojiCandleColor.value = newDojiCandleColor.value.filter { !it.isWhitespace() }.split(",")
                                        endButtonColor.value = newEndButtonColor.value.filter { !it.isWhitespace() }.split(",")
                                        priceColor.value = newPriceColor.value.filter { !it.isWhitespace() }.split(",")
                                        selectedColor.value = newSelectedColor.value.filter { !it.isWhitespace() }.split(",")
                                        textColor.value = newTextColor.value.filter { !it.isWhitespace() }.split(",")
                                        separatorColor.value = newSeparatorColor.value.filter { !it.isWhitespace() }.split(",")

                                        reDraw.value = !reDraw.value
                                        showSettings.value = false
                                    }) {
                                    Text(text = "Apply")
                                }
                                Button(
                                    modifier = Modifier
                                        .width(GetWidth()),
                                    onClick = {
                                        showSettings.value = false
                                    }) {
                                    Text(text = "Back")
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private fun generateRandomData(startPrice: Int, endPrice: Int, candleCount: Int, previousCandleClose: Int = -1): Pair<MutableList<CandleFeed>, MutableList<AreaFeed>> {
        val priceRange = endPrice - startPrice

        var prevCandleClose: Int
        if (previousCandleClose == -1) {
            prevCandleClose = (startPrice..endPrice).random()
        } else {
            prevCandleClose = previousCandleClose
        }

        var candleHeight = 0f
        var topShadowHeight = 0f
        var bottomShadowHeight = 0f
        var randomSeed1: Int
        var randomSeed2: Int
        var randomSeed3: Int
        var candleType: Boolean
        var open: Int
        var close: Int
        var max: Int
        var min: Int
        val randomDataCandle = mutableListOf<CandleFeed>()
        val randomDataArea = mutableListOf<AreaFeed>()

        val unixTime = System.currentTimeMillis()
        //Log.d("debug", "unixTime, $unixTime")

        for (i in 1..candleCount) {

            randomSeed1 = (0..1000).random()
            randomSeed2 = (0..10).random()
            randomSeed3 = (0..10).random()
            candleType = Random.nextBoolean()
            when (randomSeed1) {
                in 0..300 -> candleHeight = priceRange * 0.05f
                in 301..600 -> candleHeight = priceRange * 0.1f
                in 601..700 -> candleHeight = priceRange * 0.15f
                in 701..750 -> candleHeight = priceRange * 0.2f
                in 751..800 -> candleHeight = priceRange * 0.25f
                in 801..850 -> candleHeight = priceRange * 0.05f
                in 851..900 -> candleHeight = priceRange * 0.03f
                in 901..950 -> candleHeight = priceRange * 0.02f
                in 951..990 -> candleHeight = priceRange * 0.01f
                in 991..1000 -> candleHeight = priceRange * 0.001f
                else -> { }
            }
            when (randomSeed2) {
                0 -> topShadowHeight = candleHeight
                1 -> topShadowHeight = candleHeight * 1.02f
                2 -> topShadowHeight = candleHeight * 0.98f
                3 -> topShadowHeight = candleHeight * 1.05f
                4 -> topShadowHeight = candleHeight * 0.95f
                5 -> topShadowHeight = candleHeight * 1.1f
                6 -> topShadowHeight = candleHeight * 0.9f
                7 -> topShadowHeight = candleHeight * 0.7f
                8 -> topShadowHeight = candleHeight * 0.5f
                9 -> topShadowHeight = candleHeight * 0.3f
                10 -> topShadowHeight = candleHeight * 0.1f
                //in 0..10 -> topShadowHeight = 0f
                else -> { }
            }
            when (randomSeed3) {
                0 -> bottomShadowHeight = candleHeight
                1 -> bottomShadowHeight = candleHeight * 1.02f
                2 -> bottomShadowHeight = candleHeight * 0.98f
                3 -> bottomShadowHeight = candleHeight * 1.05f
                4 -> bottomShadowHeight = candleHeight * 0.95f
                5 -> bottomShadowHeight = candleHeight * 1.1f
                6 -> bottomShadowHeight = candleHeight * 0.9f
                7 -> bottomShadowHeight = candleHeight * 0.7f
                8 -> bottomShadowHeight = candleHeight * 0.5f
                9 -> bottomShadowHeight = candleHeight * 0.3f
                10 -> bottomShadowHeight = candleHeight * 0.1f
                //in 0..10 -> bottomShadowHeight = 0f
                else -> { }
            }
            if (candleType && (prevCandleClose + candleHeight + topShadowHeight > endPrice)) candleType = !candleType
            if (!candleType && (prevCandleClose - candleHeight - bottomShadowHeight < startPrice)) candleType = !candleType

            if (candleType) { //green
                open = prevCandleClose
                close = (prevCandleClose..prevCandleClose + candleHeight.toInt()).random()
                max = (close..close+topShadowHeight.toInt()).random()
                min = (open - bottomShadowHeight.toInt()..open).random()

            } else { //red
                open = prevCandleClose
                close = (prevCandleClose - candleHeight.toInt()..prevCandleClose).random()
                max = (open..open + topShadowHeight.toInt()).random()
                min = (close - bottomShadowHeight.toInt()..close).random()
            }
            randomDataCandle.add(
                CandleFeed(
                    open.toFloat(),
                    close.toFloat(),
                    max.toFloat(),
                    min.toFloat(),
                    (unixTime - (candleCount - i) * 1000).toString()
                )
            )
            randomDataArea.add(
                AreaFeed(
                    open.toFloat(),
                    (unixTime - (candleCount - i) * 1000).toString()
                )
            )

            prevCandleClose = randomDataCandle[randomDataCandle.size-1].close.toInt()
        }
        return Pair(randomDataCandle, randomDataArea)
    }

    private fun addRandomCandle(startPrice: Int, endPrice: Int, candlesList: MutableList<CandleFeed>, areaList: MutableList<AreaFeed>) {
        val pair = generateRandomData(startPrice, endPrice, 1, candlesList[candlesList.size-1].close.toInt())
        candlesList.add(pair.first[0])
        areaList.add(pair.second[0])
    }

}

inline fun <reified T> MutableList<T>.removeRange(range: IntRange) {
    val fromIndex = range.start
    val toIndex = range.last
    if (fromIndex == toIndex) {
        return
    }

    if (fromIndex >= size) {
        throw IndexOutOfBoundsException("fromIndex $fromIndex >= size $size")
    }
    if (toIndex > size) {
        throw IndexOutOfBoundsException("toIndex $toIndex > size $size")
    }
    if (fromIndex > toIndex) {
        throw IndexOutOfBoundsException("fromIndex $fromIndex > toIndex $toIndex")
    }

    val filtered = filterIndexed { i, t -> i < fromIndex || i > toIndex }
    clear()
    addAll(filtered)
}

@Composable
fun GetWidth(): Dp {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp.dp
}

@Composable
fun GetHeight(): Dp {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp.dp
}

internal fun Float.dpToFloat(): Float {
    val scale = Resources.getSystem().displayMetrics.density
    return (this * scale)
}
//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    CandlestickChartTheme {
//
//    }
//}