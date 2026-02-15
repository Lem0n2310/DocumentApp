package org.example.documenteditordesktop.ComposeFunpackage

import androidx.compose.foundation.background
import org.example.documenteditordesktop.ClassesViewModels.Manager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import org.example.documenteditordesktop.ClassesViewModels.AppIcons
import org.example.documenteditordesktop.ClassesViewModels.Navigation
import org.example.documenteditordesktop.ClassesViewModels.Screen
import org.example.documenteditordesktop.ClassesViewModels.RecentDocument
import java.awt.Desktop
import java.io.File


@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun RecentDocs(){
    val manager = remember { Manager<RecentDocument>(RecentDocument::class.java) }
    var recentDocs by remember { mutableStateOf(manager.loadJson()) }
    val isHover: MutableMap<RecentDocument, Boolean> = remember { mutableStateMapOf<RecentDocument, Boolean>().apply {
        recentDocs.forEach { document -> this[document] = false }
    } }

    val showOpenVarianse: MutableMap<RecentDocument, Boolean> = remember { mutableStateMapOf<RecentDocument, Boolean>().apply {
        recentDocs.forEach { document -> this[document] = false }
    } }


    Box(modifier = Modifier.fillMaxSize()){
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
                        Column{
                            Button(
                                onClick = { showOpenVarianse[document]?.let { showOpenVarianse[document] = !it } },
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .width(400.dp)
                                    .height(50.dp)
                                    .onPointerEvent(PointerEventType.Enter){isHover[document] = true}
                                    .onPointerEvent(PointerEventType.Exit){isHover[document] = false},
                            ) {
                                Text(document.name)

                                if(isHover[document] == true){
                                    TextButton(
                                        onClick = {
                                            manager.deleteDocument(document.name)
                                            recentDocs = manager.loadJson()
                                        },
                                        modifier = Modifier.onPointerEvent(PointerEventType.Enter){isHover[document] = true}
                                            .onPointerEvent(PointerEventType.Exit){isHover[document] = false},
                                    ){
                                        Text("delete")
                                    }
                                }
                            }

                            if(showOpenVarianse[document] == true){
                                Column{
                                    TextButton(
                                        onClick = {
                                            val file = File(document.path)
                                            if (Desktop.isDesktopSupported() && file.exists()){
                                                Desktop.getDesktop().open(file.parentFile)
                                            }
                                            else{
                                                println("Файла по этому пути не существует, или он не поддержиавается")
                                            }
                                        },
                                        modifier = Modifier.onPointerEvent(PointerEventType.Enter){isHover[document] = true}
                                            .onPointerEvent(PointerEventType.Exit){isHover[document] = false},
                                    ){
                                        Text("Открыть с помощью проводника")
                                    }

                                    TextButton(
                                        onClick = {
                                            Navigation.navigateTo(Screen.TemplateInputRoute(document.templateId, document.nameForDev, dict = document.dict))
                                        },
                                    ){
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
        title = { Text("Настройки") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xff8192fe),
            titleContentColor =  Color.White,
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
            TextButton(
                onClick = {
                    manager.clear()
                    recentDocs = manager.loadJson()
                },
            ) {
                Text("delete all")
            }
        }
    )

}

/*
@Composable
fun dialogs() {
    var dialogVisible = true
    if (dialogVisible) {
        AlertDialog(
            onDismissRequest = {
                dialogVisible = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        dialogVisible = false
                    }
                ) {
                    Text("Ок")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dialogVisible = false
                    }
                ) {
                    Text("Ура!")
                }
            },
            title = { Text("Документик!") },
            text = { Text("Как вам?") }
        )
    }
}

 */
