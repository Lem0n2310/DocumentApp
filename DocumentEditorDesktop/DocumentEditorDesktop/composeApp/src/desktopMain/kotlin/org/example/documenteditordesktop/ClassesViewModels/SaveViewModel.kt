package com.example.documenteditor.ClassesViewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import documenteditordesktop.composeapp.generated.resources.Res
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.RecentDocument
import org.example.documenteditordesktop.ClassesViewModels.Manager
import org.example.documenteditordesktop.templatesFun.absoluteTemplate
import java.awt.FileDialog
import java.awt.Frame
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter

class SaveViewModel {
    var isFileSaved by  mutableStateOf(false)

    fun showOpenDialog(
        onFileSelected: (File) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        // Использование AWT FileDialog (более нативный вид)
        val dialog = FileDialog(null as Frame?, "Открыть файл", FileDialog.LOAD).apply {
            // Устанавливаем фильтр для DOCX файлов
            filenameFilter = FilenameFilter { _, name ->
                name.endsWith(".docx", ignoreCase = true)
            }
            isVisible = true
        }

        if (dialog.file != null) {
            val file = File(dialog.directory, dialog.file)
            onFileSelected(file)
        } else {
            onCancel()
        }
    }


    fun showSaveDialog(
        defaultFileName: String,
        onFileSelected: (File) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        // Вариант 1: Использование AWT FileDialog (более нативный вид)
        val dialog = FileDialog(null as Frame?, "Сохранить файл", FileDialog.SAVE).apply {
            file = defaultFileName
            isVisible = true
        }

        if (dialog.file != null) {
            val file = File(dialog.directory, dialog.file).let { file ->
                if (!file.name.endsWith(".docx")) File(file.parent, "${file.name}.docx") else file
            }
            onFileSelected(file)
        } else {
            onCancel()
        }
    }

    suspend fun save(
        selectedTemplate: DocumentTemplate,
        fieldValues: Map<String, String>,
        nameForDev: String,
        defaultFileName: String,
        templateId: Int
    ): Boolean {
        val manager = Manager<RecentDocument>(RecentDocument::class.java)
        val templateFolder = File(System.getProperty("user.home"),"DocumentEditor/Templates")
        val templateFile = File(templateFolder, "${nameForDev}.docx")
        val templateDocument = FileInputStream(templateFile).use{stream ->
            XWPFDocument(stream)
        }
        val document = absoluteTemplate(
            templateDocument,
            fieldValues
        )

        var isSuccess = false

        showSaveDialog(
            defaultFileName = defaultFileName,
            onFileSelected = { outputFile ->
                try {
                    FileOutputStream(outputFile).use { fos ->
                        document.write(fos)
                    }

                    manager.addDocument(
                        RecentDocument(
                            path = outputFile.absolutePath,
                            dict = fieldValues,
                            name = outputFile.name,
                            templateId = templateId,
                            nameForDev = nameForDev
                        )
                    )

                    isSuccess = true
                    isFileSaved = true
                    println("Файл сохранён: ${outputFile.absolutePath}")
                } catch (e: Exception) {
                    println("Ошибка при сохранении: ${e.message}")
                    isSuccess = false
                }
            },
            onCancel = {
                isSuccess = false
            }
        )

        return isSuccess
    }
}