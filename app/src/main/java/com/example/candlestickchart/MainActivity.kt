package com.example.candlestickchart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.candlestickchart.ui.theme.CandlestickChartTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CandlestickChartTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        Greeting()
                        CustomComponent(chartWidth = GetWidth())
                        Text(text = "")
                        CustomComponent(chartWidth = GetWidth() * 0.5f, chartHeight = GetHeight() * 0.3f)
                        Text(text = "")
                        CustomComponent(chartWidth = GetWidth() * 0.8f, chartHeight = GetHeight() * 0.15f)
                    }
                }
            }
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