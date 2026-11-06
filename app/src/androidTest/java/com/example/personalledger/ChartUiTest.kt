package com.example.personalledger

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.personalledger.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ChartUiTest {
    @get:Rule val rule = createComposeRule()
    @Test fun typeMenuShowsSelectionAndSwitchesChart() {
        var visible by mutableStateOf(true)
        var selectedType by mutableStateOf("EXPENSE")
        rule.setContent { ChartTypeOverlay(visible, selectedType, { visible = false }) { selectedType = it; visible = false } }
        rule.onNodeWithTag("chartTypeEXPENSE").assertIsSelected()
        rule.onNodeWithTag("chartTypeINCOME").assertIsNotSelected().performClick()
        rule.runOnIdle { assertEquals("INCOME", selectedType); assertFalse(visible); visible = true }
        rule.onNodeWithTag("chartTypeINCOME").assertIsSelected()
        rule.onNodeWithTag("chartTypeScrim").performClick()
        rule.runOnIdle { assertFalse(visible); assertEquals("INCOME", selectedType) }
    }
    @Test fun categoryAndNoteDrillDownUseSelectedData() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build()
        val model = LedgerViewModel(context, db)
        val today = LocalDate.now()
        val snapshot = runBlocking {
            model.repository.initialize()
            model.repository.save(null, "food", 1000, today, "午餐")
            model.repository.save(null, "daily", 2000, today, "午餐")
            model.setChart("EXPENSE", PeriodKind.MONTH, today.toString()).join()
            model.repository.snapshot()
        }
        var category: Category? = null
        var tag: NoteTag? = null
        rule.setContent {
            var selected by remember { mutableStateOf<Category?>(null) }
            ChartScreen(model, java.time.YearMonth.from(today), snapshot.transactions, snapshot.categories, snapshot.tags, selected, { category = it; selected = it }, { item, _ -> tag = item }, {}, {})
        }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("餐饮").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("餐饮").performClick()
        rule.runOnIdle { assertEquals("food", category?.id) }
        rule.onAllNodesWithText("午餐").onFirst().performClick()
        rule.runOnIdle { assertEquals("food", tag?.categoryId) }
    }
    @Test fun oldPeriodsRestoreSelectionAndAllowReselection() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build()
        val model = LedgerViewModel(context, db)
        val now = LocalDate.now()
        val old = LocalDate.of(2018, now.plusMonths(5).monthValue, 1)
        var showChart by mutableStateOf(true)
        rule.setContent { if (showChart) ChartScreen(model, java.time.YearMonth.now(), emptyList(), emptyList(), emptyList(), null, {}, { _, _ -> }, {}, {}) }
        for (kind in PeriodKind.entries) {
            rule.runOnIdle { showChart = false }
            runBlocking { model.setChart("EXPENSE", kind, old.toString()).join() }
            rule.runOnIdle { showChart = true }
            val label = when (kind) {
                PeriodKind.WEEK -> "${periodFor(old, kind).start.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}周"
                PeriodKind.MONTH -> "${old.monthValue}月"
                PeriodKind.YEAR -> "2018年"
            }
            rule.waitUntil(10_000) { model.period.value == kind && model.chartAnchor.value == old.toString() && rule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
            rule.onNodeWithText(label).assertIsSelected()
            val newLabel = when (kind) { PeriodKind.WEEK -> "上周"; PeriodKind.MONTH -> "${LocalDate.now().monthValue}月"; PeriodKind.YEAR -> "${LocalDate.now().year}年" }
            rule.onNodeWithText(newLabel).performClick()
            rule.waitUntil(10_000) { model.chartAnchor.value != old.toString() }
            rule.onNodeWithText(newLabel).assertIsSelected()
        }
    }

    @Test fun trendTapChoosesNearestPoint() {
        val period = periodFor(LocalDate.of(2026, 11, 2), PeriodKind.WEEK)
        rule.setContent { TrendChart(List(7) { java.math.BigInteger.valueOf(it.toLong() * 100) }, period) }
        rule.onNodeWithTag("trendChart").performTouchInput { click(androidx.compose.ui.geometry.Offset(width * .15f, height / 2f)) }
        rule.onNodeWithText("2026-11-03  1.00").assertExists()
    }

    @Test fun tenThousandBillsRenderAndScrollInDetails() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build()
        val model = LedgerViewModel(context, db)
        val categories = defaultCategories().filter { it.id == "food" || it.id == "daily" }
        val food = categories.first { it.id == "food" }
        val tags = List(1_000) { i -> NoteTag(i.toLong() + 1, if (i < 800) "food" else "daily", "备注%04d".format(i), "备注%04d".format(i), "") }
        val date = LocalDate.now().withDayOfMonth(1)
        val records = List(10_000) { i ->
            val tag = tags[i % tags.size]
            LedgerTransaction(i.toLong() + 1, "EXPENSE", tag.categoryId, tag.id, tag.displayName, 100, date.toString(), "", "")
        }
        runBlocking { model.setChart("EXPENSE", PeriodKind.MONTH, date.toString()).join() }
        var selectedTag by mutableStateOf<NoteTag?>(null)
        var editedId: Long? = null
        rule.setContent {
            val tag = selectedTag
            if (tag != null) TagScreen(model, tag, periodFor(date, PeriodKind.MONTH), categories, records, {}, { selectedTag = null })
            else ChartScreen(model, java.time.YearMonth.from(date), records, categories, tags, food, {}, { item, _ -> selectedTag = item }, { editedId = it.id }, {})
        }
        rule.waitUntil(30_000) { rule.onAllNodesWithText("8000 笔").fetchSemanticsNodes().isNotEmpty() }
        val composed = rule.onAllNodesWithTag("noteRankingRow", useUnmergedTree = true).fetchSemanticsNodes().size
        assertTrue("Expected only viewport rows, found $composed", composed in 1..30)
        rule.onNodeWithTag("chartDetails").performScrollToKey("tag-800")
        rule.onNodeWithText("备注0799").assertExists().performClick()
        rule.onNodeWithText("10").assertExists()
        rule.onNodeWithText("¥10.00").assertExists()
        assertTrue(rule.onAllNodesWithText("备注0799").fetchSemanticsNodes().size >= 2)
        rule.onNodeWithTag("tagDetails").performTouchInput { swipeUp() }
        rule.runOnIdle { selectedTag = null }
        rule.onNodeWithTag("chartDetails").performScrollToKey(1L)
        rule.onNodeWithText("备注0000").performClick()
        rule.runOnIdle { assertEquals(1L, editedId) }
        rule.onNodeWithTag("chartDetails").performTouchInput { swipeUp() }
    }
}
