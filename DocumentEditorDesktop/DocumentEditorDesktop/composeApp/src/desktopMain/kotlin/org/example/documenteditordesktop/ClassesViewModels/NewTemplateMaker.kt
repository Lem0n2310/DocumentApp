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

    fun flushCurrentTextBlockIfAny() {
        if (currentBlockParas.isEmpty()) return

        val repsForBlock = replacementsByBlock[idCounter]

        if (!repsForBlock.isNullOrEmpty()) {
            // Как в parseDocxFile: абзацы склеиваются через '\n'
            // (paragraphs.joinToString("\n")).
            data class ParagraphSegment(
                val paragraph: XWPFParagraph,
                val rangeStart: Int,
                val rangeEnd: Int
            )

            val segments = mutableListOf<ParagraphSegment>()
            var cursor = 0

            currentBlockParas.forEachIndexed { index, p ->
                val t = p.text ?: ""
                val start = cursor
                val end = start + t.length
                segments.add(
                    ParagraphSegment(
                        paragraph = p,
                        rangeStart = start,
                        rangeEnd = end
                    )
                )
                cursor = end
                // Разделитель '\n' между абзацами (кроме последнего)
                if (index < currentBlockParas.lastIndex) {
                    cursor += 1
                }
            }

            // Локальные замены по абзацам. Важно: раньше брались только
            // замены целиком внутри одного абзаца — многострочные реквизиты
            // (выделение через несколько абзацев) отбрасывались, и {{KEY}}
            // в DOCX не попадал.
            val perParaReps: MutableMap<XWPFParagraph, MutableList<Replacement>> = mutableMapOf()

            fun addParaRep(paragraph: XWPFParagraph, rep: Replacement) {
                perParaReps.getOrPut(paragraph) { mutableListOf() }.add(rep)
            }

            // С конца, чтобы при пересечениях индексы оставались стабильнее
            for (r in repsForBlock.sortedByDescending { it.start }) {
                // end — как в TextRange: exclusive
                val affected = segments.filter { seg ->
                    r.start < seg.rangeEnd && r.end > seg.rangeStart
                }
                if (affected.isEmpty()) continue

                val first = affected.first()
                val last = affected.last()

                if (first === last) {
                    val paraTextLen = (first.paragraph.text ?: "").length
                    val localStart = (r.start - first.rangeStart).coerceIn(0, paraTextLen)
                    val localEnd = (r.end - first.rangeStart).coerceIn(localStart, paraTextLen)
                    if (localStart < localEnd) {
                        addParaRep(first.paragraph, Replacement(localStart, localEnd, r.key))
                    }
                } else {
                    // Многострочное выделение:
                    // — в первом абзаце: хвост от start заменяем на {{KEY}}
                    // — средние абзацы: очищаем
                    // — в последнем: удаляем префикс до end
                    val firstLen = (first.paragraph.text ?: "").length
                    val localStart = (r.start - first.rangeStart).coerceIn(0, firstLen)
                    addParaRep(first.paragraph, Replacement(localStart, firstLen, r.key))

                    for (i in 1 until affected.lastIndex) {
                        val mid = affected[i]
                        val midLen = (mid.paragraph.text ?: "").length
                        if (midLen > 0) {
                            addParaRep(mid.paragraph, Replacement(0, midLen, ""))
                        }
                    }

                    val lastLen = (last.paragraph.text ?: "").length
                    val localEnd = (r.end - last.rangeStart).coerceIn(0, lastLen)
                    if (localEnd > 0) {
                        addParaRep(last.paragraph, Replacement(0, localEnd, ""))
                    }
                }
            }

            segments.forEach { segment ->
                val localReps = perParaReps[segment.paragraph] ?: return@forEach
                if (localReps.isEmpty()) return@forEach
                val paraText = segment.paragraph.text ?: ""
                applyReplacementsToParagraphRuns(segment.paragraph, paraText, localReps)
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
                                // Тот же формат, что в parseDocxFile / cellTextWithParagraphs
                                val original = cell.paragraphs.joinToString("\n") { it.text ?: "" }
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