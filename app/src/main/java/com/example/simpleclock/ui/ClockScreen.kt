package com.example.simpleclock.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ClockScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    val dateFormatter = remember {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMMdEEEE")
        SimpleDateFormat(pattern, Locale.getDefault())
    }
    val amPmStrings = remember { DateFormatSymbols.getInstance().amPmStrings }

    // 对齐到整秒刷新，避免累积漂移
    val timeMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            val now = System.currentTimeMillis()
            value = now
            delay(1000L - now % 1000L)
        }
    }

    val calendar = remember(timeMillis) {
        Calendar.getInstance().apply { timeInMillis = timeMillis }
    }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)

    val timeText = if (is24Hour) {
        String.format(Locale.US, "%02d:%02d:%02d", hour, minute, second)
    } else {
        String.format(Locale.US, "%d:%02d:%02d", (hour + 11) % 12 + 1, minute, second)
    }
    val amPm = if (is24Hour) "" else amPmStrings[calendar.get(Calendar.AM_PM)]

    // 农历一天只变一次，按“年 + 一年中的第几天”缓存
    val lunar = remember(calendar.get(Calendar.YEAR), calendar.get(Calendar.DAY_OF_YEAR)) {
        lunarDateFrom(timeMillis)
    }
    val dateText = dateFormatter.format(Date(timeMillis))

    val landscape = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val baseStyle = remember {
        TextStyle(fontWeight = FontWeight.Light, fontFeatureSettings = "tnum", letterSpacing = 0.sp)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 按可用宽高实测出最大字号：不同机型 / 字体 / 12-24 小时制都不会溢出
        val probe = measurer.measure(AnnotatedString("00:00:00"), baseStyle.copy(fontSize = 100.sp))
        val amPmReserve = with(density) { if (amPm.isNotEmpty()) 44.dp.toPx() else 0f }
        val widthBudget = with(density) { maxWidth.toPx() } * 0.94f - amPmReserve
        val heightBudget = with(density) { (maxHeight * 0.55f).toPx() }
        val byWidth = if (probe.size.width > 0) 100f * widthBudget / probe.size.width else 100f
        val byHeight = if (probe.size.height > 0) 100f * heightBudget / probe.size.height else 200f
        val timeSize = minOf(byWidth, byHeight).coerceIn(28f, 220f).sp

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = if (landscape) 20.sp else 17.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = lunar.dateText + " · " + lunar.yearText,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = if (landscape) 19.sp else 17.sp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeText,
                    style = baseStyle.copy(fontSize = timeSize),
                    maxLines = 1
                )
                if (amPm.isNotEmpty()) {
                    Text(
                        text = amPm,
                        modifier = Modifier.padding(
                            start = 8.dp,
                            bottom = with(density) { (timeSize * 0.16f).toDp() }
                        ),
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = timeSize * 0.25f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
