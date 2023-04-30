package com.example.candlestickchart

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContentProviderCompat.requireContext
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.candlestickchart.ui.theme.CandlestickChartTheme
import org.json.JSONObject

const val API_KEY = "fVYFzwwxhgYGobQCWje8h9oYE5pufXvm"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CandlestickChartTheme {
                val value = remember { mutableStateOf(mutableListOf<MutableList<Float>>()) }
                requestData("AAPL", "1", "hour", "2023-02-01", "2023-03-07", "50000", value)
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        Greeting()
                        CandlestickChartComponent(chartWidth = GetWidth(), candles = value.value)
                        Text(text = "")
                        CandlestickChartComponent(chartWidth = GetWidth() * 0.5f, chartHeight = GetHeight() * 0.3f, candles = value.value)
                        Text(text = "")
                        CandlestickChartComponent(chartWidth = GetWidth() * 0.8f, chartHeight = GetHeight() * 0.15f, candles = value.value)
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
        return candles
    }

    fun showList(list: MutableList<MutableList<Float>>) {
        for (row in list) {
            Log.d("debug", row.toString())
        }
    }
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

@Composable
fun Greeting() {
    Text(text = "Candlestick chart")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CandlestickChartTheme {
        Column {
            Greeting()
        }

    }
}