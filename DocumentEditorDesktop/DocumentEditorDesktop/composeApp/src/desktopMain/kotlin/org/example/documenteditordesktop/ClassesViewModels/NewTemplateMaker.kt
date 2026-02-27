package org.example.documenteditordesktop.ClassesViewModels

import androidx.compose.runtime.snapshots.SnapshotStateMap
import org.apache.poi.xwpf.usermodel.*
import java.io.File
import kotlin.math.abs

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

    // Карта цветов для каждого ключа плейсхолдера ({{KEY}})
    val keyColors: MutableMap<String, String> = mutableMapOf()

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

        // Назначаем детерминированный цвет для каждого поля
        if (!keyColors.containsKey(keyString)) {
            keyColors[keyString] = colorForKey(keyString)
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

        val repsForBlock = replacementsByBlock[idCounter]

        if (!repsForBlock.isNullOrEmpty()) {
            // Раскладываем объединённый текст по абзацам так же,
            // как это делал parseDocxFile: каждый непустой параграф
            // даёт свой текст, пустой — один символ переноса строки.
            data class ParagraphSegment(
                val paragraph: XWPFParagraph,
                val rangeStart: Int,
                val rangeEnd: Int,
                val isBlank: Boolean
            )

            val segments = mutableListOf<ParagraphSegment>()
            var cursor = 0

            currentBlockParas.forEach { p ->
                val t = p.text ?: ""
                if (t.isNotBlank()) {
                    val start = cursor
                    val end = start + t.length
                    segments.add(
                        ParagraphSegment(
                            paragraph = p,
                            rangeStart = start,
                            rangeEnd = end,
                            isBlank = false
                        )
                    )
                    cursor = end
                } else {
                    // пустой параграф представлен как "\n"
                    val start = cursor
                    val end = start + 1
                    segments.add(
                        ParagraphSegment(
                            paragraph = p,
                            rangeStart = start,
                            rangeEnd = end,
                            isBlank = true
                        )
                    )
                    cursor = end
                }
            }

            // Для каждого абзаца собираем только те замены,
            // которые полностью попадают в его диапазон.
            segments.forEach { segment ->
                if (segment.isBlank) return@forEach

                val para = segment.paragraph
                val paraText = para.text ?: ""
                if (paraText.isEmpty()) return@forEach

                val localReps = repsForBlock
                    .filter { r ->
                        r.start >= segment.rangeStart && r.end <= segment.rangeEnd
                    }
                    .map { r ->
                        val localStart = (r.start - segment.rangeStart).coerceIn(0, paraText.length)
                        val localEnd = (r.end - segment.rangeStart).coerceIn(localStart, paraText.length)
                        Replacement(localStart, localEnd, r.key)
                    }

                if (localReps.isNotEmpty()) {
                    applyReplacementsToParagraphRuns(para, paraText, localReps)
                }
            }
        }

        // Этот TextBlock занимает один id
        idCounter++
        currentBlockParas.clear()
    }

    /**
     * Применяет замены к параграфу, стараясь максимально сохранить
     * структуру и стили run‑ов.
     *
     * @param paragraph сам параграф
     * @param originalText текст параграфа в том же виде, как его видит parseDocxFile (paragraph.text)
     * @param reps замены в координатах originalText
     */
    fun applyReplacementsToParagraphRuns(
        paragraph: XWPFParagraph,
        originalText: String,
        reps: List<Replacement>
    ) {
        if (reps.isEmpty()) return

        val runs = paragraph.runs.toList()
        if (runs.isEmpty()) {
            // Фоллбек: если run‑ов нет, просто переписываем текст
            val replaced = applyReplacementsToString(originalText, reps)
            setParagraphText(paragraph, replaced)
            return
        }

        val runTexts = runs.map { it.getText(0) ?: "" }
        val full = runTexts.joinToString("")

        // Если почему‑то текст из run‑ов не совпадает с paragraph.text,
        // лучше не рисковать и переписать параграф целиком.
        if (full.length != originalText.length) {
            val replaced = applyReplacementsToString(originalText, reps)
            setParagraphText(paragraph, replaced)
            return
        }

        val newFull = applyReplacementsToString(full, reps)

        // Распределяем текст по существующим run‑ам последовательно,
        // сохраняя их стили. Границы стилей могут немного сдвинуться,
        // но пробелы и структура текста останутся.
        var index = 0
        runs.forEach { run ->
            val old = run.getText(0) ?: ""
            val oldLen = old.length
            val end = (index + oldLen).coerceAtMost(newFull.length)
            val slice = if (index < newFull.length) newFull.substring(index, end) else ""

            run.setText("", 0)
            if (slice.isNotEmpty()) {
                run.setText(slice, 0)
            }

            index = end
        }

        // Если новый текст длиннее суммы длин run‑ов, допишем хвост в последний run
        if (index < newFull.length && runs.isNotEmpty()) {
            val last = runs.last()
            val tail = newFull.substring(index)
            val current = last.getText(0) ?: ""
            last.setText(current + tail, 0)
        }
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

    // ----- 3) Подсвечиваем все плейсхолдеры по их ключам -----
    applyColorsToPlaceholders(doc, keyColors)

    // ----- 4) Сохраняем в целевой файл -----
    targetFile.outputStream().use { out ->
        doc.write(out)
    }
    doc.close()
}

// Небольшая палитра цветов для разных полей
private fun colorForKey(key: String): String {
    val palette = listOf(
        "FF0000", // красный
        "007AFF", // синий
        "34C759", // зелёный
        "FF9500", // оранжевый
        "AF52DE", // фиолетовый
        "FF2D55", // розовый
        "5AC8FA", // голубой
        "8E8E93"  // серый
    )
    val index = abs(key.hashCode()) % palette.size
    return palette[index]
}

// Проходим по документу и подсвечиваем все run'ы, содержащие плейсхолдер
private fun applyColorsToPlaceholders(
    doc: XWPFDocument,
    keyColors: Map<String, String>
) {
    if (keyColors.isEmpty()) return

    fun colorRuns(paragraph: XWPFParagraph) {
        paragraph.runs?.forEach { run ->
            val text = run.getText(0) ?: return@forEach
            for ((key, color) in keyColors) {
                if (text.contains(key)) {
                    run.color = color
                    break
                }
            }
        }
    }

    // Параграфы вне таблиц
    doc.paragraphs.forEach { paragraph ->
        colorRuns(paragraph)
    }

    // Параграфы внутри таблиц
    doc.tables.forEach { table ->
        table.rows.forEach { row ->
            row.tableCells.forEach { cell ->
                cell.paragraphs.forEach { paragraph ->
                    colorRuns(paragraph)
                }
            }
        }
    }
}