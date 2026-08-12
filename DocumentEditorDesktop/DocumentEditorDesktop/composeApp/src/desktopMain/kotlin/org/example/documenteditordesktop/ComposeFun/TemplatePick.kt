package com.example.documenteditor.ComposeFun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.documenteditor.ClassesViewModels.SaveViewModel
import kotlinx.coroutines.launch
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.Manager
import org.example.documenteditordesktop.ClassesViewModels.TemplatePortable
import java.io.File


// Выбор шаблона
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TemplatePickScreen() { // Получаем список шаблонов и нав
    val manager = Manager<DocumentTemplate>(DocumentTemplate::class.java)
    // Делаем список шаблонов реактивным, чтобы UI обновлялся при удалении
    val templates = remember {
        mutableStateListOf<DocumentTemplate>().apply {
            addAll(manager.loadJson())
        }
    }
    var deleteFlag by remember { mutableStateOf(false) }
    val backgroundColor = remember { Color(0xff9DA7E8) }
    val templateFolder by remember { mutableStateOf(File(System.getProperty("user.home"), "DocumentEditor/Templates")) }
    val templatesToDelete = remember { mutableStateListOf<DocumentTemplate>() }
    val saveViewModel = remember { SaveViewModel() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                // Перебираем все шаблоны из templates и под каждого создаем свою кнопку
                templates.forEach { template ->
                    var checked by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (!deleteFlag) {
                                Navigation.navigateTo(
                                    Screen.TemplateInputRoute(
                                        templateId = template.id,
                                        nameForDev = template.nameForDevelop
                                    )
                                )
                                println("name for dev: ${template.nameForDevelop}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = Color.Black,
                            containerColor = Color.White
                        ),
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .height(50.dp)
                    )
                    {
                        if (deleteFlag) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    checked = it
                                    if (it) {
                                        templatesToDelete.add(template)
                                    } else {
                                        templatesToDelete.remove(template)
                                    }
                                }
                            )
                        }
                        Text(template.nameForUser)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }


    // Снэек бар с подписью местонахождения и кнопкой назад
    TopAppBar(
        title = { Text("Выбор шаблона") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xff8192fe),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = { Navigation.navigateTo(Screen.MainScreenRoute) }) {
                Icon(
                    imageVector = AppIcons.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = {
            if (templatesToDelete.isEmpty()) {
                TextButton(
                    onClick = {
                        saveViewModel.showOpenTemplatePackageDialog(
                            onFileSelected = { packageFile ->
                                TemplatePortable.importTemplate(packageFile)
                                    .onSuccess { imported ->
                                        templates.add(imported)
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Шаблон «${imported.nameForUser}» импортирован"
                                            )
                                        }
                                    }
                                    .onFailure { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                error.message ?: "Не удалось импортировать шаблон"
                                            )
                                        }
                                    }
                            }
                        )
                    }
                ) {
                    Text("Импорт", color = Color.White)
                }
                TextButton(
                    onClick = {
                        deleteFlag = !deleteFlag
                    }
                ) {
                    Text(if (deleteFlag) "Готово" else "Изменить", color = Color.White)
                }
            } else {
                TextButton(
                    onClick = {
                        val selected = templatesToDelete.toList()
                        if (selected.size != 1) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Выберите один шаблон, чтобы поделиться"
                                )
                            }
                            return@TextButton
                        }
                        val template = selected.first()
                        saveViewModel.showSaveTemplatePackageDialog(
                            defaultFileName = template.nameForUser.ifBlank { template.nameForDevelop },
                            onFileSelected = { dest ->
                                TemplatePortable.exportTemplate(template, dest)
                                    .onSuccess { file ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Файл сохранён: ${file.name}. Перенесите его на другое устройство и нажмите «Импорт»."
                                            )
                                        }
                                    }
                                    .onFailure { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                error.message ?: "Не удалось экспортировать шаблон"
                                            )
                                        }
                                    }
                            }
                        )
                    }
                ) {
                    Text("Поделиться", color = Color.White)
                }
                IconButton(
                    onClick = {
                        for (template in templatesToDelete) {
                            // удаляем файл
                            val file = File(templateFolder, "${template.nameForDevelop}.docx")
                            file.delete()
                            manager.deleteDocument(id = template.id)
                            // Удаляем шаблон из локального списка, чтобы кнопка пропала с экрана
                            templates.remove(template)
                        }
                        templatesToDelete.clear()
                    },
                ) {
                    Icon(
                        imageVector = AppIcons.DeleteImage,
                        contentDescription = null
                    )
                }
            }
        }
    )
}
