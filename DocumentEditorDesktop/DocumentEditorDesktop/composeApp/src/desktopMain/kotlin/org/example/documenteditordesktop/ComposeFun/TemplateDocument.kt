package org.example.documenteditordesktop.ComposeFun

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.apache.poi.xwpf.usermodel.XWPFTableCell
import java.io.File
import java.io.FileInputStream

sealed class TemplateElement {
    data class TextBlock(
        val id: Int,
        var text: String,
    ) : TemplateElement()

    data class Table(
        val id: Int,
        val rows: List<List<String>>
    ) : TemplateElement()
}

data class TemplateState(
    val elements: MutableList<TemplateElement>,
    var nameForUser: String = "",
)

/** Текст ячейки с сохранением абзацев (как joinToString("\n")). */
private fun cellTextWithParagraphs(cell: XWPFTableCell): String =
    cell.paragraphs.joinToString("\n") { it.text ?: "" }

/**
 * Парсер DOCX файла.
 * Абзацы внутри TextBlock склеиваются через '\n', чтобы в редакторе
 * сохранялось деление на параграфы; та же схема используется в applyTemplateChangesByIndex.
 */
fun parseDocxFile(file: File): TemplateState {
    return FileInputStream(file).use { stream ->
        XWPFDocument(stream).use { doc ->
            val elements = mutableListOf<TemplateElement>()
            var idCounter = 0
            val paragraphBuffer = mutableListOf<String>()

            fun flushTextBlock() {
                if (paragraphBuffer.isEmpty()) return
                val text = paragraphBuffer.joinToString("\n")
                elements.add(TemplateElement.TextBlock(id = idCounter++, text = text))
                paragraphBuffer.clear()
            }

            doc.bodyElements.forEach { element ->
                when (element) {
                    is XWPFParagraph -> {
                        paragraphBuffer.add(element.text ?: "")
                    }

                    is XWPFTable -> {
                        flushTextBlock()
                        val rows = element.rows.map { row ->
                            row.tableCells.map { cell -> cellTextWithParagraphs(cell) }
                        }
                        elements.add(TemplateElement.Table(id = idCounter++, rows = rows))
                    }
                }
            }
            flushTextBlock()
            TemplateState(elements = elements)
        }
    }
}
