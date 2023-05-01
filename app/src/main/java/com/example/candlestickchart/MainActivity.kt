package com.example.candlestickchart

import android.os.AsyncTask
import android.os.Bundle
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

const val API_KEY = "fVYFzwwxhgYGobQCWje8h9oYE5pufXvm"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CandlestickChartTheme {
                val ticker = remember{mutableStateOf("AAPL")}
                val from = remember{mutableStateOf("2023-02-01")}
                val to = remember{mutableStateOf("2023-04-07")}
                val timespan = remember{mutableStateOf("hour")}

                val candles = remember { mutableStateOf(mutableListOf<MutableList<Float>>()) }
                //requestData("AAPL", "1", "hour", "2023-02-01", "2023-04-07", "50000", value)
                //requestData(ticker.value, "1", timespan.value, from.value, to.value, "50000", value)
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
                                    .border(width = 2.dp, color = Color.Gray, shape = RectangleShape),
                                value = ticker.value,
                                onValueChange = {newText -> ticker.value = newText},
                                placeholder = { Text("ticker") },
                                textStyle = TextStyle(fontSize=12.sp))
                            TextField(
                                modifier = Modifier
                                    .width(100.dp)
                                    .border(width = 2.dp, color = Color.Gray, shape = RectangleShape),
                                value = from.value,
                                onValueChange = {newText -> from.value = newText},
                                placeholder = { Text("from") },
                                textStyle = TextStyle(fontSize=12.sp))
                            TextField(
                                modifier = Modifier
                                    .width(100.dp)
                                    .border(width = 2.dp, color = Color.Gray, shape = RectangleShape),
                                value = to.value,
                                onValueChange = {newText -> to.value = newText},
                                placeholder = { Text("to") },
                                textStyle = TextStyle(fontSize=12.sp))
                            TextField(
                                modifier = Modifier
                                    .width(60.dp)
                                    .border(width = 2.dp, color = Color.Gray, shape = RectangleShape),
                                value = timespan.value,
                                onValueChange = {newText -> timespan.value = newText},
                                placeholder = { Text("timespan") },
                                textStyle = TextStyle(fontSize=12.sp))
                            Button(onClick = {requestData(ticker.value, "1", timespan.value, from.value, to.value, "50000", candles)}){
                                Text(text = "Show", fontSize = 10.sp)
                            }
                        }
                        CandlestickChartComponent(
                            candles = candles.value,
                            chartWidth = GetWidth(),
                            //rightBarWidth = 50.dp,
                            //candleWidth = 25.dp,
                            //gapWidth = 8.dp,
                            //significantDigits = 2,
                            //backgroundColor = Color.Gray,
                            //rightBarColor = Color.LightGray,
                            //textColor = Color.Red,
                            //priceLineColor = Color.Green,
                            //positiveCandleColor = Color.Magenta,
                            //negativeCandleColor = Color.Blue
                        )
                        Text(text = "", modifier = Modifier.height(2.dp))
                        CandlestickChartComponent(
                            candles = candles.value,
                            chartWidth = GetWidth() * 0.5f,
                            chartHeight = GetHeight() * 0.3f)
                        Text(text = "", modifier = Modifier.height(2.dp))
                        CandlestickChartComponent(
                            candles = candles.value,
                            chartWidth = GetWidth() * 0.8f,
                            chartHeight = GetHeight() * 0.15f)
                    }
                }
            }
        }
    }

    private fun requestData(ticker: String, multiplier: String, timespan: String, from: String, to: String, limit: String, candlesList: MutableState<MutableList<MutableList<Float>>>) {
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
                val list = parseData(result)
                candlesList.value = list

            },
            {
                error -> Log.d("debug", "Request Error: $error")
            }
        )
        queue.add(request)

    }

    private fun parseData(result: String): MutableList<MutableList<Float>> {
        val root = JSONObject(result)
        try {
            val results = root.getJSONArray("results")
            val candles = MutableList(0) { MutableList(4) { 0f } } //open, close, max, min
            Log.d("debug", results.length().toString())
            for (i in 0 until results.length()) {
                val currentCandle = results.getJSONObject(i)
                candles.add(mutableListOf(
                    currentCandle.getString("o").toFloat(),
                    currentCandle.getString("c").toFloat(),
                    currentCandle.getString("h").toFloat(),
                    currentCandle.getString("l").toFloat(),))
            }
            //Log.d("debug", "Result: ${candles[1][0]}")
            showList(candles)
            //больше 250000 пикселей - ошибка
            if (candles.size > 8000) candles.removeRange(8000..candles.size)
            return candles
        } catch (e: JSONException) {
            return mutableListOf<MutableList<Float>>()
        }
    }

    fun showList(list: MutableList<MutableList<Float>>) {
        for (row in list) {
            Log.d("debug", row.toString())
        }
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CandlestickChartTheme {
        Column {

        }

    }
}