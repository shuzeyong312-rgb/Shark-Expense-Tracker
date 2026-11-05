package com.example.personalledger

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.personalledger.data.*
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

val yellow = Color(0xFFFFDA43)
val ink = Color(0xFF292929)
val pageGray = Color(0xFFF5F5F5)
fun navigationColor(selected: Boolean) = if (selected) ink else Color(0xFF888888)

@Composable
fun AvatarFace(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        drawCircle(ink, r)
        drawCircle(Color.White, r * .1f, Offset(center.x - r * .32f, center.y - r * .2f))
        drawCircle(Color.White, r * .1f, Offset(center.x + r * .32f, center.y - r * .2f))
        val smile = Path().apply { moveTo(center.x - r * .45f, center.y + r * .18f); quadraticTo(center.x, center.y + r * .7f, center.x + r * .45f, center.y + r * .18f) }
        drawPath(smile, Color.White, style = Stroke(r * .1f))
    }
}

@Composable
fun CatIcon(key: String, outline: Boolean, modifier: Modifier = Modifier, tint: Color? = null) {
    val id = iconResource(key, outline)
    if (id != 0) Image(painterResource(id), null, modifier, colorFilter = tint?.let { ColorFilter.tint(it) })
}

@Composable
fun LedgerApp(model: LedgerViewModel) {
    val categories by model.categories.collectAsState()
    val transactions by model.transactions.collectAsState()
    val tags by model.tags.collectAsState()
    val month by model.month.collectAsState()
    val message by model.message.collectAsState()
    val stack = rememberSaveable(saver = listSaver<SnapshotStateList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() })) { mutableStateListOf("home") }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var tagId by rememberSaveable { mutableStateOf<Long?>(null) }
    var periodStart by rememberSaveable { mutableStateOf<String?>(null) }
    var periodKind by rememberSaveable { mutableStateOf<String?>(null) }
    var managerType by rememberSaveable { mutableStateOf("EXPENSE") }
    val edit = transactions.find { it.id == editId }
    val selectedCategory = categories.find { it.id == categoryId }
    val selectedTag = tags.find { it.id == tagId }
    val selectedPeriod = periodStart?.let { start -> periodKind?.let { periodFor(LocalDate.parse(start), PeriodKind.valueOf(it)) } }
    val savedStates = rememberSaveableStateHolder()
    fun go(screen: String) { stack.add(screen) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun home() { stack.clear(); stack.add("home") }
    Column(Modifier.fillMaxSize().background(Color.White).navigationBarsPadding()) {
        Spacer(Modifier.fillMaxWidth().background(yellow).statusBarsPadding())
        val screen = stack.last()
        NavDisplay(backStack = stack, onBack = { back() }, modifier = Modifier.weight(1f), entryProvider = { key ->
            NavEntry(key) {
              savedStates.SaveableStateProvider(key) {
                when (key) {
                    "home" -> HomeScreen(model, month, categories, transactions, model::setMonth, { stack.clear(); stack.add("mine") }) { savedStates.removeState("record"); editId = it.id; go("record") }
                    "record" -> if (editId == null || edit != null) RecordScreen(model, categories, edit, { if (editId == null) home() else back() }, { home() }, { managerType = it; go("manager") })
                    "manager" -> CategoryManager(model, categories, managerType, { back() })
                    "chart" -> ChartScreen(model, month, transactions, categories, tags, null, { categoryId = it.id; go("category") }, { _, _ -> }, { savedStates.removeState("record"); editId = it.id; go("record") }, { back() })
                    "category" -> selectedCategory?.let { category -> ChartScreen(model, month, transactions, categories, tags, categories.find { it.id == category.id } ?: category, {}, { tag, period -> tagId = tag.id; periodStart = period.start.toString(); periodKind = period.kind.name; go("tag") }, { savedStates.removeState("record"); editId = it.id; go("record") }, { back() }) }
                    "tag" -> selectedTag?.let { TagScreen(model, it, selectedPeriod, categories, transactions, { savedStates.removeState("record"); editId = it.id; go("record") }, { back() }) }
                    "agent" -> AgentScreen()
                    "mine" -> MineScreen(model, transactions, { managerType = "EXPENSE"; go("manager") })
                }
              }
            }
        })
        if (screen in listOf("home", "chart", "agent", "mine")) {
            Row(Modifier.fillMaxWidth().height(64.dp).background(Color.White), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                listOf("明细" to "home", "图表" to "chart", "＋" to "record", "账伴" to "agent", "我的" to "mine").forEach { (label, target) ->
                    Box(Modifier.weight(1f).fillMaxHeight().clickable {
                        if (target == "record") { editId = null; savedStates.removeState("record"); go(target) } else { stack.clear(); stack.add(target) }
                    }, contentAlignment = Alignment.Center) {
                        if (target == "record") Box(Modifier.offset(y = (-10).dp).size(66.dp).background(Color.White, CircleShape).padding(5.dp).background(yellow, CircleShape), contentAlignment = Alignment.Center) { Text(label, color = ink, fontSize = 32.sp) }
                        else Column(horizontalAlignment = Alignment.CenterHorizontally) { val color = navigationColor(target == screen); CatIcon("nav_$target", false, Modifier.size(22.dp), color); Text(label, color = color, fontSize = 11.sp, fontWeight = if (target == screen) FontWeight.Bold else FontWeight.Normal) }
                    }
                }
            }
        }
    }
    if (message != null) AlertDialog(onDismissRequest = model::clearMessage, title = { Text("提示") }, text = { Text(message.orEmpty()) }, confirmButton = { TextButton(onClick = model::clearMessage) { Text("确定") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(model: LedgerViewModel, month: YearMonth, categories: List<Category>, all: List<LedgerTransaction>, setMonth: (YearMonth) -> Unit, mine: () -> Unit, edit: (LedgerTransaction) -> Unit) {
    var picker by remember { mutableStateOf(false) }
    var draft by remember(month) { mutableStateOf(month) }
    val listState = rememberLazyListState()
    val monthItems = remember(month, all) { all.filter { it.occurredOn.startsWith(month.toString()) } }
    val grouped = remember(monthItems) { monthItems.groupBy { it.occurredOn } }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    var pull by remember { mutableFloatStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val threshold = with(density) { 72.dp.toPx() }
    val nested = remember(listState, month, threshold) { object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (available.y < 0) {
                if (pull < 0) pull = (pull - available.y).coerceAtMost(0f)
                else if (!listState.canScrollForward) pull = (pull - available.y).coerceAtMost(threshold * 2)
            } else if (available.y > 0) {
                if (pull > 0) pull = (pull - available.y).coerceAtLeast(0f)
                else if (!listState.canScrollBackward && month < YearMonth.now()) pull = (pull - available.y).coerceAtLeast(-threshold * 2)
            }
            return Offset.Zero
        }
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (pull >= threshold) { setMonth(month.minusMonths(1)); listState.scrollToItem(0) }
            else if (pull <= -threshold && month < YearMonth.now()) { setMonth(month.plusMonths(1)); listState.scrollToItem(0) }
            pull = 0f
            return Velocity.Zero
        }
    } }
    LaunchedEffect(month) { listState.scrollToItem(0) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxWidth().background(yellow).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(35.dp).background(Color(0xFFFFF8D6), CircleShape), contentAlignment = Alignment.Center) { AvatarFace(Modifier.size(15.dp)) }
            Text("记账", fontFamily = FontFamily.Serif, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.size(35.dp).background(Color(0xFFFFF8D6), CircleShape).clickable(onClick = mine), contentAlignment = Alignment.Center) { Text("☰", fontSize = 18.sp) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable { draft = month; picker = true }) { Text("${month.year}年", fontSize = 12.sp); Text("${month.monthValue} 月 ▾", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            listOf("收入" to "INCOME", "支出" to "EXPENSE").forEach { (label, type) -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(label, fontSize = 12.sp, color = Color(0xFF7B6A2D), fontFamily = FontFamily.Serif); Text(formatAmountMinor(monthItems.filter { it.type == type }.sumMinor()), fontSize = 20.sp) } }
        }
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().nestedScroll(nested).testTag("billList")) {
            if (monthItems.isEmpty()) item { Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { Text("这个月还没有账单", color = Color.Gray) } }
            grouped.forEach { (date, items) ->
                item { Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern("M月d日 EEEE", java.util.Locale.CHINA)), fontSize = 12.sp, color = Color.Gray); Text("支出 ${formatAmountMinor(items.filter { it.type == "EXPENSE" }.sumMinor())}  收入 ${formatAmountMinor(items.filter { it.type == "INCOME" }.sumMinor())}", fontSize = 11.sp, color = Color.Gray) } }
                items(items, key = { it.id }) { transaction -> BillRow(transaction, categoryMap[transaction.categoryId], { edit(transaction) }, { model.run { model.repository.delete(transaction.id) } }) }
            }
        }
        if (pull < 0f) Box(Modifier.fillMaxWidth().height(72.dp).align(Alignment.TopCenter).offset { IntOffset(0, -(threshold + pull).coerceAtLeast(0f).toInt()) }, contentAlignment = Alignment.Center) {
            Text(if (pull <= -threshold) "松开查看下月数据" else "下拉查看下月数据", fontSize = 12.sp, color = Color.Gray)
        }
        if (pull > 0f) Box(Modifier.fillMaxWidth().height(72.dp).align(Alignment.BottomCenter).offset { IntOffset(0, (threshold - pull).coerceAtLeast(0f).toInt()) }, contentAlignment = Alignment.Center) {
            Text(if (pull >= threshold) "松开查看上月数据" else "上拉查看上月数据", fontSize = 12.sp, color = Color.Gray)
        }
        }
    }
    if (picker) ModalBottomSheet(onDismissRequest = { picker = false }, containerColor = Color.White) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { picker = false }) { Text("取消") }
            Text("选择月份", Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
            TextButton(onClick = { setMonth(draft); picker = false }) { Text("确定") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
            WheelPicker(9999, draft.year - 1, { "${it + 1} 年" }, Modifier.weight(1f)) { draft = draft.withYear(it + 1) }
            WheelPicker(12, draft.monthValue - 1, { "${it + 1} 月" }, Modifier.weight(1f)) { draft = draft.withMonth(it + 1) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun WheelPicker(count: Int, initial: Int, label: (Int) -> String, modifier: Modifier = Modifier, changed: (Int) -> Unit) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    val scope = rememberCoroutineScope()
    LaunchedEffect(state) { snapshotFlow { state.firstVisibleItemIndex }.distinctUntilChanged().collect { changed(it) } }
    LazyColumn(state = state, modifier = modifier.height(150.dp), contentPadding = PaddingValues(vertical = 50.dp), flingBehavior = rememberSnapFlingBehavior(state)) {
        items(count) { index -> Box(Modifier.fillMaxWidth().height(50.dp).clickable { scope.launch { state.animateScrollToItem(index) } }, contentAlignment = Alignment.Center) { Text(label(index), color = if (index == state.firstVisibleItemIndex) ink else Color.LightGray, fontSize = if (index == state.firstVisibleItemIndex) 20.sp else 16.sp) } }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillRow(item: LedgerTransaction, category: Category?, edit: () -> Unit, delete: () -> Unit) {
    var actions by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = edit, onLongClick = { actions = true }).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(pageGray, CircleShape), contentAlignment = Alignment.Center) { CatIcon(category?.iconKey ?: "wallet", false, Modifier.size(25.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(item.noteSnapshot ?: category?.name.orEmpty(), fontSize = 15.sp); if (item.noteSnapshot != null) Text(category?.name.orEmpty(), fontSize = 11.sp, color = Color.Gray) }
        Text((if (item.type == "INCOME") "+" else "−") + formatAmountMinor(item.amountMinor), color = if (item.type == "INCOME") Color(0xFF44A376) else ink, fontWeight = FontWeight.Medium)
    }
    if (actions) AlertDialog(onDismissRequest = { actions = false }, title = { Text("账单操作") }, confirmButton = { TextButton(onClick = { actions = false; edit() }) { Text("编辑") } }, dismissButton = { TextButton(onClick = { actions = false; confirm = true }) { Text("删除") } })
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("删除账单？") }, text = { Text("删除后无法恢复。") }, confirmButton = { TextButton(onClick = { confirm = false; delete() }) { Text("删除") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("取消") } })
}
