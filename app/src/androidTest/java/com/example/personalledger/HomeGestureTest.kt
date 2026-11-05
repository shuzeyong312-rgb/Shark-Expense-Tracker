package com.example.personalledger

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth

class HomeGestureTest {
    @get:Rule val rule = createComposeRule()
    @Test fun emptyMonthPullMovesAcrossYearBoundary() {
        rule.setContent {
            var month by remember { mutableStateOf(YearMonth.of(2026, 1)) }
            val model = remember { LedgerViewModel(ApplicationProvider.getApplicationContext()) }
            HomeScreen(model, month, emptyList(), emptyList(), { month = it }, {}, {})
        }
        rule.onNodeWithText("2026年").assertExists()
        rule.onNodeWithTag("billList").performTouchInput { swipeUp() }
        rule.waitForIdle()
        rule.onNodeWithText("2025年").assertExists()
        rule.onNodeWithText("12 月 ▾").assertExists()
    }

    @Test fun pullHintAppearsOnlyWhileDraggingFromBottom() {
        rule.setContent {
            var month by remember { mutableStateOf(YearMonth.of(2026, 1)) }
            val model = remember { LedgerViewModel(ApplicationProvider.getApplicationContext()) }
            HomeScreen(model, month, emptyList(), emptyList(), { month = it }, {}, {})
        }
        rule.onNodeWithText("上拉查看上月数据").assertDoesNotExist()
        rule.onNodeWithTag("billList").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(0f, -100f))
        }
        rule.onNodeWithText("上拉查看上月数据").assertExists()
        rule.onNodeWithTag("billList").performTouchInput { up() }
        rule.onNodeWithText("上拉查看上月数据").assertDoesNotExist()
    }

    @Test fun pullDownMovesToNextMonthAcrossYearBoundary() {
        val year = YearMonth.now().year - 2
        rule.setContent {
            var month by remember { mutableStateOf(YearMonth.of(year, 12)) }
            val model = remember { LedgerViewModel(ApplicationProvider.getApplicationContext()) }
            HomeScreen(model, month, emptyList(), emptyList(), { month = it }, {}, {})
        }
        rule.onNodeWithText("下拉查看下月数据").assertDoesNotExist()
        rule.onNodeWithTag("billList").performTouchInput { down(center); moveBy(androidx.compose.ui.geometry.Offset(0f, 100f)) }
        rule.onNodeWithText("下拉查看下月数据").assertExists()
        rule.onNodeWithTag("billList").performTouchInput { up() }
        rule.onNodeWithTag("billList").performTouchInput { swipeDown() }
        rule.onNodeWithText("${year + 1}年").assertExists()
        rule.onNodeWithText("1 月 ▾").assertExists()
    }

    @Test fun currentMonthPullDownHasNoHintOrNavigation() {
        val current = YearMonth.now()
        rule.setContent {
            var month by remember { mutableStateOf(current) }
            val model = remember { LedgerViewModel(ApplicationProvider.getApplicationContext()) }
            HomeScreen(model, month, emptyList(), emptyList(), { month = it }, {}, {})
        }
        rule.onNodeWithTag("billList").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(0f, 100f))
        }
        rule.onNodeWithText("下拉查看下月数据").assertDoesNotExist()
        rule.onNodeWithText("松开查看下月数据").assertDoesNotExist()
        rule.onNodeWithTag("billList").performTouchInput { up() }
        rule.onNodeWithText("${current.monthValue} 月 ▾").assertExists()
    }

    @Test fun monthPickerRequiresConfirmation() {
        rule.setContent {
            var month by remember { mutableStateOf(YearMonth.of(2026, 1)) }
            val model = remember { LedgerViewModel(ApplicationProvider.getApplicationContext()) }
            HomeScreen(model, month, emptyList(), emptyList(), { month = it }, {}, {})
        }
        rule.onNodeWithText("1 月 ▾").performClick()
        rule.onNodeWithText("2 月").performClick()
        rule.onNodeWithText("取消").performClick()
        rule.onNodeWithText("1 月 ▾").assertExists()
        rule.onNodeWithText("1 月 ▾").performClick()
        rule.onNodeWithText("2 月").performClick()
        rule.onNodeWithText("确定").performClick()
        rule.onNodeWithText("2 月 ▾").assertExists()
    }
}
