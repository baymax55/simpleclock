package com.example.simpleclock.ui

import java.util.Locale

/**
 * 把毫秒格式化成 mm:ss / h:mm:ss，[hundredths] 为 true 时追加百分秒（秒表用）。
 */
internal fun formatTime(millis: Long, hundredths: Boolean = false): String {
    val safe = millis.coerceAtLeast(0L)
    val totalSeconds = safe / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val base = if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
    return if (hundredths) {
        String.format(Locale.US, "%s.%02d", base, (safe % 1000) / 10)
    } else {
        base
    }
}
