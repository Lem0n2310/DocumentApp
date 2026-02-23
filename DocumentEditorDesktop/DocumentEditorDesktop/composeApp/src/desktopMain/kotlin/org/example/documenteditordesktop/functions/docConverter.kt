package org.example.documenteditordesktop.functions

import org.apache.poi.hwpf.HWPFDocument
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

