package com.example.documenteditor.ComposeFun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.documenteditor.ClassesViewModels.SaveViewModel
import kotlinx.coroutines.launch
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.TemplatePortable
import org.example.documenteditordesktop.functions.convertDocToDocx
import java.io.File

@Composable
fun MainScreen() {
    val saveViewModel = remember { SaveViewModel() }
    var openCreateTemplate by remember { mutableStateOf(false) }
    var file: File? = null
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xff9DA7E8)),
        contentAlignment = Alignment.Center // Бокс на весь экран, контент по центру
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center // Бокс на весь экран, контент по центру
        ) {
            // Кнопки в колонке
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = { Navigation.navigateTo(Screen.TemplatePickRoute) },// to templatePick
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.Black,
                        containerColor = Color.White
                    )
                ) {
                    Text("Создать документ по шаблону")
                }

                Button(
                    { Navigation.navigateTo(Screen.RecentDocsRoute) },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.Black,
                        containerColor = Color.White
                    )
                ) {
                    Text("Недавние проекты")
                }

                Button(
                    onClick = {
                        saveViewModel.showOpenDialog(
                            onFileSelected = { currentFile ->
                                try {
                                    val resultFile = if (currentFile.extension.equals("doc", ignoreCase = true)) {
                                        convertDocToDocx(currentFile)
                                    } else {
                                        currentFile
                                    }
                                    file = resultFile
                                    openCreateTemplate = true
                                    Navigation.navigateTo(Screen.NewTemplateScreenRoute(resultFile))
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            e.message ?: "Не удалось открыть файл"
                                        )
                                    }
                                }
                            },
                            onCancel = {
                                println("Открытие файла отменено")
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.Black,
                        containerColor = Color.White
                    )
                ) {
                    Text("Создать шаблон")
                }

                Button(
                    onClick = {
                        saveViewModel.showOpenTemplatePackageDialog(
                            onFileSelected = { packageFile ->
                                TemplatePortable.importTemplate(packageFile)
                                    .onSuccess { imported ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Шаблон «${imported.nameForUser}» готов к работе"
                                            )
                                        }
                                        Navigation.navigateTo(Screen.TemplatePickRoute)
                                    }
                                    .onFailure { error ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                error.message ?: "Не удалось импортировать шаблон"
                                            )
                                        }
                                    }
                            },
                            onCancel = {
                                println("Импорт шаблона отменён")
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.Black,
                        containerColor = Color.White
                    )
                ) {
                    Text("Импортировать шаблон")
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }

    Box(modifier = Modifier.padding(start = 25.dp, top = 25.dp)) {
        IconButton(
            onClick = { Navigation.navigateTo(Screen.SettingsScreenRoute) },
            content = { Icon(imageVector = AppIcons.SettingsImage, "Меню") },
        )
    }
}

