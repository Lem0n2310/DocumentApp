package com.example.documenteditor.functions

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFTableCell

/**
 * Изменение текста в файле БЕЗ потери стилей.
 *
 * Раньше мы брали paragraph.text, делали replace и пересоздавали один run —
 * так терялись все run‑стили (жирный, курсив, шрифт и т.п.).
 *
 * Теперь проходимся по каждому run и меняем текст только внутри него,
 * сохраняя форматирование run.
 */
fun textChange(document: XWPFDocument, replace: Map<String, String>) {
    document.paragraphs.forEach { paragraph ->
        applyReplaceInParagraph(paragraph, replace)
    }
}

/**
 * Изменение текста в таблицах БЕЗ потери стилей.
 *
 * Раньше использовалось tableCell.text = updatedText, что удаляло всю
 * структуру параграфов и run‑ов в ячейке.
 *
 * Теперь заменяем текст только внутри существующих run‑ов.
 */
fun tableChange(document: XWPFDocument, replace: Map<String, String>) {
    document.tables.forEach { table ->
        table.rows.forEach { row ->
            row.tableCells.forEach { cell ->
                applyReplaceInTableCell(cell, replace)
            }
        }
    }
}

private fun applyReplaceInParagraph(
    paragraph: XWPFParagraph,
    replace: Map<String, String>
) {
    if (paragraph.runs.isEmpty()) return

    paragraph.runs.forEach { run ->
        val originalText = run.getText(0) ?: return@forEach
        var updatedText: String? = null

        replace.forEach { (oldWord, newWord) ->
            if (originalText.contains(oldWord)) {
                updatedText = (updatedText ?: originalText).replace(oldWord, newWord)
            }
        }

        // Если были изменения — записываем новый текст, но НЕ трогаем стили run
        updatedText?.let { newText ->
            run.setText(newText, 0)
        }
    }
}

private fun applyReplaceInTableCell(
    cell: XWPFTableCell,
    replace: Map<String, String>
) {
    cell.paragraphs.forEach { paragraph ->
        applyReplaceInParagraph(paragraph, replace)
    }
}

