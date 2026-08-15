package com.example.documenteditor.ComposeFun

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.documenteditor.ClassesViewModels.SaveViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.DocumentCase
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.Manager
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.RecentDocument
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.TemplatePortable
import org.example.documenteditordesktop.functions.convertDocToDocx
import java.awt.Desktop
import java.io.File
import java.io.FileInputStream

private val SidebarColor = Color(0xff8192fe)
private val MainBackground = Color(0xff9DA7E8)
private val AddCellGray = Color(0xFFD6D6D6)
private val CardBorder = Color(0xff8192fe)
private val FullscreenPreviewMinSize = 170.dp
private val WindowedPreviewWidth = 110.dp

@Composable
private fun isAppFullScreen(): Boolean {
    val windowSize = LocalWindowInfo.current.containerSize
    val screen = remember {
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
    }
    return windowSize.width >= (screen.width * 0.9).toInt() &&
        windowSize.height >= (screen.height * 0.9).toInt()
}

@Composable
fun MainScreen() {
    val saveViewModel = remember { SaveViewModel() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val templateManager = remember { Manager<DocumentTemplate>(DocumentTemplate::class.java) }
    val recentManager = remember { Manager<RecentDocument>(RecentDocument::class.java) }
    val caseManager = remember { Manager<DocumentCase>(DocumentCase::class.java) }
    val templates = remember {
        mutableStateListOf<DocumentTemplate>().apply { addAll(templateManager.loadJson()) }
    }
    val recentDocs = remember {
        mutableStateListOf<RecentDocument>().apply { addAll(recentManager.loadJson()) }
    }
    val cases = remember {
        mutableStateListOf<DocumentCase>().apply { addAll(caseManager.loadJson()) }
    }
    var showAddTemplateDialog by remember { mutableStateOf(false) }
    val templateFolder = remember {
        File(System.getProperty("user.home"), "DocumentEditor/Templates")
    }

    fun createTemplateManually() {
        saveViewModel.showOpenDialog(
            onFileSelected = { currentFile ->
                try {
                    val resultFile = if (currentFile.extension.equals("doc", ignoreCase = true)) {
                        convertDocToDocx(currentFile)
                    } else {
                        currentFile
                    }
                    Navigation.navigateTo(Screen.NewTemplateScreenRoute(resultFile))
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar(e.message ?: "Не удалось открыть файл")
                    }
                }
            }
        )
    }

    fun importTemplate() {
        saveViewModel.showOpenTemplatePackageDialog(
            onFileSelected = { packageFile ->
                TemplatePortable.importTemplate(packageFile)
                    .onSuccess { imported ->
                        templates.add(imported)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Шаблон «${imported.nameForUser}» готов к работе"
                            )
                        }
                    }
                    .onFailure { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                error.message ?: "Не удалось импортировать шаблон"
                            )
                        }
                    }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(SidebarColor)
            ) {
                Text(
                    text = "Недавние документы",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
                )

                if (recentDocs.isEmpty()) {
                    Text(
                        text = "Пока нет сохранённых документов",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(recentDocs, key = { it.path }) { document ->
                            RecentDocumentRow(
                                document = document,
                                onOpenInEditor = {
                                    Navigation.navigateTo(
                                        Screen.TemplateInputRoute(
                                            templateId = document.templateId,
                                            nameForDev = document.nameForDev,
                                            dict = document.dict
                                        )
                                    )
                                },
                                onOpenInExplorer = {
                                    val file = File(document.path)
                                    if (Desktop.isDesktopSupported() && file.exists()) {
                                        Desktop.getDesktop().open(file.parentFile)
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Файл не найден или проводник недоступен"
                                            )
                                        }
                                    }
                                },
                                onDelete = {
                                    recentManager.deleteDocument(document.path)
                                    recentDocs.remove(document)
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { Navigation.navigateTo(Screen.SettingsScreenRoute) }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.SettingsImage,
                        contentDescription = "Настройки",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Настройки", color = Color.White, fontSize = 15.sp)
                }
            }

            Column(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .background(MainBackground)
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                val fullScreen = isAppFullScreen()
                val previewWidth = if (fullScreen) null else WindowedPreviewWidth
                val gridMinSize = if (fullScreen) FullscreenPreviewMinSize else WindowedPreviewWidth
                val gridHSpace = if (fullScreen) 20.dp else 12.dp
                val gridVSpace = if (fullScreen) 24.dp else 14.dp

                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        text = "Шаблоны",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Выберите шаблон или добавьте новый",
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = gridMinSize),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(gridHSpace),
                        verticalArrangement = Arrangement.spacedBy(gridVSpace),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        item(key = "add-template") {
                            AddTemplateCell(
                                onClick = { showAddTemplateDialog = true },
                                previewWidth = previewWidth,
                                title = "Добавить шаблон",
                                caption = "Новый шаблон"
                            )
                        }
                        items(templates, key = { it.id }) { template ->
                            TemplatePreviewCard(
                                template = template,
                                previewWidth = previewWidth,
                                onOpen = {
                                    Navigation.navigateTo(
                                        Screen.TemplateInputRoute(
                                            templateId = template.id,
                                            nameForDev = template.nameForDevelop
                                        )
                                    )
                                },
                                onShare = {
                                    saveViewModel.showSaveTemplatePackageDialog(
                                        defaultFileName = template.nameForUser.ifBlank { template.nameForDevelop },
                                        onFileSelected = { dest ->
                                            TemplatePortable.exportTemplate(template, dest)
                                                .onSuccess { file ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            "Файл сохранён: ${file.name}"
                                                        )
                                                    }
                                                }
                                                .onFailure { error ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            error.message ?: "Не удалось экспортировать шаблон"
                                                        )
                                                    }
                                                }
                                        }
                                    )
                                },
                                onDelete = {
                                    File(templateFolder, "${template.nameForDevelop}.docx").delete()
                                    templateManager.deleteDocument(id = template.id)
                                    templates.remove(template)
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .height(1.dp)
                        .background(Color.Black.copy(alpha = 0.12f))
                )

                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        text = "Дела",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Объедините шаблоны и заполните общие данные один раз",
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = gridMinSize),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(gridHSpace),
                        verticalArrangement = Arrangement.spacedBy(gridVSpace),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        item(key = "add-case") {
                            AddTemplateCell(
                                onClick = { Navigation.navigateTo(Screen.CaseScreenRoute()) },
                                previewWidth = previewWidth,
                                title = "Добавить дело",
                                caption = "Новое дело"
                            )
                        }
                        items(cases, key = { it.id }) { documentCase ->
                            CasePreviewCard(
                                documentCase = documentCase,
                                previewWidth = previewWidth,
                                onOpen = {
                                    Navigation.navigateTo(Screen.CaseScreenRoute(documentCase.id))
                                },
                                onDelete = {
                                    caseManager.deleteDocument(id = documentCase.id)
                                    cases.remove(documentCase)
                                }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }

    if (showAddTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showAddTemplateDialog = false },
            title = { Text("Добавить шаблон") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            showAddTemplateDialog = false
                            createTemplateManually()
                        }
                    ) {
                        Text("Создать вручную")
                    }
                    TextButton(
                        onClick = {
                            showAddTemplateDialog = false
                            importTemplate()
                        }
                    ) {
                        Text("Импортировать")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddTemplateDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun RecentDocumentRow(
    document: RecentDocument,
    onOpenInEditor: () -> Unit,
    onOpenInExplorer: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenInEditor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.name,
                color = Color.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = document.path,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box {
            Text(
                text = "⋮",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp,
                modifier = Modifier
                    .clickable { menuExpanded = true }
                    .padding(start = 8.dp, end = 4.dp)
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Открыть в редакторе") },
                    onClick = {
                        menuExpanded = false
                        onOpenInEditor()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Открыть в проводнике") },
                    onClick = {
                        menuExpanded = false
                        onOpenInExplorer()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Удалить из списка") },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun AddTemplateCell(
    onClick: () -> Unit,
    previewWidth: Dp?,
    title: String,
    caption: String
) {
    val compact = previewWidth != null
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .then(if (previewWidth != null) Modifier.width(previewWidth) else Modifier.fillMaxWidth())
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(4.dp))
                .background(AddCellGray)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = Color(0xFF555555),
                fontSize = if (compact) 11.sp else 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = caption,
            fontSize = if (compact) 12.sp else 13.sp,
            color = Color.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CasePreviewCard(
    documentCase: DocumentCase,
    previewWidth: Dp?,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val cardWidthModifier =
        if (previewWidth != null) Modifier.width(previewWidth) else Modifier.fillMaxWidth()
    val templateCount = documentCase.templateIds.size
    val templatesLabel = when {
        templateCount % 10 == 1 && templateCount % 100 != 11 -> "$templateCount шаблон"
        templateCount % 10 in 2..4 && templateCount % 100 !in 12..14 -> "$templateCount шаблона"
        else -> "$templateCount шаблонов"
    }

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
                .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                .clickable(onClick = onOpen),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = documentCase.name.ifBlank { "Дело" },
                    fontSize = if (previewWidth != null) 12.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
                Text(
                    text = templatesLabel,
                    fontSize = if (previewWidth != null) 10.sp else 13.sp,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        Row(
            modifier = cardWidthModifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = documentCase.name.ifBlank { "Дело" },
                fontSize = 13.sp,
                color = Color.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
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

@Composable
private fun TemplatePreviewCard(
    template: DocumentTemplate,
    previewWidth: Dp?,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var preview by remember(template.nameForDevelop) { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }

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
                .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
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
                    DropdownMenuItem(
                        text = { Text("Поделиться") },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
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
