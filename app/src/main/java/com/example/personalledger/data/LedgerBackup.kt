package com.example.personalledger.data

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.time.Instant
import java.time.LocalDate

private fun JSONObject.string(key: String): String = (get(key) as? String) ?: error("Invalid $key")
private fun JSONObject.integer(key: String): Long = when (val value = get(key)) { is Int -> value.toLong(); is Long -> value; else -> error("Invalid $key") }
private fun JSONObject.flag(key: String): Boolean = (get(key) as? Boolean) ?: error("Invalid $key")
private fun JSONObject.optionalString(key: String): String? = if (isNull(key) && has(key)) null else string(key)
private fun JSONObject.optionalLong(key: String): Long? = if (isNull(key) && has(key)) null else integer(key)
private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { get(it) as? JSONObject ?: error("Invalid array item") }

data class LedgerBackup(val categories: List<Category>, val tags: List<NoteTag>, val transactions: List<LedgerTransaction>) {
    fun validate() {
        require(categories.isNotEmpty())
        require(categories.map { it.id }.distinct().size == categories.size)
        require(categories.map { it.type to it.normalizedName }.distinct().size == categories.size)
        require(tags.map { it.id }.distinct().size == tags.size)
        require(tags.map { it.categoryId to it.normalizedName }.distinct().size == tags.size)
        require(transactions.map { it.id }.distinct().size == transactions.size)
        val cats = categories.associateBy { it.id }
        val tagsById = tags.associateBy { it.id }
        val defaults = defaultCategories().associateBy { it.id }
        require(defaults.all { (id, expected) -> cats[id]?.let { it.isSystem && it.name == expected.name && it.type == expected.type && it.iconKey == expected.iconKey } == true })
        categories.forEach {
            require(it.id.isNotBlank() && it.name.isNotBlank() && it.name == it.name.trim() && normalizeName(it.name) == it.normalizedName && it.iconKey.isNotBlank())
            require(it.type in listOf("EXPENSE", "INCOME") && it.sortOrder >= 0)
            require(if (it.isSystem) defaults.containsKey(it.id) else it.id !in defaults && it.name.codePointCount(0, it.name.length) <= 4 && it.iconKey in customIconKeys)
            Instant.parse(it.createdAt)
        }
        listOf("EXPENSE", "INCOME").forEach { type -> require(categories.any { it.type == type && it.isVisible }) }
        tags.forEach {
            require(it.id > 0 && cats.containsKey(it.categoryId) && it.displayName.isNotBlank() && it.displayName == it.displayName.trim() && normalizeName(it.displayName) == it.normalizedName)
            Instant.parse(it.createdAt)
        }
        transactions.forEach {
            require(it.id > 0 && it.amountMinor > 0 && cats[it.categoryId]?.type == it.type)
            require((it.noteTagId == null) == (it.noteSnapshot == null))
            it.noteTagId?.let { id -> require(tagsById[id]?.categoryId == it.categoryId && !it.noteSnapshot.isNullOrBlank() && normalizeName(it.noteSnapshot) == tagsById[id]?.normalizedName) }
            require(LocalDate.parse(it.occurredOn).toString() == it.occurredOn)
            Instant.parse(it.createdAt); Instant.parse(it.updatedAt)
        }
    }

    fun json(): String {
        fun category(c: Category) = JSONObject().put("id", c.id).put("name", c.name).put("normalizedName", c.normalizedName).put("type", c.type).put("iconKey", c.iconKey).put("sortOrder", c.sortOrder).put("isSystem", c.isSystem).put("isVisible", c.isVisible).put("createdAt", c.createdAt)
        fun tag(t: NoteTag) = JSONObject().put("id", t.id).put("categoryId", t.categoryId).put("displayName", t.displayName).put("normalizedName", t.normalizedName).put("createdAt", t.createdAt)
        fun transaction(t: LedgerTransaction) = JSONObject().put("id", t.id).put("type", t.type).put("categoryId", t.categoryId).put("noteTagId", t.noteTagId ?: JSONObject.NULL).put("noteSnapshot", t.noteSnapshot ?: JSONObject.NULL).put("amountMinor", t.amountMinor).put("occurredOn", t.occurredOn).put("createdAt", t.createdAt).put("updatedAt", t.updatedAt)
        return JSONObject().put("schemaVersion", 1).put("appVersion", "1.0").put("exportedAt", Instant.now().toString()).put("categories", JSONArray(categories.map(::category))).put("noteTags", JSONArray(tags.map(::tag))).put("transactions", JSONArray(transactions.map(::transaction))).toString(2)
    }

    fun csv(): String {
        val cats = categories.associateBy { it.id }
        fun quote(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        fun safeText(s: String) = if (s.trimStart().firstOrNull() in listOf('=', '+', '-', '@') || s.firstOrNull() in listOf('\t', '\r', '\n')) "'$s" else s
        val rows = transactions.sortedWith(compareByDescending<LedgerTransaction> { it.occurredOn }.thenByDescending { it.createdAt }).map {
            listOf(it.occurredOn, if (it.type == "INCOME") "收入" else "支出", formatAmountMinor(it.amountMinor), safeText(cats.getValue(it.categoryId).name), safeText(it.noteSnapshot.orEmpty()), it.createdAt, it.updatedAt).joinToString(",", transform = ::quote)
        }
        return "\uFEFF" + (listOf("账单日期,收支类型,金额,一级分类,二级备注,创建时间,最后修改时间") + rows).joinToString("\r\n")
    }

    companion object {
        fun parse(text: String): LedgerBackup {
            val tokener = JSONTokener(text)
            val root = tokener.nextValue() as? JSONObject ?: error("Invalid backup")
            require(tokener.nextClean() == '\u0000')
            require(root.integer("schemaVersion") == 1L)
            root.string("appVersion"); Instant.parse(root.string("exportedAt"))
            val categories = root.getJSONArray("categories").objects().map { Category(it.string("id"), it.string("name"), it.string("normalizedName"), it.string("type"), it.string("iconKey"), Math.toIntExact(it.integer("sortOrder")), it.flag("isSystem"), it.flag("isVisible"), it.string("createdAt")) }
            val tags = root.getJSONArray("noteTags").objects().map { NoteTag(it.integer("id"), it.string("categoryId"), it.string("displayName"), it.string("normalizedName"), it.string("createdAt")) }
            val transactions = root.getJSONArray("transactions").objects().map { LedgerTransaction(it.integer("id"), it.string("type"), it.string("categoryId"), it.optionalLong("noteTagId"), it.optionalString("noteSnapshot"), it.integer("amountMinor"), it.string("occurredOn"), it.string("createdAt"), it.string("updatedAt")) }
            return LedgerBackup(categories, tags, transactions).also { it.validate() }
        }
    }
}
