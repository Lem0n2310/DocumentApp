package org.example.documenteditordesktop.ClassesViewModels


import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File


/**
 * Все экраны, используемые в приложении
 */
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