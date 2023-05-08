package com.example.candlestickchart

import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContentProviderCompat.requireContext
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.candlestickchart.ui.theme.CandlestickChartTheme
import org.json.JSONException
import org.json.JSONObject
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

const val API_KEY = "fVYFzwwxhgYGobQCWje8h9oYE5pufXvm"

class MainActivity : ComponentActivity() {
//    lateinit var mainHandler: Handler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CandlestickChartTheme {
                val ticker = remember{mutableStateOf("NVDA")}
                val from = remember{mutableStateOf("2023-01-01")}
                val to = remember{mutableStateOf("2023-05-05")}
                val timespan = remember{mutableStateOf("hour")}

                val candles = remember { mutableStateOf(mutableListOf<MutableList<Float>>()) }
                //val candles = remember { mutableStateOf(generateRandomData(220, 260, 500)) }
                //candles.value = generateRandomData(220, 260, 500, candles)
//                var randomCandles = mutableListOf<MutableList<Float>>()
//                randomCandles = generateRandomData(220, 260, 500)

                val timestamps = remember { mutableStateOf(mutableListOf<MutableList<String>>()) }
                val timeFormat = remember { mutableStateOf(listOf<String>()) }

//                val updateTask = object : Runnable {
//                    override fun run() {
//                        mainHandler.postDelayed(this, 1000)
//                        addRandomCandle(220, 260, randomCandles)
//                        candles.value = randomCandles
//                    }
//                }
//                mainHandler = Handler(Looper.getMainLooper())
//                mainHandler.post(updateTask)

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        Row() {
                            TextField(
                                modifier = Modifier
                                    .width(70.dp)
                                    .border(
                                        width = 2.dp,
                                        color = Color.Gray,
                                        shape = RectangleShape
                                    ),
                                value = ticker.value,
                                onValueChange = {newText -> ticker.value = newText},
                                placeholder = { Text("ticker") },
                                textStyle = TextStyle(fontSize=12.sp))
                            TextField(
                                modifier = Modifier
                                    .width(100.dp)
                                    .border(
                                        width = 2.dp,
                                        color = Color.Gray,
                                        shape = RectangleShape
                                    ),
                                value = from.value,
                                onValueChange = {newText -> from.value = newText},
                                placeholder = { Text("from") },
                                textStyle = TextStyle(fontSize=12.sp))
                            TextField(
                                modifier = Modifier
                                    .width(100.dp)
                                    .border(
                                        width = 2.dp,
                                        color = Color.Gray,
                                        shape = RectangleShape
                                    ),
                                value = to.value,
                                onValueChange = {newText -> to.value = newText},
                                placeholder = { Text("to") },
                                textStyle = TextStyle(fontSize=12.sp))
                            TextField(
                                modifier = Modifier
                                    .width(60.dp)
                                    .border(
                                        width = 2.dp,
                                        color = Color.Gray,
                                        shape = RectangleShape
                                    ),
                                value = timespan.value,
                                onValueChange = {newText -> timespan.value = newText},
                                placeholder = { Text("timespan") },
                                textStyle = TextStyle(fontSize=12.sp))
                            Button(onClick = {requestData(ticker.value, "1", timespan.value, from.value, to.value, "50000", candles, timestamps, timeFormat)}){
                                Text(text = "Show", fontSize = 10.sp)
                            }
                        }
                        CandlestickChartComponent(
                            candles = candles.value,
                            timestamps = timestamps.value,
                            timeFormat = timeFormat.value,
                            selectedTimeFormat = listOf("2", " ", "1", " ", "3", ":", "4"),
                            minIndent = 12,
                            dateOffset = 1,
                            chartWidth = GetWidth(),
                            //rightBarWidth = 50.dp,
                            //candleWidth = 25.dp,
                            //gapWidth = 8.dp,
                            //significantDigits = 2,
                            //bottomBarHeight = 20.dp,
                            //priceLineThickness = 1.dp,
                            //priceLineStyle = floatArrayOf(30f, 10f, 10f, 10f),
                            //selectedLineThickness = 1.dp,
                            //dojiCandleThickness = 1.dp,
                            //selectedLineStyle = floatArrayOf(10f, 10f),
                            //endButtonSize = 40.dp,
                            //backgroundColor = Color.Gray,
                            //rightBarColor = Color.LightGray,
                            //textColor = Color.Red,
                            //priceLineColor = Color.Green,
                            //positiveCandleColor = Color.Magenta,
                            //negativeCandleColor = Color.Blue,
                            //dojiCandleColor = Color.Yellow,
                            //endButtonColor = Color.Red
                        )
                        Text(text = "", modifier = Modifier.height(2.dp))
                        CandlestickChartComponent(
                            candles = candles.value,
                            timestamps = timestamps.value,
                            timeFormat = timeFormat.value,
                            selectedTimeFormat = listOf("2", " ", "1", " ", "3", ":", "4"),
                            chartWidth = GetWidth() * 0.5f,
                            chartHeight = GetHeight() * 0.3f)
                        Text(text = "", modifier = Modifier.height(2.dp))
                        CandlestickChartComponent(
                            candles = candles.value,
                            timestamps = timestamps.value,
                            timeFormat = timeFormat.value,
                            selectedTimeFormat = listOf("2", " ", "1", " ", "3", ":", "4"),
                            chartWidth = GetWidth() * 0.8f,
                            chartHeight = GetHeight() * 0.15f)
                    }
                }
            }
        }
    }

    val dateFormat = SimpleDateFormat("yyyy MMM dd HH mm ss", Locale.ENGLISH)

    private fun getDateString(time: String) : MutableList<String> = stringToWords(dateFormat.format(time.toLong()))

    private fun stringToWords(s : String) = s.trim().splitToSequence(' ')
        .filter { it.isNotEmpty() }
        .toMutableList()


    private fun requestData(ticker: String, multiplier: String, timespan: String, from: String, to: String, limit: String, candlesList: MutableState<MutableList<MutableList<Float>>>, timestampsList: MutableState<MutableList<MutableList<String>>>, timeFormat: MutableState<List<String>>) {
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
                candlesList.value = list
                timestampsList.value = list1
                when (timespan) {
                    "minute" -> timeFormat.value = listOf<String>("3", ":00")
                    "hour" -> timeFormat.value = listOf<String>("2", " ", "1")
                    "day" -> timeFormat.value = listOf<String>("1")
                    "week" -> timeFormat.value = listOf<String>("1")
                    "month" -> timeFormat.value = listOf<String>("0")
                    "quarter" -> timeFormat.value = listOf<String>("0")
                    "year" -> timeFormat.value = listOf<String>("0")
                    else -> {
                        timeFormat.value = listOf<String>()
                    }
                }
            },
            {
                error -> Log.d("debug", "Request Error: $error")
            }
        )
        queue.add(request)
    }

    private fun parseData(result: String): Pair<MutableList<MutableList<Float>>, MutableList<MutableList<String>>> {
        val root = JSONObject(result)
        try {
            val results = root.getJSONArray("results")
            val candles = MutableList(0) { MutableList(4) { 0f } } //open, close, max, min
            Log.d("debug", results.length().toString())
            val timestamps = MutableList(0) { MutableList(4) { "" } }
            for (i in 0 until results.length()) {
                val currentCandle = results.getJSONObject(i)
                candles.add(mutableListOf(
                    currentCandle.getString("o").toFloat(),
                    currentCandle.getString("c").toFloat(),
                    currentCandle.getString("h").toFloat(),
                    currentCandle.getString("l").toFloat(),))
                timestamps.add(getDateString(currentCandle.getString("t")))
            }
            //Log.d("debug", "Result: ${candles[1][0]}")
            showList(candles)
//            //больше 250000 пикселей - ошибка
//            if (candles.size > 8000) candles.removeRange(8000..candles.size)
            if (candles.size > 2000) candles.removeRange(2000..candles.size)
            return Pair(candles, timestamps)
        } catch (e: JSONException) {
            return Pair(mutableListOf<MutableList<Float>>(), mutableListOf<MutableList<String>>())
        }
    }

    fun showList(list: MutableList<MutableList<Float>>) {
        for (row in list) {
            Log.d("debug", row.toString())
        }
    }

//    private fun generateRandomData(startPrice: Int, endPrice: Int, candleCount: Int): MutableList<MutableList<Float>> {
//        val priceRange = endPrice - startPrice
//        var prevCandleClose = (startPrice..endPrice).random()
//        var candleHeight = 0f
//        var topShadowHeight = 0f
//        var bottomShadowHeight = 0f
//        var randomSeed1 = 0
//        var randomSeed2 = 0
//        var randomSeed3 = 0
//        var candleType = false
//        var open = 0
//        var close = 0
//        var max = 0
//        var min = 0
//        var randomData = MutableList(0) { MutableList(4) { 0f } } //open, close, max, min
//        for (i in 1..candleCount) {
//            randomSeed1 = (0..1000).random()
//            randomSeed2 = (0..10).random()
//            randomSeed3 = (0..10).random()
//            candleType = Random.nextBoolean()
//            when (randomSeed1) {
//                in 0..300 -> candleHeight = priceRange * 0.1f
//                in 301..600 -> candleHeight = priceRange * 0.2f
//                in 601..700 -> candleHeight = priceRange * 0.3f
//                in 701..750 -> candleHeight = priceRange * 0.35f
//                in 751..800 -> candleHeight = priceRange * 0.4f
//                in 801..850 -> candleHeight = priceRange * 0.05f
//                in 851..900 -> candleHeight = priceRange * 0.03f
//                in 901..950 -> candleHeight = priceRange * 0.02f
//                in 951..990 -> candleHeight = priceRange * 0.01f
//                in 991..1000 -> candleHeight = priceRange * 0.001f
//                else -> { }
//            }
//            when (randomSeed2) {
//                in 0..3 -> topShadowHeight = candleHeight * 1.1f
//                in 4..5 -> topShadowHeight = candleHeight * 1.2f
//                in 6..7 -> topShadowHeight = candleHeight * 1.3f
//                8 -> topShadowHeight = candleHeight * 1.05f
//                9 -> topShadowHeight = candleHeight * 1.01f
//                10 -> topShadowHeight = candleHeight
//                else -> { }
//            }
//            when (randomSeed3) {
//                in 0..3 -> bottomShadowHeight = candleHeight * 1.1f
//                in 4..5 -> bottomShadowHeight = candleHeight * 1.2f
//                in 6..7 -> bottomShadowHeight = candleHeight * 1.3f
//                8 -> bottomShadowHeight = candleHeight * 1.05f
//                9 -> bottomShadowHeight = candleHeight * 1.01f
//                10 -> bottomShadowHeight = candleHeight
//                else -> { }
//            }
//            if (candleType) { //green
//                open = prevCandleClose
//                close = (prevCandleClose..prevCandleClose + candleHeight.toInt()).random()
//                max = (close..close+topShadowHeight.toInt()).random()
//                min = (open - bottomShadowHeight.toInt()..open).random()
//
//            } else { //red
//                open = prevCandleClose
//                close = (prevCandleClose - candleHeight.toInt()..prevCandleClose).random()
//                max = (open..open + topShadowHeight.toInt()).random()
//                min = (close - bottomShadowHeight.toInt()..close).random()
//            }
//            randomData.add(mutableListOf(
//                open.toFloat(),
//                close.toFloat(),
//                max.toFloat(),
//                min.toFloat(),
//            ))
//            prevCandleClose = randomData[randomData.size-1][1].toInt()
//        }
//        return randomData
//    }
//
//    private fun addRandomCandle(startPrice: Int, endPrice: Int, candlesList: MutableList<MutableList<Float>>) {
//        candlesList.add(generateRandomData(startPrice, endPrice, 1)[0])
//    }

}

//private fun getDateTime(s: String): String? {
//    try {
//        val sdf = SimpleDateFormat("MM/dd/yyyy")
//        val netDate = Date(Long.parseLong(s) * 1000)
//        return sdf.format(netDate)
//    } catch (e: Exception) {
//        return e.toString()
//    }
//}

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
fun GetWidth() : Dp {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    return screenWidth
}
@Composable
fun GetHeight() : Dp {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    return screenHeight
}

//@Preview(showBackground = true)
//@Composable
//fun DefaultPreview() {
//    CandlestickChartTheme {
//
//    }
//}