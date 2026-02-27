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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.documenteditor.ClassesViewModels.templates
import org.apache.poi.xwpf.usermodel.*
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.TextParamsForSelectionViewModel
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

// Парсер DOCX файла
private fun parseDocxFile(file: File): TemplateState {
    val doc = XWPFDocument(file.inputStream())
    val elements = mutableListOf<TemplateElement>()
    var idCounter = 0
    var paragraphText = ""

    doc.bodyElements.forEach { element ->
        when (element) {
            is XWPFParagraph -> {
                if (element.text.isNotBlank()) {
                    paragraphText += element.text
                } else {
                    paragraphText += "\n"
                }
            }

            is XWPFTable -> {
                if (paragraphText != "") {
                    elements.add(TemplateElement.TextBlock(id = idCounter++, text = paragraphText))
                }
                paragraphText = ""
                val rows = element.rows.map { row -> row.tableCells.map { cell -> cell.text } }
                elements.add(TemplateElement.Table(id = idCounter++, rows = rows))
            }
        }
    }
    if (paragraphText != "") {
        elements.add(TemplateElement.TextBlock(id = idCounter++, text = paragraphText))
    }

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
    onSelectedInfoChanged: (String, Int, Int, Int) -> Unit
) {
    val text = when (element) {
        is TemplateElement.Table -> {
            if (rowId != -1 && colId != -1){
                element.rows[rowId][colId]
            }else{
                return
            }
        }
        is TemplateElement.TextBlock -> element.text
    }

    val id = when (element) {
        is TemplateElement.Table -> element.id
        is TemplateElement.TextBlock -> element.id
    }
    var textFieldValue by remember(text) {
        mutableStateOf(TextFieldValue(text, TextRange.Zero))
    }
    val textBlockId = id

    val textLayoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                if (!newValue.selection.collapsed) {
                    val start = minOf(newValue.selection.start, newValue.selection.end)
                    val end = maxOf(newValue.selection.start, newValue.selection.end)

                    if (start >= 0 && end <= text.length && start <= end) {
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp)
    ) {
        element.rows.forEachIndexed { rowIdx, row ->
            Row {
                row.forEachIndexed { colIdx, cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .border(1.dp, Color.Gray)
                            .clickable {
                                println(rowIdx)
                                println(colIdx)
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
fun TableCellFieldDialog(
    element: TemplateElement.Table,
    rowId: Int,
    colId: Int,
    closeDialog: (Boolean) -> Unit,
    getChanges: (SnapshotStateMap<List<Int>, String>) -> Unit
) {
    val selectedFragments = remember { mutableStateMapOf<List<Int>, String>() }
    var currentSelectedText by remember { mutableStateOf("") }
    var tableId: Int? by remember { mutableStateOf(null) }
    var fragmentStartId: Int? by remember { mutableStateOf(null) }
    var fragmentEndId: Int? by remember { mutableStateOf(null) }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(
                onClick = {
                    if (currentSelectedText.isNotBlank()) {
                        val key = listOfNotNull(tableId, rowId, colId, fragmentStartId, fragmentEndId)
                        selectedFragments[key] = currentSelectedText
                        currentSelectedText = ""
                    }
                    getChanges(selectedFragments)
                    closeDialog(false)
                }
            ){
                Icon(AppIcons.Check, "")
            }
        },
        dismissButton = {
            TextButton(onClick = {closeDialog(false)}){
                Text("Закрыть")
            }
        },
        title = {},
        text = {
            Row {
                Box(Modifier.weight(0.7f)) {
                    TextBlockEditor(
                        element = element,
                        rowId = rowId,
                        colId = colId,
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

                LazyColumn(Modifier.weight(0.3f)) {
                    item { Text("Выделенный текст:", modifier = Modifier.padding(top = 12.dp)) }

                    // отображаем список ранее добавленных выделений
                    items(selectedFragments.values.filter { it.isNotBlank() }) { selectedText ->
                        Text(selectedText, modifier = Modifier.padding(8.dp))
                    }

                    // отображаем текущее выделение
                    item { Text(currentSelectedText, modifier = Modifier.padding(8.dp)) }

                    // кнопка +
                    item {
                        Button(onClick = {
                            if (currentSelectedText.isNotBlank()) {
                                val key = listOfNotNull(tableId, rowId, colId, fragmentStartId, fragmentEndId)
                                selectedFragments[key] = currentSelectedText
                                currentSelectedText = ""
                            }
                        }) {
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
    getSelection: (String, Int, Int, Int) -> Unit,
    onFragmentsChange: (SnapshotStateMap<List<Int>, String>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedTable by remember { mutableIntStateOf(-1) }
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

    LaunchedEffect(showDialog){
        if(!showDialog && fragments.isNotEmpty()){
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
                    TextBlockEditor(
                        element = element,
                        onSelectedInfoChanged = { newText, textBlockId, fragmentStartId, fragmentEndId ->
                            if (newText.isNotBlank()) {
                                getSelection(newText, textBlockId, fragmentStartId, fragmentEndId)
                            }
                        }
                    )
                }

                is TemplateElement.Table -> {
                    TableEditor(
                        element = element,
                        onCellClick = {rowIdx, colIdx ->
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
        TableCellFieldDialog(
            element = curElement,
            rowId = selectedRow,
            colId = selectedCol,
            closeDialog = {value ->
                showDialog = value
            },
            getChanges = {value->
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
    fields: MutableMap<DocumentField, SnapshotStateMap<List<Int>, String>>,
    addField: (Boolean) -> Unit,
    editorFlagChange: (Boolean) -> Unit,
    hintChange: (String) -> Unit,
    onAddFragment: (String, Int, Int, Int) -> Unit,
    currentSelectedTextChange: (String) -> Unit,
    clear:() -> Unit,
    getDocField: (DocumentField, SnapshotStateMap<List<Int>, String>) -> Unit,
    setSelectedFragment: (SnapshotStateMap<List<Int>, String>, String) -> Unit,
    clearSelectedFragment: () -> Unit
) {
    Box(
        modifier
    ) {
        if (editorFlag) {
            IconButton(onClick = { editorFlagChange(false); clear()}, Modifier.align(Alignment.TopStart).padding(0.dp)) {
                Image(AppIcons.ArrowBack, "", colorFilter = ColorFilter.tint(Color.Blue))
            }
        }
        LazyColumn(
            modifier = Modifier
                .padding(8.dp)
                .padding(top = 40.dp),
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (editorFlag) {
                // поле подсказки (пока без изменения текста)
                item {
                    TextField(
                        value = hint,
                        onValueChange = { hintChange(it) },
                        label = { Text("Вопрос-подсказка") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item { Text("Выделенный текст:", modifier = Modifier.padding(top = 12.dp)) }

                // отображаем список ранее добавленных выделений
                items(selectedFragments.values.filter { it.isNotBlank() }) { selectedText ->
                    val key = selectedFragments.filterValues { it == selectedText }.keys.firstOrNull()
                    if(key?.size!! > 3){
                        Column(
                            Modifier.background(Color.Gray).padding(8.dp).fillMaxWidth()
                        ){
                            Text("Таблица ${key[0]}, Строка ${key[1]}, Столбец ${key[2]}")
                            Text(selectedText, modifier = Modifier.padding(2.dp))
                        }
                    }else {
                        Text(selectedText, modifier = Modifier.padding(8.dp))
                    }
                }

                // отображаем текущее выделение
                item { Text(currentSelectedText, modifier = Modifier.padding(8.dp)) }

                // кнопка +
                item {
                    Button(onClick = {
                        if (currentSelectedText.isNotBlank()) {
                            onAddFragment(currentSelectedText, currentTextBlockId!!, currentStartId!!, currentEndId!!)
                            currentSelectedTextChange("")
                        }
                    }) {
                        Icon(AppIcons.PlusImage, contentDescription = "Добавить")
                    }
                }
            } else {
                if (fields.isNotEmpty()) {
                    items(fields.keys.toList()) { field ->
                        Button(
                            modifier = Modifier
                                .background(Color.LightGray)
                                .padding(10.dp)
                                .fillMaxWidth(),
                            onClick = {
                                setSelectedFragment(fields[field]!!, field.label)
                                println(fields)
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
                    val documentField = DocumentField(
                        label = hint,
                        key = "{{${hint.uppercase().transliterateRussian().replaceSpacesWithUnderscores()}}}"
                    )
                    if (currentSelectedText.isNotBlank()) {
                        onAddFragment(currentSelectedText, currentTextBlockId!!, currentStartId!!, currentEndId!!)
                        currentSelectedTextChange("")
                    }
                    getDocField(documentField, selectedFragments)
                    println(fields)
                    editorFlagChange(false)
                },
                Modifier.align(Alignment.BottomCenter).padding(0.dp).fillMaxWidth()
            ) {
                Text("Готово")
            }
        }
        // Фиксированная кнопка "Добавить поле" в правом нижнем углу
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
    val viewModel = remember { TextParamsForSelectionViewModel() }
    val coroutineScope = rememberCoroutineScope()
    val templateState = remember { parseDocxFile(file) }
    val manager = Manager<DocumentTemplate>(DocumentTemplate::class.java)

    val fields = remember { mutableMapOf<DocumentField, SnapshotStateMap<List<Int>, String>>() }
    val textChangeMap = remember { mutableStateMapOf<String, List<List<Int>>>() }

    var showCustomToolbar by remember { mutableStateOf(false) }
    var editorFlag by remember { mutableStateOf(false) }
    var currentHint by remember { mutableStateOf("") }

    var currentSelectedText by remember { mutableStateOf("") }
    var currentTextBlockId: Int? by remember { mutableStateOf(null) }
    var currentStartId: Int? by remember { mutableStateOf(null) }
    var currentEndId: Int? by remember { mutableStateOf(null) }
    var currentField by remember { mutableStateOf<DocumentField?>(null) }
    var showAddDocumentFieldDialog by remember { mutableStateOf(false) }
    var nameForUser by remember { mutableStateOf(templateState.nameForUser) }

    // список всех добавленных выделенных фрагментов
    val selectedFragments = remember { mutableStateMapOf<List<Int>, String>() }

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
                            // Не даём сохранить шаблон без имени
                            if (nameForUser.isBlank()) return@IconButton

                            val nameForDev = nameForUser.transliterateRussian().replaceSpacesWithUnderscores()
                            val templateFolder = File(System.getProperty("user.home"),"DocumentEditor/Templates")
                            val templateFile = File(templateFolder, "${nameForDev}.docx")
                            val docFields = mutableListOf<DocumentField>()

                            // Гарантируем уникальный id на основе актуального содержимого JSON
                            val existingTemplates = manager.documents as MutableList<DocumentTemplate>
                            val existingWithSameDevName = existingTemplates.find { it.nameForDevelop == nameForDev }

                            val id = if (existingWithSameDevName != null) {
                                // Переиспользуем id, если такой шаблон уже есть
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

                            // Если шаблон с таким nameForDevelop уже существует — обновляем запись
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
                // Header
                TemplateHeader(
                    templateState,
                    onNameChanged = { newName ->
                        nameForUser = newName
                    }
                )

                // Основной контент: слева документ, справа панель выделенного текста
                Row(modifier = Modifier.fillMaxWidth()) {
                    var selectedText by remember { mutableStateOf("") }
                    var textBlockId: Int? by remember { mutableStateOf(null) }
                    var fragmentStartId: Int? by remember { mutableStateOf(null) }
                    var fragmentEndId: Int? by remember { mutableStateOf(null) }
                    // Левая часть — документ
                    LeftSide(
                        modifier = Modifier.weight(0.7f),
                        templateState = templateState,
                        getSelection = { newTextLS, textBlockIdLS, fragmentStartIdLS, fragmentEndIdLS ->
                            selectedText = newTextLS
                            textBlockId = textBlockIdLS
                            fragmentStartId = fragmentStartIdLS
                            fragmentEndId = fragmentEndIdLS
                        },
                        onFragmentsChange = {newFragments ->
                            selectedFragments.putAll(newFragments)
                        },
                    )

                    // Правая часть — панель выделенного текста
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
                        addField = { flag ->
                            showAddDocumentFieldDialog = flag
                        },
                        editorFlagChange = { newFlag ->
                            editorFlag = newFlag
                        },
                        hintChange = { newHint ->
                            currentHint = newHint
                        },
                        onAddFragment = { text, blockId, startId, endId ->
                            // Добавляем фрагмент в карту
                            val key = listOf(blockId, startId, endId)
                            selectedFragments[key] = text
                        },
                        currentSelectedTextChange = { newText ->
                            selectedText = newText
                        },
                        clear = {
                            selectedText = ""
                            selectedFragments.clear()
                        },
                        getDocField = { documentField, selectedFragment ->
                            val copy = mutableStateMapOf<List<Int>, String>()
                            copy.putAll(selectedFragment)
                            fields[documentField] = copy
                        },
                        setSelectedFragment = { newSelectedFragments, newHint ->
                            selectedFragments.clear()
                            selectedFragments.putAll(newSelectedFragments)
                            currentHint = newHint
                        },
                        clearSelectedFragment = {
                            selectedFragments.clear()
                        }
                    )
                }
            }

            if (showAddDocumentFieldDialog) {
                AddDocumentFieldDialog(
                    onConfirm = { hint ->
                        editorFlag = true
                        currentHint = hint
                        showAddDocumentFieldDialog = false
                    },
                    onDismiss = { showAddDocumentFieldDialog = false }
                )
            }

        }
    }
}