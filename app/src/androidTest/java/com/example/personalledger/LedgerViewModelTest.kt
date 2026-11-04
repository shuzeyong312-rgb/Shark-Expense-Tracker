package com.example.personalledger

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.personalledger.data.LedgerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerViewModelTest {
    @Test fun initializesAndReportsWriteErrors() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build()
        try {
            val model = LedgerViewModel(context, db)
            val categories = withTimeout(10_000) { model.categories.first { it.isNotEmpty() } }
            assertEquals(27, categories.size)
            model.run { model.repository.save(null, "food", 0, java.time.LocalDate.now(), "") }
            assertTrue(withTimeout(10_000) { model.message.first { it != null } }!!.startsWith("操作失败"))
            assertTrue(model.repository.transactions.first().isEmpty())
        } finally { db.close() }
    }
}
