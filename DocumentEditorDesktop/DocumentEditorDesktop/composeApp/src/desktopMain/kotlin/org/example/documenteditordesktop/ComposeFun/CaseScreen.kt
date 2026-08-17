package org.example.documenteditordesktop.ComposeFun

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.documenteditor.ClassesViewModels.SaveViewModel
import kotlinx.coroutines.launch
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.DocumentCase
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.Manager
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.TemplateAnswers
import org.example.documenteditordesktop.ClassesViewModels.TypicalQuestion
import org.example.documenteditordesktop.functions.countMatchedFields
import org.example.documenteditordesktop.functions.isTemplateFullyFilled
import org.example.documenteditordesktop.functions.mergedAnswers

private val Background = Color(0xff9DA7E8)
private val BarColor = Color(0xff8192fe)
private val CardBorder = Color(0xff8192fe)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseScreen(caseId: Int?) {
    val caseManager = remember { Manager(DocumentCase::class.java) }
    val templates = remember {
        Manager(DocumentTemplate::class.java).loadJson()
    }
    val existing = remember(caseId) {
        caseId?.let { id -> caseManager.loadJson().firstOrNull { it.id == id } }
    }

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    val selectedIds = remember {
        mutableStateListOf<Int>().apply { existing?.templateIds?.let { addAll(it) } }
    }
    val typicalQuestions = remember {
        mutableStateListOf<TypicalQuestion>().apply {
            existing?.typicalQuestions?.forEach { item ->
                add(
                    TypicalQuestion(
                        questions = item.questions.toMutableList(),
                        answer = item.answer
                    )
                )
            }
        }
    }
    val filledTemplates = remember {
        mutableStateListOf<TemplateAnswers>().apply {
            existing?.filledTemplates?.forEach { item ->
                add(TemplateAnswers(item.templateId, item.values.toMutableMap()))
            }
        }
    }
    var persistedId by remember { mutableStateOf(caseId) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val saveViewModel = remember { SaveViewModel() }

    fun persist(): DocumentCase {
        val id = persistedId ?: ((caseManager.documents.maxOfOrNull { it.id } ?: -1) + 1)
        val case = DocumentCase(
            id = id,
            name = name.ifBlank { "Дело ${id + 1}" },
            templateIds = selectedIds.filter { id -> templates.any { it.id == id } }.toMutableList(),
            typicalQuestions = typicalQuestions.map {
                TypicalQuestion(it.questions.toMutableList(), it.answer)
            }.toMutableList(),
            filledTemplates = filledTemplates.map {
                TemplateAnswers(it.templateId, it.values.toMutableMap())
            }.toMutableList()
        )
        caseManager.updateById(id, case)
        persistedId = id
        if (name.isBlank()) name = case.name
        return case
    }

    fun selectedTemplates(): List<DocumentTemplate> =
        templates.filter { it.id in selectedIds }

    fun answersFor(template: DocumentTemplate): Map<String, String> =
        mergedAnswers(template, typicalQuestions, filledTemplates)

    fun persistIfNeeded() {
        if (persistedId != null || selectedIds.isNotEmpty() || typicalQuestions.isNotEmpty()) {
            persist()
        }
    }

    fun openTemplate(template: DocumentTemplate) {
        val case = persist()
        Navigation.navigateTo(
            Screen.TemplateInputRoute(
                templateId = template.id,
                nameForDev = template.nameForDevelop,
                dict = answersFor(template),
                returnTo = Screen.CaseScreenRoute(case.id),
                caseId = case.id
            )
        )
    }

    fun saveCaseDocuments() {
        val chosen = selectedTemplates()
        if (chosen.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("Выберите хотя бы один шаблон") }
            return
        }
        val incomplete = chosen.filter { !isTemplateFullyFilled(it, answersFor(it)) }
        if (incomplete.isNotEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Сначала заполните: ${incomplete.joinToString(", ") { it.nameForUser }}"
                )
            }
            return
        }
        persist()
        val folder = saveViewModel.saveCaseFolder(
            caseName = name.ifBlank { "Дело" },
            templates = chosen,
            answersByTemplate = chosen.associate { it.id to answersFor(it) }
        )
        scope.launch {
            if (folder != null) {
                snackbarHostState.showSnackbar("Дело сохранено: ${folder.absolutePath}")
            } else {
                snackbarHostState.showSnackbar(
                    saveViewModel.lastError ?: "Не удалось сохранить дело"
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название дела") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(
                        text = "Шаблоны",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Отметьте документы, которые войдут в дело",
                        fontSize = 13.sp,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    )
                }
                if (templates.isEmpty()) {
                    item {
                        Text(
                            text = "Сначала добавьте шаблоны на главном экране",
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    items(templates, key = { "${it.id}#${System.identityHashCode(it)}" }) { template ->
                        val selected = template.id in selectedIds
                        val answers = answersFor(template)
                        val filled = selected && isTemplateFullyFilled(template, answers)
                        val filledCount = template.fields.count { answers[it.key]?.isNotBlank() == true }
                        TemplateSelectRow(
                            template = template,
                            selected = selected,
                            filled = filled,
                            statusText = if (!selected) {
                                "${template.fields.size} полей"
                            } else if (filled) {
                                "Заполнен"
                            } else {
                                "Заполнено $filledCount из ${template.fields.size}"
                            },
                            onToggle = {
                                if (selected) selectedIds.remove(template.id)
                                else selectedIds.add(template.id)
                                persistIfNeeded()
                            },
                            onFill = { openTemplate(template) }
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Типовые вопросы",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                editingQuestionIndex = null
                                showQuestionDialog = true
                            }
                        ) {
                            Text("Добавить типовой вопрос")
                        }
                    }
                    Text(
                        text = "Один ответ можно привязать к нескольким формулировкам — программа сама найдёт их в шаблонах",
                        fontSize = 13.sp,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    )
                }
                if (typicalQuestions.isEmpty()) {
                    item {
                        Text(
                            text = "Пока нет типовых вопросов",
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    items(typicalQuestions.size) { index ->
                        val item = typicalQuestions[index]
                        TypicalQuestionCard(
                            question = item,
                            onEdit = {
                                editingQuestionIndex = index
                                showQuestionDialog = true
                            },
                            onDelete = {
                                typicalQuestions.removeAt(index)
                                persistIfNeeded()
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }

    TopAppBar(
        title = { Text(if (persistedId == null) "Новое дело" else name.ifBlank { "Дело" }) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BarColor,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = {
                persistIfNeeded()
                Navigation.navigateTo(Screen.MainScreenRoute)
            }) {
                Icon(AppIcons.ArrowBack, contentDescription = "Назад")
            }
        },
        actions = {
            TextButton(onClick = { saveCaseDocuments() }) {
                Text("Сохранить дело", color = Color.White)
            }
            IconButton(
                onClick = {
                    if (selectedIds.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Выберите хотя бы один шаблон")
                        }
                    } else {
                        persist()
                        Navigation.navigateTo(Screen.MainScreenRoute)
                    }
                }
            ) {
                Icon(AppIcons.Check, contentDescription = "Сохранить")
            }
        }
    )

    if (showQuestionDialog) {
        val editing = editingQuestionIndex?.let { typicalQuestions.getOrNull(it) }
        TypicalQuestionDialog(
            initial = editing,
            onDismiss = { showQuestionDialog = false },
            onConfirm = { questions, answer ->
                val typical = TypicalQuestion(questions.toMutableList(), answer)
                val editIndex = editingQuestionIndex
                if (editIndex != null && editIndex in typicalQuestions.indices) {
                    typicalQuestions[editIndex] = typical
                } else {
                    typicalQuestions.add(typical)
                }
                showQuestionDialog = false
                persistIfNeeded()
                val matched = countMatchedFields(selectedTemplates(), listOf(typical))
                scope.launch {
                    val message = when {
                        selectedIds.isEmpty() ->
                            "Вопрос сохранён. Выберите шаблоны, чтобы подставить ответы"
                        matched == 0 ->
                            "В выбранных шаблонах таких вопросов нет"
                        else ->
                            "Заполнено полей: $matched"
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }
}

@Composable
private fun TemplateSelectRow(
    template: DocumentTemplate,
    selected: Boolean,
    filled: Boolean,
    statusText: String,
    onToggle: () -> Unit,
    onFill: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, if (selected) CardBorder else Color.White, RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.nameForUser,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = if (filled) Color(0xFF2E7D32) else Color.Black.copy(alpha = 0.55f)
            )
        }
        if (selected) {
            TextButton(onClick = onFill) {
                Text(if (filled) "Изменить" else "Заполнить")
            }
        }
    }
}

@Composable
private fun TypicalQuestionCard(
    question: TypicalQuestion,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onEdit)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = question.questions.filter { it.isNotBlank() }.joinToString(" / "),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Удалить",
                color = Color(0xFFB00020),
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable(onClick = onDelete)
                    .padding(start = 8.dp)
            )
        }
        Text(
            text = question.answer,
            fontSize = 14.sp,
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun TypicalQuestionDialog(
    initial: TypicalQuestion?,
    onDismiss: () -> Unit,
    onConfirm: (List<String>, String) -> Unit
) {
    val formulations = remember {
        mutableStateListOf<String>().apply {
            val existing = initial?.questions?.filter { it.isNotBlank() }.orEmpty()
            if (existing.isEmpty()) add("") else addAll(existing)
        }
    }
    var answer by remember { mutableStateOf(initial?.answer.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Типовой вопрос" else "Изменить вопрос") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Формулировки в шаблонах",
                    fontSize = 13.sp,
                    color = Color.Black.copy(alpha = 0.65f)
                )
                formulations.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { formulations[index] = it },
                        label = { Text("Вопрос ${index + 1}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        singleLine = true
                    )
                }
                TextButton(
                    onClick = { formulations.add("") },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Добавить формулировку")
                }
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Ответ") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                enabled = formulations.any { it.isNotBlank() } && answer.isNotBlank(),
                onClick = {
                    val questions = formulations.map { it.trim() }.filter { it.isNotEmpty() }
                    if (questions.isNotEmpty() && answer.isNotBlank()) {
                        onConfirm(questions, answer.trim())
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
