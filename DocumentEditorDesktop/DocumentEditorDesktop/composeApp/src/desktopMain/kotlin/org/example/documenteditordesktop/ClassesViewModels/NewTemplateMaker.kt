package org.example.documenteditordesktop.ClassesViewModels

import androidx.compose.runtime.snapshots.SnapshotStateMap
import org.apache.poi.xwpf.usermodel.*
import java.io.File

// Внутренний holder для замены
private data class Replacement(val start: Int, val end: Int, val key: String)

/**
 * Применяет замены по индексам из полей (fields) к КОПИИ исходного DOCX.
 * Идентичная схеме нумерации в parseDocxFile: TextBlock и Table получают последовательные id.
 *
 * @param sourceFile  исходный DOCX
 * @param targetFile  файл-копия, куда запишется результат
 * @param fields      Map<DocumentField, Map<List<Int>, String>>
 *                    где ключ List<Int>:
 *                       - [textBlockId, start, end] для TextBlock
 *                       - [tableId, rowId, colId, start, end] для ячеек таблиц
 *                    а DocumentField.key — это строка вида {{KEY}}
 */
fun applyTemplateChangesByIndex(
    sourceFile: File,
    targetFile: File,
    fields: MutableMap<DocumentField, SnapshotStateMap<List<Int>, String>>
) {
    val doc = XWPFDocument(sourceFile.inputStream())

    // ----- 1) Собираем замены -----

    // TextBlock: id -> список Replacement(start, end, key)
    val replacementsByBlock: MutableMap<Int, MutableList<Replacement>> = mutableMapOf()

    // Table: tableId -> (row,col) -> список Replacement(start, end, key)
    val replacementsByTable: MutableMap<Int, MutableMap<Pair<Int, Int>, MutableList<Replacement>>> = mutableMapOf()

    for ((documentField, frags) in fields) {
        val keyString = try {
            // Пытаемся получить поле key у DocumentField рефлексией, чтобы не тянуть тип
            val k = documentField::class.java.getDeclaredField("key")
            k.isAccessible = true
            (k.get(documentField) as? String) ?: continue
        } catch (_: Exception) {
            continue
        }

        for ((k, _) in frags) {
            if (k.size == 3) {
                // [blockId, start, end]
                val blockId = k[0]
                val start = k[1]
                val end = k[2]

                if (!replacementsByBlock.containsKey(blockId)) {
                    replacementsByBlock[blockId] = mutableListOf()
                }
                replacementsByBlock[blockId]!!.add(Replacement(start, end, keyString))
            } else if (k.size == 5) {
                // [tableId, rowId, colId, start, end]
                val tableId = k[0]
                val rowId = k[1]
                val colId = k[2]
                val start = k[3]
                val end = k[4]

                if (!replacementsByTable.containsKey(tableId)) {
                    replacementsByTable[tableId] = mutableMapOf()
                }
                val tableMap = replacementsByTable[tableId]!!

                val cellKey = Pair(rowId, colId)
                if (!tableMap.containsKey(cellKey)) {
                    tableMap[cellKey] = mutableListOf()
                }
                tableMap[cellKey]!!.add(Replacement(start, end, keyString))
            }
        }
    }

    // ----- 2) Обход документа с тем же счётчиком id, что в parseDocxFile -----

    var idCounter = 0
    val currentBlockParas = mutableListOf<XWPFParagraph>()

    fun combineTextBlock(paragraphs: List<XWPFParagraph>): String {
        val sb = StringBuilder()
        paragraphs.forEach { p ->
            val t = p.text ?: ""
            if (t.isNotBlank()) {
                sb.append(t)
            } else {
                // как в твоём парсере: пустой параграф — это перенос строки
                sb.append("\n")
            }
        }
        return sb.toString()
    }

    fun applyReplacementsToString(text: String, reps: List<Replacement>): String {
        var result = text
        // очень важно: применять с конца, чтобы индексы не поплыли
        val sorted = reps.sortedByDescending { it.start }
        for (r in sorted) {
            val s = r.start.coerceIn(0, result.length)
            val e = r.end.coerceIn(s, result.length)
            result = result.substring(0, s) + r.key + result.substring(e)
        }
        return result
    }

    fun clearParagraphRuns(p: XWPFParagraph) {
        // Нельзя делать p.runs.clear(); надо удалять через removeRun
        while (p.runs.size > 0) {
            p.removeRun(0)
        }
    }

    fun setParagraphText(p: XWPFParagraph, text: String) {
        clearParagraphRuns(p)
        if (text.isNotEmpty()) {
            val run = p.createRun()
            run.setText(text, 0)
        } else {
            // оставляем параграф пустым
        }
    }

    fun setCellTextPreservingNewlines(cell: XWPFTableCell, text: String) {
        // полностью очищаем параграфы в ячейке
        while (cell.paragraphs.size > 0) {
            cell.removeParagraph(0)
        }
        // создаём параграфы по \n
        val parts = text.split("\n")
        if (parts.isEmpty()) {
            cell.addParagraph() // хоть один параграф должен быть
            return
        }
        parts.forEach { part ->
            val p = cell.addParagraph()
            setParagraphText(p, part)
        }
    }

    fun flushCurrentTextBlockIfAny() {
        if (currentBlockParas.isEmpty()) return

        val reps = replacementsByBlock[idCounter]
        if (!reps.isNullOrEmpty()) {
            val original = combineTextBlock(currentBlockParas)
            val replaced = applyReplacementsToString(original, reps)

            // Пишем результат в первый параграф блока,
            // остальные параграфы очищаем (чтобы не потерять структуру body)
            setParagraphText(currentBlockParas.first(), replaced)
            for (i in 1 until currentBlockParas.size) {
                setParagraphText(currentBlockParas[i], "")
            }
        }
        // Этот TextBlock занимает один id
        idCounter++
        currentBlockParas.clear()
    }

    // основной проход
    doc.bodyElements.forEach { el ->
        when (el) {
            is XWPFParagraph -> {
                currentBlockParas.add(el)
            }

            is XWPFTable -> {
                // завершить текущий TextBlock (если есть) перед таблицей
                flushCurrentTextBlockIfAny()

                // обработать таблицу по tableId == текущий idCounter
                val tableId = idCounter
                val tableReps = replacementsByTable[tableId]

                if (tableReps != null && tableReps.isNotEmpty()) {
                    el.rows.forEachIndexed { rIdx, row ->
                        row.tableCells.forEachIndexed { cIdx, cell ->
                            val reps = tableReps[Pair(rIdx, cIdx)]
                            if (!reps.isNullOrEmpty()) {
                                val original = cell.text ?: ""
                                val replaced = applyReplacementsToString(original, reps)
                                setCellTextPreservingNewlines(cell, replaced)
                            }
                        }
                    }
                }

                // Таблица занимает один id
                idCounter++
            }
        }
    }

    // в конце может остаться хвостовой TextBlock
    flushCurrentTextBlockIfAny()

    // ----- 3) Сохраняем в целевой файл -----
    targetFile.outputStream().use { out ->
        doc.write(out)
    }
    doc.close()
}