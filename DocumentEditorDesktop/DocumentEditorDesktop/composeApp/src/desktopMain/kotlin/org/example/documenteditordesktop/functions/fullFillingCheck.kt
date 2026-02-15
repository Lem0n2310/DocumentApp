package com.example.documenteditor.functions

import androidx.compose.runtime.snapshots.SnapshotStateMap
// Проверка полноты заполненных значений
fun SnapshotStateMap<String, String>.isFull(): Boolean {
    for(value in this.values){
        if(value == "") return false
    }
    return true
}
