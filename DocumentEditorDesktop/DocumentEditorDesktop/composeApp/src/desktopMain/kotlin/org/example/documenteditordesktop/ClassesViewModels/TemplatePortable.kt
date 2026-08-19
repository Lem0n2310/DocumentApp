package org.example.documenteditordesktop.ClassesViewModels

import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Портативный пакет шаблона (.detmpl = zip с metadata + docx).
 * На другом устройстве достаточно импортировать этот файл — шаблон сразу готов к работе.
 */
object TemplatePortable {
    const val EXTENSION = "detmpl"
    private const val META_ENTRY = "template.json"
    private const val DOCX_ENTRY = "template.docx"

    private val templateFolder: File
        get() = File(System.getProperty("user.home"), "DocumentEditor/Templates")

    fun exportTemplate(template: DocumentTemplate, destination: File): Result<File> = runCatching {
        val sourceDocx = File(templateFolder, "${template.nameForDevelop}.docx")
        require(sourceDocx.exists()) {
            "Файл шаблона не найден: ${sourceDocx.absolutePath}"
        }

        val outFile = if (destination.name.endsWith(".$EXTENSION", ignoreCase = true)) {
            destination
        } else {
            File(destination.parentFile, "${destination.name}.$EXTENSION")
        }
        outFile.parentFile?.mkdirs()

        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            zip.putNextEntry(ZipEntry(META_ENTRY))
            zip.write(Gson().toJson(template).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(DOCX_ENTRY))
            FileInputStream(sourceDocx).use { input -> input.copyTo(zip) }
            zip.closeEntry()
        }

        outFile
    }

    fun importTemplate(source: File): Result<DocumentTemplate> = runCatching {
        require(source.exists()) { "Файл не найден: ${source.absolutePath}" }

        var metaJson: String? = null
        var docxBytes: ByteArray? = null

        ZipInputStream(FileInputStream(source)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name.substringAfterLast('/')) {
                    META_ENTRY -> metaJson = zip.readBytes().toString(Charsets.UTF_8)
                    DOCX_ENTRY -> docxBytes = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val json = requireNotNull(metaJson) { "В пакете нет $META_ENTRY" }
        val bytes = requireNotNull(docxBytes) { "В пакете нет $DOCX_ENTRY" }

        val imported = Gson().fromJson(json, DocumentTemplate::class.java)
            ?: error("Не удалось прочитать метаданные шаблона")

        val manager = Manager(DocumentTemplate::class.java)
        val existing = manager.loadJson()

        val uniqueDevName = uniqueNameForDevelop(
            preferred = imported.nameForDevelop.ifBlank {
                imported.nameForUser.ifBlank { "imported_template" }
            },
            taken = existing.map { it.nameForDevelop }.toSet()
        )
        val newId = (existing.maxOfOrNull { it.id } ?: 0) + 1

        templateFolder.mkdirs()
        val targetDocx = File(templateFolder, "$uniqueDevName.docx")
        targetDocx.writeBytes(bytes)

        val now = System.currentTimeMillis()
        val saved = DocumentTemplate(
            id = newId,
            nameForUser = imported.nameForUser.ifBlank { uniqueDevName },
            nameForDevelop = uniqueDevName,
            fields = imported.fields,
            createdAt = imported.createdAt.takeIf { it > 0L } ?: now,
            lastUsedAt = now
        )
        manager.addDocument(saved)
        saved
    }

    private fun uniqueNameForDevelop(preferred: String, taken: Set<String>): String {
        if (preferred !in taken) return preferred
        var index = 1
        while ("${preferred}_$index" in taken) {
            index++
        }
        return "${preferred}_$index"
    }
}
