package com.example.documenteditor.ComposeFun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.Manager
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(title: String, screenToNavigate: Screen, imageVector: ImageVector, actions: @Composable () -> Unit = {}){
    TopAppBar(
        title = { Text(title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xff8192fe),
            titleContentColor =  Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = { Navigation.navigateTo(screenToNavigate) }) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null
                )
            }
        },
        actions = {
            actions
        }
    )
}


// Выбор шаблона
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TemplatePicker() { // Получаем список шаблонов и нав
    val manager = Manager<DocumentTemplate>(DocumentTemplate::class.java)
    // Делаем список шаблонов реактивным, чтобы UI обновлялся при удалении
    val templates = remember {
        mutableStateListOf<DocumentTemplate>().apply {
            addAll(manager.loadJson())
        }
    }
    // Режим удаления (как на iOS: выбрать и подтвердить)
    var isDeleteMode by remember { mutableStateOf(false) }
    val selectedTemplates = remember {
        mutableStateMapOf<DocumentTemplate, Boolean>().apply {
            templates.forEach { template -> this[template] = false }
        }
    }
    val backgroundColor = remember { Color(0xff9DA7E8) }
    val templateFolder by remember { mutableStateOf(File(System.getProperty("user.home"),"DocumentEditor/Templates")) }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        // ленивая колонка на весь экран
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            item {
                // Перебираем все шаблоны из templates и под каждого создаем свою строку
                templates.forEach { template ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        if (isDeleteMode) {
                            Checkbox(
                                checked = selectedTemplates[template] == true,
                                onCheckedChange = { checked ->
                                    selectedTemplates[template] = checked
                                }
                            )
                        }

                        Button(
                            onClick = {
                                if (!isDeleteMode) {
                                    Navigation.navigateTo(
                                        Screen.TemplateInputRoute(
                                            templateId = template.id,
                                            nameForDev = template.nameForDevelop
                                        )
                                    )
                                    println("name for dev: ${template.nameForDevelop}")
                                }
                            },
                            enabled = !isDeleteMode,
                            colors = ButtonDefaults.buttonColors(
                                contentColor = Color.Black,
                                containerColor = Color.White
                            ),
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .height(50.dp)
                        ) {
                            Text(template.nameForUser)
                        }
                    }
                }
            }
        }
    }

    // Снэек бар с подписью местонахождения и кнопкой назад
    MyTopAppBar(
        title = "Выбор шаблона",
        screenToNavigate = Screen.MainScreenRoute,
        imageVector = AppIcons.ArrowBack,
        actions = {
            if (isDeleteMode) {
                TextButton(onClick = {
                    // Отмена режима удаления
                    isDeleteMode = false
                    selectedTemplates.keys.forEach { selectedTemplates[it] = false }
                }) {
                    Text("Отмена")
                }
                TextButton(onClick = {
                    // Подтверждение удаления выбранных шаблонов
                    val toDelete = selectedTemplates.filterValues { it }.keys.toList()
                    toDelete.forEach { template ->
                        val file = File(templateFolder, "${template.nameForDevelop}.docx")
                        if (file.exists()) {
                            file.delete()
                        }
                        manager.deleteDocument(id = template.id)
                        templates.remove(template)
                        selectedTemplates.remove(template)
                    }
                    isDeleteMode = false
                }) {
                    Text("Удалить")
                }
            } else {
                TextButton(onClick = { isDeleteMode = true }) {
                    Text("Удалить")
                }
            }
        }
    )
}


