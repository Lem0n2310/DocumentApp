package com.example.documenteditor.ClassesViewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class FieldValuesViewModel {
    var forFlag by mutableStateOf(true)
    val fieldValues =  mutableStateMapOf<String, String>()

    fun updateValue(key: String, value: String){ // Обновление данных
        if (key.isEmpty()) return
        fieldValues[key] = value
    }

    fun clearValues(){
        // Оставляем ключи с пустыми значениями — иначе isFull() на пустой map
        // считает форму заполненной и позволяет сохранить {{KEY}} как есть.
        fieldValues.keys.toList().forEach { key ->
            fieldValues[key] = ""
        }
    }
}