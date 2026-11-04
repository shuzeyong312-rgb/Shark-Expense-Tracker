package com.example.personalledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate

@Entity(indices = [Index(value = ["type", "normalizedName"], unique = true)])
data class Category(@PrimaryKey val id: String, val name: String, val normalizedName: String, val type: String, val iconKey: String, val sortOrder: Int, val isSystem: Boolean, val isVisible: Boolean, val createdAt: String)

@Entity(foreignKeys = [ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT)], indices = [Index(value = ["categoryId", "normalizedName"], unique = true), Index(value = ["id", "categoryId"], unique = true)])
data class NoteTag(@PrimaryKey(autoGenerate = true) val id: Long = 0, val categoryId: String, val displayName: String, val normalizedName: String, val createdAt: String)

@Entity(tableName = "ledger_transactions", foreignKeys = [ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = NoteTag::class, parentColumns = ["id", "categoryId"], childColumns = ["noteTagId", "categoryId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("occurredOn"), Index(value = ["type", "occurredOn"]), Index(value = ["categoryId", "occurredOn"]), Index(value = ["noteTagId", "occurredOn"]), Index(value = ["noteTagId", "categoryId"])])
data class LedgerTransaction(@PrimaryKey(autoGenerate = true) val id: Long = 0, val type: String, val categoryId: String, val noteTagId: Long?, val noteSnapshot: String?, val amountMinor: Long, val occurredOn: String, val createdAt: String, val updatedAt: String)

@Dao
interface LedgerDao {
    @Query("SELECT * FROM Category ORDER BY type, sortOrder, id") fun categories(): Flow<List<Category>>
    @Query("SELECT * FROM Category WHERE id = :id") suspend fun category(id: String): Category?
    @Query("SELECT count(*) FROM Category") suspend fun categoryCount(): Int
    @Query("SELECT * FROM Category WHERE type = :type ORDER BY sortOrder, id") suspend fun categoriesOfType(type: String): List<Category>
    @Query("SELECT * FROM Category") suspend fun allCategories(): List<Category>
    @Query("SELECT * FROM NoteTag") suspend fun allTags(): List<NoteTag>
    @Query("SELECT * FROM NoteTag") fun tags(): Flow<List<NoteTag>>
    @Query("SELECT * FROM ledger_transactions ORDER BY occurredOn DESC, createdAt DESC, id DESC") suspend fun allTransactions(): List<LedgerTransaction>
    @Query("DELETE FROM ledger_transactions") suspend fun clearTransactions()
    @Query("DELETE FROM NoteTag") suspend fun clearTags()
    @Query("DELETE FROM Category") suspend fun clearCategories()
    @Insert suspend fun insertTags(tags: List<NoteTag>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCategories(categories: List<Category>)
    @Update suspend fun updateCategory(category: Category)
    @Query("SELECT * FROM NoteTag WHERE categoryId = :categoryId AND normalizedName = :name") suspend fun tag(categoryId: String, name: String): NoteTag?
    @Insert suspend fun insertTag(tag: NoteTag): Long
    @Query("SELECT NoteTag.* FROM NoteTag LEFT JOIN ledger_transactions t ON t.noteTagId = NoteTag.id WHERE NoteTag.categoryId = :categoryId GROUP BY NoteTag.id ORDER BY count(t.id) DESC, max(t.occurredOn) DESC, NoteTag.normalizedName ASC LIMIT 9") fun quickTags(categoryId: String): Flow<List<NoteTag>>
    @Query("SELECT * FROM ledger_transactions ORDER BY occurredOn DESC, createdAt DESC, id DESC") fun transactions(): Flow<List<LedgerTransaction>>
    @Query("SELECT * FROM ledger_transactions WHERE id = :id") suspend fun transaction(id: Long): LedgerTransaction?
    @Insert suspend fun insertTransaction(transaction: LedgerTransaction): Long
    @Insert suspend fun insertTransactions(transactions: List<LedgerTransaction>)
    @Update suspend fun updateTransaction(transaction: LedgerTransaction)
    @Query("DELETE FROM ledger_transactions WHERE id = :id") suspend fun deleteTransaction(id: Long)
}

@Database(entities = [Category::class, NoteTag::class, LedgerTransaction::class], version = 1, exportSchema = true)
abstract class LedgerDatabase : RoomDatabase() { abstract fun ledgerDao(): LedgerDao }

fun normalizeName(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC).replace(Regex("\\s+"), " ").lowercase(java.util.Locale.ROOT)

class LedgerRepository(private val db: LedgerDatabase) {
    private val dao = db.ledgerDao()
    val categories = dao.categories()
    val transactions = dao.transactions()
    val tags = dao.tags()
    fun quickTags(categoryId: String) = dao.quickTags(categoryId)

    suspend fun snapshot(): LedgerBackup = db.withTransaction { LedgerBackup(dao.allCategories(), dao.allTags(), dao.allTransactions()) }

    suspend fun restore(backup: LedgerBackup) {
        backup.validate()
        db.withTransaction {
            dao.clearTransactions()
            dao.clearTags()
            dao.clearCategories()
            dao.insertCategories(backup.categories)
            dao.insertTags(backup.tags)
            dao.insertTransactions(backup.transactions)
        }
    }

    suspend fun initialize() = db.withTransaction {
        if (dao.categoryCount() == 0) dao.insertCategories(defaultCategories())
    }

    suspend fun save(id: Long?, categoryId: String, amountMinor: Long, occurredOn: LocalDate, note: String) = db.withTransaction {
        require(amountMinor > 0) { "金额必须大于零" }
        val category = requireNotNull(dao.category(categoryId))
        val old = id?.let { requireNotNull(dao.transaction(it)) }
        val now = Instant.now().toString()
        val display = note.trim().ifEmpty { null }
        val tagId = display?.let {
            val normalized = normalizeName(it)
            require(normalized.isNotEmpty())
            dao.tag(categoryId, normalized)?.id ?: dao.insertTag(NoteTag(categoryId = categoryId, displayName = it, normalizedName = normalized, createdAt = now))
        }
        val transaction = LedgerTransaction(id = old?.id ?: 0, type = category.type, categoryId = categoryId, noteTagId = tagId, noteSnapshot = display, amountMinor = amountMinor, occurredOn = occurredOn.toString(), createdAt = old?.createdAt ?: now, updatedAt = now)
        if (old == null) dao.insertTransaction(transaction) else { dao.updateTransaction(transaction); old.id }
    }
    suspend fun delete(id: Long) = db.withTransaction { dao.deleteTransaction(id) }

    suspend fun setCategoryVisible(id: String, visible: Boolean) = db.withTransaction {
        val category = requireNotNull(dao.category(id))
        val categories = dao.categoriesOfType(category.type)
        require(visible || !category.isVisible || categories.count { it.isVisible } > 1) { "至少保留一个可用分类" }
        val order = if (visible && !category.isVisible) (categories.maxOfOrNull { it.sortOrder } ?: -1) + 1 else category.sortOrder
        dao.updateCategory(category.copy(isVisible = visible, sortOrder = order))
    }

    suspend fun reorderCategories(type: String, ids: List<String>) = db.withTransaction {
        val visible = dao.categoriesOfType(type).filter { it.isVisible }
        require(ids.size == visible.size && ids.toSet() == visible.map { it.id }.toSet())
        val byId = visible.associateBy { it.id }
        ids.forEachIndexed { index, id -> dao.updateCategory(byId.getValue(id).copy(sortOrder = index)) }
    }

    suspend fun saveCategory(id: String?, type: String, name: String, iconKey: String) = db.withTransaction {
        require(type == "EXPENSE" || type == "INCOME")
        val display = name.trim()
        require(display.isNotEmpty() && display.codePointCount(0, display.length) <= 4 && iconKey in customIconKeys) { "请输入不超过四个字的类别名称并选择图标" }
        val normalized = normalizeName(display)
        val categories = dao.categoriesOfType(type)
        require(categories.none { it.normalizedName == normalized && it.id != id }) { "该收支类型下已存在同名类别" }
        if (id == null) {
            dao.insertCategories(listOf(Category(java.util.UUID.randomUUID().toString(), display, normalized, type, iconKey, (categories.maxOfOrNull { it.sortOrder } ?: -1) + 1, false, true, Instant.now().toString())))
        } else {
            val old = requireNotNull(dao.category(id))
            require(!old.isSystem && old.type == type)
            dao.updateCategory(old.copy(name = display, normalizedName = normalized, iconKey = iconKey))
        }
    }
}

internal fun defaultCategories(): List<Category> {
    val now = Instant.now().toString()
    val expense = listOf("food|餐饮|food", "shop|购物|bag", "daily|日用|daily", "traffic|交通|bus", "sport|运动|bike", "fun|娱乐|mic", "comm|通讯|phone", "clothes|服饰|shirt", "house|住房|house", "medical|医疗|medical", "study|学习|study", "pet|宠物|pet", "gift|礼物|gift", "office|办公|briefcase", "repair|维修|wrench", "relatives|亲友|family", "express|快递|package", "cashgift|礼金|cash", "digital|数码|device", "social|社交|chat", "travel|旅行|plane")
    val income = listOf("salary|工资|wallet", "parttime|兼职|briefcase", "finance|理财|chart", "giftmoney|礼金|cash", "otherincome|其他|wallet", "bonus|奖金|gift")
    return listOf("EXPENSE" to expense, "INCOME" to income).flatMap { (type, items) -> items.mapIndexed { index, entry ->
        val (id, name, icon) = entry.split('|')
        Category(id, name, normalizeName(name), type, icon, index, true, index < if (type == "EXPENSE") 13 else 5, now)
    } }
}
