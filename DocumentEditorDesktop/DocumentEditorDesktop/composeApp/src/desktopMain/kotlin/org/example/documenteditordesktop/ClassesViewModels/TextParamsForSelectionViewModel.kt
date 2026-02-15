package org.example.documenteditordesktop.ClassesViewModels;

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TextParamsForSelectionViewModel {
    var fullText by mutableStateOf("")
    val fragments = mutableStateMapOf<Int, String>()
}
