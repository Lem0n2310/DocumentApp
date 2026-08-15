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
    data class TemplateInputRoute(
        val templateId: Int,
        val nameForDev: String,
        val dict: Map<String, String>? = null,
        val returnTo: Screen? = null,
        val caseId: Int? = null
    ) : Screen()
    object TemplatePickRoute: Screen()
    object RecentDocsRoute: Screen()
    data class NewTemplateScreenRoute(val file: File): Screen()
    data class CaseScreenRoute(val caseId: Int? = null): Screen()
}

object Navigation{
    private val previousCurrentScreen = MutableStateFlow<Screen>(Screen.MainScreenRoute)

    val currentScreen: StateFlow<Screen> = previousCurrentScreen

    fun navigateTo(newScreen: Screen){
        previousCurrentScreen.value = newScreen
    }

}