package com.example.documenteditor.ComposeFun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.documenteditor.functions.textToShow
import kotlinx.coroutines.launch
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var settings by remember { mutableStateOf(SettingsManager.loadSettings()) }
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false)} // флаг отображения нижнего меню
    val isSaveValue = settings.isSaveValue // Сохранять значения внутри полей
    val showLeaveValueAlertFlagSetting = settings.showLeaveValueAlertFlag // Показывать окно сохранения введенных значений (для отображения вообще)
    val showCheckValuesFlagSetting = settings.showCheckValuesFlag // Показывть окно проверки заполнения данных (вообще)

    val coroutineScope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize().background(Color(0xff9DA7E8))) {
        Column(
            Modifier.padding(vertical = 70.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.Top
        ) {
            Row(
                Modifier //настройка проверки значений
                .padding(vertical = 8.dp)
                .fillMaxWidth()
                .clickable(
                    onClick = {
                        coroutineScope.launch {
                            settings = settings.copy(showCheckValuesFlag = !showCheckValuesFlagSetting)
                            SettingsManager.saveSettings(settings)
                        }
                    }
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
                ){
                Checkbox(
                    checked = showCheckValuesFlagSetting,
                    onCheckedChange = {
                        coroutineScope.launch {
                            settings = settings.copy(showCheckValuesFlag = it)
                            SettingsManager.saveSettings(settings)
                        }
                    }
                )
                Text("Проверять заполнение всех данных перед сохранением?", modifier = Modifier.padding(2.dp))
            }

            Row(
                Modifier
                    .padding(vertical = 8.dp)
                    .clickable(onClick = { showBottomSheet = true })
            ) {
                Text("Оставить данные после сохранения файла", modifier = Modifier.padding(2.dp)) // Настройка сохранения значений
                Text(
                    textToShow(showLeaveValueAlertFlagSetting, isSaveValue),
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
        }

        TopAppBar(
            title = "Настройки",
            screenToNavigate = Screen.MainScreenRoute,
            imageVector = AppIcons.ArrowBack,
        )

        if (showBottomSheet) {
            ModalBottomSheet( // менюшка настроек
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("Данные после сохранения файла")

                    TextButton(onClick = {
                        coroutineScope.launch {
                            settings = settings.copy(showLeaveValueAlertFlag = true)
                            SettingsManager.saveSettings(settings)
                            showBottomSheet = false
                        }
                    }) { Text("Спрашивать") }
                    TextButton(onClick = {
                        coroutineScope.launch {
                            settings = settings.copy(isSaveValue = true)
                            SettingsManager.saveSettings(settings)
                            settings = settings.copy(showLeaveValueAlertFlag = false)
                            SettingsManager.saveSettings(settings)
                            showBottomSheet = false
                        }
                    }) { Text("Сохранять") }
                    TextButton(onClick = {
                        coroutineScope.launch {
                            settings = settings.copy(isSaveValue = false)
                            SettingsManager.saveSettings(settings)
                            settings = settings.copy(showLeaveValueAlertFlag = false)
                            SettingsManager.saveSettings(settings)
                            showBottomSheet = false
                        }
                    }) { Text("Не сохранять") }
                }
            }
        }
    }
}

