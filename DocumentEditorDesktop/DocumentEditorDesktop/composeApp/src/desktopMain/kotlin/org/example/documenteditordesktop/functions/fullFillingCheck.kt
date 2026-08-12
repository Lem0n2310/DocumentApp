package com.example.documenteditor.functions

import androidx.compose.runtime.snapshots.SnapshotStateMap

/** Проверка полноты заполненных значений (пустая map ≠ «всё заполнено»). */
fun SnapshotStateMap<String, String>.isFull(): Boolean {
    if (isEmpty()) return false
    for (value in values) {
        if (value.isBlank()) return false
    }
    return true
}
