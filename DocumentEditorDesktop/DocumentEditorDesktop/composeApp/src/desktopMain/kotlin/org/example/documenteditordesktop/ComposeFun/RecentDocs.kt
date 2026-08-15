package org.example.documenteditordesktop.ComposeFunpackage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.Manager
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.RecentDocument
import org.example.documenteditordesktop.ClassesViewModels.Screen
import java.awt.Desktop
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentDocs() {
    val manager = remember { Manager<RecentDocument>(RecentDocument::class.java) }
    val recentDocs = remember {
        mutableStateListOf<RecentDocument>().apply {
            addAll(manager.loadJson())
        }
    }
    var deleteFlag by remember { mutableStateOf(false) }
    val docsToDelete = remember { mutableStateListOf<RecentDocument>() }
    val showOpenVarianse: MutableMap<RecentDocument, Boolean> = remember {
        mutableStateMapOf<RecentDocument, Boolean>().apply {
            recentDocs.forEach { document -> this[document] = false }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 50.dp)
                .background(Color(0xff9DA7E8))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    recentDocs.forEach { document ->
                        Column {
                            Button(
                                onClick = {
                                    if (!deleteFlag) {
                                        showOpenVarianse[document] = !(showOpenVarianse[document] ?: false)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    contentColor = Color.Black,
                                    containerColor = Color.White
                                ),
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .width(400.dp)
                                    .height(50.dp)
                            ) {
                                if (deleteFlag) {
                                    Checkbox(
                                        checked = document in docsToDelete,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                docsToDelete.add(document)
                                            } else {
                                                docsToDelete.remove(document)
                                            }
                                        }
                                    )
                                }
                                Text(document.name)
                            }

                            if (!deleteFlag && showOpenVarianse[document] == true) {
                                Column {
                                    TextButton(
                                        onClick = {
                                            val file = File(document.path)
                                            if (Desktop.isDesktopSupported() && file.exists()) {
                                                Desktop.getDesktop().open(file.parentFile)
                                            } else {
                                                println("Файла по этому пути не существует, или он не поддержиавается")
                                            }
                                        }
                                    ) {
                                        Text("Открыть с помощью проводника")
                                    }

                                    TextButton(
                                        onClick = {
                                            Navigation.navigateTo(
                                                Screen.TemplateInputRoute(
                                                    document.templateId,
                                                    document.nameForDev,
                                                    dict = document.dict
                                                )
                                            )
                                        }
                                    ) {
                                        Text("Открыть с помощью Редактора Документов")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text("Недавние документы") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xff8192fe),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        navigationIcon = {
            IconButton(onClick = { Navigation.navigateTo(Screen.MainScreenRoute) }) {
                Icon(
                    imageVector = AppIcons.ArrowBack,
                    contentDescription = null
                )
            }
        },
        actions = {
            if (docsToDelete.isEmpty()) {
                TextButton(
                    onClick = {
                        deleteFlag = !deleteFlag
                    }
                ) {
                    Text(if (deleteFlag) "Готово" else "Изменить", color = Color.White)
                }
            } else {
                IconButton(
                    onClick = {
                        for (document in docsToDelete) {
                            manager.deleteDocument(document.path)
                            recentDocs.remove(document)
                            showOpenVarianse.remove(document)
                        }
                        docsToDelete.clear()
                    }
                ) {
                    Icon(
                        imageVector = AppIcons.DeleteImage,
                        contentDescription = null
                    )
                }
            }
        }
    )
}



