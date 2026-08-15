package com.example.documenteditor.ClassesViewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.example.documenteditordesktop.ClassesViewModels.DocumentTemplate
import org.example.documenteditordesktop.ClassesViewModels.RecentDocument
import org.example.documenteditordesktop.ClassesViewModels.Manager
import org.example.documenteditordesktop.ClassesViewModels.TemplatePortable
import org.example.documenteditordesktop.templatesFun.absoluteTemplate
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter
import javax.swing.JFileChooser

class SaveViewModel {
    var isFileSaved by mutableStateOf(false)
    var lastError by mutableStateOf<String?>(null)

    fun showOpenDialog(
        onFileSelected: (File) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        // Использование AWT FileDialog (более нативный вид)
        val dialog = FileDialog(null as Frame?, "Открыть файл", FileDialog.LOAD).apply {
            // Фильтр для DOC и DOCX файлов
            filenameFilter = FilenameFilter { _, name ->
                name.endsWith(".docx", ignoreCase = true) ||
                        name.endsWith(".doc", ignoreCase = true)
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

    fun showOpenTemplatePackageDialog(
        onFileSelected: (File) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val dialog = FileDialog(null as Frame?, "Импортировать шаблон", FileDialog.LOAD).apply {
            filenameFilter = FilenameFilter { _, name ->
                name.endsWith(".${TemplatePortable.EXTENSION}", ignoreCase = true) ||
                    name.endsWith(".zip", ignoreCase = true)
            }
            isVisible = true
        }

        if (dialog.file != null) {
            onFileSelected(File(dialog.directory, dialog.file))
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

    fun showSaveTemplatePackageDialog(
        defaultFileName: String,
        onFileSelected: (File) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val extension = TemplatePortable.EXTENSION
        val dialog = FileDialog(null as Frame?, "Поделиться шаблоном", FileDialog.SAVE).apply {
            file = if (defaultFileName.endsWith(".$extension", ignoreCase = true)) {
                defaultFileName
            } else {
                "$defaultFileName.$extension"
            }
            isVisible = true
        }

        if (dialog.file != null) {
            val file = File(dialog.directory, dialog.file).let { chosen ->
                if (!chosen.name.endsWith(".$extension", ignoreCase = true)) {
                    File(chosen.parent, "${chosen.name}.$extension")
                } else {
                    chosen
                }
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
        isFileSaved = false
        lastError = null

        val manager = Manager<RecentDocument>(RecentDocument::class.java)
        val templateFolder = File(System.getProperty("user.home"),"DocumentEditor/Templates")
        val templateFile = File(templateFolder, "${nameForDev}.docx")

        if (!templateFile.exists()) {
            lastError = "Файл шаблона не найден: ${templateFile.name}"
            println(lastError)
            return false
        }

        val templateDocument = try {
            FileInputStream(templateFile).use { stream ->
                XWPFDocument(stream)
            }
        } catch (e: Exception) {
            lastError = "Не удалось открыть шаблон: ${e.message}"
            println(lastError)
            return false
        }

        val document = absoluteTemplate(
            templateDocument,
            fieldValues
        )

        var isSuccess = false

        showSaveDialog(
            defaultFileName = defaultFileName,
            onFileSelected = { outputFile ->
                val written = writeDocument(document, outputFile)
                if (written) {
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
                } else {
                    isSuccess = false
                    isFileSaved = false
                }
            },
            onCancel = {
                isSuccess = false
                isFileSaved = false
            }
        )

        return isSuccess
    }

    fun showChooseDirectoryDialog(
        onFolderSelected: (File) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        if (os.contains("mac")) {
            val previous = System.getProperty("apple.awt.fileDialogForDirectories")
            System.setProperty("apple.awt.fileDialogForDirectories", "true")
            val dialog = FileDialog(null as Frame?, "Куда сохранить дело", FileDialog.LOAD).apply {
                isVisible = true
            }
            if (previous == null) {
                System.clearProperty("apple.awt.fileDialogForDirectories")
            } else {
                System.setProperty("apple.awt.fileDialogForDirectories", previous)
            }
            if (dialog.directory != null && dialog.file != null) {
                onFolderSelected(File(dialog.directory, dialog.file))
            } else {
                onCancel()
            }
        } else {
            val chooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Куда сохранить дело"
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                onFolderSelected(chooser.selectedFile)
            } else {
                onCancel()
            }
        }
    }

    fun saveCaseFolder(
        caseName: String,
        templates: List<DocumentTemplate>,
        answersByTemplate: Map<Int, Map<String, String>>
    ): File? {
        lastError = null
        var result: File? = null
        showChooseDirectoryDialog(
            onFolderSelected = { parent ->
                result = writeCaseFolder(caseName, templates, answersByTemplate, parent)
            },
            onCancel = { result = null }
        )
        return result
    }

    private fun writeCaseFolder(
        caseName: String,
        templates: List<DocumentTemplate>,
        answersByTemplate: Map<Int, Map<String, String>>,
        parent: File
    ): File? {
        val folder = uniqueDirectory(parent, sanitizeFileName(caseName.ifBlank { "Дело" }))
        if (!folder.mkdirs() && !folder.exists()) {
            lastError = "Не удалось создать папку: ${folder.absolutePath}"
            return null
        }

        val recentManager = Manager<RecentDocument>(RecentDocument::class.java)
        val usedNames = mutableSetOf<String>()

        for (template in templates) {
            val answers = answersByTemplate[template.id].orEmpty()
            val fileName = uniqueFileName(
                preferred = sanitizeFileName(template.nameForUser.ifBlank { template.nameForDevelop }) + ".docx",
                used = usedNames
            )
            usedNames.add(fileName.lowercase())
            val outputFile = File(folder, fileName)
            val written = writeFilledDocument(template, answers, template.nameForDevelop, outputFile)
            if (!written) {
                return null
            }
            recentManager.addDocument(
                RecentDocument(
                    path = outputFile.absolutePath,
                    dict = answers,
                    name = outputFile.name,
                    templateId = template.id,
                    nameForDev = template.nameForDevelop
                )
            )
        }

        if (Desktop.isDesktopSupported()) {
            runCatching { Desktop.getDesktop().open(folder) }
        }
        return folder
    }

    private fun writeFilledDocument(
        selectedTemplate: DocumentTemplate,
        fieldValues: Map<String, String>,
        nameForDev: String,
        outputFile: File
    ): Boolean {
        val templateFolder = File(System.getProperty("user.home"), "DocumentEditor/Templates")
        val templateFile = File(templateFolder, "${nameForDev}.docx")
        if (!templateFile.exists()) {
            lastError = "Файл шаблона не найден: ${templateFile.name}"
            return false
        }
        return try {
            FileInputStream(templateFile).use { stream ->
                XWPFDocument(stream).use { templateDocument ->
                    val document = absoluteTemplate(templateDocument, fieldValues)
                    writeDocument(document, outputFile)
                }
            }
        } catch (e: Exception) {
            lastError = e.message ?: "Не удалось сохранить ${selectedTemplate.nameForUser}"
            false
        }
    }

    private fun writeDocument(document: XWPFDocument, outputFile: File): Boolean {
        return try {
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                document.write(fos)
            }
            true
        } catch (e: Exception) {
            lastError = e.message ?: "Ошибка при сохранении"
            false
        }
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().trim('.')
        return cleaned.ifBlank { "Документ" }
    }

    private fun uniqueDirectory(parent: File, name: String): File {
        var dir = File(parent, name)
        var index = 1
        while (dir.exists()) {
            dir = File(parent, "$name ($index)")
            index++
        }
        return dir
    }

    private fun uniqueFileName(preferred: String, used: Set<String>): String {
        if (preferred.lowercase() !in used) return preferred
        val dot = preferred.lastIndexOf('.')
        val base = if (dot > 0) preferred.substring(0, dot) else preferred
        val ext = if (dot > 0) preferred.substring(dot) else ""
        var index = 1
        var candidate: String
        do {
            candidate = "$base ($index)$ext"
            index++
        } while (candidate.lowercase() in used)
        return candidate
    }
}