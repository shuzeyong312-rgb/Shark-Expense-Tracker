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
}
