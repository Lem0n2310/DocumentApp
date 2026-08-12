package com.example.documenteditor.functions

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFTableCell

/**
 * Изменение текста в файле БЕЗ потери стилей.
 *
 * Замены выполняются на уровне параграфа: текст всех run'ов объединяется,
 * затем применяются замены (так плейсхолдеры вроде {{name}} заменяются даже
 * когда разбиты на несколько run'ов в DOCX). Результат распределяется обратно
 * по run'ам с сохранением форматирования.
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

/**
 * Пишет текст в run с мягкими переносами строк (w:br) вместо литеральных '\n',
 * чтобы многострочные значения (например, реквизиты) корректно отображались в Word.
 */
private fun setRunTextWithBreaks(run: XWPFRun, text: String) {
    run.setText("", 0)
    val parts = text.split("\n")
    parts.forEachIndexed { index, part ->
        if (index == 0) {
            run.setText(part, 0)
        } else {
            run.addBreak()
            run.setText(part)
        }
    }
}

/**
 * Заменяет плейсхолдеры в параграфе. Текст всех run'ов объединяется в одну строку,
 * затем применяются все замены — так заменяются фразы, разбитые на несколько run'ов
 * (типично для DOCX). Результат записывается обратно в run'ы с сохранением стилей.
 */
private fun applyReplaceInParagraph(
    paragraph: XWPFParagraph,
    replace: Map<String, String>
) {
    if (replace.isEmpty()) return
    val runs = paragraph.runs
    if (runs.isEmpty()) return

    val runTexts = runs.map { it.getText(0) ?: "" }
    var fullText = runTexts.joinToString("")
    val original = fullText

    replace.forEach { (oldWord, newWord) ->
        fullText = fullText.replace(oldWord, newWord)
    }
    val newFullText = fullText
    if (newFullText == original) return

    // Многострочная подстановка: нельзя размазать '\n' по run'ам как обычный текст —
    // нужен break. Кладём результат в первый run, остальные очищаем.
    if (newFullText.contains('\n')) {
        runs.forEachIndexed { index, run ->
            if (index == 0) {
                setRunTextWithBreaks(run, newFullText)
            } else {
                run.setText("", 0)
            }
        }
        return
    }

    // Распределяем новый текст по run'ам по длинам старых run'ов (сохраняем стили)
    var index = 0
    runs.forEach { run ->
        val oldLen = (run.getText(0) ?: "").length
        val end = (index + oldLen).coerceAtMost(newFullText.length)
        val slice = if (index < newFullText.length) newFullText.substring(index, end) else ""
        run.setText("", 0)
        if (slice.isNotEmpty()) {
            run.setText(slice, 0)
        }
        index = end
    }
    // Если новый текст длиннее — дописываем в последний run
    if (index < newFullText.length && runs.isNotEmpty()) {
        val last = runs.last()
        val tail = newFullText.substring(index)
        val current = last.getText(0) ?: ""
        last.setText(current + tail, 0)
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

