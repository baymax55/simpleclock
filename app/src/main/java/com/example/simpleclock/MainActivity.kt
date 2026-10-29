package com.example.simpleclock

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.simpleclock.ui.ClockScreen
import com.example.simpleclock.ui.StopwatchScreen
import com.example.simpleclock.ui.TimerScreen
import com.example.simpleclock.ui.theme.SimpleclockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val landscape = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp }
            // 横屏全屏时用深色（纯黑）背景，OLED 上更省电，夜里也不刺眼
            SimpleclockTheme(
                darkTheme = isSystemInDarkTheme() || landscape,
                fullscreen = landscape
            ) {
                SimpleClockApp()
            }
        }
    }
}

private enum class ClockTab(@param:StringRes val labelRes: Int, @param:DrawableRes val iconRes: Int) {
    Clock(R.string.tab_clock, R.drawable.ic_tab_clock),
    Stopwatch(R.string.tab_stopwatch, R.drawable.ic_tab_stopwatch),
    Timer(R.string.tab_timer, R.drawable.ic_tab_timer)
}

@Composable
fun SimpleClockApp() {
    val landscape = LocalConfiguration.current.let { it.screenWidthDp > it.screenHeightDp }
    FullscreenChrome(enabled = landscape)

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val stateHolder = rememberSaveableStateHolder()

    if (landscape) {
        // 横屏全屏：隐藏导航栏，左右滑动切换页面
        val pagerState = rememberPagerState(initialPage = selectedTab) { ClockTab.entries.size }
        LaunchedEffect(pagerState.currentPage) {
            selectedTab = pagerState.currentPage
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    TabContent(
                        tabIndex = page,
                        stateHolder = stateHolder,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                PageDots(
                    current = pagerState.currentPage,
                    count = ClockTab.entries.size,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                )
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    ClockTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            TabContent(
                tabIndex = selectedTab,
                stateHolder = stateHolder,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

/**
 * 横屏进入沉浸式全屏（隐藏状态栏与导航栏）并保持屏幕常亮；竖屏恢复系统栏。
 */
@Composable
private fun FullscreenChrome(enabled: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current
    val window = (context as? Activity)?.window

    DisposableEffect(enabled, window) {
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        view.keepScreenOn = enabled
        if (controller != null) {
            if (enabled) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            view.keepScreenOn = false
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun PageDots(current: Int, count: Int, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val selected = index == current
            Box(
                modifier = Modifier
                    .size(if (selected) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (selected) 0.75f else 0.25f))
            )
        }
    }
}

@Composable
private fun TabContent(tabIndex: Int, stateHolder: SaveableStateHolder, modifier: Modifier = Modifier) {
    // 用 SaveableStateHolder 保留各页面的状态，切页时秒表 / 计时器不会被重置
    stateHolder.SaveableStateProvider(tabIndex) {
        Box(modifier = modifier) {
            when (ClockTab.entries[tabIndex]) {
                ClockTab.Clock -> ClockScreen(Modifier.fillMaxSize())
                ClockTab.Stopwatch -> StopwatchScreen(Modifier.fillMaxSize())
                ClockTab.Timer -> TimerScreen(Modifier.fillMaxSize())
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun SimpleClockAppPreview() {
    SimpleclockTheme {
        SimpleClockApp()
    }
}
