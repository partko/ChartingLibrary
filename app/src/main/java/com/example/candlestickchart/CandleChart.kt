package com.example.candlestickchart

import android.content.res.Resources
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.constraintlayout.compose.ConstraintLayout
import java.math.RoundingMode
import kotlin.math.ceil

@Composable
fun CandlestickChartComponent(
    chartWidth: Dp = 300.dp,
    chartHeight: Dp = 300.dp,
    backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
    //candlesWidth: Float = 0.04f,
    candleWidth: Dp = 8.dp,
    candles: MutableList<MutableList<Float>>
) {
//    val candles = MutableList(10) { MutableList(4) { 0f } } //open, close, max, min
//    candles[0] = mutableListOf(560f, 540f, 580f, 530f)     // red
//    candles[1] = mutableListOf(540f, 510f, 550f, 500f)     // red
//    candles[2] = mutableListOf(510f, 515f, 540f, 505f)     // green
//    candles[3] = mutableListOf(515f, 530f, 535f, 513f)     // green
//    candles[4] = mutableListOf(530f, 532f, 545f, 520f)     // green
//    candles[5] = mutableListOf(532f, 506f, 537f, 502f)     // red
//    candles[6] = mutableListOf(506f, 505f, 526f, 501f)     // red
//    candles[7] = mutableListOf(505f, 544f, 562f, 503f)     // green
//    candles[8] = mutableListOf(544f, 556f, 570f, 537f)     // green
//    candles[9] = mutableListOf(556f, 550f, 563f, 525f)     // red

    fun getMinPrice(scrollOffset: Int, visibleCandles: Int) : Float {
        var minPrice = candles[scrollOffset][3]
        for (i in (0+scrollOffset) .. (visibleCandles+scrollOffset)) {
            if (candles[i][3] < minPrice) minPrice = candles[i][3]
        }
        return minPrice
    }

    fun getMaxPrice(scrollOffset: Int, visibleCandles: Int) : Float {
        var maxPrice = 0f
        for (i in (0+scrollOffset) .. (visibleCandles+scrollOffset)) {
            if (candles[i][2] > maxPrice) maxPrice = candles[i][2]
        }
        return maxPrice
    }

    if (candles.isNotEmpty()) {
        Log.d("debug", "Candles count = ${candles.size}")
        Log.d("debug", "chartWidthInDp = $chartWidth")
        Log.d("debug", "chartWidthInPx = ${chartWidth.toString().removeSuffix(".dp").toFloat().dpToFloat()}")

        val visibleCandles: Int = (chartWidth / (candleWidth + 2.dp)).toBigDecimal().setScale(1, RoundingMode.DOWN).toInt() //число отображаемых(видимых) свечей на графике
        Log.d("debug", "visibleCandles = $visibleCandles")
        val screenDensity: Float = Resources.getSystem().displayMetrics.density
        Log.d("debug", "screenDensity = $screenDensity")
        val candleWithSpace: Float = (candleWidth + 2.dp).value * screenDensity //ширина свеча+отступ справа в пикселях
        Log.d("debug", "candleWithSpace = $candleWithSpace")
        var scrollOffset: Int = 0 //количество сместившихся свечей при скроле
        //Log.d("debug", "scrollOffset = $scrollOffset")

//        var maxPrice = 0f
//        var minPrice = candles[0][3]

        var maxPrice = remember {
            //mutableStateOf(0f)
            mutableStateOf(getMaxPrice(scrollOffset, visibleCandles))
        }
        var minPrice = remember {
            //mutableStateOf(candles[0 + scrollOffset][3])
            mutableStateOf(getMinPrice(scrollOffset, visibleCandles))
        }

//        maxPrice.value = getMaxPrice(scrollOffset, visibleCandles)
//        minPrice.value = getMinPrice(scrollOffset, visibleCandles)

        Log.d("debug", "maxPrice = ${maxPrice.value}")
        Log.d("debug", "minPrice = ${minPrice.value}")

        //Log.d("debug", "test = ${(candles.size * candlesWidth * Resources.getSystem().displayMetrics.widthPixels).FloatToDp().dp}")

        val scrollState = rememberScrollState()
        Log.d("ScrollValue", "current: ${scrollState.value}, max: ${scrollState.maxValue}")

        if (scrollState.isScrollInProgress){
            Log.d("ScrollValue", "SCROLLING")
            scrollOffset = (scrollState.value / candleWithSpace).toInt()
            Log.d("debug", "scrollOffset = $scrollOffset")
            maxPrice.value = getMaxPrice(scrollOffset, visibleCandles)
            minPrice.value = getMinPrice(scrollOffset, visibleCandles)
        }

        var currentPrice = candles[candles.size-1][1]

        ConstraintLayout() {
            Column(modifier = Modifier
                .size(width = chartWidth, height = chartHeight)
                .background(backgroundColor)
                //.horizontalScroll(rememberScrollState())
                .horizontalScroll(scrollState)
                //.width(2000.dp)
                //.width((candles.size * (candlesWidth + 0.01f) * Resources.getSystem().displayMetrics.widthPixels).FloatToDp().dp)
                .width((candleWidth + 2.dp) * candles.size)
                .drawBehind {
                    //val chartSize = size / 1.25f
                    //val chartSize = size
                    val chartSize = Size(width = size.width * 0.9f, height = size.height * 0.94f)
                    backgroundCanvas(
                        candles = candles,
//                        maxPrice = maxPrice,
//                        minPrice = minPrice,
                        maxPrice = maxPrice.value,
                        minPrice = minPrice.value,
                        componentSize = chartSize,
                        //componentColor = backgroundColor,
                        candleWidth = candleWidth,
                        greenCandleColor = Color(
                            red = 0x00,
                            green = 0xFF,
                            blue = 0x00,
                            alpha = 0xFF
                        ),
                        redCandleColor = Color(red = 0xFF, green = 0x00, blue = 0x00, alpha = 0xFF),
                        topOffset = size.height * 0.03f,
                        chartWidth = chartWidth
                            .toString()
                            .removeSuffix(".dp")
                            .toFloat()
                            .dpToFloat()
                    )
                }) {
//            Text( //максимальная цена на графике
//                text = "$maxPrice",
//                fontSize = chartHeight.dpToSp() / 40,
//                modifier = Modifier.offset(
//                    x = chartWidth * 1.0f - chartHeight / 5,
//                    y = chartHeight * 0.0f))
//            Text( //минимальная цена на графике
//                text = "$minPrice",
//                fontSize = chartHeight.dpToSp() / 40,
//                modifier = Modifier.offset(
//                    x = chartWidth * 1.0f - chartHeight / 5,
//                    y = chartHeight * 0.8f))
//            Text( //последняя цена
//                text = "$currentPrice",
//                fontSize = chartHeight.dpToSp() / 40,
//                modifier = Modifier.offset(
//                    x = chartWidth * 1.0f - chartHeight / 5,
//                    y = chartHeight * (maxPrice - candles[candles.size-1][1])/(maxPrice-minPrice)-chartHeight *0.225f))
//                    //y = -chartHeight *0.2f))

                //Log.d("debug", "test = " + (maxPrice - candles[candles.size-1][1])/(maxPrice-minPrice))
            }
            Text( //максимальная цена на графике
                text = "${maxPrice.value}",
                fontSize = chartHeight.dpToSp() / 40,
                modifier = Modifier.offset(
                    x = chartWidth * 1.0f - chartHeight / 4.5f,
                    y = chartHeight * 0.0f))
            Text( //минимальная цена на графике
                text = "${minPrice.value}",
                fontSize = chartHeight.dpToSp() / 40,
                modifier = Modifier.offset(
                    x = chartWidth * 1.0f - chartHeight / 4.5f,
                    y = chartHeight * 0.9f))
            if (candles[candles.size-1][1] < maxPrice.value && candles[candles.size-1][1] > minPrice.value) {
                Text( //последняя цена
                    text = "$currentPrice",
                    fontSize = chartHeight.dpToSp() / 40,
                    modifier = Modifier.offset(
                        x = chartWidth * 1.0f - chartHeight / 4.5f,
                        y = chartHeight * (maxPrice.value - candles[candles.size - 1][1]) / (maxPrice.value - minPrice.value)
                    )
                )
            }
        }
    }
}

fun DrawScope.backgroundCanvas(
    candles: MutableList<MutableList<Float>>,
    maxPrice: Float,
    minPrice: Float,
    componentSize: Size,
    //componentColor: Color,
    candleWidth: Dp,
    greenCandleColor: Color,
    redCandleColor: Color,
    topOffset: Float,
    chartWidth: Float
) {
    for (i in candles.indices) {
//        Log.d("debug", "candle index = " + componentSize.width)
//        Log.d("debug", "offset y = " + (componentSize.height - (candles[i][0]-minPrice)/(maxPrice-minPrice)*componentSize.height))
//        Log.d("debug", "size height = " + (candles[i][0]-candles[i][1])/(maxPrice-minPrice)*componentSize.height)

        if (candles[i][0] <= candles[i][1]) { //зеленая свеча
            drawRect( //тело свечи
                color = greenCandleColor,
                topLeft = Offset(
                    //x = (candleWidth+0.01f)*i*chartWidth,
                    x = ((candleWidth + 2.dp)*i).toPx(),
                    y = componentSize.height - (candles[i][0]-minPrice)/(maxPrice-minPrice)*componentSize.height + topOffset),
                size = Size(
                    //width = candleWidth*chartWidth,
                    width = candleWidth.toPx(),
                    height = (candles[i][0]-candles[i][1])/(maxPrice-minPrice)*componentSize.height),
            )
            drawRect( //тень свечи
                color = greenCandleColor,
                topLeft = Offset(
                    //x = (candleWidth+0.01f)*i*chartWidth+candleWidth*0.45f*chartWidth,
                    x = ((candleWidth + 2.dp)*i).toPx() + (candleWidth*0.45f).toPx(),
                    y = componentSize.height - (candles[i][2]-minPrice)/(maxPrice-minPrice)*componentSize.height + topOffset),
                size = Size(
                    //width = candleWidth*chartWidth*0.1f,
                    width = candleWidth.toPx()*0.1f,
                    height = (candles[i][2]-candles[i][3])/(maxPrice-minPrice)*componentSize.height),
            )
        } else if (candles[i][0] > candles[i][1]) { //красная свеча
            drawRect( //тело свечи
                color = redCandleColor,
                topLeft = Offset(
                    //x = (candleWidth+0.01f)*i*chartWidth,
                    x = ((candleWidth + 2.dp)*i).toPx(),
                    y = componentSize.height - (candles[i][0]-minPrice)/(maxPrice-minPrice)*componentSize.height + topOffset),
                size = Size(
                    //width = candleWidth*chartWidth,
                    width = candleWidth.toPx(),
                    height = (candles[i][0]-candles[i][1])/(maxPrice-minPrice)*componentSize.height),
            )
            drawRect( //тень свечи
                color = redCandleColor,
                topLeft = Offset(
                    //x = (candleWidth+0.01f)*i*chartWidth+candleWidth*0.45f*chartWidth,
                    x = ((candleWidth + 2.dp)*i).toPx() + (candleWidth*0.45f).toPx(),
                    y = componentSize.height - (candles[i][2]-minPrice)/(maxPrice-minPrice)*componentSize.height + topOffset),
                size = Size(
                    //width = candleWidth*chartWidth*0.1f,
                    width = candleWidth.toPx()*0.1f,
                    height = (candles[i][2]-candles[i][3])/(maxPrice-minPrice)*componentSize.height),
            )
        }
    }
    if (candles[candles.size-1][1] < maxPrice && candles[candles.size-1][1] > minPrice) {
        drawLine(
            color = Color(red = 0x29, green = 0x31, blue = 0x33, alpha = 0xFF),
            start = Offset(
                x = 0f,
                y = componentSize.height - (candles[candles.size - 1][1] - minPrice) / (maxPrice - minPrice) * componentSize.height + topOffset
            ),
            end = Offset(
                x = (componentSize.width * 1.1f - componentSize.height / 5),
                y = componentSize.height - (candles[candles.size - 1][1] - minPrice) / (maxPrice - minPrice) * componentSize.height + topOffset
            ),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 10f, 10f, 10f), phase = 0f)
        )
    }
}

//    drawRect(
//        componentColor,
//        Offset(x = 46.dp.toPx(), y = 40.dp.toPx()),
//        Size(width = 4.dp.toPx(), height = 140.dp.toPx()),
//    )

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
//    CustomComponent()
//}
