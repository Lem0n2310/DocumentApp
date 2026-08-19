package org.example.documenteditordesktop.ComposeFun

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.example.documenteditordesktop.ClassesViewModels.DocumentField
import org.example.documenteditordesktop.functions.fieldDisplayKey
import org.example.documenteditordesktop.functions.fieldLookupKeys
import org.example.documenteditordesktop.functions.unwrapPlaceholder
import org.example.documenteditordesktop.functions.valueForField
import org.example.documenteditordesktop.functions.wrapPlaceholder

private val PLACEHOLDER_REGEX = Regex("\\{\\{[^{}]+\\}\\}")

private data class FieldRange(
    val start: Int,
    val end: Int,
    val fieldIndex: Int,
)

private fun fieldIndexForPlaceholder(
    placeholder: String,
    fields: List<DocumentField>,
): Int {
    val wrapped = wrapPlaceholder(placeholder)
    val unwrapped = unwrapPlaceholder(placeholder)
    return fields.indexOfFirst { field ->
        fieldLookupKeys(field).any { key ->
            wrapPlaceholder(key).equals(wrapped, ignoreCase = true) ||
                unwrapPlaceholder(key).equals(unwrapped, ignoreCase = true)
        }
    }
}

private fun filledValueForField(
    index: Int,
    field: DocumentField,
    fieldValues: Map<String, String>,
): String {
    val mapKey = fieldDisplayKey(field, index)
    val fromMap = fieldValues[mapKey]
    if (!fromMap.isNullOrBlank()) return fromMap
    return valueForField(field, fieldValues).orEmpty()
}

private fun buildFilledPreview(
    original: String,
    fields: List<DocumentField>,
    fieldValues: Map<String, String>,
    selectedFieldIndex: Int?,
): Pair<AnnotatedString, List<FieldRange>> {
    val ranges = mutableListOf<FieldRange>()
    val annotated = buildAnnotatedString {
        var cursor = 0
        PLACEHOLDER_REGEX.findAll(original).forEach { match ->
            if (match.range.first > cursor) {
                append(original.substring(cursor, match.range.first))
            }
            val placeholder = match.value
            val fieldIndex = fieldIndexForPlaceholder(placeholder, fields)
            if (fieldIndex < 0) {
                append(placeholder)
            } else {
                val field = fields[fieldIndex]
                val value = filledValueForField(fieldIndex, field, fieldValues)
                val empty = value.isBlank()
                val display = if (empty) {
                    field.label.ifBlank { placeholder }
                } else {
                    value
                }
                val start = length
                val selected = selectedFieldIndex == fieldIndex
                withStyle(
                    SpanStyle(
                        background = colorForFieldIndex(fieldIndex).copy(
                            alpha = if (selected) 0.82f else 0.48f
                        ),
                        color = Color(0xFF1A1A1A),
                        fontStyle = if (empty) FontStyle.Italic else FontStyle.Normal,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                ) {
                    append(display)
                }
                ranges.add(FieldRange(start, length, fieldIndex))
            }
            cursor = match.range.last + 1
        }
        if (cursor < original.length) {
            append(original.substring(cursor))
        }
    }
    return annotated to ranges
}

@Composable
private fun PreviewText(
    original: String,
    fields: List<DocumentField>,
    fieldValues: Map<String, String>,
    selectedFieldIndex: Int?,
    onFieldClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (annotated, ranges) = buildFilledPreview(
        original,
        fields,
        fieldValues,
        selectedFieldIndex,
    )
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge,
        onTextLayout = { layout = it },
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(ranges) {
                detectTapGestures { offset ->
                    val layoutResult = layout ?: return@detectTapGestures
                    val pos = layoutResult.getOffsetForPosition(offset)
                    val hit = ranges.lastOrNull { pos in it.start until it.end }
                    if (hit != null) onFieldClick(hit.fieldIndex)
                }
            }
    )
}

@Composable
fun DocumentFillPreview(
    templateState: TemplateState,
    fields: List<DocumentField>,
    fieldValues: Map<String, String>,
    selectedFieldIndex: Int?,
    onFieldClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedFieldIndex) {
        val index = selectedFieldIndex ?: return@LaunchedEffect
        val itemIndex = templateState.elements.indexOfFirst { element ->
            val texts = when (element) {
                is TemplateElement.TextBlock -> listOf(element.text)
                is TemplateElement.Table -> element.rows.flatten()
            }
            texts.any { text ->
                PLACEHOLDER_REGEX.findAll(text).any { match ->
                    fieldIndexForPlaceholder(match.value, fields) == index
                }
            }
        }
        if (itemIndex >= 0) {
            listState.animateScrollToItem(itemIndex)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        items(templateState.elements, key = { element ->
            when (element) {
                is TemplateElement.TextBlock -> "text-${element.id}"
                is TemplateElement.Table -> "table-${element.id}"
            }
        }) { element ->
            when (element) {
                is TemplateElement.TextBlock -> {
                    val containsSelected = selectedFieldIndex != null &&
                        PLACEHOLDER_REGEX.findAll(element.text).any { match ->
                            fieldIndexForPlaceholder(match.value, fields) == selectedFieldIndex
                        }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                if (containsSelected) Color(0xFFE8EEFF) else Color.Transparent
                            )
                    ) {
                        PreviewText(
                            original = element.text,
                            fields = fields,
                            fieldValues = fieldValues,
                            selectedFieldIndex = selectedFieldIndex,
                            onFieldClick = onFieldClick,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                is TemplateElement.Table -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    ) {
                        element.rows.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                row.forEach { cell ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(4.dp)
                                            .border(1.dp, Color.Gray)
                                    ) {
                                        PreviewText(
                                            original = cell,
                                            fields = fields,
                                            fieldValues = fieldValues,
                                            selectedFieldIndex = selectedFieldIndex,
                                            onFieldClick = onFieldClick,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
