package org.example.documenteditordesktop.functions

import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.usermodel.TableIterator
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Простейшее преобразование DOC -> DOCX.
 *
 * Переносит текст по абзацам, без сохранения стилей и сложной структуры.
 * Используется только как промежуточный шаг для создания шаблонов
 * из старых DOC‑файлов.
 *
 * @throws IllegalArgumentException если в DOC есть таблицы (индексы шаблона
 *         после плоской конвертации будут некорректны).
 */
fun convertDocToDocx(sourceFile: File): File {
    val userHome = System.getProperty("user.home")
    val targetDir = File(userHome, "DocumentEditor/Converted")
    if (!targetDir.exists()) {
        targetDir.mkdirs()
    }

    val targetFile = File(targetDir, sourceFile.nameWithoutExtension + ".docx")

    FileInputStream(sourceFile).use { fis ->
        val hwpf = HWPFDocument(fis)

        if (TableIterator(hwpf.range).hasNext()) {
            hwpf.close()
            throw IllegalArgumentException(
                "DOC с таблицами не поддерживается. Сохраните файл как DOCX и откройте снова."
            )
        }

        val xwpf = XWPFDocument()

        val range = hwpf.range
        val paragraphCount = range.numParagraphs()

        for (i in 0 until paragraphCount) {
            val p = range.getParagraph(i)
            val text = p.text().replace("\u0007", "").trimEnd()
            val xwpfParagraph = xwpf.createParagraph()
            val run = xwpfParagraph.createRun()
            run.setText(text)
        }

        FileOutputStream(targetFile).use { fos ->
            xwpf.write(fos)
        }
        xwpf.close()
        hwpf.close()
    }

    return targetFile
}
