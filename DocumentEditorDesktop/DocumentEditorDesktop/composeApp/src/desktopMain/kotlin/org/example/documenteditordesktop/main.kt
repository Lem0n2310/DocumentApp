package org.example.documenteditordesktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.documenteditor.ClassesViewModels.templates
import com.example.documenteditor.ComposeFun.MainScreen
import com.example.documenteditor.ComposeFun.SettingsScreen
import com.example.documenteditor.ComposeFun.TemplatePickScreen
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ComposeFun.TemplateEditorScreen
import org.example.documenteditordesktop.ComposeFun.TemplateInput
import org.example.documenteditordesktop.ComposeFunpackage.RecentDocs
import org.example.documenteditordesktop.functions.checkFolderExists
import org.example.documenteditordesktop.functions.createFolder

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "documenteditordesktop",
    ) {
        LaunchedEffect(Unit) {
            // При старте создаем необходимые папки
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
            }
        }
        // текущий экран
        val currentScreen by Navigation.currentScreen.collectAsState()

        when (currentScreen) {
            is Screen.MainScreenRoute -> MainScreen() // Главный экран
            is Screen.SettingsScreenRoute -> SettingsScreen() // Экран настроек
            is Screen.TemplatePickRoute -> TemplatePickScreen() // Экран выбора шаблона
            is Screen.TemplateInputRoute -> {
                val templateInputRoute = currentScreen as Screen.TemplateInputRoute
                TemplateInput( // экран ввода данных
                    templates = templates,
                    templateId = templateInputRoute.templateId,
                    dict = templateInputRoute.dict
                )
            }
            is Screen.RecentDocsRoute -> RecentDocs() // экран с недавними документами
            is Screen.NewTemplateScreenRoute -> { // экран создания шаблона
                val newTemplateScreenRoute = currentScreen as Screen.NewTemplateScreenRoute
                val file = newTemplateScreenRoute.file
                TemplateEditorScreen(file)
            }
        }
    }
}