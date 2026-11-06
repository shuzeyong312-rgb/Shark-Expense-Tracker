package com.example.personalledger

import android.widget.NumberPicker
import android.view.ContextThemeWrapper
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.personalledger.data.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(model: LedgerViewModel, categories: List<Category>, initial: LedgerTransaction?, back: () -> Unit, done: () -> Unit, manage: (String) -> Unit) {
    var type by rememberSaveable(initial?.id) { mutableStateOf(initial?.type ?: "EXPENSE") }
    var categoryId by rememberSaveable(initial?.id) { mutableStateOf(initial?.categoryId) }
    var showPanel by rememberSaveable(initial?.id) { mutableStateOf(initial != null) }
    var amount by rememberSaveable(initial?.id) { mutableStateOf(initial?.let { formatAmountMinor(it.amountMinor) } ?: "") }
    var note by rememberSaveable(initial?.id) { mutableStateOf(initial?.noteSnapshot.orEmpty()) }
    var dateText by rememberSaveable(initial?.id) { mutableStateOf(initial?.occurredOn ?: LocalDate.now().toString()) }
    val date = LocalDate.parse(dateText)
    var confirmDelete by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val category = categories.find { it.id == categoryId }
    val quick by remember(categoryId) { categoryId?.let { model.repository.quickTags(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList()) }.collectAsState(initial = emptyList())
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(86.dp).background(yellow), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                listOf("EXPENSE" to "支出", "INCOME" to "收入").forEach { (value, label) -> Column(Modifier.clickable { type = value; showPanel = false }.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = ink, fontSize = 20.sp, fontWeight = if (type == value) FontWeight.Bold else FontWeight.Normal); Spacer(Modifier.padding(top = 8.dp).width(60.dp).height(3.dp).background(if (type == value) ink else Color.Transparent)) } }
            }
            Text("取消", Modifier.align(Alignment.CenterEnd).clickable(onClick = back).padding(18.dp), color = ink)
        }
        val visible = categories.filter { it.type == type && it.isVisible }
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) {
            items(visible.size) { index ->
                val c = visible[index]
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { categoryId = c.id; showPanel = true }.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(52.dp).background(Color(0xFFF3F3F3), CircleShape), contentAlignment = Alignment.Center) { CatIcon(c.iconKey, true, Modifier.size(27.dp)) }
                    Text(c.name, Modifier.padding(top = 7.dp), fontSize = 13.sp)
                }
            }
            item { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { manage(type) }.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(52.dp).background(Color(0xFFF3F3F3), CircleShape), contentAlignment = Alignment.Center) { CatIcon("settings", true, Modifier.size(27.dp)) }; Text("设置", Modifier.padding(top = 7.dp), fontSize = 13.sp) } }
        }
    }
    if (showPanel && category != null) ModalBottomSheet(onDismissRequest = { showPanel = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White, dragHandle = null) {
        var noteFocused by remember { mutableStateOf(false) }
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        var keyboardWasVisible by remember { mutableStateOf(false) }
        LaunchedEffect(keyboardVisible) {
            if (keyboardWasVisible && !keyboardVisible) focusManager.clearFocus()
            keyboardWasVisible = keyboardVisible
        }
        Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clickable { showPanel = false }.weight(1f), verticalAlignment = Alignment.CenterVertically) { CatIcon(category.iconKey, true, Modifier.size(24.dp)); Spacer(Modifier.width(8.dp)); Text(category.name, fontSize = 13.sp) }
                Text(if (initial == null) "记一笔" else "编辑账单", fontSize = 19.sp)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    if (initial != null) Text("删除", Modifier.clickable { confirmDelete = true }.padding(6.dp), color = Color.Red, fontSize = 13.sp)
                    Text("取消", Modifier.clickable { if (initial == null) showPanel = false else back() }.padding(6.dp), color = Color.Gray, fontSize = 13.sp)
                }
            }
            Text("¥${amount.ifEmpty { "0.00" }}", Modifier.align(Alignment.End).padding(vertical = 10.dp), fontSize = 36.sp, fontWeight = FontWeight.Light)
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(note, { note = it }, modifier = Modifier.weight(1f).background(pageGray, RoundedCornerShape(8.dp)).padding(12.dp).testTag("noteInput").onFocusChanged { noteFocused = it.isFocused }, textStyle = MaterialTheme.typography.bodyLarge, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); focusManager.clearFocus() }), decorationBox = { inner -> Box { if (note.isEmpty()) Text("备注：输入后自动保存为标签", color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis); inner() } })
                Spacer(Modifier.width(8.dp))
                Row(Modifier.background(pageGray, RoundedCornerShape(8.dp)).clickable { focusManager.clearFocus(); keyboard?.hide(); showDatePicker = true }.padding(12.dp).testTag("dateSelector"), verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(15.dp)) {
                        val line = 1.4.dp.toPx()
                        drawRoundRect(ink, cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = Stroke(line))
                        drawLine(ink, Offset(0f, size.height * .3f), Offset(size.width, size.height * .3f), line)
                        drawLine(ink, Offset(size.width * .3f, 0f), Offset(size.width * .3f, size.height * .18f), line)
                        drawLine(ink, Offset(size.width * .7f, 0f), Offset(size.width * .7f, size.height * .18f), line)
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(if (date == LocalDate.now()) "今天" else date.toString(), color = ink, fontSize = 13.sp)
                }
            }
            if (noteFocused && quick.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp)) { quick.forEach { tag -> Text(tag.displayName, Modifier.padding(end = 8.dp).background(pageGray, RoundedCornerShape(6.dp)).clickable { note = tag.displayName }.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 12.sp) } }
            @Composable fun Key(key: String, modifier: Modifier) {
                Box(modifier.height(53.dp).border(0.5.dp, pageGray).background(if (key == "完成" || key == "=") yellow else Color.White).clickable(enabled = !saving) {
                    when (key) {
                        "⌫" -> amount = amount.dropLast(1)
                        "=" -> {
                            val cents = calculateAmountMinor(amount)
                            if (cents == null) model.run { error("请输入完整且有效的算式") }
                            else amount = formatAmountMinor(cents)
                        }
                        "完成" -> {
                            val cents = calculateAmountMinor(amount)
                            if (cents == null || cents <= 0) model.run { error("请输入大于零且最多两位小数的金额") }
                            else { saving = true; model.run { try {
                                model.repository.save(initial?.id, category.id, cents, date, note)
                                try { model.updateMonth(java.time.YearMonth.from(date)) }
                                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                                catch (_: Exception) { model.showMessage("账单已保存，但未能切换月份，请手动选择。") }
                                done()
                            } finally { saving = false } } }
                        }
                        "+", "−" -> if (amount.isNotEmpty() && amount.last().isDigit()) amount += if (key == "−") "-" else "+"
                        "." -> { val term = amount.substringAfterLast('+').substringAfterLast('-'); if (!term.contains('.')) amount += if (term.isEmpty()) "0." else "." }
                        else -> { val term = amount.substringAfterLast('+').substringAfterLast('-'); if (!term.contains('.') || term.substringAfter('.').length < 2) amount += key }
                    }
                }, contentAlignment = Alignment.Center) { Text(key, fontSize = if (key.length > 1) 16.sp else 22.sp) }
            }
            if (!keyboardVisible) Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(3f)) {
                    listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row -> Row { row.forEach { Key(it, Modifier.weight(1f)) } } }
                    Row { Key("0", Modifier.weight(2f)); Key(".", Modifier.weight(1f)) }
                }
                Column(Modifier.weight(1f)) { Key("⌫", Modifier.fillMaxWidth()); Key("+", Modifier.fillMaxWidth()); Key("−", Modifier.fillMaxWidth()); Key(if (amount.drop(1).any { it == '+' || it == '-' }) "=" else "完成", Modifier.fillMaxWidth()) }
            }
            if (!keyboardVisible) Spacer(Modifier.height(14.dp))
        }
    }
    if (showDatePicker) DateWheelSheet(date, onDismiss = { showDatePicker = false }, onConfirm = { dateText = it.toString(); showDatePicker = false })
    if (confirmDelete && initial != null) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("删除账单？") }, text = { Text("删除后无法恢复。") }, confirmButton = { TextButton(onClick = { confirmDelete = false; model.run { model.repository.delete(initial.id); done() } }) { Text("删除") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateWheelSheet(date: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var selected by remember { mutableStateOf(date) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White, dragHandle = null) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                Text("取消", Modifier.align(Alignment.CenterStart).clickable(onClick = onDismiss).padding(16.dp))
                Text("选择日期", fontSize = 18.sp)
                Text("确定", Modifier.align(Alignment.CenterEnd).clickable { onConfirm(selected) }.padding(16.dp))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                @Composable fun Wheel(label: String, min: Int, max: Int, value: Int, change: (Int) -> Unit) {
                    Box(Modifier.weight(1f).height(180.dp)) {
                        AndroidView(
                            factory = { context -> NumberPicker(ContextThemeWrapper(context, R.style.WheelTheme)).apply { minValue = min; maxValue = max; wrapSelectorWheel = false; descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS; contentDescription = label; setOnValueChangedListener { _, _, new -> change(new) } } },
                            update = { picker -> picker.minValue = min; picker.maxValue = max; picker.value = value.coerceIn(min, max) },
                            modifier = Modifier.fillMaxSize()
                        )
                        Canvas(Modifier.matchParentSize()) {
                            val inset = 12.dp.toPx()
                            val offset = 24.dp.toPx()
                            listOf(center.y - offset, center.y + offset).forEach { y -> drawLine(Color(0xFFE4E4E4), Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx()) }
                        }
                    }
                }
                Wheel("年", 1, maxOf(9999, selected.year), selected.year) { selected = selected.withYear(it) }
                Wheel("月", 1, 12, selected.monthValue) { selected = selected.withMonth(it) }
                Wheel("日", 1, selected.lengthOfMonth(), selected.dayOfMonth) { selected = selected.withDayOfMonth(it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManager(model: LedgerViewModel, categories: List<Category>, initialType: String, back: () -> Unit) {
    var type by remember { mutableStateOf(initialType) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var editor by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("gamepad") }
    val items = categories.filter { it.type == type }.sortedWith(compareBy({ it.sortOrder }, { it.id }))
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxWidth().background(yellow)) {
            Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.Center) { Text("‹", Modifier.align(Alignment.CenterStart).clickable(onClick = back).padding(horizontal = 18.dp), fontSize = 28.sp); Text("类别设置", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp).height(42.dp).border(1.dp, ink, RoundedCornerShape(7.dp))) { listOf("EXPENSE" to "支出", "INCOME" to "收入").forEach { (value, label) -> Box(Modifier.weight(1f).fillMaxHeight().background(if (type == value) ink else yellow).clickable { type = value }, contentAlignment = Alignment.Center) { Text(label, color = if (type == value) yellow else ink) } } }
            Spacer(Modifier.height(18.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            items.filter { it.isVisible }.forEachIndexed { index, c ->
                Row(Modifier.fillMaxWidth().height(64.dp).border(0.3.dp, Color(0xFFEEEEEE)).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).background(Color(0xFFF04444), CircleShape).clickable { model.run { model.repository.setCategoryVisible(c.id, false) } }, contentAlignment = Alignment.Center) { Text("−", color = Color.White, fontSize = 20.sp) }
                    Spacer(Modifier.width(14.dp)); Box(Modifier.size(46.dp).background(pageGray, CircleShape), contentAlignment = Alignment.Center) { CatIcon(c.iconKey, true, Modifier.size(25.dp)) }; Text(c.name, Modifier.weight(1f).padding(start = 14.dp))
                    if (!c.isSystem) TextButton(onClick = { editing = c; name = c.name; icon = c.iconKey; editor = true }) { Text("编辑") }
                    val rowPx = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }
                    var drag by remember(c.id) { mutableFloatStateOf(0f) }
                    fun move(destination: Int) {
                        val ids = items.filter { it.isVisible }.map { it.id }.toMutableList()
                        val target = destination.coerceIn(0, ids.lastIndex)
                        if (target != index) { ids.removeAt(index); ids.add(target, c.id); model.run { model.repository.reorderCategories(type, ids) } }
                    }
                    Text("☰", Modifier.padding(12.dp).semantics { contentDescription = "调整${c.name}顺序"; customActions = listOf(CustomAccessibilityAction("上移") { move(index - 1); true }, CustomAccessibilityAction("下移") { move(index + 1); true }) }.pointerInput(c.id, items) {
                        detectDragGestures(onDragStart = { drag = 0f }, onDragEnd = {
                            move(index + kotlin.math.round(drag / rowPx).toInt())
                        }) { change, amount -> change.consume(); drag += amount.y }
                    }, color = Color.Gray)
                }
            }
            Text("更多类别", Modifier.fillMaxWidth().background(pageGray).padding(16.dp), color = Color.Gray)
            items.filterNot { it.isVisible }.forEach { c -> Row(Modifier.fillMaxWidth().height(64.dp).clickable { model.run { model.repository.setCategoryVisible(c.id, true) } }.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(28.dp).background(Color(0xFF32A76D), CircleShape), contentAlignment = Alignment.Center) { Text("＋", color = Color.White, fontSize = 20.sp) }; Spacer(Modifier.width(14.dp)); Box(Modifier.size(46.dp).background(pageGray, CircleShape), contentAlignment = Alignment.Center) { CatIcon(c.iconKey, true, Modifier.size(25.dp)) }; Text(c.name, Modifier.padding(start = 14.dp)) } }
        }
        Box(Modifier.fillMaxWidth().height(64.dp).background(Color.White).clickable { editing = null; name = ""; icon = "gamepad"; editor = true }, contentAlignment = Alignment.Center) { Text("＋添加类别") }
    }
    if (editor) ModalBottomSheet(onDismissRequest = { editor = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), sheetGesturesEnabled = false, containerColor = Color.White, dragHandle = null) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).imePadding().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { editor = false }) { Text("取消", color = ink) }; Text("${if (editing == null) "添加" else "编辑"}${if (type == "EXPENSE") "支出" else "收入"}类别", fontWeight = FontWeight.Bold)
                TextButton(enabled = name.isNotBlank(), onClick = { model.run { model.repository.saveCategory(editing?.id, type, name, icon); editor = false } }) { Text("完成", color = if (name.isNotBlank()) ink else Color.LightGray) }
            }
            Box(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 22.dp).size(76.dp).background(yellow, CircleShape), contentAlignment = Alignment.Center) { CatIcon(icon, true, Modifier.size(34.dp)) }
            BasicTextField(name, { if (it.codePointCount(0, it.length) <= 4) name = it }, modifier = Modifier.fillMaxWidth().background(pageGray, RoundedCornerShape(7.dp)).padding(16.dp), textStyle = MaterialTheme.typography.bodyLarge, singleLine = true, decorationBox = { inner -> Box { if (name.isEmpty()) Text("类别名称（最多4字）", color = Color.Gray); inner() } })
            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("categoryIcons")) { iconGroups.forEach { (title, icons) -> Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) { Text(title, color = ink) }; icons.chunked(5).forEach { row -> Row(Modifier.fillMaxWidth()) { row.forEach { (key, label) -> Box(Modifier.weight(1f).aspectRatio(1f).semantics { contentDescription = label }.clickable { icon = key }, contentAlignment = Alignment.Center) { Box(Modifier.size(54.dp).background(if (key == icon) yellow else pageGray, CircleShape), contentAlignment = Alignment.Center) { CatIcon(key, true, Modifier.size(27.dp)) } } }; repeat(5-row.size) { Spacer(Modifier.weight(1f)) } } } } }
            }
        }
    }
}
