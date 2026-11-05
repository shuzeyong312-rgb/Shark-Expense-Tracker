package com.example.personalledger

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.personalledger.data.*
import java.math.BigInteger
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

private val donutColors = listOf(Color(0xFFF5CF3B), Color(0xFFEF8A76), Color(0xFF8B9CE8), Color(0xFF57B99A), Color(0xFFF0A54D), Color(0xFF61B7DC), Color(0xFFD47BB0), Color(0xFFA7B47A), Color(0xFF8D91A7), Color(0xFFD6A4E4), Color(0xFF77B5A7), Color(0xFFD2A26B))

@Composable
fun ChartScreen(model: LedgerViewModel, month: YearMonth, all: List<LedgerTransaction>, categories: List<Category>, tags: List<NoteTag>, category: Category?, openCategory: (Category) -> Unit, openTag: (NoteTag, Period) -> Unit, edit: (LedgerTransaction) -> Unit, back: () -> Unit) {
    val savedType by model.chartType.collectAsState()
    val kind by model.period.collectAsState()
    val savedAnchor by model.chartAnchor.collectAsState()
    var menu by remember { mutableStateOf(false) }
    var sortRecent by remember { mutableStateOf(false) }
    val type = category?.type ?: savedType
    val defaultAnchor = when (kind) { PeriodKind.WEEK -> LocalDate.now().minusWeeks(1); PeriodKind.MONTH, PeriodKind.YEAR -> month.atDay(1) }
    val anchor = remember(savedAnchor, defaultAnchor) { runCatching { LocalDate.parse(savedAnchor) }.getOrNull() ?: defaultAnchor }
    val period = periodFor(anchor, kind)
    val count = when (kind) { PeriodKind.WEEK -> 6; PeriodKind.MONTH -> 7; PeriodKind.YEAR -> 6 }
    val recent = (count - 1 downTo 0).map { offset -> when (kind) { PeriodKind.WEEK -> defaultAnchor.minusWeeks(offset.toLong()); PeriodKind.MONTH -> defaultAnchor.minusMonths(offset.toLong()); PeriodKind.YEAR -> defaultAnchor.minusYears(offset.toLong()) } }
    val choices = (recent + anchor).distinctBy { periodFor(it, kind).start }.sorted()
    val choicesScroll = rememberScrollState()
    val choiceWidth = with(LocalDensity.current) { 58.dp.toPx() }
    LaunchedEffect(kind, period.start, choices) {
        choicesScroll.scrollTo((choices.indexOfFirst { periodFor(it, kind).start == period.start } * choiceWidth).roundToInt())
    }
    val filtered = all.filter { it.type == type && (category == null || it.categoryId == category.id) && it.occurredOn >= period.start.toString() && it.occurredOn <= period.end.toString() }
    val series = remember(filtered, period) { periodSeries(filtered, period, type) }
    val total = series.sumBig()
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxWidth().background(yellow).height(76.dp)) {
            Box(Modifier.fillMaxWidth().height(47.dp), contentAlignment = Alignment.Center) {
                if (category != null) Text("‹", Modifier.align(Alignment.CenterStart).clickable(onClick = back).padding(horizontal = 20.dp), fontSize = 28.sp)
                if (category != null) Text(category.name, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                else Box { Text(if (type == "EXPENSE") "支出 ▾" else "收入 ▾", Modifier.clickable { menu = true }, fontSize = 19.sp, fontWeight = FontWeight.Bold); DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) { listOf("EXPENSE" to "支出", "INCOME" to "收入").forEach { (value, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { model.setChart(value, kind, savedAnchor); menu = false }) } } }
            }
            Row(Modifier.fillMaxWidth().height(29.dp).border(1.dp, ink), horizontalArrangement = Arrangement.Center) { PeriodKind.entries.forEach { value -> Box(Modifier.weight(1f).fillMaxHeight().background(if (kind == value) ink else yellow).clickable { model.setChart(savedType, value, null) }, contentAlignment = Alignment.Center) { Text(when (value) { PeriodKind.WEEK -> "周"; PeriodKind.MONTH -> "月"; PeriodKind.YEAR -> "年" }, color = if (kind == value) yellow else ink, fontWeight = if (kind == value) FontWeight.Bold else FontWeight.Normal) } } }
        }
        Row(Modifier.fillMaxWidth().height(39.dp).horizontalScroll(choicesScroll), verticalAlignment = Alignment.CenterVertically) {
            choices.forEach { choice ->
                val p = periodFor(choice, kind)
                val label = when (kind) { PeriodKind.WEEK -> if (periodFor(choice, kind).start == periodFor(defaultAnchor, kind).start) "上周" else "${p.start.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())}周"; PeriodKind.MONTH -> "${choice.monthValue}月"; PeriodKind.YEAR -> "${choice.year}年" }
                Column(Modifier.width(58.dp).clickable { model.setChart(savedType, kind, choice.toString()) }, horizontalAlignment = Alignment.CenterHorizontally) { Text(label, fontWeight = if (p.start == period.start) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp); Spacer(Modifier.padding(top = 5.dp).fillMaxWidth(.75f).height(2.dp).background(if (p.start == period.start) ink else Color.Transparent)) }
            }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 18.dp).testTag("chartDetails")) {
          item { Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("总${if (type == "EXPENSE") "支出" else "收入"}：${formatAmountMinor(total)}", fontSize = 14.sp, color = Color.Gray); Text("平均值：${formatAverageMinor(total, series.size)}", Modifier.padding(top = 4.dp), fontSize = 14.sp, color = Color.Gray) }
                Column(horizontalAlignment = Alignment.End) { Text(formatAmountMinor(series.maxOrNull() ?: BigInteger.ZERO), fontSize = 12.sp, color = Color.Gray); if (category != null) Text("${filtered.size} 笔", fontSize = 12.sp) }
            }
            TrendChart(series, period)
            if (category == null) {
                Text("${if (type == "EXPENSE") "支出" else "收入"}排行榜", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                val ranking = filtered.groupBy { it.categoryId }.mapValues { (_, v) -> v.sumMinor() }.entries.sortedByDescending { it.value }
                if (ranking.isEmpty()) Text("暂无数据", Modifier.padding(24.dp), color = Color.Gray)
                val maximum = ranking.maxOfOrNull { it.value } ?: BigInteger.ZERO
                ranking.forEach { (id, value) -> categoryMap[id]?.let { c -> RankingRow(c.name, c.iconKey, value, total, maximum) { openCategory(c) } } }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (type == "EXPENSE") "支出排行榜" else "收入排行榜", fontWeight = FontWeight.Bold); Row { Text("按金额", Modifier.clickable { sortRecent = false }.padding(horizontal = 8.dp), color = if (!sortRecent) ink else Color.Gray); Text("按时间", Modifier.clickable { sortRecent = true }, color = if (sortRecent) ink else Color.Gray) } }
                Text("一级分类：${category.name} · 二级标签：记账备注", Modifier.padding(top = 5.dp), fontSize = 11.sp, color = Color.Gray)
                val tagged = filtered.filter { it.noteTagId != null }.groupBy { it.noteTagId!! }
                val entries = tagged.entries.sortedWith(if (sortRecent) compareByDescending<Map.Entry<Long, List<LedgerTransaction>>> { entry -> entry.value.maxOf { it.occurredOn } }.thenByDescending { entry -> entry.value.sumMinor() } else compareByDescending { entry -> entry.value.sumMinor() })
                val tagsById = tags.associateBy { it.id }
                val taggedTotal = entries.map { it.value.sumMinor() }.sumBig()
                val maximum = entries.maxOfOrNull { it.value.sumMinor() } ?: BigInteger.ZERO
                if (entries.isEmpty()) Text("暂无备注", Modifier.padding(24.dp), color = Color.Gray)
                entries.forEach { (id, records) -> tagsById[id]?.let { t -> Column { RankingRow(t.displayName, category.iconKey, records.sumMinor(), taggedTotal, maximum) { openTag(t, period) }; Text("${records.size} 笔 · 最近 ${records.maxOf { it.occurredOn }}", Modifier.padding(start = 50.dp), fontSize = 11.sp, color = Color.Gray) } } }
                Text("备注占比", Modifier.padding(top = 24.dp), fontWeight = FontWeight.Bold)
                if (entries.isEmpty()) Text("暂无备注数据", Modifier.padding(vertical = 18.dp), color = Color.Gray)
                else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val byAmount = entries.sortedByDescending { it.value.sumMinor() }
                    Box(contentAlignment = Alignment.Center) { Donut(byAmount.map { it.value.sumMinor() }); Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("备注总额", fontSize = 11.sp, color = Color.Gray); Text(formatAmountMinor(taggedTotal), fontSize = 14.sp) } }
                    Column(Modifier.weight(1f)) { byAmount.take(6).forEachIndexed { index, (id, records) -> tagsById[id]?.let { t -> Row(Modifier.fillMaxWidth().clickable { openTag(t, period) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(donutColors[index % donutColors.size])); Text(" ${t.displayName}  ${formatPercent(records.sumMinor(), taggedTotal)}%  ${formatAmountMinor(records.sumMinor())}", fontSize = 12.sp) } } } }
                }
                Text("全部账单", Modifier.padding(top = 24.dp), fontWeight = FontWeight.Bold)
                if (filtered.isEmpty()) Text("本周期暂无该分类账单", Modifier.padding(24.dp), color = Color.Gray)
            }
          } }
          if (category != null) items(filtered, key = { it.id }) { row -> BillRow(row, category, { edit(row) }, { model.run { model.repository.delete(row.id) } }) }
        }
    }
}

@Composable
fun TrendChart(series: List<BigInteger>, period: Period) {
    var selected by remember(series) { mutableIntStateOf(-1) }
    if (selected >= 0) Text("${period.slots()[selected]}  ${formatAmountMinor(series[selected])}", Modifier.fillMaxWidth(), fontSize = 11.sp, color = Color.Gray)
    Canvas(Modifier.fillMaxWidth().height(124.dp).padding(vertical = 10.dp).testTag("trendChart").pointerInput(series) {
        detectTapGestures { offset -> if (series.isNotEmpty()) selected = ((offset.x / size.width) * (series.size - 1)).roundToInt().coerceIn(0, series.lastIndex) }
    }) {
        if (series.isNotEmpty()) {
            val max = (series.maxOrNull() ?: BigInteger.ZERO).max(BigInteger.ONE)
            val points = series.mapIndexed { i, value -> Offset(if (series.size == 1) 0f else size.width * i / (series.size - 1), size.height - size.height * BigDecimal(value).divide(BigDecimal(max), 6, RoundingMode.HALF_UP).toFloat()) }
            val path = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) } }
            repeat(4) { i -> drawLine(Color(0xFFEEEEEE), Offset(0f, size.height * i / 3), Offset(size.width, size.height * i / 3), strokeWidth = 1.dp.toPx()) }
            drawPath(path, Color(0xFF999999), style = Stroke(width = 1.dp.toPx()))
            points.forEachIndexed { i, point -> drawCircle(if (series[i].signum() == 0) Color.White else yellow, 3.dp.toPx(), point); drawCircle(if (i == selected) ink else Color.Gray, 3.dp.toPx(), point, style = Stroke(1.dp.toPx())) }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val slots = period.slots()
        val indices = if (slots.size <= 7) slots.indices.toList() else (0..6).map { it * (slots.size - 1) / 6 }.distinct()
        indices.forEach { index -> Text(if (period.kind == PeriodKind.YEAR) "${slots[index].monthValue}月" else "${slots[index].monthValue}/${slots[index].dayOfMonth}", fontSize = 10.sp, color = Color.Gray) }
    }
}

@Composable
fun Donut(values: List<BigInteger>) {
    Canvas(Modifier.size(150.dp).padding(12.dp)) {
        val total = values.sumBig()
        var start = -90f
        if (total.signum() == 0) drawCircle(pageGray, style = Stroke(22.dp.toPx()))
        else values.forEachIndexed { i, value -> val sweep = 360f * BigDecimal(value).divide(BigDecimal(total), 6, RoundingMode.HALF_UP).toFloat(); drawArc(donutColors[i % donutColors.size], start, sweep, false, style = Stroke(22.dp.toPx())); start += sweep }
    }
}

@Composable
fun RankingRow(name: String, icon: String, value: BigInteger, total: BigInteger, maximum: BigInteger, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).background(Color(0xFFFFF5C8), CircleShape), contentAlignment = Alignment.Center) { CatIcon(icon, false, Modifier.size(23.dp)) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(name); LinearProgressIndicator(progress = { if (maximum.signum() == 0) 0f else BigDecimal(value).divide(BigDecimal(maximum), 6, RoundingMode.HALF_UP).toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp), color = yellow, trackColor = pageGray) }
        Text("${formatPercent(value, total)}%   ${formatAmountMinor(value)}", Modifier.padding(start = 12.dp), fontSize = 12.sp)
    }
}

@Composable
fun TagScreen(model: LedgerViewModel, tag: NoteTag, initial: Period?, categories: List<Category>, all: List<LedgerTransaction>, edit: (LedgerTransaction) -> Unit, back: () -> Unit) {
    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: PeriodKind.MONTH) }
    val anchor = remember { initial?.start ?: LocalDate.now() }
    val period = periodFor(anchor, kind)
    val records = all.filter { it.categoryId == tag.categoryId && it.noteTagId == tag.id && it.occurredOn >= period.start.toString() && it.occurredOn <= period.end.toString() }
    val total = records.sumMinor()
    LazyColumn(Modifier.fillMaxSize().background(pageGray).testTag("tagDetails")) {
        item { Column {
        Column(Modifier.fillMaxWidth().background(yellow)) {
            Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) { Text("‹", Modifier.align(Alignment.CenterStart).clickable(onClick = back).padding(horizontal = 18.dp), fontSize = 28.sp); Text("备注明细", fontSize = 19.sp) }
            Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(Color(0xFFFFF8D8), CircleShape), contentAlignment = Alignment.Center) { Text(tag.displayName.take(1)) }; Text(tag.displayName, Modifier.padding(start = 10.dp), fontSize = 18.sp) }
                Text("¥${formatAmountMinor(total)}", Modifier.padding(top = 10.dp), fontSize = 30.sp)
                Text("${categories.find { it.id == tag.categoryId }?.name.orEmpty()} · ${period.start} 至 ${period.end}", fontSize = 11.sp, color = Color(0xFF7B6A2D))
            }
        }
        Row(Modifier.fillMaxWidth().padding(14.dp).background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).padding(16.dp)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(records.size.toString(), fontSize = 19.sp); Text("使用次数", fontSize = 12.sp, color = Color.Gray) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(formatAverageMinor(total, records.size), fontSize = 19.sp); Text("平均单笔", fontSize = 12.sp, color = Color.Gray) }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { PeriodKind.entries.forEach { k -> Box(Modifier.weight(1f).background(if (kind == k) ink else pageGray, androidx.compose.foundation.shape.RoundedCornerShape(7.dp)).clickable { kind = k }.padding(9.dp), contentAlignment = Alignment.Center) { Text(when(k) { PeriodKind.WEEK -> "周"; PeriodKind.MONTH -> "月"; PeriodKind.YEAR -> "年" }, color = if (kind == k) yellow else Color.Gray) } } }
        Text("相关账单", Modifier.padding(18.dp), fontWeight = FontWeight.Bold)
        if (records.isEmpty()) Text("当前周期没有使用该标签的账单", Modifier.padding(24.dp), color = Color.Gray)
        } }
        items(records, key = { it.id }) { row -> BillRow(row, categories.find { it.id == row.categoryId }, { edit(row) }, { model.run { model.repository.delete(row.id) } }) }
    }
}
