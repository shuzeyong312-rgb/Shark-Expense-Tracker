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
    @Test fun oldPeriodRemainsSelectable() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build()
        val model = LedgerViewModel(context, db)
        val old = LocalDate.of(2018, 2, 1)
        runBlocking { model.setChart("EXPENSE", PeriodKind.YEAR, old.toString()).join() }
        rule.setContent { ChartScreen(model, java.time.YearMonth.now(), emptyList(), emptyList(), emptyList(), null, {}, { _, _ -> }, {}, {}) }
        rule.waitUntil(10_000) { rule.onAllNodesWithText("2018年").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("2018年").performClick()
        rule.runOnIdle { assertEquals(old.toString(), model.chartAnchor.value) }
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
        val category = defaultCategories().first { it.id == "food" }
        val tag = NoteTag(1, category.id, "性能备注", "性能备注", "")
        val date = LocalDate.now().withDayOfMonth(1)
        val records = List(10_000) { i -> LedgerTransaction(i.toLong() + 1, "EXPENSE", category.id, tag.id, "性能备注", 100, date.toString(), "", "") }
        runBlocking { model.setChart("EXPENSE", PeriodKind.MONTH, date.toString()).join() }
        val before = android.os.Debug.getNativeHeapAllocatedSize()
        val started = android.os.SystemClock.elapsedRealtime()
        var showTag by mutableStateOf(false)
        rule.setContent { if (showTag) TagScreen(model, tag, periodFor(date, PeriodKind.MONTH), listOf(category), records, {}, {}) else ChartScreen(model, java.time.YearMonth.from(date), records, listOf(category), listOf(tag), category, {}, { _, _ -> }, {}, {}) }
        rule.waitUntil(30_000) { rule.onAllNodesWithText("10000 笔").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("chartDetails").performTouchInput { swipeUp() }
        rule.waitForIdle()
        android.util.Log.i("LedgerPerformance", "category ms=${android.os.SystemClock.elapsedRealtime()-started} nativeDelta=${android.os.Debug.getNativeHeapAllocatedSize()-before}")
        rule.runOnIdle { showTag = true }
        rule.onNodeWithText("10000").assertExists()
        rule.onNodeWithTag("tagDetails").performTouchInput { swipeUp() }
        rule.waitForIdle()
        android.util.Log.i("LedgerPerformance", "tag totalMs=${android.os.SystemClock.elapsedRealtime()-started} nativeDelta=${android.os.Debug.getNativeHeapAllocatedSize()-before}")
    }
}
