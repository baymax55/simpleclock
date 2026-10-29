package com.example.simpleclock.ui

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleclock.R
import kotlinx.coroutines.delay

@Composable
fun StopwatchScreen(modifier: Modifier = Modifier) {
    var running by rememberSaveable { mutableStateOf(false) }
    var accumulated by rememberSaveable { mutableLongStateOf(0L) }
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    val laps: SnapshotStateList<Long> = rememberSaveable(
        saver = listSaver<SnapshotStateList<Long>, Long>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf() }

    // 用 elapsedRealtime 计算，不受系统时间修改影响
    LaunchedEffect(running) {
        while (running) {
            now = SystemClock.elapsedRealtime()
            delay(16L)
        }
    }

    val elapsed = accumulated + if (running) (now - startedAt).coerceAtLeast(0L) else 0L

    val onReset: () -> Unit = {
        running = false
        accumulated = 0L
        laps.clear()
    }
    val onLap: () -> Unit = { laps.add(elapsed) }
    val onToggle: () -> Unit = {
        if (running) {
            accumulated += SystemClock.elapsedRealtime() - startedAt
            running = false
        } else {
            startedAt = SystemClock.elapsedRealtime()
            now = startedAt
            running = true
        }
    }
    val primaryLabel = when {
        running -> R.string.action_pause
        elapsed > 0L -> R.string.action_resume
        else -> R.string.action_start
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight

        if (landscape) {
            // 横屏：计时在左，计次列表在右
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    StopwatchTime(elapsed)
                    Spacer(Modifier.height(24.dp))
                    StopwatchActions(
                        primaryLabel = primaryLabel,
                        resetEnabled = running || elapsed > 0L,
                        lapEnabled = running,
                        onReset = onReset,
                        onLap = onLap,
                        onToggle = onToggle
                    )
                }
                if (laps.isNotEmpty()) {
                    VerticalDivider()
                    LapList(laps, Modifier.weight(1f).fillMaxHeight())
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StopwatchTime(elapsed)
                }
                if (laps.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                    LapList(laps, Modifier.weight(1f))
                }
                StopwatchActions(
                    primaryLabel = primaryLabel,
                    resetEnabled = running || elapsed > 0L,
                    lapEnabled = running,
                    onReset = onReset,
                    onLap = onLap,
                    onToggle = onToggle,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun StopwatchTime(elapsed: Long) {
    val landscape = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp }
    val fontSize = with(LocalDensity.current) { (if (landscape) 96.dp else 52.dp).toSp() }
    Text(
        text = formatTime(elapsed, hundredths = true),
        fontSize = fontSize,
        fontWeight = FontWeight.Light,
        fontFamily = FontFamily.Monospace,
        maxLines = 1,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun StopwatchActions(
    primaryLabel: Int,
    resetEnabled: Boolean,
    lapEnabled: Boolean,
    onReset: () -> Unit,
    onLap: () -> Unit,
    onToggle: () -> Unit,
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
        OutlinedButton(
            onClick = onLap,
            enabled = lapEnabled,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (lapEnabled) 0.6f else 0.3f)),
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.action_lap))
        }
        Button(onClick = onToggle, modifier = Modifier.weight(1.2f)) {
            Text(stringResource(primaryLabel))
        }
    }
}

@Composable
private fun LapList(laps: List<Long>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(laps) { index, total ->
            val split = total - if (index > 0) laps[index - 1] else 0L
            LapRow(
                number = index + 1,
                split = split,
                total = total,
                highlight = index == laps.lastIndex
            )
        }
    }
}

@Composable
private fun LapRow(number: Int, split: Long, total: Long, highlight: Boolean) {
    val splitColor = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.lap_number, number),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "+" + formatTime(split, hundredths = true),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = splitColor,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.2f)
            )
            Text(
                text = formatTime(total, hundredths = true),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.2f)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
    }
}
