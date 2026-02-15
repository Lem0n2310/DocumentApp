package org.example.documenteditordesktop.ComposeFun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.documenteditor.ClassesViewModels.FieldValuesViewModel
import com.example.documenteditor.ClassesViewModels.SaveViewModel
import com.example.documenteditor.functions.isFull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.SettingsManager
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.Manager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateInput(templates: List<DocumentTemplate>, templateId: Int, dict: Map<String, String>? = null) {
    val coroutineScope = rememberCoroutineScope()

    // вьюхи
    val saveViewModel = remember { SaveViewModel() }
    val fieldViewModel = remember { FieldValuesViewModel() }
    var settings by remember { mutableStateOf(SettingsManager.loadSettings()) }

    // Флаги
    val showCheckValuesFlagSetting = settings.showCheckValuesFlag // Показывть окно проверки заполнения данных (вообще)
    var showCheckValuesDialog by remember { mutableStateOf(false) } //Показывать окно сохранения введенных значений (В нужный момент)
    val showLeaveValueAlertFlagSetting = settings.showLeaveValueAlertFlag // Показывать окно сохранения введенных значений (для отображения вообще)
    var showLeaveValueDialog by remember { mutableStateOf(false) } // Показывать окно сохранения введенных значений (для отображения в нужный момент)
    val isSaveValue = settings.isSaveValue // Сохранять введенные значения после сохранения
    val currentTemplates by remember {
        mutableStateOf(Manager<DocumentTemplate>(DocumentTemplate::class.java).loadJson())
    }

    // Шаблон
    val selectedTemplate by remember { mutableStateOf(currentTemplates.first { it.id == templateId }) } // Выбраный шаблон

    LaunchedEffect(Unit) {
        if(fieldViewModel.forFlag) {
            selectedTemplate.fields.forEach { field ->
                fieldViewModel.fieldValues[field.key] = ""
            }
            fieldViewModel.forFlag = false
        }
        if(!dict.isNullOrEmpty()){
            selectedTemplate.fields.forEach { documentField ->
                fieldViewModel.fieldValues[documentField.key] = dict[documentField.key]!!
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xff9DA7E8)),
        contentAlignment = Alignment.Center,
    ) {
        if (showCheckValuesDialog) { // Отображение диалога подтверждения заполнения всех значений
            AlertDialog(
                onDismissRequest = { showCheckValuesDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        showCheckValuesDialog = false
                        if (showLeaveValueAlertFlagSetting) { // Проверяем включено ли в настройках отбражение диалога
                            showLeaveValueDialog = true // включаем диалог
                        } else {
                            if (isSaveValue) {
                                coroutineScope.launch {
                                    saveViewModel.save(
                                        selectedTemplate = selectedTemplate,
                                        fieldValues = fieldViewModel.fieldValues,
                                        nameForDev = selectedTemplate.nameForDevelop,
                                        defaultFileName = selectedTemplate.nameForUser + ".docx",
                                        templateId = templateId,
                                    )
                                }
                            } // Проверяем включено ли в настройках сохранение значений, сохраняем
                            else {
                                coroutineScope.launch {
                                    saveViewModel.save(
                                        selectedTemplate = selectedTemplate,
                                        fieldValues = fieldViewModel.fieldValues,
                                        nameForDev = selectedTemplate.nameForDevelop,
                                        defaultFileName = selectedTemplate.nameForUser + ".docx",
                                        templateId = templateId,
                                    )
                                } // Сохраняем
                                coroutineScope.launch {
                                    delay(2000) // Через 2 секунды удаляем введенные значения, если файл сохранен
                                    if (saveViewModel.isFileSaved) {
                                        fieldViewModel.clearValues()
                                    }
                                }
                            }
                        }
                    }) { Text("Всё равно сохранить") }
                },
                dismissButton = { TextButton(onClick = { showCheckValuesDialog = false }) { Text("Дополнить") } },
                title = { Text("Были заполнены не все строки") },
                text = {
                    Row {
                        Checkbox(
                            checked = !showCheckValuesFlagSetting,
                            onCheckedChange = { // если галочка стоит
                                //showMessage(context = context, message = "Можно изменить в настройках")
                                coroutineScope.launch {
                                    // Больше не спрашиваем перед сохраненением
                                    settings = settings.copy(showCheckValuesFlag = !it)
                                    SettingsManager.saveSettings(settings)
                                }
                            }
                        )
                        Text("Больше не показывать это окно")
                    }
                }
            )
        }

        if (showLeaveValueDialog) {// Диалог: Оставлять значения после сохранения
            AlertDialog(
                onDismissRequest = { showLeaveValueDialog = false }, // убираем диалог если нажали мимо
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            settings = settings.copy(isSaveValue = true)
                            SettingsManager.saveSettings(settings)
                        }
                        showLeaveValueDialog = false
                        coroutineScope.launch {
                            saveViewModel.save(
                                selectedTemplate = selectedTemplate,
                                fieldValues = fieldViewModel.fieldValues,
                                nameForDev = selectedTemplate.nameForDevelop,
                                defaultFileName = selectedTemplate.nameForUser + ".docx",
                                templateId = templateId,
                            )
                        } // Сохраняем
                    }) { Text("Оставить") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showLeaveValueDialog = false
                        coroutineScope.launch {
                            saveViewModel.save(
                                selectedTemplate = selectedTemplate,
                                fieldValues = fieldViewModel.fieldValues,
                                nameForDev = selectedTemplate.nameForDevelop,
                                defaultFileName = selectedTemplate.nameForUser + ".docx",
                                templateId = templateId,
                            )
                        } // Сохраняем
                        coroutineScope.launch {
                            settings = settings.copy(isSaveValue = false)
                            SettingsManager.saveSettings(settings)
                            delay(2000)// Через 2 секунды удаляем введенные занчения, если файл сохранен
                            if (saveViewModel.isFileSaved) {
                                fieldViewModel.clearValues()
                            }
                        }
                    }) { Text("Убрать") }
                },
                title = { Text("Оставить введенные значения после сохранения файла") },
                text = {
                    Row {
                        Checkbox(
                            checked = !showLeaveValueAlertFlagSetting,
                            onCheckedChange = { // если галочка стоит
                                //showMessage(context = context, message = "Можно изменить в настройках")
                                coroutineScope.launch {
                                    // Больше не спрашиваем перед сохраненением
                                    settings = settings.copy(showLeaveValueAlertFlag = !it)
                                    SettingsManager.saveSettings(settings)
                                }
                            }
                        )
                        Text("Больше не показывать это окно")
                    }
                }
            )
        }
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 110.dp, bottom = 80.dp)
                .imePadding()
        )
        { // Используем Column для вертикального расположения полей
            // Проходим по всем полям выбранного шаблона
            items(selectedTemplate.fields) { field ->
                // В зависимости от типа поля отображаем соответствующий компонент
                // Текстовое поле для ввода
                TextField(
                    modifier = Modifier.padding(vertical = 10.dp),
                    value = fieldViewModel.fieldValues[field.key]
                        ?: "", // Получаем текущее значение или пустую строку
                    onValueChange = {
                        fieldViewModel.updateValue(key = field.key, value = it)
                    }, // Обновляем значение при изменении
                    label = { Text(field.label) } // Отображаем метку поля
                )
            }
        }
    }

    TopAppBar(
        title = { Text(text = selectedTemplate.nameForUser) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xff8192fe),
            titleContentColor =  Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = { Navigation.navigateTo(Screen.TemplatePickRoute) }) {
                Icon(
                    imageVector = AppIcons.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = {
            IconButton(onClick = {
                if (!fieldViewModel.fieldValues.isFull() && showCheckValuesFlagSetting) showCheckValuesDialog = true
                else {
                    if (showLeaveValueAlertFlagSetting) { // Проверяем включено ли в настройках отбражение диалога
                        showLeaveValueDialog = true // включаем диалог
                    } else {
                        if (isSaveValue) {
                            coroutineScope.launch {
                                saveViewModel.save(
                                    selectedTemplate = selectedTemplate,
                                    fieldValues = fieldViewModel.fieldValues,
                                    nameForDev = selectedTemplate.nameForDevelop,
                                    defaultFileName = selectedTemplate.nameForUser + ".docx",
                                    templateId = templateId
                                )
                            }
                        } // Проверяем включено ли в настройках сохранение значений, сохраняем
                        else {

                            coroutineScope.launch {
                                saveViewModel.save(
                                    selectedTemplate = selectedTemplate,
                                    fieldValues = fieldViewModel.fieldValues,
                                    nameForDev = selectedTemplate.nameForDevelop,
                                    defaultFileName = selectedTemplate.nameForUser + ".docx",
                                    templateId = templateId,
                                ) // Сохраняем
                                delay(2000) // Через 2 секунды удаляем введенные занчения, если файл сохранен
                                if (saveViewModel.isFileSaved) {
                                    fieldViewModel.clearValues()
                                }
                            }
                        }
                    }
                }
            }) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null
                )
            }
        }
    )
}
