package com.example.personalledger

import android.content.Context
import android.app.Application
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.personalledger.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

private val Context.preferences by preferencesDataStore("ledger_preferences")
private val monthKey = stringPreferencesKey("month")
private val chartTypeKey = stringPreferencesKey("chart_type")
private val periodKey = stringPreferencesKey("period")
private val anchorKey = stringPreferencesKey("chart_anchor")

class LedgerViewModel(private val context: Application, private val database: LedgerDatabase = Room.databaseBuilder(context, LedgerDatabase::class.java, "ledger.db").build()) : ViewModel() {
    val repository = LedgerRepository(database)
    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = repository.transactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val tags = repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val month = context.preferences.data.map { runCatching { YearMonth.parse(it[monthKey]) }.getOrNull() ?: YearMonth.now() }.stateIn(viewModelScope, SharingStarted.Eagerly, YearMonth.now())
    val chartType = context.preferences.data.map { it[chartTypeKey] ?: "EXPENSE" }.stateIn(viewModelScope, SharingStarted.Eagerly, "EXPENSE")
    val period = context.preferences.data.map { runCatching { PeriodKind.valueOf(it[periodKey] ?: "WEEK") }.getOrDefault(PeriodKind.WEEK) }.stateIn(viewModelScope, SharingStarted.Eagerly, PeriodKind.WEEK)
    val chartAnchor = context.preferences.data.map { it[anchorKey] }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    init { run { repository.initialize() } }
    fun clearMessage() { _message.value = null }
    fun showMessage(message: String) { _message.value = message }
    fun run(success: String? = null, action: suspend () -> Unit) = viewModelScope.launch {
        try { action(); if (success != null) _message.value = success }
        catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (e: Exception) { _message.value = "操作失败：${e.localizedMessage ?: "请重试"}" }
    }
    suspend fun updateMonth(value: YearMonth) { context.preferences.edit {
        it[monthKey] = value.toString()
        if (it[periodKey] != "WEEK") it.remove(anchorKey)
    } }
    fun setMonth(value: YearMonth) = run { updateMonth(value) }
    fun setChart(type: String, kind: PeriodKind, anchor: String?) = run { context.preferences.edit {
        it[chartTypeKey] = type; it[periodKey] = kind.name
        if (anchor == null) it.remove(anchorKey) else it[anchorKey] = anchor
    } }
    fun export(uri: Uri, json: Boolean) = run("文件已保存") {
        val backup = repository.snapshot()
        val content = if (json) backup.json() else backup.csv()
        withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(content.toByteArray(Charsets.UTF_8)) } }
    }
    suspend fun readBackup(uri: Uri): LedgerBackup = withContext(Dispatchers.IO) {
        val text = requireNotNull(context.contentResolver.openInputStream(uri)).bufferedReader(Charsets.UTF_8).use { it.readText() }
        LedgerBackup.parse(text)
    }
    override fun onCleared() { database.close() }
}
