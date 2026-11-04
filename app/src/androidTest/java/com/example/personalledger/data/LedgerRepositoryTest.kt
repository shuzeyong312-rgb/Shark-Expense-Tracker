package com.example.personalledger.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LedgerRepositoryTest {
    private lateinit var db: LedgerDatabase
    private lateinit var repository: LedgerRepository

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LedgerDatabase::class.java).build()
        repository = LedgerRepository(db)
        repository.initialize()
    }
    @After fun tearDown() = db.close()

    @Test fun initializesOnlySystemCategories() = runBlocking {
        assertEquals(27, repository.categories.first().size)
        assertTrue(repository.transactions.first().isEmpty())
        repository.initialize()
        assertEquals(27, repository.categories.first().size)
    }

    @Test fun editingMovesTagAndPreservesCreatedTime() = runBlocking {
        val date = LocalDate.of(2026, 11, 4)
        val id = repository.save(null, "food", 1234, date, " 午饭 ")
        repository.save(null, "food", 100, date, "午饭")
        assertEquals(1, repository.quickTags("food").first().size)
        val original = repository.transactions.first().first { it.id == id }
        repository.save(id, "daily", 2000, date, "午饭")
        val edited = repository.transactions.first().first { it.id == id }
        assertEquals(original.createdAt, edited.createdAt)
        assertNotEquals(original.noteTagId, edited.noteTagId)
        assertEquals("daily", repository.quickTags("daily").first().single().categoryId)
        repository.save(id, "daily", 2000, date, " ")
        assertNull(repository.transactions.first().first { it.id == id }.noteTagId)
        repository.delete(id)
        assertEquals(1, repository.transactions.first().size)
    }

    @Test fun rejectsZeroAndMissingCategory() = runBlocking {
        try { repository.save(null, "food", 0, LocalDate.now(), ""); fail() } catch (_: IllegalArgumentException) { }
        try { repository.save(null, "missing", 100, LocalDate.now(), ""); fail() } catch (_: IllegalArgumentException) { }
        assertTrue(repository.transactions.first().isEmpty())
    }

    @Test fun categoryVisibilityAndCustomNamesAreValidated() = runBlocking {
        val visible = repository.categories.first().filter { it.type == "INCOME" && it.isVisible }
        visible.drop(1).forEach { repository.setCategoryVisible(it.id, false) }
        try { repository.setCategoryVisible(visible.first().id, false); fail() } catch (_: IllegalArgumentException) { }
        repository.saveCategory(null, "INCOME", "稿费", "gamepad")
        try { repository.saveCategory(null, "INCOME", " 稿费 ", "gamepad"); fail() } catch (_: IllegalArgumentException) { }
        assertEquals(1, repository.categories.first().count { it.name == "稿费" })
    }

    @Test fun tenThousandTransactionsCanBeQueriedAndSummed() = runBlocking {
        val entries = List(10_000) { LedgerTransaction(type = "EXPENSE", categoryId = "food", noteTagId = null, noteSnapshot = null, amountMinor = 123, occurredOn = "2026-11-04", createdAt = "2026-11-04T00:00:00Z", updatedAt = "2026-11-04T00:00:00Z") }
        db.ledgerDao().insertTransactions(entries)
        val records = repository.transactions.first()
        assertEquals(10_000, records.size)
        assertEquals(java.math.BigInteger.valueOf(1_230_000L), periodSeries(records, periodFor(LocalDate.of(2026, 11, 4), PeriodKind.MONTH), "EXPENSE").sumBig())
    }

    @Test fun backupRoundTripAndInvalidRestorePreserveData() = runBlocking {
        repository.save(null, "food", 120, LocalDate.of(2026, 11, 4), "午餐,\"面\"")
        val backup = repository.snapshot()
        val decoded = LedgerBackup.parse(backup.json())
        assertEquals(backup, decoded)
        assertTrue(backup.csv().startsWith("\uFEFF"))
        assertTrue(backup.csv().contains("\"午餐,\"\"面\"\"\""))
        repository.save(null, "daily", 200, LocalDate.now(), "")
        try { repository.restore(decoded.copy(transactions = decoded.transactions.map { it.copy(categoryId = "missing") })); fail() } catch (_: IllegalArgumentException) { }
        assertEquals(2, repository.transactions.first().size)
        repository.restore(decoded)
        assertEquals(backup, repository.snapshot())
        try { LedgerBackup.parse(backup.json().replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")); fail() } catch (_: IllegalArgumentException) { }
    }

    @Test fun csvProtectsSpreadsheetFormulaText() = runBlocking {
        repository.save(null, "food", 100, LocalDate.now(), "=1+1")
        val backup = repository.snapshot()
        assertTrue(backup.csv().contains("\"'=1+1\""))
        assertEquals("=1+1", LedgerBackup.parse(backup.json()).transactions.single().noteSnapshot)
    }

    @Test fun databaseFailureRollsBackCompleteRestore() = runBlocking {
        repository.save(null, "food", 100, LocalDate.now(), "原账单")
        val original = repository.snapshot()
        repository.save(null, "daily", 200, LocalDate.now(), "新账单")
        val current = repository.snapshot()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_category BEFORE INSERT ON Category BEGIN SELECT RAISE(ABORT, 'test failure'); END")
        try { repository.restore(original); fail() } catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(current, repository.snapshot())
    }

    @Test fun quickTagsAreIsolatedAndRankByUseThenRecentDate() = runBlocking {
        repository.save(null, "food", 100, LocalDate.of(2026, 1, 1), "早餐")
        val other = repository.save(null, "food", 100, LocalDate.of(2026, 1, 2), "午餐")
        repository.save(null, "daily", 100, LocalDate.of(2026, 1, 3), "早餐")
        assertEquals(listOf("午餐", "早餐"), repository.quickTags("food").first().map { it.displayName })
        repository.save(null, "food", 100, LocalDate.of(2026, 1, 1), "早餐")
        assertEquals(listOf("早餐", "午餐"), repository.quickTags("food").first().map { it.displayName })
        assertEquals(listOf("早餐"), repository.quickTags("daily").first().map { it.displayName })
        repository.delete(other)
        assertEquals(2, repository.quickTags("food").first().size)
    }

    @Test fun backupRejectsBrokenReferencesDuplicatesAndTrailingData() = runBlocking {
        repository.save(null, "food", 100, LocalDate.now(), "午餐")
        val backup = repository.snapshot()
        val broken = backup.copy(transactions = backup.transactions.map { it.copy(categoryId = "daily") })
        try { broken.validate(); fail() } catch (_: IllegalArgumentException) { }
        try { backup.copy(tags = backup.tags + backup.tags).validate(); fail() } catch (_: IllegalArgumentException) { }
        try { LedgerBackup.parse(backup.json() + " garbage"); fail() } catch (_: IllegalArgumentException) { }
        assertEquals(backup, repository.snapshot())
    }

    @Test fun categoryOrderingPersistsAndIncomeRemainsSeparate() = runBlocking {
        val income = repository.categories.first().filter { it.type == "INCOME" && it.isVisible }
        repository.reorderCategories("INCOME", income.map { it.id }.reversed())
        assertEquals(income.map { it.id }.reversed(), repository.categories.first().filter { it.type == "INCOME" && it.isVisible }.map { it.id })
        val id = repository.save(null, "salary", 5000, LocalDate.now(), "工资")
        val item = repository.transactions.first().single { it.id == id }
        assertEquals("INCOME", item.type)
        assertEquals(5000L, item.amountMinor)
        assertEquals("salary", item.categoryId)
        try { repository.reorderCategories("INCOME", listOf(income.first().id)); fail() } catch (_: IllegalArgumentException) { }
    }

    @Test fun editingCustomCategoryRetainsHistoricalRelationship() = runBlocking {
        repository.saveCategory(null, "EXPENSE", "茶饮", "coffee")
        val custom = repository.categories.first().single { it.name == "茶饮" }
        repository.save(null, custom.id, 2500, LocalDate.now(), "茶")
        repository.saveCategory(custom.id, "EXPENSE", "饮料", "bottle")
        repository.setCategoryVisible(custom.id, false)
        val edited = repository.categories.first().single { it.id == custom.id }
        assertEquals("饮料", edited.name)
        assertEquals("bottle", edited.iconKey)
        assertFalse(edited.isVisible)
        assertEquals(custom.id, repository.transactions.first().single().categoryId)
        repository.setCategoryVisible(custom.id, true)
        assertTrue(repository.categories.first().single { it.id == custom.id }.isVisible)
    }
}
