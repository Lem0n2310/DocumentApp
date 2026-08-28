package com.example.documenteditor.functions

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import org.example.documenteditordesktop.functions.wrapPlaceholder

/**
 * Заменяет плейсхолдеры в обычных параграфах документа.
 * Берёт документ и карту замен, затем для каждого параграфа вызывает applyReplaceInParagraph.
 */
fun textChange(document: XWPFDocument, replace: Map<String, String>) {
    document.paragraphs.forEach { paragraph ->
        applyReplaceInParagraph(paragraph, replace)
    }
}

/**
 * Заменяет плейсхолдеры в таблицах документа.
 * Обходит таблицы → строки → ячейки и для каждой ячейки вызывает applyReplaceInTableCell.
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
 * Записывает в run многострочный текст.
 * Очищает run, режет строку по '\n', первую часть ставит через setText,
 * каждую следующую — через addBreak и setText.
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
 * Заменяет плейсхолдеры в одном параграфе.
 * Склеивает текст всех run'ов, применяет замены к этой строке,
 * затем записывает результат обратно в те же run'ы.
 */
private fun applyReplaceInParagraph(
    paragraph: XWPFParagraph,
    replace: Map<String, String>
) {
    // Пустая карта или параграф без run'ов — менять нечего.
    if (replace.isEmpty()) return
    val runs = paragraph.runs
    if (runs.isEmpty()) return

    // Собираем полный текст параграфа из кусков отдельных run'ов.
    val runTexts = runs.map { it.getText(0) ?: "" }
    var fullText = runTexts.joinToString("")
    val original = fullText

    // Берём пары {{ключ}} → ответ и последовательно подставляем их в строку.
    placeholderReplacements(replace).forEach { (oldWord, newWord) ->
        fullText = fullText.replace(oldWord, newWord)
    }
    val newFullText = fullText
    if (newFullText == original) return

    // Если в результате есть переносы строк — весь текст кладём в первый run, остальные очищаем.
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

    // Режем новый текст кусками той же длины, что были у старых run'ов, и пишем по одному куску в каждый run.
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
    // Хвост, который не влез в длины старых run'ов, дописываем в последний run.
    if (index < newFullText.length && runs.isNotEmpty()) {
        val last = runs.last()
        val tail = newFullText.substring(index)
        val current = last.getText(0) ?: ""
        last.setText(current + tail, 0)
    }
}

/**
 * Строит список замен для документа: {{ключ}} → значение.
 * Каждый ключ карты оборачивается в {{…}}, повтор того же плейсхолдера пропускается,
 * затем пары сортируются по убыванию длины ключа.
 */
private fun placeholderReplacements(replace: Map<String, String>): List<Pair<String, String>> {
    val byPlaceholder = linkedMapOf<String, String>()
    replace.forEach { (oldWord, newWord) ->
        val placeholder = wrapPlaceholder(oldWord)
        if (placeholder.isNotEmpty()) {
            byPlaceholder.putIfAbsent(placeholder, newWord)
        }
    }
    return byPlaceholder.entries
        .sortedByDescending { it.key.length }
        .map { it.key to it.value }
}

/**
 * Заменяет плейсхолдеры внутри ячейки таблицы.
 * Берёт все параграфы ячейки и для каждого вызывает applyReplaceInParagraph.
 */
private fun applyReplaceInTableCell(
    cell: XWPFTableCell,
    replace: Map<String, String>
) {
    cell.paragraphs.forEach { paragraph ->
        applyReplaceInParagraph(paragraph, replace)
    }
}
