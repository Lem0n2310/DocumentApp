package org.example.documenteditordesktop.ComposeFun

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import java.io.File
import java.io.FileInputStream

val CatalogCardBorder = Color(0xff8192fe)
val CatalogPreviewWidth = 86.dp
val CatalogPreviewMinSize = 98.dp

@Composable
fun isAppFullScreen(): Boolean {
    val windowSize = LocalWindowInfo.current.containerSize
    val screen = remember {
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    }
    return windowSize.width >= (screen.width * 0.9).toInt() &&
        windowSize.height >= (screen.height * 0.9).toInt()
}

enum class ListSort(val label: String) {
    Used("По использованию"),
    Created("По созданию"),
    Name("По названию")
}

fun <T> List<T>.searchAndSort(
    query: String,
    sort: ListSort,
    nameOf: (T) -> String,
    createdAt: (T) -> Long,
    lastUsedAt: (T) -> Long,
    idOf: (T) -> Int
): List<T> {
    val needle = query.trim().lowercase()
    val filtered = if (needle.isEmpty()) {
        this
    } else {
        filter { nameOf(it).lowercase().contains(needle) }
    }
    return when (sort) {
        ListSort.Name -> filtered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, nameOf))
        ListSort.Created -> filtered.sortedByDescending { item ->
            createdAt(item).takeIf { it > 0L } ?: idOf(item).toLong()
        }
        ListSort.Used -> filtered.sortedByDescending { item ->
            lastUsedAt(item).takeIf { it > 0L } ?: createdAt(item)
        }
    }
}

@Composable
fun CatalogHeader(
    title: String,
    subtitle: String,
    query: String,
    onQueryChange: (String) -> Unit,
    sort: ListSort,
    onSortChange: (ListSort) -> Unit,
    expanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (onToggleExpand != null) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (expanded) AppIcons.ArrowBack else AppIcons.PlusImage,
                        contentDescription = if (expanded) "Свернуть" else "На весь экран",
                        tint = Color.Black
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CatalogSearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.weight(1f)
            )
            CatalogSortMenu(sort = sort, onSortChange = onSortChange)
        }
    }
}

@Composable
private fun CatalogSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp),
        cursorBrush = SolidColor(Color.Black),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        decorationBox = { innerTextField ->
            Box {
                if (query.isEmpty()) {
                    Text("Поиск", color = Color.Black.copy(alpha = 0.4f), fontSize = 14.sp)
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun CatalogSortMenu(
    sort: ListSort,
    onSortChange: (ListSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(sort.label, color = Color.Black)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ListSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TemplatePreviewCard(
    template: DocumentTemplate,
    previewWidth: Dp?,
    onOpen: () -> Unit,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    selected: Boolean = false,
    statusText: String? = null,
    statusFilled: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    var preview by remember(template.nameForDevelop) { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    val hasMenu = onShare != null || onDelete != null

    LaunchedEffect(template.nameForDevelop) {
        preview = withContext(Dispatchers.IO) {
            loadTemplatePreview(template.nameForDevelop)
        }
    }

    val cardWidthModifier =
        if (previewWidth != null) Modifier.width(previewWidth) else Modifier.fillMaxWidth()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = cardWidthModifier
                .aspectRatio(0.72f)
                .shadow(3.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = CatalogCardBorder,
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable(onClick = onOpen)
        ) {
            Text(
                text = preview.ifBlank { template.nameForUser },
                fontSize = if (previewWidth != null) 7.sp else 8.sp,
                lineHeight = if (previewWidth != null) 9.sp else 11.sp,
                color = Color(0xFF4A4A4A),
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(if (previewWidth != null) 6.dp else 10.dp)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(CatalogCardBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = "Выбран",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Row(
            modifier = cardWidthModifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = template.nameForUser,
                fontSize = 13.sp,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (hasMenu) {
                Box {
                    Text(
                        text = "⋮",
                        fontSize = 16.sp,
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable { menuExpanded = true }
                            .padding(start = 4.dp)
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (onShare != null) {
                            DropdownMenuItem(
                                text = { Text("Поделиться") },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Удалить") },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
        if (!statusText.isNullOrBlank()) {
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = if (statusFilled) Color(0xFF2E7D32) else Color.Black.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = cardWidthModifier.padding(top = 2.dp)
            )
        }
        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = cardWidthModifier.height(32.dp)
            ) {
                Text(actionLabel)
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

private fun loadTemplatePreview(nameForDevelop: String): String {
    val file = File(System.getProperty("user.home"), "DocumentEditor/Templates/$nameForDevelop.docx")
    if (!file.exists()) return ""
    return runCatching {
        FileInputStream(file).use { stream ->
            XWPFDocument(stream).use { document ->
                buildString {
                    for (paragraph in document.paragraphs) {
                        val text = paragraph.text.trim()
                        if (text.isEmpty()) continue
                        if (isNotEmpty()) append('\n')
                        append(text)
                        if (length > 320) break
                    }
                }.take(320)
            }
        }
    }.getOrDefault("")
}
