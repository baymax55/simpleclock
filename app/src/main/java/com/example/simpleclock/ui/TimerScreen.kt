package com.example.simpleclock.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleclock.R
import kotlinx.coroutines.delay
import java.util.Locale

private const val DEFAULT_DURATION_MS = 5 * 60 * 1000L
private const val MAX_MINUTES = 99

@Composable
fun TimerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var totalMs by rememberSaveable { mutableLongStateOf(DEFAULT_DURATION_MS) }
    var remainingMs by rememberSaveable { mutableLongStateOf(DEFAULT_DURATION_MS) }
    var running by rememberSaveable { mutableStateOf(false) }
    var finished by rememberSaveable { mutableStateOf(false) }
    var deadline by rememberSaveable { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(running, deadline) {
        while (running) {
            now = SystemClock.elapsedRealtime()
            if (deadline - now <= 0L) {
                remainingMs = 0L
                running = false
                finished = true
                playAlarm(context)
            }
            delay(50L)
        }
    }

    val displayMs = if (running) (deadline - now).coerceIn(0L, totalMs) else remainingMs
    val progress = if (totalMs > 0L) displayMs.toFloat() / totalMs else 0f

    val minutes = (totalMs / 60_000L).toInt()
    val seconds = ((totalMs / 1000L) % 60L).toInt()

    fun updateDuration(newMinutes: Int, newSeconds: Int) {
        val ms = (newMinutes.coerceIn(0, MAX_MINUTES) * 60L + newSeconds.coerceIn(0, 59)) * 1000L
        totalMs = ms
        remainingMs = ms
        finished = false
        running = false
    }

    val onReset = {
        running = false
        finished = false
        remainingMs = totalMs
        deadline = 0L
    }
    val onPrimary = {
        when {
            running -> {
                remainingMs = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                running = false
            }
            finished -> {
                remainingMs = totalMs
                finished = false
                deadline = SystemClock.elapsedRealtime() + totalMs
                now = SystemClock.elapsedRealtime()
                running = true
            }
            else -> {
                deadline = SystemClock.elapsedRealtime() + remainingMs
                now = SystemClock.elapsedRealtime()
                running = true
            }
        }
    }
    val primaryLabel = when {
        running -> R.string.action_pause
        finished -> R.string.action_restart
        remainingMs < totalMs -> R.string.action_resume
        else -> R.string.action_start
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        val landscape = maxWidth > maxHeight
        val ringSize = if (landscape) {
            minOf(maxHeight * 0.8f, maxWidth * 0.42f)
        } else {
            minOf(maxWidth * 0.7f, maxHeight * 0.4f)
        }

        if (landscape) {
            // 横屏：表环在左，设置与按钮在右
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    RingBlock(displayMs, progress, finished, ringSize)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StepperRow(
                        minutes = minutes,
                        seconds = seconds,
                        enabled = !running,
                        onMinutes = { updateDuration(it, seconds) },
                        onSeconds = { updateDuration(minutes, it) }
                    )
                    Spacer(Modifier.height(28.dp))
                    TimerActions(
                        primaryLabel = primaryLabel,
                        resetEnabled = running || remainingMs != totalMs || finished,
                        primaryEnabled = running || finished || remainingMs > 0L,
                        onReset = onReset,
                        onPrimary = onPrimary
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RingBlock(displayMs, progress, finished, ringSize)
                Spacer(Modifier.height(32.dp))
                StepperRow(
                    minutes = minutes,
                    seconds = seconds,
                    enabled = !running,
                    onMinutes = { updateDuration(it, seconds) },
                    onSeconds = { updateDuration(minutes, it) }
                )
                Spacer(Modifier.height(32.dp))
                TimerActions(
                    primaryLabel = primaryLabel,
                    resetEnabled = running || remainingMs != totalMs || finished,
                    primaryEnabled = running || finished || remainingMs > 0L,
                    onReset = onReset,
                    onPrimary = onPrimary
                )
            }
        }
    }
}

@Composable
private fun RingBlock(displayMs: Long, progress: Float, finished: Boolean, size: Dp) {
    // 字号跟随表环大小，横屏全屏时不会显得空
    val fontSize = with(LocalDensity.current) { (size * 0.16f).toSp() }
    Box(contentAlignment = Alignment.Center) {
        ProgressRing(progress = progress, modifier = Modifier.size(size))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTime(displayMs),
                fontSize = fontSize,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                color = if (finished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (finished) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.timer_finished),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StepperRow(
    minutes: Int,
    seconds: Int,
    enabled: Boolean,
    onMinutes: (Int) -> Unit,
    onSeconds: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        DurationStepper(
            label = stringResource(R.string.label_minutes),
            value = minutes,
            step = 1,
            range = 0..MAX_MINUTES,
            enabled = enabled,
            onChange = onMinutes
        )
        DurationStepper(
            label = stringResource(R.string.label_seconds),
            value = seconds,
            step = 5,
            range = 0..55,
            enabled = enabled,
            onChange = onSeconds
        )
    }
}

@Composable
private fun TimerActions(
    @StringRes primaryLabel: Int,
    resetEnabled: Boolean,
    primaryEnabled: Boolean,
    onReset: () -> Unit,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.widthIn(max = 420.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onReset,
            enabled = resetEnabled,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (resetEnabled) 0.6f else 0.3f)),
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.action_reset))
        }
        Button(onClick = onPrimary, enabled = primaryEnabled, modifier = Modifier.weight(1.4f)) {
            Text(stringResource(primaryLabel))
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val indicatorColor = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val strokeWidth = (size.minDimension * 0.04f).coerceAtLeast(8.dp.toPx())
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        if (progress > 0f) {
            drawArc(
                color = indicatorColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun DurationStepper(
    label: String,
    value: Int,
    step: Int,
    range: IntRange,
    enabled: Boolean,
    onChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(
                onClick = { onChange((value - step).coerceIn(range)) },
                enabled = enabled,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = "−", fontSize = 20.sp)
            }
            Text(
                text = String.format(Locale.US, "%02d", value),
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.Center,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            FilledTonalButton(
                onClick = { onChange((value + step).coerceIn(range)) },
                enabled = enabled,
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = "+", fontSize = 20.sp)
            }
        }
    }
}

private fun playAlarm(context: Context) {
    try {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 90)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1500)
        Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 1600L)
    } catch (_: RuntimeException) {
        // 设备没有可用音频输出时忽略
    }

    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    if (vibrator != null && vibrator.hasVibrator()) {
        val pattern = longArrayOf(0L, 300L, 200L, 300L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
