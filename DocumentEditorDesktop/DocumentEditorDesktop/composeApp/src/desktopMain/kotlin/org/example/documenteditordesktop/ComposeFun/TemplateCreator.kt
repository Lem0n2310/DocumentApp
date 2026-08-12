package org.example.documenteditordesktop.ComposeFun

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.apache.poi.xwpf.usermodel.*
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.applyTemplateChangesByIndex
import org.example.documenteditordesktop.ClassesViewModels.DocumentField
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.Manager
import org.example.documenteditordesktop.functions.replaceSpacesWithUnderscores
import org.example.documenteditordesktop.functions.transliterateRussian
import java.io.File
import kotlin.collections.set

sealed class TemplateElement {
    data class TextBlock(
        val id: Int,
        var text: String,
    ) : TemplateElement()

    data class Table(
        val id: Int,
        val rows: List<List<String>>
    ) : TemplateElement()
}

data class TemplateState(
    val elements: MutableList<TemplateElement>,
    var nameForUser: String = "",
)

/** Диапазон подсветки внутри текстового блока / ячейки */
private data class HighlightRange(
    val start: Int,
    val end: Int,
    val color: Color,
)

/** Палитра цветов полей: каждому полю — свой устойчивый цвет */
private val FIELD_COLORS = listOf(
    Color(0xFF81C784), // зелёный — удобно для ФИО
    Color(0xFFFFF176), // жёлтый — адрес и т.п.
    Color(0xFF64B5F6), // синий
    Color(0xFFFFB74D), // оранжевый
    Color(0xFFCE93D8), // сиреневый
    Color(0xFF4DB6AC), // бирюзовый
    Color(0xFFE57373), // красный
    Color(0xFFA1887F), // коричневый
)

private fun colorForFieldIndex(index: Int): Color =
    FIELD_COLORS[index.mod(FIELD_COLORS.size)]

private fun fieldColorMap(
    fields: Map<DocumentField, *>
): Map<DocumentField, Color> =
    fields.keys.mapIndexed { index, field -> field to colorForFieldIndex(index) }.toMap()

private fun rangesOverlap(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean =
    aStart < bEnd && bStart < aEnd

private fun buildHighlightedValue(
    text: String,
    highlights: List<HighlightRange>,
    selection: TextRange,
): TextFieldValue {
    val annotated = buildAnnotatedString {
        if (highlights.isEmpty()) {
            append(text)
            return@buildAnnotatedString
        }
        val sorted = highlights
            .filter { it.start in 0 until text.length && it.end in 1..text.length && it.start < it.end }
            .sortedBy { it.start }

        var cursor = 0
        for (h in sorted) {
            val start = maxOf(h.start, cursor)
            if (start > cursor) {
                append(text.substring(cursor, start))
            }
            if (h.end > start) {
                withStyle(
                    SpanStyle(
                        background = h.color.copy(alpha = 0.55f),
                        color = Color(0xFF1A1A1A),
                    )
                ) {
                    append(text.substring(start, h.end))
                }
                cursor = h.end
            }
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
    return TextFieldValue(annotated, selection)
}

/**
 * Собирает подсветки для текстового блока [blockId]
 * или для ячейки таблицы (если заданы row/col).
 */
private fun collectHighlights(
    blockId: Int,
    fields: Map<DocumentField, Map<List<Int>, String>>,
    fieldColors: Map<DocumentField, Color>,
    inProgressFragments: Map<List<Int>, String>,
    inProgressColor: Color,
    skipFieldLabel: String? = null,
    rowId: Int = -1,
    colId: Int = -1,
): List<HighlightRange> {
    val result = mutableListOf<HighlightRange>()

    fun matchesKey(key: List<Int>): Boolean {
        return if (rowId >= 0 && colId >= 0) {
            key.size == 5 && key[0] == blockId && key[1] == rowId && key[2] == colId
        } else {
            key.size == 3 && key[0] == blockId
        }
    }

    fun rangeFromKey(key: List<Int>): Pair<Int, Int> {
        return if (key.size == 5) key[3] to key[4] else key[1] to key[2]
    }

    fields.forEach { (field, fragments) ->
        if (skipFieldLabel != null && field.label == skipFieldLabel) return@forEach
        val color = fieldColors[field] ?: colorForFieldIndex(0)
        fragments.forEach { (key, _) ->
            if (matchesKey(key)) {
                val (start, end) = rangeFromKey(key)
                result.add(HighlightRange(start, end, color))
            }
        }
    }

    inProgressFragments.forEach { (key, _) ->
        if (matchesKey(key)) {
            val (start, end) = rangeFromKey(key)
            result.add(HighlightRange(start, end, inProgressColor))
        }
    }

    return result
}

private fun cellHighlightColor(
    tableId: Int,
    rowId: Int,
    colId: Int,
    fields: Map<DocumentField, Map<List<Int>, String>>,
    fieldColors: Map<DocumentField, Color>,
    inProgressFragments: Map<List<Int>, String>,
    inProgressColor: Color,
    skipFieldLabel: String? = null,
): Color? {
    fields.forEach { (field, fragments) ->
        if (skipFieldLabel != null && field.label == skipFieldLabel) return@forEach
        fragments.keys.forEach { key ->
            if (key.size == 5 && key[0] == tableId && key[1] == rowId && key[2] == colId) {
                return fieldColors[field]
            }
        }
    }
    inProgressFragments.keys.forEach { key ->
        if (key.size == 5 && key[0] == tableId && key[1] == rowId && key[2] == colId) {
            return inProgressColor
        }
    }
    return null
}

/** Проверяет, пересекается ли новый диапазон с уже занятыми фрагментами в том же блоке/ячейке */
private fun isRangeTaken(
    blockId: Int,
    start: Int,
    end: Int,
    fields: Map<DocumentField, Map<List<Int>, String>>,
    inProgressFragments: Map<List<Int>, String>,
    skipFieldLabel: String? = null,
    rowId: Int = -1,
    colId: Int = -1,
): Boolean {
    fun check(fragments: Map<List<Int>, String>): Boolean {
        fragments.keys.forEach { key ->
            if (rowId >= 0 && colId >= 0) {
                if (key.size == 5 &&
                    key[0] == blockId &&
                    key[1] == rowId &&
                    key[2] == colId &&
                    rangesOverlap(start, end, key[3], key[4])
                ) {
                    return true
                }
            } else if (key.size == 3 && key[0] == blockId) {
                if (rangesOverlap(start, end, key[1], key[2])) return true
            }
        }
        return false
    }
    fields.forEach { (field, fragments) ->
        if (skipFieldLabel != null && field.label == skipFieldLabel) return@forEach
        if (check(fragments)) return true
    }
    return check(inProgressFragments)
}

/** Текст ячейки с сохранением абзацев (как joinToString("\n")). */
private fun cellTextWithParagraphs(cell: XWPFTableCell): String =
    cell.paragraphs.joinToString("\n") { it.text ?: "" }

// Парсер DOCX файла.
// Абзацы внутри TextBlock склеиваются через '\n', чтобы в редакторе
// сохранялось деление на параграфы; та же схема используется в applyTemplateChangesByIndex.
private fun parseDocxFile(file: File): TemplateState {
    val doc = XWPFDocument(file.inputStream())
    val elements = mutableListOf<TemplateElement>()
    var idCounter = 0
    val paragraphBuffer = mutableListOf<String>()

    fun flushTextBlock() {
        if (paragraphBuffer.isEmpty()) return
        val text = paragraphBuffer.joinToString("\n")
        elements.add(TemplateElement.TextBlock(id = idCounter++, text = text))
        paragraphBuffer.clear()
    }

    doc.bodyElements.forEach { element ->
        when (element) {
            is XWPFParagraph -> {
                paragraphBuffer.add(element.text ?: "")
            }

            is XWPFTable -> {
                flushTextBlock()
                val rows = element.rows.map { row ->
                    row.tableCells.map { cell -> cellTextWithParagraphs(cell) }
                }
                elements.add(TemplateElement.Table(id = idCounter++, rows = rows))
            }
        }
    }
    flushTextBlock()

    return TemplateState(elements = elements)
}

@Composable
private fun TemplateHeader(
    templateState: TemplateState,
    onNameChanged: (String) -> Unit,
) {
    var nameForUser by remember { mutableStateOf(templateState.nameForUser) }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = nameForUser,
            onValueChange = {
                nameForUser = it
                templateState.nameForUser = it
                onNameChanged(it)
            },
            label = { Text("Название шаблона") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TextBlockEditor(
    element: TemplateElement,
    rowId: Int = -1,
    colId: Int = -1,
    highlights: List<HighlightRange> = emptyList(),
    onSelectedInfoChanged: (String, Int, Int, Int) -> Unit
) {
    val text = when (element) {
        is TemplateElement.Table -> {
            if (rowId != -1 && colId != -1) {
                element.rows[rowId][colId]
            } else {
                return
            }
        }
        is TemplateElement.TextBlock -> element.text
    }

    val id = when (element) {
        is TemplateElement.Table -> element.id
        is TemplateElement.TextBlock -> element.id
    }
    var selection by remember(text) { mutableStateOf(TextRange.Zero) }
    val textFieldValue = remember(text, highlights, selection) {
        buildHighlightedValue(text, highlights, selection)
    }
    val textBlockId = id

    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                selection = newValue.selection
                if (!newValue.selection.collapsed) {
                    val start = minOf(newValue.selection.start, newValue.selection.end)
                    val end = maxOf(newValue.selection.start, newValue.selection.end)

                    if (start >= 0 && end <= text.length && start < end) {
                        val selectedText = text.substring(start, end)
                        onSelectedInfoChanged(selectedText, textBlockId, start, end)
                    } else {
                        onSelectedInfoChanged("", 0, 0, 0)
                    }
                } else {
                    onSelectedInfoChanged("", 0, 0, 0)
                }
            },
            onTextLayout = { textLayoutResult.value = it },
            readOnly = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TableEditor(
    element: TemplateElement.Table,
    onCellClick: (Int, Int) -> Unit,
    cellColors: Map<Pair<Int, Int>, Color> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp)
    ) {
        element.rows.forEachIndexed { rowIdx, row ->
            Row {
                row.forEachIndexed { colIdx, cell ->
                    val highlight = cellColors[rowIdx to colIdx]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .border(1.dp, Color.Gray)
                            .background(highlight?.copy(alpha = 0.45f) ?: Color.Transparent)
                            .clickable {
                                onCellClick(rowIdx, colIdx)
                            }
                    ) {
                        Text(text = cell, modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDocumentFieldDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hint by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавление поля") },
        text = {
            Column {
                TextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Уточняющий вопрос") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(hint) }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun TableCellFieldDialog(
    element: TemplateElement.Table,
    rowId: Int,
    colId: Int,
    highlights: List<HighlightRange> = emptyList(),
    fields: Map<DocumentField, Map<List<Int>, String>> = emptyMap(),
    skipFieldLabel: String? = null,
    closeDialog: (Boolean) -> Unit,
    getChanges: (SnapshotStateMap<List<Int>, String>) -> Unit
) {
    val selectedFragments = remember { mutableStateMapOf<List<Int>, String>() }
    var currentSelectedText by remember { mutableStateOf("") }
    var tableId: Int? by remember { mutableStateOf(null) }
    var fragmentStartId: Int? by remember { mutableStateOf(null) }
    var fragmentEndId: Int? by remember { mutableStateOf(null) }

    val liveHighlights = remember(highlights, selectedFragments.toMap()) {
        highlights + selectedFragments.mapNotNull { (key, _) ->
            if (key.size == 5) {
                HighlightRange(key[3], key[4], Color(0xFF90CAF9))
            } else null
        }
    }

    fun tryAddCurrentFragment(): Boolean {
        val tid = tableId ?: return false
        val start = fragmentStartId ?: return false
        val end = fragmentEndId ?: return false
        if (currentSelectedText.isBlank()) return false
        if (isRangeTaken(
                blockId = tid,
                start = start,
                end = end,
                fields = fields,
                inProgressFragments = selectedFragments,
                skipFieldLabel = skipFieldLabel,
                rowId = rowId,
                colId = colId,
            )
        ) {
            return false
        }
        selectedFragments[listOf(tid, rowId, colId, start, end)] = currentSelectedText
        currentSelectedText = ""
        return true
    }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(
                onClick = {
                    tryAddCurrentFragment()
                    getChanges(selectedFragments)
                    closeDialog(false)
                }
            ) {
                Icon(AppIcons.Check, "")
            }
        },
        dismissButton = {
            TextButton(onClick = { closeDialog(false) }) {
                Text("Закрыть")
            }
        },
        title = {},
        text = {
            Row {
                Box(modifier = Modifier.weight(0.7f)) {
                    TextBlockEditor(
                        element = element,
                        rowId = rowId,
                        colId = colId,
                        highlights = liveHighlights,
                        onSelectedInfoChanged = { selectedText, tableIdLS, fragmentStartIdLS, fragmentEndIdLS ->
                            if (selectedText.isNotBlank()) {
                                currentSelectedText = selectedText
                                tableId = tableIdLS
                                fragmentStartId = fragmentStartIdLS
                                fragmentEndId = fragmentEndIdLS
                            }
                        }
                    )
                }

                LazyColumn(modifier = Modifier.weight(0.3f)) {
                    item { Text("Выделенный текст:", modifier = Modifier.padding(top = 12.dp)) }

                    items(selectedFragments.toList()) { (_, selectedText) ->
                        if (selectedText.isNotBlank()) {
                            Text(
                                selectedText,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .background(Color(0xFF90CAF9).copy(alpha = 0.55f))
                                    .padding(4.dp)
                            )
                        }
                    }

                    item { Text(currentSelectedText, modifier = Modifier.padding(8.dp)) }

                    item {
                        Button(onClick = { tryAddCurrentFragment() }) {
                            Icon(AppIcons.PlusImage, contentDescription = "Добавить")
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun LeftSide(
    modifier: Modifier,
    templateState: TemplateState,
    fields: Map<DocumentField, SnapshotStateMap<List<Int>, String>>,
    fieldColors: Map<DocumentField, Color>,
    selectedFragments: SnapshotStateMap<List<Int>, String>,
    inProgressColor: Color,
    skipFieldLabel: String?,
    getSelection: (String, Int, Int, Int) -> Unit,
    onFragmentsChange: (SnapshotStateMap<List<Int>, String>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedRow by remember { mutableIntStateOf(-1) }
    var selectedCol by remember { mutableIntStateOf(-1) }
    var curElement: TemplateElement.Table by remember {
        mutableStateOf(
            TemplateElement.Table(
                id = -1,
                rows = listOf(listOf(""))
            )
        )
    }
    val fragments = remember { mutableStateMapOf<List<Int>, String>() }

    LaunchedEffect(showDialog) {
        if (!showDialog && fragments.isNotEmpty()) {
            onFragmentsChange(fragments)
            fragments.clear()
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
    ) {
        items(templateState.elements) { element ->
            when (element) {
                is TemplateElement.TextBlock -> {
                    val highlights = collectHighlights(
                        blockId = element.id,
                        fields = fields,
                        fieldColors = fieldColors,
                        inProgressFragments = selectedFragments,
                        inProgressColor = inProgressColor,
                        skipFieldLabel = skipFieldLabel,
                    )
                    TextBlockEditor(
                        element = element,
                        highlights = highlights,
                        onSelectedInfoChanged = { newText, textBlockId, fragmentStartId, fragmentEndId ->
                            if (newText.isNotBlank()) {
                                getSelection(newText, textBlockId, fragmentStartId, fragmentEndId)
                            }
                        }
                    )
                }

                is TemplateElement.Table -> {
                    val cellColors = buildMap {
                        element.rows.forEachIndexed { rowIdx, row ->
                            row.indices.forEach { colIdx ->
                                cellHighlightColor(
                                    tableId = element.id,
                                    rowId = rowIdx,
                                    colId = colIdx,
                                    fields = fields,
                                    fieldColors = fieldColors,
                                    inProgressFragments = selectedFragments,
                                    inProgressColor = inProgressColor,
                                    skipFieldLabel = skipFieldLabel,
                                )?.let { put(rowIdx to colIdx, it) }
                            }
                        }
                    }
                    TableEditor(
                        element = element,
                        cellColors = cellColors,
                        onCellClick = { rowIdx, colIdx ->
                            selectedRow = rowIdx
                            selectedCol = colIdx
                            showDialog = true
                            curElement = element
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        val dialogHighlights = collectHighlights(
            blockId = curElement.id,
            fields = fields,
            fieldColors = fieldColors,
            inProgressFragments = selectedFragments,
            inProgressColor = inProgressColor,
            skipFieldLabel = skipFieldLabel,
            rowId = selectedRow,
            colId = selectedCol,
        )
        TableCellFieldDialog(
            element = curElement,
            rowId = selectedRow,
            colId = selectedCol,
            highlights = dialogHighlights,
            fields = fields,
            skipFieldLabel = skipFieldLabel,
            closeDialog = { value ->
                showDialog = value
            },
            getChanges = { value ->
                fragments.clear()
                fragments.putAll(value)
            }
        )
    }
}

@Composable
fun RightSide(
    modifier: Modifier,
    editorFlag: Boolean,
    hint: String,
    selectedFragments: SnapshotStateMap<List<Int>, String>,
    currentSelectedText: String,
    currentTextBlockId: Int?,
    currentStartId: Int?,
    currentEndId: Int?,
    fields: SnapshotStateMap<DocumentField, SnapshotStateMap<List<Int>, String>>,
    fieldColors: Map<DocumentField, Color>,
    inProgressColor: Color,
    addField: (Boolean) -> Unit,
    editorFlagChange: (Boolean) -> Unit,
    hintChange: (String) -> Unit,
    onAddFragment: (String, Int, Int, Int) -> Unit,
    currentSelectedTextChange: (String) -> Unit,
    clear: () -> Unit,
    getDocField: (DocumentField, SnapshotStateMap<List<Int>, String>) -> Unit,
    setSelectedFragment: (SnapshotStateMap<List<Int>, String>, String, DocumentField) -> Unit,
    clearSelectedFragment: () -> Unit
) {
    Box(modifier) {
        if (editorFlag) {
            IconButton(
                onClick = { editorFlagChange(false); clear() },
                Modifier.align(Alignment.TopStart).padding(0.dp)
            ) {
                Image(AppIcons.ArrowBack, "", colorFilter = ColorFilter.tint(Color.Blue))
            }
        }
        LazyColumn(
            modifier = Modifier
                .padding(8.dp)
                .padding(top = 40.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Top
        ) {
            if (editorFlag) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(inProgressColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        TextField(
                            value = hint,
                            onValueChange = { hintChange(it) },
                            label = { Text("Вопрос-подсказка") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item { Text("Выделенный текст:", modifier = Modifier.padding(top = 12.dp)) }

                items(selectedFragments.toList()) { (key, selectedText) ->
                    if (selectedText.isBlank()) return@items
                    if (key.size > 3) {
                        Column(
                            Modifier
                                .background(inProgressColor.copy(alpha = 0.55f))
                                .padding(8.dp)
                                .fillMaxWidth()
                        ) {
                            Text("Таблица ${key[0]}, Строка ${key[1]}, Столбец ${key[2]}")
                            Text(selectedText, modifier = Modifier.padding(2.dp))
                        }
                    } else {
                        Text(
                            selectedText,
                            modifier = Modifier
                                .padding(8.dp)
                                .background(inProgressColor.copy(alpha = 0.55f))
                                .padding(4.dp)
                                .fillMaxWidth()
                        )
                    }
                }

                item { Text(currentSelectedText, modifier = Modifier.padding(8.dp)) }

                item {
                    Button(onClick = {
                        if (currentSelectedText.isNotBlank() &&
                            currentTextBlockId != null &&
                            currentStartId != null &&
                            currentEndId != null
                        ) {
                            onAddFragment(
                                currentSelectedText,
                                currentTextBlockId,
                                currentStartId,
                                currentEndId
                            )
                            currentSelectedTextChange("")
                        }
                    }) {
                        Icon(AppIcons.PlusImage, contentDescription = "Добавить")
                    }
                }
            } else {
                if (fields.isNotEmpty()) {
                    items(fields.keys.toList()) { field ->
                        val color = fieldColors[field] ?: Color.LightGray
                        Button(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color.copy(alpha = 0.75f),
                                contentColor = Color(0xFF1A1A1A)
                            ),
                            onClick = {
                                setSelectedFragment(fields[field]!!, field.label, field)
                                editorFlagChange(true)
                            }
                        ) {
                            Text(field.label)
                        }
                    }
                } else {
                    item {
                        Text("Выделите текст для просмотра", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        if (editorFlag) {
            Button(
                onClick = {
                    if (hint.isBlank() || selectedFragments.isEmpty()) {
                        return@Button
                    }
                    val documentField = DocumentField(
                        label = hint,
                        key = "{{${hint.uppercase().transliterateRussian().replaceSpacesWithUnderscores()}}}"
                    )
                    if (currentSelectedText.isNotBlank() &&
                        currentTextBlockId != null &&
                        currentStartId != null &&
                        currentEndId != null
                    ) {
                        onAddFragment(
                            currentSelectedText,
                            currentTextBlockId,
                            currentStartId,
                            currentEndId
                        )
                        currentSelectedTextChange("")
                    }
                    // После возможного onAddFragment ещё раз проверяем: если фрагмент
                    // не добавился из‑за пересечения и список пуст — не сохраняем поле.
                    if (selectedFragments.isEmpty()) {
                        return@Button
                    }
                    getDocField(documentField, selectedFragments)
                    editorFlagChange(false)
                },
                Modifier.align(Alignment.BottomCenter).padding(0.dp).fillMaxWidth(),
                enabled = hint.isNotBlank() &&
                    (selectedFragments.isNotEmpty() || currentSelectedText.isNotBlank())
            ) {
                Text("Готово")
            }
        }
        if (!editorFlag) {
            Button(
                onClick = {
                    clearSelectedFragment()
                    addField(true)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
            ) {
                Text("Добавить поле")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(file: File) {
    val templateState = remember { parseDocxFile(file) }
    val manager = Manager<DocumentTemplate>(DocumentTemplate::class.java)

    val fields = remember { mutableStateMapOf<DocumentField, SnapshotStateMap<List<Int>, String>>() }

    var editorFlag by remember { mutableStateOf(false) }
    var currentHint by remember { mutableStateOf("") }
    var editingField by remember { mutableStateOf<DocumentField?>(null) }
    var showAddDocumentFieldDialog by remember { mutableStateOf(false) }
    var nameForUser by remember { mutableStateOf(templateState.nameForUser) }

    val selectedFragments = remember { mutableStateMapOf<List<Int>, String>() }

    val fieldColors = remember(fields.keys.toList()) { fieldColorMap(fields) }
    val skipFieldLabel = if (editorFlag) currentHint.ifBlank { null } else null
    val inProgressColor = remember(editingField, fieldColors, fields.size, editorFlag) {
        editingField?.let { fieldColors[it] }
            ?: colorForFieldIndex(fields.size)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        Navigation.navigateTo(Screen.MainScreenRoute)
                    }) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Закрыть")
                    }
                },
                title = { Text("Редактор шаблона") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        enabled = nameForUser.isNotBlank(),
                        onClick = {
                            if (nameForUser.isBlank()) return@IconButton

                            val nameForDev = nameForUser.transliterateRussian().replaceSpacesWithUnderscores()
                            val templateFolder = File(System.getProperty("user.home"), "DocumentEditor/Templates")
                            if (!templateFolder.exists()) {
                                templateFolder.mkdirs()
                            }
                            val templateFile = File(templateFolder, "${nameForDev}.docx")
                            val docFields = mutableListOf<DocumentField>()

                            val existingTemplates = manager.documents as MutableList<DocumentTemplate>
                            val existingWithSameDevName = existingTemplates.find { it.nameForDevelop == nameForDev }

                            val id = if (existingWithSameDevName != null) {
                                existingWithSameDevName.id
                            } else {
                                if (existingTemplates.isNotEmpty()) {
                                    existingTemplates.maxOf { it.id } + 1
                                } else {
                                    0
                                }
                            }

                            applyTemplateChangesByIndex(
                                sourceFile = file,
                                targetFile = templateFile,
                                fields = fields,
                            )

                            fields.keys.forEach { documentField ->
                                docFields.add(documentField)
                            }

                            if (existingWithSameDevName != null) {
                                existingTemplates.removeIf { it.id == existingWithSameDevName.id }
                            }

                            manager.addDocument(
                                DocumentTemplate(
                                    id = id,
                                    nameForUser = nameForUser,
                                    nameForDevelop = nameForDev,
                                    fields = docFields
                                )
                            )

                            Navigation.navigateTo(Screen.MainScreenRoute)
                        }
                    ) {
                        Icon(AppIcons.Check, contentDescription = "Сохранить")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                TemplateHeader(
                    templateState,
                    onNameChanged = { newName ->
                        nameForUser = newName
                    }
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    var selectedText by remember { mutableStateOf("") }
                    var textBlockId: Int? by remember { mutableStateOf(null) }
                    var fragmentStartId: Int? by remember { mutableStateOf(null) }
                    var fragmentEndId: Int? by remember { mutableStateOf(null) }

                    LeftSide(
                        modifier = Modifier.weight(0.7f),
                        templateState = templateState,
                        fields = fields,
                        fieldColors = fieldColors,
                        selectedFragments = selectedFragments,
                        inProgressColor = inProgressColor,
                        skipFieldLabel = skipFieldLabel,
                        getSelection = { newTextLS, textBlockIdLS, fragmentStartIdLS, fragmentEndIdLS ->
                            selectedText = newTextLS
                            textBlockId = textBlockIdLS
                            fragmentStartId = fragmentStartIdLS
                            fragmentEndId = fragmentEndIdLS
                        },
                        onFragmentsChange = { newFragments ->
                            selectedFragments.putAll(newFragments)
                        },
                    )

                    RightSide(
                        Modifier.weight(0.3f),
                        editorFlag = editorFlag,
                        hint = currentHint,
                        selectedFragments = selectedFragments,
                        currentSelectedText = selectedText,
                        currentTextBlockId = textBlockId,
                        currentStartId = fragmentStartId,
                        currentEndId = fragmentEndId,
                        fields = fields,
                        fieldColors = fieldColors,
                        inProgressColor = inProgressColor,
                        addField = { flag ->
                            editingField = null
                            showAddDocumentFieldDialog = flag
                        },
                        editorFlagChange = { newFlag ->
                            editorFlag = newFlag
                            if (!newFlag) editingField = null
                        },
                        hintChange = { newHint ->
                            currentHint = newHint
                        },
                        onAddFragment = { text, blockId, startId, endId ->
                            if (isRangeTaken(
                                    blockId = blockId,
                                    start = startId,
                                    end = endId,
                                    fields = fields,
                                    inProgressFragments = selectedFragments,
                                    skipFieldLabel = skipFieldLabel,
                                )
                            ) {
                                return@RightSide
                            }
                            val key = listOf(blockId, startId, endId)
                            selectedFragments[key] = text
                        },
                        currentSelectedTextChange = { newText ->
                            selectedText = newText
                        },
                        clear = {
                            selectedText = ""
                            selectedFragments.clear()
                            editingField = null
                        },
                        getDocField = { documentField, selectedFragment ->
                            editingField?.let { fields.remove(it) }
                            val copy = mutableStateMapOf<List<Int>, String>()
                            copy.putAll(selectedFragment)
                            fields[documentField] = copy
                            editingField = null
                        },
                        setSelectedFragment = { newSelectedFragments, newHint, field ->
                            selectedFragments.clear()
                            selectedFragments.putAll(newSelectedFragments)
                            currentHint = newHint
                            editingField = field
                        },
                        clearSelectedFragment = {
                            selectedFragments.clear()
                            editingField = null
                        }
                    )
                }
            }

            if (showAddDocumentFieldDialog) {
                AddDocumentFieldDialog(
                    onConfirm = { hint ->
                        editorFlag = true
                        currentHint = hint
                        editingField = null
                        showAddDocumentFieldDialog = false
                    },
                    onDismiss = { showAddDocumentFieldDialog = false }
                )
            }
        }
    }
}
