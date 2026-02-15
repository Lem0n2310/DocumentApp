package com.example.documenteditor.functions

import org.apache.poi.xwpf.usermodel.XWPFDocument

// изменение текста в файле
fun textChange(document: XWPFDocument, replace: Map<String, String>){
    document.paragraphs.forEach { paragraph ->
        replace.forEach { (oldWord, newWord) ->
            if (paragraph.text.contains(oldWord)) {
                val updatedText = paragraph.text.replace(oldWord, newWord)
                paragraph.runs.forEach { run -> run.setText("", 0) }
                paragraph.createRun().setText(updatedText)
            }
        }
    }
}

fun tableChange(document: XWPFDocument, replace: Map<String, String>){
    document.tables.forEach { table ->
        table.rows.forEach { tableRow ->
            tableRow.tableCells.forEach { tableCell ->
                replace.forEach { (oldWord, newWord) ->
                    if(tableCell.text.contains(oldWord)){
                        val updatedText = tableCell.text.replace(oldWord, newWord)
                        tableCell.text = updatedText
                    }
                }
            }
        }
    }
}
