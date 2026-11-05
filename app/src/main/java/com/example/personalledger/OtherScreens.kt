package com.example.personalledger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.personalledger.data.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MineScreen(model: LedgerViewModel, transactions: List<LedgerTransaction>, manage: () -> Unit) {
    var pending by remember { mutableStateOf<LedgerBackup?>(null) }
    var confirm by remember { mutableIntStateOf(0) }
    var info by remember { mutableStateOf<String?>(null) }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> if (uri != null) model.export(uri, false) }
    val createJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) model.export(uri, true) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) model.run { pending = model.readBackup(uri); confirm = 1 } }
    val timestamp = { LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) }
    Column(Modifier.fillMaxSize().background(pageGray).verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(yellow).padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).background(Color(0xFFFFF6CE), CircleShape).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { AvatarFace(Modifier.size(24.dp)) }
                Column(Modifier.padding(start = 14.dp)) { Text("我的记账本", fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("简单记录，让每一笔都清晰", Modifier.padding(top = 5.dp), fontSize = 12.sp) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 14.dp)) {
                listOf(transactions.map { it.occurredOn }.distinct().size.toString() to "记账天数", transactions.size.toString() to "记账总笔数", "—" to "连续打卡").forEach { (value, title) -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, fontSize = 20.sp); Text(title, fontSize = 12.sp, color = Color(0xFF7B6A2D)) } }
            }
        }
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp).background(Color.White, RoundedCornerShape(12.dp))) { MenuRow("分类管理", manage) }
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp).background(Color.White, RoundedCornerShape(12.dp))) {
            MenuRow("导出账单数据") { createCsv.launch("ledger-${timestamp()}.csv") }
            MenuRow("数据备份") { createJson.launch("ledger-backup-${timestamp()}.json") }
            MenuRow("从备份恢复") { open.launch(arrayOf("application/json")) }
            MenuRow("数据存储说明") { info = "账单保存在本机。备份文件未加密，请妥善保管。" }
        }
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp).background(Color.White, RoundedCornerShape(12.dp))) { MenuRow("关于") { info = "个人记账 APP V1" } }
        Text("无广告 · 无会员 · 数据由自己掌控", Modifier.align(Alignment.CenterHorizontally).padding(16.dp), color = Color.Gray, fontSize = 12.sp)
    }
    if (confirm == 1 && pending != null) AlertDialog(onDismissRequest = { confirm = 0; pending = null }, title = { Text("恢复备份") }, text = { Text("备份包含 ${pending!!.categories.size} 个分类、${pending!!.tags.size} 个备注标签和 ${pending!!.transactions.size} 笔账单。恢复将覆盖当前全部账务数据。") }, confirmButton = { TextButton(onClick = { confirm = 2 }) { Text("继续") } }, dismissButton = { TextButton(onClick = { confirm = 0; pending = null }) { Text("取消") } })
    if (confirm == 2 && pending != null) AlertDialog(onDismissRequest = { confirm = 0; pending = null }, title = { Text("再次确认覆盖") }, text = { Text("当前全部分类、备注和账单将被备份内容替换。此操作无法撤销。") }, confirmButton = { TextButton(onClick = { val backup = pending!!; confirm = 0; pending = null; model.run("恢复成功") { model.repository.restore(backup) } }) { Text("确认恢复") } }, dismissButton = { TextButton(onClick = { confirm = 0; pending = null }) { Text("取消") } })
    if (info != null) AlertDialog(onDismissRequest = { info = null }, title = { Text("说明") }, text = { Text(info.orEmpty()) }, confirmButton = { TextButton(onClick = { info = null }) { Text("确定") } })
}

@Composable
fun MenuRow(text: String, onClick: () -> Unit) {
    val symbol = when (text) { "分类管理" -> "▦"; "导出账单数据", "数据备份" -> "⇩"; "从备份恢复" -> "⇧"; "数据存储说明" -> "◉"; else -> "ⓘ" }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 17.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).background(Color(0xFFFFF6CE), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Text(symbol, fontSize = 16.sp) }
        Text(text, Modifier.weight(1f).padding(start = 13.dp), fontFamily = androidx.compose.ui.text.font.FontFamily.Serif)
        Text("›", color = Color.Gray)
    }
}

@Composable
fun AgentScreen() {
    val secondary = Color(0xFF686157)
    Column(Modifier.fillMaxSize().background(pageGray)) {
        BoxWithConstraints(Modifier.fillMaxWidth().background(yellow)) {
            val compact = maxWidth < 360.dp || androidx.compose.ui.platform.LocalConfiguration.current.fontScale > 1.2f
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).background(Color(0xFFFFF6CE), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { CatIcon("agent_mark", true, Modifier.size(24.dp)) }
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                        Text("账伴", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = ink)
                        Text("陪你看懂每一笔收支", fontSize = 12.sp, color = Color(0xFF66531C), lineHeight = 17.sp)
                    }
                    if (!compact) Text("AI 助手 · 概念预览", Modifier.background(Color(0xFFFFF6CE), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = Color(0xFF66531C), lineHeight = 16.sp)
                }
                if (compact) Text("AI 助手 · 概念预览", Modifier.padding(top = 8.dp).background(Color(0xFFFFF6CE), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = Color(0xFF66531C), lineHeight = 16.sp)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text("今天", Modifier.align(Alignment.CenterHorizontally), color = secondary, fontSize = 12.sp)
            Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(34.dp).background(Color(0xFFFFEAA4), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { CatIcon("agent_avatar", true, Modifier.size(20.dp)) }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("账伴", color = secondary, fontSize = 12.sp)
                    Column(Modifier.padding(top = 6.dp).background(Color.White, RoundedCornerShape(4.dp, 14.dp, 14.dp, 14.dp)).padding(14.dp)) {
                        Text("你好，我是账伴。", fontSize = 14.sp, lineHeight = 22.sp, color = ink, fontWeight = FontWeight.SemiBold)
                        Text("这里会成为你的账单分析空间。未来，你可以像聊天一样提问，一起梳理收支、观察消费变化，让每一笔账都更容易看懂。", fontSize = 14.sp, lineHeight = 22.sp, color = Color(0xFF45413A))
                    }
                }
            }
            Text("以后可以这样和我聊", Modifier.padding(top = 24.dp, bottom = 10.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ink)
            val suggestions = listOf("wallet" to "这个月的钱主要花在哪里？", "chart" to "最近消费有什么变化？", "nav_chart" to "帮我看看每周支出趋势", "book" to "哪些标签的累计消费较高？")
            suggestions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (icon, label) ->
                        Row(Modifier.weight(1f).fillMaxHeight().heightIn(min = 72.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(30.dp).background(Color(0xFFFFF6CE), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { CatIcon(icon, true, Modifier.size(18.dp), Color(0xFF80651E)) }
                            Text(label, Modifier.padding(start = 8.dp), fontSize = 12.sp, lineHeight = 18.sp, color = Color(0xFF45413A))
                        }
                    }
                }
            }
            Text("以上为未来对话场景示意，当前仅展示界面，不会读取或分析账单。", Modifier.padding(top = 8.dp, bottom = 4.dp), color = secondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
        Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}, enabled = false, modifier = Modifier.semantics { contentDescription = "附件功能预留" }) { Text("＋", fontSize = 22.sp) }
                OutlinedTextField(value = "", onValueChange = {}, enabled = false, placeholder = { Text("和账伴聊聊你的账单…", fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(24.dp), colors = OutlinedTextFieldDefaults.colors(disabledContainerColor = Color(0xFFF5F4F1), disabledBorderColor = Color(0xFFE5E2DB), disabledPlaceholderColor = Color(0xFF77736C)))
                IconButton(onClick = {}, enabled = false, modifier = Modifier.semantics { contentDescription = "发送功能预留" }) { Text("↑", fontSize = 22.sp) }
            }
            Text("智能对话功能将在后续版本接入", Modifier.padding(top = 6.dp), color = secondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}
