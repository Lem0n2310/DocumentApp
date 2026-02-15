package org.example.documenteditordesktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.documenteditor.ClassesViewModels.templates
import com.example.documenteditor.ComposeFun.MainScreen
import com.example.documenteditor.ComposeFun.SettingsScreen
import com.example.documenteditor.ComposeFun.TemplatePicker
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ComposeFun.TemplateEditorScreen
import org.example.documenteditordesktop.ComposeFun.TemplateInput
import org.example.documenteditordesktop.ComposeFunpackage.RecentDocs
import org.example.documenteditordesktop.functions.checkFolderExists
import org.example.documenteditordesktop.functions.createFolder
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "documenteditordesktop",
    ) {
        LaunchedEffect(Unit) {
            // Создаем необходимые папки
            val baseFolder = "DocumentEditor"
            if (!checkFolderExists(baseFolder)) {
                createFolder(baseFolder)
                createFolder("$baseFolder/DataBase")
                createFolder("$baseFolder/Templates")
                println("Папки созданы успешно")
            } else {
                // Проверяем и создаем подпапки если нужно
                if (!checkFolderExists("$baseFolder/DataBase")) {
                    createFolder("$baseFolder/DataBase")
                }
                if (!checkFolderExists("$baseFolder/Templates")) {
                    createFolder("$baseFolder/Templates")
                }
                println("Папки уже существуют")
            }
        }

        val currentScreen by Navigation.currentScreen.collectAsState()
        var selectedFile by remember { mutableStateOf<File?>(null) }

        when (currentScreen) {
            is Screen.MainScreenRoute -> MainScreen()
            is Screen.SettingsScreenRoute -> SettingsScreen()
            is Screen.TemplatePickRoute -> TemplatePicker()
            is Screen.TemplateInputRoute -> {
                val templateInputRoute = currentScreen as Screen.TemplateInputRoute
                if(templateInputRoute.dict.isNullOrEmpty()) {
                    TemplateInput(
                        templates = templates,
                        templateId = templateInputRoute.templateId,
                        dict = null
                    )
                }else{
                    TemplateInput(
                        templates = templates,
                        templateId = templateInputRoute.templateId,
                        dict = templateInputRoute.dict
                    )
                }
            }
            is Screen.RecentDocsRoute -> RecentDocs()
            is Screen.NewTemplateScreenRoute -> {
                val newTemplateScreenRoute = currentScreen as Screen.NewTemplateScreenRoute
                val file = newTemplateScreenRoute.file
                TemplateEditorScreen(file)
            }
        }
    }
}