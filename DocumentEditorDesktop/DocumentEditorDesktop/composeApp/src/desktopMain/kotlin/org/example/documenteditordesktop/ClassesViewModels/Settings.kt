package org.example.documenteditordesktop.ClassesViewModels

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AppSettings(
    val isSaveValue: Boolean = true,
    val showLeaveValueAlertFlag: Boolean = true,
    val showCheckValuesFlag: Boolean = true
)

object SettingsManager{
    private val json = Json{prettyPrint = true}

    private val settingsDir = File(System.getProperty("user.home"), "DocumentEditor/DataBase")
    private val settingsFile = File(settingsDir, "Settings.json")

    fun saveSettings(settings: AppSettings){
        val jsonString = json.encodeToString(settings)
        settingsFile.writeText(jsonString)
    }

    fun loadSettings(): AppSettings {
        return if (settingsFile.exists()){
            val jsonString = settingsFile.readText()
            json.decodeFromString<AppSettings>(jsonString)
        } else {
            createSettings()
        }
    }

    private fun createSettings(): AppSettings {
        val defaultSettings = AppSettings()
        val jsonString = json.encodeToString(defaultSettings)
        settingsFile.writeText(jsonString)
        return defaultSettings
    }
}
