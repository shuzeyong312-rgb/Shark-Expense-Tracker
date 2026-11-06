package com.example.personalledger

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.personalledger.data.LedgerDatabase
import org.junit.Rule
import org.junit.Test

class CategoryUiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun iconListCanScrollToEndAndSelectLastIcon() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val model = LedgerViewModel(context, Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build())
        rule.setContent { val categories by model.categories.collectAsState(); CategoryManager(model, categories, "EXPENSE") {} }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("＋添加类别").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("＋添加类别").performClick()
        rule.onNodeWithTag("categoryIcons").performScrollToNode(hasContentDescription("轮船"))
        val titleTop = rule.onNodeWithText("添加支出类别").getUnclippedBoundsInRoot().top
        rule.onNodeWithTag("categoryIcons").performTouchInput { down(center); moveBy(Offset(0f, -200f)) }
        org.junit.Assert.assertEquals(titleTop.value, rule.onNodeWithText("添加支出类别").getUnclippedBoundsInRoot().top.value, 1f)
        rule.onNodeWithTag("categoryIcons").performTouchInput { up() }
        rule.onNodeWithTag("categoryIcons").performTouchInput { swipeUp() }
        rule.onNodeWithContentDescription("轮船").assertIsDisplayed().performClick()
        rule.onNodeWithText("类别名称（最多4字）").performTextInput("船票")
        rule.onNodeWithText("完成").performClick()
        rule.waitUntil(10_000) { model.categories.value.any { it.name == "船票" && it.iconKey == "ship" } }
    }

    @Test fun customCategoryCanBeCreated() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val model = LedgerViewModel(context, Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build())
        rule.setContent { val categories by model.categories.collectAsState(); CategoryManager(model, categories, "EXPENSE") {} }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("餐饮").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("＋添加类别").performClick()
        rule.onNodeWithText("类别名称（最多4字）").performTextInput("茶饮")
        rule.onNodeWithText("完成").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("茶饮").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("茶饮").assertExists()
    }

    @Test fun draggingCategoryPersistsNewOrder() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val model = LedgerViewModel(context, Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build())
        rule.setContent { val categories by model.categories.collectAsState(); CategoryManager(model, categories, "EXPENSE") {} }
        rule.waitUntil(10_000) { rule.onAllNodesWithContentDescription("调整餐饮顺序").fetchSemanticsNodes().isNotEmpty() }
        val distance = with(rule.density) { 70.dp.toPx() }
        rule.onNodeWithContentDescription("调整餐饮顺序").performTouchInput { swipe(center, center + Offset(0f, distance), durationMillis = 600) }
        rule.waitUntil(10_000) { model.categories.value.filter { it.type == "EXPENSE" && it.isVisible }.firstOrNull()?.id == "shop" }
    }
}
