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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.documenteditor.ClassesViewModels.SaveViewModel
import kotlinx.coroutines.launch
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.DocumentCase
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.Manager
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.RecentDocument
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.TemplatePortable
import org.example.documenteditordesktop.ComposeFun.CatalogCardBorder
import org.example.documenteditordesktop.ComposeFun.CatalogHeader
import org.example.documenteditordesktop.ComposeFun.CatalogPreviewMinSize
import org.example.documenteditordesktop.ComposeFun.CatalogPreviewWidth
import org.example.documenteditordesktop.ComposeFun.ListSort
import org.example.documenteditordesktop.ComposeFun.TemplatePreviewCard
import org.example.documenteditordesktop.ComposeFun.isAppFullScreen
import org.example.documenteditordesktop.ComposeFun.searchAndSort
import org.example.documenteditordesktop.functions.convertDocToDocx
import org.example.documenteditordesktop.functions.duplicateCase
import java.awt.Desktop
import java.io.File

private val SidebarColor = Color(0xff8192fe)
private val MainBackground = Color(0xff9DA7E8)
private val AddCellGray = Color(0xFFD6D6D6)
private val CardBorder = CatalogCardBorder

private enum class HomeSection { Templates, Cases }

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
    var expandedSection by remember { mutableStateOf<HomeSection?>(null) }
    var templateQuery by remember { mutableStateOf("") }
    var templateSort by remember { mutableStateOf(ListSort.Used) }
    var caseQuery by remember { mutableStateOf("") }
    var caseSort by remember { mutableStateOf(ListSort.Used) }
    val templateFolder = remember {
        File(System.getProperty("user.home"), "DocumentEditor/Templates")
    }
    val visibleTemplates = templates.searchAndSort(
        query = templateQuery,
        sort = templateSort,
        nameOf = { it.nameForUser },
        createdAt = { it.createdAt },
        lastUsedAt = { it.lastUsedAt },
        idOf = { it.id }
    )
    val visibleCases = cases.searchAndSort(
        query = caseQuery,
        sort = caseSort,
        nameOf = { it.name },
        createdAt = { it.createdAt },
        lastUsedAt = { it.lastUsedAt },
        idOf = { it.id }
    )

    fun markTemplateUsed(template: DocumentTemplate) {
        template.lastUsedAt = System.currentTimeMillis()
        templateManager.updateById(template.id, template)
    }

    fun markCaseUsed(documentCase: DocumentCase) {
        documentCase.lastUsedAt = System.currentTimeMillis()
        caseManager.updateById(documentCase.id, documentCase)
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
        val fullScreen = isAppFullScreen()
        val previewWidth = CatalogPreviewWidth
        val gridMinSize = CatalogPreviewMinSize
        val gridHSpace = if (fullScreen) 16.dp else 10.dp
        val gridVSpace = if (fullScreen) 18.dp else 12.dp

        @Composable
        fun TemplatesPane(modifier: Modifier, expanded: Boolean) {
            Column(modifier = modifier.fillMaxWidth()) {
                CatalogHeader(
                    title = "Шаблоны",
                    subtitle = "Выберите шаблон или добавьте новый",
                    query = templateQuery,
                    onQueryChange = { templateQuery = it },
                    sort = templateSort,
                    onSortChange = { templateSort = it },
                    expanded = expanded,
                    onToggleExpand = {
                        expandedSection = if (expanded) null else HomeSection.Templates
                    }
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
                    items(visibleTemplates, key = { "${it.id}#${System.identityHashCode(it)}" }) { template ->
                        TemplatePreviewCard(
                            template = template,
                            previewWidth = previewWidth,
                            onOpen = {
                                markTemplateUsed(template)
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
        }

        @Composable
        fun CasesPane(modifier: Modifier, expanded: Boolean) {
            Column(modifier = modifier.fillMaxWidth()) {
                CatalogHeader(
                    title = "Дела",
                    subtitle = "Объедините шаблоны и заполните общие данные один раз",
                    query = caseQuery,
                    onQueryChange = { caseQuery = it },
                    sort = caseSort,
                    onSortChange = { caseSort = it },
                    expanded = expanded,
                    onToggleExpand = {
                        expandedSection = if (expanded) null else HomeSection.Cases
                    }
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
                    items(visibleCases, key = { "${it.id}#${System.identityHashCode(it)}" }) { documentCase ->
                        CasePreviewCard(
                            documentCase = documentCase,
                            previewWidth = previewWidth,
                            onOpen = {
                                markCaseUsed(documentCase)
                                Navigation.navigateTo(Screen.CaseScreenRoute(documentCase.id))
                            },
                            onDuplicate = {
                                val copy = duplicateCase(documentCase, caseManager)
                                cases.add(copy)
                                Navigation.navigateTo(Screen.CaseScreenRoute(copy.id))
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

        if (expandedSection != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MainBackground)
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                when (expandedSection) {
                    HomeSection.Templates -> TemplatesPane(Modifier.fillMaxSize(), expanded = true)
                    HomeSection.Cases -> CasesPane(Modifier.fillMaxSize(), expanded = true)
                    null -> Unit
                }
            }
        } else {
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
                            items(recentDocs, key = { "${it.path}#${System.identityHashCode(it)}" }) { document ->
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
                    TemplatesPane(Modifier.weight(1f), expanded = false)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .height(1.dp)
                            .background(Color.Black.copy(alpha = 0.12f))
                    )
                    CasesPane(Modifier.weight(1f), expanded = false)
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
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
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
    onDuplicate: () -> Unit,
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
                        text = { Text("Дублировать") },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
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
