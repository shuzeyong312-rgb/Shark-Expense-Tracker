package com.example.personalledger

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class LedgerUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun recordFlowSavesAndDeletesBill() {
        val note = "界面测试${System.nanoTime()}"
        rule.onNodeWithText("＋").performClick()
        rule.onNodeWithText("餐饮").assertExists()
        rule.onNodeWithTag("noteInput").assertDoesNotExist()
        rule.onNodeWithText("餐饮").performClick()
        rule.onNodeWithText("0", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithText("1", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("noteInput").performTextInput(note)
        rule.onNodeWithTag("noteInput").performImeAction()
        rule.onNodeWithText("完成").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("noteInput").fetchSemanticsNodes().isEmpty() && rule.onAllNodesWithText(note).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(note).performTouchInput { longClick() }
        rule.onNodeWithText("删除").performClick()
        rule.onNodeWithText("删除", useUnmergedTree = true).performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText(note).fetchSemanticsNodes().isEmpty() }
    }

    @Test fun assistantInputIsDisabled() {
        rule.onNodeWithText("账伴").performClick()
        rule.onNodeWithText("和账伴聊聊你的账单…").assertIsNotEnabled()
    }

    @Test fun switchingCategoryKeepsDraft() {
        rule.onNodeWithText("＋").performClick()
        rule.onNodeWithText("餐饮").performClick()
        rule.onNodeWithText("1", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("noteInput").performTextInput("草稿")
        rule.onNodeWithTag("noteInput").performImeAction()
        rule.onAllNodesWithText("餐饮").onLast().performClick()
        rule.onNodeWithText("日用").performClick()
        rule.onNodeWithTag("noteInput").assertTextContains("草稿")
        rule.onNodeWithText("¥1").assertExists()
    }
    @Test fun newDraftSurvivesActivityRecreation() {
        rule.onNodeWithText("＋").performClick()
        rule.onNodeWithText("餐饮").performClick()
        rule.onNodeWithText("1", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("noteInput").performTextInput("旋转草稿")
        rule.onNodeWithTag("noteInput").performImeAction()
        rule.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        rule.waitUntil(10_000) { rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("noteInput").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("noteInput").assertTextContains("旋转草稿")
        rule.onNodeWithText("¥1").assertExists()
        rule.onNodeWithText("记一笔").assertExists()
    }

    @Test fun editDraftSurvivesActivityRecreation() {
        val note = "编辑恢复${System.nanoTime()}"
        rule.onNodeWithText("＋").performClick()
        rule.onNodeWithText("餐饮").performClick()
        rule.onNodeWithText("1", useUnmergedTree = true).performClick()
        rule.onNodeWithTag("noteInput").performTextInput(note)
        rule.onNodeWithTag("noteInput").performImeAction()
        rule.onNodeWithText("完成").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText(note).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(note).performClick()
        rule.onNodeWithTag("noteInput").performTextInput("修改")
        rule.onNodeWithTag("noteInput").performImeAction()
        rule.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        rule.waitUntil(10_000) { rule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
        rule.waitUntil(10_000) { rule.onAllNodesWithTag("noteInput").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("编辑账单").assertExists()
        rule.onNodeWithTag("noteInput").assertTextContains("修改" + note)
        rule.onNodeWithText("完成").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("修改" + note).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("修改" + note).performTouchInput { longClick() }
        rule.onNodeWithText("删除").performClick()
        rule.onNodeWithText("删除", useUnmergedTree = true).performClick()
    }
}
