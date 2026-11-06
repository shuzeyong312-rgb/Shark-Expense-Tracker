package com.example.personalledger

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.BackEventCompat
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.pressBack
import com.example.personalledger.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AnimationUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var model: LedgerViewModel

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        model = LedgerViewModel(context, Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build())
        rule.setContent { LedgerApp(model) }
        rule.waitUntil(10_000) { model.categories.value.isNotEmpty() }
        runBlocking { model.setMonth(java.time.YearMonth.now()).join(); model.setChart("EXPENSE", PeriodKind.MONTH, null).join() }
        rule.waitForIdle()
    }

    private fun openRecord() {
        rule.onNodeWithText("＋").performClick()
        rule.onNodeWithText("餐饮").performClick()
    }

    private fun finishAnimation() {
        rule.mainClock.advanceTimeBy(1_000)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
    }

    @Test fun recordCancelAndSystemBackKeepDraftUntilSaveFinishes() {
        openRecord()
        rule.onNodeWithText("1", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("noteInput").performTextInput("动效草稿")
        rule.onNodeWithTag("noteInput").performImeAction()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("完成").fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.autoAdvance = false
        rule.onAllNodesWithText("取消").onLast().performClick()
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithTag("noteInput").assertExists()
        finishAnimation()
        rule.onNodeWithTag("noteInput").assertDoesNotExist()
        rule.onNodeWithText("餐饮").performClick()
        rule.onNodeWithTag("noteInput").assertTextContains("动效草稿")
        rule.onNodeWithText("¥1").assertExists()
        rule.waitForIdle()
        pressBack()
        rule.waitForIdle()
        rule.onNodeWithTag("noteInput").assertDoesNotExist()
        rule.onNodeWithText("餐饮").performClick()
        rule.onNodeWithTag("noteInput").assertTextContains("动效草稿")
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("完成").performClick()
        rule.waitUntil(10_000) { model.transactions.value.any { it.noteSnapshot == "动效草稿" } }
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithTag("noteInput").assertExists()
        finishAnimation()
        rule.onNodeWithTag("noteInput").assertDoesNotExist()
        rule.onNodeWithText("动效草稿").assertExists()
    }

    @Test fun monthCancelAndConfirmWaitForExit() {
        repeat(2) { index ->
            rule.onNodeWithText("${LocalDate.now().monthValue} 月 ▾").performClick()
            rule.waitForIdle()
            rule.mainClock.autoAdvance = false
            rule.onNodeWithText(if (index == 0) "取消" else "确定").performClick()
            rule.mainClock.advanceTimeBy(32)
            rule.onNodeWithText("选择月份").assertExists()
            finishAnimation()
            rule.onNodeWithText("选择月份").assertDoesNotExist()
        }
    }

    @Test fun categoryCancelSaveAndReturn() {
        rule.onNodeWithText("我的").performClick()
        rule.onNodeWithText("分类管理").performClick()
        rule.onNodeWithText("＋添加类别").performClick()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("取消").performClick()
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithText("添加支出类别").assertExists()
        finishAnimation()
        rule.onNodeWithText("＋添加类别").performClick()
        rule.onNodeWithText("类别名称（最多4字）").performTextInput("动效")
        rule.onNodeWithText("完成").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("添加支出类别").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("编辑").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("取消").performClick()
        rule.onNodeWithText("编辑").performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performTextReplacement("更新")
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("完成").performClick()
        rule.waitUntil(10_000) { model.categories.value.any { it.name == "更新" } }
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithText("编辑支出类别").assertExists()
        finishAnimation()
        rule.onNodeWithText("编辑支出类别").assertDoesNotExist()
        pressBack()
        rule.onNodeWithText("分类管理").assertExists()
        rule.onNodeWithText("＋添加类别").assertDoesNotExist()
    }

    @Test fun chartDetailsReturnAndTabsHaveNoHorizontalMotion() {
        runBlocking { model.repository.save(null, "food", 100, LocalDate.now(), "动效备注") }
        rule.waitUntil(10_000) { model.transactions.value.isNotEmpty() }
        rule.onNodeWithText("图表").performClick()
        rule.onNodeWithText("餐饮").performClick()
        rule.onNodeWithTag("chartDetails").performScrollToNode(hasText("动效备注"))
        rule.onAllNodesWithText("动效备注").onFirst().performClick()
        rule.onNodeWithTag("tagDetails").assertExists()
        pressBack()
        rule.onNodeWithTag("chartDetails").assertExists()
        pressBack()
        rule.onNodeWithTag("chartTypeSelector").assertExists()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("明细").performClick()
        rule.mainClock.advanceTimeBy(112)
        assertEquals(0f, rule.onNodeWithTag("billList").getUnclippedBoundsInRoot().left.value, 1f)
        finishAnimation()
        repeat(4) {
            rule.onNodeWithText("＋").performClick()
            rule.onNodeWithText("取消").performClick()
            rule.onNodeWithText("＋").assertExists()
        }
    }

    @Test fun predictiveBackCanCancelThenCommit() {
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("＋").performClick()
        rule.mainClock.advanceTimeBy(64)
        assertTrue(rule.onNodeWithTag("recordPage").getUnclippedBoundsInRoot().left.value > 0f)
        finishAnimation()
        val dispatcher = rule.activity.onBackPressedDispatcher
        rule.mainClock.autoAdvance = false
        rule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 500f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(200f, 500f, .5f, BackEventCompat.EDGE_LEFT))
        }
        rule.mainClock.advanceTimeBy(64)
        rule.onNodeWithText("取消").assertExists()
        assertTrue(rule.onNodeWithTag("recordPage").getUnclippedBoundsInRoot().left.value > 0f)
        rule.runOnUiThread { dispatcher.dispatchOnBackCancelled() }
        finishAnimation()
        rule.onNodeWithText("餐饮").assertExists()
        assertEquals(0f, rule.onNodeWithTag("recordPage").getUnclippedBoundsInRoot().left.value, 1f)
        rule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 500f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(300f, 500f, .8f, BackEventCompat.EDGE_LEFT))
            dispatcher.onBackPressed()
        }
        rule.waitForIdle()
        rule.onNodeWithText("＋").assertExists()
        rule.onNodeWithText("取消").assertDoesNotExist()
    }

    @Test fun repeatedBackDuringExitKeepsRecordDraftAndParent() {
        openRecord()
        rule.onNodeWithText("2", useUnmergedTree = true).performClick()
        rule.onAllNodesWithText("餐饮").onLast().performClick()
        rule.onNodeWithText("设置").performClick()
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("‹").performClick()
        rule.onNodeWithText("‹").performClick()
        rule.mainClock.advanceTimeBy(64)
        rule.onNodeWithTag("recordPage").assertExists()
        finishAnimation()
        rule.onNodeWithText("餐饮").performClick()
        rule.onNodeWithText("¥2").assertExists()
    }
}
