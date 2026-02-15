package org.example.documenteditordesktop.ClassesViewModels

import com.example.documenteditor.ComposeFun.MainScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.File

sealed class Screen {
    object MainScreenRoute : Screen()
    object SettingsScreenRoute: Screen()
    data class TemplateInputRoute(val templateId: Int, val nameForDev: String, val dict: Map<String, String>? = null) : Screen()
    object TemplatePickRoute: Screen()
    object RecentDocsRoute: Screen()
    data class NewTemplateScreenRoute(val file: File): Screen()
}

object Navigation{

    private val privCurrentScreen = MutableStateFlow<Screen>(Screen.MainScreenRoute)

    val currentScreen: StateFlow<Screen> = privCurrentScreen

    fun navigateTo(newScreen: Screen){
        privCurrentScreen.value = newScreen
    }

}